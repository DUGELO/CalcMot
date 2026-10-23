import type {ClaimView,ClaimState,DecisionSummary,EvidenceView,EventView,GrantSummary,Page} from './contracts';
import {AdminFailure} from './security';

export interface ClaimRow {claim_id:string;uid:string;state:ClaimState;opened_at:string;resolved_at:string|null;provisional_until:string|null;revision:number}
export const claimView=(r:ClaimRow):ClaimView=>({claimId:r.claim_id,state:r.state,openedAt:r.opened_at,
  resolvedAt:r.resolved_at,provisionalUntil:r.provisional_until,revision:r.revision,serverNow:new Date().toISOString()});
export const decisionView=(r:any):DecisionSummary=>({eventId:r.event_id,kind:r.kind,basis:r.basis,
  toRevision:r.to_revision,occurredAt:r.occurred_at});
export const grantView=(g:any):GrantSummary=>({legacyCoreVersionBaseline:g.legacyCoreVersionBaseline,
  grantRevision:g.grantRevision,legacyGrantedAt:g.legacyGrantedAt});
function valid(condition:unknown):asserts condition {if(!condition)throw new AdminFailure('INVALID_REQUEST',400)}
export function query(url:URL,allowed:string[]) {
  const seen=new Set<string>();
  for(const [k] of url.searchParams){if(!allowed.includes(k)||seen.has(k))throw new AdminFailure('INVALID_REQUEST',400);seen.add(k)}
}
function pagination(url:URL,scope:string):{limit:number;after:unknown[]|null} {
  const value=url.searchParams.get('limit')??'20';
  valid(/^[1-9][0-9]?$/.test(value)&&+value<=50);
  const token=url.searchParams.get('cursor'); let after:unknown[]|null=null;
  if(token!==null) {
    valid(token.length>0&&token.length<=1024&&/^[A-Za-z0-9_-]+$/.test(token));
    try {
      const parsed=JSON.parse(atob(token.replace(/-/g,'+').replace(/_/g,'/')));
      valid(Array.isArray(parsed)&&parsed.length===2&&parsed[0]===scope&&Array.isArray(parsed[1]));
      after=parsed[1];
    }catch{throw new AdminFailure('INVALID_REQUEST',400)}
  }
  return {limit:+value,after};
}
function page<T>(rows:any[],limit:number,scope:string,map:(r:any)=>T,position:(r:any)=>unknown[]):Page<T> {
  const more=rows.length>limit, selected=rows.slice(0,limit);
  const next=more?btoa(JSON.stringify([scope,position(selected[selected.length-1])])).replace(/=/g,'').replace(/\+/g,'-').replace(/\//g,'_'):null;
  return {items:selected.map(map),nextCursor:next,serverNow:new Date().toISOString()};
}
const identifier=(v:unknown)=>typeof v==='string'&&/^[A-Za-z0-9_.:-]{1,128}$/.test(v);
export function claimId(value:string) {if(!identifier(value))throw new AdminFailure('INVALID_REQUEST',400);return value}

/** Read models only. Caller authorizes before construction/use; all SQL is project-scoped. */
export class AdminReads {
  constructor(private db:D1Database,private project:string,private owner:string){}
  async target(id:string):Promise<ClaimRow> {
    const row=await this.db.prepare(`SELECT claim_id,uid,state,opened_at,resolved_at,provisional_until,revision
      FROM legacy_claims WHERE firebase_project_id=? AND claim_id=?`).bind(this.project,id).first<ClaimRow>();
    if(!row)throw new AdminFailure('NOT_FOUND',404);return row;
  }
  async claims(url:URL) {
    query(url,['limit','cursor','state','protocol']);
    const state=url.searchParams.get('state'), protocol=url.searchParams.get('protocol');
    valid(state===null||['UNDER_REVIEW','GRANTED','REJECTED'].includes(state));
    if(protocol!==null)claimId(protocol);
    const scope=JSON.stringify(['claims',this.project,state,protocol]);
    const {limit,after}=pagination(url,scope);
    if(after)valid(after.length===2&&typeof after[0]==='string'&&after[0].length<=40&&Number.isFinite(Date.parse(after[0]))&&identifier(after[1]));
    const values:unknown[]=[this.project];
    let where='firebase_project_id=?';
    if(state!==null){where+=' AND state=?';values.push(state)}
    if(protocol!==null){where+=' AND claim_id=?';values.push(protocol)}
    if(after){where+=' AND (opened_at,claim_id)<(?,?)';values.push(...after)}
    const rows=await this.db.prepare(`SELECT claim_id,state,opened_at,resolved_at,provisional_until,revision
      FROM legacy_claims WHERE ${where} ORDER BY opened_at DESC,claim_id DESC LIMIT ?`).bind(...values,limit+1).all();
    return page(rows.results,limit,scope,claimView,r=>[r.opened_at,r.claim_id]);
  }
  async counts() {
    const rows=await this.db.prepare('SELECT state,count(*) AS total FROM legacy_claims WHERE firebase_project_id=? GROUP BY state')
      .bind(this.project).all<{state:ClaimState;total:number}>();
    const counts:Record<ClaimState,number>={UNDER_REVIEW:0,GRANTED:0,REJECTED:0};
    for(const row of rows.results)counts[row.state]=row.total;
    return {counts,serverNow:new Date().toISOString()};
  }
  async inspect(row:ClaimRow) {
    const result=await this.db.batch([
      this.db.prepare(`SELECT event_id,kind,basis,to_revision,occurred_at FROM legacy_review_events
        WHERE claim_id=? AND kind IN ('APPROVE','REJECT','REQUEST_MORE','OWNERSHIP_CONFLICT') ORDER BY to_revision DESC LIMIT 1`).bind(row.claim_id),
      this.db.prepare(`SELECT legacy_core_version_baseline,grant_revision,legacy_granted_at FROM legacy_grants
        WHERE firebase_project_id=? AND uid=?`).bind(this.project,row.uid)
    ]);
    const event=result[0].results[0],g=result[1].results[0] as any;
    return {view:claimView(row),latestDecision:event?decisionView(event):null,
      grant:g?{legacyCoreVersionBaseline:g.legacy_core_version_baseline,grantRevision:g.grant_revision,legacyGrantedAt:g.legacy_granted_at}:null};
  }
  async evidence(row:ClaimRow,url:URL) {
    query(url,['limit','cursor']);const scope=JSON.stringify(['evidence',this.project,row.claim_id]);
    const {limit,after}=pagination(url,scope);if(after)valid(after.length===1&&identifier(after[0]));
    const rows=await this.db.prepare(`SELECT evidence_id,kind,category,origin_key,reference,authority_evidence_id,added_at
      FROM legacy_review_evidence WHERE claim_id=? ${after?'AND evidence_id>?':''} ORDER BY evidence_id LIMIT ?`)
      .bind(row.claim_id,...(after??[]),limit+1).all();
    return page<EvidenceView>(rows.results,limit,scope,r=>({evidenceId:r.evidence_id,kind:r.kind,category:r.category,
      originKey:r.origin_key,reference:r.reference,...(r.authority_evidence_id?{authorityEvidenceId:r.authority_evidence_id}:{}),addedAt:r.added_at}),r=>[r.evidence_id]);
  }
  async events(row:ClaimRow,url:URL) {
    query(url,['limit','cursor']);const scope=JSON.stringify(['events',this.project,row.claim_id]);
    const {limit,after}=pagination(url,scope);
    if(after)valid(after.length===2&&Number.isSafeInteger(after[0])&&Number(after[0])>=0&&identifier(after[1]));
    const rows=await this.db.prepare(`SELECT * FROM (
      SELECT event_id,kind,basis,actor_uid,occurred_at,from_revision,to_revision,details_json FROM legacy_review_events WHERE claim_id=?
      UNION ALL SELECT issuance_id,'GRANT_ISSUED',basis,actor_uid,issued_at,approved_revision,completed_revision,'{}'
      FROM legacy_review_issuances WHERE firebase_project_id=? AND claim_id=?)
      ${after?'WHERE (to_revision,event_id)>(?,?)':''} ORDER BY to_revision,event_id LIMIT ?`)
      .bind(row.claim_id,this.project,row.claim_id,...(after??[]),limit+1).all();
    return page<EventView>(rows.results,limit,scope,r=>{
      const raw=JSON.parse(r.details_json), details:EventView['details']={};
      if(typeof raw.reason==='string')details.reason=raw.reason;
      if(Array.isArray(raw.evidenceIds)&&raw.evidenceIds.every((id:unknown)=>typeof id==='string'))details.evidenceIds=raw.evidenceIds;
      for(const key of ['ownershipConfirmed','contradictionsCleared','legacyUseConcluded'] as const)
        if(typeof raw[key]==='boolean')details[key]=raw[key];
      if(typeof raw.evidenceId==='string')details.evidenceId=raw.evidenceId;
      if(typeof raw.grantIssuance==='string')details.grantIssuance=raw.grantIssuance;
      return {eventId:r.event_id,kind:r.kind,basis:r.basis,actor:r.actor_uid===this.owner?'OWNER':'SYSTEM',
        occurredAt:r.occurred_at,fromRevision:r.from_revision,toRevision:r.to_revision,details};
    },r=>[r.to_revision,r.event_id]);
  }
}
