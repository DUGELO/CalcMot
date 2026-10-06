package br.com.calcmot.securityrecording.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object RecordingMigrations {
    /** No media deletion, fabricated configuration, duration or retroactive expiry. */
    val FROM_1_TO_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            listOf(
                "processEpoch TEXT", "ownerId TEXT", "lens TEXT", "orientation INTEGER",
                "quality TEXT", "segmentMinutes INTEGER", "capturedDurationMs INTEGER",
                "completionReason TEXT", "retentionDays INTEGER", "expiresAtUtcMs INTEGER",
                "temporaryAvailability TEXT NOT NULL DEFAULT 'AVAILABLE'",
                "galleryState TEXT NOT NULL DEFAULT 'NONE'", "exportState TEXT NOT NULL DEFAULT 'IDLE'",
                "galleryUri TEXT", "exportPath TEXT", "exportSha256 TEXT", "gallerySha256 TEXT", "lastShareLaunchedAtUtcMs INTEGER",
                "retentionOrigin TEXT NOT NULL DEFAULT 'LEGACY_UNBOUNDED'"
            ).forEach { db.execSQL("ALTER TABLE recording_sessions ADD COLUMN $it") }
            db.execSQL("""CREATE TABLE IF NOT EXISTS recording_operation_claims (
                sessionId TEXT NOT NULL PRIMARY KEY, operationId TEXT NOT NULL,
                kind TEXT NOT NULL, baseRevision INTEGER NOT NULL, processEpoch TEXT NOT NULL,
                status TEXT NOT NULL, FOREIGN KEY(sessionId) REFERENCES recording_sessions(id)
                ON UPDATE NO ACTION ON DELETE CASCADE)""")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_recording_operation_claims_operationId ON recording_operation_claims(operationId)")
        }
    }
}
