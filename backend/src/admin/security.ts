import {verifySession,type Session} from '../auth';
import type {JWTVerifyGetKey} from 'jose';
import type {AdminError,ErrorCode} from './contracts';

export interface AdminConfig {
  FIREBASE_PROJECT_ID:string; LEGACY_OWNER_UID?:string; ADMIN_ORIGIN:string;
  ADMIN_REQUESTS_PER_MINUTE:string; ADMIN_MUTATIONS_PER_MINUTE:string;
  ADMIN_ENVIRONMENT?:'DEV'|'PROD'; ADMIN_MUTATIONS_ENABLED?:'true'|'false';
}
/** Existing DEV deployments remain compatible; PROD must be explicitly selected. */
export function adminEnvironment(env:AdminConfig):'DEV'|'PROD' {return env.ADMIN_ENVIRONMENT??'DEV'}
export function environmentResponse(response:Response,env:AdminConfig):Response {
  const headers=new Headers(response.headers);
  const environment=adminEnvironment(env);
  headers.set('X-CalcMot-Environment',['DEV','PROD'].includes(environment)?environment:'UNCONFIGURED');
  return new Response(response.body,{status:response.status,statusText:response.statusText,headers});
}
export function requireMutations(request:Request,env:AdminConfig) {
  const expected=request.headers.get('X-CalcMot-Expected-Environment');
  if(request.method==='POST' && ((expected!==null && expected!==adminEnvironment(env)) ||
    (adminEnvironment(env)==='PROD' && expected!=='PROD')))
    throw new AdminFailure('RETRY_LATER',503);
  if(request.method==='POST' && !mutationsEnabled(env))
    throw new AdminFailure('RETRY_LATER',503);
}
export function mutationsEnabled(env:AdminConfig):boolean {
  return env.ADMIN_MUTATIONS_ENABLED!=='false' &&
    (adminEnvironment(env)!=='PROD' || env.ADMIN_MUTATIONS_ENABLED==='true');
}
export class AdminFailure extends Error {
  constructor(readonly code:ErrorCode,readonly status:number){super(code)}
}
export function requireAdminConfig(env:AdminConfig) {
  try {
    const origin=new URL(env.ADMIN_ORIGIN);
    if(!['DEV','PROD'].includes(adminEnvironment(env)) ||
      (env.ADMIN_MUTATIONS_ENABLED!==undefined && !['true','false'].includes(env.ADMIN_MUTATIONS_ENABLED)) ||
      (adminEnvironment(env)==='PROD' && origin.protocol!=='https:'))throw Error();
    if(origin.origin!==env.ADMIN_ORIGIN || origin.username || origin.password ||
      (origin.protocol!=='https:' && !(origin.protocol==='http:' && ['127.0.0.1','localhost'].includes(origin.hostname))) ||
      !env.FIREBASE_PROJECT_ID || !env.LEGACY_OWNER_UID || env.LEGACY_OWNER_UID.length>128) throw Error();
    for(const n of [env.ADMIN_REQUESTS_PER_MINUTE,env.ADMIN_MUTATIONS_PER_MINUTE])
      if(!/^[1-9][0-9]{0,3}$/.test(n))throw Error();
  }catch {throw new AdminFailure('RETRY_LATER',503)}
}
export function browserBoundary(request:Request,env:AdminConfig) {
  const origin=request.headers.get('Origin');
  if(new URL(request.url).origin!==env.ADMIN_ORIGIN || request.headers.get('X-CalcMot-Admin')!=='1' ||
    request.headers.get('Sec-Fetch-Site')==='cross-site' ||
    (origin!==null && origin!==env.ADMIN_ORIGIN) || (request.method!=='GET' && origin!==env.ADMIN_ORIGIN))
    throw new AdminFailure('FORBIDDEN',403);
}
export async function authorize(request:Request,env:AdminConfig,keys?:JWTVerifyGetKey):Promise<Session> {
  const session=await verifySession(request,env.FIREBASE_PROJECT_ID,keys);
  if(session.uid!==env.LEGACY_OWNER_UID || session.claims.admin!==true)
    throw new AdminFailure('FORBIDDEN',403);
  return session;
}
export function recent(session:Session,now=new Date()) {
  const age=+now/1000-Number(session.claims.auth_time);
  return Number.isFinite(age) && age>=0 && age<=300;
}

/** Per isolate only. No persistent personal identifiers, no distributed-quota claim. */
export class AdminRateGuard {
  private minute=-1; private requests=0; private mutations=0;
  admit(env:AdminConfig,mutation=false,now=Date.now()) {
    const minute=Math.floor(now/60000);
    if(minute!==this.minute){this.minute=minute;this.requests=0;this.mutations=0}
    if(mutation ? ++this.mutations>Number(env.ADMIN_MUTATIONS_PER_MINUTE) : ++this.requests>Number(env.ADMIN_REQUESTS_PER_MINUTE))
      throw new AdminFailure('RATE_LIMITED',429);
  }
}
const messages:Record<ErrorCode,string>={
  AUTH_REQUIRED:'Entre novamente para continuar.',FORBIDDEN:'Operação não autorizada.',
  REAUTH_REQUIRED:'Confirme novamente sua conta Google.',INVALID_REQUEST:'Solicitação inválida.',
  NOT_FOUND:'Solicitação não encontrada.',METHOD_NOT_ALLOWED:'Método não disponível.',
  PAYLOAD_TOO_LARGE:'Conteúdo acima do limite permitido.',UNSUPPORTED_MEDIA_TYPE:'Envie conteúdo JSON.',
  PRECONDITION_REQUIRED:'Informe revisão e chave da operação.',CONFLICT:'O pedido mudou ou a operação conflita. Consulte novamente.',
  GRANT_PRESERVED:'O direito existente foi preservado.',MORE_EVIDENCE_REQUIRED:'A análise exige evidências adicionais.',
  LINK_REQUIRED:'A identidade recuperável precisa ser confirmada.',POLICY_REQUIRED:'A política precisa estar autorizada pelo operador.',
  RATE_LIMITED:'Aguarde antes de tentar novamente.',RETRY_LATER:'Não foi possível concluir agora. Tente novamente.'
};
export function json(value:unknown,status=200):Response {
  return Response.json(value,{status,headers:{'Cache-Control':'no-store','X-Content-Type-Options':'nosniff',
    'X-Robots-Tag':'noindex, nofollow, noarchive',
    'Referrer-Policy':'no-referrer','Vary':'Origin','Content-Security-Policy':"default-src 'none'; frame-ancestors 'none'"}});
}
export function failure(code:ErrorCode,status:number,requestId=crypto.randomUUID()) {
  const body:AdminError={code,message:messages[code],requestId,retryable:status===429||status===503,serverNow:new Date().toISOString()};
  const response=json(body,status);response.headers.set('X-Request-ID',requestId);
  if(status===429)response.headers.set('Retry-After','60');
  return response;
}
