package br.com.calcmot.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun PrivacyPolicyScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onSupport: () -> Unit
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .testTag(UiTestTags.PRIVACY_POLICY_SCREEN)
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                modifier = Modifier.testTag(UiTestTags.PRIVACY_POLICY_BACK_BUTTON),
                onClick = onBack
            ) {
                Text("Voltar")
            }
            Text(
                text = "Privacidade",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            ElevatedCard {
                Text(
                    modifier = Modifier.padding(20.dp),
                    text = "O CalcMot calcula ofertas visíveis no seu aparelho. Ele não toca na tela, não aceita corridas, não recusa corridas e não vende seus dados.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            PrivacyListItem(
                title = "Onde atua",
                body = "Em apps de motorista, quando aparece uma oferta completa."
            )
            PrivacyListItem(
                title = "O que não faz",
                body = "Não acessa banco, mensagens, fotos ou navegador para calcular corrida."
            )
            PrivacyListItem(
                title = "Controle do usuário",
                body = "Você pode pausar o cálculo automático ou remover a permissão no Android."
            )
            HorizontalDivider()
            Text(
                text = "Detalhes da política",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp)
            )
            PolicyDetail(
                title = "Última atualização",
                body = "27 de maio de 2026."
            )
            PolicyDetail(
                title = "Dados processados",
                body = "Durante uma oferta, o app pode processar no próprio aparelho textos visíveis no card, como valor, distância, tempo e endereços mostrados pelo app de motorista."
            )
            PolicyDetail(
                title = "Permissão do Android",
                body = "A permissão permite identificar ofertas visíveis e mostrar o cálculo por cima do app de motorista. O processamento acontece localmente."
            )
            PolicyDetail(
                title = "Compartilhamento",
                body = "O CalcMot não vende dados pessoais e não compartilha dados de ofertas ou corridas com terceiros."
            )
            PolicyDetail(
                title = "Suporte",
                body = "Para dúvidas, suporte ou solicitações relacionadas à privacidade, entre em contato pelo email $CALCMOT_SUPPORT_EMAIL."
            )
            androidx.compose.material3.Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(UiTestTags.PRIVACY_POLICY_SUPPORT_BUTTON),
                onClick = onSupport
            ) {
                Text("Enviar email para suporte")
            }
        }
    }
}

@Composable
private fun PrivacyListItem(
    title: String,
    body: String
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(body) }
    )
}

@Composable
private fun PolicyDetail(
    title: String,
    body: String
) {
    OutlinedCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(text = body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
