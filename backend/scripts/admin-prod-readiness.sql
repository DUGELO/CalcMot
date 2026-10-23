-- Read-only aggregate inventory. No UID, token, email, evidence or personal narrative.
PRAGMA quick_check;
PRAGMA foreign_key_check;
SELECT name FROM d1_migrations ORDER BY id;
SELECT name FROM sqlite_master WHERE type='index' AND name LIKE 'legacy_admin_%' ORDER BY name;
SELECT 'claims' AS category, count(*) AS total FROM legacy_claims
UNION ALL SELECT 'grants', count(*) FROM legacy_grants
UNION ALL SELECT 'policies', count(*) FROM legacy_policies
UNION ALL SELECT 'events', count(*) FROM legacy_review_events;
SELECT 'issuances' AS category, count(*) AS total FROM legacy_review_issuances
UNION ALL SELECT 'evidence', count(*) FROM legacy_review_evidence
UNION ALL SELECT 'historical_use', count(*) FROM legacy_historical_use
UNION ALL SELECT 'snapshots', count(*) FROM entitlement_snapshots;
