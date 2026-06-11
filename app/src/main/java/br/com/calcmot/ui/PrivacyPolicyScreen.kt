package br.com.calcmot.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import br.com.calcmot.ui.design.components.CalcMotButton
import br.com.calcmot.ui.design.components.CalcMotButtonVariant
import br.com.calcmot.ui.design.components.CalcMotCard
import br.com.calcmot.ui.design.components.CalcMotScaffold
import br.com.calcmot.ui.design.components.CalcMotSectionHeader
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotSpacing
import br.com.calcmot.ui.design.tokens.CalcMotTypography

@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit,
    onSupport: () -> Unit
) {
    CalcMotScaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = CalcMotSpacing.ScreenHorizontal, vertical = CalcMotSpacing.ScreenVertical),
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.SectionGap)
        ) {
            CalcMotButton(
                modifier = Modifier.testTag(UiTestTags.PRIVACY_POLICY_BACK_BUTTON),
                text = "Voltar",
                onClick = onBack,
                variant = CalcMotButtonVariant.SECONDARY
            )

            CalcMotSectionHeader(
                modifier = Modifier.testTag(UiTestTags.PRIVACY_POLICY_SCREEN),
                title = "Política de privacidade",
                subtitle = "Última atualização: 27 de maio de 2026."
            )

            PolicySection(
                title = "Resumo",
                body = "O CalcMot é um assistente de leitura de ofertas para motoristas de aplicativo. Ele mostra métricas como R$/km, R$/h e tempo total para ajudar você a avaliar uma oferta visível na tela."
            )

            PolicySection(
                title = "Dados processados",
                body = "Durante uma oferta, o app pode processar no próprio aparelho textos visíveis no card, como valor, distância, tempo, nota exibida e endereços mostrados pelo app de motorista."
            )

            PolicySection(
                title = "Acessibilidade e leitura local",
                body = "O CalcMot usa o Serviço de Acessibilidade do Android para identificar cards de oferta e exibir um aviso sobreposto. Quando necessário, o app pode ler localmente somente o card visível na tela."
            )

            PolicySection(
                title = "Processamento local",
                body = "Os cálculos acontecem no aparelho. O CalcMot não envia screenshots, endereços, localização, dados de corrida, notas ou conteúdo de tela para servidores externos."
            )

            PolicySection(
                title = "O que o app não faz",
                body = "O app não aceita corridas, não recusa corridas, não toca na tela por você e não controla o app de motorista. O CalcMot não é afiliado à Uber."
            )

            PolicySection(
                title = "Compartilhamento",
                body = "O CalcMot não vende dados pessoais e não compartilha dados de ofertas ou corridas com terceiros."
            )

            PolicySection(
                title = "Controle do usuário",
                body = "Você pode pausar o monitoramento dentro do app a qualquer momento. Também pode desativar o Serviço de Acessibilidade do CalcMot nas configurações do Android."
            )

            PolicySection(
                title = "Suporte",
                body = "Para dúvidas, suporte ou solicitações relacionadas à privacidade, entre em contato pelo email $CALCMOT_SUPPORT_EMAIL."
            )

            CalcMotButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(UiTestTags.PRIVACY_POLICY_SUPPORT_BUTTON),
                text = "Enviar email para suporte",
                onClick = onSupport
            )
        }
    }
}

@Composable
private fun PolicySection(
    title: String,
    body: String
) {
    CalcMotCard {
        Column(
            modifier = Modifier.padding(CalcMotSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Xs)
        ) {
            Text(text = title, style = CalcMotTypography.CardTitle, color = CalcMotColors.TextPrimary)
            Text(text = body, style = CalcMotTypography.Body, color = CalcMotColors.TextSecondary)
        }
    }
}
