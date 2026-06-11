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
                title = "Sua segurança primeiro",
                subtitle = "Antes dos detalhes legais, veja o que o CalcMot faz e o que ele não faz."
            )

            PolicySection(
                title = "3 garantias importantes",
                body = "O CalcMot não toca na tela. Não aceita nem recusa corridas. Não envia ofertas para servidor."
            )

            PolicySection(
                title = "O que o CalcMot não faz",
                body = "Não acessa sua conta bancária. Não lê mensagens. Não aceita corridas por você. Não recusa corridas por você. Não vende seus dados."
            )

            PolicySection(
                title = "Controle do usuário",
                body = "Você pode pausar o CalcMot dentro do app a qualquer momento. Também pode remover a permissão de leitura da oferta nas configurações do Android."
            )

            CalcMotSectionHeader(
                title = "Detalhes da política",
                subtitle = "Última atualização: 27 de maio de 2026."
            )

            PolicySection(
                title = "Resumo",
                body = "O CalcMot é um assistente de leitura de ofertas para motoristas de aplicativo. Ele mostra R$/km, R$/hora e tempo total para ajudar você a avaliar uma oferta visível na tela."
            )

            PolicySection(
                title = "Dados processados",
                body = "Durante uma oferta, o app pode processar no próprio aparelho textos visíveis no card, como valor, distância, tempo, nota exibida e endereços mostrados pelo app de motorista."
            )

            PolicySection(
                title = "Permissão para ler a oferta",
                body = "O CalcMot usa a permissão de leitura da tela para identificar ofertas visíveis e mostrar o cálculo por cima da Uber. Quando necessário, o app lê localmente somente a oferta visível na tela."
            )

            PolicySection(
                title = "Processamento local",
                body = "Os cálculos acontecem no aparelho. O CalcMot não envia screenshots, endereços, localização, dados de corrida, notas ou conteúdo de tela para servidores externos."
            )

            PolicySection(
                title = "Compartilhamento",
                body = "O CalcMot não vende dados pessoais e não compartilha dados de ofertas ou corridas com terceiros."
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
