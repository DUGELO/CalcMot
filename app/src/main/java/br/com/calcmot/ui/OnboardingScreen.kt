package br.com.calcmot.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.calcmot.AppPermissionState

@Composable
fun OnboardingScreen(
    permissionState: AppPermissionState,
    onPermissionsRefresh: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var showPrivacyPolicy by remember { mutableStateOf(false) }

    if (showPrivacyPolicy) {
        PrivacyPolicyScreen(
            onBack = { showPrivacyPolicy = false },
            onSupport = { uriHandler.openUri("mailto:$CALCMOT_SUPPORT_EMAIL") }
        )
    } else {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(UiTestTags.ONBOARDING_SCREEN)
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("CalcMot", style = MaterialTheme.typography.displayLarge)
                    Text(
                        text = "Assistente de leitura de ofertas para motoristas de app.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                DisclosureCard()

                PermissionCheckItem(
                    text = "Serviço de acessibilidade",
                    detail = if (permissionState.hasAccessibilityService) {
                        "Ativo"
                    } else {
                        "Necessário para ler cards de oferta visíveis."
                    },
                    isChecked = permissionState.hasAccessibilityService,
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                )

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(UiTestTags.OPEN_ACCESSIBILITY_BUTTON),
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    enabled = !permissionState.hasAccessibilityService
                ) {
                    Text("Abrir acessibilidade")
                }

                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(UiTestTags.REFRESH_PERMISSIONS_BUTTON),
                    onClick = onPermissionsRefresh
                ) {
                    Text("Atualizar estado")
                }

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(UiTestTags.FINISH_ONBOARDING_BUTTON),
                    onClick = onPermissionsRefresh,
                    enabled = permissionState.hasAllRequiredPermissions
                ) {
                    Text("Concluir")
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        modifier = Modifier.testTag(UiTestTags.PRIVACY_LINK),
                        onClick = { showPrivacyPolicy = true }
                    ) {
                        Text("Política de privacidade")
                    }
                    TextButton(
                        modifier = Modifier.testTag(UiTestTags.SUPPORT_LINK),
                        onClick = { uriHandler.openUri("mailto:$CALCMOT_SUPPORT_EMAIL") }
                    ) {
                        Text("Suporte")
                    }
                }
            }
        }
    }
}

@Composable
private fun DisclosureCard() {
    Card(
        modifier = Modifier.testTag(UiTestTags.ACCESSIBILITY_DISCLOSURE),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Como o CalcMot usa acessibilidade",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "O CalcMot lê cards de oferta visíveis no app de motorista para calcular R$/km, R$/h e tempo total. O processamento acontece no aparelho.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "O app não toca na tela, não aceita corridas, não recusa corridas e não é afiliado à Uber.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun PermissionCheckItem(
    text: String,
    detail: String,
    isChecked: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(UiTestTags.ACCESSIBILITY_PERMISSION_ITEM),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Checkbox(checked = isChecked, onCheckedChange = null)
        }
    }
}
