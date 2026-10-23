// Offline release preparation only: never executes Wrangler, HTTP, SQL or credentials.
import {readFileSync,readdirSync,mkdirSync,writeFileSync} from 'node:fs';
import {resolve,dirname} from 'node:path';
import {fileURLToPath,pathToFileURL} from 'node:url';
import {createHash} from 'node:crypto';
const root=resolve(dirname(fileURLToPath(import.meta.url)),'..');
const digest=file=>createHash('sha256').update(readFileSync(resolve(root,file),'utf8').replace(/\r\n/g,'\n')).digest('hex');
export const DEV_DATABASE_ID='4d0c0c71-c606-45a6-a037-43bccef8c05e';
export const routes=[
  {pattern:'https://calcmot.com.br/api/admin/v1',zone_name:'calcmot.com.br'},
  {pattern:'https://calcmot.com.br/api/admin/v1/*',zone_name:'calcmot.com.br'}
];
export function loadExample(file) {
  return JSON.parse(readFileSync(resolve(root,file),'utf8').replace(/^\/\/.*$/mg,''));
}
export function validateProduction(gateway,worker,api) {
  for(const [item,name] of [[gateway,'calcmot-admin-gateway-prod'],[worker,'calcmot-admin-private-prod'],[api,'calcmot-api-prod']]) {
    if(item.name!==name || item.workers_dev!==false || item.preview_urls!==false || item.routes?.length!==0 ||
      item.observability?.enabled!==false || item.vars?.FIREBASE_PROJECT_ID!=='calcmot')throw Error('UNSAFE_PROD_CONFIG');
    if('LEGACY_OWNER_UID' in item.vars)throw Error('OWNER_MUST_BE_A_SECRET');
  }
  for(const item of [gateway,worker]) {
    if(item.vars.ADMIN_ENVIRONMENT!=='PROD'||item.vars.ADMIN_MUTATIONS_ENABLED!=='false'||
      item.vars.ADMIN_ORIGIN!=='https://calcmot.com.br')throw Error('UNSAFE_PROD_ADMIN');
  }
  if(gateway.d1_databases || gateway.services?.length!==1 || gateway.services[0].binding!=='ADMIN'||
    gateway.services[0].service!==worker.name)throw Error('UNSAFE_PROD_BINDING');
  for(const item of [worker,api]) {
    const db=item.d1_databases;
    if(db?.length!==1 || db[0].binding!=='DB'||db[0].database_name!=='calcmot-identity-prod'||
      db[0].database_id!== 'REPLACE_WITH_VERIFIED_PROD_D1_ID')throw Error('UNSAFE_PROD_DATABASE');
  }
  if(api.vars.ENTITLEMENT_READ_ENABLED!=='false'||api.vars.LEGACY_CLAIMS_ENABLED!=='false')throw Error('UNSAFE_PROD_CLIENT_API');
}
export function validateDatabaseId(id) {
  if(!/^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$/i.test(id)||id.toLowerCase()===DEV_DATABASE_ID)
    throw Error('VERIFIED_DISTINCT_PROD_DATABASE_REQUIRED');
}
export function prepare(databaseId) {
  const gateway=loadExample('wrangler.admin-gateway.prod.example.jsonc');
  const worker=loadExample('wrangler.admin-private.prod.example.jsonc');
  const api=loadExample('wrangler.api.prod.example.jsonc');
  validateProduction(gateway,worker,api);
  const files=readdirSync(resolve(root,'migrations')).filter(f=>f.endsWith('.sql')).sort();
  const expected=['0001_entitlement_snapshots.sql','0002_legacy_grants.sql','0003_legacy_authority.sql',
    '0004_legacy_version_35.sql','0005_legacy_review.sql','0006_legacy_review_issuance.sql',
    '0007_entitlement_source_versions.sql','0008_legacy_privacy.sql','0009_admin_read_indexes.sql'];
  if(JSON.stringify(files)!==JSON.stringify(expected))throw Error('MIGRATION_SET_CHANGED_REVIEW_REQUIRED');
  const migrations=files.map(file=>({file,sha256:digest('migrations/'+file)}));
  const inputs=[];
  const collect=(dir)=>{for(const entry of readdirSync(resolve(root,dir),{withFileTypes:true})) {
    const file=dir+'/'+entry.name;
    if(entry.isDirectory())collect(file);
    else inputs.push({file,sha256:digest(file)});
  }};
  collect('src');
  for(const file of ['package-lock.json','wrangler.admin-gateway.prod.example.jsonc','wrangler.admin-private.prod.example.jsonc','wrangler.api.prod.example.jsonc',
    'scripts/prepare-admin-prod.mjs','scripts/bootstrap-admin-prod-policy.mjs','scripts/check-admin-prod-readiness.mjs',
    'scripts/validate-prod-migrations-local.mjs','scripts/admin-prod-readiness.sql',
    'scripts/assess-admin-cpu.mjs','scripts/admin-cpu-query.graphql'])
    inputs.push({file,sha256:digest(file)});
  if(databaseId)validateDatabaseId(databaseId);
  const out=resolve(root,'.tmp/admin-prod-release');mkdirSync(out,{recursive:true});
  const manifest={preparedAt:new Date().toISOString(),activationAuthorized:false,mutationsEnabled:false,
    databaseId:databaseId??null,migrations,inputs,hashEncoding:'utf8-lf',contractVersion:'1.3.0',environment:'PROD'};
  writeFileSync(resolve(out,'manifest.json'),JSON.stringify(manifest,null,2)+'\n');
  // Do not emit deploy-ready files until an actual new D1 UUID has been checked by the operator.
  if(databaseId) {
    for(const [file,item] of [['gateway.staged.jsonc',gateway],['private.jsonc',worker],['api.staged.jsonc',api]]) {
      item.$schema=resolve(root,'node_modules/wrangler/config-schema.json');
      item.main=resolve(root,item.main);
      if(item.d1_databases){item.d1_databases[0].database_id=databaseId;item.d1_databases[0].migrations_dir=resolve(root,'migrations')}
      writeFileSync(resolve(out,file),JSON.stringify(item,null,2)+'\n');
    }
    writeFileSync(resolve(out,'gateway.cutover.jsonc'),JSON.stringify({...gateway,routes},null,2)+'\n');
  }
  return manifest;
}
if(process.argv[1]&&import.meta.url===pathToFileURL(resolve(process.argv[1])).href) {
  const args=process.argv.slice(2);
  if(args.length!==0 && !(args.length===2&&args[0]==='--database-id'))throw Error('USAGE: node scripts/prepare-admin-prod.mjs [--database-id VERIFIED_UUID]');
  const manifest=prepare(args[1]);
  console.log(JSON.stringify({offline:true,environment:manifest.environment,migrations:manifest.migrations.length,
    deploymentFilesPrepared:manifest.databaseId!==null,mutationsEnabled:false}));
}
