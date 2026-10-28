import validSnapshot from './generated/entitlement-validator.js';
import {validateLegacyGrant, validateLegacyClaim} from './generated/legacy-validator.js';
import type {Session} from './auth';
import type {LegacyGrant, LegacyClaim} from './legacy';

/** S01 read model, never a writable Android DTO or a financial source. */
export interface EntitlementSnapshot {
  contractVersion:'1.0.0'; revision:number; serverNow:string; checkedAt:string;
  subscriptionRequired:boolean; legacyCore:boolean; coreAllowed:boolean; proAllowed:boolean;
  proFeatureSet:string[];
  legacy:{state:'UNKNOWN'|'UNDER_REVIEW'|'GRANTED'|'REJECTED';legacyGrantedAt:string|null;
    legacyEvidenceType:string|null;legacyCoreVersionBaseline:string|null;migrationVersion:string|null;grantRevision:number|null};
  play:{state:string;billingPhase:string;validUntil:string|null;checkedAt:string|null;productId:string|null;basePlanId:string|null};
  promo:{state:string;startAt:string|null;validUntil:string|null};
  review:{claimId:string|null;provisionalUntil:string|null};
  nextAction:string;
}
interface Sources {
  revision:number; changed_at:string; seen_snapshot:number; seen_grant:number; seen_claim:number;
  snapshot_json:string|null; snapshot_revision:number|null;
  grant_json:string|null; grant_columns:string|null; claim_json:string|null;
}
export class ProjectionUnavailable extends Error {}
function requireSource(value:unknown): asserts value {
  if(!value) throw new ProjectionUnavailable('Authoritative sources unavailable');
}
function storedSnapshot(raw:string,revision:number|null,now:Date): EntitlementSnapshot {
  const value:unknown = JSON.parse(raw);
  requireSource(validSnapshot(value) && value.revision===revision && Date.parse(value.checkedAt)<=now.getTime());
  return value as EntitlementSnapshot;
}

/** One consistent, indexed D1 SELECT. No GET writes, network calls, cache TTL,
 * re-evaluation of old eligibility, jobs or synthesized paid/promotional rights.
 * The version marker changes atomically with authoritative source writes.
 */
export async function readEntitlement(db:D1Database,project:string,session:Session):Promise<EntitlementSnapshot> {
  requireSource(session.uid===session.claims.sub && session.uid.length>0 && session.uid.length<=128 &&
    session.claims.aud===project && session.claims.iss===`https://securetoken.google.com/${project}`);
  const row = await db.prepare(`SELECT v.revision, v.changed_at, v.seen_snapshot, v.seen_grant, v.seen_claim,
      s.snapshot_json, s.revision AS snapshot_revision, g.grant_json, c.claim_json,
      CASE WHEN g.uid IS NULL THEN NULL ELSE json_object('uid',g.uid,'status',g.status,
        'policyCutoffUtc',g.policy_cutoff_utc,'policyVersion',g.policy_version,'legacyGrantedAt',g.legacy_granted_at,
        'legacyEvidenceType',g.legacy_evidence_type,'evidenceHash',g.evidence_hash,
        'legacyCoreVersionBaseline',g.legacy_core_version_baseline,'migrationVersion',g.migration_version,
        'grantRevision',g.grant_revision,'decisionActor',g.decision_actor) END AS grant_columns
    FROM entitlement_source_versions v
    LEFT JOIN entitlement_snapshots s ON s.firebase_project_id=v.firebase_project_id AND s.uid=v.uid
    LEFT JOIN legacy_grants g ON g.firebase_project_id=v.firebase_project_id AND g.uid=v.uid
    LEFT JOIN legacy_claims c ON c.firebase_project_id=v.firebase_project_id AND c.uid=v.uid
    WHERE v.firebase_project_id=? AND v.uid=?`).bind(project,session.uid).first<Sources>();
  const now = new Date();
  requireSource(row && Number.isSafeInteger(row.revision) && row.revision>=0 &&
    Number.isFinite(Date.parse(row.changed_at)) && Date.parse(row.changed_at)<=now.getTime());
  requireSource(!row.seen_snapshot || row.snapshot_json);
  requireSource(!row.seen_grant || row.grant_json);
  requireSource(!row.seen_claim || row.claim_json || row.grant_json);
  let grant:LegacyGrant|null=null; let claim:LegacyClaim|null=null;
  if(row.grant_json) {
    const value:unknown=JSON.parse(row.grant_json);
    requireSource(validateLegacyGrant(value) && value.uid===session.uid && Date.parse(value.legacyGrantedAt)<=now.getTime());
    const columns:Record<string,unknown>=JSON.parse(row.grant_columns!);
    requireSource(Object.entries(columns).every(([key,v])=>v===(value as unknown as Record<string,unknown>)[key]));
    grant=value;
  }
  if(row.claim_json) {
    const value:unknown=JSON.parse(row.claim_json);
    requireSource(validateLegacyClaim(value) && value.uid===session.uid && Date.parse(value.openedAt)<=now.getTime() &&
      (value.resolvedAt===null || Date.parse(value.resolvedAt)<=now.getTime()));
    claim=value;
    requireSource(value.state!=='GRANTED' || grant);
  }
  let snapshot:EntitlementSnapshot;
  if(row.snapshot_json) {
    snapshot=storedSnapshot(row.snapshot_json,row.snapshot_revision,now);
    // Never interpret a missing permanent source as an explicit revocation.
    requireSource(snapshot.legacy.state!=='GRANTED' || grant);
  } else {
    requireSource(grant); // No grant/base => 503, not a fictitious grant or plan.
    snapshot={contractVersion:'1.0.0',revision:row.revision,serverNow:now.toISOString(),checkedAt:row.changed_at,
      subscriptionRequired:false,legacyCore:false,coreAllowed:true,proAllowed:false,proFeatureSet:[],
      legacy:{state:'UNKNOWN',legacyGrantedAt:null,legacyEvidenceType:null,legacyCoreVersionBaseline:null,migrationVersion:null,grantRevision:null},
      play:{state:'NONE',billingPhase:'NONE',validUntil:null,checkedAt:null,productId:null,basePlanId:null},
      promo:{state:'NONE',startAt:null,validUntil:null},review:{claimId:null,provisionalUntil:null},nextAction:'NONE'};
  }
  if(grant) {
    snapshot.legacy={state:'GRANTED',legacyGrantedAt:grant.legacyGrantedAt,legacyEvidenceType:grant.legacyEvidenceType,
      legacyCoreVersionBaseline:grant.legacyCoreVersionBaseline,migrationVersion:grant.migrationVersion,grantRevision:grant.grantRevision};
    // A resolved review is not an independent provisional grant.
    snapshot.review={claimId:claim?.claimId ?? snapshot.review.claimId,provisionalUntil:null};
  } else if(claim) {
    snapshot.legacy={state:claim.state,legacyGrantedAt:null,legacyEvidenceType:null,
      legacyCoreVersionBaseline:null,migrationVersion:null,grantRevision:null};
    snapshot.review={claimId:claim.claimId,provisionalUntil:claim.state==='UNDER_REVIEW'?claim.provisionalUntil:null};
  }
  if(snapshot.play.checkedAt!==null) requireSource(Date.parse(snapshot.play.checkedAt)<=now.getTime());
  if(snapshot.promo.state!=='NONE') requireSource(Date.parse(snapshot.promo.startAt!)<Date.parse(snapshot.promo.validUntil!));
  const playValid=['ACTIVE','CANCELED_WITH_ACCESS','GRACE'].includes(snapshot.play.state) &&
    snapshot.play.validUntil!==null && Date.parse(snapshot.play.validUntil)>now.getTime();
  const promoValid=snapshot.promo.state==='ACTIVE' && snapshot.promo.startAt!==null && snapshot.promo.validUntil!==null &&
    Date.parse(snapshot.promo.startAt)<=now.getTime() && Date.parse(snapshot.promo.validUntil)>now.getTime();
  const reviewValid=snapshot.legacy.state==='UNDER_REVIEW' && snapshot.review.provisionalUntil!==null &&
    Date.parse(snapshot.review.provisionalUntil)>now.getTime();
  const temporalMismatch=snapshot.proAllowed!==(playValid || promoValid);
  snapshot.legacyCore=snapshot.legacy.state==='GRANTED';
  snapshot.coreAllowed=!snapshot.subscriptionRequired || snapshot.legacyCore || playValid || promoValid || reviewValid;
  snapshot.proAllowed=playValid || promoValid;
  if(!snapshot.proAllowed) snapshot.proFeatureSet=[];
  // State/dates remain the source's observation, even when access just expired.
  // UNKNOWN requests reconciliation without discarding independent Legacy Core.
  if(snapshot.play.state==='UNKNOWN' || snapshot.play.billingPhase==='UNKNOWN' || temporalMismatch) snapshot.nextAction='RETRY';
  else if(!row.snapshot_json || (grant && ['LINK_ACCOUNT','LEGACY_CLAIM','CHOOSE_PLAN'].includes(snapshot.nextAction)))
    snapshot.nextAction='NONE';
  snapshot.revision=row.revision; snapshot.serverNow=now.toISOString(); snapshot.checkedAt=row.changed_at;
  requireSource(validSnapshot(snapshot));
  return snapshot;
}
