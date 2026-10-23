import { createRemoteJWKSet, jwtVerify, type JWTVerifyGetKey, type JWTPayload } from 'jose';

// Never take a key URL, issuer, audience, role or UID from the HTTP request.
const googleKeys = createRemoteJWKSet(new URL(
  'https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com'
), { timeoutDuration: 5000 });

export class InvalidSession extends Error {}
export interface Session { uid: string; claims: JWTPayload }

export async function verifySession(
  request: Request, projectId: string, keys: JWTVerifyGetKey = googleKeys,
  now: Date = new Date()
): Promise<Session> {
  const authorization = request.headers.get('Authorization');
  const match = authorization?.match(/^Bearer ([A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+)$/);
  if (!match || match[1].length > 16384) throw new InvalidSession();
  try {
    const { payload, protectedHeader } = await jwtVerify(match[1], keys, {
      algorithms: ['RS256'], issuer: `https://securetoken.google.com/${projectId}`,
      audience: projectId, currentDate: now,
      requiredClaims: ['exp', 'iat', 'sub', 'auth_time']
    });
    const seconds = Math.floor(now.getTime() / 1000);
    if (!protectedHeader.kid || typeof payload.sub !== 'string' ||
        payload.sub.length < 1 || payload.sub.length > 128 ||
        typeof payload.iat !== 'number' || payload.iat > seconds ||
        typeof payload.auth_time !== 'number' || payload.auth_time > seconds ||
        payload.auth_time < 0 || payload.iat < payload.auth_time ||
        typeof payload.exp !== 'number' || payload.exp <= payload.iat ||
        payload.aud !== projectId) throw new InvalidSession();
    return { uid: payload.sub, claims: payload };
  } catch (error) {
    // Only proven token errors are 401. Trusted key service/runtime failures are 503.
    if (error instanceof InvalidSession) throw error;
    const invalidTokenCodes = new Set(['ERR_JWT_CLAIM_VALIDATION_FAILED', 'ERR_JWT_EXPIRED',
      'ERR_JWS_SIGNATURE_VERIFICATION_FAILED', 'ERR_JWS_INVALID', 'ERR_JWT_INVALID',
      'ERR_JOSE_ALG_NOT_ALLOWED', 'ERR_JWKS_NO_MATCHING_KEY', 'ERR_JOSE_NOT_SUPPORTED']);
    if (invalidTokenCodes.has((error as {code?: string}).code ?? '')) throw new InvalidSession();
    throw error;
  }
}

export function hasRole(session: Session, role: 'admin' | 'creator'): boolean {
  // Custom claims must be boolean server-issued values, never email/domain checks.
  return session.claims[role] === true;
}
