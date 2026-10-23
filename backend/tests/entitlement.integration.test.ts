import {afterAll,beforeAll,describe,expect,it} from 'vitest';
import {Miniflare} from 'miniflare';
import {mkdirSync,mkdtempSync,readFileSync,writeFileSync} from 'node:fs';
import path from 'node:path';
import {createLocalJWKSet,exportJWK,generateKeyPair,SignJWT} from 'jose';
import {handleRequest,type Env} from '../src/index';
import {verifySession,type Session} from '../src/auth';
import {LegacyGrantService,LEGACY_CORE_BASELINE} from '../src/legacy';
import {LegacyReviewService} from '../src/legacy-review';
import {readEntitlement,type EntitlementSnapshot} from '../src/entitlement';
import validSnapshot from '../src/generated/entitlement-validator.js';
import {build} from 'esbuild';

// All identities, policies and rights are synthetic in an isolated persisted
// LOCAL D1. Test keys never replace Google keys in a deployed Worker/console.
mkdirSync('.tmp',{recursive:true});
const state=mkdtempSync(path.resolve('.tmp/s07-d1-'));
const runtime=()=>new Miniflare({modules:true,script:'export default {fetch(){return new Response("test")}}',
  compatibilityDate:'2026-07-01',cf:false,d1Databases:{DB:'s07-local-fixtures-only'},d1Persist:state});
let mf=runtime(); let db:D1Database; let env:Env;
let pair:Awaited<ReturnType<typeof generateKeyPair>>; let keys:ReturnType<typeof createLocalJWKSet>;
let serial=0; const id=()=>`s07-fixture-${++serial}`;
const baseFixture=JSON.parse(readFileSync('../docs/contracts/test-matrix.md','utf8').match(/```json\r?\n([\s\S]*?)```/)![1])
  .baseFixtures.snapshot_blocked.instance;
const jwt=async(uid:string,project='calcmot',admin=false)=>{
  const t=Math.floor(Date.now()/1000);
  return new SignJWT({sub:uid,aud:project,iss:`https://securetoken.google.com/${project}`,iat:t-5,auth_time:t-10,exp:t+3600,
    admin,firebase:{identities:{'google.com':['synthetic-only']}}}).setProtectedHeader({alg:'RS256',kid:'s07'}).sign(pair.privateKey);
};
async function identity(uid=id(),project='calcmot',admin=false):Promise<Session> {
  return verifySession(new Request('https://test.invalid',{headers:{Authorization:`Bearer ${await jwt(uid,project,admin)}`}}),project,keys);
}
async function get(session:Session,query='',method='GET') {
  return handleRequest(new Request(`https://test.invalid/v1/me/entitlement${query}`,{method,
    headers:{Authorization:`Bearer ${await jwt(session.uid,session.claims.aud as string)}`},
    ...(method==='GET'?{}:{body:'{"legacyCore":true,"proAllowed":true}'})}),env,keys);
}
function base(revision=0):EntitlementSnapshot {
  return {...structuredClone(baseFixture),revision,checkedAt:'2026-01-01T00:00:00.000Z',serverNow:'2026-01-01T00:00:00.000Z'};
}
async function put(session:Session,snapshot=base()) {
  await db.prepare(`INSERT INTO entitlement_snapshots VALUES (?, ?, ?, ?)
    ON CONFLICT(firebase_project_id,uid) DO UPDATE SET revision=excluded.revision,snapshot_json=excluded.snapshot_json`)
    .bind(session.claims.aud as string,session.uid,snapshot.revision,JSON.stringify(snapshot)).run();
}
async function source(session:Session,issue=true) {
  const evidence=id(); const policy=id(); const hash=serial.toString(16).padStart(64,'0'); const now=new Date();
  const project=session.claims.aud as string;
  await db.prepare('INSERT INTO legacy_policies VALUES (?, ?, NULL, ?, ?, ?, ?, ?, ?, 12)')
    .bind(project,policy,LEGACY_CORE_BASELINE,'s07-fixture-migration','2026-01-01T00:00:00.000Z',
      'synthetic-verifier','["SUPPORT_VERIFIED"]','synthetic-history-policy').run();
  await db.prepare('INSERT INTO legacy_evidence VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)')
    .bind(project,evidence,session.uid,hash,'SUPPORT_VERIFIED','VERIFIED',now.toISOString(),
      '2026-01-01T00:00:00.000Z','synthetic-verifier').run();
  await db.prepare('INSERT INTO legacy_historical_use VALUES (?, ?, 12, ?, ?, ?, ?)')
    .bind(project,evidence,'INDEPENDENT_HISTORICAL_RECORD','synthetic-history-only',now.toISOString(),'synthetic-verifier').run();
  const grantService=new LegacyGrantService({DB:db,FIREBASE_PROJECT_ID:project});
  if(issue) expect((await grantService.recordGrant(session,evidence,policy,new Date())).granted).toBe(true);
  return {grantService,evidence,policy};
}
async function version(session:Session) {
  return db.prepare('SELECT * FROM entitlement_source_versions WHERE firebase_project_id=? AND uid=?')
    .bind(session.claims.aud as string,session.uid).first<{revision:number}>();
}
async function apply(file:string) {
  const sql=readFileSync(`migrations/${file}`,'utf8').replace(/--[^\n]*/g,'');
  const triggers=[...sql.matchAll(/CREATE TRIGGER[\s\S]*?END;/g)].map(m=>m[0]);
  for(const s of sql.replace(/CREATE TRIGGER[\s\S]*?END;/g,'').split(';').map(s=>s.trim()).filter(Boolean)) await db.prepare(s).run();
  for(const trigger of triggers) await db.prepare(trigger).run();
}
let beforeUpgrade:Session; let oldGrant:unknown; let oldSnapshot:Session;
beforeAll(async()=>{
  db=await mf.getD1Database('DB') as unknown as D1Database;
  for(const file of ['0001_entitlement_snapshots.sql','0002_legacy_grants.sql','0003_legacy_authority.sql',
    '0004_legacy_version_35.sql','0005_legacy_review.sql','0006_legacy_review_issuance.sql']) await apply(file);
  pair=await generateKeyPair('RS256');keys=createLocalJWKSet({keys:[{...await exportJWK(pair.publicKey),kid:'s07',alg:'RS256'}]});
  beforeUpgrade=await identity(); const s=await source(beforeUpgrade); oldGrant=await s.grantService.getGrant(beforeUpgrade);
  oldSnapshot=await identity();await put(oldSnapshot,base(41));
  await apply('0007_entitlement_source_versions.sql');
  env={DB:db,FIREBASE_PROJECT_ID:'calcmot',ENTITLEMENT_PROJECTION_ENABLED:'true'};
},20000);
afterAll(async()=>{await mf.dispose();});

describe('S07 real local D1 source projection and authenticated HTTP',()=>{
  it('backfills revisions only and projects a pre-migration grant without changing its source',async()=>{
    const result=await get(beforeUpgrade);expect(result.status).toBe(200);
    const body=await result.json() as EntitlementSnapshot;
    expect(validSnapshot(body)).toBe(true);expect(body.legacyCore).toBe(true);expect(body.coreAllowed).toBe(true);
    expect(body.proAllowed).toBe(false);expect(body.proFeatureSet).toEqual([]);expect(body.legacy.state).toBe('GRANTED');
    expect(await new LegacyGrantService(env).getGrant(beforeUpgrade)).toEqual(oldGrant);
    expect((await version(oldSnapshot))!.revision).toBeGreaterThan(41);
    expect(result.headers.get('Cache-Control')).toBe('no-store');
    expect(JSON.stringify(body)).not.toContain(beforeUpgrade.uid);expect(JSON.stringify(body)).not.toContain('evidenceHash');
  });
  it('does not manufacture a grant or enable new subscriptions for missing sources',async()=>{
    const user=await identity();expect((await get(user)).status).toBe(503);expect(await version(user)).toBeNull();
    await put(user);const result=await get(user);const body=await result.json() as EntitlementSnapshot;
    expect(result.status).toBe(200);expect(body.legacyCore).toBe(false);expect(body.proAllowed).toBe(false);expect(body.coreAllowed).toBe(false);
    expect(await new LegacyGrantService(env).getGrant(user)).toBeNull();
    const bridge={...base(1),subscriptionRequired:false,coreAllowed:true,nextAction:'NONE'};
    await put(user,bridge);expect((await (await get(user)).json() as EntitlementSnapshot).coreAllowed).toBe(true);
  });
  it('uses one SELECT and no writes; repeated/concurrent GETs keep the same revision',async()=>{
    const user=await identity();await source(user);const before=await version(user);
    let prepares=0;let query='';const counted={prepare(sql:string){prepares++;query=sql;expect(sql.trimStart().startsWith('SELECT')).toBe(true);return db.prepare(sql);}} as D1Database;
    await readEntitlement(counted,'calcmot',user);expect(prepares).toBe(1);
    const plan=await db.prepare(`EXPLAIN QUERY PLAN ${query}`).bind('calcmot',user.uid).all<{detail:string}>();
    expect(plan.results.filter(p=>p.detail.includes('SEARCH'))).toHaveLength(4);
    expect(plan.results.some(p=>p.detail.includes('SCAN'))).toBe(false);
    await expect(db.prepare('UPDATE entitlement_source_versions SET revision=revision-1 WHERE uid=?').bind(user.uid).run())
      .rejects.toThrow('entitlement source revision conflict');
    const responses=await Promise.all(Array.from({length:4},()=>get(user)));
    const bodies=await Promise.all(responses.map(r=>r.json())) as EntitlementSnapshot[];
    expect(new Set(bodies.map(b=>b.revision)).size).toBe(1);expect(await version(user)).toEqual(before);
  });
  it.each(['ACTIVE','CANCELED_WITH_ACCESS','GRACE','ON_HOLD','PAUSED','PENDING','EXPIRED','REVOKED','UNKNOWN'])
    ('normalizes %s without inventing payment and preserves independent legacy',async(state)=>{
      const user=await identity();const value=base();const active=['ACTIVE','CANCELED_WITH_ACCESS','GRACE'].includes(state);
      value.play={state,billingPhase:state==='UNKNOWN'?'UNKNOWN':'PAID_PERIOD',validUntil:'2099-01-01T00:00:00Z',
        checkedAt:'2026-01-01T00:00:00Z',productId:'synthetic-product',basePlanId:'synthetic-plan'};
      value.coreAllowed=active;value.proAllowed=active;value.proFeatureSet=active?['PRO_INSIGHTS_WEEKLY']:[];
      await put(user,value);const initial=await readEntitlement(db,'calcmot',user);
      expect(initial.coreAllowed).toBe(active);expect(initial.proAllowed).toBe(active);
      await source(user);const projected=await readEntitlement(db,'calcmot',user);
      expect(projected.coreAllowed).toBe(true);expect(projected.proAllowed).toBe(active);expect(projected.play).toEqual(value.play);
      expect(projected.proFeatureSet).toEqual(value.proFeatureSet);if(state==='UNKNOWN') expect(projected.nextAction).toBe('RETRY');
    });
  it('expires temporal access without rewriting financial records or revoking Core legacy',async()=>{
    const user=await identity();const value=base();
    value.play={state:'ACTIVE',billingPhase:'TRIAL',validUntil:'2026-01-02T00:00:00Z',checkedAt:'2026-01-01T00:00:00Z',
      productId:'synthetic-trial',basePlanId:'synthetic-plan'};
    value.promo={state:'ACTIVE',startAt:'2026-01-01T00:00:00Z',validUntil:'2026-01-02T00:00:00Z'};
    value.coreAllowed=true;value.proAllowed=true;value.proFeatureSet=['PRO_INSIGHTS_WEEKLY'];
    await put(user,value);const initial=await readEntitlement(db,'calcmot',user);expect(initial.coreAllowed).toBe(false);
    expect(initial.proAllowed).toBe(false);expect(initial.nextAction).toBe('RETRY');
    await source(user);const body=await readEntitlement(db,'calcmot',user);
    expect(body.legacyCore).toBe(true);expect(body.proAllowed).toBe(false);expect(body.proFeatureSet).toEqual([]);
    const raw=await db.prepare('SELECT snapshot_json FROM entitlement_snapshots WHERE uid=?').bind(user.uid).first<{snapshot_json:string}>();
    expect(JSON.parse(raw!.snapshot_json)).toEqual(value);
  });
  it('does not start future promo early and accepts an existing finite review only while valid',async()=>{
    const user=await identity();const value=base();value.promo={state:'ACTIVE',startAt:'2098-01-01T00:00:00Z',validUntil:'2099-01-01T00:00:00Z'};
    await put(user,value);expect((await readEntitlement(db,'calcmot',user)).proAllowed).toBe(false);
    value.revision=1;value.legacy.state='UNDER_REVIEW';value.review={claimId:'synthetic-review',provisionalUntil:'2099-01-01T00:00:00Z'};
    value.coreAllowed=true;await put(user,value);expect((await readEntitlement(db,'calcmot',user)).coreAllowed).toBe(true);
    value.revision=2;value.review.provisionalUntil='2026-01-01T00:00:00Z';value.coreAllowed=false;await put(user,value);
    expect((await readEntitlement(db,'calcmot',user)).coreAllowed).toBe(false);
    const review=new LegacyReviewService(env);await review.open(user,{evidenceType:'USER_DECLARATION',evidenceHash:null},id());
    const body=await readEntitlement(db,'calcmot',user);expect(body.legacy.state).toBe('UNDER_REVIEW');expect(body.review.provisionalUntil).toBeNull();
  });
  it('merges a concurrent financial/promo update with grant arrival without any snapshot overwrite',async()=>{
    const user=await identity();const prepared=await source(user,false);const value=base(10);
    value.play={state:'ACTIVE',billingPhase:'PAID_PERIOD',validUntil:'2099-01-01T00:00:00Z',checkedAt:'2026-01-01T00:00:00Z',
      productId:'synthetic-paid',basePlanId:'synthetic-plan'};value.coreAllowed=true;value.proAllowed=true;await put(user,value);
    const next={...value,revision:11,promo:{state:'ACTIVE',startAt:'2026-01-01T00:00:00Z',validUntil:'2099-01-01T00:00:00Z'}};
    const before=(await version(user))!.revision;
    await Promise.all([put(user,next),prepared.grantService.recordGrant(user,prepared.evidence,prepared.policy)]);
    const body=await readEntitlement(db,'calcmot',user);expect(body.legacyCore).toBe(true);expect(body.proAllowed).toBe(true);
    expect(body.play).toEqual(next.play);expect(body.promo).toEqual(next.promo);expect(body.revision).toBeGreaterThan(before);
    const stored=await db.prepare('SELECT snapshot_json FROM entitlement_snapshots WHERE uid=?').bind(user.uid).first<{snapshot_json:string}>();
    expect(JSON.parse(stored!.snapshot_json)).toEqual(next);
  });
  it('rolls back source and version when a version write fails, then safely retries',async()=>{
    const user=await identity();const p=await source(user,false);
    await db.prepare(`CREATE TRIGGER s07_fail BEFORE INSERT ON entitlement_source_versions WHEN NEW.uid='${user.uid}'
      BEGIN SELECT RAISE(ABORT,'synthetic version failure'); END`).run();
    await expect(p.grantService.recordGrant(user,p.evidence,p.policy)).rejects.toThrow('synthetic version failure');
    expect(await p.grantService.getGrant(user)).toBeNull();expect(await version(user)).toBeNull();expect((await get(user)).status).toBe(503);
    await db.prepare('DROP TRIGGER s07_fail').run();await p.grantService.recordGrant(user,p.evidence,p.policy);
    expect((await get(user)).status).toBe(200);const v=await version(user);
    await p.grantService.recordGrant(user,p.evidence,p.policy);expect(await version(user)).toEqual(v);
  });
  it('returns retryable S01 errors on corrupt/missing sources or outages, without confirming partial state',async()=>{
    const user=await identity();await put(user,{...base(),legacyCore:true} as EntitlementSnapshot);
    expect((await get(user)).status).toBe(503);
    const lost=await identity();await put(lost);await source(lost);
    await db.prepare('DELETE FROM entitlement_snapshots WHERE uid=?').bind(lost.uid).run(); // Synthetic data-loss simulation only.
    const result=await get(lost);expect(result.status).toBe(503);const error=await result.json() as any;
    expect(error.code).toBe('RETRY_LATER');expect(error.retryable).toBe(true);
    expect(Object.keys(error).sort()).toEqual(['code','requestId','serverNow','retryable'].sort());
    expect(await new LegacyGrantService(env).getGrant(lost)).not.toBeNull();
    const unavailable={...env,DB:{prepare(){throw new Error('private failure');}} as unknown as D1Database};
    const failed=await handleRequest(new Request('https://test.invalid/v1/me/entitlement',
      {headers:{Authorization:`Bearer ${await jwt(lost.uid)}`}}),unavailable,keys);
    expect(failed.status).toBe(503);expect(await failed.text()).not.toContain('private failure');
  });
  it('projects an assisted S06 source and rolls back its revision when issuance completion fails',async()=>{
    const user=await identity();const owner=await identity('s07-owner','calcmot',true);
    const review=new LegacyReviewService({...env,LEGACY_OWNER_UID:owner.uid});await review.authorizePolicy(owner);
    const claim=await review.open(user,{evidenceType:'USER_DECLARATION',evidenceHash:null},id());
    for(const [revision,category,originKey] of [[0,'SUPPORT_RECORD','historical-support'],[1,'ACCOUNT_CONTINUITY','channel-check']] as const)
      await review.addEvidence(owner,user.uid,claim.claimId,revision,{kind:'AUXILIARY',category,originKey,reference:id()},id());
    const evidence=(await review.inspect(owner,user.uid,claim.claimId)).evidence.map((e:any)=>e.evidence_id);
    const approval=await review.decide(owner,user.uid,claim.claimId,2,{outcome:'APPROVE',evidenceIds:evidence,
      reason:'Synthetic assisted legacy review only',ownershipConfirmed:true,contradictionsCleared:true,legacyUseConcluded:true},id());
    const prior=await version(user);const key=id();
    await db.prepare(`CREATE TRIGGER s07_late_fail BEFORE UPDATE ON legacy_claims WHEN OLD.uid='${user.uid}' AND NEW.state='GRANTED'
      BEGIN SELECT RAISE(ABORT,'synthetic late failure'); END`).run();
    await expect(review.issue(owner,user.uid,claim.claimId,approval.view.revision,{approvalEventId:approval.decision.event_id},key))
      .rejects.toThrow('synthetic late failure');
    expect(await version(user)).toEqual(prior);expect(await new LegacyGrantService(env).getGrant(user)).toBeNull();
    expect((await get(user)).status).toBe(503);
    await db.prepare('DROP TRIGGER s07_late_fail').run();
    await review.issue(owner,user.uid,claim.claimId,approval.view.revision,{approvalEventId:approval.decision.event_id},key);
    const result=await get(user);expect(result.status).toBe(200);const body=await result.json() as EntitlementSnapshot;
    expect(body.legacyCore).toBe(true);expect(body.legacy.legacyEvidenceType).toBe('ASSISTED_ASSESSMENT');
    expect(body.proAllowed).toBe(false);expect(body.review.provisionalUntil).toBeNull();expect(body.revision).toBeGreaterThan(prior!.revision);
  },15000);
  it('isolates UID/project, ignores forged query UID and rejects Android snapshot writes',async()=>{
    const a=await identity();await source(a);const b=await identity();
    expect((await get(b,`?uid=${a.uid}`)).status).toBe(503);expect((await get(a,`?uid=${b.uid}`)).status).toBe(200);
    const otherProject=await identity(a.uid,'other-project');await source(otherProject);
    await expect(readEntitlement(db,'calcmot',otherProject)).rejects.toThrow();
    expect((await readEntitlement(db,'other-project',otherProject)).legacyCore).toBe(true);
    expect((await get(a,'','POST')).status).toBe(403);
    expect((await handleRequest(new Request('https://test.invalid/v1/me/entitlement'),env,keys)).status).toBe(401);
  });
  it('recovers the same source/revision after restarting D1 with a fresh signed identity',async()=>{
    const user=await identity();await source(user);const first=await readEntitlement(db,'calcmot',user);
    await mf.dispose();mf=runtime();db=await mf.getD1Database('DB') as unknown as D1Database;env={...env,DB:db};
    const fresh=await identity(user.uid);const result=await get(fresh);const body=await result.json() as EntitlementSnapshot;
    expect(result.status).toBe(200);expect(body.revision).toBe(first.revision);expect(body.legacy).toEqual(first.legacy);
    expect(body.coreAllowed).toBe(true);expect(body.proAllowed).toBe(false);
    // Exercise the compiled Worker inside workerd, not just handleRequest in
    // Node. Public synthetic key injection exists in this test entry only.
    const publicKey={...await exportJWK(pair.publicKey),kid:'s07',alg:'RS256'};
    const compiled=await build({stdin:{contents:`import {handleRequest} from './src/index.ts';
      import {createLocalJWKSet} from 'jose';const keys=createLocalJWKSet({keys:[${JSON.stringify(publicKey)}]});
      export default {fetch:(request,env)=>handleRequest(request,env,keys)};`,resolveDir:process.cwd(),loader:'ts'},
      absWorkingDir:process.cwd(),tsconfigRaw:{},bundle:true,platform:'browser',format:'esm',write:false,logLevel:'silent'});
    await mf.dispose();
    mf=new Miniflare({modules:true,script:new TextDecoder().decode(compiled.outputFiles[0].contents),cf:false,
      compatibilityDate:'2026-07-01',bindings:{FIREBASE_PROJECT_ID:'calcmot',ENTITLEMENT_PROJECTION_ENABLED:'true'},
      d1Databases:{DB:'s07-local-fixtures-only'},d1Persist:state});
    const workerResponse=await mf.dispatchFetch('https://test.invalid/v1/me/entitlement',
      {headers:{Authorization:`Bearer ${await jwt(user.uid)}`}});
    expect(workerResponse.status).toBe(200);const workerBody=await workerResponse.json() as EntitlementSnapshot;
    expect(validSnapshot(workerBody)).toBe(true);expect(workerBody.legacy).toEqual(body.legacy);
    expect(workerBody.revision).toBe(body.revision);expect(workerBody.proAllowed).toBe(false);
    writeFileSync(path.resolve('../.tmp/s07-evidence.json'),JSON.stringify({scope:'persisted DEV LOCAL, synthetic signed identities',
      httpStatus:result.status,coreAllowed:body.coreAllowed,legacyCore:body.legacyCore,proAllowed:body.proAllowed,
      schemaValid:validSnapshot(body),stableRevisionAfterRestart:true,getsWrite:false,queryCount:1,realGoogleOperator:false,
      compiledWorkerHttpStatus:workerResponse.status,compiledWorkerSchemaValid:validSnapshot(workerBody)},null,2));
  },15000);
});
