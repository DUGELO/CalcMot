import type {Session} from '../src/auth';

/** Read-only preflight AFTER verifySession (Google JWKS). This function neither
 * issues admin claims nor authorizes a grant; the existing console does that.
 * Never return UID, email, token, claim payload or account profile.
 */
export function operatorReadiness(session: Session, project: string, ownerUid: string | undefined,
  expected: 'owner' | 'ordinary', now = new Date()) {
  const projectMatches = session.claims.aud === project &&
    session.claims.iss === `https://securetoken.google.com/${project}` && session.uid === session.claims.sub;
  const ownerConfigured = !!ownerUid;
  const ownerMatches = ownerConfigured && session.uid === ownerUid;
  const admin = session.claims.admin === true;
  const authTime = session.claims.auth_time;
  const age = typeof authTime === 'number' ? now.getTime()/1000-authTime : NaN;
  const recentAuthentication = Number.isFinite(age) && age >= 0 && age <= 300;
  const ownerAuthorized = projectMatches && ownerMatches && admin && recentAuthentication;
  const ordinaryConfirmed = projectMatches && ownerConfigured && !ownerMatches && !admin;
  return {status:(expected === 'owner' ? ownerAuthorized : ordinaryConfirmed) ? 'GO' : 'HOLD',
    scope:'single verified Firebase session, no global privilege inventory', expected,
    projectMatches, ownerConfigured, ownerMatches, admin, recentAuthentication,
    unexpectedOtherAdmin:!ownerMatches && admin, credentialsOmitted:true};
}
