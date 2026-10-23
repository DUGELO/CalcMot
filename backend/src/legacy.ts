import { validateLegacyGrant } from './generated/legacy-validator.js';
import type { Env } from './index';
import type { Session } from './auth';

/** Owner-approved commercial rule; this constant does not activate any grants. */
export const LEGACY_MAX_HISTORICAL_VERSION_CODE = 12;
export const LEGACY_CORE_BASELINE = 'calcmot-core-3.5-vc12';

/** Frozen S01 source of Core rights; never a Premium snapshot. */
export interface LegacyGrant {
  uid: string; status: 'GRANTED'; policyCutoffUtc: string | null; policyVersion: string;
  legacyGrantedAt: string; legacyEvidenceType: string; evidenceHash: string;
  legacyCoreVersionBaseline: string; migrationVersion: string; grantRevision: number;
  decisionActor: string;
}
/** Frozen record; S06 workflow is implemented separately in legacy-review.ts. */
export interface LegacyClaim {
  claimId: string; uid: string; state: 'UNDER_REVIEW' | 'GRANTED' | 'REJECTED';
  evidenceType: string; evidenceHash: string | null; openedAt: string; resolvedAt: string | null;
  reviewer: string | null; provisionalUntil: string | null; policyVersion: string;
}
interface PolicyRow {
  policy_version: string; cutoff_utc: string | null; core_baseline: string;
  migration_version: string; approved_by: string; approved_at: string;
  accepted_evidence_kinds_json: string; criteria_reference: string;
  maximum_historical_version_code: number | null;
}
interface EvidenceRow {
  evidence_id: string; uid: string; evidence_hash: string; evidence_kind: string;
  verification: string; qualifying_at: string | null; verified_by: string | null;
}
interface HistoricalUseRow {
  used_version_code: number; verification_basis: string; history_reference: string;
  verified_at: string; verified_by: string;
}
export type GrantResult =
  | { granted: true; grant: LegacyGrant; idempotent: boolean }
  | { granted: false; reason: 'LINK_REQUIRED' | 'POLICY_REQUIRED' | 'INVALID_EVIDENCE' |
      'NOT_ELIGIBLE' | 'OWNERSHIP_REVIEW_REQUIRED' | 'HISTORICAL_PROOF_REQUIRED' };
const utc = (value: string): boolean =>
  /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d{1,3})?Z$/.test(value) && Number.isFinite(Date.parse(value));

/** Private D1 only. Session must originate in verifySession.
 * S06 may read this source; no HTTP grant writer is enabled.
 * Policy and verified evidence have no writer here: OQ-02 operations must supply the trusted
 * approval/verification process before real migration can be wired.
 */
export class LegacyGrantService {
  constructor(private readonly env: Pick<Env, 'DB' | 'FIREBASE_PROJECT_ID'>) {}

  private uid(session: Session): string {
    const project = this.env.FIREBASE_PROJECT_ID;
    if (!project || session.uid !== session.claims.sub || session.uid.length < 1 ||
        session.uid.length > 128 || session.claims.aud !== project ||
        session.claims.iss !== `https://securetoken.google.com/${project}`)
      throw new Error('Invalid verified identity context');
    return session.uid;
  }

  private linked(session: Session): boolean {
    const firebase = session.claims.firebase as { identities?: Record<string, unknown> } | undefined;
    const google = firebase?.identities?.['google.com'];
    return Array.isArray(google) && google.some(value => typeof value === 'string' && value.length > 0);
  }

  async getGrant(session: Session): Promise<LegacyGrant | null> {
    const uid = this.uid(session);
    const row = await this.env.DB.prepare(
      'SELECT grant_json FROM legacy_grants WHERE firebase_project_id = ? AND uid = ?'
    ).bind(this.env.FIREBASE_PROJECT_ID, uid).first<{ grant_json: string }>();
    if (!row) return null;
    const grant: unknown = JSON.parse(row.grant_json);
    if (!validateLegacyGrant(grant) || grant.uid !== uid) throw new Error('Invalid stored legacy grant');
    return grant;
  }

  /** Local evidence is always AUXILIARY, never verified by dates/hash sent by a client. */
  async observeLocal(session: Session, evidenceHash: string, now = new Date()): Promise<string> {
    const uid = this.uid(session);
    if (!/^[a-f0-9]{64}$/.test(evidenceHash)) throw new Error('Invalid evidence digest');
    const project = this.env.FIREBASE_PROJECT_ID;
    const id = crypto.randomUUID();
    await this.env.DB.prepare(`INSERT INTO legacy_evidence
      (firebase_project_id, evidence_id, uid, evidence_hash, evidence_kind, verification, observed_at)
      VALUES (?, ?, ?, ?, 'LOCAL_INSTALL_METADATA', 'AUXILIARY', ?)
      ON CONFLICT(firebase_project_id, uid, evidence_kind, evidence_hash) DO NOTHING`)
      .bind(project, id, uid, evidenceHash, now.toISOString()).run();
    const row = await this.env.DB.prepare(`SELECT evidence_id FROM legacy_evidence
      WHERE firebase_project_id = ? AND uid = ? AND evidence_kind = 'LOCAL_INSTALL_METADATA' AND evidence_hash = ?`)
      .bind(project, uid, evidenceHash).first<{ evidence_id: string }>();
    if (!row) throw new Error('Observation not stored');
    return row.evidence_id;
  }

  async recordGrant(session: Session, evidenceId: string, policyVersion: string, now = new Date()): Promise<GrantResult> {
    const uid = this.uid(session);
    const project = this.env.FIREBASE_PROJECT_ID;
    if (!this.linked(session)) return { granted: false, reason: 'LINK_REQUIRED' };
    const evidence = await this.env.DB.prepare(`SELECT evidence_id, uid, evidence_hash, evidence_kind,
      verification, qualifying_at, verified_by FROM legacy_evidence
      WHERE firebase_project_id = ? AND evidence_id = ?`)
      .bind(project, evidenceId).first<EvidenceRow>();
    if (!evidence) return { granted: false, reason: 'INVALID_EVIDENCE' };
    if (evidence.uid !== uid) return { granted: false, reason: 'OWNERSHIP_REVIEW_REQUIRED' };
    if (evidence.verification !== 'VERIFIED' || !evidence.verified_by || !evidence.qualifying_at ||
        !utc(evidence.qualifying_at) || Date.parse(evidence.qualifying_at) > now.getTime() ||
        !['SERVER_BRIDGE_OBSERVATION', 'SUPPORT_VERIFIED'].includes(evidence.evidence_kind))
      return { granted: false, reason: 'INVALID_EVIDENCE' };
    const existing = await this.getGrant(session);
    if (existing) return existing.evidenceHash === evidence.evidence_hash && existing.policyVersion === policyVersion
      ? { granted: true, grant: existing, idempotent: true }
      : { granted: false, reason: 'OWNERSHIP_REVIEW_REQUIRED' };
    const policy = await this.env.DB.prepare(`SELECT policy_version, cutoff_utc, core_baseline,
      migration_version, approved_by, approved_at, accepted_evidence_kinds_json, criteria_reference,
      maximum_historical_version_code FROM legacy_policies
      WHERE firebase_project_id = ? AND policy_version = ?`)
      .bind(project, policyVersion).first<PolicyRow>();
    if (!policy || !policy.core_baseline || !policy.migration_version || !policy.approved_by || !policy.criteria_reference ||
        !utc(policy.approved_at) || Date.parse(policy.approved_at) > now.getTime() ||
        policy.cutoff_utc !== null || policy.maximum_historical_version_code !== LEGACY_MAX_HISTORICAL_VERSION_CODE ||
        policy.core_baseline !== LEGACY_CORE_BASELINE)
      return { granted: false, reason: 'POLICY_REQUIRED' };
    const accepted: unknown = JSON.parse(policy.accepted_evidence_kinds_json);
    if (!Array.isArray(accepted) || !accepted.includes(evidence.evidence_kind))
      return { granted: false, reason: 'NOT_ELIGIBLE' };
    // A migration registration, linked UID, or current app attestation is NOT history.
    // Only an independently verified history record can establish use of <=12.
    const history = await this.env.DB.prepare(`SELECT used_version_code, verification_basis,
      history_reference, verified_at, verified_by FROM legacy_historical_use
      WHERE firebase_project_id = ? AND evidence_id = ?`)
      .bind(project, evidenceId).first<HistoricalUseRow>();
    if (!history || !history.history_reference || !utc(history.verified_at) ||
        Date.parse(history.verified_at) > now.getTime() || history.verified_by !== evidence.verified_by ||
        Date.parse(history.verified_at) < Date.parse(evidence.qualifying_at) ||
        !['INDEPENDENT_HISTORICAL_RECORD', 'ASSISTED_REVIEW_VERIFIED'].includes(history.verification_basis))
      return { granted: false, reason: 'HISTORICAL_PROOF_REQUIRED' };
    // Nonqualifying proof is not a permanent classification of the person: they
    // may later recover independently verified earlier use. Never infer it from
    // the currently installed version or from the Google account creation time.
    if (!Number.isSafeInteger(history.used_version_code) || history.used_version_code < 1 ||
        history.used_version_code > LEGACY_MAX_HISTORICAL_VERSION_CODE)
      return { granted: false, reason: 'NOT_ELIGIBLE' };
    const grant: LegacyGrant = {
      uid, status: 'GRANTED', policyCutoffUtc: policy.cutoff_utc, policyVersion: policy.policy_version,
      legacyGrantedAt: now.toISOString(), legacyEvidenceType: evidence.evidence_kind,
      evidenceHash: evidence.evidence_hash, legacyCoreVersionBaseline: policy.core_baseline,
      migrationVersion: policy.migration_version, grantRevision: 1, decisionActor: evidence.verified_by
    };
    if (!validateLegacyGrant(grant)) throw new Error('Invalid legacy grant');
    // Unique constraints arbitrate concurrent retries/ownership; S07 owns snapshots.
    const inserted = await this.env.DB.prepare(`INSERT INTO legacy_grants
      (firebase_project_id, uid, status, policy_cutoff_utc, policy_version, legacy_granted_at,
       legacy_evidence_type, evidence_hash, legacy_core_version_baseline, migration_version,
       grant_revision, decision_actor, grant_json) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      ON CONFLICT DO NOTHING`)
      .bind(project, uid, grant.status, grant.policyCutoffUtc, grant.policyVersion, grant.legacyGrantedAt,
        grant.legacyEvidenceType, grant.evidenceHash, grant.legacyCoreVersionBaseline,
        grant.migrationVersion, grant.grantRevision, grant.decisionActor, JSON.stringify(grant)).run();
    const stored = await this.getGrant(session);
    if (!stored || stored.evidenceHash !== grant.evidenceHash || stored.policyVersion !== grant.policyVersion)
      return { granted: false, reason: 'OWNERSHIP_REVIEW_REQUIRED' };
    return { granted: true, grant: stored, idempotent: inserted.meta.changes === 0 };
  }

  /** Recovery intention only; no S06 decision/provisional access/transfer. */
  async prepareRecovery(session: Session, evidenceId: string, now = new Date()): Promise<string> {
    const uid = this.uid(session);
    if (typeof evidenceId !== 'string' || evidenceId.length < 1 || evidenceId.length > 128)
      throw new Error('Invalid recovery reference');
    await this.env.DB.prepare(`INSERT INTO legacy_recovery_requests
      (firebase_project_id, uid, evidence_id, opened_at, state)
      VALUES (?, ?, ?, ?, 'PENDING_OWNERSHIP_REVIEW')
      ON CONFLICT(firebase_project_id, uid, evidence_id) DO NOTHING`)
      .bind(this.env.FIREBASE_PROJECT_ID, uid, evidenceId, now.toISOString()).run();
    return 'PENDING_OWNERSHIP_REVIEW';
  }
}
