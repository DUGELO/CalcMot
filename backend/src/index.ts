import validSnapshot from './generated/entitlement-validator.js';
import { InvalidSession, verifySession } from './auth';
import type { JWTVerifyGetKey } from 'jose';
import { AbuseGuard, type AbuseConfig } from './abuse';
import { LegacyReviewService, ReviewError } from './legacy-review';
import { readEntitlement } from './entitlement';

export interface Env extends AbuseConfig {
  DB: D1Database; FIREBASE_PROJECT_ID: string; LEGACY_CLAIMS_ENABLED?: string; ENTITLEMENT_PROJECTION_ENABLED?:string;
}
const abuse = new AbuseGuard();
export interface StoredSnapshot {
  revision: number; checkedAt: string; subscriptionRequired: boolean;
  legacyCore: boolean; coreAllowed: boolean; proAllowed: boolean;
  legacy: {state: string};
  play: {state: string; validUntil: string | null};
  promo: {state: string; startAt: string | null; validUntil: string | null};
  review: {provisionalUntil: string | null};
}

function response(body: unknown, status: number, requestId: string): Response {
  return Response.json(body, { status, headers: {
    'X-Request-ID': requestId, 'Cache-Control': 'no-store',
    'X-Content-Type-Options': 'nosniff'
  }});
}

export async function handleRequest(request: Request, env: Env, keys?: JWTVerifyGetKey): Promise<Response> {
  const requestId = crypto.randomUUID();
  const now = new Date();
  const error = (code: string, status: number, retryable = false) => response({
    code, requestId, serverNow: now.toISOString(), retryable
  }, status, requestId);
  try {
    if (!env.FIREBASE_PROJECT_ID || !env.DB) return error('RETRY_LATER', 503, true);
    if (!abuse.request(env, now.getTime())) return error('RETRY_LATER', 503, true);
    const session = await verifySession(request, env.FIREBASE_PROJECT_ID, keys, now);
    const url = new URL(request.url);
    if (url.pathname === '/v1/legacy/claim') {
      if (request.method !== 'POST') return error('FORBIDDEN', 403);
      if (env.LEGACY_CLAIMS_ENABLED !== 'true') return error('RETRY_LATER', 503, true);
      if (!abuse.read(env, env.FIREBASE_PROJECT_ID, session.uid, now.getTime()))
        return error('RATE_LIMITED', 429, true);
      // Bound the actual stream, including a body without Content-Length.
      const reader = request.body?.getReader();
      if (!reader) return error('INVALID_REQUEST', 400);
      const chunks: Uint8Array[] = []; let size = 0;
      for (;;) {
        const part = await reader.read(); if (part.done) break;
        size += part.value.byteLength;
        if (size > 4096) { await reader.cancel(); return error('INVALID_REQUEST', 400); }
        chunks.push(part.value);
      }
      const bytes = new Uint8Array(size); let offset = 0;
      for (const chunk of chunks) { bytes.set(chunk, offset); offset += chunk.byteLength; }
      let input;
      try { input = JSON.parse(new TextDecoder('utf-8', {fatal:true}).decode(bytes)); }
      catch { return error('INVALID_REQUEST', 400); }
      try {
        const view = await new LegacyReviewService(env).open(session, input, request.headers.get('Idempotency-Key') ?? '', now);
        return response(view, 200, requestId);
      } catch (cause) {
        if (cause instanceof ReviewError) {
          if (cause.code === 'INVALID_REQUEST') return error('INVALID_REQUEST', 400);
          if (cause.code === 'CONFLICT') return error('INVALID_REQUEST', 409);
          if (cause.code === 'FORBIDDEN') return error('FORBIDDEN', 403);
        }
        throw cause;
      }
    }
    // All unimplemented privileged operations fail closed; no financial/admin stubs.
    if (url.pathname.startsWith('/admin/') || url.pathname.startsWith('/internal/'))
      return error('FORBIDDEN', 403);
    if (url.pathname !== '/v1/me/entitlement') return error('NOT_FOUND', 404);
    if (request.method !== 'GET') return error('FORBIDDEN', 403);
    if (!abuse.read(env, env.FIREBASE_PROJECT_ID, session.uid, now.getTime()))
      return error('RETRY_LATER', 503, true);
    if(env.ENTITLEMENT_PROJECTION_ENABLED==='true')
      return response(await readEntitlement(env.DB,env.FIREBASE_PROJECT_ID,session),200,requestId);
    // GET has no owner parameters: unknown query fields never override the token UID.
    // Keep this route's frozen 200/401/503 responses rather than inventing a 400 contract.
    const row = await env.DB.prepare(
      'SELECT snapshot_json, revision FROM entitlement_snapshots WHERE firebase_project_id = ? AND uid = ?'
    ).bind(env.FIREBASE_PROJECT_ID, session.uid).first<{snapshot_json: string; revision: number}>();
    if (!row) return error('RETRY_LATER', 503, true);
    const snapshot = JSON.parse(row.snapshot_json);
    if (!validSnapshot(snapshot) || snapshot.revision !== row.revision) return error('RETRY_LATER', 503, true);
    // S07 owns future projection. Do not serve stale temporal access as fresh authority.
    const playValid = ['ACTIVE', 'CANCELED_WITH_ACCESS', 'GRACE'].includes(snapshot.play.state) &&
      snapshot.play.validUntil !== null && Date.parse(snapshot.play.validUntil) > now.getTime();
    const promoValid = snapshot.promo.state === 'ACTIVE' && snapshot.promo.startAt !== null &&
      snapshot.promo.validUntil !== null && Date.parse(snapshot.promo.startAt) <= now.getTime() &&
      Date.parse(snapshot.promo.validUntil) > now.getTime();
    const reviewValid = snapshot.legacy.state === 'UNDER_REVIEW' && snapshot.review.provisionalUntil !== null &&
      Date.parse(snapshot.review.provisionalUntil) > now.getTime();
    const coreAllowed = !snapshot.subscriptionRequired || snapshot.legacyCore || playValid || promoValid || reviewValid;
    if (snapshot.coreAllowed !== coreAllowed || snapshot.proAllowed !== (playValid || promoValid) ||
        Date.parse(snapshot.checkedAt) > now.getTime()) return error('RETRY_LATER', 503, true);
    return response({ ...snapshot, serverNow: now.toISOString() }, 200, requestId);
  } catch (cause) {
    return cause instanceof InvalidSession ? error('AUTH_REQUIRED', 401) : error('RETRY_LATER', 503, true);
  }
}

export default { fetch: (request: Request, env: Env) => handleRequest(request, env) };
