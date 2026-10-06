package br.com.calcmot.securityrecording.data

import android.content.Context
import androidx.room.withTransaction
import br.com.calcmot.securityrecording.application.SecurityRecordingBootstrap
import br.com.calcmot.securityrecording.domain.InterruptedSessionRecovery
import br.com.calcmot.securityrecording.domain.RecoveryTerminalState
import br.com.calcmot.securityrecording.domain.VerifiedRecordingSegment
import br.com.calcmot.securityrecording.domain.VerifiedRecordingSession
import br.com.calcmot.securityrecording.platform.VerifiedSegmentPromoter
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** The only read/recovery gateway used by the recording library. */
class RecordingLibraryRepository private constructor(
    private val context: Context,
    private val database: RecordingDatabase,
    private val promoter: VerifiedSegmentPromoter = VerifiedSegmentPromoter()
) {
    suspend fun verifiedSessions(): List<VerifiedRecordingSession> = withContext(Dispatchers.IO) {
        val dao = database.recordingDao()
        val segmentsBySession = dao.allSegments().groupBy { it.sessionId }
        dao.sessions()
            .filter { it.phase == "VERIFIED" || it.phase == "FAILED" }
            .map { session ->
                val segments = segmentsBySession[session.id].orEmpty()
                    .filter { segment ->
                        segment.verified && runCatching {
                            val file = File(segment.path)
                            file.isFile && promoter.verify(file, session.id).let { it.isVerified && it.sha256 == segment.sha256 }
                        }.getOrDefault(false)
                    }
                    .map { VerifiedRecordingSegment(it.id, it.ordinal, it.path, it.durationMs, it.bytes) }
                VerifiedRecordingSession(session.id, session.createdAtUtcMs, session.finalizedAtUtcMs, session.phase, session.failureCode, segments)
            }
    }

    /**
     * A dead process never restarts capture. It only reconciles files belonging to
     * known interrupted sessions and promotes an artifact after full verification.
     */
    suspend fun recoverInterruptedSessions() {
        check(SecurityRecordingBootstrap.awaitReady(context)) { "recovery_pending" }
    }

    internal suspend fun reconcilePreviousProcess() = withContext(Dispatchers.IO) {
        recoveryMutex.withLock {
            val dao = database.recordingDao()
            dao.interruptedSessions().forEach { session ->
            // Bootstrap completes before any new owner is admitted in this process.
            check(!br.com.calcmot.securityrecording.application.RecordingResourceCoordinator.isOwnerAlive(session.ownerId)) { "live_owner" }
            val claim = RecordingOperationClaimEntity(
                session.id, java.util.UUID.randomUUID().toString(), "RECOVERY", session.revision,
                SecurityRecordingBootstrap.processEpoch, "CLAIMED"
            )
            val previousClaim = dao.operation(session.id)
            val recoveryClaim = if (previousClaim != null) {
                check(previousClaim.kind == "RECOVERY") { "operation_reconciliation_pending" }
                check(previousClaim.processEpoch != SecurityRecordingBootstrap.processEpoch || previousClaim.status == "CLAIMED")
                if (previousClaim.processEpoch != SecurityRecordingBootstrap.processEpoch) {
                    check(dao.takeOverOperation(session.id, previousClaim.processEpoch, SecurityRecordingBootstrap.processEpoch) == 1)
                }
                previousClaim.copy(processEpoch = SecurityRecordingBootstrap.processEpoch)
            } else {
                check(dao.reserveOperation(claim)) { "recovery_claim_pending" }
                claim
            }
            val existingSegments = dao.segmentsForSession(session.id)
            val recoveredPaths = existingSegments.map { File(it.path).absolutePath }.toMutableSet()
            val validExistingIds = existingSegments.filter { segment ->
                segment.verified && runCatching { promoter.verify(File(segment.path), session.id).let { it.isVerified && it.sha256 == segment.sha256 } }.getOrDefault(false)
            }.map { it.id }.toSet()
            var confirmedDuration = existingSegments.filter { it.id in validExistingIds }.sumOf { it.durationMs }
            var validCount = validExistingIds.size
            var nextOrdinal = (existingSegments.maxOfOrNull { it.ordinal } ?: -1) + 1
            val directory = File(context.filesDir, "security-recording/sessions/${session.id}")
            val candidatesByPath = directory.listFiles()
                .orEmpty()
                .filter { it.isFile && (it.name.endsWith(".pending") || it.name.endsWith(".mp4")) }
                .associateBy { it.absolutePath }
            InterruptedSessionRecovery.unknownCandidatePaths(recoveredPaths, candidatesByPath.keys)
                .forEach { candidatePath ->
                    val file = candidatesByPath.getValue(candidatePath)
                    val artifact = runCatching {
                        if (file.name.endsWith(".pending")) {
                            promoter.promote(file, File(directory, file.nameWithoutExtension + ".mp4"), session.id).getOrThrow()
                        } else promoter.verify(file, session.id)
                    }.getOrNull()
                    if (artifact?.isVerified == true) {
                        val path = File(directory, artifact.fileName).absolutePath
                        val segmentId = "${session.id}-recovered-$nextOrdinal"
                        runCatching {
                            database.withTransaction {
                                check(dao.insertSegment(
                                    RecordingSegmentEntity(
                                        id = segmentId,
                                        sessionId = session.id,
                                        ordinal = nextOrdinal,
                                        path = path,
                                        durationMs = artifact.durationMs,
                                        bytes = artifact.bytes,
                                        verified = true,
                                        sha256 = artifact.sha256
                                    )
                                ) != -1L)
                                dao.insertArtifact(
                                    RecordingArtifactEntity(
                                        id = "${session.id}-artifact-recovered-$nextOrdinal",
                                        sessionId = session.id,
                                        segmentId = segmentId,
                                        path = path,
                                        state = "VERIFIED_RECOVERED",
                                        sha256 = artifact.sha256
                                    )
                                )
                                dao.insertClaims(
                                    listOf(
                                        RecordingClaimEntity("${session.id}-claim-container-$nextOrdinal", session.id, "container_readable_$nextOrdinal", "true"),
                                        RecordingClaimEntity("${session.id}-claim-duration-$nextOrdinal", session.id, "duration_positive_$nextOrdinal", "true"),
                                        RecordingClaimEntity("${session.id}-claim-audio-$nextOrdinal", session.id, "has_audio_$nextOrdinal", "true"),
                                        RecordingClaimEntity("${session.id}-claim-video-$nextOrdinal", session.id, "has_video_$nextOrdinal", "true")
                                    )
                                )
                            }
                        }.getOrThrow()
                        val inserted = true
                        if (inserted) {
                            validCount++
                            confirmedDuration = Math.addExact(confirmedDuration, artifact.durationMs)
                            nextOrdinal++
                            recoveredPaths += path
                        }
                    }
                }
            val preserved = InterruptedSessionRecovery.terminalState(validCount) == RecoveryTerminalState.PRESERVED
            database.withTransaction {
                check(dao.changeOperationStatus(session.id, recoveryClaim.operationId, recoveryClaim.processEpoch, "CLAIMED", "COMMITTING") == 1)
                val current = dao.session(session.id) ?: error("missing_session")
                check(current.revision == recoveryClaim.baseRevision + 1)
                val finalizedAt = current.finalizedAtUtcMs ?: System.currentTimeMillis()
                // Duration includes only media verified during this reconciliation.
                check(dao.updateSessionEntity(current.copy(
                    revision = current.revision + 1,
                    phase = if (preserved) "VERIFIED" else "FAILED",
                    failureCode = if (preserved) "interrupted_recovered" else "interrupted_no_valid_segment",
                    completionReason = "unexpected_end", finalizedAtUtcMs = finalizedAt,
                    capturedDurationMs = if (preserved) confirmedDuration else current.capturedDurationMs,
                    expiresAtUtcMs = current.retentionDays?.let { br.com.calcmot.securityrecording.domain.RecordingRetention.expiresAt(finalizedAt, it) }
                )) == 1)
                check(dao.releaseOperation(session.id, recoveryClaim.operationId, recoveryClaim.processEpoch) == 1)
            }
            }
        }
    }

    companion object {
        private val recoveryMutex = Mutex()

        private val ACTIVE_PHASES = setOf("PREPARING", "RECORDING", "FINALIZING")

        fun create(context: Context): RecordingLibraryRepository = RecordingLibraryRepository(
            context.applicationContext,
            RecordingDatabaseProvider.get(context)
        )
    }
}
