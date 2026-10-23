import type {Session} from './auth';
import {reviewDigest} from './legacy-review';

export type PrivacyMode = 'EVIDENCE_RETENTION' | 'AUDIT_RETENTION' | 'ERASE_ACCOUNT';
type Env = {DB:D1Database; FIREBASE_PROJECT_ID:string; LEGACY_OWNER_UID?:string};
type Inventory = {claimRevision:number; sourceRevision:number; grantRevision:number; claims:number;
  evidence:number; history:number; reviewEvidence:number; events:number; receipts:number; requests:number;
  recoveries:number; identities:number; grants:number; snapshots:number; versions:number; resolvedAt:string|null;
  finallyClosedAt:string|null; state:string|null};

/** Private operator operations only. No Worker route, Android role or SQL supplied by clients. */
export class LegacyPrivacyService {
  constructor(private env:Env) {}
  private authorize(session:Session, now:Date) {
    const c=session.claims;
    if(!this.env.LEGACY_OWNER_UID || session.uid!==this.env.LEGACY_OWNER_UID || c.sub!==session.uid ||
      c.aud!==this.env.FIREBASE_PROJECT_ID || c.iss!==`https://securetoken.google.com/${this.env.FIREBASE_PROJECT_ID}` ||
      c.admin!==true || typeof c.auth_time!=='number' || c.auth_time>now.getTime()/1000 || now.getTime()/1000-c.auth_time>300)
      throw new Error('FORBIDDEN');
  }
  private inventory(uid:string) {
    return this.env.DB.prepare(`SELECT
      COALESCE((SELECT revision FROM legacy_claims WHERE firebase_project_id=?1 AND uid=?2),-1) claimRevision,
      COALESCE((SELECT revision FROM entitlement_source_versions WHERE firebase_project_id=?1 AND uid=?2),-1) sourceRevision,
      COALESCE((SELECT grant_revision FROM legacy_grants WHERE firebase_project_id=?1 AND uid=?2),-1) grantRevision,
      (SELECT count(*) FROM legacy_claims WHERE firebase_project_id=?1 AND uid=?2) claims,
      (SELECT count(*) FROM legacy_evidence WHERE firebase_project_id=?1 AND uid=?2) evidence,
      (SELECT count(*) FROM legacy_historical_use WHERE firebase_project_id=?1 AND evidence_id IN
        (SELECT evidence_id FROM legacy_evidence WHERE firebase_project_id=?1 AND uid=?2)) history,
      (SELECT count(*) FROM legacy_review_evidence WHERE claim_id IN
        (SELECT claim_id FROM legacy_claims WHERE firebase_project_id=?1 AND uid=?2)) reviewEvidence,
      (SELECT count(*) FROM legacy_review_events WHERE claim_id IN
        (SELECT claim_id FROM legacy_claims WHERE firebase_project_id=?1 AND uid=?2)) events,
      (SELECT count(*) FROM legacy_review_issuances WHERE firebase_project_id=?1 AND uid=?2) receipts,
      (SELECT count(*) FROM legacy_claim_requests WHERE firebase_project_id=?1 AND uid=?2) requests,
      (SELECT count(*) FROM legacy_recovery_requests WHERE firebase_project_id=?1 AND uid=?2) recoveries,
      (SELECT count(*) FROM legacy_claim_identities WHERE claim_id IN
        (SELECT claim_id FROM legacy_claims WHERE firebase_project_id=?1 AND uid=?2)) identities,
      (SELECT count(*) FROM legacy_grants WHERE firebase_project_id=?1 AND uid=?2) grants,
      (SELECT count(*) FROM entitlement_snapshots WHERE firebase_project_id=?1 AND uid=?2) snapshots,
      (SELECT count(*) FROM entitlement_source_versions WHERE firebase_project_id=?1 AND uid=?2) versions,
      (SELECT resolved_at FROM legacy_claims WHERE firebase_project_id=?1 AND uid=?2) resolvedAt,
      (SELECT f.finally_closed_at FROM legacy_privacy_closures f JOIN legacy_claims c ON c.claim_id=f.claim_id
        WHERE c.firebase_project_id=?1 AND c.uid=?2 AND c.revision=f.claim_revision) finallyClosedAt,
      (SELECT state FROM legacy_claims WHERE firebase_project_id=?1 AND uid=?2) state
    `).bind(this.env.FIREBASE_PROJECT_ID,uid).first<Inventory>();
  }
  async plan(operator:Session,uid:string,mode:PrivacyMode,now=new Date()) {
    this.authorize(operator,now);
    if(!uid || uid.length>128 || !['EVIDENCE_RETENTION','AUDIT_RETENTION','ERASE_ACCOUNT'].includes(mode)) throw new Error('INVALID_REQUEST');
    const inventory=(await this.inventory(uid))!;
    let due=mode==='ERASE_ACCOUNT';
    if(inventory.finallyClosedAt && ['GRANTED','REJECTED'].includes(inventory.state??'')) {
      const until=new Date(inventory.finallyClosedAt);
      if(mode==='EVIDENCE_RETENTION') until.setUTCDate(until.getUTCDate()+30);
      if(mode==='AUDIT_RETENTION') until.setUTCFullYear(until.getUTCFullYear()+1);
      due ||= now>=until;
    }
    return {mode, due, inventory, fingerprint:await reviewDigest([this.env.FIREBASE_PROJECT_ID,uid,mode,inventory]),
      externalSteps:mode==='ERASE_ACCOUNT'?['FIREBASE_AUTH','SUPPORT','DEVICE_CACHE','PROVIDER_BACKUPS']:['SUPPORT_AND_ATTACHMENTS'],
      automaticRecoveryAfterErasure:mode==='ERASE_ACCOUNT'?false:null};
  }
  async closeCase(operator:Session,uid:string,now=new Date()) {
    this.authorize(operator,now);
    const row=await this.env.DB.prepare(`SELECT claim_id,revision,resolved_at FROM legacy_claims
      WHERE firebase_project_id=? AND uid=? AND state IN ('GRANTED','REJECTED') AND resolved_at IS NOT NULL`)
      .bind(this.env.FIREBASE_PROJECT_ID,uid).first<{claim_id:string;revision:number;resolved_at:string}>();
    if(!row || Date.parse(row.resolved_at)>+now) throw new Error('CONFLICT');
    await this.env.DB.prepare(`INSERT INTO legacy_privacy_closures SELECT claim_id,revision,? FROM legacy_claims
      WHERE claim_id=? AND revision=? AND state IN ('GRANTED','REJECTED') ON CONFLICT DO NOTHING`)
      .bind(now.toISOString(),row.claim_id,row.revision).run();
    return {finallyClosed:true};
  }
  async execute(operator:Session,uid:string,mode:PrivacyMode,input:{operationId:string;fingerprint:string;
    ownershipConfirmed:boolean;eraseWithoutAutomaticRecovery?:boolean},now=new Date()) {
    this.authorize(operator,now);
    if(!input.ownershipConfirmed || !/^[A-Za-z0-9_-]{16,128}$/.test(input.operationId) ||
       (mode==='ERASE_ACCOUNT' && input.eraseWithoutAutomaticRecovery!==true)) throw new Error('INVALID_REQUEST');
    const existing=await this.env.DB.prepare('SELECT mode,state,result_json FROM legacy_privacy_operations WHERE operation_id=? AND firebase_project_id=?')
      .bind(input.operationId,this.env.FIREBASE_PROJECT_ID).first<{mode:string;state:string;result_json:string}>();
    if(existing) {
      if(existing.mode!==mode || existing.state!=='COMPLETE') throw new Error('CONFLICT');
      return {complete:true,idempotent:true,externalStepsPending:true};
    }
    const plan=await this.plan(operator,uid,mode,now);
    if(!plan.due || plan.fingerprint!==input.fingerprint) throw new Error('CONFLICT');
    const i=plan.inventory;
    // Checked again in the INSERT statement; SQLite serializes trigger execution.
    await this.env.DB.prepare(`INSERT INTO legacy_privacy_operations
      SELECT ?1,?2,?3,?4,?5,'APPLYING',?6 WHERE
      COALESCE((SELECT revision FROM legacy_claims WHERE firebase_project_id=?2 AND uid=?3),-1)=?7 AND
      COALESCE((SELECT revision FROM entitlement_source_versions WHERE firebase_project_id=?2 AND uid=?3),-1)=?8 AND
      COALESCE((SELECT grant_revision FROM legacy_grants WHERE firebase_project_id=?2 AND uid=?3),-1)=?9`)
      .bind(input.operationId,this.env.FIREBASE_PROJECT_ID,uid,mode,now.toISOString(),
        JSON.stringify({policy:'oq07-dev-2026-10-16',mode,counts:{claims:i.claims,evidence:i.evidence,
          history:i.history,reviewEvidence:i.reviewEvidence,events:i.events,receipts:i.receipts,grants:i.grants}}),
        i.claimRevision,i.sourceRevision,i.grantRevision).run();
    const saved=await this.env.DB.prepare("SELECT state FROM legacy_privacy_operations WHERE operation_id=? AND firebase_project_id=?")
      .bind(input.operationId,this.env.FIREBASE_PROJECT_ID).first<{state:string}>();
    if(saved?.state!=='COMPLETE') throw new Error('CONFLICT');
    return {complete:true,idempotent:false,externalStepsPending:true};
  }
}
