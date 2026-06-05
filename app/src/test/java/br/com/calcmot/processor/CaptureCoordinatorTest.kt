package br.com.calcmot.processor

import br.com.calcmot.model.OfferCandidate
import br.com.calcmot.model.OfferCaptureRejectionReason
import br.com.calcmot.model.OfferCaptureSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureCoordinatorTest {

    private var now = 1_000L

    @Test
    fun `same production candidate needs two frames before overlay`() {
        val coordinator = CaptureCoordinator(requiredMatchingFrames = 2)
        val candidate = offer(price = 10.0)

        val first = coordinator.acceptCandidate(OfferCaptureSource.ACCESSIBILITY_TREE, candidate)
        val second = coordinator.acceptCandidate(OfferCaptureSource.ACCESSIBILITY_TREE, candidate)

        assertTrue(first is CaptureDecision.WaitingForStability)
        assertTrue(second is CaptureDecision.ShowOverlay)
        assertEquals(candidate.overlayFingerprint(), (second as CaptureDecision.ShowOverlay).overlayFingerprint)
    }

    @Test
    fun `trusted accessibility candidate can show overlay on first frame`() {
        val coordinator = CaptureCoordinator(requiredMatchingFrames = 2)
        val candidate = offer(price = 10.0)

        val decision = coordinator.acceptCandidate(
            source = OfferCaptureSource.ACCESSIBILITY_TREE,
            candidate = candidate,
            trustedSingleFrame = true
        )

        assertTrue(decision is CaptureDecision.ShowOverlay)
        assertEquals(candidate.overlayFingerprint(), (decision as CaptureDecision.ShowOverlay).overlayFingerprint)
    }

    @Test
    fun `trusted accessibility candidate stays idempotent on first frame`() {
        val coordinator = CaptureCoordinator(requiredMatchingFrames = 2)
        val candidate = offer(price = 10.0)

        val first = coordinator.acceptCandidate(
            source = OfferCaptureSource.ACCESSIBILITY_TREE,
            candidate = candidate,
            trustedSingleFrame = true
        )
        val second = coordinator.acceptCandidate(
            source = OfferCaptureSource.ACCESSIBILITY_TREE,
            candidate = candidate,
            trustedSingleFrame = true
        )

        assertTrue(first is CaptureDecision.ShowOverlay)
        assertTrue(second is CaptureDecision.RenewCurrentOverlay)
        assertEquals(candidate.overlayFingerprint(), (second as CaptureDecision.RenewCurrentOverlay).overlayFingerprint)
    }

    @Test
    fun `trusted different candidate does not replace visible overlay on first frame`() {
        val coordinator = CaptureCoordinator(requiredMatchingFrames = 2)
        val firstOffer = offer(price = 10.0)
        val secondOffer = offer(price = 12.0)

        coordinator.acceptCandidate(
            source = OfferCaptureSource.ACCESSIBILITY_TREE,
            candidate = firstOffer,
            trustedSingleFrame = true
        )
        val firstDifferentFrame = coordinator.acceptCandidate(
            source = OfferCaptureSource.ACCESSIBILITY_TREE,
            candidate = secondOffer,
            trustedSingleFrame = true
        )
        val confirmedDifferentFrame = coordinator.acceptCandidate(
            source = OfferCaptureSource.ACCESSIBILITY_TREE,
            candidate = secondOffer,
            trustedSingleFrame = true
        )

        assertTrue(firstDifferentFrame is CaptureDecision.HideStaleOverlay)
        assertEquals(
            secondOffer.overlayFingerprint(),
            (firstDifferentFrame as CaptureDecision.HideStaleOverlay).overlayFingerprint
        )
        assertTrue(confirmedDifferentFrame is CaptureDecision.ShowOverlay)
        assertEquals(
            secondOffer.overlayFingerprint(),
            (confirmedDifferentFrame as CaptureDecision.ShowOverlay).overlayFingerprint
        )
    }

    @Test
    fun `route change does not flicker when offer fingerprint is the same`() {
        val coordinator = CaptureCoordinator(requiredMatchingFrames = 2)
        val candidate = offer(price = 10.0)

        coordinator.acceptCandidate(OfferCaptureSource.ACCESSIBILITY_TREE, candidate)
        val decision = coordinator.acceptCandidate(OfferCaptureSource.UIAUTOMATOR_LAB, candidate)

        assertTrue(decision is CaptureDecision.ShowOverlay)
    }

    @Test
    fun `new offer hides stale overlay and waits for stable frame`() {
        val coordinator = CaptureCoordinator(requiredMatchingFrames = 2)
        val firstOffer = offer(price = 10.0)
        val secondOffer = offer(price = 12.0)

        coordinator.acceptCandidate(OfferCaptureSource.ACCESSIBILITY_TREE, firstOffer)
        coordinator.acceptCandidate(OfferCaptureSource.ACCESSIBILITY_TREE, firstOffer)
        val replacement = coordinator.acceptCandidate(OfferCaptureSource.ACCESSIBILITY_TREE, secondOffer)

        assertTrue(replacement is CaptureDecision.HideStaleOverlay)
    }

    @Test
    fun `single invalid frame keeps current overlay but second invalid hides it`() {
        val coordinator = CaptureCoordinator(requiredMatchingFrames = 2, requiredInvalidFramesToReset = 2)
        val candidate = offer(price = 10.0)

        coordinator.acceptCandidate(OfferCaptureSource.ACCESSIBILITY_TREE, candidate)
        coordinator.acceptCandidate(OfferCaptureSource.ACCESSIBILITY_TREE, candidate)

        val firstInvalid = coordinator.rejectFrame(
            OfferCaptureSource.ACCESSIBILITY_TREE,
            OfferCaptureRejectionReason.NOT_CARD_LIKE
        )
        val secondInvalid = coordinator.rejectFrame(
            OfferCaptureSource.ACCESSIBILITY_TREE,
            OfferCaptureRejectionReason.NOT_CARD_LIKE
        )

        assertTrue(firstInvalid is CaptureDecision.KeepCurrentOverlay)
        assertTrue(secondInvalid is CaptureDecision.HideOverlay)
    }

    @Test
    fun `strong invalid context hides overlay immediately without waiting for second frame`() {
        val coordinator = CaptureCoordinator(requiredMatchingFrames = 2, requiredInvalidFramesToReset = 2)
        val candidate = offer(price = 10.0)

        coordinator.acceptCandidate(OfferCaptureSource.ACCESSIBILITY_TREE, candidate, trustedSingleFrame = true)
        val invalidContext = coordinator.rejectFrame(
            OfferCaptureSource.ACCESSIBILITY_TREE,
            OfferCaptureRejectionReason.INVALID_CONTEXT_STILL_THERE
        )
        val nextSameCandidate = coordinator.acceptCandidate(
            OfferCaptureSource.ACCESSIBILITY_TREE,
            candidate,
            trustedSingleFrame = true
        )

        assertTrue(invalidContext is CaptureDecision.HideOverlay)
        assertEquals(
            OfferCaptureRejectionReason.INVALID_CONTEXT_STILL_THERE,
            (invalidContext as CaptureDecision.HideOverlay).reason
        )
        assertTrue(nextSameCandidate is CaptureDecision.ShowOverlay)
    }

    @Test
    fun `only strong invalid contexts hide overlay immediately without waiting for second frame`() {
        val strongInvalidContexts = listOf(
            OfferCaptureRejectionReason.INVALID_CONTEXT_STILL_THERE,
            OfferCaptureRejectionReason.INVALID_CONTEXT_REQUEST_UNAVAILABLE
        )
        val weakInvalidContexts = listOf(
            OfferCaptureRejectionReason.INVALID_CONTEXT_NO_REQUEST,
            OfferCaptureRejectionReason.INVALID_CONTEXT_OFFLINE
        )

        strongInvalidContexts.forEach { reason ->
            val coordinator = CaptureCoordinator(requiredMatchingFrames = 2, requiredInvalidFramesToReset = 2)
            val candidate = offer(price = 10.0)
            coordinator.acceptCandidate(OfferCaptureSource.ACCESSIBILITY_TREE, candidate, trustedSingleFrame = true)

            val decision = coordinator.rejectFrame(OfferCaptureSource.ACCESSIBILITY_TREE, reason)

            assertTrue("$reason must hide immediately", decision is CaptureDecision.HideOverlay)
            assertEquals(reason, (decision as CaptureDecision.HideOverlay).reason)
        }
        weakInvalidContexts.forEach { reason ->
            val coordinator = CaptureCoordinator(requiredMatchingFrames = 2, requiredInvalidFramesToReset = 2)
            val candidate = offer(price = 10.0)
            coordinator.acceptCandidate(OfferCaptureSource.ACCESSIBILITY_TREE, candidate, trustedSingleFrame = true)

            val decision = coordinator.rejectFrame(OfferCaptureSource.ACCESSIBILITY_TREE, reason)
            val repeatedDecision = coordinator.rejectFrame(OfferCaptureSource.ACCESSIBILITY_TREE, reason)

            assertTrue("$reason must not hide immediately", decision is CaptureDecision.KeepCurrentOverlay)
            assertEquals(reason, (decision as CaptureDecision.KeepCurrentOverlay).reason)
            assertTrue("$reason must not hide actionable overlay on repeat", repeatedDecision is CaptureDecision.KeepCurrentOverlay)
            assertEquals(reason, (repeatedDecision as CaptureDecision.KeepCurrentOverlay).reason)
        }
    }

    @Test
    fun `uiautomator lab can submit stable trip directly`() {
        val coordinator = CaptureCoordinator(requiredMatchingFrames = 2)
        val trip = offer(price = 21.0).toTripData()!!

        val decision = coordinator.acceptStableTrip(OfferCaptureSource.UIAUTOMATOR_LAB, trip)

        assertTrue(decision is CaptureDecision.ShowOverlay)
        assertEquals(OfferCaptureSource.UIAUTOMATOR_LAB, decision.source)
        assertEquals(trip.overlayFingerprint(), (decision as CaptureDecision.ShowOverlay).overlayFingerprint)
    }

    @Test
    fun `visible overlay expires when candidate is not reconfirmed`() {
        val coordinator = coordinatorWithClock(overlayTtlMillis = 1_200)
        val candidate = offer(price = 10.0)

        coordinator.acceptCandidate(OfferCaptureSource.ACCESSIBILITY_TREE, candidate, trustedSingleFrame = true)
        now += 1_199
        assertEquals(null, coordinator.expireOverlayIfStale())

        now += 1
        val decision = coordinator.expireOverlayIfStale()

        assertTrue(decision is CaptureDecision.HideOverlay)
    }

    @Test
    fun `same candidate renews overlay ttl`() {
        val coordinator = coordinatorWithClock(overlayTtlMillis = 1_200)
        val candidate = offer(price = 10.0)

        coordinator.acceptCandidate(OfferCaptureSource.ACCESSIBILITY_TREE, candidate, trustedSingleFrame = true)
        now += 1_000
        val renewed = coordinator.acceptCandidate(
            OfferCaptureSource.ACCESSIBILITY_TREE,
            candidate,
            trustedSingleFrame = true
        )
        now += 1_000

        assertTrue(renewed is CaptureDecision.RenewCurrentOverlay)
        assertEquals(null, coordinator.expireOverlayIfStale())
    }

    @Test
    fun `double tap suppresses same offer but allows different offer`() {
        val coordinator = coordinatorWithClock(userDismissSuppressMillis = 10_000)
        val candidate = offer(price = 10.0)
        val nextCandidate = offer(price = 12.0)

        coordinator.acceptCandidate(OfferCaptureSource.ACCESSIBILITY_TREE, candidate, trustedSingleFrame = true)
        coordinator.dismissCurrentOverlayByUser()

        val suppressed = coordinator.acceptCandidate(
            OfferCaptureSource.ACCESSIBILITY_TREE,
            candidate,
            trustedSingleFrame = true
        )
        val different = coordinator.acceptCandidate(
            OfferCaptureSource.ACCESSIBILITY_TREE,
            nextCandidate,
            trustedSingleFrame = true
        )

        assertTrue(suppressed is CaptureDecision.SuppressedCandidate)
        assertTrue(different is CaptureDecision.ShowOverlay)
    }

    @Test
    fun `double tap suppression expires`() {
        val coordinator = coordinatorWithClock(userDismissSuppressMillis = 10_000)
        val candidate = offer(price = 10.0)

        coordinator.acceptCandidate(OfferCaptureSource.ACCESSIBILITY_TREE, candidate, trustedSingleFrame = true)
        coordinator.dismissCurrentOverlayByUser()
        now += 10_000

        val decision = coordinator.acceptCandidate(
            OfferCaptureSource.ACCESSIBILITY_TREE,
            candidate,
            trustedSingleFrame = true
        )

        assertTrue(decision is CaptureDecision.ShowOverlay)
    }

    private fun offer(price: Double): OfferCandidate {
        return OfferCandidate(
            price = price,
            pickupDistanceKm = 1.0,
            pickupTimeMin = 4,
            tripDistanceKm = 5.0,
            tripTimeMin = 12
        )
    }

    private fun coordinatorWithClock(
        overlayTtlMillis: Long = 1_200,
        userDismissSuppressMillis: Long = 10_000
    ): CaptureCoordinator {
        now = 1_000
        return CaptureCoordinator(
            requiredMatchingFrames = 2,
            overlayTtlMillis = overlayTtlMillis,
            userDismissSuppressMillis = userDismissSuppressMillis,
            clockMillis = { now }
        )
    }
}
