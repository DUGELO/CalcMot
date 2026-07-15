package br.com.calcmot.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.calcmot.ui.design.components.CalcMotFeedbackAction
import br.com.calcmot.ui.design.components.CalcMotFeedbackNavigation
import br.com.calcmot.ui.design.components.CalcMotFeedbackSheet
import br.com.calcmot.ui.design.components.CalcMotFeedbackTone
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotTypography
import kotlinx.coroutines.delay

private data class AccessibilityGuideStep(
    val screenTitle: String,
    val instruction: String
)

private val accessibilityGuideSteps = listOf(
    AccessibilityGuideStep(
        screenTitle = "Acessibilidade",
        instruction = "Toque em Aplicativos instalados"
    ),
    AccessibilityGuideStep(
        screenTitle = "Aplicativos instalados",
        instruction = "Selecione CalcMot"
    ),
    AccessibilityGuideStep(
        screenTitle = "CalcMot",
        instruction = "Ative Usar o CalcMot e confirme"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccessibilityActivationGuideSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    onOpenSettings: () -> Unit
) {
    CalcMotFeedbackSheet(
        visible = visible,
        title = "Como ativar",
        subtitle = "Siga estes três passos nas configurações do Android.",
        onDismissRequest = onDismissRequest,
        modifier = Modifier.testTag(UiTestTags.ACCESSIBILITY_GUIDE_SHEET),
        tone = CalcMotFeedbackTone.INFO,
        navigation = CalcMotFeedbackNavigation.CLOSE,
        heroIcon = null,
        primaryAction = CalcMotFeedbackAction(
            label = "Abrir configurações",
            onClick = onOpenSettings
        ),
        footer = {
            Text(
                text = "Os nomes podem variar um pouco conforme o celular.",
                modifier = Modifier.fillMaxWidth(),
                color = CalcMotColors.TextSecondary,
                style = CalcMotTypography.Caption,
                textAlign = TextAlign.Center
            )
        }
    ) {
        AccessibilityActivationGuide()
    }
}

@Composable
private fun AccessibilityActivationGuide() {
    var currentStep by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_800)
            currentStep = (currentStep + 1) % accessibilityGuideSteps.size
        }
    }

    val step = accessibilityGuideSteps[currentStep]
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription =
                    "Tutorial para ativar acessibilidade. Primeiro Aplicativos instalados, depois CalcMot, por fim Usar o CalcMot."
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AndroidSettingsFrame(currentStep = currentStep, title = step.screenTitle)
        Text(
            text = "${currentStep + 1}. ${step.instruction}",
            color = CalcMotColors.TextPrimary,
            style = CalcMotTypography.CardTitle,
            textAlign = TextAlign.Center
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            accessibilityGuideSteps.indices.forEach { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == currentStep) 9.dp else 7.dp)
                        .background(
                            color = if (index == currentStep) {
                                CalcMotColors.Success
                            } else {
                                CalcMotColors.BorderStrong
                            },
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@Composable
private fun AndroidSettingsFrame(currentStep: Int, title: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(188.dp),
        shape = RoundedCornerShape(20.dp),
        color = CalcMotColors.AppBackground,
        border = BorderStroke(1.dp, CalcMotColors.BorderStrong)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = CalcMotColors.TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = title,
                    color = CalcMotColors.TextPrimary,
                    style = CalcMotTypography.CardTitle
                )
            }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(CalcMotColors.BorderSubtle)
            )
            when (currentStep) {
                0 -> {
                    SettingsGuideRow(
                        icon = Icons.Outlined.AccessibilityNew,
                        label = "Leitor de tela"
                    )
                    SettingsGuideRow(
                        icon = Icons.Outlined.Apps,
                        label = "Aplicativos instalados",
                        highlighted = true
                    )
                }

                1 -> {
                    SettingsGuideRow(
                        icon = Icons.Outlined.AccessibilityNew,
                        label = "CalcMot",
                        supportingText = "Desativado",
                        highlighted = true
                    )
                    SettingsGuideRow(
                        icon = Icons.Outlined.Settings,
                        label = "Outros serviços"
                    )
                }

                else -> {
                    SettingsGuideRow(
                        icon = Icons.Outlined.AccessibilityNew,
                        label = "Usar o CalcMot",
                        supportingText = "Ativado",
                        supportingColor = CalcMotColors.Success,
                        highlighted = true,
                        trailing = {
                            Switch(checked = true, onCheckedChange = null)
                        }
                    )
                    Text(
                        text = "Confirme quando o Android solicitar.",
                        modifier = Modifier.padding(horizontal = 12.dp),
                        color = CalcMotColors.TextSecondary,
                        style = CalcMotTypography.Caption
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsGuideRow(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    supportingColor: Color = CalcMotColors.TextSecondary,
    highlighted: Boolean = false,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .border(
                width = 1.dp,
                color = if (highlighted) CalcMotColors.BrandSecondary else CalcMotColors.BorderSubtle,
                shape = RoundedCornerShape(14.dp)
            )
            .background(
                color = if (highlighted) {
                    CalcMotColors.BrandSecondary.copy(alpha = 0.12f)
                } else {
                    CalcMotColors.Surface.copy(alpha = 0.6f)
                },
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (highlighted) CalcMotColors.BrandSecondary else CalcMotColors.TextSecondary,
            modifier = Modifier.size(25.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = CalcMotColors.TextPrimary,
                style = CalcMotTypography.CardTitle
            )
            supportingText?.let {
                Text(
                    text = it,
                    color = supportingColor,
                    style = CalcMotTypography.Caption
                )
            }
        }
        if (trailing != null) {
            trailing()
        } else if (highlighted) {
            Icon(
                imageVector = Icons.Outlined.TouchApp,
                contentDescription = null,
                tint = CalcMotColors.BrandSecondary,
                modifier = Modifier.size(27.dp)
            )
        }
    }
}
