package br.com.calcmot.finance.ledger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LedgerAssociationEngineTest {
    @Test
    fun `strong evidence links the matching record`() {
        val result = LedgerAssociationEngine.associate(
            evidence = evidence(),
            candidates = listOf(candidate("match"), candidate("old", occurredAtMillis = -3_000_000L))
        )

        assertTrue(result is AssociationResult.Strong)
        assertEquals("match", (result as AssociationResult.Strong).recordId)
    }

    @Test
    fun `equivalent candidates stay ambiguous`() {
        val result = LedgerAssociationEngine.associate(
            evidence = evidence(),
            candidates = listOf(candidate("one"), candidate("two"))
        )

        assertTrue(result is AssociationResult.Ambiguous)
        assertEquals(setOf("one", "two"), (result as AssociationResult.Ambiguous).candidateIds.toSet())
    }

    @Test
    fun `different platform and weak evidence are rejected`() {
        val wrongPlatform = candidate("99").copy(platform = "99")
        val weak = candidate("weak").copy(fareCents = 900L, durationSeconds = null, distanceMeters = null)

        assertTrue(LedgerAssociationEngine.associate(evidence(), listOf(wrongPlatform)) is AssociationResult.Rejected)
        assertTrue(LedgerAssociationEngine.associate(evidence(), listOf(weak)) is AssociationResult.Rejected)
    }

    private fun evidence() = AssociationEvidence(
        platform = "uber",
        observedAtMillis = 1_000_000L,
        fareCents = 1_105L,
        category = "UberX",
        durationSeconds = 840,
        distanceMeters = 5_000L
    )

    private fun candidate(id: String, occurredAtMillis: Long = 950_000L) = AssociationCandidate(
        recordId = id,
        platform = "uber",
        occurredAtMillis = occurredAtMillis,
        fareCents = 1_105L,
        category = "UberX",
        durationSeconds = 840,
        distanceMeters = 5_000L
    )
}
