package br.com.calcmot.telemetry

import android.content.Context
import br.com.calcmot.AppSettings
import br.com.calcmot.DriverApp
import br.com.calcmot.model.OfferCandidate
import br.com.calcmot.model.OfferCaptureSource
import java.time.LocalDate
import java.util.ArrayDeque
import kotlin.math.abs
import kotlin.math.roundToLong

enum class MetricsStudyLedgerResult(val value: String) {
    NEW_RECORD("new_record"),
    DEDUPLICATED("deduplicated"),
    NOT_RECORDED("not_recorded")
}

internal enum class OfferEpisodeStudyStrategy(val value: String) {
    EXACT_MATCH("exact_match"),
    STRICT_60_SECONDS("strict_60"),
    STRICT_90_SECONDS("strict_90"),
    STRICT_180_SECONDS("strict_180"),
    NEW_OBSERVATION("new_observation")
}

/**
 * Compares deduplication hypotheses without changing the production ledger.
 * Signatures remain in memory only and never contain screen text or locations.
 */
internal class OfferEpisodeStudyEvaluator {
    private val recentByPlatform = mutableMapOf<String, ArrayDeque<OfferSignature>>()

    @Synchronized
    fun evaluate(
        platform: String,
        observedAtMillis: Long,
        candidate: OfferCandidate
    ): OfferEpisodeStudyStrategy {
        val recent = recentByPlatform.getOrPut(platform) { ArrayDeque() }
        while (recent.isNotEmpty() && !recent.first().isWithinStudyWindow(observedAtMillis)) {
            recent.removeFirst()
        }

        val current = OfferSignature.from(observedAtMillis, candidate)
        val previous = recent.toList().asReversed()
        val strategy = when {
            previous.any { current.isExactMatch(it, EXACT_MATCH_WINDOW_MILLIS) } ->
                OfferEpisodeStudyStrategy.EXACT_MATCH
            previous.any { current.isStrictMatch(it, SIXTY_SECONDS_MILLIS) } ->
                OfferEpisodeStudyStrategy.STRICT_60_SECONDS
            previous.any { current.isStrictMatch(it, NINETY_SECONDS_MILLIS) } ->
                OfferEpisodeStudyStrategy.STRICT_90_SECONDS
            previous.any { current.isStrictMatch(it, STUDY_WINDOW_MILLIS) } ->
                OfferEpisodeStudyStrategy.STRICT_180_SECONDS
            else -> OfferEpisodeStudyStrategy.NEW_OBSERVATION
        }

        recent.addLast(current)
        while (recent.size > MAX_IN_MEMORY_SIGNATURES_PER_PLATFORM) recent.removeFirst()
        return strategy
    }

    @Synchronized
    fun clear() {
        recentByPlatform.clear()
    }

    private data class OfferSignature(
        val observedAtMillis: Long,
        val fingerprint: String,
        val fareCents: Long,
        val pickupDistanceMeters: Long,
        val pickupTimeSeconds: Int,
        val tripDistanceMeters: Long,
        val tripTimeSeconds: Int,
        val passengerRatingMilli: Int?
    ) {
        fun isWithinStudyWindow(nowMillis: Long): Boolean {
            val age = nowMillis - observedAtMillis
            return age in 0..STUDY_WINDOW_MILLIS
        }

        fun isExactMatch(other: OfferSignature, windowMillis: Long): Boolean =
            fingerprint == other.fingerprint && isWithin(other, windowMillis)

        fun isStrictMatch(other: OfferSignature, windowMillis: Long): Boolean {
            if (!isWithin(other, windowMillis)) return false
            if (fareCents != other.fareCents) return false
            if (abs(pickupDistanceMeters - other.pickupDistanceMeters) > PICKUP_DISTANCE_TOLERANCE_METERS) return false
            if (abs(pickupTimeSeconds - other.pickupTimeSeconds) > PICKUP_TIME_TOLERANCE_SECONDS) return false
            if (abs(tripDistanceMeters - other.tripDistanceMeters) > TRIP_DISTANCE_TOLERANCE_METERS) return false
            if (abs(tripTimeSeconds - other.tripTimeSeconds) > TRIP_TIME_TOLERANCE_SECONDS) return false
            if (passengerRatingMilli != null && other.passengerRatingMilli != null &&
                passengerRatingMilli != other.passengerRatingMilli
            ) return false
            return true
        }

        private fun isWithin(other: OfferSignature, windowMillis: Long): Boolean {
            val age = observedAtMillis - other.observedAtMillis
            return age in 0..windowMillis
        }

        companion object {
            fun from(observedAtMillis: Long, candidate: OfferCandidate) = OfferSignature(
                observedAtMillis = observedAtMillis,
                fingerprint = candidate.fingerprint,
                fareCents = (candidate.price * 100.0).roundToLong(),
                pickupDistanceMeters = (candidate.pickupDistanceKm * 1_000.0).roundToLong(),
                pickupTimeSeconds = candidate.pickupTimeMin * 60,
                tripDistanceMeters = (candidate.tripDistanceKm * 1_000.0).roundToLong(),
                tripTimeSeconds = candidate.tripTimeMin * 60,
                passengerRatingMilli = candidate.passengerRating?.times(1_000.0)?.roundToLong()?.toInt()
            )
        }
    }

    private companion object {
        const val SIXTY_SECONDS_MILLIS = 60_000L
        const val NINETY_SECONDS_MILLIS = 90_000L
        const val EXACT_MATCH_WINDOW_MILLIS = 120_000L
        const val STUDY_WINDOW_MILLIS = 180_000L
        const val MAX_IN_MEMORY_SIGNATURES_PER_PLATFORM = 32
        const val PICKUP_DISTANCE_TOLERANCE_METERS = 400L
        const val PICKUP_TIME_TOLERANCE_SECONDS = 120
        const val TRIP_DISTANCE_TOLERANCE_METERS = 200L
        const val TRIP_TIME_TOLERANCE_SECONDS = 60
    }
}

class MetricsResearchRecorder internal constructor(
    context: Context,
    private val analytics: AnalyticsTracker = TelemetryProvider.analytics,
    private val evaluator: OfferEpisodeStudyEvaluator = OfferEpisodeStudyEvaluator(),
    private val currentEpochDay: () -> Long = { LocalDate.now().toEpochDay() }
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun recordOfferObservation(
        driverApp: DriverApp,
        source: OfferCaptureSource,
        candidate: OfferCandidate,
        observedAtMillis: Long,
        ledgerResult: MetricsStudyLedgerResult
    ) {
        if (!AppSettings.isMetricsResearchEnabled(appContext)) return
        if (driverApp !in DriverApp.supported || source == OfferCaptureSource.UIAUTOMATOR_LAB) return

        val strategy = evaluator.evaluate(driverApp.id, observedAtMillis, candidate)
        recordDailyCounter(
            StudyCounter(
                platform = driverApp.id,
                source = source.id,
                strategy = strategy.value,
                ledgerResult = ledgerResult.value
            )
        )
    }

    fun onConsentChanged(enabled: Boolean) {
        evaluator.clear()
        if (!enabled) {
            prefs.edit().clear().commit()
        } else {
            prefs.edit().putLong(KEY_EPOCH_DAY, currentEpochDay()).commit()
        }
        if (enabled) {
            analytics.track(
                AnalyticsEvents.METRICS_STUDY_CONSENT,
                mapOf(
                    AnalyticsParams.SOURCE to AnalyticsValues.SOURCE_SYSTEM,
                    AnalyticsParams.REASON to AnalyticsValues.STATUS_ACTIVE
                )
            )
        }
    }

    @Synchronized
    private fun recordDailyCounter(counter: StudyCounter) {
        val today = currentEpochDay()
        val storedDay = if (prefs.contains(KEY_EPOCH_DAY)) {
            prefs.getLong(KEY_EPOCH_DAY, today)
        } else {
            prefs.edit().putLong(KEY_EPOCH_DAY, today).commit()
            today
        }
        if (storedDay != today) flushAndReset(today)

        val key = counter.storageKey()
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply()
    }

    private fun flushAndReset(today: Long) {
        val summaries = prefs.all.mapNotNull { (key, value) ->
            StudyCounter.fromStorageKey(key)?.let { counter ->
                val count = value as? Int ?: return@let null
                counter to count
            }
        }

        // Remove local counters before dispatch so a process restart cannot resend a day.
        prefs.edit().clear().putLong(KEY_EPOCH_DAY, today).commit()
        summaries.forEach { (counter, count) ->
            analytics.track(
                AnalyticsEvents.METRICS_STUDY_DAILY_SUMMARY,
                mapOf(
                    AnalyticsParams.PLATFORM to counter.platform,
                    AnalyticsParams.SOURCE to counter.source,
                    AnalyticsParams.STUDY_VERSION to STUDY_VERSION,
                    AnalyticsParams.STUDY_STRATEGY to counter.strategy,
                    AnalyticsParams.STUDY_LEDGER_RESULT to counter.ledgerResult,
                    AnalyticsParams.SAMPLE_BUCKET to sampleBucket(count)
                )
            )
        }
    }

    private data class StudyCounter(
        val platform: String,
        val source: String,
        val strategy: String,
        val ledgerResult: String
    ) {
        fun storageKey(): String = listOf(COUNTER_PREFIX, platform, source, strategy, ledgerResult)
            .joinToString(SEPARATOR)

        companion object {
            fun fromStorageKey(value: String): StudyCounter? {
                val parts = value.split(SEPARATOR)
                if (parts.size != 5 || parts.first() != COUNTER_PREFIX) return null
                return StudyCounter(parts[1], parts[2], parts[3], parts[4])
            }
        }
    }

    internal companion object {
        const val PREFS_NAME = "calcmot_metrics_research"
        const val STUDY_VERSION = "offer_episode_v1"
        private const val KEY_EPOCH_DAY = "epoch_day"
        private const val COUNTER_PREFIX = "counter"
        private const val SEPARATOR = ":"

        fun sampleBucket(count: Int): String = when (count) {
            1 -> "1"
            in 2..5 -> "2_to_5"
            in 6..20 -> "6_to_20"
            else -> "21_plus"
        }
    }
}

object MetricsResearchProvider {
    @Volatile private var recorder: MetricsResearchRecorder? = null

    fun get(context: Context): MetricsResearchRecorder = recorder ?: synchronized(this) {
        recorder ?: MetricsResearchRecorder(context.applicationContext).also { recorder = it }
    }

    fun updateConsent(context: Context, enabled: Boolean) {
        AppSettings.setMetricsResearchEnabled(context, enabled)
        get(context).onConsentChanged(enabled)
    }
}
