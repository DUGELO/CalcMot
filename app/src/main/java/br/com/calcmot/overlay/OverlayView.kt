package br.com.calcmot.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.calcmot.model.ProfitabilityCalculator
import br.com.calcmot.model.ProfitabilityQuality
import br.com.calcmot.model.ProfitabilityResult
import br.com.calcmot.model.ProfitabilitySettings
import br.com.calcmot.model.TripData
import br.com.calcmot.model.OfferFinancialImpact
import br.com.calcmot.ui.theme.SemanticAttention
import br.com.calcmot.ui.theme.SemanticBad
import br.com.calcmot.ui.theme.SemanticGood
import java.util.Locale

@Composable
fun OverlayView(
    tripData: TripData,
    profitability: ProfitabilityResult? = null,
    financialImpact: OfferFinancialImpact? = null
) {
    val result = profitability ?: ProfitabilityCalculator.calculate(tripData, ProfitabilitySettings())
    val quality = getTripQuality(result)
    val foreground = if (quality == TripQuality.BAD) Color.White else Color.Black
    val perKm = result?.netPerKm ?: tripData.valorPorKm
    val perHour = result?.netPerHour ?: tripData.valorPorHora

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(quality.color.copy(alpha = 0.94f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = quality.text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = foreground
        )
        Text(
            text = brCurrencyPerUnit(perKm, "km"),
            style = MaterialTheme.typography.displayLarge,
            color = foreground
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = brCurrencyPerUnit(perHour, "h"),
                style = MaterialTheme.typography.headlineLarge,
                color = foreground
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = "${tripData.minutosTotais} min",
                style = MaterialTheme.typography.headlineLarge,
                color = foreground
            )
        }
        financialImpact?.let {
            FinancialImpactBlock(
                impact = it,
                foreground = foreground
            )
        }
    }
}

@Composable
private fun FinancialImpactBlock(
    impact: OfferFinancialImpact,
    foreground: Color
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(foreground.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = impact.message,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = foreground
        )
        Text(
            text = impact.subtext,
            style = MaterialTheme.typography.labelMedium,
            color = foreground.copy(alpha = 0.9f)
        )
    }
}

private fun brCurrencyPerUnit(value: Double, unit: String): String {
    return String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f/%s", value, unit)
}

enum class TripQuality(val text: String, val color: Color) {
    GOOD("BOA", SemanticGood),
    MEDIUM("MÉDIA", SemanticAttention),
    BAD("RUIM", SemanticBad)
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

@Deprecated("Use getTripQuality(tripData, settings) so the semaforo considers net profit.")
fun getGrossTripQuality(tripData: TripData): TripQuality {
    val valorPorKm = tripData.valorPorKm
    return when {
        valorPorKm >= 2.5 -> TripQuality.GOOD
        valorPorKm >= 1.8 -> TripQuality.MEDIUM
        else -> TripQuality.BAD
    }
}
