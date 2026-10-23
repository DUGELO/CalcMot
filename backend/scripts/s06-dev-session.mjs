// Private pilot bridge: loopback UI + USB/ADB token handoff. No token/UID on disk or stdout.
import http from 'node:http';
import net from 'node:net';
import {randomBytes} from 'node:crypto';
import {build} from 'esbuild';
import {fileURLToPath} from 'node:url';
import path from 'node:path';
import {executeConsoleCommand} from './legacy-console-command.mjs';
import {openDevBinding} from './legacy-dev-binding.mjs';

const backend=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const port=8966, tokenPort=8967, origin=`http://127.0.0.1:${port}`;
const csrf=randomBytes(24).toString('hex');
let ownerUid=null, token=null, validUntil=0, binding=null;
const bundle=await build({stdin:{contents:"export {verifySession} from './src/auth.ts'; export {LegacyReviewService} from './src/legacy-review.ts'; export {LegacyPrivacyService} from './src/legacy-privacy.ts'; export {operatorReadiness} from './scripts/operator-readiness.ts'",resolveDir:backend,loader:'ts'},
  bundle:true,platform:'node',format:'esm',write:false,logLevel:'silent'});
const api=await import(`data:text/javascript;base64,${Buffer.from(bundle.outputFiles[0].contents).toString('base64')}`);
const {verifySession}=api;
const sanitize=(value)=>Array.isArray(value)?value.map(sanitize):value&&typeof value==='object'?
  Object.fromEntries(Object.entries(value).filter(([k])=>!['uid','actor_uid','actorUid','reviewer','decisionActor','added_by'].includes(k))
    .map(([k,v])=>[k,sanitize(v)])):value;
const status=()=>({ownerConfigured:ownerUid!==null,operatorReady:token!==null&&Date.now()<validUntil,tokenStoredOnDisk:false});
const server=http.createServer(async(req,res)=>{
  res.setHeader('Cache-Control','no-store'); res.setHeader('X-Content-Type-Options','nosniff');
  res.setHeader('Content-Security-Policy',"default-src 'none'; style-src 'unsafe-inline'; form-action 'self'; frame-ancestors 'none'");
  if(req.headers.host!==`127.0.0.1:${port}`) {res.writeHead(403).end();return;}
  if(req.method==='GET'&&req.url==='/') {
    res.setHeader('Content-Type','text/html; charset=utf-8');
    res.end(`<!doctype html><html lang="pt-BR"><meta charset="utf-8"><title>Operador S06 DEV — privado</title>
      <style>body{font:16px system-ui;max-width:720px;margin:32px auto;background:#111820;color:#eee}input,textarea,select,button{font:inherit;margin:8px;padding:10px}textarea{width:90%;height:160px}</style>
      <h1>Operador S06 — somente DEV</h1><p>Console privado neste computador. Não é produção. Não cole tokens neste formulário.</p>
      <p>${JSON.stringify(status())}</p>
      <form method="post" action="/owner"><input type="hidden" name="csrf" value="${csrf}">
      <label>UID do proprietário conferido no Firebase <input type="password" name="uid" required autocomplete="off"></label>
      <button>Configurar UID conferido</button></form>
      <form method="post" action="/command"><input type="hidden" name="csrf" value="${csrf}">
      <label>Comando privado JSON <textarea name="command" required spellcheck="false">{"action":"policy"}</textarea></label>
      <button>Executar comando DEV autorizado</button></form></html>`); return;
  }
  if(req.method==='GET'&&req.url==='/status') {res.setHeader('Content-Type','application/json');res.end(JSON.stringify(status()));return;}
  if(req.method!=='POST'||req.headers.origin&&req.headers.origin!==origin) {res.writeHead(403).end();return;}
  try {
    let body=''; for await(const chunk of req) {body+=chunk;if(Buffer.byteLength(body)>32768)throw new Error('INVALID_REQUEST');}
    const form=new URLSearchParams(body);
    if(form.get('csrf')!==csrf)throw new Error('FORBIDDEN');
    if(req.url==='/owner') {
      const uid=form.get('uid');
      if(ownerUid!==null||!uid||uid.length>128)throw new Error('FORBIDDEN');
      ownerUid=uid; res.setHeader('Content-Type','text/plain; charset=utf-8');res.end('UID configurado somente em memória. Volte para o console.');return;
    }
    if(req.url!=='/command'||!status().operatorReady)throw new Error('AUTH_REQUIRED');
    const command=JSON.parse(form.get('command')??'{}');
    if(!command||typeof command!=='object'||'token'in command||'uid'in command)throw new Error('INVALID_REQUEST');
    const session=await verifySession(new Request('https://local.invalid',{headers:{Authorization:`Bearer ${token}`}}),'calcmot');
    if(api.operatorReadiness(session,'calcmot',ownerUid,'owner',new Date()).status!=='GO')throw new Error('FORBIDDEN');
    binding??=await openDevBinding();
    const result=await executeConsoleCommand(api,{DB:binding.db,FIREBASE_PROJECT_ID:'calcmot',LEGACY_OWNER_UID:ownerUid},session,command);
    res.setHeader('Content-Type','application/json');res.end(JSON.stringify(sanitize(result)));
  }catch(error) {
    const allowed=new Set(['FORBIDDEN','INVALID_REQUEST','CONFLICT','NOT_FOUND','MORE_EVIDENCE_REQUIRED','LINK_REQUIRED','GRANT_PRESERVED','POLICY_REQUIRED']);
    const code=allowed.has(error?.code)?error.code:allowed.has(error?.message)?error.message:'AUTH_REQUIRED_OR_UNAVAILABLE';
    res.writeHead(403,{'Content-Type':'application/json'}).end(JSON.stringify({code}));
  }
});
const handoff=net.createServer(socket=>{
  socket.setTimeout(10000,()=>socket.destroy());let input='';
  socket.on('data',async bytes=>{
    input+=bytes;if(input.length>32768){socket.destroy();return;}
    if(!input.includes('\n'))return;
    socket.pause();
    try {
      const data=JSON.parse(input.trim());input='';
      const session=await verifySession(new Request('https://local.invalid',{headers:{Authorization:`Bearer ${data.token}`}}),'calcmot');
      const now=Date.now()/1000;
      if(!ownerUid||session.uid!==ownerUid||session.claims.admin!==true||typeof session.claims.auth_time!=='number'||
        session.claims.auth_time>now||now-session.claims.auth_time>300)throw new Error('FORBIDDEN');
      token=data.token;validUntil=Math.min(session.claims.auth_time+300,session.claims.exp)*1000;
      socket.end(JSON.stringify({accepted:true})+'\n');
    }catch {token=null;validUntil=0;socket.end(JSON.stringify({accepted:false})+'\n');}
  });
});
await new Promise(resolve=>server.listen(port,'127.0.0.1',resolve));
await new Promise(resolve=>handoff.listen(tokenPort,'127.0.0.1',resolve));
process.stdout.write('S06_PRIVATE_DEV_BRIDGE_READY http://127.0.0.1:8966 tokenPort=8967\n');
const stop=async()=>{token=null;ownerUid=null;server.close();handoff.close();await binding?.dispose();process.exit(0);};
process.on('SIGINT',stop);process.on('SIGTERM',stop);
