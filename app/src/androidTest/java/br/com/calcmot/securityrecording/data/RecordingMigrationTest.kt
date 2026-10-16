package br.com.calcmot.securityrecording.data

import androidx.room.Room
import androidx.room.withTransaction
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.json.JSONObject
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RecordingMigrationTest {
    /**
     * Create a real v1 SQLite file from Room's checked-in schema (not a hand-written
     * approximation), then let the production Room open helper execute and validate
     * FROM_1_TO_2. This avoids MigrationTestHelper's incompatible test-only JSON
     * serializer while preserving both schema validation and actual data checks.
     */
    @Test fun preservesLegacyFactsAndDoesNotInventExpiry() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "recording-migration-synthetic"
        val schemaPath = "br.com.calcmot.securityrecording.data.RecordingDatabase/1.json"
        context.deleteDatabase(name)
        try {
            val schema = context.assets.open(schemaPath).bufferedReader().use { reader ->
                JSONObject(reader.readText()).getJSONObject("database")
            }
            context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { db ->
                val entities = schema.getJSONArray("entities")
                for (i in 0 until entities.length()) {
                    val entity = entities.getJSONObject(i)
                    val table = entity.getString("tableName")
                    db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                }
                for (i in 0 until entities.length()) {
                    val entity = entities.getJSONObject(i)
                    val indices = entity.optJSONArray("indices") ?: continue
                    val table = entity.getString("tableName")
                    for (j in 0 until indices.length()) {
                        db.execSQL(
                            indices.getJSONObject(j).getString("createSql")
                                .replace("\${TABLE_NAME}", table)
                        )
                    }
                }
                val setup = schema.getJSONArray("setupQueries")
                for (i in 0 until setup.length()) db.execSQL(setup.getString(i))

                db.execSQL("INSERT INTO recording_sessions(id,revision,phase,createdAtUtcMs) VALUES('valid',4,'VERIFIED',100),('active',1,'RECORDING',200),('failed',2,'FAILED',300)")
                db.execSQL("INSERT INTO recording_segments(id,sessionId,ordinal,path,durationMs,bytes,verified,sha256) VALUES('segment','valid',0,'synthetic.mp4',500,1000,1,'synthetic-digest')")
                db.execSQL("INSERT INTO recording_claims(id,sessionId,name,value) VALUES('fact','valid','has_audio','true')")
                db.version = 1
            }

            val reopened = Room.databaseBuilder(context, RecordingDatabase::class.java, name)
                .addMigrations(RecordingMigrations.FROM_1_TO_2)
                .build()
            try {
                // Opening the real DAO triggers Room's own migration and full schema
                // verification. Never use fallbackToDestructiveMigration here.
                runBlocking {
                    val sessions = reopened.recordingDao().sessions()
                    assertEquals(3, sessions.size)
                    val valid = sessions.single { it.id == "valid" }
                    assertEquals("VERIFIED", valid.phase)
                    assertNull(valid.capturedDurationMs)
                    assertNull(valid.expiresAtUtcMs)
                    assertEquals("LEGACY_UNBOUNDED", valid.retentionOrigin)
                    val active = sessions.single { it.id == "active" }
                    assertEquals("RECORDING", active.phase)
                    assertEquals(1, reopened.recordingDao().segmentsForSession("valid").size)
                }
                reopened.openHelper.readableDatabase.query(
                    "SELECT value FROM recording_claims WHERE id='fact'"
                ).use {
                    assertTrue(it.moveToFirst())
                    assertEquals("true", it.getString(0))
                }
                // A schema mismatch here would already have thrown during Room open.
                assertEquals(2, reopened.openHelper.readableDatabase.version)
            } finally {
                reopened.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    @Test fun operationOwnerAndRevisionProtectCommit() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, RecordingDatabase::class.java).build()
        try {
            val dao=db.recordingDao()
            dao.insertSession(RecordingSessionEntity("session",0,"VERIFIED",createdAtUtcMs=100))
            val claim=RecordingOperationClaimEntity("session","operation","EXPORT",0,"epoch","CLAIMED")
            assertTrue(dao.reserveOperation(claim))
            assertFalse(dao.reserveOperation(claim.copy(operationId="second",baseRevision=1)))
            assertFalse(dao.completeOperation("session","operation","other-process"))
            assertEquals(1,dao.changeOperationStatus("session","operation","epoch","CLAIMED","COMMITTING"))
            assertTrue(dao.completeOperation("session","operation","epoch"))
            assertEquals(2L,dao.session("session")!!.revision)
            assertFalse(dao.completeOperation("session","operation","epoch"))
        } finally { db.close() }
    }
    @Test fun failedCommitPreservesRecoverableClaimAndRevision() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, RecordingDatabase::class.java).build()
        try {
            val dao = db.recordingDao()
            dao.insertSession(RecordingSessionEntity("session",0,"FINALIZING",createdAtUtcMs=100,ownerId="owner",processEpoch="old"))
            val claim = RecordingOperationClaimEntity("session","recovery","RECOVERY",0,"new","CLAIMED")
            assertTrue(dao.reserveOperation(claim))
            assertFalse(dao.writeOwned(1,dao.session("session")!!.copy(revision=2)))
            val failed = runCatching { db.withTransaction {
                assertEquals(1,dao.changeOperationStatus("session","recovery","new","CLAIMED","COMMITTING"))
                dao.updateSessionEntity(dao.session("session")!!.copy(revision=2,phase="VERIFIED"))
                error("synthetic_commit_fault")
            } }
            assertTrue(failed.isFailure)
            assertEquals(1L,dao.session("session")!!.revision)
            assertEquals("FINALIZING",dao.session("session")!!.phase)
            assertEquals("CLAIMED",dao.operation("session")!!.status)
            assertEquals(1,dao.takeOverOperation("session","new","next"))
            assertEquals(1,dao.changeOperationStatus("session","recovery","next","CLAIMED","COMMITTING"))
            assertTrue(dao.completeOperation("session","recovery","next"))
            assertNull(dao.operation("session"))
        } finally { db.close() }
    }

}
