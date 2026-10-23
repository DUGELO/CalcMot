-- S05: additive authority checks over the existing local 0002 draft.
-- No seeded policies, evidence, grants or snapshots; never applied remotely here.
CREATE TABLE legacy_policies (
    firebase_project_id TEXT NOT NULL CHECK(length(firebase_project_id) > 0),
    policy_version TEXT NOT NULL CHECK(length(policy_version) > 0),
    cutoff_utc TEXT CHECK(cutoff_utc IS NULL OR (cutoff_utc LIKE '%Z' AND julianday(cutoff_utc) IS NOT NULL)),
    core_baseline TEXT NOT NULL CHECK(length(core_baseline) > 0),
    migration_version TEXT NOT NULL CHECK(length(migration_version) > 0),
    approved_at TEXT NOT NULL CHECK(approved_at LIKE '%Z' AND julianday(approved_at) IS NOT NULL),
    approved_by TEXT NOT NULL CHECK(length(approved_by) > 0),
    accepted_evidence_kinds_json TEXT NOT NULL CHECK(json_valid(accepted_evidence_kinds_json)
        AND json_type(accepted_evidence_kinds_json) = 'array' AND json_array_length(accepted_evidence_kinds_json) > 0),
    criteria_reference TEXT NOT NULL CHECK(length(criteria_reference) > 0),
    CHECK(cutoff_utc IS NULL OR julianday(cutoff_utc) > julianday(approved_at)),
    PRIMARY KEY(firebase_project_id, policy_version)
) WITHOUT ROWID;

CREATE TABLE legacy_evidence (
    firebase_project_id TEXT NOT NULL CHECK(length(firebase_project_id) > 0),
    evidence_id TEXT NOT NULL CHECK(length(evidence_id) BETWEEN 1 AND 128),
    uid TEXT NOT NULL CHECK(length(uid) BETWEEN 1 AND 128),
    evidence_hash TEXT NOT NULL CHECK(length(evidence_hash) = 64 AND evidence_hash NOT GLOB '*[^0-9a-f]*'),
    evidence_kind TEXT NOT NULL CHECK(evidence_kind IN ('LOCAL_INSTALL_METADATA', 'SERVER_BRIDGE_OBSERVATION', 'SUPPORT_VERIFIED')),
    verification TEXT NOT NULL CHECK(verification IN ('AUXILIARY', 'VERIFIED')),
    observed_at TEXT NOT NULL CHECK(observed_at LIKE '%Z' AND julianday(observed_at) IS NOT NULL),
    qualifying_at TEXT CHECK(qualifying_at IS NULL OR (qualifying_at LIKE '%Z' AND julianday(qualifying_at) IS NOT NULL)),
    verified_by TEXT,
    CHECK((verification = 'AUXILIARY' AND qualifying_at IS NULL AND verified_by IS NULL)
       OR (verification = 'VERIFIED' AND evidence_kind != 'LOCAL_INSTALL_METADATA'
           AND qualifying_at IS NOT NULL AND verified_by IS NOT NULL AND length(verified_by) > 0
           AND julianday(qualifying_at) <= julianday(observed_at))),
    PRIMARY KEY(firebase_project_id, evidence_id),
    UNIQUE(firebase_project_id, uid, evidence_kind, evidence_hash)
) WITHOUT ROWID;

-- A verified proof cannot silently acquire a different owner.
CREATE UNIQUE INDEX legacy_verified_evidence_owner
ON legacy_evidence(firebase_project_id, evidence_hash) WHERE verification = 'VERIFIED';
CREATE UNIQUE INDEX legacy_grant_single_evidence_owner
ON legacy_grants(firebase_project_id, evidence_hash);

CREATE TABLE legacy_recovery_requests (
    firebase_project_id TEXT NOT NULL CHECK(length(firebase_project_id) > 0),
    uid TEXT NOT NULL CHECK(length(uid) BETWEEN 1 AND 128),
    evidence_id TEXT NOT NULL CHECK(length(evidence_id) BETWEEN 1 AND 128),
    opened_at TEXT NOT NULL CHECK(opened_at LIKE '%Z' AND julianday(opened_at) IS NOT NULL),
    state TEXT NOT NULL CHECK(state = 'PENDING_OWNERSHIP_REVIEW'),
    PRIMARY KEY(firebase_project_id, uid, evidence_id)
) WITHOUT ROWID;

CREATE TRIGGER legacy_policy_no_update BEFORE UPDATE ON legacy_policies
BEGIN SELECT RAISE(ABORT, 'legacy policy is immutable'); END;
CREATE TRIGGER legacy_policy_no_delete BEFORE DELETE ON legacy_policies
BEGIN SELECT RAISE(ABORT, 'legacy policy is immutable'); END;
CREATE TRIGGER legacy_evidence_no_update BEFORE UPDATE ON legacy_evidence
BEGIN SELECT RAISE(ABORT, 'legacy evidence is immutable'); END;
CREATE TRIGGER legacy_evidence_no_delete BEFORE DELETE ON legacy_evidence
BEGIN SELECT RAISE(ABORT, 'legacy evidence is immutable'); END;
CREATE TRIGGER legacy_recovery_no_update BEFORE UPDATE ON legacy_recovery_requests
BEGIN SELECT RAISE(ABORT, 'recovery requires an authorized review process'); END;

-- Validate column/JSON agreement and require independently verified evidence and
-- an approved, matching policy. Existing records are preserved, never rewritten.
CREATE TRIGGER legacy_grant_authority BEFORE INSERT ON legacy_grants
WHEN COALESCE(
    json_type(NEW.grant_json) = 'object'
    AND json_type(NEW.grant_json, '$.policyCutoffUtc') IN ('null', 'text')
    AND json_extract(NEW.grant_json, '$.uid') = NEW.uid
    AND json_extract(NEW.grant_json, '$.status') = 'GRANTED'
    AND json_extract(NEW.grant_json, '$.policyCutoffUtc') IS NEW.policy_cutoff_utc
    AND json_extract(NEW.grant_json, '$.policyVersion') = NEW.policy_version
    AND json_extract(NEW.grant_json, '$.legacyGrantedAt') = NEW.legacy_granted_at
    AND json_extract(NEW.grant_json, '$.legacyEvidenceType') = NEW.legacy_evidence_type
    AND json_extract(NEW.grant_json, '$.evidenceHash') = NEW.evidence_hash
    AND json_extract(NEW.grant_json, '$.legacyCoreVersionBaseline') = NEW.legacy_core_version_baseline
    AND json_extract(NEW.grant_json, '$.migrationVersion') = NEW.migration_version
    AND json_type(NEW.grant_json, '$.grantRevision') = 'integer'
    AND json_extract(NEW.grant_json, '$.grantRevision') = NEW.grant_revision
    AND json_extract(NEW.grant_json, '$.decisionActor') = NEW.decision_actor
    AND julianday(NEW.legacy_granted_at) IS NOT NULL
    AND EXISTS(SELECT 1 FROM legacy_policies p JOIN legacy_evidence e
      ON e.firebase_project_id = p.firebase_project_id
      WHERE p.firebase_project_id = NEW.firebase_project_id AND p.policy_version = NEW.policy_version
        AND p.cutoff_utc IS NEW.policy_cutoff_utc
        AND p.core_baseline = NEW.legacy_core_version_baseline AND p.migration_version = NEW.migration_version
        AND EXISTS(SELECT 1 FROM json_each(p.accepted_evidence_kinds_json) k WHERE k.value = e.evidence_kind)
        AND julianday(p.approved_at) <= julianday(NEW.legacy_granted_at)
        AND e.uid = NEW.uid AND e.verification = 'VERIFIED'
        AND e.evidence_hash = NEW.evidence_hash AND e.evidence_kind = NEW.legacy_evidence_type
        AND e.verified_by = NEW.decision_actor
        AND julianday(e.observed_at) <= julianday(NEW.legacy_granted_at)
        AND (p.cutoff_utc IS NULL OR julianday(e.qualifying_at) < julianday(p.cutoff_utc))), 0) = 0
BEGIN SELECT RAISE(ABORT, 'legacy grant requires approved policy and verified ownership'); END;
