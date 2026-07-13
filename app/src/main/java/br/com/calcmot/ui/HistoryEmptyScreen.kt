package br.com.calcmot.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.calcmot.ui.design.theme.CalcMotTheme
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotShape

@Composable
fun HistoryEmptyScreen(
    onBack: () -> Unit,
    onOpenDriverApp: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CalcMotColors.AppBackground)
            .testTag(UiTestTags.HISTORY_EMPTY_SCREEN)
    ) {
        HistoryBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HistoryTopBar(onBack = onBack)

            HistoryHeroIllustration(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(184.dp)
            )

            Text(
                text = buildAnnotatedString {
                    append("Nenhuma oferta\nanalisada ")
                    withStyle(SpanStyle(color = CalcMotColors.Success)) {
                        append("ainda")
                    }
                },
                color = CalcMotColors.TextPrimary,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 30.sp
            )
            Text(
                text = "Quando o CalcMot identificar uma oferta,\nela aparecerá aqui com R$/km, R$/h e classificação.",
                color = CalcMotColors.TextSecondary,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 21.sp
            )

            HistoryStartCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            )

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag(UiTestTags.HISTORY_OPEN_DRIVER_BUTTON),
                onClick = onOpenDriverApp,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CalcMotColors.PrimaryActionBlue,
                    contentColor = CalcMotColors.TextPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhoneAndroid,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Abrir app de motorista",
                    modifier = Modifier.padding(start = 14.dp),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            TextButton(
                modifier = Modifier.testTag(UiTestTags.HISTORY_DIAGNOSTICS_BUTTON),
                onClick = onOpenDiagnostics
            ) {
                Text(
                    text = "Ver diagnóstico",
                    color = CalcMotColors.BrandSecondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            HistoryPrivacyFooter(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 0.dp, bottom = 14.dp)
            )
        }
    }
}

@Composable
private fun HistoryTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Voltar",
                tint = CalcMotColors.TextPrimary,
                modifier = Modifier.size(30.dp)
            )
        }
        Text(
            text = "Histórico",
            modifier = Modifier.padding(start = 12.dp),
            color = CalcMotColors.TextPrimary,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun HistoryHeroIllustration(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape)
                .border(1.dp, CalcMotColors.BorderSubtle, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(142.dp)
                .clip(CircleShape)
                .border(1.2.dp, CalcMotColors.BorderStrong, CircleShape)
        )
        Icon(
            imageVector = Icons.Outlined.Speed,
            contentDescription = null,
            tint = CalcMotColors.BorderStrong.copy(alpha = 0.88f),
            modifier = Modifier.size(130.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 24.dp)
                .size(82.dp)
                .border(
                    BorderStroke(1.2.dp, CalcMotColors.BorderStrong),
                    RoundedCornerShape(18.dp)
                )
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            CalcMotColors.SurfaceSoft.copy(alpha = 0.84f),
                            CalcMotColors.Surface.copy(alpha = 0.92f)
                        )
                    ),
                    RoundedCornerShape(18.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.FactCheck,
                contentDescription = null,
                tint = CalcMotColors.TextMuted,
                modifier = Modifier.size(52.dp)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 4.dp, bottom = 16.dp)
                .size(46.dp)
                .clip(CircleShape)
                .background(CalcMotColors.Surface.copy(alpha = 0.94f))
                .border(3.dp, CalcMotColors.Success, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = CalcMotColors.Success,
                modifier = Modifier.size(31.dp)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp)
                .fillMaxWidth(0.52f)
                .height(14.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            CalcMotColors.Success.copy(alpha = 0.26f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
private fun HistoryStartCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .border(
                BorderStroke(1.dp, CalcMotColors.BorderSubtle),
                RoundedCornerShape(CalcMotShape.Lg)
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        CalcMotColors.SurfaceElevated.copy(alpha = 0.72f),
                        CalcMotColors.Surface.copy(alpha = 0.84f)
                    )
                ),
                RoundedCornerShape(CalcMotShape.Lg)
            )
            .padding(horizontal = 18.dp, vertical = 15.dp)
    ) {
        Text(
            text = "Como começar",
            color = CalcMotColors.TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
        HistoryStartRow(
            icon = Icons.Outlined.ToggleOn,
            text = "1. Ative o cálculo automático"
        )
        HistoryStartRow(
            icon = Icons.Outlined.PhoneAndroid,
            text = "2. Abra seu app de motorista"
        )
        HistoryStartRow(
            icon = Icons.Outlined.Timer,
            text = "3. Aguarde uma oferta aparecer",
            showDivider = false
        )
    }
}

@Composable
private fun HistoryStartRow(
    icon: ImageVector,
    text: String,
    showDivider: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(CalcMotColors.Success.copy(alpha = 0.11f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CalcMotColors.Success,
                    modifier = Modifier.size(27.dp)
                )
            }
            Text(
                text = text,
                color = CalcMotColors.TextPrimary,
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .padding(start = 64.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(CalcMotColors.BorderSubtle)
            )
        }
    }
}

@Composable
private fun HistoryPrivacyFooter(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            tint = CalcMotColors.Success,
            modifier = Modifier.size(34.dp)
        )
        Text(
            text = "O histórico não mostra endereços,\nnomes de passageiros ou dados sensíveis.",
            modifier = Modifier.padding(start = 18.dp),
            color = CalcMotColors.TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun HistoryBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CalcMotColors.Success.copy(alpha = 0.07f),
                        CalcMotColors.AppBackground.copy(alpha = 0.72f),
                        CalcMotColors.AppBackground
                    )
                )
            )
    )
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun HistoryEmptyScreenPreview() {
    CalcMotTheme {
        HistoryEmptyScreen(
            onBack = {},
            onOpenDriverApp = {},
            onOpenDiagnostics = {}
        )
    }
}
