package br.com.calcmot.ui.design.domain

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import br.com.calcmot.ui.design.components.CalcMotCard
import br.com.calcmot.ui.design.components.CalcMotCardVariant
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotSpacing
import br.com.calcmot.ui.design.tokens.CalcMotTypography

@Composable
fun GoalPresetCard(
    title: String,
    kmGoal: String,
    hourGoal: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CalcMotCard(
        modifier = modifier,
        variant = if (selected) CalcMotCardVariant.Premium else CalcMotCardVariant.Default
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = CalcMotTypography.CardTitle,
                color = if (selected) CalcMotColors.BrandPrimary else CalcMotColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(CalcMotSpacing.Xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = kmGoal,
                    style = CalcMotTypography.MetricValue,
                    color = CalcMotColors.TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = hourGoal,
                    style = CalcMotTypography.MetricValue,
                    color = CalcMotColors.TextPrimary
                )
            }
        }
    }
}
