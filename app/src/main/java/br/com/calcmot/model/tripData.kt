package br.com.calcmot.model

data class TripData(
    val valor: Double,
    val distanciaKm: Double,
    val minutosTotais: Int,
    val valorPorKm: Double,
    val valorPorHora: Double,
    val nota: Double? = null
)

data class OfferCandidate(
    val price: Double,
    val pickupDistanceKm: Double,
    val pickupTimeMin: Int,
    val tripDistanceKm: Double,
    val tripTimeMin: Int,
    val passengerRating: Double? = null
) {
    val totalDistanceKm: Double
        get() = pickupDistanceKm + tripDistanceKm

    val totalTimeMin: Int
        get() = pickupTimeMin + tripTimeMin

    val fingerprint: String
        get() = "%.2f|%.1f|%d|%.1f|%d".format(
            java.util.Locale.US,
            price,
            pickupDistanceKm,
            pickupTimeMin,
            tripDistanceKm,
            tripTimeMin
        )

    fun toTripData(): TripData? {
        if (price <= 0.0 || totalDistanceKm <= 0.0 || totalTimeMin <= 0) return null

        val valorPorKm = price / totalDistanceKm
        val valorPorHora = price / (totalTimeMin / 60.0)

        if (!isSaneTripMetric(valorPorKm, valorPorHora)) return null

        return TripData(
            valor = price,
            distanciaKm = totalDistanceKm,
            minutosTotais = totalTimeMin,
            valorPorKm = valorPorKm,
            valorPorHora = valorPorHora,
            nota = passengerRating
        )
    }

    private fun isSaneTripMetric(valorPorKm: Double, valorPorHora: Double): Boolean {
        return totalDistanceKm <= MAX_TOTAL_DISTANCE_KM &&
            totalTimeMin <= MAX_TOTAL_TIME_MIN &&
            valorPorKm <= MAX_VALUE_PER_KM &&
            valorPorHora <= MAX_VALUE_PER_HOUR
    }

    private companion object {
        const val MAX_TOTAL_DISTANCE_KM = 500.0
        const val MAX_TOTAL_TIME_MIN = 12 * 60
        const val MAX_VALUE_PER_KM = 30.0
        const val MAX_VALUE_PER_HOUR = 500.0
    }
}
