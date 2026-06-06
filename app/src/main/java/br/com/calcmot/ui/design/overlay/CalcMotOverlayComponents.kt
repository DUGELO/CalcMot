package br.com.calcmot.ui.design.overlay

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
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotOpacity
import br.com.calcmot.ui.design.tokens.CalcMotShape
import br.com.calcmot.ui.design.tokens.CalcMotSpacing
import br.com.calcmot.ui.design.tokens.CalcMotTypography

@Composable
fun CalcMotOverlayContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(CalcMotShape.Lg))
            .background(CalcMotColors.OverlayBackground.copy(alpha = CalcMotOpacity.OverlayMedium))
            .padding(CalcMotSpacing.Sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Xs)
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
            .clip(RoundedCornerShape(CalcMotShape.Pill))
            .background(color)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = CalcMotTypography.MetricLabel.copy(
                fontWeight = FontWeight.Bold,
                color = if (color == CalcMotColors.Danger) Color.White else CalcMotColors.TextInverse
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
            style = CalcMotTypography.MetricHero,
            color = CalcMotColors.TextPrimary
        )
        secondaryValue?.let {
            Text(
                text = it,
                style = CalcMotTypography.MetricValue,
                color = CalcMotColors.TextSecondary.copy(alpha = CalcMotOpacity.Secondary)
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
            .clip(RoundedCornerShape(CalcMotShape.Sm))
            .background(CalcMotColors.SurfaceElevated.copy(alpha = 0.5f))
            .padding(horizontal = CalcMotSpacing.Sm, vertical = CalcMotSpacing.Xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = impact.message,
            style = CalcMotTypography.Caption.copy(fontWeight = FontWeight.Bold),
            color = CalcMotColors.TextPrimary
        )
        Text(
            text = impact.subtext,
            style = CalcMotTypography.Caption,
            color = CalcMotColors.TextSecondary.copy(alpha = CalcMotOpacity.Secondary)
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
            .background(CalcMotColors.BorderSubtle)
    )
}
