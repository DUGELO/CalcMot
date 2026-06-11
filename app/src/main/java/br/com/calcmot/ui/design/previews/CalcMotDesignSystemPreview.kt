package br.com.calcmot.ui.design.previews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import br.com.calcmot.ui.design.components.CalcMotButton
import br.com.calcmot.ui.design.components.CalcMotCard
import br.com.calcmot.ui.design.components.CalcMotInfoBanner
import br.com.calcmot.ui.design.domain.DailySummaryCard
import br.com.calcmot.ui.design.domain.DailySummaryUiState
import br.com.calcmot.ui.design.domain.PermissionStatus
import br.com.calcmot.ui.design.domain.PermissionStatusCard
import br.com.calcmot.ui.design.theme.CalcMotTheme
import br.com.calcmot.ui.design.tokens.CalcMotSpacing

@Preview(showBackground = true, backgroundColor = 0xFF101114)
@Composable
private fun CalcMotDesignSystemPreview() {
    CalcMotTheme {
        Column(
            modifier = Modifier.padding(CalcMotSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Md)
        ) {
            CalcMotInfoBanner(
                title = "Beta fechado",
                body = "Configure suas metas e valide o overlay em ofertas reais."
            )
            PermissionStatusCard(
                title = "Acessibilidade",
                description = "Necessária para ler cards visíveis no app de motorista.",
                status = PermissionStatus.REQUIRED,
                actionLabel = "Ativar",
                onAction = {}
            )
            DailySummaryCard(
                state = DailySummaryUiState(
                    offersAnalyzed = 12,
                    offersAboveGoal = 7,
                    offersBelowGoal = 5,
                    averagePerKm = "R$ 2,10",
                    averagePerHour = "R$ 61"
                )
            )
            CalcMotCard {
                CalcMotButton(
                    text = "Abrir app de motorista",
                    onClick = {},
                    modifier = Modifier.padding(CalcMotSpacing.CardPadding)
                )
            }
        }
    }
}
