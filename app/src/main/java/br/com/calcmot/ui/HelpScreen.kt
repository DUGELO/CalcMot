package br.com.calcmot.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import br.com.calcmot.ui.design.components.CalcMotButton
import br.com.calcmot.ui.design.components.CalcMotButtonVariant
import br.com.calcmot.ui.design.components.CalcMotCard
import br.com.calcmot.ui.design.components.CalcMotSectionHeader
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotSpacing
import br.com.calcmot.ui.design.tokens.CalcMotTypography

@Composable
fun HelpScreen(
    onOpenPrivacy: () -> Unit,
    onSupport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(UiTestTags.HELP_SCREEN)
            .padding(horizontal = CalcMotSpacing.ScreenHorizontal, vertical = CalcMotSpacing.ScreenVertical),
        verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Md)
    ) {
        CalcMotSectionHeader(
            title = "Ajuda",
            subtitle = "Tire dúvidas sobre segurança, privacidade e suporte do CalcMot."
        )

        HelpCard(
            title = "Segurança e privacidade",
            body = "Veja como o CalcMot lê a oferta, o que fica no aparelho e o que ele não faz.",
            actionText = "Ver privacidade",
            actionTag = UiTestTags.HELP_PRIVACY_BUTTON,
            onAction = onOpenPrivacy
        )

        HelpCard(
            title = "Falar com suporte",
            body = "Use o email de suporte para dúvidas, problemas ou solicitações sobre seus dados.",
            actionText = "Enviar email",
            actionTag = UiTestTags.HELP_SUPPORT_BUTTON,
            onAction = onSupport
        )
    }
}

@Composable
private fun HelpCard(
    title: String,
    body: String,
    actionText: String,
    actionTag: String,
    onAction: () -> Unit
) {
    CalcMotCard {
        Column(
            modifier = Modifier.padding(CalcMotSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)
        ) {
            Text(text = title, style = CalcMotTypography.CardTitle, color = CalcMotColors.TextPrimary)
            Text(text = body, style = CalcMotTypography.Body, color = CalcMotColors.TextSecondary)
            CalcMotButton(
                text = actionText,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(actionTag),
                onClick = onAction,
                variant = CalcMotButtonVariant.SECONDARY
            )
        }
    }
}
