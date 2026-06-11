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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
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
import br.com.calcmot.AppPermissionState
import br.com.calcmot.ui.design.components.CalcMotButton
import br.com.calcmot.ui.design.components.CalcMotButtonVariant
import br.com.calcmot.ui.design.components.CalcMotCard
import br.com.calcmot.ui.design.components.CalcMotScaffold
import br.com.calcmot.ui.design.components.CalcMotSectionHeader
import br.com.calcmot.ui.design.domain.PermissionStatus
import br.com.calcmot.ui.design.domain.PermissionStatusCard
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotSpacing
import br.com.calcmot.ui.design.tokens.CalcMotTypography

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
        CalcMotScaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(UiTestTags.ONBOARDING_SCREEN)
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(horizontal = CalcMotSpacing.ScreenHorizontal, vertical = CalcMotSpacing.Xl),
                verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.SectionGap)
            ) {
                CalcMotSectionHeader(
                    title = "CalcMot",
                    subtitle = "Assistente de leitura de ofertas para motoristas de app."
                )

                DisclosureCard()

                PermissionStatusCard(
                    modifier = Modifier.testTag(UiTestTags.ACCESSIBILITY_PERMISSION_ITEM),
                    title = "Permita a leitura da oferta",
                    description = if (permissionState.hasAccessibilityService) {
                        "Permissão ativada. Agora você pode usar o CalcMot na Uber."
                    } else {
                        "O CalcMot usa essa permissão para ler a oferta visível enquanto você usa um app de motorista."
                    },
                    status = if (permissionState.hasAccessibilityService) {
                        PermissionStatus.ACTIVE
                    } else {
                        PermissionStatus.REQUIRED
                    },
                    actionLabel = "Permitir leitura da oferta",
                    actionModifier = Modifier
                        .fillMaxWidth()
                        .testTag(UiTestTags.OPEN_ACCESSIBILITY_BUTTON),
                    onAction = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                )

                CalcMotButton(
                    text = "Já permiti, verificar novamente",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(UiTestTags.REFRESH_PERMISSIONS_BUTTON),
                    onClick = onPermissionsRefresh,
                    variant = CalcMotButtonVariant.SECONDARY
                )

                CalcMotButton(
                    text = "Continuar",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(UiTestTags.FINISH_ONBOARDING_BUTTON),
                    onClick = onPermissionsRefresh,
                    enabled = permissionState.hasAllRequiredPermissions
                )

                if (!permissionState.hasAllRequiredPermissions) {
                    Text(
                        text = "Ainda falta permitir a leitura da oferta. Toque no botão acima e ative o CalcMot nas configurações do Android.",
                        style = CalcMotTypography.Caption,
                        color = CalcMotColors.TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(CalcMotSpacing.Sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CalcMotButton(
                        text = "Ver política de privacidade",
                        modifier = Modifier.testTag(UiTestTags.PRIVACY_LINK),
                        onClick = { showPrivacyPolicy = true },
                        variant = CalcMotButtonVariant.GHOST
                    )
                    CalcMotButton(
                        text = "Suporte",
                        modifier = Modifier.testTag(UiTestTags.SUPPORT_LINK),
                        onClick = { uriHandler.openUri("mailto:$CALCMOT_SUPPORT_EMAIL") },
                        variant = CalcMotButtonVariant.GHOST
                    )
                }
            }
        }
    }
}

@Composable
private fun DisclosureCard() {
    CalcMotCard(
        modifier = Modifier.testTag(UiTestTags.ACCESSIBILITY_DISCLOSURE)
    ) {
        Column(
            modifier = Modifier.padding(CalcMotSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)
        ) {
            Text(
                text = "Como o CalcMot lê a oferta",
                style = CalcMotTypography.CardTitle,
                color = CalcMotColors.TextPrimary
            )
            Text(
                text = "- Calcula R$/km e R$/hora quando aparece uma oferta.",
                style = CalcMotTypography.Body,
                color = CalcMotColors.TextSecondary
            )
            Text(
                text = "- Tudo acontece no seu aparelho.",
                style = CalcMotTypography.Body,
                color = CalcMotColors.TextSecondary
            )
            Text(
                text = "- Não toca na tela e não aceita corridas.",
                style = CalcMotTypography.Body,
                color = CalcMotColors.TextSecondary
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
    CalcMotCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(UiTestTags.ACCESSIBILITY_PERMISSION_ITEM),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(CalcMotSpacing.CardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text, style = CalcMotTypography.CardTitle, color = CalcMotColors.TextPrimary)
                Text(
                    text = detail,
                    style = CalcMotTypography.Caption,
                    color = CalcMotColors.TextSecondary
                )
            }
            Checkbox(checked = isChecked, onCheckedChange = null)
        }
    }
}
