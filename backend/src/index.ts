import validSnapshot from './generated/entitlement-validator.js';
import { InvalidSession, verifySession } from './auth';
import type { JWTVerifyGetKey } from 'jose';
import { AbuseGuard, type AbuseConfig } from './abuse';

export interface Env extends AbuseConfig { DB: D1Database; FIREBASE_PROJECT_ID: string }
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
    // All unimplemented privileged operations fail closed; no financial/admin stubs.
    if (url.pathname.startsWith('/admin/') || url.pathname.startsWith('/internal/'))
      return error('FORBIDDEN', 403);
    if (url.pathname !== '/v1/me/entitlement') return error('NOT_FOUND', 404);
    if (request.method !== 'GET') return error('FORBIDDEN', 403);
    if (!abuse.read(env, env.FIREBASE_PROJECT_ID, session.uid, now.getTime()))
      return error('RETRY_LATER', 503, true);
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
