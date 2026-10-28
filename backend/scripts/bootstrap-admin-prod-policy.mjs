// Private one-time policy metadata setup. Never accepts an action, grant or target UID.
// --check is offline. --apply requires a legitimate Firebase owner session on stdin.
import {readFile,mkdtemp,writeFile,rm} from 'node:fs/promises';
import {resolve,dirname} from 'node:path';
import {fileURLToPath} from 'node:url';
import {build} from 'esbuild';
import {createHash} from 'node:crypto';
import {validateDatabaseId,loadExample,validateProduction} from './prepare-admin-prod.mjs';
const root=resolve(dirname(fileURLToPath(import.meta.url)),'..');
let proxy,temporary,mutationAttempted=false;
try {
  const args=process.argv.slice(2);
  if(args.length!==1||!['--check','--apply'].includes(args[0]))throw Error('MODE_REQUIRED');
  validateProduction(loadExample('wrangler.admin-gateway.prod.example.jsonc'),
    loadExample('wrangler.admin-private.prod.example.jsonc'),loadExample('wrangler.api.prod.example.jsonc'));
  if(args[0]==='--check') {
    console.log(JSON.stringify({offline:true,applied:false,policyOnly:true,grantsWritten:0}));
  }else {
    const config=JSON.parse(await readFile(resolve(root,'.tmp/admin-prod-release/private.jsonc'),'utf8'));
    const binding=config.d1_databases?.[0];validateDatabaseId(binding?.database_id??'');
    if(config.name!=='calcmot-admin-private-prod'||config.account_id!=='45086fe60ce934025fcac45e65eb4b69'||
      config.vars?.FIREBASE_PROJECT_ID!=='calcmot'||config.vars.ADMIN_ENVIRONMENT!=='PROD'||
      config.vars.ADMIN_MUTATIONS_ENABLED!=='false'||config.routes?.length||config.workers_dev!==false||
      config.preview_urls!==false||config.d1_databases.length!==1||binding.database_name!=='calcmot-identity-prod')
      throw Error('PROD_TARGET_REQUIRED');
    const owner=process.env.LEGACY_OWNER_UID;
    if(!owner||owner.length>128)throw Error('OWNER_CONFIGURATION_REQUIRED');
    let text='';for await(const chunk of process.stdin){text+=chunk;if(Buffer.byteLength(text)>16384)throw Error('INVALID_INPUT')}
    const input=JSON.parse(text);text='';
    if(Object.keys(input).sort().join(',')!=='databaseId,policyVersion,token'||typeof input.token!=='string'||
      input.databaseId!==binding.database_id||input.policyVersion!=='legacy-through-3.5-vc12')throw Error('CONFIRMATION_REQUIRED');
    const bundled=await build({stdin:{contents:"export {verifySession} from './src/auth.ts'; export {LegacyReviewService} from './src/legacy-review.ts'; export {operatorReadiness} from './scripts/operator-readiness.ts';",
      resolveDir:root,loader:'ts'},absWorkingDir:root,tsconfigRaw:{},bundle:true,platform:'node',format:'esm',write:false,logLevel:'silent'});
    const api=await import('data:text/javascript;base64,'+Buffer.from(bundled.outputFiles[0].contents).toString('base64'));
    const session=await api.verifySession(new Request('https://calcmot.com.br',{headers:{Authorization:'Bearer '+input.token}}),'calcmot');
    delete input.token;
    if(api.operatorReadiness(session,'calcmot',owner,'owner').status!=='GO')throw Error('OWNER_SESSION_REQUIRED');
    const linked=session.claims.firebase?.identities?.['google.com'];
    if(!Array.isArray(linked)||!linked.some(id=>typeof id==='string'&&id.length>0))throw Error('OWNER_SESSION_REQUIRED');
    // No authenticated remote DB connection exists before all identity checks above.
    temporary=await mkdtemp(resolve(root,'.tmp/prod-policy-bootstrap-'));
    const configPath=resolve(temporary,'wrangler.jsonc');
    await writeFile(configPath,JSON.stringify({name:'private-prod-policy-bootstrap',account_id:config.account_id,
      compatibility_date:config.compatibility_date,workers_dev:false,main:resolve(root,'src/index.ts'),
      d1_databases:[{...binding,remote:true,migrations_dir:resolve(root,'migrations')}]}));
    const {getPlatformProxy}=await import('wrangler');
    proxy=await getPlatformProxy({configPath,envFiles:[],persist:false,remoteBindings:true});
    mutationAttempted=true;
    const result=await new api.LegacyReviewService({DB:proxy.env.DB,FIREBASE_PROJECT_ID:'calcmot',LEGACY_OWNER_UID:owner}).authorizePolicy(session);
    const recorded=await proxy.env.DB.prepare(`SELECT approved_at FROM legacy_policies
      WHERE firebase_project_id=? AND policy_version=? AND approved_by=? AND core_baseline=?`)
      .bind('calcmot',result.policyVersion,owner,result.coreBaseline).first();
    if(!recorded?.approved_at||!Number.isFinite(Date.parse(recorded.approved_at)))throw Error('POLICY_RECEIPT_UNCONFIRMED');
    // Stable receipt derived only from non-personal policy metadata, not a forged history record.
    const operationId='policy-'+createHash('sha256').update(JSON.stringify(['calcmot',result.policyVersion,recorded.approved_at])).digest('hex').slice(0,24);
    console.log(JSON.stringify({policyOnly:true,environment:'PROD',operationId,policyVersion:result.policyVersion,
      policyRecordedAt:recorded.approved_at,verifiedAt:new Date().toISOString(),coreBaseline:result.coreBaseline,grantsWritten:0}));
  }
}catch(error) {
  const codes=new Set(['MODE_REQUIRED','PROD_TARGET_REQUIRED','OWNER_CONFIGURATION_REQUIRED','INVALID_INPUT','CONFIRMATION_REQUIRED','OWNER_SESSION_REQUIRED','FORBIDDEN','POLICY_REQUIRED']);
  const code=codes.has(error?.code)?error.code:codes.has(error?.message)?error.message:'BOOTSTRAP_NOT_COMPLETED';
  console.error(JSON.stringify({code,applied:mutationAttempted?null:false}));process.exitCode=1;
}finally{await proxy?.dispose();if(temporary)await rm(resolve(temporary,'wrangler.jsonc'),{force:true})}
