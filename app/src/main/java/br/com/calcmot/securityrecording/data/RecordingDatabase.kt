package br.com.calcmot.securityrecording.data

import android.content.Context
import androidx.room.Update
import androidx.room.ColumnInfo
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.Index
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "recording_sessions")
data class RecordingSessionEntity(
    @PrimaryKey val id: String,
    val revision: Long,
    val phase: String,
    val failureCode: String? = null,
    val createdAtUtcMs: Long,
    val finalizedAtUtcMs: Long? = null,
    val processEpoch: String? = null,
    val ownerId: String? = null,
    val lens: String? = null,
    val orientation: Int? = null,
    val quality: String? = null,
    val segmentMinutes: Int? = null,
    val capturedDurationMs: Long? = null,
    val completionReason: String? = null,
    val retentionDays: Int? = null,
    val expiresAtUtcMs: Long? = null,
    @ColumnInfo(defaultValue = "'AVAILABLE'") val temporaryAvailability: String = "AVAILABLE",
    @ColumnInfo(defaultValue = "'NONE'") val galleryState: String = "NONE",
    @ColumnInfo(defaultValue = "'IDLE'") val exportState: String = "IDLE",
    val galleryUri: String? = null,
    val exportPath: String? = null,
    val exportSha256: String? = null,
    val gallerySha256: String? = null,
    val lastShareLaunchedAtUtcMs: Long? = null,
    @ColumnInfo(defaultValue = "'LEGACY_UNBOUNDED'") val retentionOrigin: String = "LEGACY_UNBOUNDED"
)

@Entity(
    tableName = "recording_segments",
    foreignKeys = [ForeignKey(
        entity = RecordingSessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["sessionId", "ordinal"], unique = true)]
)
data class RecordingSegmentEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val ordinal: Int,
    val path: String,
    val durationMs: Long,
    val bytes: Long,
    val verified: Boolean,
    val sha256: String = ""
)

@Entity(
    tableName = "recording_artifacts",
    foreignKeys = [
        ForeignKey(entity = RecordingSessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = RecordingSegmentEntity::class, parentColumns = ["id"], childColumns = ["segmentId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("sessionId"), Index("segmentId", unique = true)]
)
data class RecordingArtifactEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val segmentId: String,
    val path: String,
    val state: String,
    val sha256: String
)

@Entity(
    tableName = "recording_claims",
    foreignKeys = [ForeignKey(entity = RecordingSessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["sessionId", "name"], unique = true)]
)
data class RecordingClaimEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val name: String,
    val value: String
)

/** Reserved for explicitly observed capture gaps; Epic 1 never manufactures pause data. */
@Entity(
    tableName = "recording_gaps",
    foreignKeys = [ForeignKey(entity = RecordingSessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("sessionId")]
)
data class RecordingGapEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val startedMonotonicMs: Long,
    val durationMs: Long,
    val reason: String
)

/** Durable ownership, deliberately separate from recording_claims verification facts. */
@Entity(
    tableName = "recording_operation_claims",
    foreignKeys = [ForeignKey(entity = RecordingSessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["operationId"], unique = true)]
)
data class RecordingOperationClaimEntity(
    @PrimaryKey val sessionId: String,
    val operationId: String,
    val kind: String,
    val baseRevision: Long,
    val processEpoch: String,
    val status: String
)

@Dao interface RecordingDao {
    @Update suspend fun updateSessionEntity(value: RecordingSessionEntity): Int
    @Transaction
    suspend fun writeOwned(expectedRevision: Long, value: RecordingSessionEntity): Boolean {
        val previous = session(value.id) ?: return false
        if (previous.revision != expectedRevision || value.revision != expectedRevision + 1 ||
            previous.ownerId != value.ownerId || previous.processEpoch != value.processEpoch ||
            operation(value.id) != null) return false
        return updateSessionEntity(value) == 1
    }

    @Query("SELECT * FROM recording_sessions WHERE id = :id")
    suspend fun session(id: String): RecordingSessionEntity?
    @Query("SELECT * FROM recording_sessions ORDER BY createdAtUtcMs DESC, id")
    fun observeSessions(): Flow<List<RecordingSessionEntity>>
    @Query("SELECT * FROM recording_sessions WHERE id = :id")
    fun observeSession(id: String): Flow<RecordingSessionEntity?>
    @Query("SELECT * FROM recording_gaps WHERE sessionId = :id ORDER BY startedMonotonicMs")
    suspend fun gaps(id: String): List<RecordingGapEntity>
    @Query("SELECT * FROM recording_operation_claims WHERE sessionId = :id")
    suspend fun operation(id: String): RecordingOperationClaimEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOperation(claim: RecordingOperationClaimEntity)
    @Query("DELETE FROM recording_operation_claims WHERE sessionId = :id AND operationId = :operationId AND processEpoch = :epoch")
    suspend fun releaseOperation(id: String, operationId: String, epoch: String): Int
    @Query("UPDATE recording_operation_claims SET status = :status WHERE sessionId = :id AND operationId = :operationId AND processEpoch = :epoch AND status = :expectedStatus")
    suspend fun changeOperationStatus(id: String, operationId: String, epoch: String, expectedStatus: String, status: String): Int
    @Query("UPDATE recording_sessions SET revision = revision + 1 WHERE id = :id AND revision = :revision")
    suspend fun advanceRevision(id: String, revision: Long): Int

    /** Reserving and incrementing the one revision are atomic, even across connections. */
    @Transaction
    suspend fun reserveOperation(claim: RecordingOperationClaimEntity): Boolean {
        val current = session(claim.sessionId) ?: return false
        if (current.revision != claim.baseRevision || operation(claim.sessionId) != null) return false
        if (advanceRevision(claim.sessionId, claim.baseRevision) != 1) return false
        insertOperation(claim)
        return true
    }

    @Transaction
    suspend fun completeOperation(id: String, operationId: String, epoch: String): Boolean {
        val claim = operation(id) ?: return false
        if (claim.operationId != operationId || claim.processEpoch != epoch || claim.status != "COMMITTING") return false
        if (advanceRevision(id, claim.baseRevision + 1) != 1) return false
        check(releaseOperation(id, operationId, epoch) == 1)
        return true
    }

    @Query("SELECT * FROM recording_operation_claims")
    suspend fun operations(): List<RecordingOperationClaimEntity>
    @Query("UPDATE recording_operation_claims SET processEpoch = :newEpoch WHERE sessionId = :id AND processEpoch = :oldEpoch")
    suspend fun takeOverOperation(id: String, oldEpoch: String, newEpoch: String): Int

    @Query("UPDATE recording_sessions SET capturedDurationMs = :duration, revision = revision + 1 WHERE id = :id AND revision = :revision AND ownerId = :owner AND processEpoch = :epoch AND (capturedDurationMs IS NULL OR capturedDurationMs <= :duration)")
    suspend fun checkpointDuration(id: String, revision: Long, owner: String, epoch: String, duration: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertSession(session: RecordingSessionEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertSegment(segment: RecordingSegmentEntity): Long
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertArtifact(artifact: RecordingArtifactEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertClaims(claims: List<RecordingClaimEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertGap(gap: RecordingGapEntity): Long
    @Query("UPDATE recording_sessions SET phase = :phase, revision = revision + 1 WHERE id = :id AND revision = :expectedRevision")
    suspend fun compareAndSetPhase(id: String, expectedRevision: Long, phase: String): Int
    @Query("UPDATE recording_sessions SET phase = :phase, revision = revision + 1, failureCode = :failureCode, finalizedAtUtcMs = :finalizedAt WHERE id = :id AND revision = :expectedRevision")
    suspend fun compareAndSetTerminal(id: String, expectedRevision: Long, phase: String, failureCode: String?, finalizedAt: Long): Int
    @Query("SELECT * FROM recording_sessions ORDER BY COALESCE(finalizedAtUtcMs, createdAtUtcMs) DESC")
    suspend fun sessions(): List<RecordingSessionEntity>
    @Query("SELECT * FROM recording_segments WHERE sessionId = :sessionId ORDER BY ordinal ASC")
    suspend fun segmentsForSession(sessionId: String): List<RecordingSegmentEntity>
    @Query("SELECT * FROM recording_segments ORDER BY sessionId, ordinal ASC")
    suspend fun allSegments(): List<RecordingSegmentEntity>
    @Query("SELECT * FROM recording_sessions WHERE phase IN ('PREPARING', 'RECORDING', 'PAUSING', 'PAUSED', 'RESUMING', 'ROTATING', 'FINALIZING')")
    suspend fun interruptedSessions(): List<RecordingSessionEntity>
    @Query("UPDATE recording_sessions SET phase = 'FAILED', failureCode = :failureCode, finalizedAtUtcMs = COALESCE(finalizedAtUtcMs, :finalizedAt), revision = revision + 1 WHERE id = :id AND phase IN ('PREPARING', 'RECORDING', 'PAUSING', 'PAUSED', 'RESUMING', 'ROTATING', 'FINALIZING')")
    suspend fun failActiveSession(id: String, failureCode: String, finalizedAt: Long): Int
}

@Database(
    entities = [
        RecordingSessionEntity::class,
        RecordingSegmentEntity::class,
        RecordingArtifactEntity::class,
        RecordingClaimEntity::class,
        RecordingGapEntity::class,
        RecordingOperationClaimEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class RecordingDatabase : RoomDatabase() { abstract fun recordingDao(): RecordingDao }

/** One process-wide Room instance prevents connection-pool and file-descriptor churn. */
object RecordingDatabaseProvider {
    @Volatile private var instance: RecordingDatabase? = null

    fun get(context: Context): RecordingDatabase = instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
            context.applicationContext,
            RecordingDatabase::class.java,
            "calcmot_recordings.db"
        ).addMigrations(RecordingMigrations.FROM_1_TO_2).build().also { instance = it }
    }
}
