// Always LOCAL. No --remote option, no account credentials, no copied DEV state.
import {mkdtempSync,writeFileSync,readFileSync,mkdirSync} from 'node:fs';
import {resolve,dirname} from 'node:path';
import {fileURLToPath} from 'node:url';
import {spawnSync} from 'node:child_process';
const root=resolve(dirname(fileURLToPath(import.meta.url)),'..');
mkdirSync(resolve(root,'.tmp'),{recursive:true});
const out=mkdtempSync(resolve(root,'.tmp/prod-migration-validation-'));
const config=resolve(out,'wrangler.jsonc');
writeFileSync(config,JSON.stringify({name:'synthetic-prod-schema-validation',compatibility_date:'2026-07-01',
  d1_databases:[{binding:'DB',database_name:'synthetic-prod-schema-validation',
    database_id:'11111111-2222-3333-4444-555555555555',migrations_dir:resolve(root,'migrations')}]}));
const invoke=(args)=>{
  const run=spawnSync(process.execPath,[resolve(root,'node_modules/wrangler/bin/wrangler.js'),...args,
    '--config',config,'--local','--persist-to',resolve(out,'state')],
    {cwd:root,encoding:'utf8',env:{...process.env,WRANGLER_SEND_METRICS:'false',CI:'true'},windowsHide:true});
  if(run.status!==0)throw Error('LOCAL_VALIDATION_FAILED: '+run.stderr+' '+run.stdout);
  return run.stdout;
};
const first=invoke(['d1','migrations','apply','DB']);
const second=invoke(['d1','migrations','apply','DB']);
const sql=`PRAGMA quick_check; PRAGMA foreign_key_check;
 SELECT count(*) AS migrations FROM d1_migrations;
 SELECT count(*) AS grants FROM legacy_grants;
 SELECT count(*) AS claims FROM legacy_claims;
 SELECT count(*) AS policies FROM legacy_policies;
 SELECT count(*) AS events FROM legacy_review_events;
 SELECT count(*) AS admin_indexes FROM sqlite_master WHERE type='index' AND name LIKE 'legacy_admin_%';`;
const inspected=invoke(['d1','execute','DB','--command',sql,'--json']);
// Also exercise the exact read-only inventory SQL used for remote prechecks.
invoke(['d1','execute','DB','--command',readFileSync(resolve(root,'scripts/admin-prod-readiness.sql'),'utf8').replace(/^--.*$/mg,'').trim(),'--json']);
const results=JSON.parse(inspected.slice(inspected.indexOf('[')));
const values=results.flatMap(item=>item.results??[]);
for(const [field,value] of Object.entries({quick_check:'ok',migrations:9,grants:0,claims:0,policies:0,events:0,admin_indexes:4}))
  if(!values.some(item=>item[field]===value))throw Error('LOCAL_INTEGRITY_FAILED: '+field);
writeFileSync(resolve(out,'validation.json'),JSON.stringify({local:true,synthetic:true,passes:2,results},null,2));
writeFileSync(resolve(out,'apply-first.log'),first);writeFileSync(resolve(out,'apply-second.log'),second);
console.log(JSON.stringify({local:true,synthetic:true,migrations:9,appliedTwice:true,grants:0,claims:0,policies:0,adminIndexes:4,quickCheck:'ok'}));
