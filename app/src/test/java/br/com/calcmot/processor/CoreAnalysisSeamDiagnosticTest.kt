package br.com.calcmot.processor

import br.com.calcmot.DriverApp
import br.com.calcmot.model.OfferCandidate
import br.com.calcmot.model.OfferCaptureSource
import br.com.calcmot.model.OfferCaptureRejectionReason
import org.junit.Assert.*
import org.junit.Test

/** Characterizes the existing seam. These are NOT claims of completed S10 Core integration. */
class CoreAnalysisSeamDiagnosticTest {
    private val candidate = OfferCandidate(10.0, 1.0, 2, 4.0, 8)

    @Test fun `both platforms renew nine repeated frames after the first accepted result`() {
        for ((platform, source) in listOf(DriverApp.UBER to OfferCaptureSource.ACCESSIBILITY_TREE,
            DriverApp.NINETY_NINE to OfferCaptureSource.NINETY_NINE_OCR)) {
            val coordinator = CaptureCoordinator()
            val decisions = (1..10).map {
                coordinator.acceptCandidate(source, candidate, trustedSingleFrame = true, driverApp = platform)
            }
            assertEquals(1, decisions.count { it is CaptureDecision.ShowOverlay })
            assertEquals(9, decisions.count { it is CaptureDecision.RenewCurrentOverlay })
        }
    }

    @Test fun `TTL then same offer produces ShowOverlay again so counting ShowOverlay is not logical dedupe`() {
        var now = 1000L
        val coordinator = CaptureCoordinator(clockMillis = { now })
        val first = coordinator.acceptCandidate(OfferCaptureSource.ACCESSIBILITY_TREE, candidate, true)
        now += 6000
        assertNotNull(coordinator.expireOverlayIfStale())
        val replay = coordinator.acceptCandidate(OfferCaptureSource.ACCESSIBILITY_TREE, candidate, true)
        assertTrue(first is CaptureDecision.ShowOverlay)
        assertTrue(replay is CaptureDecision.ShowOverlay)
        assertEquals((first as CaptureDecision.ShowOverlay).overlayFingerprint,
            (replay as CaptureDecision.ShowOverlay).overlayFingerprint)
    }

    @Test fun `same valued new offer without observable boundary is indistinguishable from a repeated frame`() {
        val coordinator = CaptureCoordinator()
        assertTrue(coordinator.acceptCandidate(OfferCaptureSource.ACCESSIBILITY_TREE, candidate, true)
            is CaptureDecision.ShowOverlay)
        assertTrue(coordinator.acceptCandidate(OfferCaptureSource.ACCESSIBILITY_TREE, candidate.copy(), true)
            is CaptureDecision.RenewCurrentOverlay)
    }

    @Test fun `transient invalid frames followed by the same result also produce another ShowOverlay`() {
        val coordinator = CaptureCoordinator()
        assertTrue(coordinator.acceptCandidate(OfferCaptureSource.NINETY_NINE_OCR, candidate, true,
            DriverApp.NINETY_NINE) is CaptureDecision.ShowOverlay)
        repeat(2) { coordinator.rejectFrame(OfferCaptureSource.NINETY_NINE_OCR,
            OfferCaptureRejectionReason.INVALID_FRAME) }
        assertTrue(coordinator.acceptCandidate(OfferCaptureSource.NINETY_NINE_OCR, candidate, true,
            DriverApp.NINETY_NINE) is CaptureDecision.ShowOverlay)
    }
}
