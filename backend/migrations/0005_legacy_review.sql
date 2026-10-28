-- S06: private review workflow. No grant, policy or provisional access is seeded.
ALTER TABLE legacy_claims ADD COLUMN revision INTEGER NOT NULL DEFAULT 0 CHECK(revision >= 0);

CREATE TABLE legacy_claim_requests (
    firebase_project_id TEXT NOT NULL,
    uid TEXT NOT NULL,
    key_hash TEXT NOT NULL CHECK(length(key_hash) = 64),
    request_hash TEXT NOT NULL CHECK(length(request_hash) = 64),
    claim_id TEXT NOT NULL REFERENCES legacy_claims(claim_id),
    PRIMARY KEY(firebase_project_id, uid, key_hash)
) WITHOUT ROWID;
CREATE TABLE legacy_claim_identities (
    claim_id TEXT NOT NULL PRIMARY KEY REFERENCES legacy_claims(claim_id),
    linked_google INTEGER NOT NULL CHECK(linked_google IN (0, 1)),
    verified_at TEXT NOT NULL
) WITHOUT ROWID;
CREATE TABLE legacy_review_evidence (
    evidence_id TEXT NOT NULL PRIMARY KEY,
    claim_id TEXT NOT NULL REFERENCES legacy_claims(claim_id),
    kind TEXT NOT NULL CHECK(kind IN ('HISTORY', 'AUXILIARY', 'DECLARATION')),
    category TEXT NOT NULL,
    origin_key TEXT NOT NULL,
    reference TEXT NOT NULL,
    authority_evidence_id TEXT,
    request_hash TEXT NOT NULL,
    added_by TEXT NOT NULL,
    added_at TEXT NOT NULL,
    UNIQUE(claim_id, request_hash)
);
CREATE TABLE legacy_review_events (
    event_id TEXT NOT NULL PRIMARY KEY,
    claim_id TEXT NOT NULL REFERENCES legacy_claims(claim_id),
    from_revision INTEGER NOT NULL CHECK(from_revision >= 0),
    to_revision INTEGER NOT NULL CHECK(to_revision = from_revision + 1),
    kind TEXT NOT NULL CHECK(kind IN ('EVIDENCE_ADDED', 'APPROVE', 'REJECT', 'REQUEST_MORE', 'OWNERSHIP_CONFLICT', 'RESTORE')),
    basis TEXT NOT NULL CHECK(basis IN ('VERIFIABLE_HISTORY', 'ASSISTED_ASSESSMENT', 'EXISTING_GRANT', 'NONE')),
    actor_uid TEXT NOT NULL,
    occurred_at TEXT NOT NULL,
    operation_key_hash TEXT NOT NULL,
    request_hash TEXT NOT NULL,
    details_json TEXT NOT NULL CHECK(json_valid(details_json)),
    UNIQUE(claim_id, from_revision),
    UNIQUE(claim_id, operation_key_hash)
);

-- Reopening is an audited owner action; original rejection events stay immutable.
DROP TRIGGER legacy_claim_state_transition;
CREATE TRIGGER legacy_claim_state_transition BEFORE UPDATE ON legacy_claims
WHEN OLD.state != 'UNDER_REVIEW' AND NOT (
    OLD.state = 'REJECTED' AND (
      (NEW.state = 'GRANTED' AND EXISTS(SELECT 1 FROM legacy_grants g
        WHERE g.firebase_project_id = OLD.firebase_project_id AND g.uid = OLD.uid))
      OR (NEW.state = 'UNDER_REVIEW' AND EXISTS(SELECT 1 FROM legacy_review_events e
        WHERE e.claim_id = OLD.claim_id AND e.from_revision = OLD.revision AND e.kind = 'REQUEST_MORE'))
    )
)
BEGIN SELECT RAISE(ABORT, 'resolved legacy claim requires audited appeal or existing grant'); END;

CREATE TRIGGER legacy_claim_revision_guard BEFORE UPDATE ON legacy_claims
WHEN NEW.uid != OLD.uid OR NEW.firebase_project_id != OLD.firebase_project_id
  OR NEW.claim_id != OLD.claim_id OR NEW.policy_version != OLD.policy_version
  OR NEW.opened_at != OLD.opened_at OR NEW.evidence_type != OLD.evidence_type
  OR NEW.evidence_hash IS NOT OLD.evidence_hash OR NEW.revision != OLD.revision + 1
BEGIN SELECT RAISE(ABORT, 'legacy claim ownership or revision conflict'); END;

CREATE TRIGGER legacy_claim_granted_guard BEFORE UPDATE ON legacy_claims
WHEN NEW.state = 'GRANTED' AND NOT EXISTS(SELECT 1 FROM legacy_grants g
    WHERE g.firebase_project_id = NEW.firebase_project_id AND g.uid = NEW.uid)
BEGIN SELECT RAISE(ABORT, 'claim GRANTED requires an existing grant'); END;
CREATE TRIGGER legacy_claim_rejection_guard BEFORE UPDATE ON legacy_claims
WHEN NEW.state = 'REJECTED' AND EXISTS(SELECT 1 FROM legacy_grants g
    WHERE g.firebase_project_id = NEW.firebase_project_id AND g.uid = NEW.uid)
BEGIN SELECT RAISE(ABORT, 'review cannot reject an existing grant'); END;

CREATE TRIGGER legacy_review_evidence_no_update BEFORE UPDATE ON legacy_review_evidence
BEGIN SELECT RAISE(ABORT, 'review evidence is append only'); END;
CREATE TRIGGER legacy_review_evidence_no_delete BEFORE DELETE ON legacy_review_evidence
BEGIN SELECT RAISE(ABORT, 'review evidence is append only'); END;
CREATE TRIGGER legacy_review_events_no_update BEFORE UPDATE ON legacy_review_events
BEGIN SELECT RAISE(ABORT, 'review audit is append only'); END;
CREATE TRIGGER legacy_review_events_no_delete BEFORE DELETE ON legacy_review_events
BEGIN SELECT RAISE(ABORT, 'review audit is append only'); END;
CREATE TRIGGER legacy_claim_requests_no_update BEFORE UPDATE ON legacy_claim_requests
BEGIN SELECT RAISE(ABORT, 'claim request is immutable'); END;
CREATE TRIGGER legacy_claim_requests_no_delete BEFORE DELETE ON legacy_claim_requests
BEGIN SELECT RAISE(ABORT, 'claim request is immutable'); END;
