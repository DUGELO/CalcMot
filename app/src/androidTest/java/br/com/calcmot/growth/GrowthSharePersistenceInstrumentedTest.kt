package br.com.calcmot.growth

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Uses an isolated preference file: never touches a real driver's usage or settings. */
@RunWith(AndroidJUnit4::class)
class GrowthSharePersistenceInstrumentedTest {
    @Test
    fun `background worker durably stores consecutive days in Android preferences`() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences(
            "calcmot_growth_async_instrumentation_test", Context.MODE_PRIVATE
        )
        prefs.edit().clear().commit()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val queue = GrowthShareUsageQueue(scope)
            for (day in listOf(100L, 101L, 102L)) {
                queue.enqueue(day) {
                    val previousDay = prefs.getLong("last_day", -1L)
                    val previousStreak = prefs.getInt("streak", 0)
                    val nextStreak = if (day == previousDay + 1) previousStreak + 1 else 1
                    prefs.edit()
                        .putLong("last_day", day)
                        .putInt("streak", nextStreak)
                        .commit()
                }
            }
            withTimeout(10_000L) { queue.awaitIdle() }
            // Re-obtain the Android persistence handle after the worker has finished.
            val persisted = context.getSharedPreferences(
                "calcmot_growth_async_instrumentation_test", Context.MODE_PRIVATE
            )
            assertEquals(102L, persisted.getLong("last_day", -1L))
            assertEquals(3, persisted.getInt("streak", 0))
        } finally {
            scope.cancel()
            prefs.edit().clear().commit()
        }
    }
}
