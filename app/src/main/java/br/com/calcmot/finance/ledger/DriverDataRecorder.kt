package br.com.calcmot.finance.ledger

import android.content.Context
import br.com.calcmot.AppSettings
import br.com.calcmot.DriverApp
import br.com.calcmot.model.FinancialImpactCalculator
import br.com.calcmot.model.OfferCaptureSource
import br.com.calcmot.model.OfferCandidate
import br.com.calcmot.telemetry.AnalyticsEvents
import br.com.calcmot.telemetry.AnalyticsParams
import br.com.calcmot.telemetry.MetricsResearchProvider
import br.com.calcmot.telemetry.MetricsStudyLedgerResult
import br.com.calcmot.telemetry.TelemetryProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.roundToLong

interface DriverDataRecorder {
    fun recordStableOffer(
        driverApp: DriverApp,
        source: OfferCaptureSource,
        candidate: OfferCandidate,
        observedAtMillis: Long = System.currentTimeMillis()
    )
}

class LocalDriverDataRecorder(
    private val context: Context,
    private val repository: FinancialLedgerRepository = FinanceLedgerProvider.repository(context),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : DriverDataRecorder {
    private val writeMutex = Mutex()

    override fun recordStableOffer(
        driverApp: DriverApp,
        source: OfferCaptureSource,
        candidate: OfferCandidate,
        observedAtMillis: Long
    ) {
        if (driverApp !in DriverApp.supported || source == OfferCaptureSource.UIAUTOMATOR_LAB) return
        val financialHistoryEnabled = AppSettings.isFinancialHistoryEnabled(context)
        val metricsResearchEnabled = AppSettings.isMetricsResearchEnabled(context)
        if (!financialHistoryEnabled && !metricsResearchEnabled) return

        scope.launch {
            writeMutex.withLock {
                runCatching {
                    val tripData = candidate.toTripData() ?: return@runCatching
                    val goal = AppSettings.getDriverGoal(context)
                    val impact = FinancialImpactCalculator.calculate(tripData, goal) ?: return@runCatching
                    if (!financialHistoryEnabled) {
                        MetricsResearchProvider.get(context).recordOfferObservation(
                            driverApp = driverApp,
                            source = source,
                            candidate = candidate,
                            observedAtMillis = observedAtMillis,
                            ledgerResult = MetricsStudyLedgerResult.NOT_RECORDED
                        )
                        return@runCatching
                    }
                    val inserted = repository.recordStableOffer(
                        StableOfferObservation(
                            platform = driverApp.id,
                            source = when (driverApp) {
                                DriverApp.UBER -> LedgerSource.UBER_ACCESSIBILITY
                                DriverApp.NINETY_NINE -> LedgerSource.NINETY_NINE_VISUAL
                                DriverApp.UNKNOWN -> return@runCatching
                            },
                            observedAtMillis = observedAtMillis,
                            fingerprint = candidate.fingerprint,
                            fareCents = (candidate.price * 100.0).roundToLong(),
                            pickupDistanceMeters = (candidate.pickupDistanceKm * 1_000.0).roundToLong(),
                            pickupTimeSeconds = candidate.pickupTimeMin * 60,
                            tripDistanceMeters = (candidate.tripDistanceKm * 1_000.0).roundToLong(),
                            tripTimeSeconds = candidate.tripTimeMin * 60,
                            passengerRatingMilli = candidate.passengerRating?.times(1_000.0)?.roundToLong()?.toInt(),
                            classification = impact.classification.name,
                            classificationReason = impact.subtext,
                            goalPerKmMicros = (goal.minValuePerKm * 1_000_000.0).roundToLong(),
                            goalPerHourCents = (goal.minValuePerHour * 100.0).roundToLong(),
                            goalMode = goal.mode.name
                        )
                    )
                    if (metricsResearchEnabled) {
                        MetricsResearchProvider.get(context).recordOfferObservation(
                            driverApp = driverApp,
                            source = source,
                            candidate = candidate,
                            observedAtMillis = observedAtMillis,
                            ledgerResult = if (inserted) {
                                MetricsStudyLedgerResult.NEW_RECORD
                            } else {
                                MetricsStudyLedgerResult.DEDUPLICATED
                            }
                        )
                    }
                    if (inserted) {
                        TelemetryProvider.analytics.track(
                            AnalyticsEvents.FINANCIAL_RECORD_CREATED,
                            mapOf(
                                AnalyticsParams.PLATFORM to driverApp.id,
                                AnalyticsParams.SOURCE to source.id,
                                AnalyticsParams.RECORD_TYPE to "offer"
                            )
                        )
                    }
                }.onFailure { error ->
                    TelemetryProvider.crashReporter.recordNonFatal(
                        error = error,
                        reason = "financial_ledger_offer_write",
                        params = mapOf(
                            AnalyticsParams.PLATFORM to driverApp.id,
                            AnalyticsParams.SOURCE to source.id
                        )
                    )
                }
            }
        }
    }
}

object DriverDataRecorderProvider {
    @Volatile private var recorder: DriverDataRecorder? = null

    fun get(context: Context): DriverDataRecorder = recorder ?: synchronized(this) {
        recorder ?: LocalDriverDataRecorder(context.applicationContext).also { recorder = it }
    }
}
