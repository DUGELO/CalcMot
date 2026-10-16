package br.com.calcmot.growth

import java.util.TimeZone

/** Local-only organic growth gate. No identity, contacts, GPS or offer content. */
internal data class GrowthShareState(
    val lastUsageDay: Long = -1L,
    val consecutiveDays: Int = 0,
    val previousPromptAt: Long = 0L,
    val lastPromptAt: Long = 0L,
    val quietUntil: Long = 0L,
    val dismissals: Int = 0
)

internal enum class GrowthShareSurface { CARD, NOTIFICATION }

internal object GrowthSharePolicy {
    private const val DAY_MS = 86_400_000L
    private const val THIRTY_DAYS_MS = 30 * DAY_MS
    private const val FOURTEEN_DAYS_MS = 14 * DAY_MS
    private const val SIXTY_DAYS_MS = 60 * DAY_MS

    fun localDay(nowMillis: Long): Long =
        Math.floorDiv(nowMillis + TimeZone.getDefault().getOffset(nowMillis).toLong(), DAY_MS)

    fun onOverlayShown(state: GrowthShareState, today: Long): GrowthShareState = when {
        today <= state.lastUsageDay -> state
        today == state.lastUsageDay + 1L ->
            state.copy(lastUsageDay = today, consecutiveDays = (state.consecutiveDays + 1).coerceAtMost(365))
        else -> state.copy(lastUsageDay = today, consecutiveDays = 1)
    }

    fun visibleStreak(state: GrowthShareState, today: Long): Int =
        if (state.lastUsageDay in (today - 1L)..today) state.consecutiveDays else 0

    fun eligible(state: GrowthShareState, nowMillis: Long, today: Long): Boolean {
        if (visibleStreak(state, today) < 2) return false
        if (nowMillis < state.quietUntil) return false
        // At most two prompts in any rolling 30-day window.
        if (state.previousPromptAt > 0L && nowMillis >= state.previousPromptAt &&
            nowMillis - state.previousPromptAt < THIRTY_DAYS_MS
        ) return false
        return true
    }

    fun surface(state: GrowthShareState, notificationsAllowed: Boolean): GrowthShareSurface =
        if (state.lastPromptAt > 0L && notificationsAllowed) GrowthShareSurface.NOTIFICATION
        else GrowthShareSurface.CARD

    fun shown(state: GrowthShareState, nowMillis: Long): GrowthShareState =
        state.copy(
            previousPromptAt = state.lastPromptAt,
            lastPromptAt = nowMillis,
            quietUntil = nowMillis + if (state.lastPromptAt > 0L) SIXTY_DAYS_MS else FOURTEEN_DAYS_MS
        )

    fun dismissed(state: GrowthShareState, nowMillis: Long): GrowthShareState {
        val dismissals = state.dismissals + 1
        return state.copy(
            dismissals = dismissals,
            quietUntil = maxOf(state.quietUntil, nowMillis + if (dismissals >= 2) SIXTY_DAYS_MS else FOURTEEN_DAYS_MS)
        )
    }

    fun shared(state: GrowthShareState, nowMillis: Long): GrowthShareState =
        state.copy(dismissals = 0, quietUntil = maxOf(state.quietUntil, nowMillis + SIXTY_DAYS_MS))

}
