import { afterAll, beforeAll, beforeEach, describe, expect, it } from 'vitest';
import { Miniflare } from 'miniflare';
import { createLocalJWKSet, exportJWK, generateKeyPair, SignJWT } from 'jose';
import { readFileSync } from 'node:fs';
import { handleRequest, type Env } from '../src/index';
import { hasRole, verifySession } from '../src/auth';

const mf = new Miniflare({ modules: true, script: 'export default {fetch(){return new Response("test")}}',
  compatibilityDate: '2026-07-01', d1Databases: { DB: 'local-test' } });
const pair = await generateKeyPair('RS256', { extractable: true });
const jwk = { ...await exportJWK(pair.publicKey), kid: 'test-key', alg: 'RS256' };
const keys = createLocalJWKSet({ keys: [jwk] });
let env: Env;
const fixtures = JSON.parse(readFileSync('../docs/contracts/test-matrix.md', 'utf8').match(/```json\r?\n([\s\S]*?)```/)![1]);
function snapshot(revision = 0) {
  const base = structuredClone(fixtures.baseFixtures.snapshot_blocked.instance);
  return { ...base, revision, checkedAt: '2026-01-01T00:00:00Z', serverNow: '2026-01-01T00:00:00Z',
    subscriptionRequired: false, coreAllowed: true, nextAction: 'NONE' };
}
async function token(uid = 'alice', overrides: Record<string, unknown> = {}, secret = pair.privateKey) {
  const now = Math.floor(Date.now() / 1000);
  return new SignJWT({ sub: uid, iss: 'https://securetoken.google.com/calcmot', aud: 'calcmot',
    iat: now - 10, auth_time: now - 20, exp: now + 3600, ...overrides })
    .setProtectedHeader({ alg: 'RS256', kid: 'test-key' }).sign(secret);
}
async function call(jwt?: string, path = '/v1/me/entitlement', method = 'GET', body?: string) {
  return handleRequest(new Request(`https://fixture.invalid${path}`, {
    method, headers: jwt ? { Authorization: `Bearer ${jwt}` } : {}, body
  }), env, keys);
}
async function put(uid: string, value = snapshot(), project = 'calcmot') {
  await env.DB.prepare('INSERT INTO entitlement_snapshots VALUES (?, ?, ?, ?)')
    .bind(project, uid, value.revision, JSON.stringify(value)).run();
}

beforeAll(async () => {
  env = { DB: await mf.getD1Database('DB') as unknown as D1Database, FIREBASE_PROJECT_ID: 'calcmot' };
  const migration = readFileSync('migrations/0001_entitlement_snapshots.sql', 'utf8');
  // D1 exec accepts trigger bodies through prepared statements separately.
  const triggerOffset = migration.indexOf('CREATE TRIGGER');
  await env.DB.exec(migration.slice(0, triggerOffset).replace(/--[^\n]*/g, '').replace(/\n/g, ' '));
  await env.DB.prepare(migration.slice(triggerOffset)).run();
});
beforeEach(async () => { await env.DB.exec('DELETE FROM entitlement_snapshots'); });
afterAll(async () => { await mf.dispose(); });

describe('Firebase authentication and owner isolation', () => {
  it('rejects absent/malformed/unsigned tokens with contract error envelope', async () => {
    for (const jwt of [undefined, 'invalid', 'eyJhbGciOiJub25lIn0.e30.']) {
      const result = await call(jwt);
      expect(result.status).toBe(401);
      const body = await result.json() as any;
      expect(body.code).toBe('AUTH_REQUIRED');
      expect(body.retryable).toBe(false);
      expect(body.requestId).toBe(result.headers.get('X-Request-ID'));
      expect(Object.keys(body).sort()).toEqual(['code','requestId','retryable','serverNow']);
    }
  });
  it.each([
    { iss: 'https://issuer.invalid' }, { aud: 'other-project' }, { aud: ['calcmot','other'] },
    { exp: 1 }, { iat: 9999999999 }, { auth_time: 9999999999 }, { sub: '' },
    { sub: 'x'.repeat(129) }, { exp: undefined }, { auth_time: undefined }, { iat: undefined }
  ])('rejects invalid verified claims %j', async (claims) => {
    expect((await call(await token('alice', claims))).status).toBe(401);
  });
  it('rejects an incorrect signature and HMAC algorithm', async () => {
    const other = await generateKeyPair('RS256');
    expect((await call(await token('alice', {}, other.privateKey))).status).toBe(401);
    const hmac = await new SignJWT({sub:'alice'}).setProtectedHeader({alg:'HS256'}).sign(new Uint8Array(32));
    expect((await call(hmac)).status).toBe(401);
  });
  it('returns only the owner snapshot and does not grant a missing user', async () => {
    await put('alice', snapshot(3)); await put('bob', snapshot(7));
    expect((await (await call(await token('alice'))).json() as any).revision).toBe(3);
    expect((await (await call(await token('bob'))).json() as any).revision).toBe(7);
    expect((await call(await token('missing'))).status).toBe(503);
  });
  it('isolates the same UID across Firebase projects', async () => {
    await put('alice', snapshot(7), 'other-project');
    expect((await call(await token())).status).toBe(503);
  });
  it('binds malicious subject strings as SQL values rather than executable SQL', async () => {
    await put('alice');
    expect((await call(await token("alice' OR 1=1 --"))).status).toBe(503);
  });
  it('returns retryable 503 when trusted key discovery is unavailable, without accepting a token', async () => {
    const result = await handleRequest(new Request('https://fixture.invalid/v1/me/entitlement', {
      headers: {Authorization: `Bearer ${await token()}`}
    }), env, async () => { throw new TypeError('network detail'); });
    expect(result.status).toBe(503);
    expect((await result.json() as any).code).toBe('RETRY_LATER');
  });
  it('refuses forged UID query, grants and all write requests', async () => {
    await put('alice'); await put('bob', snapshot(7));
    const forgedOwner = await call(await token(), '/v1/me/entitlement?uid=bob');
    expect(forgedOwner.status).toBe(200);
    expect((await forgedOwner.json() as any).revision).toBe(0);
    expect((await call(await token(), '/v1/me/entitlement', 'POST', '{"legacyCore":true,"uid":"bob"}')).status).toBe(403);
    for (const path of ['/admin/legacy/grant','/internal/reward/apply']) {
      expect((await call(await token(), path, 'POST', '{}')).status).toBe(403);
    }
    expect((await (await call(await token())).json() as any).legacyCore).toBe(false);
  });
  it('uses boolean signed roles only, with no email privilege or truthy strings', async () => {
    const session = await verifySession(new Request('https://fixture.invalid', {headers:{Authorization:`Bearer ${await token('alice',{email:'admin@example.com',admin:'true',creator:true})}`}}), 'calcmot', keys);
    expect(hasRole(session, 'admin')).toBe(false);
    expect(hasRole(session, 'creator')).toBe(true);
  });
  it('fails closed on malformed authoritative snapshot, stale access or database failure', async () => {
    await put('alice', { ...snapshot(), proAllowed: true });
    expect((await call(await token())).status).toBe(503);
    await env.DB.exec('DELETE FROM entitlement_snapshots');
    await put('alice', {...snapshot(), checkedAt: '2999-01-01T00:00:00Z'});
    expect((await call(await token())).status).toBe(503);
    const badEnv = {...env, DB:{prepare(){throw new Error('sensitive database details');}} as unknown as D1Database};
    const result = await handleRequest(new Request('https://fixture.invalid/v1/me/entitlement', {headers:{Authorization:`Bearer ${await token()}`}}), badEnv, keys);
    expect(result.status).toBe(503);
    expect(await result.text()).not.toContain('sensitive');
  });
  it('does not expose stored UID or bearer data and marks responses no-store', async () => {
    await put('alice');
    const jwt = await token(); const result = await call(jwt); const text = await result.text();
    expect(result.status).toBe(200); expect(result.headers.get('Cache-Control')).toBe('no-store');
    expect(text).not.toContain(jwt); expect(text).not.toContain('alice');
  });
  it('enforces SQL uniqueness, JSON structure, revision monotonicity and owner immutability', async () => {
    await put('alice', snapshot(3));
    await expect(put('alice', snapshot(3))).rejects.toThrow();
    await expect(env.DB.prepare('UPDATE entitlement_snapshots SET uid = ? WHERE uid = ?').bind('bob','alice').run()).rejects.toThrow();
    await expect(env.DB.prepare('UPDATE entitlement_snapshots SET revision = 2 WHERE uid = ?').bind('alice').run()).rejects.toThrow();
    await expect(env.DB.prepare('INSERT INTO entitlement_snapshots VALUES (?, ?, ?, ?)').bind('calcmot','bob',0,'broken-json').run()).rejects.toThrow();
    await expect(env.DB.prepare('INSERT INTO entitlement_snapshots VALUES (?, ?, ?, ?)').bind('calcmot','bob',0,'{}').run()).rejects.toThrow();
  });
});
