package br.com.calcmot.finance.ledger

import java.time.Instant
import java.time.ZoneId

data class HourlyPerformance(
    val hourOfDay: Int,
    val recordCount: Int,
    val averageValuePerKm: Double,
    val medianValuePerKm: Double,
    val averageValuePerHour: Double,
    val medianValuePerHour: Double
)

data class FinancialAnalysis(
    val eligibleHourlyBuckets: List<HourlyPerformance>,
    val bestHourByKm: HourlyPerformance?,
    val worstHourByKm: HourlyPerformance?,
    val bestHourByHour: HourlyPerformance?,
    val worstHourByHour: HourlyPerformance?
)

object FinancialAnalytics {
    const val MIN_RECORDS_PER_HOUR = 3

    fun analyze(
        offers: List<OfferRecordEntity>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): FinancialAnalysis {
        val official = offers.filter {
            it.confidence == LedgerConfidence.CONFIRMED.name ||
                it.confidence == LedgerConfidence.DRIVER_CORRECTED.name
        }
        val hourly = official.groupBy {
            Instant.ofEpochMilli(it.observedAtMillis).atZone(zoneId).hour
        }.mapNotNull { (hour, records) ->
            if (records.size < MIN_RECORDS_PER_HOUR) return@mapNotNull null
            val perKm = records.mapNotNull { it.valuePerKm() }
            val perHour = records.mapNotNull { it.valuePerHour() }
            if (perKm.size < MIN_RECORDS_PER_HOUR || perHour.size < MIN_RECORDS_PER_HOUR) return@mapNotNull null
            HourlyPerformance(
                hourOfDay = hour,
                recordCount = records.size,
                averageValuePerKm = perKm.average(),
                medianValuePerKm = perKm.median(),
                averageValuePerHour = perHour.average(),
                medianValuePerHour = perHour.median()
            )
        }.sortedBy { it.hourOfDay }

        return FinancialAnalysis(
            eligibleHourlyBuckets = hourly,
            bestHourByKm = hourly.maxByOrNull { it.medianValuePerKm },
            worstHourByKm = hourly.minByOrNull { it.medianValuePerKm },
            bestHourByHour = hourly.maxByOrNull { it.medianValuePerHour },
            worstHourByHour = hourly.minByOrNull { it.medianValuePerHour }
        )
    }

    private fun OfferRecordEntity.valuePerKm(): Double? {
        val km = (pickupDistanceMeters + tripDistanceMeters) / 1000.0
        return if (km > 0.0) fareCents / 100.0 / km else null
    }

    private fun OfferRecordEntity.valuePerHour(): Double? {
        val hours = (pickupTimeSeconds + tripTimeSeconds) / 3600.0
        return if (hours > 0.0) fareCents / 100.0 / hours else null
    }

    private fun List<Double>.median(): Double {
        val sorted = sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) (sorted[middle - 1] + sorted[middle]) / 2.0 else sorted[middle]
    }
}
