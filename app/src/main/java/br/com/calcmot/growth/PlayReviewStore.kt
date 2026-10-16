package br.com.calcmot.growth

import android.content.Context

/** Independent of the sharing prompts. Persists only an attempt date and count, locally. */
internal object PlayReviewStore {
    private const val PREFS = "calcmot_play_review_v1"

    @Synchronized
    fun claimAutomaticPrompt(context: Context, consecutiveDays: Int): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val state = PlayReviewState(
            lastAttemptAt = prefs.getLong("last_attempt_at", 0L),
            attempts = prefs.getInt("attempts", 0)
        )
        val now = System.currentTimeMillis()
        if (!PlayReviewPolicy.eligible(state, consecutiveDays, now)) return false
        val next = PlayReviewPolicy.attempted(state, now)
        prefs.edit()
            .putLong("last_attempt_at", next.lastAttemptAt)
            .putInt("attempts", next.attempts)
            .apply()
        return true
    }
}
