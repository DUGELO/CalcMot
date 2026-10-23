// Read-only PROD inventory. No credentials or personal rows are printed.
import {readFileSync,writeFileSync} from 'node:fs';
import {resolve,dirname} from 'node:path';
import {fileURLToPath} from 'node:url';
import {spawnSync} from 'node:child_process';
import {validateDatabaseId} from './prepare-admin-prod.mjs';
const root=resolve(dirname(fileURLToPath(import.meta.url)),'..');
const args=process.argv.slice(2);
if(args.length>1||(args.length===1&&args[0]!=='--expect-policy'))throw Error('INVALID_INVENTORY_MODE');
const expectedPolicies=args[0]==='--expect-policy'?1:0;
const configPath=resolve(root,'.tmp/admin-prod-release/private.jsonc');
const config=JSON.parse(readFileSync(configPath,'utf8'));
const binding=config.d1_databases?.[0];
validateDatabaseId(binding?.database_id??'');
if(config.name!=='calcmot-admin-private-prod'||config.vars?.ADMIN_ENVIRONMENT!=='PROD'||
  binding?.database_name!=='calcmot-identity-prod')throw Error('PROD_TARGET_REQUIRED');
const sql=readFileSync(resolve(root,'scripts/admin-prod-readiness.sql'),'utf8').replace(/^--.*$/mg,'').trim();
const run=spawnSync(process.execPath,[resolve(root,'node_modules/wrangler/bin/wrangler.js'),'d1','execute','DB',
  '--remote','--config',configPath,'--command',sql,'--json'],
  {cwd:root,encoding:'utf8',windowsHide:true,env:{...process.env,WRANGLER_SEND_METRICS:'false'}});
if(run.status!==0)throw Error('READ_ONLY_PROD_INVENTORY_FAILED');
const result=JSON.parse(run.stdout);
if(result.some(x=>!x.success))throw Error('PROD_QUERY_FAILED');
const counts=Object.fromEntries(result.flatMap(x=>x.results??[]).filter(x=>'category' in x).map(x=>[x.category,x.total]));
const summary={environment:'PROD',databaseId:binding.database_id,readOnly:true,
  quickCheck:result[0].results[0]?.quick_check,foreignKeyViolations:result[1].results.length,
  migrations:result[2].results.map(x=>x.name),indexes:result[3].results.map(x=>x.name),counts,
  sqlDurationMs:result.reduce((sum,x)=>sum+(x.meta?.timings?.sql_duration_ms??0),0)};
if(summary.quickCheck!=='ok'||summary.foreignKeyViolations!==0||summary.migrations.length!==9||summary.indexes.length!==4)
  throw Error('PROD_INTEGRITY_FAILED');
// This preparation gate is intentionally stricter than an operational inventory.
if(Object.keys(counts).length!==8||Object.entries(counts).some(([kind,n])=>n!==(kind==='policies'?expectedPolicies:0)))
  throw Error('PROD_NOT_EMPTY_REVIEW_REQUIRED');
writeFileSync(resolve(root,'.tmp/admin-prod-release/remote-readiness.json'),JSON.stringify(summary,null,2)+'\n');
console.log(JSON.stringify(summary,null,2));
