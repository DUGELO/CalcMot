package br.com.calcmot.ui.screens

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
import br.com.calcmot.ui.CALCMOT_SUPPORT_EMAIL
import br.com.calcmot.ui.UiTestTags
import br.com.calcmot.ui.design.components.CalcMotButton
import br.com.calcmot.ui.design.components.CalcMotButtonVariant
import br.com.calcmot.ui.design.components.CalcMotCard
import br.com.calcmot.ui.design.components.CalcMotScaffold
import br.com.calcmot.ui.design.components.CalcMotStatusBadge
import br.com.calcmot.ui.design.components.CalcMotStatusVariant
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
                    .padding(horizontal = CalcMotSpacing.ScreenHorizontal, vertical = CalcMotSpacing.ScreenVertical),
                verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.SectionGap)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Xs)) {
                    Text(
                        text = "CalcMot",
                        style = CalcMotTypography.ScreenTitle,
                        color = CalcMotColors.TextPrimary
                    )
                    Text(
                        text = "Assistente de leitura de ofertas para motoristas de app.",
                        style = CalcMotTypography.Body,
                        color = CalcMotColors.TextSecondary
                    )
                }

                DisclosureCard()

                PermissionStatusCard(
                    title = "Acessibilidade",
                    status = if (permissionState.hasAccessibilityService) "Ativo" else "Pendente",
                    variant = if (permissionState.hasAccessibilityService) CalcMotStatusVariant.Active else CalcMotStatusVariant.Error,
                    onAction = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                )

                Spacer(modifier = Modifier.weight(1f))

                Column(verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)) {
                    CalcMotButton(
                        text = "Atualizar estado",
                        onClick = onPermissionsRefresh,
                        modifier = Modifier.fillMaxWidth(),
                        variant = CalcMotButtonVariant.Secondary
                    )

                    CalcMotButton(
                        text = "Concluir Onboarding",
                        onClick = onPermissionsRefresh,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = permissionState.hasAllRequiredPermissions
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CalcMotButton(
                        text = "Privacidade",
                        onClick = { showPrivacyPolicy = true },
                        variant = CalcMotButtonVariant.Ghost
                    )
                    CalcMotButton(
                        text = "Suporte",
                        onClick = { uriHandler.openUri("mailto:$CALCMOT_SUPPORT_EMAIL") },
                        variant = CalcMotButtonVariant.Ghost
                    )
                }
            }
        }
    }
}

@Composable
private fun DisclosureCard() {
    CalcMotCard(modifier = Modifier.testTag(UiTestTags.ACCESSIBILITY_DISCLOSURE)) {
        Column(verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)) {
            Text(
                text = "Como o CalcMot funciona",
                style = CalcMotTypography.CardTitle,
                color = CalcMotColors.TextPrimary
            )
            Text(
                text = "O CalcMot lê cards de oferta visíveis para calcular rentabilidade real. O processamento acontece 100% no seu aparelho.",
                style = CalcMotTypography.Body,
                color = CalcMotColors.TextSecondary
            )
            Text(
                text = "Não aceitamos nem recusamos corridas automaticamente. O controle é sempre seu.",
                style = CalcMotTypography.Caption,
                color = CalcMotColors.TextMuted
            )
        }
    }
}

@Composable
private fun PermissionStatusCard(
    title: String,
    status: String,
    variant: CalcMotStatusVariant,
    onAction: () -> Unit
) {
    CalcMotCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = CalcMotTypography.CardTitle,
                modifier = Modifier.weight(1f)
            )
            CalcMotStatusBadge(text = status, variant = variant)
        }
        Spacer(modifier = Modifier.height(CalcMotSpacing.Md))
        CalcMotButton(
            text = if (variant == CalcMotStatusVariant.Active) "Já ativado" else "Ativar agora",
            onClick = onAction,
            modifier = Modifier.fillMaxWidth(),
            enabled = variant != CalcMotStatusVariant.Active
        )
    }
}
