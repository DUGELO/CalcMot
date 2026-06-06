package br.com.calcmot.overlay

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import br.com.calcmot.model.ProfitabilityCalculator
import br.com.calcmot.model.ProfitabilityQuality
import br.com.calcmot.model.ProfitabilityResult
import br.com.calcmot.model.ProfitabilitySettings
import br.com.calcmot.model.TripData
import br.com.calcmot.model.OfferFinancialImpact
import br.com.calcmot.ui.design.overlay.CalcMotOverlayContainer
import br.com.calcmot.ui.design.overlay.FinancialImpactBlockDS
import br.com.calcmot.ui.design.overlay.MetricRow
import br.com.calcmot.ui.design.overlay.OfferQualityBadge
import br.com.calcmot.ui.design.overlay.OverlayDragHandle
import br.com.calcmot.ui.design.tokens.CalcMotColors
import java.util.Locale

@Composable
fun OverlayView(
    tripData: TripData,
    profitability: ProfitabilityResult? = null,
    financialImpact: OfferFinancialImpact? = null
) {
    val result = profitability ?: ProfitabilityCalculator.calculate(tripData, ProfitabilitySettings())
    val quality = getTripQuality(result)
    val perKm = result?.netPerKm ?: tripData.valorPorKm
    val perHour = result?.netPerHour ?: tripData.valorPorHora

    CalcMotOverlayContainer {
        OverlayDragHandle()

        OfferQualityBadge(
            text = quality.text,
            color = quality.color
        )

        MetricRow(
            primaryValue = brCurrencyPerUnit(perKm, "km")
        )

        MetricRow(
            primaryValue = brCurrencyPerUnit(perHour, "h"),
            secondaryValue = "${tripData.minutosTotais} min"
        )

        financialImpact?.let {
            FinancialImpactBlockDS(impact = it)
        }
    }
}

private fun brCurrencyPerUnit(value: Double, unit: String): String {
    return String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f/%s", value, unit)
}

enum class TripQuality(val text: String, val color: Color) {
    GREAT("ÓTIMA", CalcMotColors.Great),
    GOOD("BOA", CalcMotColors.Success),
    MEDIUM("ATENÇÃO", CalcMotColors.Warning),
    BAD("RUIM", CalcMotColors.Danger)
}

fun getTripQuality(
    tripData: TripData,
    settings: ProfitabilitySettings = ProfitabilitySettings()
): TripQuality {
    return getTripQuality(ProfitabilityCalculator.calculate(tripData, settings))
}

fun getTripQuality(profitability: ProfitabilityResult?): TripQuality {
    return when (profitability?.quality) {
        ProfitabilityQuality.GREAT -> TripQuality.GREAT
        ProfitabilityQuality.GOOD -> TripQuality.GOOD
        ProfitabilityQuality.MEDIUM -> TripQuality.MEDIUM
        ProfitabilityQuality.BAD -> TripQuality.BAD
        null -> TripQuality.BAD
    }
}

@Deprecated("Use getTripQuality(tripData, settings) so the semaforo considers net profit.")
fun getGrossTripQuality(tripData: TripData): TripQuality {
    val valorPorKm = tripData.valorPorKm
    return when {
        valorPorKm >= 2.5 -> TripQuality.GOOD
        valorPorKm >= 1.8 -> TripQuality.MEDIUM
        else -> TripQuality.BAD
    }
}
