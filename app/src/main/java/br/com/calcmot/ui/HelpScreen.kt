package br.com.calcmot.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun HelpScreen(
    modifier: Modifier = Modifier,
    onOpenPrivacy: () -> Unit,
    onSupport: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(UiTestTags.HELP_SCREEN)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Ajuda",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
        )
        FaqItem(
            title = "Como usar",
            body = "Abra o app de motorista. O aviso aparece quando surgir uma oferta completa."
        )
        FaqItem(
            title = "O que significam as cores",
            body = "Boa fica dentro da meta. Média fica no limite. Ruim fica abaixo."
        )
        FaqItem(
            title = "Por que precisa da permissão",
            body = "Para calcular automaticamente a oferta visível nos apps de motorista."
        )
        FaqItem(
            title = "Funciona fora da Uber ou 99?",
            body = "Não. Fora dos apps de motorista, o CalcMot fica em espera."
        )
        FaqItem(
            title = "Como pausar",
            body = "Entre em Configurações e desligue o cálculo automático."
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text("Privacidade") },
            supportingContent = { Text("Resumo de dados e segurança.") },
            trailingContent = {
                TextButton(
                    modifier = Modifier.testTag(UiTestTags.HELP_PRIVACY_BUTTON),
                    onClick = onOpenPrivacy
                ) {
                    Text("Abrir")
                }
            }
        )
        ListItem(
            headlineContent = { Text("Suporte") },
            supportingContent = { Text("Dúvidas, problemas ou solicitações.") },
            trailingContent = {
                TextButton(
                    modifier = Modifier.testTag(UiTestTags.HELP_SUPPORT_BUTTON),
                    onClick = onSupport
                ) {
                    Text("Email")
                }
            }
        )
    }
}

@Composable
private fun FaqItem(
    title: String,
    body: String
) {
    ListItem(
        modifier = Modifier.testTag(UiTestTags.FAQ_ITEM),
        headlineContent = { Text(title) },
        supportingContent = { Text(body) }
    )
    HorizontalDivider()
}
