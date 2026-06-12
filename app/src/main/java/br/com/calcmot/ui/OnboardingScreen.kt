package br.com.calcmot.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
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
        Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(UiTestTags.ONBOARDING_SCREEN)
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "CalcMot",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Veja se a corrida compensa antes de aceitar.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(
                    modifier = Modifier.testTag(UiTestTags.ACCESSIBILITY_DISCLOSURE),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OnboardingCheck("Calcula R$/km e R$/h.")
                    OnboardingCheck("Mostra Boa, Média ou Ruim.")
                    OnboardingCheck("Fica em espera fora dos apps de motorista.")
                }

                ElevatedCard(modifier = Modifier.testTag(UiTestTags.ACCESSIBILITY_PERMISSION_ITEM)) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Permissão para calcular automaticamente",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = if (permissionState.hasAccessibilityService) {
                                "Permissão ativa. Você já pode usar o CalcMot nos apps de motorista."
                            } else {
                                "Ative uma vez para o aviso aparecer quando uma oferta surgir."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(UiTestTags.OPEN_ACCESSIBILITY_BUTTON),
                            enabled = !permissionState.hasAccessibilityService,
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            }
                        ) {
                            Text("Ativar permissão")
                        }
                        OutlinedButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(UiTestTags.REFRESH_PERMISSIONS_BUTTON),
                            onClick = onPermissionsRefresh
                        ) {
                            Text("Já ativei")
                        }
                        if (!permissionState.hasAllRequiredPermissions) {
                            Text(
                                text = "Depois de ativar a permissão, você poderá continuar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Button(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(UiTestTags.FINISH_ONBOARDING_BUTTON),
                                onClick = onPermissionsRefresh
                            ) {
                                Text("Continuar")
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        modifier = Modifier.testTag(UiTestTags.PRIVACY_LINK),
                        onClick = { showPrivacyPolicy = true }
                    ) {
                        Text("Privacidade")
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
private fun OnboardingCheck(text: String) {
    Text(
        text = "✓ $text",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground
    )
}
