package br.com.calcmot.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import br.com.calcmot.ui.CALCMOT_SUPPORT_EMAIL
import br.com.calcmot.ui.design.components.CalcMotScaffold
import br.com.calcmot.ui.design.components.CalcMotTopBar
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotSpacing
import br.com.calcmot.ui.design.tokens.CalcMotTypography

@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit,
    onSupport: () -> Unit
) {
    CalcMotScaffold(
        topBar = {
            CalcMotTopBar(
                title = "Privacidade",
                onBackClick = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = CalcMotSpacing.ScreenHorizontal, vertical = CalcMotSpacing.ScreenVertical)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Lg)
        ) {
            Text(
                text = "Política de Privacidade",
                style = CalcMotTypography.ScreenTitle,
                color = CalcMotColors.TextPrimary
            )
            
            PolicySection(
                title = "Privacidade em primeiro lugar",
                content = "O CalcMot foi desenhado para ser offline por padrão. Não coletamos seus dados de corrida, localização ou identificação pessoal em servidores externos."
            )

            PolicySection(
                title = "Como usamos a Acessibilidade",
                content = "O serviço de acessibilidade é usado exclusivamente para ler o conteúdo dos cards de oferta da Uber Driver quando eles estão visíveis na tela. Não interagimos com botões, não alteramos dados e não capturamos senhas."
            )
            
            PolicySection(
                title = "Armazenamento Local",
                content = "Todas as configurações e histórico de finanças são salvos apenas no seu aparelho. Se você desinstalar o app, esses dados serão apagados."
            )
        }
    }
}

@Composable
private fun PolicySection(
    title: String,
    content: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Xs)) {
        Text(
            text = title,
            style = CalcMotTypography.SectionTitle,
            color = CalcMotColors.TextPrimary
        )
        Text(
            text = content,
            style = CalcMotTypography.Body,
            color = CalcMotColors.TextSecondary
        )
    }
}
