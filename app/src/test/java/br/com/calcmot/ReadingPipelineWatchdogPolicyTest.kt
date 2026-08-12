package br.com.calcmot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReadingPipelineWatchdogPolicyTest {

    @Test
    fun busyPipelinePastTimeoutRequestsControlledReset() {
        val now = ReadingPipelineWatchdogPolicy.MIN_RESET_INTERVAL_MS + 50_000L
        val snapshot = ReadingPipelineRuntime.Snapshot(
            pipelineState = ReadingPipelineRuntime.PipelineState.OCR,
            stateSinceElapsedRealtime = now - ReadingPipelineWatchdogPolicy.BUSY_TIMEOUT_MS,
            lastActivityElapsedRealtime = now - ReadingPipelineWatchdogPolicy.BUSY_TIMEOUT_MS
        )

        assertEquals(
            ReadingPipelineRuntime.RestartReason.WATCHDOG_BUSY,
            ReadingPipelineWatchdogPolicy.reason(
                snapshot = snapshot,
                nowElapsedRealtime = now,
                lastResetElapsedRealtime = 0L
            )
        )
    }

    @Test
    fun healthyIdlePipelineDoesNotReset() {
        val now = 900_000L
        val snapshot = ReadingPipelineRuntime.Snapshot(
            pipelineState = ReadingPipelineRuntime.PipelineState.IDLE,
            stateSinceElapsedRealtime = now - 1_000L,
            lastActivityElapsedRealtime = now - 1_000L
        )

        assertNull(
            ReadingPipelineWatchdogPolicy.reason(snapshot, now, lastResetElapsedRealtime = 0L)
        )
    }

    @Test
    fun resetIsRateLimited() {
        val now = 900_000L
        val snapshot = ReadingPipelineRuntime.Snapshot(
            pipelineState = ReadingPipelineRuntime.PipelineState.BUSY,
            stateSinceElapsedRealtime = 0L,
            lastActivityElapsedRealtime = 0L
        )

        assertNull(
            ReadingPipelineWatchdogPolicy.reason(
                snapshot = snapshot,
                nowElapsedRealtime = now,
                lastResetElapsedRealtime = now - 1_000L
            )
        )
    }

    @Test
    fun failedPipelineIsRecoveredAfterGracePeriod() {
        val now = ReadingPipelineWatchdogPolicy.MIN_RESET_INTERVAL_MS + 50_000L
        val snapshot = ReadingPipelineRuntime.Snapshot(
            pipelineState = ReadingPipelineRuntime.PipelineState.FAILED,
            stateSinceElapsedRealtime = now - ReadingPipelineWatchdogPolicy.BUSY_TIMEOUT_MS,
            lastActivityElapsedRealtime = now
        )

        assertEquals(
            ReadingPipelineRuntime.RestartReason.WATCHDOG_FAILED,
            ReadingPipelineWatchdogPolicy.reason(snapshot, now, lastResetElapsedRealtime = 0L)
        )
    }
}
