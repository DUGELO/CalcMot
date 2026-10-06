package br.com.calcmot.securityrecording.domain

/** A library item is an aggregate: a session is never repeated for each media file. */
data class VerifiedRecordingSession(
    val id: String,
    val createdAtUtcMs: Long,
    val finalizedAtUtcMs: Long?,
    val phase: String,
    val failureCode: String?,
    val segments: List<VerifiedRecordingSegment>
) {
    val durationMs: Long get() = segments.sumOf { it.durationMs }
    val bytes: Long get() = segments.sumOf { it.bytes }
    val isAvailable: Boolean get() = phase == "VERIFIED" && segments.isNotEmpty()
}

data class VerifiedRecordingSegment(
    val id: String,
    val ordinal: Int,
    val path: String,
    val durationMs: Long,
    val bytes: Long
)

/** Maps a session-wide timestamp to the Media3 item and its local timestamp. */
data class SegmentPlaybackPosition(val segmentIndex: Int, val positionMs: Long)

object RecordingPlayback {
    fun positionFor(segments: List<VerifiedRecordingSegment>, targetMs: Long): SegmentPlaybackPosition? {
        if (segments.isEmpty()) return null
        var remaining = targetMs.coerceIn(0L, segments.sumOf { it.durationMs })
        segments.forEachIndexed { index, segment ->
            // A segment boundary belongs to the next item. This avoids seeking to a
            // completed item when the user scrubs to its exact final millisecond.
            if (remaining < segment.durationMs || index == segments.lastIndex) {
                return SegmentPlaybackPosition(index, remaining.coerceAtMost(segment.durationMs))
            }
            remaining -= segment.durationMs
        }
        return null
    }
}

/**
 * Fail closed when a capture was interrupted. This is deliberately pure so the
 * recovery policy is testable independently from Room, files and Android media APIs.
 */
object InterruptedSessionRecovery {
    fun terminalState(validSegmentCount: Int): RecoveryTerminalState =
        if (validSegmentCount > 0) RecoveryTerminalState.PRESERVED else RecoveryTerminalState.FAILED

    /**
     * Returns a deterministic, duplicate-free scan plan. On a subsequent recovery
     * pass every previously persisted artifact is part of [knownPaths], so nothing
     * is promoted or inserted twice.
     */
    fun unknownCandidatePaths(knownPaths: Set<String>, scannedPaths: Iterable<String>): List<String> =
        scannedPaths.asSequence()
            .filterNot(knownPaths::contains)
            .distinct()
            .sorted()
            .toList()
}

enum class RecoveryTerminalState { PRESERVED, FAILED }
