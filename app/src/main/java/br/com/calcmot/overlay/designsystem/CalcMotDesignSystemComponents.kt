package br.com.calcmot.overlay.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.calcmot.model.OfferFinancialImpact

@Composable
fun CalcMotOverlayContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(CalcMotShape.OverlayRadius))
            .background(CalcMotColors.OverlayBackground.copy(alpha = CalcMotOpacity.OverlayMedium))
            .padding(CalcMotSpacing.OverlayPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.MetricGap)
    ) {
        content()
    }
}

@Composable
fun OfferQualityBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(CalcMotShape.BadgeRadius))
            .background(color)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = CalcMotTypography.MetricLabel.copy(
                fontWeight = FontWeight.Bold,
                color = if (color == CalcMotColors.Bad) Color.White else Color.Black
            )
        )
    }
}

@Composable
fun MetricRow(
    primaryValue: String,
    secondaryValue: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)
    ) {
        Text(
            text = primaryValue,
            style = CalcMotTypography.ValuePrimary,
            color = CalcMotColors.TextPrimary
        )
        secondaryValue?.let {
            Text(
                text = it,
                style = CalcMotTypography.MetricValue,
                color = CalcMotColors.TextSecondary.copy(alpha = CalcMotOpacity.SecondaryText)
            )
        }
    }
}

@Composable
fun FinancialImpactBlockDS(
    impact: OfferFinancialImpact,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(CalcMotShape.SmallRadius))
            .background(CalcMotColors.SurfaceElevated.copy(alpha = 0.5f))
            .padding(horizontal = CalcMotSpacing.Sm, vertical = CalcMotSpacing.Xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = impact.message,
            style = CalcMotTypography.ImpactMessage,
            color = CalcMotColors.TextPrimary
        )
        Text(
            text = impact.subtext,
            style = CalcMotTypography.ImpactSubMessage,
            color = CalcMotColors.TextSecondary.copy(alpha = CalcMotOpacity.SecondaryText)
        )
    }
}

@Composable
fun GoalStatusPill(
    value: Double,
    modifier: Modifier = Modifier
) {
    val isPositive = value >= 0
    val color = if (isPositive) CalcMotColors.Good else CalcMotColors.Bad
    val sign = if (isPositive) "+" else ""
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(CalcMotShape.BadgeRadius))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = String.format(java.util.Locale.US, "%sR$ %.2f", sign, value),
            style = CalcMotTypography.MetricLabel.copy(
                color = color,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
fun OverlayDragHandle(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(32.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(CalcMotColors.Divider)
    )
}
