-- Read-only admin queue support. No policy, grant, operator or user data seeded.
CREATE INDEX legacy_admin_queue ON legacy_claims(firebase_project_id, opened_at DESC, claim_id DESC);
CREATE INDEX legacy_admin_state_queue ON legacy_claims(firebase_project_id, state, opened_at DESC, claim_id DESC);
CREATE INDEX legacy_admin_evidence ON legacy_review_evidence(claim_id, evidence_id);
CREATE INDEX legacy_admin_events ON legacy_review_events(claim_id, to_revision, event_id);
