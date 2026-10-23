import type {JWTVerifyGetKey} from 'jose';
import {InvalidSession} from '../auth';
import {AdminFailure,AdminRateGuard,authorize,browserBoundary,failure,recent,requireAdminConfig,adminEnvironment,environmentResponse,requireMutations,type AdminConfig} from './security';

export interface AdminGatewayEnv extends AdminConfig {ADMIN:{handle(request:Request):Promise<Response>}}
const guard=new AdminRateGuard();
/** Reusable by a Pages Function or dedicated Worker. No D1 binding or browser secret here. */
export async function handleGateway(request:Request,env:AdminGatewayEnv,keys?:JWTVerifyGetKey,rate=guard):Promise<Response> {
  return environmentResponse(await gatewayRequest(request,env,keys,rate),env);
}
async function gatewayRequest(request:Request,env:AdminGatewayEnv,keys:JWTVerifyGetKey|undefined,rate:AdminRateGuard):Promise<Response> {
  try {
    requireAdminConfig(env);browserBoundary(request,env);rate.admit(env);
    if(!['GET','POST'].includes(request.method))throw new AdminFailure('METHOD_NOT_ALLOWED',405);
    const url=new URL(request.url);
    if(!url.pathname.startsWith('/api/admin/v1/'))throw new AdminFailure('NOT_FOUND',404);
    const session=await authorize(request,env,keys);
    if(request.method==='POST'&&!recent(session))throw new AdminFailure('REAUTH_REQUIRED',403);
    requireMutations(request,env);
    if(request.method==='POST')rate.admit(env,true);
    if(!env.ADMIN?.handle)throw new AdminFailure('RETRY_LATER',503);
    // Drop irrelevant inbound headers/cookies; preserve only this contract's inputs.
    const headers=new Headers();
    for(const name of ['Authorization','Origin','Content-Type','Content-Encoding','If-Match','Idempotency-Key','Sec-Fetch-Site','X-CalcMot-Admin','X-CalcMot-Expected-Environment']) {
      const value=request.headers.get(name);if(value!==null)headers.set(name,value);
    }
    headers.set('X-CalcMot-Target-Environment',adminEnvironment(env));
    const response=await env.ADMIN.handle(new Request(request.url,{method:request.method,headers,
      ...(request.method==='POST'?{body:request.body}:{}),redirect:'manual'}));
    if(response.status>=300&&response.status<400)throw new AdminFailure('RETRY_LATER',503);
    if(response.headers.get('X-CalcMot-Environment')!==adminEnvironment(env))throw new AdminFailure('RETRY_LATER',503);
    return response;
  }catch(error) {
    if(error instanceof InvalidSession)return failure('AUTH_REQUIRED',401);
    if(error instanceof AdminFailure)return failure(error.code,error.status);
    return failure('RETRY_LATER',503);
  }
}
export default {fetch:(request:Request,env:AdminGatewayEnv)=>handleGateway(request,env)};
