package br.com.calcmot.securityrecording.data

import androidx.room.Room
import androidx.room.withTransaction
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RecordingMigrationTest {
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(), RecordingDatabase::class.java
    )

    @Test fun preservesLegacyFactsAndDoesNotInventExpiry() {
        val name = "recording-migration-synthetic"
        helper.createDatabase(name, 1).apply {
            execSQL("INSERT INTO recording_sessions(id,revision,phase,createdAtUtcMs) VALUES('valid',4,'VERIFIED',100),('active',1,'RECORDING',200),('failed',2,'FAILED',300)")
            execSQL("INSERT INTO recording_segments(id,sessionId,ordinal,path,durationMs,bytes,verified,sha256) VALUES('segment','valid',0,'synthetic.mp4',500,1000,1,'synthetic-digest')")
            execSQL("INSERT INTO recording_claims(id,sessionId,name,value) VALUES('fact','valid','has_audio','true')")
            close()
        }
        helper.runMigrationsAndValidate(name, 2, true, RecordingMigrations.FROM_1_TO_2).apply {
            query("SELECT phase,capturedDurationMs,expiresAtUtcMs,retentionOrigin FROM recording_sessions WHERE id='valid'").use {
                assertTrue(it.moveToFirst()); assertEquals("VERIFIED",it.getString(0))
                assertTrue(it.isNull(1)); assertTrue(it.isNull(2)); assertEquals("LEGACY_UNBOUNDED",it.getString(3))
            }
            query("SELECT value FROM recording_claims WHERE id='fact'").use { assertTrue(it.moveToFirst()); assertEquals("true",it.getString(0)) }
            query("SELECT COUNT(*) FROM recording_segments").use { it.moveToFirst(); assertEquals(1,it.getInt(0)) }
            close()
        }
        // Reopen through the production provider schema, without a destructive fallback.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val reopened = Room.databaseBuilder(context, RecordingDatabase::class.java, name)
            .addMigrations(RecordingMigrations.FROM_1_TO_2).build()
        try {
                runBlocking { assertEquals(3,reopened.recordingDao().sessions().size) }
            }
        finally { reopened.close() }
        context.deleteDatabase(name)
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
