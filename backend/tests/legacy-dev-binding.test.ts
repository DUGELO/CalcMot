import {expect,it} from 'vitest';
// Target validation does not instantiate a proxy or authenticate with Cloudflare.
import {assertDevBinding} from '../scripts/legacy-dev-binding.mjs';
const config = () => ({name:'calcmot-api-dev',account_id:'45086fe60ce934025fcac45e65eb4b69',
  vars:{FIREBASE_PROJECT_ID:'calcmot'},d1_databases:[{binding:'DB',database_name:'calcmot-identity-dev',
    database_id:'4d0c0c71-c606-45a6-a037-43bccef8c05e'}]});
it('accepts only the existing DEV account and binding',()=>{
  expect(assertDevBinding(config()).binding).toBe('DB');
  for(const mutate of [
    (c:any)=>c.name='production', (c:any)=>c.account_id='other',
    (c:any)=>c.vars.FIREBASE_PROJECT_ID='other', (c:any)=>c.d1_databases[0].database_id='other',
    (c:any)=>c.d1_databases[0].database_name='prod', (c:any)=>c.d1_databases.push(c.d1_databases[0]),
    (c:any)=>c.routes=['production/*'], (c:any)=>c.services=[{binding:'PROD'}]
  ]) { const c=config(); mutate(c); expect(()=>assertDevBinding(c)).toThrow('DEV_TARGET_REQUIRED'); }
});
