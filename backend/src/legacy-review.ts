import { hasRole, type Session } from './auth';
import { LegacyGrantService, LEGACY_CORE_BASELINE, LEGACY_MAX_HISTORICAL_VERSION_CODE,
  type LegacyClaim, type LegacyGrant } from './legacy';
import { validateLegacyClaim, validateLegacyGrant } from './generated/legacy-validator.js';

const POLICY = 'legacy-through-3.5-vc12';
export class ReviewError extends Error {
  constructor(public readonly code: 'FORBIDDEN' | 'INVALID_REQUEST' | 'CONFLICT' | 'NOT_FOUND' |
    'MORE_EVIDENCE_REQUIRED' | 'LINK_REQUIRED' | 'GRANT_PRESERVED' | 'POLICY_REQUIRED') { super(code); }
}
export interface ReviewEnv { DB: D1Database; FIREBASE_PROJECT_ID: string; LEGACY_OWNER_UID?: string }
interface ClaimRow { claim_json: string; revision: number }
export interface ClaimView {
  claimId: string; state: LegacyClaim['state']; openedAt: string; resolvedAt: string | null;
  provisionalUntil: string | null; revision: number; serverNow: string;
}
export interface EvidenceInput {
  kind: 'HISTORY' | 'AUXILIARY' | 'DECLARATION';
  category: 'SUPPORT_RECORD' | 'INSTALL_CONTEXT' | 'USAGE_CONTEXT' | 'ACCOUNT_CONTINUITY' | 'USER_STATEMENT';
  originKey: string; reference: string; authorityEvidenceId?: string;
}
interface EvidenceRow {
  evidence_id: string; kind: EvidenceInput['kind']; category: EvidenceInput['category'];
  origin_key: string; reference: string; authority_evidence_id: string | null;
}
export interface DecisionInput {
  outcome: 'APPROVE' | 'REJECT' | 'REQUEST_MORE' | 'OWNERSHIP_CONFLICT';
  evidenceIds: string[]; reason: string; ownershipConfirmed: boolean;
  contradictionsCleared: boolean; legacyUseConcluded: boolean;
}
interface EventRow {
  event_id: string; kind: string; basis: string; actor_uid: string; occurred_at: string;
  from_revision: number; to_revision: number; request_hash: string; details_json: string;
}
export async function reviewDigest(value: unknown): Promise<string> {
  const bytes = new TextEncoder().encode(JSON.stringify(value));
  return [...new Uint8Array(await crypto.subtle.digest('SHA-256', bytes))]
    .map(byte => byte.toString(16).padStart(2, '0')).join('');
}
function check(condition: unknown, code: ReviewError['code'] = 'INVALID_REQUEST'): asserts condition {
  if (!condition) throw new ReviewError(code);
}
function exact(value: object, allowed: string[]) {
  check(value !== null && typeof value === 'object' && !Array.isArray(value));
  check(Object.keys(value).every(key => allowed.includes(key)));
}
const opaque = (value: unknown): value is string =>
  typeof value === 'string' && /^[A-Za-z0-9_.:/-]{1,128}$/.test(value);

/** The Worker exposes only own claim intake. Sessions must come from
 * verifySession. All private operator actions require a signed admin role, explicit owner
 * UID and recent reauthentication. Issuance is private, never exposed by the
 * Worker. Human assessment is not recorded as cryptographic history.
 */
export class LegacyReviewService {
  constructor(private readonly env: ReviewEnv) {}

  private identity(session: Session): string {
    check(session.uid === session.claims.sub && session.uid.length > 0 && session.uid.length <= 128 &&
      session.claims.aud === this.env.FIREBASE_PROJECT_ID &&
      session.claims.iss === `https://securetoken.google.com/${this.env.FIREBASE_PROJECT_ID}`, 'FORBIDDEN');
    return session.uid;
  }
  private owner(session: Session, now: Date): string {
    const uid = this.identity(session);
    const authTime = session.claims.auth_time;
    check(this.env.LEGACY_OWNER_UID && uid === this.env.LEGACY_OWNER_UID && hasRole(session, 'admin') &&
      typeof authTime === 'number' && authTime <= now.getTime() / 1000 &&
      now.getTime() / 1000 - authTime <= 300, 'FORBIDDEN');
    return uid;
  }
  private linked(session: Session): boolean {
    const firebase = session.claims.firebase as {identities?: Record<string, unknown>} | undefined;
    const ids = firebase?.identities?.['google.com'];
    return Array.isArray(ids) && ids.some(id => typeof id === 'string' && id.length > 0);
  }
  private async read(uid: string, claimId?: string): Promise<{claim: LegacyClaim; revision: number} | null> {
    const row = await this.env.DB.prepare(`SELECT claim_json, revision FROM legacy_claims
      WHERE firebase_project_id = ? AND uid = ? ${claimId ? 'AND claim_id = ?' : ''}`)
      .bind(...(claimId ? [this.env.FIREBASE_PROJECT_ID, uid, claimId] : [this.env.FIREBASE_PROJECT_ID, uid]))
      .first<ClaimRow>();
    if (!row) return null;
    const claim: unknown = JSON.parse(row.claim_json);
    check(validateLegacyClaim(claim) && claim.uid === uid, 'CONFLICT');
    return {claim, revision: row.revision};
  }
  private view(value: {claim: LegacyClaim; revision: number}, now: Date): ClaimView {
    const {claim, revision} = value;
    return {claimId: claim.claimId, state: claim.state, openedAt: claim.openedAt,
      resolvedAt: claim.resolvedAt, provisionalUntil: claim.provisionalUntil,
      revision, serverNow: now.toISOString()};
  }
  async getOwn(session: Session, now = new Date()): Promise<ClaimView | null> {
    let row = await this.read(this.identity(session));
    if (row) {
      const grant = await new LegacyGrantService(this.env).getGrant(session);
      check(row.claim.state !== 'GRANTED' || grant, 'CONFLICT');
      if (row.claim.state !== 'GRANTED' && grant) row = await this.restore(session, row, now);
    }
    return row ? this.view(row, now) : null;
  }
  private async restore(session: Session, row: {claim: LegacyClaim; revision: number}, now: Date) {
    const id = crypto.randomUUID();
    const claim: LegacyClaim = {...row.claim, state: 'GRANTED', reviewer: 'backend:existing-grant',
      resolvedAt: now.toISOString(), provisionalUntil: null};
    check(validateLegacyClaim(claim));
    await this.env.DB.batch([
      this.env.DB.prepare(`INSERT INTO legacy_review_events
        SELECT ?, claim_id, revision, revision + 1, 'RESTORE', 'EXISTING_GRANT', ?, ?, ?, ?, ?
        FROM legacy_claims WHERE claim_id = ? AND firebase_project_id = ? AND uid = ? AND revision = ?
        AND state != 'GRANTED' AND EXISTS(SELECT 1 FROM legacy_grants WHERE firebase_project_id = ? AND uid = ?)
        ON CONFLICT DO NOTHING`)
        .bind(id, session.uid, now.toISOString(), await reviewDigest(`restore:${row.revision}`),
          await reviewDigest([row.claim.claimId, row.revision, 'EXISTING_GRANT']),
          JSON.stringify({authority:'EXISTING_GRANT',newGrant:false}), row.claim.claimId,
          this.env.FIREBASE_PROJECT_ID, session.uid, row.revision, this.env.FIREBASE_PROJECT_ID, session.uid),
      this.env.DB.prepare(`UPDATE legacy_claims SET state = 'GRANTED', resolved_at = ?, reviewer = ?,
        provisional_until = NULL, claim_json = ?, revision = revision + 1 WHERE claim_id = ? AND revision = ?
        AND EXISTS(SELECT 1 FROM legacy_review_events WHERE event_id = ?)`)
        .bind(claim.resolvedAt, claim.reviewer, JSON.stringify(claim), row.claim.claimId, row.revision, id)
    ]);
    return (await this.read(session.uid, row.claim.claimId))!;
  }

  /** Frozen claim request shape. Every submitted label/digest is a declaration,
   * never verified evidence. One durable case per project/UID; keys are scoped to
   * that identity. Different payload on a reused key is a conflict.
   */
  async open(session: Session, input: {evidenceType: string; evidenceHash: string | null}, key: string, now = new Date()): Promise<ClaimView> {
    const uid = this.identity(session);
    exact(input, ['evidenceType', 'evidenceHash']);
    check(typeof input.evidenceType === 'string' && /^[A-Z0-9_:-]{1,64}$/.test(input.evidenceType));
    check(input.evidenceHash === null || (typeof input.evidenceHash === 'string' && /^[a-f0-9]{64}$/.test(input.evidenceHash)));
    check(opaque(key));
    const requestHash = await reviewDigest([input.evidenceType, input.evidenceHash]);
    const keyHash = await reviewDigest(key);
    const prior = await this.env.DB.prepare(`SELECT request_hash FROM legacy_claim_requests
      WHERE firebase_project_id = ? AND uid = ? AND key_hash = ?`)
      .bind(this.env.FIREBASE_PROJECT_ID, uid, keyHash).first<{request_hash: string}>();
    check(!prior || prior.request_hash === requestHash, 'CONFLICT');
    const grant = await new LegacyGrantService(this.env).getGrant(session);
    const claim: LegacyClaim = {
      claimId: crypto.randomUUID(), uid, state: grant ? 'GRANTED' : 'UNDER_REVIEW',
      evidenceType: input.evidenceType, evidenceHash: input.evidenceHash, openedAt: now.toISOString(),
      resolvedAt: grant ? now.toISOString() : null, reviewer: grant ? 'backend:existing-grant' : null,
      provisionalUntil: null, policyVersion: grant?.policyVersion ?? POLICY
    };
    check(validateLegacyClaim(claim));
    await this.env.DB.batch([
      this.env.DB.prepare(`INSERT INTO legacy_claims
        (claim_id, firebase_project_id, uid, state, evidence_type, evidence_hash, opened_at,
         resolved_at, reviewer, provisional_until, policy_version, claim_json, revision)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, ?, ?, 0)
        ON CONFLICT(firebase_project_id, uid) DO NOTHING`)
        .bind(claim.claimId, this.env.FIREBASE_PROJECT_ID, uid, claim.state, claim.evidenceType, claim.evidenceHash,
          claim.openedAt, claim.resolvedAt, claim.reviewer, claim.policyVersion, JSON.stringify(claim)),
      this.env.DB.prepare(`INSERT INTO legacy_claim_requests
        SELECT firebase_project_id, uid, ?, ?, claim_id FROM legacy_claims
        WHERE firebase_project_id = ? AND uid = ? ON CONFLICT DO NOTHING`)
        .bind(keyHash, requestHash, this.env.FIREBASE_PROJECT_ID, uid),
      this.env.DB.prepare(`INSERT INTO legacy_claim_identities SELECT claim_id, ?, ? FROM legacy_claims
        WHERE firebase_project_id = ? AND uid = ? ON CONFLICT(claim_id) DO UPDATE SET
        linked_google = MAX(linked_google, excluded.linked_google), verified_at = excluded.verified_at`)
        .bind(this.linked(session) ? 1 : 0, now.toISOString(), this.env.FIREBASE_PROJECT_ID, uid)
    ]);
    const receipt = await this.env.DB.prepare(`SELECT request_hash FROM legacy_claim_requests
      WHERE firebase_project_id = ? AND uid = ? AND key_hash = ?`)
      .bind(this.env.FIREBASE_PROJECT_ID, uid, keyHash).first<{request_hash: string}>();
    check(receipt?.request_hash === requestHash, 'CONFLICT');
    const row = await this.read(uid); check(row, 'CONFLICT');
    check(row.claim.state !== 'GRANTED' || grant, 'CONFLICT');
    return this.view(grant && row.claim.state !== 'GRANTED' ? await this.restore(session, row, now) : row, now);
  }

  /** UID target is only accepted for this private owner operation and must match
   * the case. A normal user cannot use inspect to enumerate or read another UID.
   */
  async inspect(operator: Session, uid: string, claimId: string, now = new Date()) {
    this.owner(operator, now);
    const row = await this.read(uid, claimId); check(row, 'NOT_FOUND');
    const evidence = await this.env.DB.prepare('SELECT * FROM legacy_review_evidence WHERE claim_id = ? ORDER BY evidence_id')
      .bind(claimId).all();
    const events = await this.env.DB.prepare('SELECT * FROM legacy_review_events WHERE claim_id = ? ORDER BY to_revision')
      .bind(claimId).all();
    const issuance = await this.env.DB.prepare('SELECT * FROM legacy_review_issuances WHERE claim_id = ?')
      .bind(claimId).first();
    return {view: this.view(row, now), evidence: evidence.results, events: events.results, issuance};
  }
  private async history(uid: string, id: string, now: Date) {
    return this.env.DB.prepare(`SELECT h.used_version_code FROM legacy_evidence e JOIN legacy_historical_use h
      ON h.firebase_project_id = e.firebase_project_id AND h.evidence_id = e.evidence_id
      WHERE e.firebase_project_id = ? AND e.uid = ? AND e.evidence_id = ? AND e.verification = 'VERIFIED'
      AND h.verified_by = e.verified_by AND julianday(h.verified_at) <= julianday(?)
      AND julianday(h.verified_at) >= julianday(e.qualifying_at)`)
      .bind(this.env.FIREBASE_PROJECT_ID, uid, id, now.toISOString()).first<{used_version_code: number}>();
  }
  private async event(claimId: string, key: string): Promise<EventRow | null> {
    return this.env.DB.prepare('SELECT * FROM legacy_review_events WHERE claim_id = ? AND operation_key_hash = ?')
      .bind(claimId, await reviewDigest(key)).first<EventRow>();
  }

  async addEvidence(operator: Session, uid: string, claimId: string, revision: number, input: EvidenceInput, key: string, now = new Date()) {
    const actor = this.owner(operator, now);
    exact(input, ['kind', 'category', 'originKey', 'reference', 'authorityEvidenceId']);
    check(['HISTORY','AUXILIARY','DECLARATION'].includes(input.kind));
    check(['SUPPORT_RECORD','INSTALL_CONTEXT','USAGE_CONTEXT','ACCOUNT_CONTINUITY','USER_STATEMENT'].includes(input.category));
    check(opaque(input.originKey) && opaque(input.reference) && opaque(key) && Number.isSafeInteger(revision) && revision >= 0);
    check(input.kind !== 'DECLARATION' || input.category === 'USER_STATEMENT');
    if (input.kind === 'HISTORY') {
      check(opaque(input.authorityEvidenceId));
      const history = await this.history(uid, input.authorityEvidenceId, now);
      check(history && history.used_version_code <= LEGACY_MAX_HISTORICAL_VERSION_CODE, 'MORE_EVIDENCE_REQUIRED');
    } else check(input.authorityEvidenceId === undefined);
    const row = await this.read(uid, claimId); check(row, 'NOT_FOUND');
    const fingerprint = await reviewDigest([uid, revision, input.kind, input.category, input.originKey, input.reference, input.authorityEvidenceId ?? null]);
    const previous = await this.event(claimId, key);
    if (previous) { check(previous.request_hash === fingerprint, 'CONFLICT'); return this.view(row, now); }
    check(row.claim.state === 'UNDER_REVIEW' && row.revision === revision, 'CONFLICT');
    const id = crypto.randomUUID(); const nextRevision = revision + 1;
    await this.env.DB.batch([
      this.env.DB.prepare(`INSERT INTO legacy_review_evidence
        SELECT ?, claim_id, ?, ?, ?, ?, ?, ?, ?, ? FROM legacy_claims
        WHERE claim_id = ? AND firebase_project_id = ? AND uid = ? AND state = 'UNDER_REVIEW' AND revision = ?
        ON CONFLICT DO NOTHING`)
        .bind(id, input.kind, input.category, input.originKey, input.reference, input.authorityEvidenceId ?? null,
          fingerprint, actor, now.toISOString(), claimId, this.env.FIREBASE_PROJECT_ID, uid, revision),
      this.env.DB.prepare(`INSERT INTO legacy_review_events
        SELECT ?, ?, ?, ?, 'EVIDENCE_ADDED', 'NONE', ?, ?, ?, ?, ?
        WHERE EXISTS(SELECT 1 FROM legacy_review_evidence WHERE evidence_id = ?) ON CONFLICT DO NOTHING`)
        .bind(id, claimId, revision, nextRevision, actor, now.toISOString(), await reviewDigest(key), fingerprint,
          JSON.stringify({evidenceId: id}), id),
      this.env.DB.prepare(`UPDATE legacy_claims SET revision = ? WHERE claim_id = ? AND revision = ?
        AND EXISTS(SELECT 1 FROM legacy_review_events WHERE event_id = ?)`)
        .bind(nextRevision, claimId, revision, id)
    ]);
    const saved = await this.event(claimId, key); check(saved?.request_hash === fingerprint, 'CONFLICT');
    return this.view((await this.read(uid, claimId))!, now);
  }

  /** Approval is an audited owner judgment READY_FOR_GRANT, not GRANTED. No
   * new grant/snapshot/provisional is emitted here. Private issue checks this
   * exact revision and authority; a stale approval is not a reusable voucher.
   */
  async decide(operator: Session, uid: string, claimId: string, revision: number, input: DecisionInput, key: string, now = new Date()) {
    const actor = this.owner(operator, now);
    exact(input, ['outcome','evidenceIds','reason','ownershipConfirmed','contradictionsCleared','legacyUseConcluded']);
    check(['APPROVE','REJECT','REQUEST_MORE','OWNERSHIP_CONFLICT'].includes(input.outcome));
    check(typeof input.reason === 'string' && input.reason.trim().length > 0 && input.reason.length <= 1000 &&
      Array.isArray(input.evidenceIds) && input.evidenceIds.length <= 16 && input.evidenceIds.every(opaque) &&
      new Set(input.evidenceIds).size === input.evidenceIds.length && opaque(key) && Number.isSafeInteger(revision) && revision >= 0);
    check([input.ownershipConfirmed, input.contradictionsCleared, input.legacyUseConcluded].every(flag => typeof flag === 'boolean'));
    const row = await this.read(uid, claimId); check(row, 'NOT_FOUND');
    const fingerprint = await reviewDigest([uid, revision, input.outcome, [...input.evidenceIds].sort(), input.reason,
      input.ownershipConfirmed, input.contradictionsCleared, input.legacyUseConcluded]);
    const previous = await this.event(claimId, key);
    if (previous) { check(previous.request_hash === fingerprint, 'CONFLICT'); return {view: this.view(row, now), decision: previous}; }
    check((row.claim.state === 'UNDER_REVIEW' || (row.claim.state === 'REJECTED' && input.outcome === 'REQUEST_MORE')) &&
      row.revision === revision, 'CONFLICT');
    const existing = await this.env.DB.prepare('SELECT 1 AS present FROM legacy_grants WHERE firebase_project_id = ? AND uid = ?')
      .bind(this.env.FIREBASE_PROJECT_ID, uid).first();
    check(!existing, 'GRANT_PRESERVED');
    const all = await this.env.DB.prepare('SELECT * FROM legacy_review_evidence WHERE claim_id = ?').bind(claimId).all<EvidenceRow>();
    const selected = input.evidenceIds.map(id => all.results.find(evidence => evidence.evidence_id === id));
    check(selected.every(Boolean), 'NOT_FOUND');
    const evidence = selected as EvidenceRow[];
    let basis = 'NONE';
    if (input.outcome === 'APPROVE') {
      const identity = await this.env.DB.prepare('SELECT linked_google FROM legacy_claim_identities WHERE claim_id = ?')
        .bind(claimId).first<{linked_google: number}>();
      check(identity?.linked_google === 1, 'LINK_REQUIRED');
      check(input.ownershipConfirmed && input.contradictionsCleared && input.legacyUseConcluded, 'MORE_EVIDENCE_REQUIRED');
      for (const item of evidence.filter(item => item.kind === 'HISTORY')) {
        const history = await this.history(uid, item.authority_evidence_id!, now);
        if (history && history.used_version_code <= LEGACY_MAX_HISTORICAL_VERSION_CODE) basis = 'VERIFIABLE_HISTORY';
      }
      if (basis === 'NONE') {
        const auxiliary = evidence.filter(item => item.kind === 'AUXILIARY' && item.category !== 'USER_STATEMENT');
        // v1 sufficiency gate for a HUMAN assessment, never an automatic
        // classifier or cryptographic assertion. Correlated copies count once.
        const pair = auxiliary.some(a => auxiliary.some(b => a.origin_key !== b.origin_key && a.category !== b.category &&
          ['SUPPORT_RECORD','ACCOUNT_CONTINUITY'].includes(a.category) &&
          ['INSTALL_CONTEXT','USAGE_CONTEXT','ACCOUNT_CONTINUITY'].includes(b.category)));
        check(pair, 'MORE_EVIDENCE_REQUIRED');
        basis = 'ASSISTED_ASSESSMENT';
      }
    }
    const eventId = crypto.randomUUID(); const nextRevision = revision + 1;
    const resolved = input.outcome === 'REJECT';
    const claim: LegacyClaim = {...row.claim, state: resolved ? 'REJECTED' : 'UNDER_REVIEW',
      resolvedAt: resolved ? now.toISOString() : null, reviewer: resolved ? actor : null, provisionalUntil: null};
    check(validateLegacyClaim(claim));
    const details = JSON.stringify({reason: input.reason, evidenceIds: [...input.evidenceIds].sort(),
      ownershipConfirmed: input.ownershipConfirmed, contradictionsCleared: input.contradictionsCleared,
      legacyUseConcluded: input.legacyUseConcluded,
      grantIssuance: input.outcome === 'APPROVE' ? 'AWAITING_AUTHORIZED_ISSUANCE' : 'NONE',
      assessmentVersion: 's06-owner-assessment-v1'});
    await this.env.DB.batch([
      this.env.DB.prepare(`INSERT INTO legacy_review_events
        SELECT ?, claim_id, ?, ?, ?, ?, ?, ?, ?, ?, ? FROM legacy_claims
        WHERE claim_id = ? AND firebase_project_id = ? AND uid = ? AND revision = ? AND state = ?
        AND NOT EXISTS(SELECT 1 FROM legacy_grants WHERE firebase_project_id = ? AND uid = ?)
        ON CONFLICT DO NOTHING`)
        .bind(eventId, revision, nextRevision, input.outcome, basis, actor, now.toISOString(), await reviewDigest(key), fingerprint,
          details, claimId, this.env.FIREBASE_PROJECT_ID, uid, revision, row.claim.state, this.env.FIREBASE_PROJECT_ID, uid),
      this.env.DB.prepare(`UPDATE legacy_claims SET state = ?, resolved_at = ?, reviewer = ?, provisional_until = NULL,
        claim_json = ?, revision = ? WHERE claim_id = ? AND revision = ?
        AND EXISTS(SELECT 1 FROM legacy_review_events WHERE event_id = ?)`)
        .bind(claim.state, claim.resolvedAt, claim.reviewer, JSON.stringify(claim), nextRevision, claimId, revision, eventId)
    ]);
    const decision = await this.event(claimId, key); check(decision?.request_hash === fingerprint, 'CONFLICT');
    return {view: this.view((await this.read(uid, claimId))!, now), decision};
  }

  /** Explicit private registration of the approved rule, by the sole owner.
   * No arbitrary cutoff, feature set, retention period or operator can be supplied.
   * The migration itself seeds nothing. Existing policies are never overwritten.
   */
  async authorizePolicy(operator: Session, now = new Date()) {
    const actor = this.owner(operator, now);
    await this.env.DB.prepare(`INSERT INTO legacy_policies
      (firebase_project_id, policy_version, cutoff_utc, core_baseline, migration_version,
       approved_at, approved_by, accepted_evidence_kinds_json, criteria_reference, maximum_historical_version_code)
      VALUES (?, ?, NULL, ?, 's06-owner-assessment-v1', ?, ?, ?, 'OQ-02-LEGACY-POLICY:owner-assessment-v1', 12)
      ON CONFLICT DO NOTHING`).bind(this.env.FIREBASE_PROJECT_ID, POLICY, LEGACY_CORE_BASELINE,
        now.toISOString(), actor, JSON.stringify(['ASSISTED_ASSESSMENT','VERIFIABLE_HISTORY'])).run();
    const policy = await this.issuancePolicy(actor, POLICY, now);
    return {policyVersion: policy.policy_version, coreBaseline: policy.core_baseline};
  }
  private async issuancePolicy(actor: string, version: string, now: Date) {
    const p = await this.env.DB.prepare(`SELECT * FROM legacy_policies
      WHERE firebase_project_id = ? AND policy_version = ?`)
      .bind(this.env.FIREBASE_PROJECT_ID, version).first<{
        policy_version: string; core_baseline: string; migration_version: string; approved_by: string;
        approved_at: string; cutoff_utc: string | null; maximum_historical_version_code: number;
        accepted_evidence_kinds_json: string;
      }>();
    check(p && p.approved_by === actor && p.cutoff_utc === null &&
      p.maximum_historical_version_code === 12 && p.core_baseline === LEGACY_CORE_BASELINE &&
      Number.isFinite(Date.parse(p.approved_at)) && Date.parse(p.approved_at) <= now.getTime(), 'POLICY_REQUIRED');
    return p;
  }
  private async issued(uid: string, claimId: string, fingerprint: string, now: Date) {
    const receipt = await this.env.DB.prepare(`SELECT grant_json, request_hash FROM legacy_review_issuances
      WHERE firebase_project_id = ? AND uid = ? AND claim_id = ?`)
      .bind(this.env.FIREBASE_PROJECT_ID, uid, claimId).first<{grant_json: string; request_hash: string}>();
    check(receipt?.request_hash === fingerprint, 'CONFLICT');
    const stored = await this.env.DB.prepare('SELECT grant_json FROM legacy_grants WHERE firebase_project_id = ? AND uid = ?')
      .bind(this.env.FIREBASE_PROJECT_ID, uid).first<{grant_json: string}>();
    const grant: unknown = stored ? JSON.parse(stored.grant_json) : null;
    check(validateLegacyGrant(grant) && grant.uid === uid && stored!.grant_json === receipt.grant_json, 'CONFLICT');
    const row = await this.read(uid, claimId);
    check(row?.claim.state === 'GRANTED', 'CONFLICT');
    return {view: this.view(row, now), grant};
  }

  /** Private owner command, not an HTTP endpoint. Its sole write inserts a
   * completion receipt: SQL guards + trigger atomically issue the immutable S05
   * source and resolve the claim. A receipt cannot survive failed completion.
   */
  async issue(operator: Session, uid: string, claimId: string, revision: number,
    input: {approvalEventId: string}, key: string, now = new Date()) {
    const actor = this.owner(operator, now);
    exact(input, ['approvalEventId']);
    check(opaque(input.approvalEventId) && opaque(key) && Number.isSafeInteger(revision) && revision >= 1);
    const row = await this.read(uid, claimId); check(row, 'NOT_FOUND');
    const fingerprint = await reviewDigest([this.env.FIREBASE_PROJECT_ID, uid, claimId, revision, input.approvalEventId, actor]);
    const prior = await this.env.DB.prepare('SELECT request_hash FROM legacy_review_issuances WHERE claim_id = ?')
      .bind(claimId).first<{request_hash: string}>();
    if (prior) {
      check(prior.request_hash === fingerprint, 'CONFLICT');
      return {...await this.issued(uid, claimId, fingerprint, now), idempotent: true};
    }
    check(row.claim.state === 'UNDER_REVIEW' && row.revision === revision, 'CONFLICT');
    const existing = await this.env.DB.prepare('SELECT 1 FROM legacy_grants WHERE firebase_project_id = ? AND uid = ?')
      .bind(this.env.FIREBASE_PROJECT_ID, uid).first();
    check(!existing, 'GRANT_PRESERVED');
    const approval = await this.env.DB.prepare('SELECT * FROM legacy_review_events WHERE claim_id = ? AND event_id = ?')
      .bind(claimId, input.approvalEventId).first<EventRow>();
    check(approval?.kind === 'APPROVE' && approval.to_revision === revision && approval.actor_uid === actor &&
      ['ASSISTED_ASSESSMENT','VERIFIABLE_HISTORY'].includes(approval.basis), 'CONFLICT');
    const details = JSON.parse(approval.details_json);
    check(details.ownershipConfirmed === true && details.contradictionsCleared === true && details.legacyUseConcluded === true &&
      typeof details.reason === 'string' && details.reason.trim().length > 0, 'MORE_EVIDENCE_REQUIRED');
    const policy = await this.issuancePolicy(actor, row.claim.policyVersion, now);
    check(Date.parse(policy.approved_at) <= Date.parse(approval.occurred_at), 'POLICY_REQUIRED');
    const accepted: unknown = JSON.parse(policy.accepted_evidence_kinds_json);
    check(Array.isArray(accepted) && accepted.includes(approval.basis), 'POLICY_REQUIRED');
    const evidenceHash = await reviewDigest(['S06_AUTHORIZED_REVIEW', this.env.FIREBASE_PROJECT_ID, uid,
      claimId, revision, approval.event_id, approval.request_hash, approval.basis, actor]);
    const grant: LegacyGrant = {uid, status: 'GRANTED', policyCutoffUtc: null, policyVersion: policy.policy_version,
      legacyGrantedAt: now.toISOString(), legacyEvidenceType: approval.basis, evidenceHash,
      legacyCoreVersionBaseline: policy.core_baseline, migrationVersion: policy.migration_version,
      grantRevision: 1, decisionActor: actor};
    check(validateLegacyGrant(grant));
    const inserted = await this.env.DB.prepare(`INSERT INTO legacy_review_issuances
      SELECT ?, firebase_project_id, uid, claim_id, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? FROM legacy_claims
      WHERE claim_id = ? AND firebase_project_id = ? AND uid = ? AND revision = ? AND state = 'UNDER_REVIEW'
        AND NOT EXISTS(SELECT 1 FROM legacy_review_issuances WHERE claim_id = ?)`)
      .bind(crypto.randomUUID(), approval.event_id, revision, revision + 1, approval.basis, actor, now.toISOString(),
        await reviewDigest(key), fingerprint, evidenceHash, policy.policy_version, JSON.stringify(grant),
        claimId, this.env.FIREBASE_PROJECT_ID, uid, revision, claimId).run();
    return {...await this.issued(uid, claimId, fingerprint, now), idempotent: inserted.meta.changes === 0};
  }
}
