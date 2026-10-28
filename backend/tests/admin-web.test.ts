import {beforeAll,afterAll,it,expect,vi} from 'vitest';
import {Miniflare,Response as LocalResponse} from 'miniflare';
import {build} from 'esbuild';
import {readFileSync,readdirSync} from 'node:fs';
import {generateKeyPair,exportJWK,createLocalJWKSet,SignJWT} from 'jose';
import {handleAdmin,type AdminEnv} from '../src/admin/api';
import {handleGateway} from '../src/admin/gateway';
import {AdminRateGuard} from '../src/admin/security';
import {LegacyReviewService,type DecisionInput} from '../src/legacy-review';
import {readEntitlement} from '../src/entitlement';
import type {Session} from '../src/auth';

vi.setConfig({testTimeout:30000});
const origin='https://admin.synthetic.invalid',project='calcmot-admin-synthetic',ownerUid='synthetic-owner';
let mf:Miniflare,prodMf:Miniflare,db:D1Database,prodDb:D1Database,env:AdminEnv,review:LegacyReviewService;
let pair:Awaited<ReturnType<typeof generateKeyPair>>,keys:ReturnType<typeof createLocalJWKSet>;
let ownerToken:string;let serial=0;const key=()=>`admin-synthetic-${++serial}`;
const config={FIREBASE_PROJECT_ID:project,LEGACY_OWNER_UID:ownerUid,ADMIN_ORIGIN:origin,
  ADMIN_REQUESTS_PER_MINUTE:'9999',ADMIN_MUTATIONS_PER_MINUTE:'9999'};
const identity=(uid:string,p=project,admin=false):Session=>({uid,claims:{sub:uid,aud:p,iss:`https://securetoken.google.com/${p}`,
  admin,auth_time:Math.floor(Date.now()/1000)-5,firebase:{identities:{'google.com':['synthetic-google']}}}});
async function token(uid=ownerUid,admin=true,options:{project?:string;age?:number;expired?:boolean}={}) {
  const now=Math.floor(Date.now()/1000),p=options.project??project;
  return new SignJWT({sub:uid,aud:p,iss:`https://securetoken.google.com/${p}`,admin,iat:now-10,
    auth_time:now-(options.age??15),exp:options.expired?now-1:now+3600,
    firebase:{identities:{'google.com':['synthetic-google']}}}).setProtectedHeader({alg:'RS256',kid:'admin-test'}).sign(pair.privateKey);
}
function req(path:string,method='GET',body?:unknown,extra:Record<string,string>={},bearer=ownerToken) {
  return new Request(origin+'/api/admin/v1'+path,{method,headers:{Authorization:`Bearer ${bearer}`,
    Origin:origin,'X-CalcMot-Admin':'1',...(body!==undefined?{'Content-Type':'application/json','If-Match':'"0"','Idempotency-Key':key()}:{}),...extra},
    ...(body!==undefined?{body:typeof body==='string'?body:JSON.stringify(body)}:{})});
}
async function call(path:string,method='GET',body?:unknown,headers:Record<string,string>={}) {
  return handleAdmin(req(path,method,body,headers),env,keys,new AdminRateGuard());
}
async function open(p=project) {
  const session=identity(key(),p);
  const service=new LegacyReviewService({...env,FIREBASE_PROJECT_ID:p});
  return {session,view:await service.open(session,{evidenceType:'USER_DECLARATION',evidenceHash:null},key())};
}
const decision=(outcome:DecisionInput['outcome'],evidenceIds:string[]=[]):DecisionInput=>({outcome,evidenceIds,
  reason:'Synthetic local owner assessment only.',ownershipConfirmed:true,contradictionsCleared:true,legacyUseConcluded:true});
beforeAll(async()=>{
  pair=await generateKeyPair('RS256');const jwk={...await exportJWK(pair.publicKey),kid:'admin-test',alg:'RS256'};
  keys=createLocalJWKSet({keys:[jwk]});ownerToken=await token();
  const bundle=async(file:string)=>(await build({entryPoints:[file],bundle:true,platform:'browser',format:'esm',write:false,
    external:['cloudflare:workers'],logLevel:'silent'})).outputFiles[0].text;
  mf=new Miniflare({workers:[
    {name:'gateway',modules:true,script:await bundle('src/admin/gateway.ts'),compatibilityDate:'2026-07-01',
      bindings:config,serviceBindings:{ADMIN:'private'},outboundService:()=>LocalResponse.json({keys:[jwk]})},
    {name:'private',modules:true,script:await bundle('src/admin/worker.ts'),compatibilityDate:'2026-07-01',
      bindings:config,d1Databases:{DB:'admin-web-local-synthetic-only'},outboundService:()=>LocalResponse.json({keys:[jwk]})}
  ]});
  prodMf=new Miniflare({workers:[
    {name:'prod-gateway',modules:true,script:await bundle('src/admin/gateway.ts'),compatibilityDate:'2026-07-01',
      bindings:{...config,ADMIN_ENVIRONMENT:'PROD',ADMIN_MUTATIONS_ENABLED:'false'},serviceBindings:{ADMIN:'prod-private'},
      outboundService:()=>LocalResponse.json({keys:[jwk]})},
    {name:'prod-private',modules:true,script:await bundle('src/admin/worker.ts'),compatibilityDate:'2026-07-01',
      bindings:{...config,ADMIN_ENVIRONMENT:'PROD',ADMIN_MUTATIONS_ENABLED:'false'},d1Databases:{DB:'admin-prod-local-synthetic-only'},
      outboundService:()=>LocalResponse.json({keys:[jwk]})}
  ]});
  db=await mf.getD1Database('DB','private') as unknown as D1Database;env={...config,DB:db};review=new LegacyReviewService(env);
  prodDb=await prodMf.getD1Database('DB','prod-private') as unknown as D1Database;
  for(const file of readdirSync('migrations').filter(f=>f.endsWith('.sql')).sort()) {
    const sql=readFileSync('migrations/'+file,'utf8').replace(/--[^\n]*/g,'');
    const triggers=[...sql.matchAll(/CREATE TRIGGER[\s\S]*?END;/g)].map(m=>m[0]);
    const rest=sql.replace(/CREATE TRIGGER[\s\S]*?END;/g,'').split(';').map(x=>x.trim()).filter(Boolean);
    for(const target of [db,prodDb]) {
      if(rest.length)await target.batch(rest.map(s=>target.prepare(s)));
      if(triggers.length)await target.batch(triggers.map(s=>target.prepare(s)));
    }
  }
  await review.authorizePolicy(identity(ownerUid,project,true));
},90000);
afterAll(async()=>{await Promise.all([mf?.dispose(),prodMf?.dispose()])});

it('denies anonymous, invalid, expired, ordinary, another admin and wrong project before D1',async()=>{
  const parts=ownerToken.split('.');
  parts[1]=Buffer.from(JSON.stringify({...JSON.parse(Buffer.from(parts[1],'base64url').toString()),sub:'forged-owner'})).toString('base64url');
  const forged=parts.join('.');
  let reads=0;const noDb={...env,DB:{prepare(){reads++;throw Error('DB must not be reached')}} as unknown as D1Database};
  for(const [credential,status] of [['invalid',401],[forged,401],[await token('ordinary',false),403],
    [await token(ownerUid,false),403],[await token('other-admin',true),403],[await token(ownerUid,true,{expired:true}),401],
    [await token(ownerUid,true,{project:'other-project'}),401]] as const)
    expect((await handleAdmin(req('/claims','GET',undefined,{},credential),noDb,keys)).status).toBe(status);
  const anonymous=req('/claims');anonymous.headers.delete('Authorization');
  expect((await handleAdmin(anonymous,noDb,keys)).status).toBe(401);expect(reads).toBe(0);
});
it('stages PROD read-only and rejects environment mismatch before database access or mutation',async()=>{
  let touched=0;
  const dbTrap={prepare(){touched++;throw Error('must not access D1')}} as unknown as D1Database;
  const prod={...env,DB:dbTrap,ADMIN_ENVIRONMENT:'PROD' as const,ADMIN_MUTATIONS_ENABLED:'false' as const};
  const gateway={...prod,ADMIN:{handle:(r:Request)=>handleAdmin(r,prod,keys,new AdminRateGuard())}};
  const session=await handleGateway(req('/session'),gateway,keys,new AdminRateGuard());
  expect(session.status).toBe(200);expect(session.headers.get('X-CalcMot-Environment')).toBe('PROD');
  expect(await session.json()).toMatchObject({environment:'PROD',contractVersion:'1.3.0',mutationsEnabled:false});
  for(const flag of [undefined,'false'] as const) {
    const closed={...prod,ADMIN_MUTATIONS_ENABLED:flag};
    const r=req('/claims/synthetic/decisions','POST',decision('REJECT'));
    r.headers.set('X-CalcMot-Target-Environment','PROD');
    expect((await handleAdmin(r,closed,keys)).status).toBe(503);
    expect((await handleGateway(req('/claims/synthetic/decisions','POST',decision('REJECT')),
      {...gateway,ADMIN_MUTATIONS_ENABLED:flag},keys,new AdminRateGuard())).status).toBe(503);
  }
  // Even with a future activation flag, a wrong service binding must fail before D1.
  for(const [outer,inner] of [['PROD','DEV'],['DEV','PROD']] as const) {
    const result=await handleGateway(req('/claims/synthetic/decisions','POST',decision('REJECT')),
      {...config,ADMIN_ENVIRONMENT:outer,ADMIN_MUTATIONS_ENABLED:'true',ADMIN:{handle:(r:Request)=>
        handleAdmin(r,{...prod,ADMIN_ENVIRONMENT:inner,ADMIN_MUTATIONS_ENABLED:'true'},keys)}},keys,new AdminRateGuard());
    expect(result.status).toBe(503);expect(result.headers.get('X-CalcMot-Environment')).toBe(outer);
  }
  const noTarget=await handleAdmin(req('/session'),prod,keys);expect(noTarget.status).toBe(503);
  for(const expected of [undefined,'DEV']) {
    const r=req('/claims/synthetic/decisions','POST',decision('REJECT'));
    r.headers.set('X-CalcMot-Target-Environment','PROD');
    if(expected)r.headers.set('X-CalcMot-Expected-Environment',expected);
    // Protect old DEV tabs even after a separately approved future activation.
    expect((await handleAdmin(r,{...prod,ADMIN_MUTATIONS_ENABLED:'true'},keys)).status).toBe(503);
  }
  const invalid={...prod,ADMIN_ENVIRONMENT:'STAGING'} as unknown as AdminEnv;
  expect((await handleAdmin(req('/session'),invalid,keys)).status).toBe(503);
  expect(touched).toBe(0);
});
it('distinguishes fresh session from stale reauthentication and never refreshes auth_time by GET',async()=>{
  const stale=await token(ownerUid,true,{age:301});
  const result=await handleAdmin(req('/session','GET',undefined,{},stale),env,keys);
  expect(result.status).toBe(200);expect(await result.json()).toMatchObject({contractVersion:'1.3.0',environment:'DEV',authenticated:true,reauthenticationRequired:true});
  const gateway={...config,ADMIN:{handle:(r:Request)=>handleAdmin(r,env,keys,new AdminRateGuard())}};
  for(const path of ['/claims','/claims/counts']) {
    expect((await handleAdmin(req(path,'GET',undefined,{},stale),env,keys)).status).toBe(200);
    expect((await handleGateway(req(path,'GET',undefined,{},stale),gateway,keys,new AdminRateGuard())).status).toBe(200);
  }
  for(const section of ['evidence','decisions','issuance']) {
    const request=()=>req('/claims/synthetic-case/'+section,'POST',{}, {},stale);
    for(const denied of [await handleAdmin(request(),env,keys),await handleGateway(request(),gateway,keys,new AdminRateGuard())]) {
      expect(denied.status).toBe(403);expect(await denied.json()).toMatchObject({code:'REAUTH_REQUIRED'});
    }
  }
});
it('gateway rejects cross-origin, cookies-only, missing marker, and cannot be bypassed with fake admin headers',async()=>{
  let forwarded=0;const gateway={...config,ADMIN:{async handle(r:Request){forwarded++;
    expect(r.headers.has('Cookie')).toBe(false);
    return handleAdmin(r,env,keys)}}};
  for(const mutate of [
    (r:Request)=>r.headers.set('Origin','https://evil.invalid'),
    (r:Request)=>r.headers.delete('X-CalcMot-Admin'),
    (r:Request)=>r.headers.set('Sec-Fetch-Site','cross-site'),
    (r:Request)=>{r.headers.delete('Authorization');r.headers.set('Cookie','admin=true');r.headers.set('X-Admin','true')}
  ]){const r=req('/claims');mutate(r);expect((await handleGateway(r,gateway,keys,new AdminRateGuard())).status).toBeGreaterThanOrEqual(400)}
  expect(forwarded).toBe(0);
  const withoutOrigin=req('/claims/synthetic-case/decisions','POST',decision('REQUEST_MORE'));
  withoutOrigin.headers.delete('Origin');
  expect((await handleGateway(withoutOrigin,gateway,keys,new AdminRateGuard())).status).toBe(403);
  const good=req('/session');good.headers.delete('Origin');good.headers.set('Cookie','irrelevant=synthetic-cookie');
  expect((await handleGateway(good,gateway,keys,new AdminRateGuard())).status).toBe(200);
  expect(forwarded).toBe(1);
  expect((await handleGateway(req('/claims','OPTIONS'),gateway,keys,new AdminRateGuard())).headers.has('Access-Control-Allow-Origin')).toBe(false);
});
it('rate limits and missing configuration fail closed without paid services',async()=>{
  const low={...env,ADMIN_REQUESTS_PER_MINUTE:'1'};const rate=new AdminRateGuard();
  expect((await handleAdmin(req('/session'),low,keys,rate)).status).toBe(200);
  const limited=await handleAdmin(req('/session'),low,keys,rate);
  expect(limited.status).toBe(429);expect(limited.headers.get('Retry-After')).toBe('60');
  expect((await handleAdmin(req('/session'),{...env,LEGACY_OWNER_UID:undefined},keys)).status).toBe(503);
});
it('gateway never follows redirects or exposes CORS, and private authorization is independent',async()=>{
  const redirect={...config,ADMIN:{async handle(){return new Response(null,{status:302,headers:{Location:'https://evil.invalid'}})}}};
  const result=await handleGateway(req('/claims'),redirect,keys,new AdminRateGuard());
  expect(result.status).toBe(503);expect(result.headers.has('Location')).toBe(false);
  expect(result.headers.has('Access-Control-Allow-Origin')).toBe(false);
  const fakeGateway=req('/claims','GET',undefined,{'X-Admin':'true'},await token('ordinary',false));
  expect((await handleAdmin(fakeGateway,env,keys,new AdminRateGuard())).status).toBe(403);
});
it('gateway does not dispatch public site paths or foreign hostnames to the private service',async()=>{
  let forwarded=0;const gateway={...config,ADMIN:{async handle(){forwarded++;return new Response('{}')}}};
  for(const path of ['/','/calculadora','/admin','/api/admin/v10/claims']) {
    const r=new Request(origin+path,{headers:req('/session').headers});
    expect((await handleGateway(r,gateway,keys,new AdminRateGuard())).status).toBe(404);
  }
  const foreign=new Request('https://other.invalid/api/admin/v1/session',{headers:req('/session').headers});
  expect((await handleGateway(foreign,gateway,keys,new AdminRateGuard())).status).toBe(403);
  expect(forwarded).toBe(0);
});
it('review deployment examples target only the official API paths and the existing DEV database',()=>{
  const read=(name:string)=>JSON.parse(readFileSync(name,'utf8').replace(/^\s*\/\/.*$/gm,''));
  const gateway=read('wrangler.admin-gateway.dev.example.jsonc');
  const privateWorker=read('wrangler.admin-private.dev.example.jsonc');
  expect(gateway.name).toBe('calcmot-admin-gateway-dev');
  expect(gateway.routes).toEqual([
    {pattern:'https://calcmot.com.br/api/admin/v1',zone_name:'calcmot.com.br'},
    {pattern:'https://calcmot.com.br/api/admin/v1/*',zone_name:'calcmot.com.br'}]);
  expect(gateway.services).toEqual([{binding:'ADMIN',service:'calcmot-admin-private-dev'}]);
  expect(gateway.d1_databases).toBeUndefined();
  expect(privateWorker.routes).toEqual([]);
  expect(privateWorker.d1_databases[0]).toMatchObject({binding:'DB',database_name:'calcmot-identity-dev',
    database_id:'4d0c0c71-c606-45a6-a037-43bccef8c05e'});
  for(const config of [gateway,privateWorker]) {
    expect(config.workers_dev).toBe(false);expect(config.preview_urls).toBe(false);
    expect(config.vars.ADMIN_ORIGIN).toBe('https://calcmot.com.br');
    expect(config.vars.LEGACY_OWNER_UID).toBeUndefined();
    expect(config.account_id).toBe('45086fe60ce934025fcac45e65eb4b69');
  }
});
it('needs no edge cookie or Access configuration; Firebase owner is sufficient',async()=>{
  const gateway={...config,ADMIN:{handle:(r:Request)=>handleAdmin(r,env,keys,new AdminRateGuard())}};
  const result=await handleGateway(req('/claims'),gateway,keys,new AdminRateGuard());
  expect(result.status).toBe(200);
  expect(result.headers.get('Cache-Control')).toBe('no-store');
});
it('paginates and filters on server, exact protocol search and counts are project-scoped',async()=>{
  const cases=[];for(let n=0;n<4;n++)cases.push(await open());
  const foreign=await open('foreign-synthetic-project');
  const first=await(await call('/claims?limit=2')).json() as any;
  expect(first.items).toHaveLength(2);expect(first.nextCursor).toBeTruthy();
  const second=await(await call('/claims?limit=2&cursor='+first.nextCursor)).json() as any;
  expect(second.items).toHaveLength(2);expect(new Set([...first.items,...second.items].map(x=>x.claimId)).size).toBe(4);
  const found=await(await call('/claims?protocol='+cases[0].view.claimId)).json() as any;
  expect(found.items.map((x:any)=>x.claimId)).toEqual([cases[0].view.claimId]);
  expect((await call('/claims/'+foreign.view.claimId)).status).toBe(404);
  expect((await(await call('/claims/counts')).json() as any).counts.UNDER_REVIEW).toBe(4);
  expect(JSON.stringify(first)).not.toContain('uid');
  for(const suffix of ['?limit=51','?limit=01','?limit=2&limit=3','?uid=fake','?protocol=%25','?state=APPROVED',
    '?cursor=invalid','?state=GRANTED&cursor='+first.nextCursor])expect((await call('/claims'+suffix)).status).toBe(400);
});
it('requires revision/idempotency, bounds body and rejects duplicate fields',async()=>{
  const c=await open();const path='/claims/'+c.view.claimId+'/decisions';
  const missing=req(path,'POST',decision('REQUEST_MORE'));missing.headers.delete('If-Match');
  expect((await handleAdmin(missing,env,keys)).status).toBe(428);
  expect((await call(path,'POST',decision('REQUEST_MORE'),{'If-Match':'W/"0"'})).status).toBe(400);
  expect((await call(path,'POST','x'.repeat(8193))).status).toBe(413);
  expect((await call(path,'POST',decision('REQUEST_MORE'),{'Content-Type':'text/plain'})).status).toBe(415);
  expect((await call(path,'POST','{"outcome":"APPROVE","outcome":"REJECT"}')).status).toBe(400);
  expect((await call(path,'POST',{...decision('REJECT'),uid:'forged'})).status).toBe(400);
});
it('complementation and rejection are audited, paginated, replay-safe, and never issue grants',async()=>{
  const c=await open(),path='/claims/'+c.view.claimId;const idempotency=key();
  const more=await call(path+'/decisions','POST',decision('REQUEST_MORE'),{'Idempotency-Key':idempotency});expect(more.status).toBe(200);
  const replay=await call(path+'/decisions','POST',decision('REQUEST_MORE'),{'Idempotency-Key':idempotency});expect(replay.status).toBe(200);
  expect((await call(path+'/decisions','POST',decision('REJECT'),{'Idempotency-Key':idempotency})).status).toBe(409);
  expect((await call(path+'/decisions','POST',decision('REJECT'))).status).toBe(409);
  expect((await call(path+'/decisions','POST',decision('REJECT'),{'If-Match':'"1"'})).status).toBe(200);
  const inspected=await(await call(path)).json() as any;expect(inspected.view.state).toBe('REJECTED');expect(inspected.grant).toBeNull();
  const events=await(await call(path+'/events?limit=1')).json() as any;
  expect(events.items[0].kind).toBe('REQUEST_MORE');expect(events.nextCursor).toBeTruthy();
  const next=await(await call(path+'/events?limit=1&cursor='+events.nextCursor)).json() as any;expect(next.items[0].kind).toBe('REJECT');
  const serialized=JSON.stringify([inspected,events,next]);expect(serialized).not.toMatch(/actor_uid|operation_key_hash|request_hash|synthetic-owner/);
});

it('reopens rejected cases only through an audited current REQUEST_MORE without granting rights',async()=>{
  const c=await open(),path='/claims/'+c.view.claimId;
  expect((await call(path+'/decisions','POST',decision('REJECT'))).status).toBe(200);
  expect((await call(path+'/decisions','POST',decision('APPROVE'),{'If-Match':'"1"'})).status).toBe(409);
  const input=decision('REQUEST_MORE'),headers={'If-Match':'"1"','Idempotency-Key':key()};
  const reopened=await call(path+'/decisions','POST',input,headers);
  expect(reopened.status).toBe(200);
  expect(await reopened.json()).toMatchObject({view:{state:'UNDER_REVIEW',revision:2},decision:{kind:'REQUEST_MORE'}});
  expect((await call(path+'/decisions','POST',input,headers)).status).toBe(200);
  expect((await call(path+'/decisions','POST',input,{'If-Match':'"1"'})).status).toBe(409);
  const detail=await(await call(path)).json() as any;
  expect(detail.grant).toBeNull();expect(detail.view.revision).toBe(2);
  const audit=await(await call(path+'/events')).json() as any;
  expect(audit.items.map((e:any)=>e.kind)).toEqual(['REJECT','REQUEST_MORE']);
});

it('actual workerd RPC binding completes assisted review and idempotent issuance; private HTTP always denies',async()=>{
  const c=await open(),path='/claims/'+c.view.claimId;
  const rpc=async(suffix:string,method='GET',input?:unknown,headers:Record<string,string>={})=>{
    const r=req(suffix,method,input,headers);return mf.dispatchFetch(r.url,{method:r.method,headers:Object.fromEntries(r.headers),
      ...(input!==undefined?{body:JSON.stringify(input)}:{})});
  };
  const directRequest=req(path);
  const direct=await (await mf.getWorker('private')).fetch(directRequest.url,{headers:Object.fromEntries(directRequest.headers)});expect(direct.status).toBe(403);
  const a=await rpc(path+'/evidence','POST',{kind:'AUXILIARY',category:'SUPPORT_RECORD',originKey:key(),reference:'synthetic:support'});expect(a.status).toBe(200);
  const b=await rpc(path+'/evidence','POST',{kind:'AUXILIARY',category:'INSTALL_CONTEXT',originKey:key(),reference:'synthetic:install'},{'If-Match':'"1"'});expect(b.status).toBe(200);
  const evidence=await(await rpc(path+'/evidence')).json() as any;expect(evidence.items).toHaveLength(2);
  const approved=await rpc(path+'/decisions','POST',decision('APPROVE',evidence.items.map((e:any)=>e.evidenceId)),{'If-Match':'"2"'});
  expect(approved.status).toBe(200);const approval=await approved.json() as any;expect(approval.view.state).toBe('UNDER_REVIEW');
  expect((await(await rpc(path)).json() as any).grant).toBeNull();
  const input={approvalEventId:approval.decision.eventId,confirmation:{claimId:c.view.claimId,permanentCoreOnly:true}};
  const headers={'If-Match':'"3"','Idempotency-Key':key()};
  expect((await rpc(path+'/issuance','POST',{...input,confirmation:{...input.confirmation,claimId:'wrong'}} ,headers)).status).toBe(400);
  // Induced DB failure after receipt insertion must roll everything back.
  await db.prepare(`CREATE TRIGGER admin_synthetic_fail BEFORE INSERT ON legacy_grants WHEN NEW.uid='${c.session.uid}'
    BEGIN SELECT RAISE(ABORT,'controlled local failure'); END`).run();
  const failed=await rpc(path+'/issuance','POST',input,headers);expect(failed.status).toBe(503);
  expect((await failed.text())).not.toContain('controlled local failure');
  expect(await db.prepare('SELECT 1 FROM legacy_review_issuances WHERE claim_id=?').bind(c.view.claimId).first()).toBeNull();
  await db.prepare('DROP TRIGGER admin_synthetic_fail').run();
  const issued=await rpc(path+'/issuance','POST',input,headers);expect(issued.status).toBe(200);
  expect(await issued.json()).toMatchObject({view:{state:'GRANTED'},grant:{grantRevision:1},idempotent:false});
  expect(await(await rpc(path+'/issuance','POST',input,headers)).json()).toMatchObject({idempotent:true});
  const audit=await(await rpc(path+'/events')).json() as any;expect(audit.items.map((e:any)=>e.kind)).toContain('GRANT_ISSUED');
  const entitlement=await readEntitlement(db,project,c.session);expect(entitlement.legacyCore).toBe(true);expect(entitlement.proAllowed).toBe(false);
  const original=await db.prepare('SELECT grant_json FROM legacy_grants WHERE uid=?').bind(c.session.uid).first();
  expect((await rpc(path+'/decisions','POST',decision('REJECT'),{'If-Match':'"4"'})).status).toBe(409);
  expect((await rpc(path+'/decisions','POST',decision('REQUEST_MORE'),{'If-Match':'"4"'})).status).toBe(409);
  expect(await db.prepare('SELECT grant_json FROM legacy_grants WHERE uid=?').bind(c.session.uid).first()).toEqual(original);
  expect((await(await rpc(path)).json() as any).view.state).toBe('GRANTED');
});
it('runs a separate staged PROD workerd binding and empty D1 without copying DEV cases or authority',async()=>{
  const gateway={fetch:(url:string,init:Parameters<Miniflare['dispatchFetch']>[1])=>prodMf.dispatchFetch(url,init)};
  const get=async(path:string)=>{const r=req(path);return gateway.fetch(r.url,{headers:Object.fromEntries(r.headers)})};
  const session=await get('/session');
  expect(session.status).toBe(200);expect(await session.json()).toMatchObject({environment:'PROD',mutationsEnabled:false});
  const counts=await get('/claims/counts');expect(counts.headers.get('X-CalcMot-Environment')).toBe('PROD');
  expect(await counts.json()).toMatchObject({counts:{UNDER_REVIEW:0,GRANTED:0,REJECTED:0}});
  for(const table of ['legacy_claims','legacy_grants','legacy_policies','legacy_review_events','legacy_evidence'])
    expect(await prodDb.prepare(`SELECT count(*) AS n FROM ${table}`).first()).toEqual({n:0});
  expect(await prodDb.prepare('PRAGMA quick_check').first()).toEqual({quick_check:'ok'});
  expect((await prodDb.prepare('PRAGMA foreign_key_check').all()).results).toEqual([]);
  expect((await prodDb.prepare("SELECT name FROM sqlite_master WHERE type='index' AND name LIKE 'legacy_admin_%'").all()).results).toHaveLength(4);
  const post=req('/claims/irrelevant/decisions','POST',decision('REJECT'));
  const denied=await gateway.fetch(post.url,{method:'POST',headers:Object.fromEntries(post.headers),body:JSON.stringify(decision('REJECT'))});
  expect(denied.status).toBe(503);
  expect(await prodDb.prepare('SELECT count(*) AS n FROM legacy_claims').first()).toEqual({n:0});
  const direct=await(await prodMf.getWorker('prod-private')).fetch(req('/session').url);
  expect(direct.status).toBe(403);expect(direct.headers.get('X-CalcMot-Environment')).toBe('PROD');
});
it('explicit PROD activation retains reauthentication, approval separation and idempotent issuance in local D1 only',async()=>{
  const prod:AdminEnv={...env,DB:prodDb,ADMIN_ENVIRONMENT:'PROD',ADMIN_MUTATIONS_ENABLED:'true'};
  const localReview=new LegacyReviewService(prod),operator=identity(ownerUid,project,true);
  await localReview.authorizePolicy(operator);
  const applicant=identity('synthetic-prod-activation-only');
  const claim=await localReview.open(applicant,{evidenceType:'USER_DECLARATION',evidenceHash:null},key());
  const path='/claims/'+claim.claimId;
  const invoke=async(suffix:string,method='GET',input?:unknown,headers:Record<string,string>={},bearer=ownerToken)=>
    handleAdmin(req(suffix,method,input,{'X-CalcMot-Expected-Environment':'PROD','X-CalcMot-Target-Environment':'PROD',...headers},bearer),
      prod,keys,new AdminRateGuard());
  expect(await(await invoke('/session')).json()).toMatchObject({environment:'PROD',mutationsEnabled:true});
  const stale=await token(ownerUid,true,{age:301});
  expect((await invoke(path+'/evidence','POST',{}, {},stale)).status).toBe(403);
  for(const [revision,category]of [[0,'SUPPORT_RECORD'],[1,'INSTALL_CONTEXT']]as const)
    expect((await invoke(path+'/evidence','POST',{kind:'AUXILIARY',category,originKey:key(),reference:'synthetic:activation'},
      {'If-Match':`"${revision}"`})).status).toBe(200);
  const evidence=await(await invoke(path+'/evidence')).json() as any;
  const approved=await invoke(path+'/decisions','POST',decision('APPROVE',evidence.items.map((e:any)=>e.evidenceId)),{'If-Match':'"2"'});
  expect(approved.status).toBe(200);const approval=await approved.json() as any;
  expect((await(await invoke(path)).json() as any).grant).toBeNull();
  const input={approvalEventId:approval.decision.eventId,confirmation:{claimId:claim.claimId,permanentCoreOnly:true}};
  const headers={'If-Match':'"3"','Idempotency-Key':key()};
  expect(await(await invoke(path+'/issuance','POST',input,headers)).json()).toMatchObject({view:{state:'GRANTED'},idempotent:false});
  expect(await(await invoke(path+'/issuance','POST',input,headers)).json()).toMatchObject({idempotent:true});
  expect(await prodDb.prepare('SELECT count(*) AS n FROM legacy_grants').first()).toEqual({n:1});
});
