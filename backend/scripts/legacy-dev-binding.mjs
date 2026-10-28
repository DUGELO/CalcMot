import {readFile, mkdir, writeFile, rm} from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const backend = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const approvedDev = {
  account: '45086fe60ce934025fcac45e65eb4b69',
  database: '4d0c0c71-c606-45a6-a037-43bccef8c05e'
};

/** Explicit target guard, not a grant of remote-operation authorization. */
export function assertDevBinding(config) {
  const binding = config?.d1_databases?.[0];
  if (config?.name !== 'calcmot-api-dev' || config.account_id !== approvedDev.account ||
      config.vars?.FIREBASE_PROJECT_ID !== 'calcmot' || config.d1_databases.length !== 1 ||
      binding?.binding !== 'DB' || binding.database_name !== 'calcmot-identity-dev' ||
      binding.database_id !== approvedDev.database || config.routes?.length || config.route ||
      config.services?.length || config.kv_namespaces?.length || config.r2_buckets?.length)
    throw new Error('DEV_TARGET_REQUIRED');
  return binding;
}

/** Private Node process only: reuses the D1 binding API/batch, exposes no HTTP admin route.
 * Caller must verify the Firebase owner BEFORE opening this authenticated remote proxy.
 */
export async function openDevBinding() {
  const source = JSON.parse(await readFile(path.join(backend, 'wrangler.dev.jsonc'), 'utf8'));
  const binding = assertDevBinding(source);
  const directory = path.resolve(backend, '../.tmp/s06-dev-console');
  await mkdir(directory, {recursive:true});
  const configPath = path.join(directory, `binding-${process.pid}.json`);
  // No Firebase bearer, owner UID or admin credential is included in Wrangler config.
  await writeFile(configPath, JSON.stringify({name:'calcmot-s06-private-dev-console',
    account_id:approvedDev.account, main:path.join(backend,'src/index.ts'),
    compatibility_date:source.compatibility_date, workers_dev:false,
    d1_databases:[{...binding,remote:true,migrations_dir:path.join(backend,'migrations')}]}));
  let proxy;
  try {
    const {getPlatformProxy} = await import('wrangler');
    proxy = await getPlatformProxy({configPath,envFiles:[],persist:false,remoteBindings:true});
    return {db:proxy.env.DB, dispose:async()=>{await proxy.dispose(); await rm(configPath,{force:true});}};
  } catch (error) {
    await proxy?.dispose(); await rm(configPath,{force:true}); throw error;
  }
}
