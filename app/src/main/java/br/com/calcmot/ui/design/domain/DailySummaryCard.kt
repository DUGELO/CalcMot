package br.com.calcmot.ui.design.domain

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import br.com.calcmot.ui.design.components.CalcMotCard
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotSpacing
import br.com.calcmot.ui.design.tokens.CalcMotTypography

@Composable
fun DailySummaryCard(
    offersAnalyzed: Int,
    avgKm: String,
    avgHour: String,
    modifier: Modifier = Modifier
) {
    CalcMotCard(modifier = modifier) {
        Text(
            text = "Resumo do Dia",
            style = CalcMotTypography.CardTitle,
            color = CalcMotColors.TextPrimary
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = CalcMotSpacing.Md),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SummaryItem(label = "Analisadas", value = offersAnalyzed.toString())
            SummaryItem(label = "Média R$/km", value = avgKm)
            SummaryItem(label = "Média R$/h", value = avgHour)
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String
) {
    Column {
        Text(
            text = value,
            style = CalcMotTypography.MetricValue,
            color = CalcMotColors.TextPrimary
        )
        Text(
            text = label,
            style = CalcMotTypography.Caption,
            color = CalcMotColors.TextMuted
        )
    }
}
