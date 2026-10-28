// Explicit, one-use browser handoff. Firebase tokens remain in memory/stdin only.
import http from 'node:http';
import {randomBytes,timingSafeEqual} from 'node:crypto';
import {spawn} from 'node:child_process';
import {writeFile} from 'node:fs/promises';
import {resolve,dirname} from 'node:path';
import {fileURLToPath,pathToFileURL} from 'node:url';
const root=resolve(dirname(fileURLToPath(import.meta.url)),'..');
export const allowedOrigin='https://calcmot.com.br';
export const databaseId='d0232771-b6e7-49a1-ae9f-05267fbdf3a1';
export const policyVersion='legacy-through-3.5-vc12';
export function createPolicyReceiver({apply,nonce=randomBytes(32).toString('hex'),now=Date.now,ttl=600000,closeAfterUse=false}) {
  const created=now();let consumed=false,busy=false,receipt=null;
  const server=http.createServer(async(req,res)=>{
    res.setHeader('Cache-Control','no-store');res.setHeader('X-Content-Type-Options','nosniff');
    res.setHeader('Referrer-Policy','no-referrer');
    const reply=(status,value)=>{res.writeHead(status,{'Content-Type':'application/json'});res.end(JSON.stringify(value));};
    const port=server.address()?.port;
    if(req.headers.host!==`127.0.0.1:${port}`)return reply(403,{code:'FORBIDDEN'});
    if(req.method==='GET'&&req.url==='/'){
      res.setHeader('Content-Security-Policy',"default-src 'none'; style-src 'unsafe-inline'; frame-ancestors 'none'; base-uri 'none'");
      res.writeHead(200,{'Content-Type':'text/html; charset=utf-8'});
      res.end(`<!doctype html><html lang="pt-BR"><meta charset="utf-8"><title>Política CalcMot PROD</title><h1>Registro privado da política PROD</h1><p>Somente a política aprovada será registrada. Nenhum direito será emitido.</p><a rel="noreferrer" href="${allowedOrigin}/admin#policy-bootstrap=${port}.${nonce}">Autenticar e confirmar no CalcMot</a></html>`);return;
    }
    if(req.headers.origin!==allowedOrigin)return reply(403,{code:'FORBIDDEN'});
    res.setHeader('Access-Control-Allow-Origin',allowedOrigin);res.setHeader('Vary','Origin');
    if(req.method==='OPTIONS'&&['/bootstrap','/challenge'].includes(req.url)){
      res.setHeader('Access-Control-Allow-Methods','POST');res.setHeader('Access-Control-Allow-Headers','Content-Type');
      res.setHeader('Access-Control-Allow-Private-Network','true');res.writeHead(204).end();return;
    }
    if(req.method!=='POST'||!['/bootstrap','/challenge'].includes(req.url))return reply(404,{code:'NOT_FOUND'});
    if(now()-created>=ttl||consumed||busy)return reply(409,{code:'HANDOFF_UNAVAILABLE'});
    if(req.headers['content-type']?.split(';')[0]!=='application/json')return reply(415,{code:'INVALID_REQUEST'});
    req.setTimeout(10000,()=>req.destroy());
    try{
      let body='';for await(const chunk of req){body+=chunk;if(Buffer.byteLength(body)>16384)return reply(413,{code:'INVALID_REQUEST'});}
      const input=JSON.parse(body);body='';
      const challenge=req.url==='/challenge';
      if(!input||Object.keys(input).sort().join(',')!==(challenge?'nonce':'nonce,token')||typeof input.nonce!=='string'||
        (!challenge&&(typeof input.token!=='string'||input.token.length<20||input.token.length>15000))||
        Buffer.byteLength(input.nonce)!==Buffer.byteLength(nonce)||!timingSafeEqual(Buffer.from(input.nonce),Buffer.from(nonce)))
        return reply(403,{code:'FORBIDDEN'});
      if(challenge)return reply(200,{protocolVersion:1,environment:'PROD',policyVersion,grantsWritten:0});
      busy=true;consumed=true;
      try{receipt=await apply(input.token);input.token='';return reply(200,receipt);}
      finally{input.token='';busy=false;if(closeAfterUse)res.once('finish',()=>server.close());}
    }catch{return reply(403,{code:'BOOTSTRAP_NOT_COMPLETED'});}
  });
  return {server,get receipt(){return receipt;}};
}
async function applyPolicy(token){
  const {build}=await import('esbuild');
  const bundled=await build({stdin:{contents:"export {verifySession} from './src/auth.ts'",resolveDir:root,loader:'ts'},
    bundle:true,platform:'node',format:'esm',write:false,tsconfigRaw:{},logLevel:'silent'});
  const {verifySession}=await import('data:text/javascript;base64,'+Buffer.from(bundled.outputFiles[0].contents).toString('base64'));
  const session=await verifySession(new Request(allowedOrigin,{headers:{Authorization:'Bearer '+token}}),'calcmot');
  // Remote authority checks the actual owner allowlist; never trust email or a client UID.
  const response=await fetch(allowedOrigin+'/api/admin/v1/session',{headers:{Authorization:'Bearer '+token,
    'X-CalcMot-Admin':'1',Origin:allowedOrigin},redirect:'error'});
  if(response.status!==200||response.headers.get('X-CalcMot-Environment')!=='PROD'||session.claims.admin!==true)
    throw Error('OWNER_SESSION_REQUIRED');
  const authority=await response.json();
  if(authority.authenticated!==true||authority.environment!=='PROD'||authority.mutationsEnabled!==false||
    authority.contractVersion!=='1.3.0')throw Error('OWNER_SESSION_REQUIRED');
  const input=JSON.stringify({databaseId,policyVersion,token});token='';
  return await new Promise((accept,reject)=>{
    const child=spawn(process.execPath,['scripts/bootstrap-admin-prod-policy.mjs','--apply'],{
      cwd:root,windowsHide:true,env:{...process.env,LEGACY_OWNER_UID:session.uid},stdio:['pipe','pipe','pipe']});
    const timeout=setTimeout(()=>child.kill(),90000);timeout.unref();
    let output='';child.stdout.on('data',chunk=>{output+=chunk;if(output.length>4096)child.kill();});
    child.stderr.on('data',()=>{});child.on('error',()=>reject(Error('BOOTSTRAP_NOT_COMPLETED')));
    child.on('exit',code=>{clearTimeout(timeout);try{
      const result=JSON.parse(output);output='';
      if(code!==0||result.policyOnly!==true||result.environment!=='PROD'||result.grantsWritten!==0||
        result.policyVersion!==policyVersion||!/^policy-[a-f0-9]{24}$/.test(result.operationId))throw Error();
      accept({policyOnly:true,environment:'PROD',operationId:result.operationId,policyVersion,
        policyRecordedAt:result.policyRecordedAt,verifiedAt:result.verifiedAt,
        coreBaseline:result.coreBaseline,grantsWritten:0});
    }catch{reject(Error('BOOTSTRAP_NOT_COMPLETED'));}});
    child.stdin.on('error',()=>{});child.stdin.end(input);
  });
}
if(process.argv[1]&&import.meta.url===pathToFileURL(resolve(process.argv[1])).href){
  const receiver=createPolicyReceiver({apply:async token=>{
    const result=await applyPolicy(token);
    await writeFile(resolve(root,'.tmp/admin-prod-release/policy-receipt.json'),JSON.stringify(result,null,2)+'\n');
    return result;
  },closeAfterUse:true});
  receiver.server.listen(8971,'127.0.0.1',()=>console.log('PRIVATE_POLICY_SESSION_READY http://127.0.0.1:8971/'));
  const timeout=setTimeout(()=>receiver.server.close(),600000);timeout.unref();
  process.on('SIGINT',()=>receiver.server.close());
}
