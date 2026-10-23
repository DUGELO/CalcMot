import {beforeAll,afterAll,it,expect,vi} from 'vitest';
import {Miniflare} from 'miniflare';
import {readFileSync,readdirSync} from 'node:fs';
import {LegacyReviewService} from '../src/legacy-review';
import {LegacyPrivacyService,type PrivacyMode} from '../src/legacy-privacy';
import {LegacyGrantService} from '../src/legacy';
import type {Session} from '../src/auth';

const mf=new Miniflare({modules:true,script:'export default {fetch(){return new Response("local only")}}',
  compatibilityDate:'2026-07-01',d1Databases:{DB:'privacy-synthetic-only'}});
vi.setConfig({testTimeout:30000});
let db:D1Database;
let review:LegacyReviewService;
let privacy:LegacyPrivacyService;
const date=new Date('2026-01-10T12:00:00Z');
const user=(uid:string,admin=false,now=date):Session=>({uid,claims:{sub:uid,aud:'calcmot',
  iss:'https://securetoken.google.com/calcmot',admin,auth_time:Math.floor(+now/1000)-5,
  firebase:{identities:{'google.com':['synthetic-only']}}}});
const owner=(now=date)=>user('synthetic-owner',true,now);
let id=0;
const key=()=>`privacy-synthetic-${++id}`;
beforeAll(async()=>{
  db=await mf.getD1Database('DB') as unknown as D1Database;
  for(const file of readdirSync('migrations').filter(x=>x.endsWith('.sql')).sort()) {
    const sql=readFileSync(`migrations/${file}`,'utf8').replace(/--[^\n]*/g,'');
    const triggers=[...sql.matchAll(/CREATE TRIGGER[\s\S]*?END;/g)].map(m=>m[0]);
    const parts=sql.replace(/CREATE TRIGGER[\s\S]*?END;/g,'').split(';').map(x=>x.trim()).filter(Boolean);
    if(parts.length) await db.batch(parts.map(part=>db.prepare(part)));
    if(triggers.length) await db.batch(triggers.map(trigger=>db.prepare(trigger)));
  }
  const env={DB:db,FIREBASE_PROJECT_ID:'calcmot',LEGACY_OWNER_UID:'synthetic-owner'};
  review=new LegacyReviewService(env); privacy=new LegacyPrivacyService(env);
  await review.authorizePolicy(owner(),date);
},60000);
afterAll(()=>mf.dispose());
async function issued() {
  const applicant=user(key());
  const c=await review.open(applicant,{evidenceType:'USER_DECLARATION',evidenceHash:null},key(),date);
  await review.addEvidence(owner(),applicant.uid,c.claimId,0,{kind:'AUXILIARY',category:'SUPPORT_RECORD',
    originKey:'synthetic-support',reference:'synthetic-ref-support'},key(),date);
  await review.addEvidence(owner(),applicant.uid,c.claimId,1,{kind:'AUXILIARY',category:'INSTALL_CONTEXT',
    originKey:'synthetic-install',reference:'synthetic-ref-install'},key(),date);
  const inspected=await review.inspect(owner(),applicant.uid,c.claimId,date);
  const approved=await review.decide(owner(),applicant.uid,c.claimId,2,{outcome:'APPROVE',
    evidenceIds:inspected.evidence.map((e:any)=>e.evidence_id),reason:'Synthetic contextualized owner decision.',
    ownershipConfirmed:true,contradictionsCleared:true,legacyUseConcluded:true},key(),date);
  const result=await review.issue(owner(),applicant.uid,c.claimId,3,{approvalEventId:approved.decision.event_id},key(),date);
  await privacy.closeCase(owner(),applicant.uid,date);
  return {applicant,claim:c.claimId,grant:result.grant};
}
async function execute(uid:string,mode:PrivacyMode,now:Date,operationId=key()) {
  const plan=await privacy.plan(owner(now),uid,mode,now);
  return privacy.execute(owner(now),uid,mode,{operationId,fingerprint:plan.fingerprint,
    ownershipConfirmed:true,eraseWithoutAutomaticRecovery:mode==='ERASE_ACCOUNT'},now);
}
it('dry-run is read only and rejects ordinary users, another admin and stale auth',async()=>{
  const c=await issued(); const plan=await privacy.plan(owner(),c.applicant.uid,'EVIDENCE_RETENTION',date);
  expect(plan.due).toBe(false); expect(plan.inventory.reviewEvidence).toBe(2);
  for(const session of [c.applicant,user('other-admin',true),owner(new Date(+date-600000))])
    await expect(privacy.plan(session,c.applicant.uid,'ERASE_ACCOUNT',date)).rejects.toThrow('FORBIDDEN');
  expect((await privacy.plan(owner(),c.applicant.uid,'ERASE_ACCOUNT',date)).inventory.reviewEvidence).toBe(2);
});
it('refuses early retention and direct deletion; removes evidence at 30 days without touching grant',async()=>{
  const c=await issued(); const other=await issued();
  await expect(execute(c.applicant.uid,'EVIDENCE_RETENTION',date)).rejects.toThrow('CONFLICT');
  await expect(db.prepare('DELETE FROM legacy_review_evidence WHERE claim_id=?').bind(c.claim).run()).rejects.toThrow('scoped privacy');
  await execute(c.applicant.uid,'EVIDENCE_RETENTION',new Date(+date+30*86400000));
  const remaining=await privacy.plan(owner(),c.applicant.uid,'ERASE_ACCOUNT',date);
  expect(remaining.inventory.reviewEvidence).toBe(0); expect(remaining.inventory.events).toBe(3);
  const grants=new LegacyGrantService({DB:db,FIREBASE_PROJECT_ID:'calcmot'});
  expect(await grants.getGrant(c.applicant)).toEqual(c.grant);
  expect(await grants.getGrant(other.applicant)).toEqual(other.grant);
  expect((await privacy.plan(owner(),other.applicant.uid,'ERASE_ACCOUNT',date)).inventory.reviewEvidence).toBe(2);
});
it('expires detailed audit after twelve calendar months while permanent grant remains recoverable',async()=>{
  const c=await issued();
  await execute(c.applicant.uid,'EVIDENCE_RETENTION',new Date('2026-02-10T12:00:00Z'));
  await expect(execute(c.applicant.uid,'AUDIT_RETENTION',new Date('2027-01-09T12:00:00Z'))).rejects.toThrow('CONFLICT');
  await execute(c.applicant.uid,'AUDIT_RETENTION',new Date('2027-01-10T12:00:00Z'));
  const result=await privacy.plan(owner(),c.applicant.uid,'ERASE_ACCOUNT',date);
  expect(result.inventory.events).toBe(0); expect(result.inventory.receipts).toBe(0);
  expect(await new LegacyGrantService({DB:db,FIREBASE_PROJECT_ID:'calcmot'}).getGrant(user(c.applicant.uid))).toEqual(c.grant);
});
it('full erasure removes owned copies, leaves no target UID in receipt, and is replay-safe',async()=>{
  const c=await issued(); const operationId=key();
  const plan=await privacy.plan(owner(),c.applicant.uid,'ERASE_ACCOUNT',date);
  await expect(privacy.execute(owner(),c.applicant.uid,'ERASE_ACCOUNT',{operationId,fingerprint:plan.fingerprint,
    ownershipConfirmed:true},date)).rejects.toThrow('INVALID_REQUEST');
  await execute(c.applicant.uid,'ERASE_ACCOUNT',date,operationId);
  const result=await privacy.plan(owner(),c.applicant.uid,'ERASE_ACCOUNT',date);
  for(const [k,v] of Object.entries(result.inventory)) if(!['claimRevision','sourceRevision','grantRevision','resolvedAt','finallyClosedAt','state'].includes(k)) expect(v,k).toBe(0);
  expect((await db.prepare('SELECT target_uid,state FROM legacy_privacy_operations WHERE operation_id=?').bind(operationId).first())?.target_uid).toBeNull();
  expect(await execute(c.applicant.uid,'ERASE_ACCOUNT',date,operationId)).toMatchObject({complete:true,idempotent:true});
});
it('rolls back all deletions and receipt on failure, then permits retry',async()=>{
  const c=await issued(); const operationId=key();
  await db.prepare(`CREATE TRIGGER privacy_test_fail BEFORE DELETE ON legacy_grants WHEN OLD.uid='${c.applicant.uid}'
    BEGIN SELECT RAISE(ABORT,'synthetic privacy failure'); END`).run();
  await expect(execute(c.applicant.uid,'ERASE_ACCOUNT',date,operationId)).rejects.toThrow('synthetic privacy failure');
  expect((await privacy.plan(owner(),c.applicant.uid,'ERASE_ACCOUNT',date)).inventory.reviewEvidence).toBe(2);
  expect(await db.prepare('SELECT 1 FROM legacy_privacy_operations WHERE operation_id=?').bind(operationId).first()).toBeNull();
  await db.prepare('DROP TRIGGER privacy_test_fail').run();
  await execute(c.applicant.uid,'ERASE_ACCOUNT',date,operationId);
});
it('stale dry-run cannot erase a case changed by the reviewer',async()=>{
  const applicant=user(key()); const c=await review.open(applicant,{evidenceType:'USER_DECLARATION',evidenceHash:null},key(),date);
  const plan=await privacy.plan(owner(),applicant.uid,'ERASE_ACCOUNT',date);
  await review.decide(owner(),applicant.uid,c.claimId,0,{outcome:'REQUEST_MORE',evidenceIds:[],reason:'Synthetic supplement.',
    ownershipConfirmed:true,contradictionsCleared:true,legacyUseConcluded:false},key(),date);
  await expect(privacy.execute(owner(),applicant.uid,'ERASE_ACCOUNT',{operationId:key(),fingerprint:plan.fingerprint,
    ownershipConfirmed:true,eraseWithoutAutomaticRecovery:true},date)).rejects.toThrow('CONFLICT');
});
it('retention requires explicit final closure and reopening invalidates that closure',async()=>{
  const applicant=user(key());
  const c=await review.open(applicant,{evidenceType:'USER_DECLARATION',evidenceHash:null},key(),date);
  await review.decide(owner(),applicant.uid,c.claimId,0,{outcome:'REJECT',evidenceIds:[],
    reason:'Synthetic negative case.',ownershipConfirmed:true,contradictionsCleared:true,
    legacyUseConcluded:false},key(),date);
  const later=new Date('2027-02-10T12:00:00Z');
  expect((await privacy.plan(owner(later),applicant.uid,'EVIDENCE_RETENTION',later)).due).toBe(false);
  await privacy.closeCase(owner(),applicant.uid,date);
  expect((await privacy.plan(owner(later),applicant.uid,'EVIDENCE_RETENTION',later)).due).toBe(true);
  await review.decide(owner(),applicant.uid,c.claimId,1,{outcome:'REQUEST_MORE',evidenceIds:[],
    reason:'Synthetic appeal; closure must no longer authorize retention.',ownershipConfirmed:true,
    contradictionsCleared:true,legacyUseConcluded:false},key(),date);
  const plan=await privacy.plan(owner(later),applicant.uid,'EVIDENCE_RETENTION',later);
  expect(plan.due).toBe(false); expect(plan.inventory.finallyClosedAt).toBeNull();
});
