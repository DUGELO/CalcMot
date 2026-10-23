-- Approved commercial boundary: historical use through 3.5 / versionCode 12.
-- Additive, no policies/proofs/grants seeded. Existing grants remain untouched.
ALTER TABLE legacy_policies ADD COLUMN maximum_historical_version_code INTEGER
    CHECK(maximum_historical_version_code IS NULL OR maximum_historical_version_code = 12);

-- This is an independently verified HISTORICAL USE record, not current app
-- attestation or client metadata. No HTTP writer exists. The verifier/reviewer
-- must actually establish the referenced history and its owner before inserting.
CREATE TABLE legacy_historical_use (
    firebase_project_id TEXT NOT NULL,
    evidence_id TEXT NOT NULL,
    used_version_code INTEGER NOT NULL CHECK(used_version_code > 0),
    verification_basis TEXT NOT NULL CHECK(verification_basis IN ('INDEPENDENT_HISTORICAL_RECORD', 'ASSISTED_REVIEW_VERIFIED')),
    history_reference TEXT NOT NULL CHECK(length(history_reference) > 0),
    verified_at TEXT NOT NULL CHECK(verified_at LIKE '%Z' AND julianday(verified_at) IS NOT NULL),
    verified_by TEXT NOT NULL CHECK(length(verified_by) > 0),
    PRIMARY KEY(firebase_project_id, evidence_id),
    FOREIGN KEY(firebase_project_id, evidence_id) REFERENCES legacy_evidence(firebase_project_id, evidence_id)
) WITHOUT ROWID;

CREATE TRIGGER legacy_history_no_update BEFORE UPDATE ON legacy_historical_use
BEGIN SELECT RAISE(ABORT, 'verified legacy history is immutable'); END;
CREATE TRIGGER legacy_history_no_delete BEFORE DELETE ON legacy_historical_use
BEGIN SELECT RAISE(ABORT, 'verified legacy history is immutable'); END;

-- Additional guard over 0003. NULL legacy cutoffs retain the frozen wire schema
-- without replacing the approved version boundary with a calendar cutoff.
-- Old policy rows missing the new rule cannot issue NEW grants. Old grants are
-- not revoked or changed by this migration.
CREATE TRIGGER legacy_grant_version_authority BEFORE INSERT ON legacy_grants
WHEN NOT EXISTS (
    SELECT 1 FROM legacy_policies p
    JOIN legacy_evidence e ON e.firebase_project_id = p.firebase_project_id
    JOIN legacy_historical_use h ON h.firebase_project_id = e.firebase_project_id AND h.evidence_id = e.evidence_id
    WHERE p.firebase_project_id = NEW.firebase_project_id AND p.policy_version = NEW.policy_version
      AND p.maximum_historical_version_code = 12 AND p.cutoff_utc IS NULL
      AND p.core_baseline = 'calcmot-core-3.5-vc12'
      AND e.uid = NEW.uid AND e.evidence_hash = NEW.evidence_hash AND e.verification = 'VERIFIED'
      AND h.used_version_code <= p.maximum_historical_version_code
      AND h.verified_by = NEW.decision_actor AND h.verified_by = e.verified_by
      AND julianday(h.verified_at) <= julianday(NEW.legacy_granted_at)
      AND julianday(h.verified_at) >= julianday(e.qualifying_at)
)
BEGIN SELECT RAISE(ABORT, 'legacy grant requires verified historical use through versionCode 12'); END;
