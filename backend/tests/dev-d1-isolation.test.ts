import { it, expect } from 'vitest';
import { execFileSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { createLocalJWKSet, exportJWK, generateKeyPair, SignJWT } from 'jose';
import { handleRequest, type Env } from '../src/index';

// Explicit opt-in: real DEV D1, local handler and ephemeral test signing keys.
// Never deploy a test issuer, replace Google keys, or use production resources.
it.skipIf(process.env.CALCMOT_REMOTE_DEV_TEST !== '1')('isolates two synthetic owners in real DEV D1 and cleans fixtures', async () => {
  const config = JSON.parse(readFileSync('wrangler.dev.jsonc', 'utf8'));
  expect(config.name).toBe('calcmot-api-dev');
  expect(config.account_id).toBe('45086fe60ce934025fcac45e65eb4b69');
  expect(config.d1_databases).toHaveLength(1);
  expect(config.d1_databases[0].database_id).toBe('4d0c0c71-c606-45a6-a037-43bccef8c05e');
  expect(config.d1_databases[0].database_name).toBe('calcmot-identity-dev');
  const cli = 'node_modules/wrangler/bin/wrangler.js';
  const sql = (statement: string) => {
    const output = execFileSync(process.execPath, [cli, 'd1', 'execute', 'DB', '--remote',
      '--config', 'wrangler.dev.jsonc', '--command', statement, '--json'], {
      encoding: 'utf8', timeout: 60000, stdio: ['ignore', 'pipe', 'pipe'],
      env: { ...process.env, WRANGLER_SEND_METRICS: 'false', CLOUDFLARE_AUTH_USE_KEYRING: 'true' }
    });
    const result = JSON.parse(output);
    expect(result.every((entry: { success: boolean }) => entry.success)).toBe(true);
    return result.flatMap((entry: { results: unknown[] }) => entry.results);
  };
  const quote = (value: unknown) => `'${String(value).replaceAll("'", "''")}'`;
  const run = crypto.randomUUID();
  const a = `s02-fixture-a-${run}`, b = `s02-fixture-b-${run}`;
  const owners = `${quote(a)},${quote(b)}`;
  const where = `firebase_project_id='calcmot' AND uid IN (${owners})`;
  const fixtureSet = JSON.parse(readFileSync('../docs/contracts/test-matrix.md', 'utf8')
    .match(/```json\r?\n([\s\S]*?)```/)![1]);
  const fixture = (revision: number) => ({ ...fixtureSet.baseFixtures.snapshot_blocked.instance, revision });
  const pair = await generateKeyPair('RS256', { extractable: true });
  const keys = createLocalJWKSet({ keys: [{ ...await exportJWK(pair.publicKey), kid: run, alg: 'RS256' }] });
  const token = async (uid: string) => {
    const now = Math.floor(Date.now() / 1000);
    return new SignJWT({ sub: uid, iss: 'https://securetoken.google.com/calcmot', aud: 'calcmot',
      iat: now - 1, auth_time: now - 2, exp: now + 120 })
      .setProtectedHeader({ alg: 'RS256', kid: run }).sign(pair.privateKey);
  };
  const db = { prepare: (statement: string) => ({ bind: (project: string, uid: string) => ({
    first: async () => {
      expect(statement).toBe('SELECT snapshot_json, revision FROM entitlement_snapshots WHERE firebase_project_id = ? AND uid = ?');
      return sql(`SELECT snapshot_json, revision FROM entitlement_snapshots WHERE firebase_project_id=${quote(project)} AND uid=${quote(uid)}`)[0] ?? null;
    }
  }) }) } as unknown as D1Database;
  const env: Env = { DB: db, FIREBASE_PROJECT_ID: 'calcmot' };
  const request = async (jwt?: string, suffix = '', method = 'GET') => handleRequest(new Request(
    `https://local-harness.invalid/v1/me/entitlement${suffix}`, {
      method, headers: jwt ? { Authorization: `Bearer ${jwt}` } : {}
    }), env, keys);
  const baseline = sql('SELECT COUNT(*) AS n FROM entitlement_snapshots')[0] as { n: number };
  const ta = await token(a), tb = await token(b);
  try {
    sql(`INSERT INTO entitlement_snapshots VALUES ('calcmot',${quote(a)},101,${quote(JSON.stringify(fixture(101)))}),('calcmot',${quote(b)},202,${quote(JSON.stringify(fixture(202)))})`);
    for (const [jwt, other, revision] of [[ta, b, 101], [tb, a, 202]] as const) {
      for (const suffix of ['', `?uid=${encodeURIComponent(other)}`]) {
        const result = await request(jwt, suffix);
        expect(result.status).toBe(200);
        const body = await result.json() as any;
        expect(body.revision).toBe(revision);
        expect(body.coreAllowed).toBe(false); expect(body.proAllowed).toBe(false);
        expect(body.legacyCore).toBe(false); expect(body.proFeatureSet).toEqual([]);
      }
    }
    expect((await request()).status).toBe(401);
    expect((await request('invalid')).status).toBe(401);
    expect((await request(ta, '', 'POST')).status).toBe(403);
    expect((await request(await token(`s02-missing-${run}`))).status).toBe(503);
  } finally {
    sql(`DELETE FROM entitlement_snapshots WHERE ${where}`);
    expect((sql(`SELECT COUNT(*) AS n FROM entitlement_snapshots WHERE ${where}`)[0] as { n: number }).n).toBe(0);
    expect((sql('SELECT COUNT(*) AS n FROM entitlement_snapshots')[0] as { n: number }).n).toBe(baseline.n);
  }
  console.info('DEV D1: owners A/B 200; forged owner stays own; 401/403/503 verified; fixtures remaining=0; baseline count restored. Local signed test JWTs; published Worker unchanged.');
}, 240000);
