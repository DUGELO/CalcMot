package br.com.calcmot.overlay

import org.junit.Assert.*
import org.junit.Test

class PendingOverlayOperationTest {
    @Test fun timedOutQueuedCardNeverRuns() {
        val operation = PendingOverlayOperation(2_000)
        operation.cancel()
        assertFalse(operation.tryStart(2_001, true))
    }
    @Test fun deadlineProtectsEvenIfWaitingThreadHasNotWokenUp() {
        assertFalse(PendingOverlayOperation(2_000).tryStart(2_000, true))
    }
    @Test fun hideOrReconnectInvalidatesOldOffer() {
        assertFalse(PendingOverlayOperation(2_000).tryStart(500, false))
    }
    @Test fun healthyOperationRunsExactlyOnce() {
        val operation = PendingOverlayOperation(2_000)
        assertTrue(operation.tryStart(500, true))
        assertFalse(operation.tryStart(501, true))
    }
    @Test fun interruptionCancelsQueuedWork() {
        val operation = PendingOverlayOperation(2_000)
        operation.cancel()
        assertFalse(operation.tryStart(100, true))
    }

    @Test fun cancellationRevokesAnAlreadyStartedRequest() {
        val operation = PendingOverlayOperation(2_000)
        assertTrue(operation.tryStart(100, true))
        operation.cancel()
        assertFalse(operation.isValid(200, true))
    }
    @Test fun startedRequestExpiresBeforeWindowCommit() {
        val operation = PendingOverlayOperation(2_000)
        assertTrue(operation.tryStart(100, true))
        assertFalse(operation.isValid(2_000, true))
    }
    @Test fun badTokenRetryMustKeepOriginalGeneration() {
        val generation = java.util.concurrent.atomic.AtomicLong(1)
        val requestGeneration = generation.get()
        val operation = PendingOverlayOperation(2_000)
        assertTrue(operation.tryStart(100, true))
        generation.incrementAndGet() // hide/expire submitted while addView was in progress
        assertFalse(operation.isValid(200, requestGeneration == generation.get()))
    }
    @Test fun concurrentCancellationAlwaysInvalidatesCommitAfterBothReturn() {
        repeat(100) {
            val operation = PendingOverlayOperation(2_000)
            val start = java.util.concurrent.CountDownLatch(1)
            val cancel = Thread { start.await(); operation.cancel() }
            cancel.start()
            start.countDown()
            operation.tryStart(100, true)
            cancel.join(2_000)
            assertFalse(cancel.isAlive)
            assertFalse(operation.isValid(101, true))
        }
    }
}
