package br.com.calcmot.finance.ledger

import kotlin.math.abs

data class AssociationEvidence(
    val platform: String,
    val observedAtMillis: Long,
    val fareCents: Long? = null,
    val category: String? = null,
    val durationSeconds: Int? = null,
    val distanceMeters: Long? = null
)

data class AssociationCandidate(
    val recordId: String,
    val platform: String,
    val occurredAtMillis: Long,
    val fareCents: Long? = null,
    val category: String? = null,
    val durationSeconds: Int? = null,
    val distanceMeters: Long? = null
)

sealed interface AssociationResult {
    data class Strong(val recordId: String, val score: Int) : AssociationResult
    data class Ambiguous(val candidateIds: List<String>, val topScore: Int) : AssociationResult
    data class Rejected(val reason: String) : AssociationResult
}

/**
 * Conservative matcher for future validated screen profiles.
 *
 * It never turns a disappearing offer into a trip. The caller must provide positive evidence
 * from a visible, supported platform screen before asking for an association.
 */
object LedgerAssociationEngine {
    private const val MAX_FORWARD_WINDOW_MILLIS = 3L * 60L * 60L * 1000L
    private const val STRONG_SCORE = 5
    private const val MIN_SCORE_GAP = 2

    fun associate(
        evidence: AssociationEvidence,
        candidates: List<AssociationCandidate>
    ): AssociationResult {
        val scored = candidates.asSequence()
            .filter { it.platform == evidence.platform }
            .filter { it.occurredAtMillis <= evidence.observedAtMillis }
            .filter { evidence.observedAtMillis - it.occurredAtMillis <= MAX_FORWARD_WINDOW_MILLIS }
            .map { it to score(evidence, it) }
            .filter { (_, score) -> score > 0 }
            .sortedByDescending { (_, score) -> score }
            .toList()

        val best = scored.firstOrNull()
            ?: return AssociationResult.Rejected("no_compatible_candidate")
        if (best.second < STRONG_SCORE) {
            return AssociationResult.Rejected("insufficient_evidence")
        }

        val runnerUp = scored.getOrNull(1)
        if (runnerUp != null && best.second - runnerUp.second < MIN_SCORE_GAP) {
            return AssociationResult.Ambiguous(
                candidateIds = scored.takeWhile { best.second - it.second < MIN_SCORE_GAP }
                    .map { it.first.recordId },
                topScore = best.second
            )
        }
        return AssociationResult.Strong(best.first.recordId, best.second)
    }

    private fun score(evidence: AssociationEvidence, candidate: AssociationCandidate): Int {
        var score = when (evidence.observedAtMillis - candidate.occurredAtMillis) {
            in 0L..20L * 60L * 1000L -> 2
            in 0L..60L * 60L * 1000L -> 1
            else -> 0
        }
        score += matchingScore(evidence.fareCents, candidate.fareCents, tolerance = 1L, weight = 3)
        score += matchingScore(evidence.durationSeconds, candidate.durationSeconds, tolerance = 120, weight = 2)
        score += matchingScore(evidence.distanceMeters, candidate.distanceMeters, tolerance = 500L, weight = 2)
        if (!evidence.category.isNullOrBlank() &&
            evidence.category.equals(candidate.category, ignoreCase = true)
        ) {
            score += 1
        }
        return score
    }

    private fun matchingScore(left: Long?, right: Long?, tolerance: Long, weight: Int): Int {
        if (left == null || right == null) return 0
        return if (abs(left - right) <= tolerance) weight else 0
    }

    private fun matchingScore(left: Int?, right: Int?, tolerance: Int, weight: Int): Int {
        if (left == null || right == null) return 0
        return if (abs(left - right) <= tolerance) weight else 0
    }
}
