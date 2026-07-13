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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ExpandCircleDown
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.calcmot.OverlayPositionPreference
import br.com.calcmot.ui.design.theme.CalcMotTheme
import br.com.calcmot.ui.design.tokens.CalcMotColors

@Composable
fun OverlayPositionScreen(
    currentPosition: OverlayPositionPreference,
    onBack: () -> Unit,
    onSave: (OverlayPositionPreference) -> Unit,
    modifier: Modifier = Modifier
) {
    var selected by remember(currentPosition) { mutableStateOf(currentPosition) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CalcMotColors.AppBackground)
            .testTag(UiTestTags.OVERLAY_POSITION_SCREEN)
    ) {
        OverlayPositionBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OverlayPositionTopBar(onBack = onBack)
            OverlayPositionHeader()
            PhonePreview(selected = selected)
            PositionSelector(
                selected = selected,
                onSelected = { selected = it }
            )
            PositionTipCard()
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 58.dp)
                    .testTag(UiTestTags.OVERLAY_POSITION_SAVE_BUTTON),
                onClick = { onSave(selected) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CalcMotColors.PrimaryActionBlue,
                    contentColor = CalcMotColors.TextPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Save,
                    contentDescription = null,
                    modifier = Modifier.size(25.dp)
                )
                Text(
                    text = "Salvar posição",
                    modifier = Modifier.padding(start = 14.dp),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            TextButton(
                modifier = Modifier.testTag(UiTestTags.OVERLAY_POSITION_DEFAULT_BUTTON),
                onClick = { selected = OverlayPositionPreference.HIGH }
            ) {
                Text(
                    text = "Usar posição padrão",
                    color = CalcMotColors.BrandSecondary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun OverlayPositionTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            modifier = Modifier.size(48.dp),
            onClick = onBack
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Voltar",
                tint = CalcMotColors.TextPrimary,
                modifier = Modifier.size(31.dp)
            )
        }
        Text(
            text = "Posição do aviso",
            modifier = Modifier.padding(start = 18.dp),
            color = CalcMotColors.TextPrimary,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun OverlayPositionHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Escolha onde o aviso aparece",
            color = CalcMotColors.TextPrimary,
            fontSize = 28.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Deixe o semáforo em uma posição confortável para ler durante a oferta.",
            color = CalcMotColors.TextSecondary,
            fontSize = 17.sp,
            lineHeight = 23.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PhonePreview(selected: OverlayPositionPreference) {
    Box(
        modifier = Modifier
            .size(width = 218.dp, height = 318.dp)
            .border(BorderStroke(3.dp, CalcMotColors.BorderStrong), RoundedCornerShape(32.dp))
            .background(CalcMotColors.Surface.copy(alpha = 0.66f), RoundedCornerShape(32.dp))
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(14.dp)
                .background(CalcMotColors.BorderStrong.copy(alpha = 0.65f), CircleShape)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 30.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PreviewLine(widthFraction = 0.58f)
            PreviewLine(widthFraction = 0.78f)
            PreviewBlock()
            PreviewBlock()
            PreviewBlock()
            Spacer(modifier = Modifier.weight(1f))
            PreviewLine(widthFraction = 0.82f, height = 28.dp)
        }
        OfferPreviewCard(
            modifier = Modifier.align(selected.previewAlignment)
        )
    }
}

@Composable
private fun OfferPreviewCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth(0.86f)
            .border(BorderStroke(1.dp, CalcMotColors.Success.copy(alpha = 0.85f)), RoundedCornerShape(12.dp))
            .background(CalcMotColors.SurfaceElevated.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = CalcMotColors.Success,
            modifier = Modifier.size(33.dp)
        )
        Box(
            modifier = Modifier
                .height(44.dp)
                .background(CalcMotColors.BorderSubtle)
                .padding(horizontal = 1.dp)
        )
        Column {
            Text(
                text = "BOA",
                color = CalcMotColors.Success,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(text = "R$ 2,10/km", color = CalcMotColors.TextPrimary, fontSize = 13.sp, maxLines = 1)
            Text(text = "R$ 48/h", color = CalcMotColors.TextPrimary, fontSize = 13.sp, maxLines = 1)
        }
    }
}

@Composable
private fun PreviewLine(widthFraction: Float, height: androidx.compose.ui.unit.Dp = 10.dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .background(CalcMotColors.SurfaceSoft.copy(alpha = 0.9f), RoundedCornerShape(100.dp))
    )
}

@Composable
private fun PreviewBlock() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(CalcMotColors.SurfaceSoft.copy(alpha = 0.9f), CircleShape)
        )
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            PreviewLine(widthFraction = 0.62f, height = 11.dp)
            PreviewLine(widthFraction = 0.42f, height = 9.dp)
        }
    }
}

@Composable
private fun PositionSelector(
    selected: OverlayPositionPreference,
    onSelected: (OverlayPositionPreference) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, CalcMotColors.BorderStrong), RoundedCornerShape(18.dp))
            .background(CalcMotColors.Surface.copy(alpha = 0.72f), RoundedCornerShape(18.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PositionOption.entries.forEach { option ->
            PositionSelectorItem(
                modifier = Modifier.weight(1f),
                option = option,
                selected = selected == option.position,
                onClick = { onSelected(option.position) }
            )
            if (option != PositionOption.entries.last()) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(56.dp)
                        .background(CalcMotColors.BorderSubtle)
                )
            }
        }
    }
}

@Composable
private fun PositionSelectorItem(
    option: PositionOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .heightIn(min = 78.dp)
            .then(if (selected) Modifier.border(BorderStroke(1.dp, CalcMotColors.Success), RoundedCornerShape(18.dp)) else Modifier)
            .background(
                color = if (selected) CalcMotColors.Success.copy(alpha = 0.08f) else Color.Transparent,
                shape = RoundedCornerShape(18.dp)
            )
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .testTag(option.testTag)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = option.icon,
            contentDescription = null,
            tint = if (selected) CalcMotColors.Success else CalcMotColors.TextSecondary,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = option.label,
            color = if (selected) CalcMotColors.Success else CalcMotColors.TextSecondary,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun PositionTipCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, CalcMotColors.BorderStrong), RoundedCornerShape(16.dp))
            .background(CalcMotColors.Surface.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Lightbulb,
            contentDescription = null,
            tint = CalcMotColors.Success,
            modifier = Modifier
                .size(46.dp)
                .background(CalcMotColors.Success.copy(alpha = 0.12f), CircleShape)
                .padding(10.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = "Dica",
                color = CalcMotColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Escolha uma posição que não cubra botões importantes do app de motorista.",
                color = CalcMotColors.TextSecondary,
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun OverlayPositionBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(
                        CalcMotColors.Success.copy(alpha = 0.055f),
                        CalcMotColors.AppBackground.copy(alpha = 0.82f),
                        CalcMotColors.AppBackground
                    )
                )
            )
    )
}

private enum class PositionOption(
    val position: OverlayPositionPreference,
    val label: String,
    val icon: ImageVector,
    val testTag: String
) {
    TOP(OverlayPositionPreference.HIGH, "Topo", Icons.Outlined.MyLocation, UiTestTags.OVERLAY_POSITION_HIGH),
    CENTER(OverlayPositionPreference.MEDIUM, "Centro", Icons.Outlined.RadioButtonChecked, UiTestTags.OVERLAY_POSITION_MEDIUM),
    BOTTOM(OverlayPositionPreference.LOW, "Inferior", Icons.Outlined.ExpandCircleDown, UiTestTags.OVERLAY_POSITION_LOW)
}

private val OverlayPositionPreference.previewAlignment: Alignment
    get() = when (this) {
        OverlayPositionPreference.HIGH -> Alignment.TopCenter
        OverlayPositionPreference.MEDIUM -> Alignment.Center
        OverlayPositionPreference.LOW -> Alignment.BottomCenter
    }

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun OverlayPositionScreenPreview() {
    CalcMotTheme {
        OverlayPositionScreen(
            currentPosition = OverlayPositionPreference.HIGH,
            onBack = {},
            onSave = {}
        )
    }
}
