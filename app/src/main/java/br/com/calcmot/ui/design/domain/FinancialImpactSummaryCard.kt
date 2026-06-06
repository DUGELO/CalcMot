package br.com.calcmot.ui.design.domain

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import br.com.calcmot.ui.design.components.CalcMotCard
import br.com.calcmot.ui.design.components.CalcMotCardVariant
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotSpacing
import br.com.calcmot.ui.design.tokens.CalcMotTypography

@Composable
fun FinancialImpactSummaryCard(
    impactAmount: String,
    modifier: Modifier = Modifier
) {
    CalcMotCard(
        modifier = modifier,
        variant = CalcMotCardVariant.Success
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Impacto Financeiro",
                style = CalcMotTypography.CardTitle,
                color = CalcMotColors.PositiveMoney
            )
            Spacer(modifier = Modifier.height(CalcMotSpacing.Sm))
            Text(
                text = "Hoje você recebeu $impactAmount em ofertas abaixo da sua meta.",
                style = CalcMotTypography.Body,
                color = CalcMotColors.TextPrimary
            )
        }
    }
}
