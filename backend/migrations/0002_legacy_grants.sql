-- S05: Persistent and versioned legacy grants and claims.
-- Immutable once granted. Authoritative server-side records only.

CREATE TABLE legacy_grants (
    firebase_project_id TEXT NOT NULL CHECK(length(firebase_project_id) > 0),
    uid TEXT NOT NULL CHECK(length(uid) BETWEEN 1 AND 128),
    status TEXT NOT NULL CHECK(status = 'GRANTED'),
    policy_cutoff_utc TEXT CHECK(policy_cutoff_utc IS NULL OR (length(policy_cutoff_utc) >= 20 AND policy_cutoff_utc LIKE '____-__-__T__:__:__%Z')),
    policy_version TEXT NOT NULL CHECK(length(policy_version) > 0),
    legacy_granted_at TEXT NOT NULL CHECK(length(legacy_granted_at) >= 20 AND legacy_granted_at LIKE '____-__-__T__:__:__%Z'),
    legacy_evidence_type TEXT NOT NULL CHECK(length(legacy_evidence_type) > 0),
    evidence_hash TEXT NOT NULL CHECK(length(evidence_hash) > 0),
    legacy_core_version_baseline TEXT NOT NULL CHECK(length(legacy_core_version_baseline) > 0),
    migration_version TEXT NOT NULL CHECK(length(migration_version) > 0),
    grant_revision INTEGER NOT NULL CHECK(grant_revision >= 0),
    decision_actor TEXT NOT NULL CHECK(length(decision_actor) > 0),
    grant_json TEXT NOT NULL CHECK(json_valid(grant_json)),
    PRIMARY KEY(firebase_project_id, uid)
) WITHOUT ROWID;

CREATE TRIGGER legacy_grant_immutable
BEFORE UPDATE ON legacy_grants
BEGIN
    SELECT RAISE(ABORT, 'legacy grants are immutable');
END;

CREATE TRIGGER legacy_grant_no_delete
BEFORE DELETE ON legacy_grants
BEGIN
    SELECT RAISE(ABORT, 'legacy grants cannot be deleted');
END;

CREATE TABLE legacy_claims (
    claim_id TEXT NOT NULL PRIMARY KEY CHECK(length(claim_id) BETWEEN 1 AND 128),
    firebase_project_id TEXT NOT NULL CHECK(length(firebase_project_id) > 0),
    uid TEXT NOT NULL CHECK(length(uid) BETWEEN 1 AND 128),
    state TEXT NOT NULL CHECK(state IN ('UNDER_REVIEW', 'GRANTED', 'REJECTED')),
    evidence_type TEXT NOT NULL CHECK(length(evidence_type) > 0),
    evidence_hash TEXT,
    opened_at TEXT NOT NULL CHECK(length(opened_at) >= 20 AND opened_at LIKE '____-__-__T__:__:__%Z'),
    resolved_at TEXT CHECK(resolved_at IS NULL OR (length(resolved_at) >= 20 AND resolved_at LIKE '____-__-__T__:__:__%Z')),
    reviewer TEXT,
    provisional_until TEXT CHECK(provisional_until IS NULL OR (length(provisional_until) >= 20 AND provisional_until LIKE '____-__-__T__:__:__%Z')),
    policy_version TEXT NOT NULL CHECK(length(policy_version) > 0),
    claim_json TEXT NOT NULL CHECK(json_valid(claim_json)),
    UNIQUE(firebase_project_id, uid)
);

CREATE TRIGGER legacy_claim_state_transition
BEFORE UPDATE ON legacy_claims
WHEN OLD.state != 'UNDER_REVIEW'
BEGIN
    SELECT RAISE(ABORT, 'resolved legacy claims cannot be modified');
END;
