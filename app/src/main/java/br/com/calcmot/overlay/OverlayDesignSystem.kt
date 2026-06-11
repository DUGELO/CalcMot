package br.com.calcmot.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.calcmot.model.ImpactMetric
import br.com.calcmot.model.OfferClassification
import br.com.calcmot.model.OfferFinancialImpact
import java.util.Locale
import kotlin.math.abs

object CalcMotColors {
    val Bad = Color(0xFFE53935)
    val Warning = Color(0xFFFFB300)
    val Good = Color(0xFF2E7D32)
    val Great = Color(0xFF6D3BFF)
    val RoyalBlueAlternative = Color(0xFF2457FF)

    val OverlayBackground = Color(0xF01A1A1A)
    val OverlayBackgroundSoft = Color(0xD91A1A1A)
    val SurfaceElevated = Color(0xF2232323)

    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFE0E0E0)
    val TextMuted = Color(0xFFBDBDBD)

    val Divider = Color(0x33FFFFFF)
}

object CalcMotOpacity {
    const val OverlayStrong = 0.94f
    const val OverlayMedium = 0.86f
    const val OverlaySoft = 0.82f

    const val AccentFull = 1.00f
    const val SecondaryText = 0.82f
    const val Disabled = 0.45f
}

object CalcMotTypography {
    val ValuePrimary = TextStyle(
        fontSize = 23.sp,
        fontWeight = FontWeight.Bold
    )

    val MetricLabel = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
    )

    val MetricValue = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold
    )

    val ImpactMessage = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
    )

    val ImpactSubMessage = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
    )
}

object CalcMotSpacing {
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp

    val OverlayPadding = 9.dp
    val MetricGap = 4.dp
    val SectionGap = 5.dp
}

object CalcMotShape {
    val OverlayRadius = 16.dp
    val BadgeRadius = 999.dp
    val CardRadius = 20.dp
}

object CalcMotElevation {
    val Overlay = 8.dp
    val Floating = 12.dp
}

enum class OverlayOfferQuality(
    val label: String,
    val meaning: String,
    val accentColor: Color,
    val badgeContentColor: Color
) {
    GREAT("ÓTIMA", "Muito acima da meta", CalcMotColors.Great, CalcMotColors.TextPrimary),
    GOOD("BOA", "Dentro da meta", CalcMotColors.Good, CalcMotColors.TextPrimary),
    WARNING("ATENÇÃO", "No limite", CalcMotColors.Warning, Color(0xFF181818)),
    BAD("RUIM", "Abaixo da meta", CalcMotColors.Bad, CalcMotColors.TextPrimary);

    companion object {
        fun fromClassification(classification: OfferClassification): OverlayOfferQuality {
            return when (classification) {
                OfferClassification.GREAT -> GREAT
                OfferClassification.GOOD -> GOOD
                OfferClassification.WARNING -> WARNING
                OfferClassification.BAD -> BAD
            }
        }
    }
}

@Composable
fun CalcMotOverlayContainer(
    quality: OverlayOfferQuality,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(CalcMotShape.OverlayRadius)
    Column(
        modifier = modifier
            .widthIn(min = 176.dp, max = 276.dp)
            .clip(shape)
            .background(CalcMotColors.OverlayBackground)
            .border(
                width = 1.dp,
                color = quality.accentColor.copy(alpha = 0.72f),
                shape = shape
            )
            .padding(CalcMotSpacing.OverlayPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.SectionGap)
    ) {
        OverlayDragHandle(color = CalcMotColors.TextMuted)
        content()
    }
}

@Composable
fun OfferDecisionHeader(
    quality: OverlayOfferQuality,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = quality.label,
            modifier = Modifier
                .clip(RoundedCornerShape(CalcMotShape.BadgeRadius))
                .background(quality.accentColor)
                .padding(horizontal = CalcMotSpacing.Sm, vertical = 3.dp)
                .defaultMinSize(minWidth = 58.dp),
            color = quality.badgeContentColor,
            style = CalcMotTypography.MetricLabel.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = quality.meaning,
            modifier = Modifier.padding(start = CalcMotSpacing.Sm),
            color = CalcMotColors.TextPrimary,
            style = CalcMotTypography.ImpactMessage,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MetricRow(
    value: String,
    label: String? = null,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
    accentColor: Color = CalcMotColors.TextPrimary
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            text = value,
            color = if (prominent) accentColor else CalcMotColors.TextPrimary,
            style = if (prominent) CalcMotTypography.ValuePrimary else CalcMotTypography.MetricValue,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                color = CalcMotColors.TextSecondary.copy(alpha = CalcMotOpacity.SecondaryText),
                style = CalcMotTypography.MetricLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun FinancialImpactLine(
    impact: OfferFinancialImpact,
    quality: OverlayOfferQuality,
    modifier: Modifier = Modifier
) {
    Text(
        text = impact.decisionImpactLine(),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 1.dp),
        color = quality.accentColor,
        style = CalcMotTypography.ImpactMessage,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun OverlayDragHandle(
    color: Color,
    modifier: Modifier = Modifier,
    width: Dp = 30.dp
) {
    Box(
        modifier = modifier
            .size(width = width, height = 3.dp)
            .clip(RoundedCornerShape(CalcMotShape.BadgeRadius))
            .background(color.copy(alpha = 0.34f))
    )
}

@Composable
fun OverlayMetricSummary(
    perKm: String,
    perHour: String,
    duration: String,
    quality: OverlayOfferQuality,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.MetricGap)
    ) {
        MetricRow(
            value = perKm,
            prominent = true,
            accentColor = quality.accentColor
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$perHour · $duration",
                color = CalcMotColors.TextPrimary,
                style = CalcMotTypography.MetricValue,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun OfferFinancialImpact.decisionImpactLine(): String {
    val value = formatMoney(abs(finalImpact))
    return if (finalImpact >= 0.0) {
        "+$value acima da meta"
    } else {
        "-$value ${negativeImpactLabel()}"
    }
}

private fun OfferFinancialImpact.negativeImpactLabel(): String {
    return when (classification) {
        OfferClassification.WARNING -> when (finalMetric) {
            ImpactMetric.KM -> "abaixo por km"
            ImpactMetric.HOUR -> "abaixo por hora"
        }
        else -> "abaixo da meta"
    }
}

private fun formatMoney(value: Double): String {
    return String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", value)
}
