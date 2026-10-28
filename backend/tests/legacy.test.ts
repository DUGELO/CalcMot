import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import { Miniflare } from 'miniflare';
import { readFileSync } from 'node:fs';
import { createLocalJWKSet, exportJWK, generateKeyPair, SignJWT } from 'jose';
import { LegacyGrantService, LEGACY_CORE_BASELINE } from '../src/legacy';
import { validateLegacyGrant } from '../src/generated/legacy-validator.js';
import { handleRequest } from '../src/index';
import type { Session } from '../src/auth';

const mf = new Miniflare({ modules: true, script: 'export default {fetch(){return new Response("test")}}',
  compatibilityDate: '2026-07-01', d1Databases: {
    DB: 'local-s05-only', EXISTING: 'local-s05-existing', PREVIOUS: 'local-s05-previous-grant'
  } });
let db: D1Database;
let service: LegacyGrantService;
const now = new Date('2026-10-13T12:00:00.000Z');
const cutoff = '2026-10-12T00:00:00.000Z'; // Historical fixture time, not an eligibility cutoff.
let serial = 0;
const next = () => `fixture-${++serial}`;
function session(uid: string, linked = true, project = 'calcmot'): Session {
  return { uid, claims: { sub: uid, aud: project, iss: `https://securetoken.google.com/${project}`,
    firebase: { identities: linked ? { 'google.com': ['synthetic-account'] } : {} } } };
}
async function policy(version = next(), cutoffUtc: string | null = null, kinds = '["SUPPORT_VERIFIED"]') {
  await db.prepare('INSERT INTO legacy_policies VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)')
    .bind('calcmot', version, cutoffUtc, LEGACY_CORE_BASELINE, 'fixture-migration',
      '2026-10-01T00:00:00.000Z', 'local-test-approval', kinds, 'synthetic-criteria-only', 12).run();
  return version;
}
async function proof(uid: string, qualifyingAt = '2026-10-11T23:59:59.999Z', hash?: string, historicalVersion: number | null = 12) {
  const id = next();
  const digest = hash ?? serial.toString(16).padStart(64, '0');
  await db.prepare(`INSERT INTO legacy_evidence
    (firebase_project_id, evidence_id, uid, evidence_hash, evidence_kind, verification,
     observed_at, qualifying_at, verified_by) VALUES (?, ?, ?, ?, 'SUPPORT_VERIFIED', 'VERIFIED', ?, ?, ?)`)
    .bind('calcmot', id, uid, digest, now.toISOString(), qualifyingAt, 'local-test-verifier').run();
  if (historicalVersion !== null) await db.prepare('INSERT INTO legacy_historical_use VALUES (?, ?, ?, ?, ?, ?, ?)')
    .bind('calcmot', id, historicalVersion, 'ASSISTED_REVIEW_VERIFIED', 'synthetic-independent-history',
      now.toISOString(), 'local-test-verifier').run();
  return { id, hash: digest };
}
async function count(table: string) {
  return (await db.prepare(`SELECT COUNT(*) AS n FROM ${table}`).first<{n: number}>())!.n;
}
async function migrate(target: D1Database, names: string[]) {
  for (const name of names) {
    const sql = readFileSync(`migrations/${name}`, 'utf8').replace(/--[^\n]*/g, '');
    const triggers = [...sql.matchAll(/CREATE TRIGGER[\s\S]*?END;/g)].map(match => match[0]);
    const tables = sql.replace(/CREATE TRIGGER[\s\S]*?END;/g, '');
    for (const statement of tables.split(';').map(value => value.trim()).filter(Boolean))
      await target.prepare(statement).run();
    for (const trigger of triggers) await target.prepare(trigger).run();
  }
}
beforeAll(async () => {
  db = await mf.getD1Database('DB') as unknown as D1Database;
  service = new LegacyGrantService({ DB: db, FIREBASE_PROJECT_ID: 'calcmot' });
  await migrate(db, ['0001_entitlement_snapshots.sql', '0002_legacy_grants.sql', '0003_legacy_authority.sql', '0004_legacy_version_35.sql']);
});
afterAll(async () => { await mf.dispose(); });

describe('S05: private legacy source, no commercial defaults or snapshot projector', () => {
  it('preserves grants issued under the previous schema while requiring the new rule for new grants', async () => {
    const previous = await mf.getD1Database('PREVIOUS') as unknown as D1Database;
    await migrate(previous, ['0001_entitlement_snapshots.sql', '0002_legacy_grants.sql', '0003_legacy_authority.sql']);
    await previous.prepare('INSERT INTO legacy_policies VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)')
      .bind('calcmot', 'prior-policy-fixture', null, 'prior-fixture-only', 'prior-fixture-migration',
        '2026-10-01T00:00:00.000Z', 'local-test-approval', '["SUPPORT_VERIFIED"]', 'prior-fixture-criteria').run();
    const e = 'previous-evidence'; const uid = 'previous-owner'; const hash = 'c'.repeat(64);
    await previous.prepare('INSERT INTO legacy_evidence VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)')
      .bind('calcmot', e, uid, hash, 'SUPPORT_VERIFIED', 'VERIFIED', now.toISOString(), cutoff, 'local-test-verifier').run();
    const original = { uid, status: 'GRANTED', policyCutoffUtc: null, policyVersion: 'prior-policy-fixture',
      legacyGrantedAt: now.toISOString(), legacyEvidenceType: 'SUPPORT_VERIFIED', evidenceHash: hash,
      legacyCoreVersionBaseline: 'prior-fixture-only', migrationVersion: 'prior-fixture-migration',
      grantRevision: 1, decisionActor: 'local-test-verifier' };
    await previous.prepare('INSERT INTO legacy_grants VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)')
      .bind('calcmot', uid, original.status, null, original.policyVersion, original.legacyGrantedAt,
        original.legacyEvidenceType, hash, original.legacyCoreVersionBaseline, original.migrationVersion,
        1, original.decisionActor, JSON.stringify(original)).run();
    await migrate(previous, ['0004_legacy_version_35.sql']);
    const upgraded = new LegacyGrantService({ DB: previous, FIREBASE_PROJECT_ID: 'calcmot' });
    expect(await upgraded.getGrant(session(uid))).toEqual(original);
    expect(await upgraded.recordGrant(session(uid), e, original.policyVersion, now))
      .toEqual({ granted: true, grant: original, idempotent: true });
    const other = 'new-owner';
    await previous.prepare('INSERT INTO legacy_evidence VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)')
      .bind('calcmot', 'new-evidence', other, 'd'.repeat(64), 'SUPPORT_VERIFIED', 'VERIFIED',
        now.toISOString(), cutoff, 'local-test-verifier').run();
    expect(await upgraded.recordGrant(session(other), 'new-evidence', original.policyVersion, now))
      .toEqual({ granted: false, reason: 'POLICY_REQUIRED' });
  });
  it('adds legacy tables without rewriting an existing snapshot or weakening its revision protection', async () => {
    const existing = await mf.getD1Database('EXISTING') as unknown as D1Database;
    await migrate(existing, ['0001_entitlement_snapshots.sql']);
    const original = JSON.stringify({ contractVersion: '1.0.0', revision: 7 }); // Storage-only fixture; no rights.
    await existing.prepare('INSERT INTO entitlement_snapshots VALUES (?, ?, ?, ?)')
      .bind('calcmot', 'existing-storage-fixture', 7, original).run();
    await migrate(existing, ['0002_legacy_grants.sql', '0003_legacy_authority.sql', '0004_legacy_version_35.sql']);
    expect(await existing.prepare('SELECT revision, snapshot_json FROM entitlement_snapshots').first())
      .toEqual({ revision: 7, snapshot_json: original });
    await expect(existing.prepare('UPDATE entitlement_snapshots SET revision = 7').run())
      .rejects.toThrow('entitlement revision or ownership conflict');
    expect((await existing.prepare('SELECT COUNT(*) AS n FROM legacy_grants').first<{n: number}>())!.n).toBe(0);
  });
  it('requires an approved policy even for independently verified evidence', async () => {
    const uid = next(); const e = await proof(uid); const before = await count('legacy_grants');
    expect(await service.recordGrant(session(uid), e.id, 'not-approved', now))
      .toEqual({ granted: false, reason: 'POLICY_REQUIRED' });
    expect(await count('legacy_grants')).toBe(before);
  });
  it('records only Core provenance and recovers the same UID after reinstall/device change, without a snapshot', async () => {
    const uid = next(); const e = await proof(uid); const p = await policy();
    const result = await service.recordGrant(session(uid), e.id, p, now);
    expect(result.granted).toBe(true);
    if (!result.granted) return;
    expect(validateLegacyGrant(result.grant)).toBe(true);
    expect(result.grant.legacyEvidenceType).toBe('SUPPORT_VERIFIED');
    expect(result.grant.decisionActor).toBe('local-test-verifier');
    expect(result.grant.grantRevision).toBe(1);
    expect('proAllowed' in result.grant).toBe(false);
    expect(await count('entitlement_snapshots')).toBe(0);
    // Fresh session, no local installation data, same trusted UID.
    expect(await service.getGrant(session(uid))).toEqual(result.grant);
    expect(await service.getGrant(session(next()))).toBeNull();
    const otherProject = new LegacyGrantService({ DB: db, FIREBASE_PROJECT_ID: 'other-project' });
    expect(await otherProject.getGrant(session(uid, true, 'other-project'))).toBeNull();
  });
  it.each([
    '2026-10-11T23:59:59.999Z', '2026-10-12T00:00:00.000Z', '2026-10-12T00:00:00.001Z'
  ])('does not replace the approved version rule with a calendar cutoff: %s', async (at) => {
    const uid = next(); const e = await proof(uid, at); const p = await policy();
    const result = await service.recordGrant(session(uid), e.id, p, now);
    expect(result.granted).toBe(true);
  });
  it('uses a null cutoff for the approved version policy, never a blanket migration grant', async () => {
    const uid = next(); const e = await proof(uid); const p = await policy(undefined, null);
    const result = await service.recordGrant(session(uid), e.id, p, now);
    expect(result.granted && result.grant.policyCutoffUtc === null).toBe(true);
  });
  it.each([[1, true], [11, true], [12, true], [13, false], [14, false]] as const)
    ('checks independently verified historical use of versionCode %i', async (historicalVersion, eligible) => {
      const uid = next(); const e = await proof(uid, undefined, undefined, historicalVersion); const p = await policy();
      const result = await service.recordGrant(session(uid), e.id, p, now);
      expect(result.granted).toBe(eligible);
      if (result.granted) expect(result.grant.legacyCoreVersionBaseline).toBe('calcmot-core-3.5-vc12');
      else expect(result.reason).toBe('NOT_ELIGIBLE');
    });
  it('does not grant for registration or even a verified migration observation without historical use', async () => {
    const uid = next(); const e = await proof(uid, undefined, undefined, null); const p = await policy();
    expect(await service.recordGrant(session(uid), e.id, p, now))
      .toEqual({ granted: false, reason: 'HISTORICAL_PROOF_REQUIRED' });
    const bridgeUid = next(); const bridgeId = next();
    await db.prepare(`INSERT INTO legacy_evidence VALUES
      ('calcmot', ?, ?, ?, 'SERVER_BRIDGE_OBSERVATION', 'VERIFIED', ?, ?, 'local-test-verifier')`)
      .bind(bridgeId, bridgeUid, serial.toString(16).padStart(64, '0'), now.toISOString(), cutoff).run();
    const bridgePolicy = await policy(undefined, null, '["SERVER_BRIDGE_OBSERVATION"]');
    expect(await service.recordGrant(session(bridgeUid), bridgeId, bridgePolicy, now))
      .toEqual({granted: false, reason: 'HISTORICAL_PROOF_REQUIRED'});
    expect(await service.getGrant(session(uid))).toBeNull();
    expect(await service.getGrant(session(bridgeUid))).toBeNull();
  });
  it('rejects a time-based policy even if its timestamp would admit a newly registered user', async () => {
    const uid = next(); const e = await proof(uid); const p = await policy(undefined, '2026-12-01T00:00:00.000Z');
    expect(await service.recordGrant(session(uid), e.id, p, now)).toEqual({granted: false, reason: 'POLICY_REQUIRED'});
  });
  it('does not let a current-version declaration overwrite immutable verified historical use', async () => {
    const uid = next(); const e = await proof(uid); const p = await policy();
    await expect(db.prepare('UPDATE legacy_historical_use SET used_version_code = 13 WHERE evidence_id = ?')
      .bind(e.id).run()).rejects.toThrow('verified legacy history is immutable');
    const result = await service.recordGrant(session(uid), e.id, p, now);
    expect(result.granted).toBe(true);
    expect(await service.getGrant(session(uid))).toEqual(result.granted ? result.grant : null);
  });
  it('does not permanently classify a person as new when earlier verified use is recovered later', async () => {
    const uid = next(); const p = await policy(); const recent = await proof(uid, undefined, undefined, 13);
    expect(await service.recordGrant(session(uid), recent.id, p, now)).toEqual({granted: false, reason: 'NOT_ELIGIBLE'});
    const historical = await proof(uid, undefined, undefined, 11);
    const result = await service.recordGrant(session(uid), historical.id, p, now);
    expect(result.granted).toBe(true);
    // New local observations / a new session do not redefine the proven history.
    await service.observeLocal(session(uid), 'f'.repeat(64), now);
    expect(await service.getGrant(session(uid))).toEqual(result.granted ? result.grant : null);
  });
  it('does not implicitly accept all verified proof kinds and preserves policy approval', async () => {
    const uid = next(); const id = next(); const p = await policy();
    await db.prepare(`INSERT INTO legacy_evidence VALUES
      ('calcmot', ?, ?, ?, 'SERVER_BRIDGE_OBSERVATION', 'VERIFIED', ?, ?, 'local-test-verifier')`)
      .bind(id, uid, serial.toString(16).padStart(64, '0'), now.toISOString(), '2026-10-11T00:00:00.000Z').run();
    expect(await service.recordGrant(session(uid), id, p, now)).toEqual({granted: false, reason: 'NOT_ELIGIBLE'});
    await expect(db.prepare('UPDATE legacy_policies SET cutoff_utc = NULL WHERE policy_version = ?')
      .bind(p).run()).rejects.toThrow('legacy policy is immutable');
  });
  it('treats client/local dates, version and hashes as auxiliary; repeated observations do not duplicate', async () => {
    const uid = next(); const digest = 'a'.repeat(64); const p = await policy();
    const id = await service.observeLocal(session(uid), digest, now);
    expect(await service.observeLocal(session(uid), digest, now)).toBe(id);
    expect(await service.recordGrant(session(uid), id, p, now))
      .toEqual({ granted: false, reason: 'INVALID_EVIDENCE' });
    await expect(db.prepare(`UPDATE legacy_evidence SET verification = 'VERIFIED',
      qualifying_at = ?, verified_by = 'forged' WHERE evidence_id = ?`).bind(cutoff, id).run())
      .rejects.toThrow('legacy evidence is immutable');
    // The SQL constraint rejects even inserting local evidence marked as verified.
    await expect(db.prepare(`INSERT INTO legacy_evidence VALUES
      ('calcmot', ?, ?, ?, 'LOCAL_INSTALL_METADATA', 'VERIFIED', ?, ?, 'forged')`)
      .bind(next(), uid, 'b'.repeat(64), now.toISOString(), cutoff).run()).rejects.toThrow();
  });
  it('does not grant to an unlinked account and only prepares a recovery reference', async () => {
    const uid = next(); const e = await proof(uid); const p = await policy();
    expect(await service.recordGrant(session(uid, false), e.id, p, now))
      .toEqual({ granted: false, reason: 'LINK_REQUIRED' });
    expect(await service.prepareRecovery(session(uid, false), e.id, now)).toBe('PENDING_OWNERSHIP_REVIEW');
    expect(await service.getGrant(session(uid, false))).toBeNull();
  });
  it('rejects a forged UID/project in the trusted identity context', async () => {
    const real = session(next());
    await expect(service.getGrant({ ...real, uid: 'forged-owner' })).rejects.toThrow('Invalid verified identity');
    await expect(service.getGrant(session(real.uid, true, 'other-project'))).rejects.toThrow('Invalid verified identity');
  });
  it('routes ownership conflict to recovery without transfer or exposing the original UID', async () => {
    const owner = next(); const claimant = next(); const e = await proof(owner); const p = await policy();
    const granted = await service.recordGrant(session(owner), e.id, p, now);
    expect(granted.granted).toBe(true);
    const before = await count('legacy_recovery_requests');
    expect(await service.recordGrant(session(claimant), e.id, p, now))
      .toEqual({ granted: false, reason: 'OWNERSHIP_REVIEW_REQUIRED' });
    await service.prepareRecovery(session(claimant), e.id, now);
    await service.prepareRecovery(session(claimant), e.id, now);
    expect(await count('legacy_recovery_requests')).toBe(before + 1);
    expect(await service.getGrant(session(claimant))).toBeNull();
    expect(await service.getGrant(session(owner))).toEqual(granted.granted ? granted.grant : null);
    await expect(proof(claimant, undefined, e.hash)).rejects.toThrow();
  });
  it('arbitrates concurrent/repeated migrations once and never changes the original grant', async () => {
    const uid = next(); const e = await proof(uid); const p = await policy(); const before = await count('legacy_grants');
    const attempts = await Promise.all(Array.from({ length: 5 }, () => service.recordGrant(session(uid), e.id, p, now)));
    expect(attempts.every(result => result.granted)).toBe(true);
    expect(attempts.filter(result => result.granted && !result.idempotent)).toHaveLength(1);
    expect(await count('legacy_grants')).toBe(before + 1);
    const original = await service.getGrant(session(uid));
    const retry = await service.recordGrant(session(uid), e.id, p, new Date('2026-12-01T00:00:00.000Z'));
    expect(retry.granted && retry.idempotent).toBe(true);
    expect(await service.getGrant(session(uid))).toEqual(original);
    await expect(db.prepare('UPDATE legacy_grants SET migration_version = ? WHERE uid = ?')
      .bind('price-or-rollout-change', uid).run()).rejects.toThrow('legacy grants are immutable');
    await expect(db.prepare('DELETE FROM legacy_grants WHERE uid = ?').bind(uid).run())
      .rejects.toThrow('legacy grants cannot be deleted');
  });
  it('rejects direct insertion of a grant without verified evidence or with forged JSON ownership', async () => {
    const uid = next(); const e = await proof(uid); const p = await policy();
    const result = await service.recordGrant(session(uid), e.id, p, now);
    if (!result.granted) throw new Error('fixture failed');
    const { grant } = result;
    const insert = (columnUid: string, jsonUid: string) => db.prepare(`INSERT INTO legacy_grants VALUES
      (?, ?, 'GRANTED', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`)
      .bind('calcmot', columnUid, grant.policyCutoffUtc, p, grant.legacyGrantedAt, grant.legacyEvidenceType,
        grant.evidenceHash, grant.legacyCoreVersionBaseline, grant.migrationVersion, 1,
        grant.decisionActor, JSON.stringify({ ...grant, uid: jsonUid })).run();
    await expect(insert(next(), next())).rejects.toThrow('legacy grant requires');
    const unproven = next();
    await expect(insert(unproven, unproven)).rejects.toThrow('legacy grant requires');
  });
  it('also enforces the historical version rule for direct privileged SQL writes', async () => {
    const uid = next(); const e = await proof(uid, undefined, undefined, 13); const p = await policy();
    const grant = { uid, status: 'GRANTED', policyCutoffUtc: null, policyVersion: p,
      legacyGrantedAt: now.toISOString(), legacyEvidenceType: 'SUPPORT_VERIFIED', evidenceHash: e.hash,
      legacyCoreVersionBaseline: LEGACY_CORE_BASELINE, migrationVersion: 'fixture-migration',
      grantRevision: 1, decisionActor: 'local-test-verifier' };
    await expect(db.prepare('INSERT INTO legacy_grants VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)')
      .bind('calcmot', uid, grant.status, null, p, grant.legacyGrantedAt, grant.legacyEvidenceType,
        e.hash, LEGACY_CORE_BASELINE, grant.migrationVersion, 1, grant.decisionActor, JSON.stringify(grant)).run())
      .rejects.toThrow('legacy grant requires verified historical use through versionCode 12');
    expect(await service.getGrant(session(uid))).toBeNull();
  });
  it('keeps grant storage recoverable when snapshots/backend/key discovery are absent or unavailable; clients cannot grant', async () => {
    const pair = await generateKeyPair('RS256', { extractable: true });
    const keys = createLocalJWKSet({ keys: [{ ...await exportJWK(pair.publicKey), kid: 's05-local', alg: 'RS256' }] });
    const uid = next(); const e = await proof(uid); const p = await policy();
    await service.recordGrant(session(uid), e.id, p, now);
    const seconds = Math.floor(Date.now() / 1000);
    const jwt = await new SignJWT({ sub: uid, iss: 'https://securetoken.google.com/calcmot', aud: 'calcmot',
      iat: seconds - 10, auth_time: seconds - 20, exp: seconds + 3600 })
      .setProtectedHeader({ alg: 'RS256', kid: 's05-local' }).sign(pair.privateKey);
    const env = { DB: db, FIREBASE_PROJECT_ID: 'calcmot' };
    const call = (path = '/v1/me/entitlement', method = 'GET') =>
      new Request(`https://fixture.invalid${path}`, { method, headers: { Authorization: `Bearer ${jwt}` },
        ...(method === 'POST' ? {body: JSON.stringify({uid:'forged',legacyCore:true,proAllowed:true})} : {}) });
    expect((await handleRequest(call(), env, keys)).status).toBe(503); // S07 not implemented.
    expect((await handleRequest(call(), env, async () => { throw new TypeError('offline'); })).status).toBe(503);
    const unavailable = { ...env, DB: { prepare() { throw new Error('DB unavailable'); } } as unknown as D1Database };
    expect((await handleRequest(call(), unavailable, keys)).status).toBe(503);
    expect((await handleRequest(call('/admin/legacy/grant', 'POST'), env, keys)).status).toBe(403);
    expect((await handleRequest(call('/v1/me/entitlement', 'POST'), env, keys)).status).toBe(403);
    expect((await handleRequest(call('/v1/legacy/claim', 'POST'), env, keys)).status).toBe(503); // S06 disabled outside local config.
    expect((await handleRequest(new Request('https://fixture.invalid/v1/me/entitlement'), env, keys)).status).toBe(401);
    expect(await service.getGrant(session(uid))).not.toBeNull();
    expect(await count('entitlement_snapshots')).toBe(0);
  });
});
