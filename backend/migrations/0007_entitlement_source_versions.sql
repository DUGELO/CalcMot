-- S07 source revision only. GET remains read-only; no grant or policy seeded.
-- Flags record formerly present sources: missing data is unavailable, not revoked.
CREATE TABLE entitlement_source_versions (
    firebase_project_id TEXT NOT NULL,
    uid TEXT NOT NULL,
    revision INTEGER NOT NULL CHECK(revision BETWEEN 0 AND 9007199254740991),
    seen_snapshot INTEGER NOT NULL CHECK(seen_snapshot IN (0,1)),
    seen_grant INTEGER NOT NULL CHECK(seen_grant IN (0,1)),
    seen_claim INTEGER NOT NULL CHECK(seen_claim IN (0,1)),
    changed_at TEXT NOT NULL,
    PRIMARY KEY(firebase_project_id, uid)
) WITHOUT ROWID;

CREATE TRIGGER s07_revision_guard BEFORE UPDATE ON entitlement_source_versions
WHEN NEW.firebase_project_id != OLD.firebase_project_id OR NEW.uid != OLD.uid
  OR NEW.revision <= OLD.revision OR NEW.seen_snapshot < OLD.seen_snapshot
  OR NEW.seen_grant < OLD.seen_grant OR NEW.seen_claim < OLD.seen_claim
BEGIN SELECT RAISE(ABORT, 'entitlement source revision conflict'); END;

-- Backfill revisions, not rights. Existing sources/JSON remain untouched.
INSERT INTO entitlement_source_versions
SELECT firebase_project_id, uid, MAX(source_revision) + 1,
       MAX(seen_snapshot), MAX(seen_grant), MAX(seen_claim), strftime('%Y-%m-%dT%H:%M:%fZ','now')
FROM (
    SELECT firebase_project_id, uid, revision AS source_revision, 1 AS seen_snapshot, 0 AS seen_grant, 0 AS seen_claim FROM entitlement_snapshots
    UNION ALL SELECT firebase_project_id, uid, grant_revision, 0, 1, 0 FROM legacy_grants
    UNION ALL SELECT firebase_project_id, uid, revision, 0, 0, 1 FROM legacy_claims
) GROUP BY firebase_project_id, uid;

CREATE TRIGGER s07_snapshot_insert AFTER INSERT ON entitlement_snapshots
BEGIN
    INSERT INTO entitlement_source_versions VALUES (NEW.firebase_project_id, NEW.uid, NEW.revision,
      1, 0, 0, strftime('%Y-%m-%dT%H:%M:%fZ','now'))
    ON CONFLICT(firebase_project_id,uid) DO UPDATE SET
      revision = MAX(entitlement_source_versions.revision + 1, excluded.revision),
      seen_snapshot = MAX(entitlement_source_versions.seen_snapshot, excluded.seen_snapshot),
      seen_grant = MAX(entitlement_source_versions.seen_grant, excluded.seen_grant),
      seen_claim = MAX(entitlement_source_versions.seen_claim, excluded.seen_claim),
      changed_at = excluded.changed_at;
END;

CREATE TRIGGER s07_snapshot_update AFTER UPDATE ON entitlement_snapshots
BEGIN
    INSERT INTO entitlement_source_versions VALUES (NEW.firebase_project_id, NEW.uid, NEW.revision,
      1, 0, 0, strftime('%Y-%m-%dT%H:%M:%fZ','now'))
    ON CONFLICT(firebase_project_id,uid) DO UPDATE SET
      revision = MAX(entitlement_source_versions.revision + 1, excluded.revision),
      seen_snapshot = MAX(entitlement_source_versions.seen_snapshot, excluded.seen_snapshot),
      seen_grant = MAX(entitlement_source_versions.seen_grant, excluded.seen_grant),
      seen_claim = MAX(entitlement_source_versions.seen_claim, excluded.seen_claim),
      changed_at = excluded.changed_at;
END;

CREATE TRIGGER s07_snapshot_delete AFTER DELETE ON entitlement_snapshots
BEGIN
    INSERT INTO entitlement_source_versions VALUES (OLD.firebase_project_id, OLD.uid, OLD.revision + 1,
      1, 0, 0, strftime('%Y-%m-%dT%H:%M:%fZ','now'))
    ON CONFLICT(firebase_project_id,uid) DO UPDATE SET
      revision = MAX(entitlement_source_versions.revision + 1, excluded.revision),
      seen_snapshot = MAX(entitlement_source_versions.seen_snapshot, excluded.seen_snapshot),
      seen_grant = MAX(entitlement_source_versions.seen_grant, excluded.seen_grant),
      seen_claim = MAX(entitlement_source_versions.seen_claim, excluded.seen_claim),
      changed_at = excluded.changed_at;
END;

CREATE TRIGGER s07_grant_insert AFTER INSERT ON legacy_grants
BEGIN
    INSERT INTO entitlement_source_versions VALUES (NEW.firebase_project_id, NEW.uid, NEW.grant_revision,
      0, 1, 0, strftime('%Y-%m-%dT%H:%M:%fZ','now'))
    ON CONFLICT(firebase_project_id,uid) DO UPDATE SET
      revision = MAX(entitlement_source_versions.revision + 1, excluded.revision),
      seen_snapshot = MAX(entitlement_source_versions.seen_snapshot, excluded.seen_snapshot),
      seen_grant = MAX(entitlement_source_versions.seen_grant, excluded.seen_grant),
      seen_claim = MAX(entitlement_source_versions.seen_claim, excluded.seen_claim),
      changed_at = excluded.changed_at;
END;

CREATE TRIGGER s07_grant_delete AFTER DELETE ON legacy_grants
BEGIN
    INSERT INTO entitlement_source_versions VALUES (OLD.firebase_project_id, OLD.uid, OLD.grant_revision + 1,
      0, 1, 0, strftime('%Y-%m-%dT%H:%M:%fZ','now'))
    ON CONFLICT(firebase_project_id,uid) DO UPDATE SET
      revision = MAX(entitlement_source_versions.revision + 1, excluded.revision),
      seen_snapshot = MAX(entitlement_source_versions.seen_snapshot, excluded.seen_snapshot),
      seen_grant = MAX(entitlement_source_versions.seen_grant, excluded.seen_grant),
      seen_claim = MAX(entitlement_source_versions.seen_claim, excluded.seen_claim),
      changed_at = excluded.changed_at;
END;

CREATE TRIGGER s07_claim_insert AFTER INSERT ON legacy_claims
BEGIN
    INSERT INTO entitlement_source_versions VALUES (NEW.firebase_project_id, NEW.uid, NEW.revision,
      0, 0, 1, strftime('%Y-%m-%dT%H:%M:%fZ','now'))
    ON CONFLICT(firebase_project_id,uid) DO UPDATE SET
      revision = MAX(entitlement_source_versions.revision + 1, excluded.revision),
      seen_snapshot = MAX(entitlement_source_versions.seen_snapshot, excluded.seen_snapshot),
      seen_grant = MAX(entitlement_source_versions.seen_grant, excluded.seen_grant),
      seen_claim = MAX(entitlement_source_versions.seen_claim, excluded.seen_claim),
      changed_at = excluded.changed_at;
END;

CREATE TRIGGER s07_claim_update AFTER UPDATE ON legacy_claims
BEGIN
    INSERT INTO entitlement_source_versions VALUES (NEW.firebase_project_id, NEW.uid, NEW.revision,
      0, 0, 1, strftime('%Y-%m-%dT%H:%M:%fZ','now'))
    ON CONFLICT(firebase_project_id,uid) DO UPDATE SET
      revision = MAX(entitlement_source_versions.revision + 1, excluded.revision),
      seen_snapshot = MAX(entitlement_source_versions.seen_snapshot, excluded.seen_snapshot),
      seen_grant = MAX(entitlement_source_versions.seen_grant, excluded.seen_grant),
      seen_claim = MAX(entitlement_source_versions.seen_claim, excluded.seen_claim),
      changed_at = excluded.changed_at;
END;

CREATE TRIGGER s07_claim_delete AFTER DELETE ON legacy_claims
BEGIN
    INSERT INTO entitlement_source_versions VALUES (OLD.firebase_project_id, OLD.uid, OLD.revision + 1,
      0, 0, 1, strftime('%Y-%m-%dT%H:%M:%fZ','now'))
    ON CONFLICT(firebase_project_id,uid) DO UPDATE SET
      revision = MAX(entitlement_source_versions.revision + 1, excluded.revision),
      seen_snapshot = MAX(entitlement_source_versions.seen_snapshot, excluded.seen_snapshot),
      seen_grant = MAX(entitlement_source_versions.seen_grant, excluded.seen_grant),
      seen_claim = MAX(entitlement_source_versions.seen_claim, excluded.seen_claim),
      changed_at = excluded.changed_at;
END;
