package br.com.calcmot.processor

import br.com.calcmot.model.OfferCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class OfferStabilityGateTest {

    @Test
    fun `same candidate on second frame returns trip data`() {
        val gate = OfferStabilityGate(requiredMatchingFrames = 2)
        val candidate = candidate()

        assertNull(gate.accept(candidate))
        val result = gate.accept(candidate)

        assertNotNull(result)
        assertEquals(10.0, result!!.valor, 0.01)
    }

    @Test
    fun `different candidate resets matching count`() {
        val gate = OfferStabilityGate(requiredMatchingFrames = 2)
        val first = candidate(price = 10.0)
        val second = candidate(price = 12.0)

        assertNull(gate.accept(first))
        assertNull(gate.accept(second))
        assertNotNull(gate.accept(second))
    }

    @Test
    fun `invalid frame resets previous stable offer`() {
        val gate = OfferStabilityGate(requiredMatchingFrames = 2)
        val candidate = candidate()

        assertNull(gate.accept(candidate))
        assertNotNull(gate.accept(candidate))
        assertNull(gate.accept(null))
        assertNull(gate.accept(candidate))
        assertNotNull(gate.accept(candidate))
    }

    @Test
    fun `gate never returns previous offer after a rejected frame`() {
        val gate = OfferStabilityGate(requiredMatchingFrames = 2)
        val first = candidate(price = 10.0)
        val second = candidate(price = 20.0)

        assertNull(gate.accept(first))
        assertNotNull(gate.accept(first))
        assertNull(gate.accept(null))
        assertNull(gate.accept(second))
        val result = gate.accept(second)

        assertNotNull(result)
        assertEquals(20.0, result!!.valor, 0.01)
    }

    private fun candidate(price: Double = 10.0): OfferCandidate {
        return OfferCandidate(
            price = price,
            pickupDistanceKm = 1.0,
            pickupTimeMin = 5,
            tripDistanceKm = 4.0,
            tripTimeMin = 15
        )
    }
}
