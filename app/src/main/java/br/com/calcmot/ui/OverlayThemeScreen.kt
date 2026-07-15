package br.com.calcmot.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.calcmot.OverlayThemePreference
import br.com.calcmot.overlay.CalcMotOverlayContainer
import br.com.calcmot.overlay.OfferDecisionHeader
import br.com.calcmot.overlay.OverlayMetricSummary
import br.com.calcmot.overlay.OverlayOfferQuality
import br.com.calcmot.ui.design.components.CalcMotButton
import br.com.calcmot.ui.design.theme.CalcMotTheme
import br.com.calcmot.ui.design.tokens.CalcMotColors

@Composable
fun OverlayThemeScreen(
    currentTheme: OverlayThemePreference,
    onBack: () -> Unit,
    onSave: (OverlayThemePreference) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTheme by remember(currentTheme) { mutableStateOf(currentTheme) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CalcMotColors.AppBackground)
            .testTag(UiTestTags.OVERLAY_THEME_SCREEN)
    ) {
        OverlayThemeBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OverlayThemeTopBar(onBack = onBack)
            Icon(
                imageVector = Icons.Outlined.Palette,
                contentDescription = null,
                tint = CalcMotColors.Success,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = "Escolha o tema do aviso",
                color = CalcMotColors.TextPrimary,
                fontSize = 27.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Use o estilo mais confortável para ler durante a oferta.",
                color = CalcMotColors.TextSecondary,
                fontSize = 17.sp,
                lineHeight = 23.sp,
                textAlign = TextAlign.Center
            )
            OverlayPreviewStage(theme = selectedTheme)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = CalcMotColors.BorderSubtle,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .background(
                        color = CalcMotColors.Surface,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(vertical = 4.dp)
            ) {
                OverlayThemePreference.entries.forEachIndexed { index, theme ->
                    OverlayThemeOption(
                        theme = theme,
                        selected = selectedTheme == theme,
                        onClick = { selectedTheme = theme }
                    )
                    if (index != OverlayThemePreference.entries.lastIndex) {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp)
                                .height(1.dp)
                                .background(CalcMotColors.BorderSubtle)
                        )
                    }
                }
            }
            CalcMotButton(
                text = "Salvar tema",
                onClick = { onSave(selectedTheme) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 58.dp)
                    .testTag(UiTestTags.OVERLAY_THEME_SAVE_BUTTON)
            )
            androidx.compose.material3.TextButton(
                onClick = { selectedTheme = OverlayThemePreference.CLASSIC },
                modifier = Modifier.testTag(UiTestTags.OVERLAY_THEME_DEFAULT_BUTTON)
            ) {
                Text(
                    text = "Usar tema padrão",
                    color = CalcMotColors.BrandSecondary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun OverlayThemeTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Voltar",
                tint = CalcMotColors.TextPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = "Tema do aviso",
            modifier = Modifier.padding(start = 12.dp),
            color = CalcMotColors.TextPrimary,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun OverlayPreviewStage(theme: OverlayThemePreference) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 164.dp)
            .border(
                BorderStroke(1.dp, CalcMotColors.BorderSubtle),
                RoundedCornerShape(16.dp)
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF111820), Color(0xFF080C11))
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        CalcMotOverlayContainer(
            quality = OverlayOfferQuality.GOOD,
            theme = theme,
            modifier = Modifier.testTag(UiTestTags.OVERLAY_THEME_PREVIEW)
        ) {
            OfferDecisionHeader(
                quality = OverlayOfferQuality.GOOD,
                theme = theme
            )
            OverlayMetricSummary(
                perKm = "R$ 6,94/km",
                perHour = "R$ 107,14/h",
                duration = "7 min",
                quality = OverlayOfferQuality.GOOD,
                theme = theme
            )
        }
    }
}

@Composable
private fun OverlayThemeOption(
    theme: OverlayThemePreference,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick
            )
            .testTag(theme.testTag)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = theme.label,
                color = CalcMotColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = theme.description,
                color = CalcMotColors.TextSecondary,
                fontSize = 14.sp,
                lineHeight = 18.sp
            )
        }
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = CalcMotColors.Success,
                unselectedColor = CalcMotColors.TextSecondary
            )
        )
    }
}

@Composable
private fun OverlayThemeBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        CalcMotColors.Success.copy(alpha = 0.055f),
                        Color.Transparent
                    )
                )
            )
    )
}

private val OverlayThemePreference.description: String
    get() = when (this) {
        OverlayThemePreference.CLASSIC -> "Escuro, discreto e já usado pelo CalcMot"
        OverlayThemePreference.OUTLINED -> "Fundo claro com borda na cor da classificação"
        OverlayThemePreference.SOLID -> "Cor da classificação em toda a superfície"
    }

private val OverlayThemePreference.testTag: String
    get() = when (this) {
        OverlayThemePreference.CLASSIC -> UiTestTags.OVERLAY_THEME_CLASSIC
        OverlayThemePreference.OUTLINED -> UiTestTags.OVERLAY_THEME_OUTLINED
        OverlayThemePreference.SOLID -> UiTestTags.OVERLAY_THEME_SOLID
    }

@Preview(name = "Tema do aviso", widthDp = 360, heightDp = 800)
@Composable
private fun OverlayThemeScreenPreview() {
    CalcMotTheme {
        OverlayThemeScreen(
            currentTheme = OverlayThemePreference.OUTLINED,
            onBack = {},
            onSave = {}
        )
    }
}

@Preview(name = "Tema do aviso - fonte grande", widthDp = 360, heightDp = 800, fontScale = 1.3f)
@Composable
private fun OverlayThemeScreenLargeFontPreview() {
    CalcMotTheme {
        OverlayThemeScreen(
            currentTheme = OverlayThemePreference.SOLID,
            onBack = {},
            onSave = {}
        )
    }
}
