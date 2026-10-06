package br.com.calcmot.securityrecording.domain

/** UI state is only projected from confirmed platform/persistence facts. */
enum class RecordingPhase { IDLE, PREPARING, RECORDING, PAUSING, PAUSED, RESUMING, ROTATING, FINALIZING, VERIFIED, FAILED }

data class RecordingReadiness(
    val cameraReady: Boolean,
    val microphoneReady: Boolean,
    val notificationsReady: Boolean,
    val lensId: String? = null
) {
    val canStart: Boolean get() = cameraReady && microphoneReady && notificationsReady && lensId != null
}

/** Immutable setup selected before a recording session exists. */
data class RecordingSetup(
    val lens: RecordingLens,
    val segmentMinutes: Int = DEFAULT_SEGMENT_MINUTES,
    val quality: RecordingQuality = RecordingQuality.P480
) {
    init {
        require(segmentMinutes in SEGMENT_DURATION_OPTIONS_MINUTES)
    }

    companion object {
        const val DEFAULT_SEGMENT_MINUTES = 10
        val SEGMENT_DURATION_OPTIONS_MINUTES = listOf(5, 10, 20, 30, 60)
    }
}

enum class RecordingLens(val label: String) {
    FRONT("Frontal"),
    BACK("Traseira")
}

/** The first release intentionally has no higher-quality fallback. */
enum class RecordingQuality(val label: String) { P480("480p") }

/** Conservative local-only guardrail for 480p A/V plus finalize headroom. */
object RecordingStoragePolicy {
    const val RESERVE_BYTES = 512L * 1024L * 1024L
    const val MAX_SEGMENT_BYTES = 1024L * 1024L * 1024L
    // Conservative admission estimate, pending device benchmark; never shown as remaining minutes.
    const val INITIAL_BYTES_PER_MINUTE = 45L * 1024L * 1024L
    fun requiredBytes(segmentMinutes: Int, finalizedSessionBytes: Long = 0L,
        bytesPerMinute: Long = INITIAL_BYTES_PER_MINUTE): Long {
        require(segmentMinutes in RecordingSetup.SEGMENT_DURATION_OPTIONS_MINUTES)
        require(finalizedSessionBytes >= 0 && bytesPerMinute > 0)
        val next = minOf(MAX_SEGMENT_BYTES, Math.multiplyExact(bytesPerMinute, segmentMinutes.toLong()))
        return Math.addExact(Math.addExact(finalizedSessionBytes, Math.multiplyExact(2L, next)), RESERVE_BYTES)
    }
    fun hasCapacity(availableBytes: Long, segmentMinutes: Int): Boolean =
        availableBytes >= requiredBytes(segmentMinutes)
    fun canContinue(availableBytes: Long, currentSessionBytes: Long): Boolean =
        currentSessionBytes >= 0 && availableBytes >= RESERVE_BYTES &&
            availableBytes - RESERVE_BYTES >= currentSessionBytes
}

data class RecordingArtifact(
    val sessionId: String,
    val fileName: String,
    val durationMs: Long,
    val bytes: Long,
    val hasAudio: Boolean,
    val hasVideo: Boolean,
    val sha256: String = ""
) {
    val isVerified: Boolean get() = durationMs > 0 && bytes > 0 && hasAudio && hasVideo
}
