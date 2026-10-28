package br.com.calcmot.entitlement

import org.junit.Assert.*
import org.junit.Test

/** Executable decision table ONLY, not a production gate or evidence of a parked vehicle.
 * Inputs needing session-stop acknowledgment/freshness are NOT available atomically today.
 */
class SafeEntryProposalTest {
    private data class Signals(val voluntary: Boolean = true, val mainFocused: Boolean = true,
        val sessionEnded: Boolean? = true, val monitoringOff: Boolean? = true,
        val pipelineIdle: Boolean? = true, val offerInFlight: Boolean? = false,
        val overlayVisible: Boolean? = false, val generationUnchanged: Boolean = true)
    private enum class Result { DEFER, NON_BLOCKING_NOTICE, EXPLICIT_RECOVERY }
    private fun evaluate(s: Signals, userRequestedRecovery: Boolean = false): Result {
        if (!s.voluntary || !s.mainFocused) return Result.DEFER
        if (s.sessionEnded != true || s.monitoringOff != true || s.pipelineIdle != true ||
            s.offerInFlight != false || s.overlayVisible != false || !s.generationUnchanged) return Result.DEFER
        return if (userRequestedRecovery) Result.EXPLICIT_RECOVERY else Result.NON_BLOCKING_NOTICE
    }
    @Test fun foreignAppOrBackgroundNeverPresentsCommercialNotice() {
        assertEquals(Result.DEFER, evaluate(Signals(mainFocused = false)))
        assertEquals(Result.DEFER, evaluate(Signals(voluntary = false)))
    }
    @Test fun voluntaryMainEntryDuringDrivingSessionStillDefers() {
        assertEquals(Result.DEFER, evaluate(Signals(sessionEnded = false)))
        assertEquals(Result.DEFER, evaluate(Signals(monitoringOff = false)))
    }
    @Test fun noOfferDoesNotMeanParkedOrSessionEnded() {
        assertEquals(Result.DEFER, evaluate(Signals(sessionEnded = null, offerInFlight = false)))
    }
    @Test fun unknownBusyOfferOrVisibleOverlayDefersWithoutSideEffects() {
        for (s in listOf(Signals(pipelineIdle = null), Signals(pipelineIdle = false),
            Signals(offerInFlight = null), Signals(offerInFlight = true), Signals(overlayVisible = true),
            Signals(overlayVisible = null), Signals(monitoringOff = null))) assertEquals(Result.DEFER, evaluate(s))
    }
    @Test fun changedGenerationBetweenDecisionAndPresentationDefers() {
        assertEquals(Result.DEFER, evaluate(Signals(generationUnchanged = false)))
    }
    @Test fun explicitCompletedSessionAllowsOnlyNonBlockingNoticeOrVoluntaryRecovery() {
        assertEquals(Result.NON_BLOCKING_NOTICE, evaluate(Signals()))
        assertEquals(Result.EXPLICIT_RECOVERY, evaluate(Signals(), userRequestedRecovery = true))
        // Model returns an intent only. No hideOverlay/stopReading/paywall/network operation exists.
    }
}
