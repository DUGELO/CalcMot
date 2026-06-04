package br.com.calcmot.model

data class ProfitabilitySettings(
    val vehicleEfficiencyKmPerUnit: Double = DEFAULT_VEHICLE_EFFICIENCY_KM_PER_UNIT,
    val inputPricePerUnit: Double = DEFAULT_INPUT_PRICE_PER_UNIT,
    val maintenanceCostPerKm: Double = DEFAULT_MAINTENANCE_COST_PER_KM,
    val goodNetPerKm: Double = DEFAULT_GOOD_NET_PER_KM,
    val mediumNetPerKm: Double = DEFAULT_MEDIUM_NET_PER_KM,
    val minimumNetPerHour: Double = DEFAULT_MINIMUM_NET_PER_HOUR
) {
    val operatingCostPerKm: Double
        get() {
            val fuelCost = if (vehicleEfficiencyKmPerUnit > 0.0 && inputPricePerUnit > 0.0) {
                inputPricePerUnit / vehicleEfficiencyKmPerUnit
            } else {
                0.0
            }
            return fuelCost + maintenanceCostPerKm.coerceAtLeast(0.0)
        }

    fun normalized(): ProfitabilitySettings {
        return copy(
            vehicleEfficiencyKmPerUnit = vehicleEfficiencyKmPerUnit.coerceAtLeast(0.0),
            inputPricePerUnit = inputPricePerUnit.coerceAtLeast(0.0),
            maintenanceCostPerKm = maintenanceCostPerKm.coerceAtLeast(0.0),
            goodNetPerKm = goodNetPerKm.coerceAtLeast(0.0),
            mediumNetPerKm = mediumNetPerKm.coerceAtLeast(0.0),
            minimumNetPerHour = minimumNetPerHour.coerceAtLeast(0.0)
        )
    }

    companion object {
        const val DEFAULT_VEHICLE_EFFICIENCY_KM_PER_UNIT = 0.0
        const val DEFAULT_INPUT_PRICE_PER_UNIT = 0.0
        const val DEFAULT_MAINTENANCE_COST_PER_KM = 0.0
        const val DEFAULT_GOOD_NET_PER_KM = 2.5
        const val DEFAULT_MEDIUM_NET_PER_KM = 1.8
        const val DEFAULT_MINIMUM_NET_PER_HOUR = 0.0
    }
}

data class ProfitabilityResult(
    val grossRevenue: Double,
    val totalDistanceKm: Double,
    val totalTimeMin: Int,
    val operatingCostPerKm: Double,
    val tripCost: Double,
    val netProfit: Double,
    val netPerKm: Double,
    val netPerHour: Double,
    val quality: ProfitabilityQuality
)

enum class ProfitabilityQuality {
    GOOD,
    MEDIUM,
    BAD
}

object ProfitabilityCalculator {
    fun calculate(
        tripData: TripData,
        settings: ProfitabilitySettings
    ): ProfitabilityResult? {
        if (tripData.valor <= 0.0 || tripData.distanciaKm <= 0.0 || tripData.minutosTotais <= 0) {
            return null
        }

        val normalizedSettings = settings.normalized()
        val operatingCostPerKm = normalizedSettings.operatingCostPerKm
        val tripCost = tripData.distanciaKm * operatingCostPerKm
        val netProfit = tripData.valor - tripCost
        val netPerKm = netProfit / tripData.distanciaKm
        val netPerHour = netProfit / (tripData.minutosTotais / 60.0)
        val quality = classify(
            netPerKm = netPerKm,
            netPerHour = netPerHour,
            settings = normalizedSettings
        )

        return ProfitabilityResult(
            grossRevenue = tripData.valor,
            totalDistanceKm = tripData.distanciaKm,
            totalTimeMin = tripData.minutosTotais,
            operatingCostPerKm = operatingCostPerKm,
            tripCost = tripCost,
            netProfit = netProfit,
            netPerKm = netPerKm,
            netPerHour = netPerHour,
            quality = quality
        )
    }

    fun classify(
        netPerKm: Double,
        netPerHour: Double,
        settings: ProfitabilitySettings
    ): ProfitabilityQuality {
        val normalizedSettings = settings.normalized()
        val reachesHourGoal = normalizedSettings.minimumNetPerHour <= 0.0 ||
            netPerHour >= normalizedSettings.minimumNetPerHour

        return when {
            netPerKm >= normalizedSettings.goodNetPerKm && reachesHourGoal -> ProfitabilityQuality.GOOD
            netPerKm >= normalizedSettings.mediumNetPerKm -> ProfitabilityQuality.MEDIUM
            else -> ProfitabilityQuality.BAD
        }
    }
}
