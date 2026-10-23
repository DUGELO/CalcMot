-- Private S06 authority, distinct from verified historical use. No data seeded.
-- A receipt INSERT and its triggers form ONE SQLite transaction: no pending
-- authorization can survive a failed grant/claim write. D1 batch is atomic too.
CREATE TABLE legacy_review_issuances (
    issuance_id TEXT PRIMARY KEY,
    firebase_project_id TEXT NOT NULL,
    uid TEXT NOT NULL,
    claim_id TEXT NOT NULL UNIQUE REFERENCES legacy_claims(claim_id),
    approval_event_id TEXT NOT NULL UNIQUE REFERENCES legacy_review_events(event_id),
    approved_revision INTEGER NOT NULL CHECK(approved_revision >= 1),
    completed_revision INTEGER NOT NULL CHECK(completed_revision = approved_revision + 1),
    basis TEXT NOT NULL CHECK(basis IN ('ASSISTED_ASSESSMENT', 'VERIFIABLE_HISTORY')),
    actor_uid TEXT NOT NULL,
    issued_at TEXT NOT NULL CHECK(issued_at LIKE '%Z' AND julianday(issued_at) IS NOT NULL),
    operation_key_hash TEXT NOT NULL CHECK(length(operation_key_hash) = 64),
    request_hash TEXT NOT NULL CHECK(length(request_hash) = 64),
    evidence_hash TEXT NOT NULL CHECK(length(evidence_hash) = 64 AND evidence_hash NOT GLOB '*[^0-9a-f]*'),
    policy_version TEXT NOT NULL,
    grant_json TEXT NOT NULL CHECK(json_valid(grant_json)),
    UNIQUE(firebase_project_id, uid),
    UNIQUE(claim_id, operation_key_hash)
);

CREATE TRIGGER legacy_review_issuance_authority BEFORE INSERT ON legacy_review_issuances
WHEN NOT EXISTS (
    SELECT 1 FROM legacy_claims c JOIN legacy_review_events e ON e.claim_id = c.claim_id
    JOIN legacy_claim_identities i ON i.claim_id = c.claim_id
    JOIN legacy_policies p ON p.firebase_project_id = c.firebase_project_id AND p.policy_version = c.policy_version
    WHERE c.claim_id = NEW.claim_id AND c.firebase_project_id = NEW.firebase_project_id AND c.uid = NEW.uid
      AND c.state = 'UNDER_REVIEW' AND c.revision = NEW.approved_revision
      AND e.event_id = NEW.approval_event_id AND e.to_revision = c.revision
      AND e.kind = 'APPROVE' AND e.basis = NEW.basis AND e.actor_uid = NEW.actor_uid
      AND i.linked_google = 1 AND p.approved_by = NEW.actor_uid
      AND p.maximum_historical_version_code = 12 AND p.cutoff_utc IS NULL
      AND p.core_baseline = 'calcmot-core-3.5-vc12' AND p.policy_version = NEW.policy_version
      AND julianday(p.approved_at) <= julianday(e.occurred_at)
      AND julianday(e.occurred_at) <= julianday(NEW.issued_at)
      AND EXISTS(SELECT 1 FROM json_each(p.accepted_evidence_kinds_json) k WHERE k.value = NEW.basis)
      AND json_type(e.details_json, '$.ownershipConfirmed') = 'true'
      AND json_type(e.details_json, '$.contradictionsCleared') = 'true'
      AND json_type(e.details_json, '$.legacyUseConcluded') = 'true'
      AND length(trim(json_extract(e.details_json, '$.reason'))) > 0
      AND json_type(e.details_json, '$.evidenceIds') = 'array'
      AND NOT EXISTS(SELECT 1 FROM json_each(e.details_json, '$.evidenceIds') x
        WHERE NOT EXISTS(SELECT 1 FROM legacy_review_evidence r WHERE r.claim_id = c.claim_id AND r.evidence_id = x.value))
      AND NOT EXISTS(SELECT 1 FROM legacy_grants g WHERE g.firebase_project_id = c.firebase_project_id AND g.uid = c.uid)
)
BEGIN SELECT RAISE(ABORT, 'issuance requires current authorized owner approval'); END;

-- Separate small guards respect the D1 SQLite expression-depth limit.
CREATE TRIGGER legacy_review_issuance_evidence BEFORE INSERT ON legacy_review_issuances
WHEN NOT EXISTS (SELECT 1 FROM legacy_claims c JOIN legacy_review_events e ON e.claim_id = c.claim_id
    WHERE c.claim_id = NEW.claim_id AND e.event_id = NEW.approval_event_id AND (
        (NEW.basis = 'ASSISTED_ASSESSMENT' AND EXISTS(
          SELECT 1 FROM legacy_review_evidence a JOIN legacy_review_evidence b ON b.claim_id = a.claim_id
          WHERE a.claim_id = c.claim_id AND a.kind = 'AUXILIARY' AND b.kind = 'AUXILIARY'
            AND a.origin_key != b.origin_key AND a.category != b.category
            AND a.category IN ('SUPPORT_RECORD', 'ACCOUNT_CONTINUITY')
            AND b.category IN ('INSTALL_CONTEXT', 'USAGE_CONTEXT', 'ACCOUNT_CONTINUITY')
            AND a.evidence_id IN (SELECT value FROM json_each(e.details_json, '$.evidenceIds'))
            AND b.evidence_id IN (SELECT value FROM json_each(e.details_json, '$.evidenceIds'))
        )) OR (NEW.basis = 'VERIFIABLE_HISTORY' AND EXISTS(
          SELECT 1 FROM legacy_review_evidence r JOIN legacy_evidence v
            ON v.firebase_project_id = c.firebase_project_id AND v.evidence_id = r.authority_evidence_id
          JOIN legacy_historical_use h ON h.firebase_project_id = v.firebase_project_id AND h.evidence_id = v.evidence_id
          WHERE r.claim_id = c.claim_id AND r.kind = 'HISTORY' AND v.uid = c.uid AND v.verification = 'VERIFIED'
            AND r.evidence_id IN (SELECT value FROM json_each(e.details_json, '$.evidenceIds'))
            AND h.used_version_code BETWEEN 1 AND 12 AND h.verified_by = v.verified_by
            AND julianday(h.verified_at) >= julianday(v.qualifying_at)
            AND julianday(h.verified_at) <= julianday(e.occurred_at)
            AND julianday(v.observed_at) <= julianday(e.occurred_at)
        ))
      ))
BEGIN SELECT RAISE(ABORT, 'issuance requires contextual evidence'); END;

CREATE TRIGGER legacy_review_issuance_payload BEFORE INSERT ON legacy_review_issuances
WHEN NOT EXISTS (SELECT 1 FROM legacy_policies p
    WHERE p.firebase_project_id = NEW.firebase_project_id AND p.policy_version = NEW.policy_version
      AND json_extract(NEW.grant_json, '$.uid') = NEW.uid
      AND json_extract(NEW.grant_json, '$.status') = 'GRANTED'
      AND json_type(NEW.grant_json, '$.policyCutoffUtc') = 'null'
      AND json_extract(NEW.grant_json, '$.policyVersion') = p.policy_version
      AND json_extract(NEW.grant_json, '$.legacyGrantedAt') = NEW.issued_at
      AND json_extract(NEW.grant_json, '$.legacyEvidenceType') = NEW.basis
      AND json_extract(NEW.grant_json, '$.evidenceHash') = NEW.evidence_hash
      AND json_extract(NEW.grant_json, '$.legacyCoreVersionBaseline') = p.core_baseline
      AND json_extract(NEW.grant_json, '$.migrationVersion') = p.migration_version
      AND json_type(NEW.grant_json, '$.grantRevision') = 'integer'
      AND json_extract(NEW.grant_json, '$.grantRevision') = 1
      AND json_extract(NEW.grant_json, '$.decisionActor') = NEW.actor_uid
      AND (SELECT COUNT(*) FROM json_each(NEW.grant_json)) = 11
)
BEGIN SELECT RAISE(ABORT, 'issuance payload does not match authority'); END;

-- Preserve the complete verified S05 path. A second, explicitly bounded path
-- permits only an atomic review receipt; human judgment never enters history.
DROP TRIGGER legacy_grant_authority;
DROP TRIGGER legacy_grant_version_authority;
CREATE TRIGGER legacy_grant_json_authority BEFORE INSERT ON legacy_grants
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
    AND json_extract(NEW.grant_json, '$.decisionActor') = NEW.decision_actor, 0) = 0
BEGIN SELECT RAISE(ABORT, 'legacy grant payload mismatch'); END;

CREATE TRIGGER legacy_grant_authority BEFORE INSERT ON legacy_grants
WHEN NOT EXISTS(SELECT 1 FROM legacy_policies p
      WHERE p.firebase_project_id = NEW.firebase_project_id AND p.policy_version = NEW.policy_version
        AND p.maximum_historical_version_code = 12 AND p.cutoff_utc IS NULL
        AND p.core_baseline = 'calcmot-core-3.5-vc12' AND p.cutoff_utc IS NEW.policy_cutoff_utc
        AND p.core_baseline = NEW.legacy_core_version_baseline AND p.migration_version = NEW.migration_version
        AND julianday(p.approved_at) <= julianday(NEW.legacy_granted_at)
        AND (
          EXISTS(SELECT 1 FROM legacy_evidence e JOIN legacy_historical_use h
            ON h.firebase_project_id = e.firebase_project_id AND h.evidence_id = e.evidence_id
            WHERE e.firebase_project_id = p.firebase_project_id AND e.uid = NEW.uid AND e.verification = 'VERIFIED'
              AND e.evidence_hash = NEW.evidence_hash AND e.evidence_kind = NEW.legacy_evidence_type
              AND e.verified_by = NEW.decision_actor AND h.verified_by = NEW.decision_actor
              AND h.used_version_code BETWEEN 1 AND 12
              AND julianday(e.observed_at) <= julianday(NEW.legacy_granted_at)
              AND julianday(h.verified_at) <= julianday(NEW.legacy_granted_at)
              AND julianday(h.verified_at) >= julianday(e.qualifying_at)
              AND EXISTS(SELECT 1 FROM json_each(p.accepted_evidence_kinds_json) k WHERE k.value = e.evidence_kind)
          ) OR EXISTS(SELECT 1 FROM legacy_review_issuances s JOIN legacy_claims c ON c.claim_id = s.claim_id
            WHERE s.firebase_project_id = p.firebase_project_id AND s.uid = NEW.uid AND s.policy_version = p.policy_version
              AND s.grant_json = NEW.grant_json AND s.evidence_hash = NEW.evidence_hash
              AND s.basis = NEW.legacy_evidence_type AND s.actor_uid = NEW.decision_actor
              AND s.issued_at = NEW.legacy_granted_at AND p.approved_by = s.actor_uid
              AND c.revision = s.approved_revision AND c.state = 'UNDER_REVIEW'
          )
        )
    )
BEGIN SELECT RAISE(ABORT, 'legacy grant requires verified history or authorized review'); END;

CREATE TRIGGER legacy_review_issue_grant AFTER INSERT ON legacy_review_issuances
BEGIN
    INSERT INTO legacy_grants
      (firebase_project_id, uid, status, policy_cutoff_utc, policy_version, legacy_granted_at,
       legacy_evidence_type, evidence_hash, legacy_core_version_baseline, migration_version,
       grant_revision, decision_actor, grant_json)
    VALUES (NEW.firebase_project_id, NEW.uid, 'GRANTED', NULL, NEW.policy_version, NEW.issued_at,
      NEW.basis, NEW.evidence_hash, json_extract(NEW.grant_json, '$.legacyCoreVersionBaseline'),
      json_extract(NEW.grant_json, '$.migrationVersion'), 1, NEW.actor_uid, NEW.grant_json);
    UPDATE legacy_claims SET state = 'GRANTED', resolved_at = NEW.issued_at, reviewer = NEW.actor_uid,
      provisional_until = NULL, revision = NEW.completed_revision,
      claim_json = json_set(claim_json, '$.state', 'GRANTED', '$.resolvedAt', NEW.issued_at,
        '$.reviewer', NEW.actor_uid, '$.provisionalUntil', NULL)
    WHERE claim_id = NEW.claim_id AND firebase_project_id = NEW.firebase_project_id AND uid = NEW.uid
      AND state = 'UNDER_REVIEW' AND revision = NEW.approved_revision;
    SELECT RAISE(ABORT, 'issuance completion failed') WHERE NOT EXISTS(SELECT 1 FROM legacy_claims c JOIN legacy_grants g
      ON g.firebase_project_id = c.firebase_project_id AND g.uid = c.uid
      WHERE c.claim_id = NEW.claim_id AND c.state = 'GRANTED' AND c.revision = NEW.completed_revision
        AND g.grant_json = NEW.grant_json);
END;
CREATE TRIGGER legacy_review_issuance_no_update BEFORE UPDATE ON legacy_review_issuances
BEGIN SELECT RAISE(ABORT, 'issuance audit is append only'); END;
CREATE TRIGGER legacy_review_issuance_no_delete BEFORE DELETE ON legacy_review_issuances
BEGIN SELECT RAISE(ABORT, 'issuance audit is append only'); END;
