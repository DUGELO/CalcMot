package br.com.calcmot.securityrecording.domain

/** REC requires an observed Start/Resume, active audio and encoded video progress. */
object RecordingCaptureEvidence {
    fun confirmed(startObserved: Boolean, activeAudio: Boolean, recordedDurationMs: Long, progressBaselineMs: Long = 0): Boolean =
        startObserved && activeAudio && recordedDurationMs > progressBaselineMs
    fun matches(command: RecordingCommand, sessionId: String, revision: Long): Boolean =
        command.sessionId == sessionId && command.expectedSessionRevision == revision
}
