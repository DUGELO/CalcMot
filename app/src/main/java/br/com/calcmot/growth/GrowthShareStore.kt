package br.com.calcmot.growth

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * One lightweight asynchronous write per successful local calendar day.
 * The overlay's UI thread only queues work, it never reads or writes preferences.
 */
internal object GrowthShareStore {
    private const val PREFS = "calcmot_growth_share_v1"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val usageQueue = GrowthShareUsageQueue(scope)

    fun recordOverlayShown(context: Context) {
        val today = GrowthSharePolicy.localDay(System.currentTimeMillis())
        val appContext = context.applicationContext
        // Enqueue once per day; the IO worker preserves ordering across midnight.
        // A failed disk write is retryable on the next displayed offer.
        usageQueue.enqueue(today) {
            synchronized(this@GrowthShareStore) {
                val current = read(appContext)
                val next = GrowthSharePolicy.onOverlayShown(current, today)
                next == current || save(appContext, next, durable = true)
            }
        }
    }

    @Synchronized
    fun claimPrompt(context: Context, notificationsAllowed: Boolean): GrowthShareSurface? {
        val now = System.currentTimeMillis()
        val state = read(context.applicationContext)
        if (!GrowthSharePolicy.eligible(state, now, GrowthSharePolicy.localDay(now))) return null
        val surface = GrowthSharePolicy.surface(state, notificationsAllowed)
        save(context.applicationContext, GrowthSharePolicy.shown(state, now))
        return surface
    }

    @Synchronized
    fun currentStreak(context: Context): Int {
        val now = System.currentTimeMillis()
        return GrowthSharePolicy.visibleStreak(
            read(context.applicationContext),
            GrowthSharePolicy.localDay(now)
        )
    }

    @Synchronized
    fun dismiss(context: Context) = change(context) {
        GrowthSharePolicy.dismissed(it, System.currentTimeMillis())
    }

    @Synchronized
    fun shared(context: Context) = change(context) {
        GrowthSharePolicy.shared(it, System.currentTimeMillis())
    }

    private fun change(context: Context, update: (GrowthShareState) -> GrowthShareState) {
        val appContext = context.applicationContext
        save(appContext, update(read(appContext)))
    }

    private fun read(context: Context): GrowthShareState {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return GrowthShareState(
            lastUsageDay = prefs.getLong("last_usage_day", -1L),
            consecutiveDays = prefs.getInt("consecutive_days", 0),
            previousPromptAt = prefs.getLong("previous_prompt", 0L),
            lastPromptAt = prefs.getLong("last_prompt", 0L),
            quietUntil = prefs.getLong("quiet_until", 0L),
            dismissals = prefs.getInt("dismissals", 0)
        )
    }

    private fun save(context: Context, state: GrowthShareState, durable: Boolean = false): Boolean {
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong("last_usage_day", state.lastUsageDay)
            .putInt("consecutive_days", state.consecutiveDays)
            .putLong("previous_prompt", state.previousPromptAt)
            .putLong("last_prompt", state.lastPromptAt)
            .putLong("quiet_until", state.quietUntil)
            .putInt("dismissals", state.dismissals)
        // Only called by the background worker for durable daily usage.
        // Other UI-driven writes retain the existing nonblocking apply() behavior.
        return if (durable) editor.commit() else {
            editor.apply()
            true
        }
    }
}
