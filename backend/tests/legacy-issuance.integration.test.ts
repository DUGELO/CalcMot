import {expect, it} from 'vitest';
import {Miniflare} from 'miniflare';
import {mkdirSync, mkdtempSync, readFileSync, writeFileSync} from 'node:fs';
import path from 'node:path';
import {createLocalJWKSet, exportJWK, generateKeyPair, SignJWT} from 'jose';
import {LegacyReviewService} from '../src/legacy-review';
import {LegacyGrantService, LEGACY_CORE_BASELINE} from '../src/legacy';
import {verifySession} from '../src/auth';
import {handleRequest} from '../src/index';
import {operatorReadiness} from '../scripts/operator-readiness';

/** Persisted DEV LOCAL only. Signed synthetic identities are verified using a
 * test-only JWK injection, NOT Google credentials or real Firebase admin claims.
 * No remote Cloudflare binding and no entitlement projection are used.
 */
it('upgrades without changing old grants and recovers an issued source after restarting persisted D1',async()=>{
  const base = path.resolve('.tmp'); mkdirSync(base,{recursive:true});
  const state = mkdtempSync(path.join(base,'s06-issuance-e2e-'));
  const runtime = () => new Miniflare({modules:true,script:'export default {fetch(){return new Response("local test")}}',
    compatibilityDate:'2026-07-01',d1Databases:{DB:'s06-issuance-fixtures-only'},d1Persist:state});
  let mf = runtime();
  try {
    let db = await mf.getD1Database('DB') as unknown as D1Database;
    const files = ['0001_entitlement_snapshots.sql','0002_legacy_grants.sql','0003_legacy_authority.sql',
      '0004_legacy_version_35.sql','0005_legacy_review.sql','0006_legacy_review_issuance.sql'];
    const migrate = async(file:string) => {
      const sql = readFileSync(`migrations/${file}`,'utf8').replace(/--[^\n]*/g,'');
      const triggers = [...sql.matchAll(/CREATE TRIGGER[\s\S]*?END;/g)].map(m=>m[0]);
      for(const part of sql.replace(/CREATE TRIGGER[\s\S]*?END;/g,'').split(';').map(s=>s.trim()).filter(Boolean))
        await db.prepare(part).run();
      for(const trigger of triggers) await db.prepare(trigger).run();
    };
    for(const file of files.slice(0,5)) await migrate(file);
    const pair = await generateKeyPair('RS256');
    const keys = createLocalJWKSet({keys:[{...await exportJWK(pair.publicKey),kid:'local-only',alg:'RS256'}]});
    const now = new Date(); const t = Math.floor(now.getTime()/1000);
    const token = async(uid:string, admin=false) => new SignJWT({sub:uid,aud:'calcmot',
      iss:'https://securetoken.google.com/calcmot',iat:t-5,auth_time:t-10,exp:t+3600,admin,
      firebase:{identities:{'google.com':['synthetic-google-identity']}}})
      .setProtectedHeader({alg:'RS256',kid:'local-only'}).sign(pair.privateKey);
    const session = async(jwt:string) => verifySession(new Request('https://local.invalid',
      {headers:{Authorization:`Bearer ${jwt}`}}),'calcmot',keys,now);
    const oldUser = await session(await token('fixture-previous-grant'));
    await db.prepare('INSERT INTO legacy_policies VALUES (?, ?, NULL, ?, ?, ?, ?, ?, ?, 12)')
      .bind('calcmot','fixture-old-policy',LEGACY_CORE_BASELINE,'fixture-migration','2026-01-01T00:00:00.000Z',
        'fixture-verifier','["SUPPORT_VERIFIED"]','fixture-criteria').run();
    await db.prepare('INSERT INTO legacy_evidence VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)')
      .bind('calcmot','fixture-old-history',oldUser.uid,'a'.repeat(64),'SUPPORT_VERIFIED','VERIFIED',
        now.toISOString(),'2026-01-01T00:00:00.000Z','fixture-verifier').run();
    await db.prepare('INSERT INTO legacy_historical_use VALUES (?, ?, 12, ?, ?, ?, ?)')
      .bind('calcmot','fixture-old-history','INDEPENDENT_HISTORICAL_RECORD','synthetic-only',now.toISOString(),'fixture-verifier').run();
    await new LegacyGrantService({DB:db,FIREBASE_PROJECT_ID:'calcmot'}).recordGrant(oldUser,'fixture-old-history','fixture-old-policy',now);
    const preserved = await new LegacyGrantService({DB:db,FIREBASE_PROJECT_ID:'calcmot'}).getGrant(oldUser);
    expect(preserved).not.toBeNull(); await migrate(files[5]);
    expect(await new LegacyGrantService({DB:db,FIREBASE_PROJECT_ID:'calcmot'}).getGrant(oldUser)).toEqual(preserved);
    const jwt = await token('fixture-assisted-user');
    const env = {DB:db,FIREBASE_PROJECT_ID:'calcmot',LEGACY_OWNER_UID:'fixture-owner',LEGACY_CLAIMS_ENABLED:'true'};
    const owner = await session(await token('fixture-owner',true));
    const applicant = await session(jwt); const review = new LegacyReviewService(env);
    // New operational preflight, after real signature validation with test-only
    // RSA keys. This is still NOT a Google-issued administrator identity.
    expect(operatorReadiness(owner,'calcmot',env.LEGACY_OWNER_UID,'owner',now).status).toBe('GO');
    expect(operatorReadiness(applicant,'calcmot',env.LEGACY_OWNER_UID,'ordinary',now).status).toBe('GO');
    const otherAdmin = await session(await token('fixture-other-admin',true));
    expect(operatorReadiness(otherAdmin,'calcmot',env.LEGACY_OWNER_UID,'owner',now).status).toBe('HOLD');
    await expect(review.inspect(otherAdmin,applicant.uid,'not-a-protocol',now)).rejects.toMatchObject({code:'FORBIDDEN'});
    await review.authorizePolicy(owner,now);
    const intake = () => new Request('https://local.invalid/v1/legacy/claim',{method:'POST',
      headers:{Authorization:`Bearer ${jwt}`,'Idempotency-Key':'fixture-intake'},
      body:JSON.stringify({evidenceType:'USER_DECLARATION',evidenceHash:null})});
    const response = await handleRequest(intake(),env,keys); expect(response.status).toBe(200);
    const opened = await response.json() as {claimId:string;state:string}; expect(opened.state).toBe('UNDER_REVIEW');
    for(const [revision,category,originKey] of [[0,'SUPPORT_RECORD','original-support'],[1,'ACCOUNT_CONTINUITY','ownership-check']] as const)
      await review.addEvidence(owner,applicant.uid,opened.claimId,revision,{kind:'AUXILIARY',category,originKey,
        reference:`synthetic:${originKey}`},`fixture-evidence-${revision}`,now);
    const inspected = await review.inspect(owner,applicant.uid,opened.claimId,now);
    const approval = await review.decide(owner,applicant.uid,opened.claimId,2,{outcome:'APPROVE',
      evidenceIds:inspected.evidence.map((e:any)=>e.evidence_id),reason:'Synthetic local assessment only; no real user benefit.',
      ownershipConfirmed:true,contradictionsCleared:true,legacyUseConcluded:true},'fixture-approve',now);
    const issued = await review.issue(owner,applicant.uid,opened.claimId,3,{approvalEventId:approval.decision.event_id},'fixture-issue',now);
    expect(issued.view.state).toBe('GRANTED');
    await mf.dispose(); mf = runtime(); db = await mf.getD1Database('DB') as unknown as D1Database;
    const recovered = await session(await token(applicant.uid)); // Fresh session/runtime.
    const restarted = new LegacyReviewService({...env,DB:db});
    expect((await restarted.getOwn(recovered,now))!.state).toBe('GRANTED');
    expect(await new LegacyGrantService({DB:db,FIREBASE_PROJECT_ID:'calcmot'}).getGrant(recovered)).toEqual(issued.grant);
    const restored = await handleRequest(intake(),{...env,DB:db},keys);
    expect(restored.status).toBe(200); expect((await restored.json() as any).state).toBe('GRANTED');
    const second = await session(await token('fixture-other-user'));
    expect(await new LegacyGrantService({DB:db,FIREBASE_PROJECT_ID:'calcmot'}).getGrant(second)).toBeNull();
    await expect(restarted.inspect(second,applicant.uid,opened.claimId,now)).rejects.toMatchObject({code:'FORBIDDEN'});
    const snapshot = await handleRequest(new Request('https://local.invalid/v1/me/entitlement',
      {headers:{Authorization:`Bearer ${jwt}`}}),{...env,DB:db},keys);
    expect(snapshot.status).toBe(503); // Source issuance is not S07 projection.
    const counts = await db.prepare(`SELECT (SELECT COUNT(*) FROM legacy_grants) AS grants,
      (SELECT COUNT(*) FROM legacy_review_issuances) AS issuances,
      (SELECT COUNT(*) FROM legacy_historical_use) AS histories,
      (SELECT COUNT(*) FROM entitlement_snapshots) AS snapshots`).first();
    expect(counts).toEqual({grants:2,issuances:1,histories:1,snapshots:0});
    writeFileSync(path.resolve('../.tmp/s06-issuance-e2e-evidence.json'),JSON.stringify({
      date:new Date().toISOString(),scope:'persisted DEV LOCAL, synthetic signed identities',
      states:['UNDER_REVIEW','APPROVE:ASSISTED_ASSESSMENT','GRANTED','GRANTED_AFTER_RESTART'],
      oldGrantPreserved:true,otherUserDenied:true,snapshotStatus:503,counts,realFirebaseAdminValidated:false,
      operationalPreflight:{owner:'GO',ordinaryControl:'GO',otherAdmin:'HOLD',scope:'synthetic signatures only'}
    },null,2));
  } finally {await mf.dispose();}
},30000);
