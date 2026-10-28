// Local authenticated operator console only. No remote/production option.
// Token is read from stdin, never argv, a file, logs, or a saved fixture.
import { mkdir } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import { build } from 'esbuild';
import { Miniflare } from 'miniflare';
import { executeConsoleCommand } from './legacy-console-command.mjs';

const backend = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const state = path.resolve(backend, '../.tmp/s06-issuance-local');
let runtime;
let remote;
try {
  const args = process.argv.slice(2);
  if (args.length > 1 || (args.length === 1 && args[0] !== '--dev')) throw new Error('INVALID_REQUEST');
  const dev = args[0] === '--dev';
  let text = '';
  for await (const chunk of process.stdin) {
    text += chunk;
    if (Buffer.byteLength(text) > 32768) throw new Error('INPUT_TOO_LARGE');
  }
  const request = JSON.parse(text);
  const allowed = ['token','action','input','key','uid','claimId','revision'];
  if (!request || Object.keys(request).some(key => !allowed.includes(key)) || typeof request.token !== 'string')
    throw new Error('INVALID_REQUEST');
  const project = process.env.FIREBASE_PROJECT_ID;
  if (!project) throw new Error('PROJECT_REQUIRED');
  const bundle = await build({ stdin: {
    contents: "export {LegacyReviewService, ReviewError} from './src/legacy-review.ts'; export {LegacyPrivacyService} from './src/legacy-privacy.ts'; export {verifySession} from './src/auth.ts';",
    resolveDir: backend, sourcefile: 's06-local-entry.ts', loader: 'ts'
  }, absWorkingDir: backend, tsconfigRaw: {}, bundle: true,
    platform: 'node', format: 'esm', write: false, logLevel: 'silent' });
  const api = await import(`data:text/javascript;base64,${Buffer.from(bundle.outputFiles[0].contents).toString('base64')}`);
  const session = await api.verifySession(new Request('https://local.invalid', {
    headers: {Authorization: `Bearer ${request.token}`}
  }), project);
  // Do not retain the bearer while opening the local database.
  delete request.token; text = '';
  const ownerAction = ['inspect','evidence','decide','issue','policy','privacy-plan','privacy-execute','privacy-close'].includes(request.action);
  if (ownerAction && (session.claims.admin !== true || !process.env.LEGACY_OWNER_UID ||
      session.uid !== process.env.LEGACY_OWNER_UID || typeof session.claims.auth_time !== 'number' ||
      session.claims.auth_time > Date.now()/1000 ||
      Math.floor(Date.now() / 1000) - session.claims.auth_time > 300)) throw new Error('FORBIDDEN');
  if (!['open','own','inspect','evidence','decide','issue','policy','privacy-plan','privacy-execute','privacy-close'].includes(request.action)) throw new Error('INVALID_REQUEST');
  // Remote mode is operator-only, including reads. Never give a normal user a D1 proxy.
  if (dev && !ownerAction) throw new Error('FORBIDDEN');
  if (dev) remote = await (await import('./legacy-dev-binding.mjs')).openDevBinding();
  else {
    await mkdir(state, {recursive: true});
    runtime = new Miniflare({ modules: true, script: 'export default {fetch(){return new Response("not exposed")}}',
    compatibilityDate: '2026-07-01', d1Databases: {DB: '00000000-0000-0000-0000-000000000000'},
    d1Persist: path.join(state, 'v3', 'd1') });
  }
  const db = remote?.db ?? await runtime.getD1Database('DB');
  const migrated = await db.prepare("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'legacy_review_issuances'").first();
  if (!migrated) throw new Error('MIGRATIONS_REQUIRED');
  const result = await executeConsoleCommand(api,
    {DB:db,FIREBASE_PROJECT_ID:project,LEGACY_OWNER_UID:process.env.LEGACY_OWNER_UID},session,request);
  // Authenticated private operator output. No bearer or private key is included.
  process.stdout.write(JSON.stringify(result) + '\n');
} catch (error) {
  const safe = new Set(['FORBIDDEN','INVALID_REQUEST','CONFLICT','NOT_FOUND','MORE_EVIDENCE_REQUIRED',
    'LINK_REQUIRED','GRANT_PRESERVED','POLICY_REQUIRED','PROJECT_REQUIRED','INPUT_TOO_LARGE','MIGRATIONS_REQUIRED']);
  const code = safe.has(error?.code) ? error.code : safe.has(error?.message) ? error.message :
    error?.constructor?.name === 'InvalidSession' ? 'AUTH_REQUIRED' : 'RETRY_LATER';
  process.stderr.write(JSON.stringify({code}) + '\n');
  process.exitCode = 1;
} finally { await remote?.dispose(); await runtime?.dispose(); }
