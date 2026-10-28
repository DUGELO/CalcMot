package br.com.calcmot.entitlement

import org.junit.Assert.*
import org.junit.Test

class SafeEntryObservationTest {
    private fun ended(generation: Long = 1) = SessionObservation("synthetic-session", generation, true, generation, 0, false, true)
    @Test fun defaultsAndMissingAcknowledgmentNeverQualify() {
        val o = SafeEntryObservation()
        assertFalse(o.qualified(EntryOrigin.VOLUNTARY_MAIN_ACTION, true, 1))
        o.observe(ended().copy(completedGeneration = null))
        assertFalse(o.qualified(EntryOrigin.VOLUNTARY_MAIN_ACTION, true, 1))
        o.observe(ended().copy(producerHealthy = null))
        assertEquals(SessionReadiness.UNKNOWN, o.state.value.readiness)
    }
    @Test fun consistentCompletionRequiresVoluntaryFocusAndKeepsGatesOff() {
        val o = SafeEntryObservation(); o.observe(ended())
        assertTrue(o.qualified(EntryOrigin.VOLUNTARY_MAIN_ACTION, true, 1))
        assertFalse(o.qualified(EntryOrigin.RESUME, true, 1))
        assertFalse(o.qualified(EntryOrigin.EXTERNAL, true, 1))
        assertFalse(o.qualified(EntryOrigin.VOLUNTARY_MAIN_ACTION, false, 1))
        assertFalse(o.commercialGatesEnabled)
    }
    @Test fun newWorkInvalidatesCapturedPresentationGeneration() {
        val o = SafeEntryObservation(); o.observe(ended())
        o.observe(ended(2).copy(inFlight = 1, completedGeneration = null))
        assertFalse(o.qualified(EntryOrigin.VOLUNTARY_MAIN_ACTION, true, 1))
        assertFalse(o.qualified(EntryOrigin.VOLUNTARY_MAIN_ACTION, true, 2))
        o.observe(ended(2)); assertTrue(o.qualified(EntryOrigin.VOLUNTARY_MAIN_ACTION, true, 2))
    }
    @Test fun contradictionsOverlayAndDisconnectSuppressAndRejectReplays() {
        val o = SafeEntryObservation(); o.observe(ended().copy(overlayVisible = true))
        assertFalse(o.qualified(EntryOrigin.VOLUNTARY_MAIN_ACTION, true, 1))
        o.observe(ended()); o.invalidate(); o.observe(ended())
        assertFalse(o.qualified(EntryOrigin.VOLUNTARY_MAIN_ACTION, true, 1))
        o.observe(ended(2)); o.observe(ended(1))
        assertEquals(SessionReadiness.UNKNOWN, o.state.value.readiness)
        o.observe(ended(3)); o.observe(ended(3).copy(producerHealthy = false)); o.observe(ended(3))
        assertFalse(o.qualified(EntryOrigin.VOLUNTARY_MAIN_ACTION, true, 3))
    }
}
