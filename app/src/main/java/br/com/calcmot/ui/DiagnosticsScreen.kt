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
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Textsms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import br.com.calcmot.AppDiagnostics
import br.com.calcmot.ui.design.theme.CalcMotTheme
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotShape

@Composable
fun DiagnosticsScreen(
    snapshot: AppDiagnostics.Snapshot,
    hasAccessibility: Boolean,
    monitoringEnabled: Boolean,
    goalPerKm: String,
    goalPerHour: String,
    onBack: () -> Unit,
    onOpenDriverApp: () -> Unit,
    onOpenHelp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CalcMotColors.AppBackground)
            .testTag(UiTestTags.DIAGNOSTICS_SCREEN)
    ) {
        DiagnosticsBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            DiagnosticsTopBar(onBack = onBack)
            DiagnosticsHeroCard(
                ready = hasAccessibility && monitoringEnabled,
                modifier = Modifier.fillMaxWidth()
            )
            DiagnosticsSectionTitle(
                text = "Verificações",
                icon = Icons.Outlined.Textsms
            )
            DiagnosticsChecksCard(
                hasAccessibility = hasAccessibility,
                monitoringEnabled = monitoringEnabled,
                goalPerKm = goalPerKm,
                goalPerHour = goalPerHour,
                modifier = Modifier.fillMaxWidth()
            )
            DiagnosticsSectionTitle(
                text = "Teste rápido",
                icon = Icons.Outlined.HourglassEmpty
            )
            DiagnosticsQuickTestCard(
                onOpenDriverApp = onOpenDriverApp,
                onOpenHelp = onOpenHelp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp)
            )
        }
    }
}

@Composable
private fun DiagnosticsTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Voltar",
                tint = CalcMotColors.TextPrimary,
                modifier = Modifier.size(31.dp)
            )
        }
        Text(
            text = "Diagnóstico",
            modifier = Modifier.padding(start = 18.dp),
            color = CalcMotColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DiagnosticsHeroCard(
    ready: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(
                BorderStroke(1.25.dp, CalcMotColors.Success.copy(alpha = 0.78f)),
                RoundedCornerShape(20.dp)
            )
            .background(
                Brush.verticalGradient(
                    listOf(
                        CalcMotColors.SurfaceElevated.copy(alpha = 0.76f),
                        CalcMotColors.Surface.copy(alpha = 0.88f)
                    )
                ),
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 22.dp, vertical = 24.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Speed,
            contentDescription = null,
            tint = CalcMotColors.Success.copy(alpha = 0.14f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(128.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
        Box(modifier = Modifier.size(118.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            listOf(CalcMotColors.Success.copy(alpha = 0.18f), Color.Transparent)
                        )
                    )
            )
            Icon(
                imageVector = Icons.Outlined.Security,
                contentDescription = null,
                tint = CalcMotColors.Success,
                modifier = Modifier.size(96.dp)
            )
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = CalcMotColors.Success,
                modifier = Modifier.size(44.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (ready) "Tudo pronto" else "Ajuste pendente",
                color = CalcMotColors.TextPrimary,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 30.sp
            )
            Text(
                text = if (ready) {
                    "O CalcMot está configurado para mostrar o semáforo de lucro quando uma oferta aparecer."
                } else {
                    "Revise os itens abaixo para deixar o CalcMot pronto para trabalhar."
                },
                color = CalcMotColors.TextSecondary,
                fontSize = 16.sp,
                lineHeight = 24.sp
            )
            DiagnosticsStatusBadge(text = if (ready) "Pronto para uso" else "Ação necessária")
        }
        }
    }
}

@Composable
private fun DiagnosticsStatusBadge(text: String) {
    Row(
        modifier = Modifier
            .border(BorderStroke(1.dp, CalcMotColors.Success.copy(alpha = 0.6f)), RoundedCornerShape(9.dp))
            .background(CalcMotColors.Success.copy(alpha = 0.08f), RoundedCornerShape(9.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = CalcMotColors.Success,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            color = CalcMotColors.Success,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DiagnosticsSectionTitle(text: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CalcMotColors.Success,
            modifier = Modifier.size(30.dp)
        )
        Text(
            text = text,
            color = CalcMotColors.TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DiagnosticsChecksCard(
    hasAccessibility: Boolean,
    monitoringEnabled: Boolean,
    goalPerKm: String,
    goalPerHour: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .border(BorderStroke(1.dp, CalcMotColors.BorderStrong), RoundedCornerShape(CalcMotShape.Lg))
            .background(CalcMotColors.Surface.copy(alpha = 0.76f), RoundedCornerShape(CalcMotShape.Lg))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        DiagnosticsCheckRow(
            icon = Icons.Outlined.PersonOutline,
            title = "Permissão de acessibilidade",
            subtitle = if (hasAccessibility) "Ativada" else "Pendente",
            checked = hasAccessibility
        )
        DividerLine()
        DiagnosticsCheckRow(
            icon = Icons.Outlined.Bolt,
            title = "Cálculo automático",
            subtitle = if (monitoringEnabled) "Ligado" else "Pausado",
            checked = monitoringEnabled
        )
        DividerLine()
        DiagnosticsCheckRow(
            icon = Icons.Outlined.Textsms,
            title = "Aviso flutuante",
            subtitle = "Pronto para aparecer sobre as ofertas",
            checked = true
        )
        DividerLine()
        DiagnosticsCheckRow(
            icon = Icons.Outlined.MyLocation,
            title = "Meta configurada",
            subtitle = "$goalPerKm · $goalPerHour",
            checked = true
        )
        DividerLine()
        DiagnosticsCheckRow(
            icon = Icons.Outlined.DirectionsCar,
            title = "Apps compatíveis",
            subtitle = "Uber Driver e 99 Motorista",
            checked = null
        )
    }
}

@Composable
private fun DiagnosticsCheckRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(CalcMotColors.Success.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CalcMotColors.Success,
                modifier = Modifier.size(27.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                color = CalcMotColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = CalcMotColors.TextSecondary,
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
        }
        Icon(
            imageVector = if (checked == null) Icons.Outlined.Info else Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = if (checked == false) CalcMotColors.Warning else if (checked == null) Color(0xFFAFC7F8) else CalcMotColors.Success,
            modifier = Modifier.size(30.dp)
        )
    }
}

@Composable
private fun DiagnosticsQuickTestCard(
    onOpenDriverApp: () -> Unit,
    onOpenHelp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .border(BorderStroke(1.dp, CalcMotColors.BorderStrong), RoundedCornerShape(CalcMotShape.Lg))
            .background(CalcMotColors.Surface.copy(alpha = 0.72f), RoundedCornerShape(CalcMotShape.Lg))
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Abra seu app de motorista e aguarde uma oferta.\nQuando ela aparecer, o CalcMot mostrará o aviso automaticamente.",
            color = CalcMotColors.TextSecondary,
            fontSize = 16.sp,
            lineHeight = 23.sp
        )
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            onClick = onOpenDriverApp,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CalcMotColors.PrimaryActionBlue,
                contentColor = CalcMotColors.TextPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.DirectionsCar,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "Abrir app de motorista",
                modifier = Modifier.padding(start = 12.dp),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
        TextButton(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = onOpenHelp
        ) {
            Text(
                text = "Ver ajuda",
                color = CalcMotColors.BrandSecondary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(CalcMotColors.BorderSubtle)
    )
}

@Composable
private fun DiagnosticsBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(
                        CalcMotColors.Success.copy(alpha = 0.07f),
                        CalcMotColors.AppBackground.copy(alpha = 0.8f),
                        CalcMotColors.AppBackground
                    )
                )
            )
    )
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun DiagnosticsScreenPreview() {
    CalcMotTheme {
        DiagnosticsScreen(
            snapshot = AppDiagnostics.Snapshot(
                eventCount = 0,
                lastEventType = 0,
                lastStage = AppDiagnostics.Stage.NEVER,
                lastUpdatedAt = 0,
                treeCandidateCount = 0,
                firstFrameCount = 0,
                stableOfferCount = 0,
                overlayShownCount = 0,
                overlayErrorCount = 0,
                frameRejectedCount = 0,
                treeRootsSeenCount = 0,
                treeTextsSeenCount = 0,
                treePriceSeenCount = 0,
                treeButtonSeenCount = 0,
                treeBlocksSeenCount = 0,
                treeRejectedCount = 0,
                uiautomatorCompleteCards = 0,
                internalTreeCompleteCards = 0,
                uiautomatorRejectedFrames = 0,
                internalTreeRejectedFrames = 0,
                lastCaptureSource = null,
                lastCaptureRejectionReason = null
            ),
            hasAccessibility = true,
            monitoringEnabled = true,
            goalPerKm = "R$ 1,80/km",
            goalPerHour = "R$ 35/h",
            onBack = {},
            onOpenDriverApp = {},
            onOpenHelp = {}
        )
    }
}
