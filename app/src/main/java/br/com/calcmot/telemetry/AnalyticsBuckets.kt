package br.com.calcmot.telemetry

import br.com.calcmot.model.OfferCandidate
import br.com.calcmot.model.TripData

object AnalyticsBuckets {
    fun from(candidate: OfferCandidate): Map<String, String> {
        return values(
            price = candidate.price,
            distanceKm = candidate.totalDistanceKm,
            durationMinutes = candidate.totalTimeMin,
            valuePerKm = candidate.price / candidate.totalDistanceKm,
            valuePerHour = candidate.price / (candidate.totalTimeMin / 60.0)
        )
    }

    fun from(tripData: TripData): Map<String, String> {
        return values(
            price = tripData.valor,
            distanceKm = tripData.distanciaKm,
            durationMinutes = tripData.minutosTotais,
            valuePerKm = tripData.valorPorKm,
            valuePerHour = tripData.valorPorHora
        )
    }

    internal fun values(
        price: Double,
        distanceKm: Double,
        durationMinutes: Int,
        valuePerKm: Double,
        valuePerHour: Double
    ): Map<String, String> = mapOf(
        AnalyticsParams.PRICE_BUCKET to priceBucket(price),
        AnalyticsParams.KM_BUCKET to distanceBucket(distanceKm),
        AnalyticsParams.DURATION_BUCKET to durationBucket(durationMinutes),
        AnalyticsParams.VALUE_PER_KM_BUCKET to valuePerKmBucket(valuePerKm),
        AnalyticsParams.VALUE_PER_HOUR_BUCKET to valuePerHourBucket(valuePerHour)
    )

    private fun priceBucket(value: Double): String = when {
        value < 10.0 -> "under_10"
        value < 20.0 -> "10_to_19"
        value < 40.0 -> "20_to_39"
        else -> "40_plus"
    }

    private fun distanceBucket(value: Double): String = when {
        value < 3.0 -> "under_3"
        value < 8.0 -> "3_to_7"
        value < 15.0 -> "8_to_14"
        else -> "15_plus"
    }

    private fun durationBucket(value: Int): String = when {
        value < 10 -> "under_10"
        value < 20 -> "10_to_19"
        value < 40 -> "20_to_39"
        else -> "40_plus"
    }

    private fun valuePerKmBucket(value: Double): String = when {
        value < 1.40 -> "under_1_40"
        value < 1.75 -> "1_40_to_1_74"
        value < 2.50 -> "1_75_to_2_49"
        else -> "2_50_plus"
    }

    private fun valuePerHourBucket(value: Double): String = when {
        value < 25.0 -> "under_25"
        value < 35.0 -> "25_to_34"
        value < 50.0 -> "35_to_49"
        else -> "50_plus"
    }
}
