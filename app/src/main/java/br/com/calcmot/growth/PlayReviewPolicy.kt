package br.com.calcmot.growth

/**
 * A conservative local gate around the Play Store's separate, undocumented review quota.
 * An attempt is not evidence that a dialog appeared or a review was submitted.
 */
internal data class PlayReviewState(
    val lastAttemptAt: Long = 0L,
    val attempts: Int = 0
)

internal object PlayReviewPolicy {
    const val MIN_CONSECUTIVE_DAYS = 5
    const val MAX_LIFETIME_ATTEMPTS = 2
    private const val COOLDOWN_MS = 120L * 86_400_000L

    fun eligible(state: PlayReviewState, consecutiveDays: Int, nowMillis: Long): Boolean =
        consecutiveDays >= MIN_CONSECUTIVE_DAYS &&
            state.attempts < MAX_LIFETIME_ATTEMPTS &&
            (state.lastAttemptAt == 0L ||
                (nowMillis > state.lastAttemptAt && nowMillis - state.lastAttemptAt >= COOLDOWN_MS))

    fun attempted(state: PlayReviewState, nowMillis: Long): PlayReviewState =
        state.copy(lastAttemptAt = nowMillis, attempts = state.attempts + 1)
}
