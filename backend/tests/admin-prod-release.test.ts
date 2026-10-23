import {it,expect} from 'vitest';
import {spawnSync} from 'node:child_process';
// @ts-expect-error Offline operator script intentionally has no production runtime dependency.
import {loadExample,validateProduction,validateDatabaseId,DEV_DATABASE_ID,routes} from '../scripts/prepare-admin-prod.mjs';
it('keeps prepared PROD isolated and commercially inactive',()=>{
  const gateway=loadExample('wrangler.admin-gateway.prod.example.jsonc');
  const worker=loadExample('wrangler.admin-private.prod.example.jsonc');
  const api=loadExample('wrangler.api.prod.example.jsonc');
  expect(()=>validateProduction(gateway,worker,api)).not.toThrow();
  for(const bad of [
    {...gateway,workers_dev:true}, {...gateway,routes},
    {...gateway,d1_databases:worker.d1_databases},
    {...gateway,services:[{binding:'ADMIN',service:'calcmot-admin-private-dev'}]},
    {...gateway,vars:{...gateway.vars,ADMIN_ENVIRONMENT:'DEV'}},
    {...gateway,vars:{...gateway.vars,ADMIN_MUTATIONS_ENABLED:'true'}},
    {...gateway,vars:{...gateway.vars,LEGACY_OWNER_UID:'must-not-be-in-config'}}
  ])expect(()=>validateProduction(bad,worker,api)).toThrow();
  expect(()=>validateProduction(gateway,{...worker,d1_databases:[{...worker.d1_databases[0],database_id:DEV_DATABASE_ID}]},api)).toThrow();
});
it('requires a distinct verified D1 UUID and limits cutover to the two administrative API routes',()=>{
  for(const id of [DEV_DATABASE_ID,DEV_DATABASE_ID.toUpperCase(),'REPLACE_WITH_VERIFIED_PROD_D1_ID','', 'not-a-uuid'])
    expect(()=>validateDatabaseId(id)).toThrow();
  expect(()=>validateDatabaseId('11111111-2222-3333-4444-555555555555')).not.toThrow();
  expect(routes.map((r:{pattern:string})=>r.pattern)).toEqual([
    'https://calcmot.com.br/api/admin/v1','https://calcmot.com.br/api/admin/v1/*']);
});
it('checks policy bootstrap offline and refuses arbitrary operation modes',()=>{
  const checked=spawnSync(process.execPath,['scripts/bootstrap-admin-prod-policy.mjs','--check'],{encoding:'utf8',windowsHide:true});
  expect(checked.status).toBe(0);
  expect(JSON.parse(checked.stdout)).toEqual({offline:true,applied:false,policyOnly:true,grantsWritten:0});
  const invalid=spawnSync(process.execPath,['scripts/bootstrap-admin-prod-policy.mjs','--grant'],{encoding:'utf8',windowsHide:true});
  expect(invalid.status).toBe(1);expect(JSON.parse(invalid.stderr)).toEqual({code:'MODE_REQUIRED',applied:false});
});
