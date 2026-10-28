import {InvalidSession} from '../auth';
import type {JWTVerifyGetKey} from 'jose';
import {LegacyReviewService,ReviewError} from '../legacy-review';
import type {EvidenceInput,DecisionInput,IssuanceInput} from './contracts';
import {AdminFailure,AdminRateGuard,authorize,browserBoundary,failure,json,recent,requireAdminConfig,adminEnvironment,environmentResponse,requireMutations,mutationsEnabled,type AdminConfig} from './security';
import {AdminReads,claimId,decisionView,grantView,query} from './reads';
import {strictJson} from './strict-json';

export interface AdminEnv extends AdminConfig {DB:D1Database}
const guard=new AdminRateGuard();
function exact(input:unknown,fields:string[]):asserts input is Record<string,unknown> {
  if(input===null||typeof input!=='object'||Array.isArray(input)||Object.keys(input).some(k=>!fields.includes(k)))
    throw new AdminFailure('INVALID_REQUEST',400);
}
async function body(request:Request):Promise<unknown> {
  if(request.headers.get('Content-Type')?.split(';')[0].trim()!=='application/json'||request.headers.has('Content-Encoding'))
    throw new AdminFailure('UNSUPPORTED_MEDIA_TYPE',415);
  const reader=request.body?.getReader();if(!reader)throw new AdminFailure('INVALID_REQUEST',400);
  const chunks:Uint8Array[]=[];let size=0;
  for(;;){const item=await reader.read();if(item.done)break;size+=item.value.length;
    if(size>8192){await reader.cancel();throw new AdminFailure('PAYLOAD_TOO_LARGE',413)}chunks.push(item.value)}
  const bytes=new Uint8Array(size);let at=0;for(const chunk of chunks){bytes.set(chunk,at);at+=chunk.length}
  try{return strictJson(new TextDecoder('utf-8',{fatal:true}).decode(bytes))}catch{throw new AdminFailure('INVALID_REQUEST',400)}
}
function preconditions(request:Request) {
  const match=request.headers.get('If-Match'),key=request.headers.get('Idempotency-Key');
  if(match===null||key===null)throw new AdminFailure('PRECONDITION_REQUIRED',428);
  if(!/^"(?:0|[1-9][0-9]*)"$/.test(match)||!/^[A-Za-z0-9_.:/-]{1,128}$/.test(key))throw new AdminFailure('INVALID_REQUEST',400);
  const revision=Number(match.slice(1,-1));if(!Number.isSafeInteger(revision))throw new AdminFailure('INVALID_REQUEST',400);
  return {revision,key};
}
/** Only reached over the internal RPC entrypoint. Verifies JWT again; never trusts gateway claims. */
export async function handleAdmin(request:Request,env:AdminEnv,keys?:JWTVerifyGetKey,rate=guard):Promise<Response> {
  return environmentResponse(await handleAdminRequest(request,env,keys,rate),env);
}
async function handleAdminRequest(request:Request,env:AdminEnv,keys:JWTVerifyGetKey|undefined,rate:AdminRateGuard):Promise<Response> {
  const requestId=crypto.randomUUID();
  try {
    requireAdminConfig(env);browserBoundary(request,env);
    rate.admit(env);
    const session=await authorize(request,env,keys),url=new URL(request.url),now=new Date();
    const target=request.headers.get('X-CalcMot-Target-Environment');
    if((target!==null && target!==adminEnvironment(env)) ||
      (adminEnvironment(env)==='PROD' && target!=='PROD'))throw new AdminFailure('RETRY_LATER',503);
    if(url.pathname==='/api/admin/v1/session') {
      query(url,[]);if(request.method!=='GET')throw new AdminFailure('METHOD_NOT_ALLOWED',405);
      return json({contractVersion:'1.3.0',environment:adminEnvironment(env),mutationsEnabled:mutationsEnabled(env),authenticated:true,reauthenticationRequired:!recent(session,now),
        reauthenticateBy:new Date((Number(session.claims.auth_time)+300)*1000).toISOString(),serverNow:now.toISOString()});
    }
    if(request.method==='POST'&&!recent(session,now))throw new AdminFailure('REAUTH_REQUIRED',403);
    requireMutations(request,env);
    if(!env.DB)throw new AdminFailure('RETRY_LATER',503);
    const reads=new AdminReads(env.DB,env.FIREBASE_PROJECT_ID,env.LEGACY_OWNER_UID!);
    if(url.pathname==='/api/admin/v1/claims' || url.pathname==='/api/admin/v1/claims/counts') {
      if(request.method!=='GET')throw new AdminFailure('METHOD_NOT_ALLOWED',405);
      if(url.pathname.endsWith('/counts')){query(url,[]);return json(await reads.counts())}
      return json(await reads.claims(url));
    }
    const route=/^\/api\/admin\/v1\/claims\/([^/]+)(?:\/(events|evidence|decisions|issuance))?$/.exec(url.pathname);
    if(!route)throw new AdminFailure('NOT_FOUND',404);
    let id:string;try{id=claimId(decodeURIComponent(route[1]))}catch{throw new AdminFailure('INVALID_REQUEST',400)}
    const section=route[2];
    if(request.method==='GET') {
      if(section && !['events','evidence'].includes(section))throw new AdminFailure('METHOD_NOT_ALLOWED',405);
      if(!section)query(url,[]);
      const row=await reads.target(id);
      return json(section==='events'?await reads.events(row,url):section==='evidence'?await reads.evidence(row,url):await reads.inspect(row));
    }
    if(request.method!=='POST'||!['evidence','decisions','issuance'].includes(section))throw new AdminFailure('METHOD_NOT_ALLOWED',405);
    query(url,[]);rate.admit(env,true);
    const {revision,key}=preconditions(request),input=await body(request),row=await reads.target(id);
    const review=new LegacyReviewService(env);
    if(section==='evidence') {
      exact(input,['kind','category','originKey','reference','authorityEvidenceId']);
      return json({view:await review.addEvidence(session,row.uid,id,revision,input as unknown as EvidenceInput,key)});
    }
    if(section==='decisions') {
      exact(input,['outcome','evidenceIds','reason','ownershipConfirmed','contradictionsCleared','legacyUseConcluded']);
      const result=await review.decide(session,row.uid,id,revision,input as unknown as DecisionInput,key);
      return json({view:result.view,decision:decisionView(result.decision)});
    }
    exact(input,['approvalEventId','confirmation']);exact(input.confirmation,['claimId','permanentCoreOnly']);
    const issue=input as unknown as IssuanceInput;
    if(issue.confirmation.claimId!==id||issue.confirmation.permanentCoreOnly!==true)throw new AdminFailure('INVALID_REQUEST',400);
    const result=await review.issue(session,row.uid,id,revision,{approvalEventId:issue.approvalEventId},key);
    return json({view:result.view,grant:grantView(result.grant),idempotent:result.idempotent});
  }catch(error) {
    if(error instanceof InvalidSession)return failure('AUTH_REQUIRED',401,requestId);
    if(error instanceof AdminFailure)return failure(error.code,error.status,requestId);
    if(error instanceof ReviewError) {
      const status={FORBIDDEN:403,INVALID_REQUEST:400,CONFLICT:409,NOT_FOUND:404,MORE_EVIDENCE_REQUIRED:422,
        LINK_REQUIRED:422,GRANT_PRESERVED:409,POLICY_REQUIRED:422}[error.code];
      return failure(error.code,status,requestId);
    }
    return failure('RETRY_LATER',503,requestId);
  }
}
