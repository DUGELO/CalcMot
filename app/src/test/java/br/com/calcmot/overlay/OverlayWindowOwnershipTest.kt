package br.com.calcmot.overlay

import org.junit.Assert.*
import org.junit.Test

class OverlayWindowOwnershipTest {
    @Test fun replacingSameFingerprintRequestRejectsOldPredrawAndCleanup() {
        val owner = OverlayWindowOwnership()
        val first = PendingOverlayOperation(2_000)
        val next = PendingOverlayOperation(2_100)
        owner.attach(first)
        assertTrue(owner.owns(first))
        owner.attach(next)
        assertFalse(owner.owns(first))
        assertTrue(owner.owns(next))
    }
    @Test fun removedWindowAndAnotherManagerDoNotShareOwnership() {
        val firstManager = OverlayWindowOwnership()
        val nextManager = OverlayWindowOwnership()
        val first = PendingOverlayOperation(2_000)
        val next = PendingOverlayOperation(2_100)
        firstManager.attach(first)
        nextManager.attach(next)
        firstManager.clear()
        assertFalse(firstManager.owns(first))
        assertFalse(nextManager.owns(first))
        assertTrue(nextManager.owns(next))
    }
}
