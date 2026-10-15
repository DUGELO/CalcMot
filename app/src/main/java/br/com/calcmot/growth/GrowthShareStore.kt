package br.com.calcmot.growth

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/**
 * One lightweight asynchronous write per successful local calendar day.
 * The overlay's UI thread only queues work, it never reads or writes preferences.
 */
internal object GrowthShareStore {
    private const val PREFS = "calcmot_growth_share_v1"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queuedDay = AtomicLong(-1L)

    fun recordOverlayShown(context: Context) {
        val today = GrowthSharePolicy.localDay(System.currentTimeMillis())
        if (queuedDay.getAndSet(today) == today) return
        val appContext = context.applicationContext
        scope.launch {
            runCatching {
                synchronized(this@GrowthShareStore) {
                    val current = read(appContext)
                    val next = GrowthSharePolicy.onOverlayShown(current, today)
                    if (next != current) save(appContext, next)
                }
            }
        }
    }

    @Synchronized
    fun claimPrompt(context: Context): Boolean {
        val now = System.currentTimeMillis()
        val state = read(context.applicationContext)
        if (!GrowthSharePolicy.eligible(state, now, GrowthSharePolicy.localDay(now))) return false
        save(context.applicationContext, GrowthSharePolicy.shown(state, now))
        return true
    }

    @Synchronized
    fun dismiss(context: Context) = change(context) {
        GrowthSharePolicy.dismissed(it, System.currentTimeMillis())
    }

    @Synchronized
    fun shared(context: Context) = change(context) {
        GrowthSharePolicy.shared(it, System.currentTimeMillis())
    }

    @Synchronized
    fun optOut(context: Context) = change(context, GrowthSharePolicy::optOut)

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
            dismissals = prefs.getInt("dismissals", 0),
            optedOut = prefs.getBoolean("opted_out", false)
        )
    }

    private fun save(context: Context, state: GrowthShareState) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong("last_usage_day", state.lastUsageDay)
            .putInt("consecutive_days", state.consecutiveDays)
            .putLong("previous_prompt", state.previousPromptAt)
            .putLong("last_prompt", state.lastPromptAt)
            .putLong("quiet_until", state.quietUntil)
            .putInt("dismissals", state.dismissals)
            .putBoolean("opted_out", state.optedOut)
            .apply()
    }
}
