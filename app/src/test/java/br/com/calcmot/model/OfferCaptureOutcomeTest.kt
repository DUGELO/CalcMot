package br.com.calcmot.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfferCaptureOutcomeTest {

    @Test
    fun `accepted candidate exposes source and fingerprint`() {
        val candidate = OfferCandidate(
            price = 12.35,
            pickupDistanceKm = 1.8,
            pickupTimeMin = 5,
            tripDistanceKm = 8.5,
            tripTimeMin = 12
        )

        val outcome = OfferCaptureOutcome.accepted(
            source = OfferCaptureSource.ACCESSIBILITY_TREE,
            candidate = candidate
        )

        assertTrue(outcome.isComplete)
        assertEquals(OfferCaptureSource.ACCESSIBILITY_TREE, outcome.source)
        assertEquals(candidate.fingerprint, outcome.fingerprint)
        assertNull(outcome.rejectionReason)
    }

    @Test
    fun `rejected outcome carries safe reason without candidate data`() {
        val outcome = OfferCaptureOutcome.rejected(
            source = OfferCaptureSource.ACCESSIBILITY_TREE,
            reason = OfferCaptureRejectionReason.NOT_CARD_LIKE
        )

        assertFalse(outcome.isComplete)
        assertEquals(OfferCaptureSource.ACCESSIBILITY_TREE, outcome.source)
        assertEquals(OfferCaptureRejectionReason.NOT_CARD_LIKE, outcome.rejectionReason)
        assertNull(outcome.fingerprint)
        assertNull(outcome.candidate)
        assertNull(outcome.tripData)
    }

    @Test
    fun `source and rejection ids round trip for oracle reports`() {
        assertEquals(
            OfferCaptureSource.UIAUTOMATOR_LAB,
            OfferCaptureSource.fromId("uiautomator_lab")
        )
        assertEquals(
            OfferCaptureRejectionReason.INCOMPLETE_TIME_DISTANCE_BLOCKS,
            OfferCaptureRejectionReason.fromId("incomplete_time_distance_blocks")
        )
        assertNull(OfferCaptureSource.fromId("unknown_source"))
        assertNull(OfferCaptureRejectionReason.fromId("unknown_reason"))
    }
}
