-- Private binding only. No public SQL endpoint, SDK access, or grant writer.
-- Future authoritative projectors own writes; S02 only reads this materialized view.
CREATE TABLE entitlement_snapshots (
    firebase_project_id TEXT NOT NULL CHECK(length(firebase_project_id) > 0),
    uid TEXT NOT NULL CHECK(length(uid) BETWEEN 1 AND 128),
    revision INTEGER NOT NULL CHECK(revision >= 0),
    snapshot_json TEXT NOT NULL CHECK(json_valid(snapshot_json))
        CHECK(COALESCE(json_extract(snapshot_json, '$.contractVersion') = '1.0.0', 0))
        CHECK(COALESCE(json_type(snapshot_json, '$.revision') = 'integer', 0))
        CHECK(COALESCE(json_extract(snapshot_json, '$.revision') = revision, 0)),
    PRIMARY KEY(firebase_project_id, uid)
) WITHOUT ROWID;

CREATE TRIGGER entitlement_revision_monotonic
BEFORE UPDATE ON entitlement_snapshots
WHEN NEW.revision <= OLD.revision OR NEW.uid != OLD.uid
    OR NEW.firebase_project_id != OLD.firebase_project_id
BEGIN
    SELECT RAISE(ABORT, 'entitlement revision or ownership conflict');
END;
