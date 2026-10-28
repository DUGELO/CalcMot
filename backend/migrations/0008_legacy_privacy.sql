-- OQ-07 scoped privacy operations; no data seeded and no public writer.
-- One receipt INSERT executes atomically. No authorization window survives the statement.
CREATE TABLE legacy_privacy_closures (
 claim_id TEXT PRIMARY KEY REFERENCES legacy_claims(claim_id),
 claim_revision INTEGER NOT NULL,
 finally_closed_at TEXT NOT NULL CHECK(julianday(finally_closed_at) IS NOT NULL)
);
CREATE TRIGGER legacy_privacy_reopen AFTER UPDATE ON legacy_claims
WHEN NEW.state='UNDER_REVIEW' OR NEW.state != OLD.state
BEGIN DELETE FROM legacy_privacy_closures WHERE claim_id=NEW.claim_id; END;
CREATE TABLE legacy_privacy_operations (
 operation_id TEXT PRIMARY KEY CHECK(length(operation_id) BETWEEN 16 AND 128),
 firebase_project_id TEXT NOT NULL,
 target_uid TEXT,
 mode TEXT NOT NULL CHECK(mode IN ('EVIDENCE_RETENTION','AUDIT_RETENTION','ERASE_ACCOUNT')),
 executed_at TEXT NOT NULL CHECK(julianday(executed_at) IS NOT NULL),
 state TEXT NOT NULL CHECK(state IN ('APPLYING','COMPLETE')),
 result_json TEXT NOT NULL CHECK(json_valid(result_json)),
 CHECK((state='APPLYING' AND target_uid IS NOT NULL) OR (state='COMPLETE' AND target_uid IS NULL))
);
CREATE TRIGGER legacy_privacy_no_update BEFORE UPDATE ON legacy_privacy_operations
WHEN NOT (OLD.state = 'APPLYING' AND NEW.state = 'COMPLETE'
 AND NEW.target_uid IS NULL AND NEW.operation_id = OLD.operation_id
 AND NEW.firebase_project_id = OLD.firebase_project_id AND NEW.mode = OLD.mode
 AND NEW.executed_at = OLD.executed_at AND NEW.result_json = OLD.result_json)
BEGIN SELECT RAISE(ABORT, 'privacy receipt is immutable'); END;

-- Metadata-only privacy minimization does not reopen or change a final decision.
DROP TRIGGER legacy_claim_state_transition;
CREATE TRIGGER legacy_claim_state_transition BEFORE UPDATE ON legacy_claims
WHEN OLD.state != 'UNDER_REVIEW' AND NOT (
 (OLD.state='REJECTED' AND (
   (NEW.state='GRANTED' AND EXISTS(SELECT 1 FROM legacy_grants g WHERE g.firebase_project_id=OLD.firebase_project_id AND g.uid=OLD.uid))
   OR (NEW.state='UNDER_REVIEW' AND EXISTS(SELECT 1 FROM legacy_review_events e
      WHERE e.claim_id=OLD.claim_id AND e.from_revision=OLD.revision AND e.kind='REQUEST_MORE'))))
 OR (NEW.state=OLD.state AND NEW.reviewer IS NULL AND NEW.resolved_at IS OLD.resolved_at
   AND NEW.provisional_until IS OLD.provisional_until AND EXISTS(SELECT 1 FROM legacy_privacy_operations p
    WHERE p.state='APPLYING' AND p.mode='AUDIT_RETENTION' AND p.firebase_project_id=OLD.firebase_project_id AND p.target_uid=OLD.uid))
)
BEGIN SELECT RAISE(ABORT, 'resolved legacy claim requires audited appeal or scoped privacy'); END;

DROP TRIGGER legacy_grant_no_delete;
CREATE TRIGGER legacy_grant_no_delete BEFORE DELETE ON legacy_grants
WHEN NOT EXISTS(SELECT 1 FROM legacy_privacy_operations p WHERE p.state='APPLYING'
 AND p.firebase_project_id=OLD.firebase_project_id AND p.target_uid=OLD.uid
 AND p.mode IN ('ERASE_ACCOUNT'))
BEGIN SELECT RAISE(ABORT, 'scoped privacy operation required'); END;

DROP TRIGGER legacy_evidence_no_delete;
CREATE TRIGGER legacy_evidence_no_delete BEFORE DELETE ON legacy_evidence
WHEN NOT EXISTS(SELECT 1 FROM legacy_privacy_operations p WHERE p.state='APPLYING'
 AND p.firebase_project_id=OLD.firebase_project_id AND p.target_uid=OLD.uid
 AND p.mode IN ('ERASE_ACCOUNT','EVIDENCE_RETENTION'))
BEGIN SELECT RAISE(ABORT, 'scoped privacy operation required'); END;

DROP TRIGGER legacy_history_no_delete;
CREATE TRIGGER legacy_history_no_delete BEFORE DELETE ON legacy_historical_use
WHEN NOT EXISTS(SELECT 1 FROM legacy_privacy_operations p WHERE p.state='APPLYING'
 AND p.firebase_project_id=OLD.firebase_project_id AND p.target_uid=(SELECT uid FROM legacy_evidence WHERE firebase_project_id = OLD.firebase_project_id AND evidence_id = OLD.evidence_id)
 AND p.mode IN ('ERASE_ACCOUNT','EVIDENCE_RETENTION'))
BEGIN SELECT RAISE(ABORT, 'scoped privacy operation required'); END;

DROP TRIGGER legacy_review_evidence_no_delete;
CREATE TRIGGER legacy_review_evidence_no_delete BEFORE DELETE ON legacy_review_evidence
WHEN NOT EXISTS(SELECT 1 FROM legacy_privacy_operations p WHERE p.state='APPLYING'
 AND p.firebase_project_id=(SELECT firebase_project_id FROM legacy_claims WHERE claim_id=OLD.claim_id) AND p.target_uid=(SELECT uid FROM legacy_claims WHERE claim_id=OLD.claim_id)
 AND p.mode IN ('ERASE_ACCOUNT','EVIDENCE_RETENTION'))
BEGIN SELECT RAISE(ABORT, 'scoped privacy operation required'); END;

DROP TRIGGER legacy_review_events_no_delete;
CREATE TRIGGER legacy_review_events_no_delete BEFORE DELETE ON legacy_review_events
WHEN NOT EXISTS(SELECT 1 FROM legacy_privacy_operations p WHERE p.state='APPLYING'
 AND p.firebase_project_id=(SELECT firebase_project_id FROM legacy_claims WHERE claim_id=OLD.claim_id) AND p.target_uid=(SELECT uid FROM legacy_claims WHERE claim_id=OLD.claim_id)
 AND p.mode IN ('ERASE_ACCOUNT','AUDIT_RETENTION'))
BEGIN SELECT RAISE(ABORT, 'scoped privacy operation required'); END;

DROP TRIGGER legacy_claim_requests_no_delete;
CREATE TRIGGER legacy_claim_requests_no_delete BEFORE DELETE ON legacy_claim_requests
WHEN NOT EXISTS(SELECT 1 FROM legacy_privacy_operations p WHERE p.state='APPLYING'
 AND p.firebase_project_id=OLD.firebase_project_id AND p.target_uid=OLD.uid
 AND p.mode IN ('ERASE_ACCOUNT','AUDIT_RETENTION'))
BEGIN SELECT RAISE(ABORT, 'scoped privacy operation required'); END;

DROP TRIGGER legacy_review_issuance_no_delete;
CREATE TRIGGER legacy_review_issuance_no_delete BEFORE DELETE ON legacy_review_issuances
WHEN NOT EXISTS(SELECT 1 FROM legacy_privacy_operations p WHERE p.state='APPLYING'
 AND p.firebase_project_id=OLD.firebase_project_id AND p.target_uid=OLD.uid
 AND p.mode IN ('ERASE_ACCOUNT','AUDIT_RETENTION'))
BEGIN SELECT RAISE(ABORT, 'scoped privacy operation required'); END;

CREATE TRIGGER legacy_privacy_apply AFTER INSERT ON legacy_privacy_operations
WHEN NEW.state='APPLYING' AND NEW.target_uid IS NOT NULL
BEGIN
 SELECT RAISE(ABORT,'privacy retention not due') WHERE NEW.mode != 'ERASE_ACCOUNT' AND NOT EXISTS(
   SELECT 1 FROM legacy_claims c JOIN legacy_privacy_closures f ON f.claim_id=c.claim_id AND f.claim_revision=c.revision
   WHERE c.firebase_project_id=NEW.firebase_project_id AND c.uid=NEW.target_uid
   AND c.state IN ('GRANTED','REJECTED') AND c.resolved_at IS NOT NULL
   AND julianday(NEW.executed_at) >= CASE WHEN NEW.mode='EVIDENCE_RETENTION'
     THEN julianday(f.finally_closed_at,'+30 days') ELSE julianday(f.finally_closed_at,'+12 months') END
 );
 DELETE FROM legacy_review_evidence WHERE claim_id IN (
   SELECT claim_id FROM legacy_claims WHERE firebase_project_id=NEW.firebase_project_id AND uid=NEW.target_uid)
   AND NEW.mode IN ('EVIDENCE_RETENTION','ERASE_ACCOUNT');
 DELETE FROM legacy_historical_use WHERE firebase_project_id=NEW.firebase_project_id
   AND evidence_id IN (SELECT evidence_id FROM legacy_evidence WHERE firebase_project_id=NEW.firebase_project_id AND uid=NEW.target_uid)
   AND NEW.mode IN ('EVIDENCE_RETENTION','ERASE_ACCOUNT');
 DELETE FROM legacy_evidence WHERE firebase_project_id=NEW.firebase_project_id AND uid=NEW.target_uid
   AND NEW.mode IN ('EVIDENCE_RETENTION','ERASE_ACCOUNT');
 DELETE FROM legacy_recovery_requests WHERE firebase_project_id=NEW.firebase_project_id AND uid=NEW.target_uid
   AND NEW.mode IN ('EVIDENCE_RETENTION','ERASE_ACCOUNT');
 DELETE FROM legacy_review_issuances WHERE firebase_project_id=NEW.firebase_project_id AND uid=NEW.target_uid
   AND NEW.mode IN ('AUDIT_RETENTION','ERASE_ACCOUNT');
 DELETE FROM legacy_review_events WHERE claim_id IN (
   SELECT claim_id FROM legacy_claims WHERE firebase_project_id=NEW.firebase_project_id AND uid=NEW.target_uid)
   AND NEW.mode IN ('AUDIT_RETENTION','ERASE_ACCOUNT');
 DELETE FROM legacy_claim_requests WHERE firebase_project_id=NEW.firebase_project_id AND uid=NEW.target_uid
   AND NEW.mode IN ('AUDIT_RETENTION','ERASE_ACCOUNT');
 UPDATE legacy_claims SET reviewer=NULL,claim_json=json_set(claim_json,'$.reviewer',NULL),revision=revision+1
   WHERE firebase_project_id=NEW.firebase_project_id AND uid=NEW.target_uid AND NEW.mode='AUDIT_RETENTION';
 UPDATE legacy_privacy_closures SET claim_revision=claim_revision+1 WHERE claim_id IN (
   SELECT claim_id FROM legacy_claims WHERE firebase_project_id=NEW.firebase_project_id AND uid=NEW.target_uid)
   AND NEW.mode='AUDIT_RETENTION';
 DELETE FROM legacy_claim_identities WHERE claim_id IN (
   SELECT claim_id FROM legacy_claims WHERE firebase_project_id=NEW.firebase_project_id AND uid=NEW.target_uid)
   AND NEW.mode='ERASE_ACCOUNT';
 DELETE FROM legacy_privacy_closures WHERE claim_id IN (
   SELECT claim_id FROM legacy_claims WHERE firebase_project_id=NEW.firebase_project_id AND uid=NEW.target_uid)
   AND NEW.mode='ERASE_ACCOUNT';
 DELETE FROM legacy_claims WHERE firebase_project_id=NEW.firebase_project_id AND uid=NEW.target_uid AND NEW.mode='ERASE_ACCOUNT';
 DELETE FROM legacy_grants WHERE firebase_project_id=NEW.firebase_project_id AND uid=NEW.target_uid AND NEW.mode='ERASE_ACCOUNT';
 DELETE FROM entitlement_snapshots WHERE firebase_project_id=NEW.firebase_project_id AND uid=NEW.target_uid AND NEW.mode='ERASE_ACCOUNT';
 DELETE FROM entitlement_source_versions WHERE firebase_project_id=NEW.firebase_project_id AND uid=NEW.target_uid AND NEW.mode='ERASE_ACCOUNT';
 UPDATE legacy_privacy_operations SET state='COMPLETE',target_uid=NULL WHERE operation_id=NEW.operation_id;
END;

