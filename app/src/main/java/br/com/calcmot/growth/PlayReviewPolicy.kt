package br.com.calcmot.growth

/**
 * Engagement-driven local pacing for Google Play's independent and unpublished review quota.
 * An attempt is NOT evidence that a rating prompt appeared or that a review was submitted.
 */
internal data class PlayReviewState(
    val lastAttemptAt: Long = 0L,
    // Diagnostic count of API requests only; never a lifetime eligibility limit.
    val attempts: Int = 0
)

internal object PlayReviewPolicy {
    const val MIN_CONSECUTIVE_DAYS = 5
    const val HIGH_ENGAGEMENT_STREAK_DAYS = 10
    private const val DAY_MS = 86_400_000L
    private const val NORMAL_COOLDOWN_MS = 21L * DAY_MS
    private const val HIGH_ENGAGEMENT_COOLDOWN_MS = 14L * DAY_MS

    fun cooldownMillis(consecutiveDays: Int): Long =
        if (consecutiveDays >= HIGH_ENGAGEMENT_STREAK_DAYS) {
            HIGH_ENGAGEMENT_COOLDOWN_MS
        } else {
            NORMAL_COOLDOWN_MS
        }

    fun eligible(state: PlayReviewState, consecutiveDays: Int, nowMillis: Long): Boolean {
        if (consecutiveDays < MIN_CONSECUTIVE_DAYS || nowMillis <= 0L) return false
        if (state.lastAttemptAt == 0L) return true
        // Never allow clock rollback or a repeated request during the cooldown.
        if (nowMillis <= state.lastAttemptAt) return false
        return nowMillis - state.lastAttemptAt >= cooldownMillis(consecutiveDays)
    }

    fun attempted(state: PlayReviewState, nowMillis: Long): PlayReviewState =
        state.copy(lastAttemptAt = nowMillis, attempts = (state.attempts + 1).coerceAtLeast(1))
}
