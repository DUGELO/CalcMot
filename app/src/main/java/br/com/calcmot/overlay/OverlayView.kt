package br.com.calcmot.overlay

import androidx.compose.runtime.Composable
import br.com.calcmot.model.OfferFinancialImpact
import br.com.calcmot.model.ProfitabilityCalculator
import br.com.calcmot.model.ProfitabilityQuality
import br.com.calcmot.model.ProfitabilityResult
import br.com.calcmot.model.ProfitabilitySettings
import br.com.calcmot.model.TripData
import java.util.Locale

@Composable
fun OverlayView(
    tripData: TripData,
    profitability: ProfitabilityResult? = null,
    financialImpact: OfferFinancialImpact? = null
) {
    val result = profitability ?: ProfitabilityCalculator.calculate(tripData, ProfitabilitySettings())
    val quality = resolveOfferQuality(result, financialImpact)
    val perKm = result?.netPerKm ?: tripData.valorPorKm
    val perHour = result?.netPerHour ?: tripData.valorPorHora

    CalcMotOverlayContainer(
        quality = quality
    ) {
        OfferQualityBadge(quality = quality)
        OverlayMetricSummary(
            perKm = brCurrencyPerUnit(perKm, "km"),
            perHour = brCurrencyPerUnit(perHour, "h"),
            duration = "${tripData.minutosTotais} min",
            quality = quality
        )
        financialImpact?.let {
            FinancialImpactBlock(
                impact = it,
                quality = quality
            )
        }
    }
}

fun resolveOfferQuality(
    profitability: ProfitabilityResult?,
    financialImpact: OfferFinancialImpact?
): OverlayOfferQuality {
    financialImpact?.classification?.let {
        return OverlayOfferQuality.fromClassification(it)
    }
    return getTripQuality(profitability).toOverlayOfferQuality()
}

private fun brCurrencyPerUnit(value: Double, unit: String): String {
    return String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f/%s", value, unit)
}

enum class TripQuality(val text: String) {
    GOOD("BOA"),
    MEDIUM("ATENÇÃO"),
    BAD("RUIM");

    fun toOverlayOfferQuality(): OverlayOfferQuality {
        return when (this) {
            GOOD -> OverlayOfferQuality.GOOD
            MEDIUM -> OverlayOfferQuality.WARNING
            BAD -> OverlayOfferQuality.BAD
        }
    }
}

fun getTripQuality(
    tripData: TripData,
    settings: ProfitabilitySettings = ProfitabilitySettings()
): TripQuality {
    return getTripQuality(ProfitabilityCalculator.calculate(tripData, settings))
}

fun getTripQuality(profitability: ProfitabilityResult?): TripQuality {
    return when (profitability?.quality) {
        ProfitabilityQuality.GOOD -> TripQuality.GOOD
        ProfitabilityQuality.MEDIUM -> TripQuality.MEDIUM
        ProfitabilityQuality.BAD -> TripQuality.BAD
        null -> TripQuality.BAD
    }
}

@Deprecated("Use getTripQuality(tripData, settings) so the semáforo considers net profit.")
fun getGrossTripQuality(tripData: TripData): TripQuality {
    val valorPorKm = tripData.valorPorKm
    return when {
        valorPorKm >= 2.5 -> TripQuality.GOOD
        valorPorKm >= 1.8 -> TripQuality.MEDIUM
        else -> TripQuality.BAD
    }
}
