package br.com.calcmot.securityrecording.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class InterruptedSessionRecoveryTest {
    @Test fun `preserves an interrupted session only when a verified segment exists`() {
        assertEquals(RecoveryTerminalState.PRESERVED, InterruptedSessionRecovery.terminalState(validSegmentCount = 1))
    }

    @Test fun `fails closed when an interrupted session has no verified media`() {
        assertEquals(RecoveryTerminalState.FAILED, InterruptedSessionRecovery.terminalState(validSegmentCount = 0))
    }

    @Test fun `maps scrub position across verified media segments`() {
        val segments = listOf(
            VerifiedRecordingSegment("one", 0, "/one.mp4", 1_000, 10),
            VerifiedRecordingSegment("two", 1, "/two.mp4", 2_000, 20)
        )

        assertEquals(SegmentPlaybackPosition(1, 500), RecordingPlayback.positionFor(segments, 1_500))
    }

    @Test fun `clamps scrub position to the end of the last segment`() {
        val segments = listOf(VerifiedRecordingSegment("one", 0, "/one.mp4", 1_000, 10))

        assertEquals(SegmentPlaybackPosition(0, 1_000), RecordingPlayback.positionFor(segments, 9_999))
    }

    @Test fun `maps an exact segment boundary to the following Media3 item`() {
        val segments = listOf(
            VerifiedRecordingSegment("one", 0, "/one.mp4", 1_000, 10),
            VerifiedRecordingSegment("two", 1, "/two.mp4", 2_000, 20)
        )

        assertEquals(SegmentPlaybackPosition(1, 0), RecordingPlayback.positionFor(segments, 1_000))
    }

    @Test fun `recovery scan ignores persisted artifacts and duplicate paths`() {
        val plan = InterruptedSessionRecovery.unknownCandidatePaths(
            knownPaths = setOf("/recordings/already-verified.mp4"),
            scannedPaths = listOf(
                "/recordings/pending-b.pending",
                "/recordings/already-verified.mp4",
                "/recordings/pending-a.pending",
                "/recordings/pending-b.pending"
            )
        )

        assertEquals(listOf("/recordings/pending-a.pending", "/recordings/pending-b.pending"), plan)
        assertEquals(emptyList<String>(), InterruptedSessionRecovery.unknownCandidatePaths(plan.toSet(), plan))
    }
}
