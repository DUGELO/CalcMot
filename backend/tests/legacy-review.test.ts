import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import { Miniflare } from 'miniflare';
import { readFileSync } from 'node:fs';
import { createLocalJWKSet, exportJWK, generateKeyPair, SignJWT } from 'jose';
import { LegacyReviewService, type DecisionInput, type EvidenceInput } from '../src/legacy-review';
import { LegacyGrantService, LEGACY_CORE_BASELINE } from '../src/legacy';
import { verifySession, type Session } from '../src/auth';
import { handleRequest } from '../src/index';
import { validateLegacyClaim, validateLegacyGrant } from '../src/generated/legacy-validator.js';

const mf = new Miniflare({modules: true, script: 'export default {fetch(){return new Response("test")}}',
  compatibilityDate: '2026-07-01', d1Databases: {DB: 's06-ephemeral-only'}});
let db: D1Database;
let review: LegacyReviewService;
let owner: Session;
let signingPair: Awaited<ReturnType<typeof generateKeyPair>>;
let keys: ReturnType<typeof createLocalJWKSet>;
const now = new Date();
let n = 0;
const next = () => `s06-fixture-${++n}`;
function user(uid = next(), linked = true, project = 'calcmot'): Session {
  return {uid, claims: {sub: uid, aud: project, iss: `https://securetoken.google.com/${project}`,
    auth_time: Math.floor(now.getTime() / 1000) - 10,
    firebase: {identities: linked ? {'google.com':['local-google-account']} : {}}}};
}
const input = {evidenceType:'USER_DECLARATION', evidenceHash:null};
const decision = (outcome: DecisionInput['outcome'], evidenceIds: string[] = []): DecisionInput => ({
  outcome, evidenceIds, reason: 'Synthetic operator judgment, no real entitlement.',
  ownershipConfirmed: true, contradictionsCleared: true, legacyUseConcluded: true
});

describe('S06 atomic permanent issuance (local synthetic identities only)',()=>{
  async function ready() {
    const c = await claim();
    const a = await add(c.session,c.view.claimId,0,'support');
    const b = await add(c.session,c.view.claimId,1,'context');
    const approval = await review.decide(owner,c.session.uid,c.view.claimId,2,
      decision('APPROVE',[a.evidenceId,b.evidenceId]),next(),now);
    return {...c,approval};
  }
  const request = (c: Awaited<ReturnType<typeof ready>>) => ({approvalEventId:c.approval.decision.event_id});
  async function receipt(claimId: string) {
    return db.prepare('SELECT * FROM legacy_review_issuances WHERE claim_id = ?').bind(claimId).first();
  }
  it('requires an explicit owner-authorized policy and never seeds it in migrations',async()=>{
    const c = await ready();
    await expect(review.issue(owner,c.session.uid,c.view.claimId,3,request(c),next(),now))
      .rejects.toMatchObject({code:'POLICY_REQUIRED'});
    expect(await receipt(c.view.claimId)).toBeNull();
    await expect(review.authorizePolicy(c.session,now)).rejects.toMatchObject({code:'FORBIDDEN'});
    expect(await review.authorizePolicy(owner,now)).toEqual({policyVersion:'legacy-through-3.5-vc12',coreBaseline:LEGACY_CORE_BASELINE});
    expect(await review.authorizePolicy(owner,now)).toEqual({policyVersion:'legacy-through-3.5-vc12',coreBaseline:LEGACY_CORE_BASELINE});
  });
  it('completes request → assisted approval → permanent source → recovery without fabricated history or Premium',async()=>{
    const c = await ready();
    const historyBefore = await count('legacy_historical_use'); const evidenceBefore = await count('legacy_evidence');
    const result = await review.issue(owner,c.session.uid,c.view.claimId,3,request(c),next(),now);
    expect(result.view.state).toBe('GRANTED'); expect(result.view.revision).toBe(4);
    expect(result.view.provisionalUntil).toBeNull(); expect(validateLegacyGrant(result.grant)).toBe(true);
    expect(result.grant.legacyEvidenceType).toBe('ASSISTED_ASSESSMENT');
    expect(result.grant.legacyCoreVersionBaseline).toBe(LEGACY_CORE_BASELINE); expect(result.grant.policyCutoffUtc).toBeNull();
    expect(result.grant.decisionActor).toBe(owner.uid); expect(result.grant).not.toHaveProperty('proAllowed');
    expect(await count('legacy_historical_use')).toBe(historyBefore); expect(await count('legacy_evidence')).toBe(evidenceBefore);
    expect(await count('entitlement_snapshots')).toBe(0);
    expect((await receipt(c.view.claimId))!.approval_event_id).toBe(c.approval.decision.event_id);
    const recovered = user(c.session.uid); // Fresh session, no original installation state.
    expect((await review.open(recovered,input,next(),now)).state).toBe('GRANTED');
    expect(await new LegacyGrantService({DB:db,FIREBASE_PROJECT_ID:'calcmot'}).getGrant(recovered)).toEqual(result.grant);
    expect((await review.getOwn(recovered,now))!.state).toBe('GRANTED');
    expect(await new LegacyGrantService({DB:db,FIREBASE_PROJECT_ID:'calcmot'}).getGrant(user())).toBeNull();
    const stored = await db.prepare('SELECT claim_json FROM legacy_claims WHERE claim_id = ?').bind(c.view.claimId)
      .first<{claim_json:string}>(); expect(validateLegacyClaim(JSON.parse(stored!.claim_json))).toBe(true);
  });
  it('issues verified-history authority separately without manufacturing or rewriting historical records',async()=>{
    const c = await claim(); const authority = await existingSource(c.session);
    await review.addEvidence(owner,c.session.uid,c.view.claimId,0,{kind:'HISTORY',category:'SUPPORT_RECORD',
      originKey:'trusted-fixture-source',reference:next(),authorityEvidenceId:authority},next(),now);
    const e = (await review.inspect(owner,c.session.uid,c.view.claimId,now)).evidence[0] as any;
    const approval = await review.decide(owner,c.session.uid,c.view.claimId,1,decision('APPROVE',[e.evidence_id]),next(),now);
    const before = await count('legacy_historical_use');
    const result = await review.issue(owner,c.session.uid,c.view.claimId,2,{approvalEventId:approval.decision.event_id},next(),now);
    expect(result.grant.legacyEvidenceType).toBe('VERIFIABLE_HISTORY'); expect(await count('legacy_historical_use')).toBe(before);
  });
  it('rejects unauthorized operator, stale reauthentication, declared ownership and forged approvals',async()=>{
    const c = await ready();
    for (const operator of [c.session,{...owner,claims:{...owner.claims,admin:false}},
      {...owner,claims:{...owner.claims,auth_time:Math.floor(now.getTime()/1000)-301}}])
      await expect(review.issue(operator,c.session.uid,c.view.claimId,3,request(c),next(),now))
        .rejects.toMatchObject({code:'FORBIDDEN'});
    await expect(review.issue(owner,c.session.uid,c.view.claimId,3,{approvalEventId:'fake-approval'},next(),now))
      .rejects.toMatchObject({code:'CONFLICT'});
    await expect(review.issue(owner,c.session.uid,c.view.claimId,3,{...request(c),uid:owner.uid} as any,next(),now))
      .rejects.toMatchObject({code:'INVALID_REQUEST'});
    const other = await claim();
    await expect(review.issue(owner,other.session.uid,c.view.claimId,3,request(c),next(),now))
      .rejects.toMatchObject({code:'NOT_FOUND'});
    await expect(review.issue(owner,other.session.uid,other.view.claimId,1,request(c),next(),now))
      .rejects.toMatchObject({code:'CONFLICT'});
    expect(await receipt(c.view.claimId)).toBeNull();
  });
  it('refuses rejection, complement and conflict as authority to issue',async()=>{
    for (const outcome of ['REJECT','REQUEST_MORE','OWNERSHIP_CONFLICT'] as const) {
      const c = await claim();
      const result = await review.decide(owner,c.session.uid,c.view.claimId,0,decision(outcome),next(),now);
      await expect(review.issue(owner,c.session.uid,c.view.claimId,1,{approvalEventId:result.decision.event_id},next(),now))
        .rejects.toMatchObject({code:'CONFLICT'});
      expect(await receipt(c.view.claimId)).toBeNull();
    }
  });
  it('invalidates approval after new evidence or complement; only the latest favorable revision may issue',async()=>{
    const c = await ready();
    await add(c.session,c.view.claimId,3,'declaration');
    await expect(review.issue(owner,c.session.uid,c.view.claimId,3,request(c),next(),now)).rejects.toMatchObject({code:'CONFLICT'});
    await expect(review.issue(owner,c.session.uid,c.view.claimId,4,request(c),next(),now)).rejects.toMatchObject({code:'CONFLICT'});
    const more = await review.decide(owner,c.session.uid,c.view.claimId,4,decision('REQUEST_MORE'),next(),now);
    await expect(review.issue(owner,c.session.uid,c.view.claimId,5,request(c),next(),now)).rejects.toMatchObject({code:'CONFLICT'});
    const details = JSON.parse(c.approval.decision.details_json);
    const latest = await review.decide(owner,c.session.uid,c.view.claimId,more.view.revision,
      decision('APPROVE',details.evidenceIds),next(),now);
    expect((await review.issue(owner,c.session.uid,c.view.claimId,6,{approvalEventId:latest.decision.event_id},next(),now)).view.state)
      .toBe('GRANTED');
  },15000);
  it('supports audited appeal, a new decision and permanent recovery without erasing rejection',async()=>{
    const c = await claim();
    await review.decide(owner,c.session.uid,c.view.claimId,0,decision('REJECT'),next(),now);
    await review.decide(owner,c.session.uid,c.view.claimId,1,decision('REQUEST_MORE'),next(),now);
    const a = await add(c.session,c.view.claimId,2,'support'); const b = await add(c.session,c.view.claimId,3,'context');
    const approval = await review.decide(owner,c.session.uid,c.view.claimId,4,decision('APPROVE',[a.evidenceId,b.evidenceId]),next(),now);
    const result = await review.issue(owner,c.session.uid,c.view.claimId,5,{approvalEventId:approval.decision.event_id},next(),now);
    expect(result.view.state).toBe('GRANTED');
    const audit = await review.inspect(owner,c.session.uid,c.view.claimId,now);
    expect(audit.events.map((v:any)=>v.kind)).toEqual(['REJECT','REQUEST_MORE','EVIDENCE_ADDED','EVIDENCE_ADDED','APPROVE']);
    expect(audit.issuance).not.toBeNull();
  });
  it('is idempotent under retries/concurrency and rejects replays for another decision',async()=>{
    const c = await ready(); const key = next(); const before = await count('legacy_grants');
    const results = await Promise.all(Array.from({length:5},()=>review.issue(owner,c.session.uid,c.view.claimId,3,request(c),key,now)));
    expect(results.every(r=>r.view.state==='GRANTED')).toBe(true); expect(await count('legacy_grants')).toBe(before+1);
    expect(new Set(results.map(r=>JSON.stringify(r.grant))).size).toBe(1);
    const again = await review.issue(owner,c.session.uid,c.view.claimId,3,request(c),key,new Date(now.getTime()+1000));
    expect(again.idempotent).toBe(true); expect(again.grant).toEqual(results[0].grant);
    await expect(review.issue(owner,c.session.uid,c.view.claimId,3,{approvalEventId:'another'},key,now))
      .rejects.toMatchObject({code:'CONFLICT'});
    await expect(db.prepare('UPDATE legacy_review_issuances SET actor_uid = ? WHERE claim_id = ?')
      .bind('forged',c.view.claimId).run()).rejects.toThrow('issuance audit is append only');
  },15000);
  it('arbitrates issuance racing with a contrary decision using the current revision',async()=>{
    const c = await ready();
    const results = await Promise.allSettled([
      review.issue(owner,c.session.uid,c.view.claimId,3,request(c),next(),now),
      review.decide(owner,c.session.uid,c.view.claimId,3,decision('REJECT'),next(),now)
    ]);
    expect(results.filter(r=>r.status==='fulfilled')).toHaveLength(1);
    const current = await review.getOwn(c.session,now);
    const grant = await new LegacyGrantService({DB:db,FIREBASE_PROJECT_ID:'calcmot'}).getGrant(c.session);
    expect(current!.state==='GRANTED').toBe(grant!==null);
    expect((await receipt(c.view.claimId))!==null).toBe(grant!==null);
  });
  it('rolls back a grant already inserted when completion fails, preserves previous grants, and allows safe retry',async()=>{
    const priorUser = user(); await existingSource(priorUser,true);
    const source = new LegacyGrantService({DB:db,FIREBASE_PROJECT_ID:'calcmot'});
    const preserved = await source.getGrant(priorUser); const c = await ready(); const key = next();
    await db.prepare(`CREATE TRIGGER s06_issue_failure BEFORE UPDATE ON legacy_claims
      WHEN OLD.claim_id = '${c.view.claimId}' AND NEW.state = 'GRANTED'
      BEGIN SELECT RAISE(ABORT, 'synthetic completion failure'); END`).run();
    await expect(review.issue(owner,c.session.uid,c.view.claimId,3,request(c),key,now)).rejects.toThrow('synthetic completion failure');
    expect(await source.getGrant(c.session)).toBeNull(); expect(await receipt(c.view.claimId)).toBeNull();
    expect((await review.getOwn(c.session,now))!.revision).toBe(3);
    expect(await source.getGrant(priorUser)).toEqual(preserved);
    await db.prepare('DROP TRIGGER s06_issue_failure').run();
    expect((await review.issue(owner,c.session.uid,c.view.claimId,3,request(c),key,now)).view.state).toBe('GRANTED');
  });
  it('does not overwrite a source that arrived after approval and never transfers it to a different UID',async()=>{
    const c = await ready(); await existingSource(c.session,true);
    const source = new LegacyGrantService({DB:db,FIREBASE_PROJECT_ID:'calcmot'}); const before = await source.getGrant(c.session);
    await expect(review.issue(owner,c.session.uid,c.view.claimId,3,request(c),next(),now)).rejects.toMatchObject({code:'GRANT_PRESERVED'});
    expect(await source.getGrant(c.session)).toEqual(before); expect(await receipt(c.view.claimId)).toBeNull();
    expect((await review.getOwn(c.session,now))!.state).toBe('GRANTED');
    expect(await source.getGrant(user())).toBeNull();
  });
  it('database guards reject direct self-grants, mismatched authority and post-approval linkage loss',async()=>{
    const c = await ready();
    const badGrant = {uid:c.session.uid,status:'GRANTED',policyCutoffUtc:null,policyVersion:'legacy-through-3.5-vc12',
      legacyGrantedAt:now.toISOString(),legacyEvidenceType:'ASSISTED_ASSESSMENT',evidenceHash:'f'.repeat(64),
      legacyCoreVersionBaseline:LEGACY_CORE_BASELINE,migrationVersion:'s06-owner-assessment-v1',grantRevision:1,decisionActor:owner.uid};
    await expect(db.prepare('INSERT INTO legacy_grants VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)')
      .bind('calcmot',c.session.uid,'GRANTED',null,badGrant.policyVersion,badGrant.legacyGrantedAt,badGrant.legacyEvidenceType,
        badGrant.evidenceHash,LEGACY_CORE_BASELINE,badGrant.migrationVersion,1,owner.uid,JSON.stringify(badGrant)).run())
      .rejects.toThrow('legacy grant requires verified history or authorized review');
    await expect(db.prepare('INSERT INTO legacy_review_issuances VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)')
      .bind(next(),'calcmot',c.session.uid,c.view.claimId,c.approval.decision.event_id,3,4,'ASSISTED_ASSESSMENT','forged-owner',
        now.toISOString(),'a'.repeat(64),'b'.repeat(64),badGrant.evidenceHash,badGrant.policyVersion,JSON.stringify(badGrant)).run())
      .rejects.toThrow(/issuance (requires current authorized owner approval|payload does not match authority)/);
    await db.prepare('UPDATE legacy_claim_identities SET linked_google = 0 WHERE claim_id = ?').bind(c.view.claimId).run();
    await expect(review.issue(owner,c.session.uid,c.view.claimId,3,request(c),next(),now))
      .rejects.toThrow('issuance requires current authorized owner approval');
    expect(await receipt(c.view.claimId)).toBeNull();
  });
});
async function claim(session = user()) {
  return {session, view: await review.open(session, input, next(), now)};
}
async function count(table: string) { return (await db.prepare(`SELECT COUNT(*) AS n FROM ${table}`).first<{n: number}>())!.n; }
async function add(session: Session, claimId: string, revision: number, type: 'support'|'context'|'declaration', origin = next()) {
  const e: EvidenceInput = {
    kind: type === 'declaration' ? 'DECLARATION' : 'AUXILIARY',
    category: type === 'support' ? 'SUPPORT_RECORD' : type === 'context' ? 'USAGE_CONTEXT' : 'USER_STATEMENT',
    originKey: origin, reference: next()
  };
  const view = await review.addEvidence(owner, session.uid, claimId, revision, e, next(), now);
  const inspected = await review.inspect(owner, session.uid, claimId, now);
  const added = inspected.evidence.find((value: any) => value.reference === e.reference) as any;
  return {view, evidenceId: added.evidence_id as string};
}
async function existingSource(session: Session, grant = false) {
  const id = next(); const hash = n.toString(16).padStart(64,'0'); const p = next();
  await db.prepare('INSERT INTO legacy_policies VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)')
    .bind('calcmot', p, null, LEGACY_CORE_BASELINE, 'local-fixture-migration',
      '2026-01-01T00:00:00.000Z', 'test-approval', '["SUPPORT_VERIFIED"]', 'fixture-criteria', 12).run();
  await db.prepare('INSERT INTO legacy_evidence VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)')
    .bind('calcmot', id, session.uid, hash, 'SUPPORT_VERIFIED','VERIFIED', now.toISOString(),
      '2026-01-01T00:00:00.000Z', 'test-verifier').run();
  await db.prepare('INSERT INTO legacy_historical_use VALUES (?, ?, ?, ?, ?, ?, ?)')
    .bind('calcmot', id, 10, 'INDEPENDENT_HISTORICAL_RECORD', 'synthetic-history-only', now.toISOString(), 'test-verifier').run();
  if (grant) expect((await new LegacyGrantService({DB:db,FIREBASE_PROJECT_ID:'calcmot'})
    .recordGrant(session, id, p, now)).granted).toBe(true);
  return id;
}
beforeAll(async () => {
  db = await mf.getD1Database('DB') as unknown as D1Database;
  for (const file of ['0001_entitlement_snapshots.sql','0002_legacy_grants.sql','0003_legacy_authority.sql',
    '0004_legacy_version_35.sql','0005_legacy_review.sql','0006_legacy_review_issuance.sql']) {
    const sql = readFileSync(`migrations/${file}`,'utf8').replace(/--[^\n]*/g,'');
    const triggers = [...sql.matchAll(/CREATE TRIGGER[\s\S]*?END;/g)].map(match=>match[0]);
    for (const part of sql.replace(/CREATE TRIGGER[\s\S]*?END;/g,'').split(';').map(s=>s.trim()).filter(Boolean))
      await db.prepare(part).run();
    for (const trigger of triggers) await db.prepare(trigger).run();
  }
  review = new LegacyReviewService({DB:db,FIREBASE_PROJECT_ID:'calcmot',LEGACY_OWNER_UID:'local-test-owner'});
  const pair = await generateKeyPair('RS256');
  signingPair = pair;
  const jwk = {...await exportJWK(pair.publicKey),kid:'s06-key',alg:'RS256'};
  const seconds = Math.floor(now.getTime()/1000);
  const jwt = await new SignJWT({sub:'local-test-owner',aud:'calcmot',iss:'https://securetoken.google.com/calcmot',
    admin:true,auth_time:seconds-10,iat:seconds-5,exp:seconds+3600})
    .setProtectedHeader({alg:'RS256',kid:'s06-key'}).sign(pair.privateKey);
  keys = createLocalJWKSet({keys:[jwk]});
  owner = await verifySession(new Request('https://local.invalid',{headers:{Authorization:`Bearer ${jwt}`}}),
    'calcmot',keys,now);
});
afterAll(async()=>{await mf.dispose();});

describe('S06 private assisted recovery and review',()=>{
  it('opens a schema-valid case without granting provisional or permanent access',async()=>{
    const before = await count('legacy_grants'); const {session,view} = await claim();
    expect(view.state).toBe('UNDER_REVIEW'); expect(view.provisionalUntil).toBeNull();
    expect(await count('legacy_grants')).toBe(before); expect(await count('entitlement_snapshots')).toBe(0);
    const stored = await db.prepare('SELECT claim_json FROM legacy_claims WHERE uid = ?').bind(session.uid)
      .first<{claim_json:string}>(); expect(validateLegacyClaim(JSON.parse(stored!.claim_json))).toBe(true);
  });
  it('implements the frozen authenticated claim route with bounded input, own UID and idempotency',async()=>{
    const session = user(); const seconds = Math.floor(Date.now()/1000);
    const jwt = await new SignJWT({...session.claims,iat:seconds-5,auth_time:seconds-10,exp:seconds+3600})
      .setProtectedHeader({alg:'RS256',kid:'s06-key'}).sign(signingPair.privateKey);
    const env = {DB:db,FIREBASE_PROJECT_ID:'calcmot',LEGACY_CLAIMS_ENABLED:'true'};
    const request = (body:string = JSON.stringify(input), key:string|undefined = 'api-case') =>
      new Request('https://local.invalid/v1/legacy/claim',{method:'POST',body,
        headers:{Authorization:`Bearer ${jwt}`,...(key === undefined ? {} : {'Idempotency-Key':key})}});
    const a = await handleRequest(request(),env,keys); expect(a.status).toBe(200);
    const view = await a.json() as any;
    expect(Object.keys(view).sort()).toEqual(['claimId','state','openedAt','resolvedAt','provisionalUntil','revision','serverNow'].sort());
    expect(view.state).toBe('UNDER_REVIEW'); expect(view.provisionalUntil).toBeNull();
    expect((await (await handleRequest(request(),env,keys)).json() as any).claimId).toBe(view.claimId);
    expect((await handleRequest(request(JSON.stringify({...input,uid:'forged'}),'forged'),env,keys)).status).toBe(400);
    expect((await handleRequest(request(JSON.stringify(input),''),env,keys)).status).toBe(400);
    expect((await handleRequest(request('x'.repeat(5000),'oversize'),env,keys)).status).toBe(400);
    expect((await handleRequest(request(JSON.stringify({evidenceType:'DIFFERENT',evidenceHash:null})),env,keys)).status).toBe(409);
    expect((await handleRequest(request(),{...env,LEGACY_CLAIMS_ENABLED:undefined},keys)).status).toBe(503);
    expect((await handleRequest(new Request('https://local.invalid/admin/legacy/grant',{method:'POST',body:'{}',
      headers:{Authorization:`Bearer ${jwt}`}}),env,keys)).status).toBe(403);
    const unavailable = {...env, DB:{prepare(){throw new Error('synthetic DB failure');}} as unknown as D1Database};
    expect((await handleRequest(request(),unavailable,keys)).status).toBe(503);
  });
  it('returns the same case for repeated/concurrent requests and rejects key reuse with another body',async()=>{
    const session = user(); const key = next();
    const results = await Promise.all(Array.from({length:5},()=>review.open(session,input,key,now)));
    expect(new Set(results.map(value=>value.claimId)).size).toBe(1);
    expect((await review.open(session,input,next(),now)).claimId).toBe(results[0].claimId);
    await expect(review.open(session,{evidenceType:'OTHER',evidenceHash:null},key,now)).rejects.toMatchObject({code:'CONFLICT'});
  });
  it('does not let a user classify declarations as verified or set identity/provisional/grant fields',async()=>{
    const session = user();
    for (const extra of [{uid:'other'},{provisionalUntil:'2099-01-01T00:00:00Z'},{legacyCore:true},{reviewer:'owner'}])
      await expect(review.open(session,{...input,...extra},next(),now)).rejects.toMatchObject({code:'INVALID_REQUEST'});
    const view = await review.open(session,{evidenceType:'VERIFIED_HISTORY',evidenceHash:'a'.repeat(64)},next(),now);
    await expect(review.decide(owner,session.uid,view.claimId,0,decision('APPROVE'),next(),now))
      .rejects.toMatchObject({code:'MORE_EVIDENCE_REQUIRED'});
  });
  it('requires owner UID plus signed boolean admin and recent authentication, with no email-based privilege',async()=>{
    const {session,view} = await claim();
    for (const operator of [session, {...owner,claims:{...owner.claims,admin:'true'}},
      {...owner,uid:'other',claims:{...owner.claims,sub:'other'}},
      {...owner,claims:{...owner.claims,auth_time:Math.floor(now.getTime()/1000)-301}}])
      await expect(review.inspect(operator,session.uid,view.claimId,now)).rejects.toMatchObject({code:'FORBIDDEN'});
    const unconfigured = new LegacyReviewService({DB:db,FIREBASE_PROJECT_ID:'calcmot'});
    await expect(unconfigured.inspect(owner,session.uid,view.claimId,now)).rejects.toMatchObject({code:'FORBIDDEN'});
  });
  it('isolates users/projects and prevents reviewing mismatched cases',async()=>{
    const a = await claim(); const b = await claim();
    expect((await review.getOwn(a.session,now))!.claimId).toBe(a.view.claimId);
    await expect(review.inspect(owner,a.session.uid,b.view.claimId,now)).rejects.toMatchObject({code:'NOT_FOUND'});
    await expect(review.getOwn({...a.session,uid:b.session.uid},now)).rejects.toMatchObject({code:'FORBIDDEN'});
    const other = new LegacyReviewService({DB:db,FIREBASE_PROJECT_ID:'other-project'});
    expect(await other.getOwn(user(a.session.uid,true,'other-project'),now)).toBeNull();
  });
  it('records a verifiable-history approval without emitting a grant or fabricating history',async()=>{
    const {session,view} = await claim(); const authority = await existingSource(session);
    const added = await review.addEvidence(owner,session.uid,view.claimId,0,{kind:'HISTORY',category:'SUPPORT_RECORD',
      originKey:'authoritative-record',reference:'private-source',authorityEvidenceId:authority},next(),now);
    const inspected = await review.inspect(owner,session.uid,view.claimId,now);
    const id = (inspected.evidence[0] as any).evidence_id;
    const before = await count('legacy_grants');
    const approved = await review.decide(owner,session.uid,view.claimId,added.revision,decision('APPROVE',[id]),next(),now);
    expect(approved.decision.basis).toBe('VERIFIABLE_HISTORY');
    expect(approved.view.state).toBe('UNDER_REVIEW'); expect(approved.view.provisionalUntil).toBeNull();
    expect(await count('legacy_grants')).toBe(before);
  });
  it('accepts an owner-assessed combination without pretending it is cryptographic proof',async()=>{
    const {session,view} = await claim();
    const a = await add(session,view.claimId,0,'support','original-support');
    const b = await add(session,view.claimId,a.view.revision,'context','device-context');
    const historyBefore = await count('legacy_historical_use');
    const result = await review.decide(owner,session.uid,view.claimId,b.view.revision,
      decision('APPROVE',[a.evidenceId,b.evidenceId]),next(),now);
    expect(result.decision.basis).toBe('ASSISTED_ASSESSMENT');
    expect(JSON.parse(result.decision.details_json).grantIssuance).toBe('AWAITING_AUTHORIZED_ISSUANCE');
    expect(await count('legacy_historical_use')).toBe(historyBefore);
    expect(await new LegacyGrantService({DB:db,FIREBASE_PROJECT_ID:'calcmot'}).getGrant(session)).toBeNull();
  });
  it('keeps incomplete/correlated evidence pending and audits a request for more information',async()=>{
    const {session,view} = await claim();
    const a = await add(session,view.claimId,0,'support','same-origin');
    const b = await add(session,view.claimId,1,'context','same-origin');
    await expect(review.decide(owner,session.uid,view.claimId,2,decision('APPROVE',[a.evidenceId,b.evidenceId]),next(),now))
      .rejects.toMatchObject({code:'MORE_EVIDENCE_REQUIRED'});
    const pending = await review.decide(owner,session.uid,view.claimId,2,decision('REQUEST_MORE',[a.evidenceId,b.evidenceId]),next(),now);
    expect(pending.view.state).toBe('UNDER_REVIEW'); expect(pending.decision.kind).toBe('REQUEST_MORE');
  });
  it('allows reviewed original support plus account continuity after all local device evidence was lost',async()=>{
    const {session,view} = await claim();
    const support = await add(session,view.claimId,0,'support','original-server-mail');
    const revision = await review.addEvidence(owner,session.uid,view.claimId,1,{kind:'AUXILIARY',
      category:'ACCOUNT_CONTINUITY',originKey:'reviewed-channel-ownership',reference:'ownership-check'},next(),now);
    const inspected = await review.inspect(owner,session.uid,view.claimId,now);
    const continuity = inspected.evidence.find((item:any)=>item.category==='ACCOUNT_CONTINUITY') as any;
    const result = await review.decide(owner,session.uid,view.claimId,revision.revision,
      decision('APPROVE',[support.evidenceId,continuity.evidence_id]),next(),now);
    expect(result.decision.basis).toBe('ASSISTED_ASSESSMENT');
  });
  it('rejects fabricated history references and cross-owner evidence',async()=>{
    const a = await claim(); const b = await claim(); const otherSource = await existingSource(b.session);
    await expect(review.addEvidence(owner,a.session.uid,a.view.claimId,0,{kind:'HISTORY',category:'SUPPORT_RECORD',
      originKey:'claimed',reference:'fake',authorityEvidenceId:otherSource},next(),now))
      .rejects.toMatchObject({code:'MORE_EVIDENCE_REQUIRED'});
    const evidence = await add(b.session,b.view.claimId,0,'support');
    await expect(review.decide(owner,a.session.uid,a.view.claimId,0,decision('APPROVE',[evidence.evidenceId]),next(),now))
      .rejects.toMatchObject({code:'NOT_FOUND'});
  });
  it('audits rejection once and protects audit records from rewriting',async()=>{
    const {session,view} = await claim(); const key = next(); const request = decision('REJECT');
    const first = await review.decide(owner,session.uid,view.claimId,0,request,key,now);
    const again = await review.decide(owner,session.uid,view.claimId,0,request,key,now);
    expect(first.view.state).toBe('REJECTED'); expect(first.decision.actor_uid).toBe(owner.uid);
    expect(again.decision.event_id).toBe(first.decision.event_id);
    await expect(db.prepare('UPDATE legacy_review_events SET kind = ? WHERE event_id = ?')
      .bind('APPROVE',first.decision.event_id).run()).rejects.toThrow('review audit is append only');
    await expect(review.decide(owner,session.uid,view.claimId,0,decision('APPROVE'),key,now))
      .rejects.toMatchObject({code:'CONFLICT'});
  });
  it('serializes competing decisions and commits state and audit together',async()=>{
    const {session,view} = await claim();
    const results = await Promise.allSettled([
      review.decide(owner,session.uid,view.claimId,0,decision('REJECT'),next(),now),
      review.decide(owner,session.uid,view.claimId,0,decision('REQUEST_MORE'),next(),now)
    ]);
    expect(results.filter(value=>value.status==='fulfilled')).toHaveLength(1);
    const inspected = await review.inspect(owner,session.uid,view.claimId,now);
    expect(inspected.events).toHaveLength(1); expect(inspected.view.revision).toBe(1);
  });
  it('allows only an audited owner appeal to reopen a rejected case without erasing its history',async()=>{
    const {session,view} = await claim();
    await review.decide(owner,session.uid,view.claimId,0,decision('REJECT'),next(),now);
    await expect(db.prepare("UPDATE legacy_claims SET state = 'UNDER_REVIEW', revision = revision + 1 WHERE claim_id = ?")
      .bind(view.claimId).run()).rejects.toThrow('resolved legacy claim requires audited appeal');
    const reopened = await review.decide(owner,session.uid,view.claimId,1,decision('REQUEST_MORE'),next(),now);
    expect(reopened.view.state).toBe('UNDER_REVIEW'); expect(reopened.view.revision).toBe(2);
    expect((await review.inspect(owner,session.uid,view.claimId,now)).events).toHaveLength(2);
    const a = await add(session,view.claimId,2,'support'); const b = await add(session,view.claimId,3,'context');
    const approval = await review.decide(owner,session.uid,view.claimId,4,decision('APPROVE',[a.evidenceId,b.evidenceId]),next(),now);
    expect(approval.decision.basis).toBe('ASSISTED_ASSESSMENT');
  });
  it('records ownership conflict without transferring an existing grant',async()=>{
    const original = user(); await existingSource(original,true);
    const {session,view} = await claim();
    const result = await review.decide(owner,session.uid,view.claimId,0,decision('OWNERSHIP_CONFLICT'),next(),now);
    expect(result.view.state).toBe('UNDER_REVIEW');
    expect(await new LegacyGrantService({DB:db,FIREBASE_PROJECT_ID:'calcmot'}).getGrant(session)).toBeNull();
    expect(await new LegacyGrantService({DB:db,FIREBASE_PROJECT_ID:'calcmot'}).getGrant(original)).not.toBeNull();
  });
  it('restores the same UID after reinstallation and never rejudges or removes an existing grant',async()=>{
    const session = user(); await existingSource(session,true);
    const before = await new LegacyGrantService({DB:db,FIREBASE_PROJECT_ID:'calcmot'}).getGrant(session);
    const view = await review.open(user(session.uid),input,next(),now);
    expect(view.state).toBe('GRANTED');
    expect((await review.open(user(session.uid),input,next(),now)).claimId).toBe(view.claimId);
    await expect(review.decide(owner,session.uid,view.claimId,0,decision('REJECT'),next(),now))
      .rejects.toMatchObject({code:'CONFLICT'});
    expect(await new LegacyGrantService({DB:db,FIREBASE_PROJECT_ID:'calcmot'}).getGrant(session)).toEqual(before);
  });
  it('restores an existing grant that appeared after a case was rejected, preserving both audit entries',async()=>{
    const {session,view} = await claim();
    await review.decide(owner,session.uid,view.claimId,0,decision('REJECT'),next(),now);
    await existingSource(session,true); // Synthetic authoritative source; no real grant.
    const restored = await review.getOwn(user(session.uid),now);
    expect(restored!.state).toBe('GRANTED'); expect(restored!.revision).toBe(2);
    const audited = await review.inspect(owner,session.uid,view.claimId,now);
    expect(audited.events.map((event:any)=>event.kind)).toEqual(['REJECT','RESTORE']);
    expect((audited.events[1] as any).basis).toBe('EXISTING_GRANT');
  });
  it('keeps an unlinked case pending without granting and leaves Core independent of failure',async()=>{
    const {session,view} = await claim(user(undefined,false));
    const a = await add(session,view.claimId,0,'support'); const b = await add(session,view.claimId,1,'context');
    await expect(review.decide(owner,session.uid,view.claimId,2,decision('APPROVE',[a.evidenceId,b.evidenceId]),next(),now))
      .rejects.toMatchObject({code:'LINK_REQUIRED'});
    // The Android offer path still has no consumer of the review or Auth API.
    const runtime = readFileSync('../app/src/main/java/br/com/calcmot/ReadingPipelineRuntime.kt','utf8');
    expect(runtime).not.toContain('legacy-review'); expect(runtime).not.toContain('LegacyReview');
  });
  it('rolls back the audit when the database fails during a decision',async()=>{
    const {session,view} = await claim();
    await db.prepare(`CREATE TRIGGER s06_test_failure BEFORE UPDATE ON legacy_claims
      WHEN OLD.claim_id = '${view.claimId}' BEGIN SELECT RAISE(ABORT, 'synthetic DB failure'); END`).run();
    await expect(review.decide(owner,session.uid,view.claimId,0,decision('REJECT'),next(),now)).rejects.toThrow('synthetic DB failure');
    const inspected = await review.inspect(owner,session.uid,view.claimId,now);
    expect(inspected.events).toHaveLength(0); expect(inspected.view.state).toBe('UNDER_REVIEW');
    await db.prepare('DROP TRIGGER s06_test_failure').run();
  });
});
