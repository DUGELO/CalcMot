package br.com.calcmot.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import br.com.calcmot.ExecutionPowerState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.calcmot.ReadingPipelineRuntime
import br.com.calcmot.ui.design.components.CalcMotButton
import br.com.calcmot.ui.design.components.CalcMotButtonVariant
import br.com.calcmot.ui.design.components.CalcMotCard
import br.com.calcmot.ui.design.components.CalcMotCardVariant
import br.com.calcmot.ui.design.components.CalcMotSwitchRow
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotSpacing
import br.com.calcmot.ui.design.tokens.CalcMotTypography
import java.text.DateFormat
import java.util.Date

@Composable
internal fun ReadingDiagnosticsScreen(
    accessibilityActive: Boolean,
    diagnosticsEnabled: Boolean,
    onBack: () -> Unit,
    onDiagnosticsEnabledChange: (Boolean) -> Unit,
    onCopy: () -> Unit,
    onRestart: () -> Unit
) {
    val context = LocalContext.current
    val snapshot by ReadingPipelineRuntime.snapshot.collectAsState()
    val power by ExecutionPowerState.snapshot.collectAsState()
    var oemReviewed by remember { mutableStateOf(ExecutionPowerState.oemReviewed(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CalcMotColors.AppBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    tint = CalcMotColors.TextPrimary
                )
            }
            Text(
                text = "Diagnóstico de leitura",
                style = CalcMotTypography.ScreenTitle,
                color = CalcMotColors.TextPrimary
            )
        }

        Text(
            text = "Use estas informações apenas quando a leitura parar ou o suporte solicitar.",
            style = CalcMotTypography.Body,
            color = CalcMotColors.TextSecondary
        )

        CalcMotCard(variant = CalcMotCardVariant.HIGHLIGHT) {
            Column(
                modifier = Modifier.padding(CalcMotSpacing.CardPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = snapshot.pipelineState.label,
                    style = CalcMotTypography.ScreenTitle,
                    color = pipelineColor(snapshot.pipelineState),
                    fontWeight = FontWeight.Bold
                )
                DiagnosticRow("Plataforma selecionada", snapshot.selectedPlatform.displayName)
                DiagnosticRow("Acessibilidade", activeLabel(accessibilityActive))
                DiagnosticRow("Overlay", snapshot.overlayStatus.label)
                DiagnosticRow("Bateria", power.optimizationLabel())
                DiagnosticRow("Background restrito", power.backgroundRestricted?.let { if (it) "Sim" else "Não" } ?: "Desconhecido")
                DiagnosticRow("Economia de energia", power.powerSave?.let { if (it) "Ativa" else "Inativa" } ?: "Desconhecida")
                DiagnosticRow("Carregando", power.charging?.let { if (it) "Sim" else "Não" } ?: "Desconhecido")
                DiagnosticRow("App standby", power.standbyBucket?.toString() ?: "Desconhecido")
                DiagnosticRow("Configuração OEM", if (oemReviewed) "Conferida pelo motorista; não verificada por API" else "Não verificada")
                DiagnosticRow("Serviço", if (snapshot.serviceConnected) "Conectado" else "Desconectado")
            }
        }

        CalcMotCard {
            Column(
                modifier = Modifier.padding(CalcMotSpacing.CardPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Atividade",
                    style = CalcMotTypography.CardTitle,
                    color = CalcMotColors.TextPrimary
                )
                DiagnosticRow("Última leitura bem-sucedida", formatTimestamp(snapshot.lastSuccessfulReadAtMillis))
                DiagnosticRow("Último evento do motorista", formatTimestamp(snapshot.lastDriverEventAtMillis))
                DiagnosticRow("Último reset", formatTimestamp(snapshot.lastResetAtMillis))
                DiagnosticRow("Último erro", snapshot.lastError ?: "Nenhum")
                DiagnosticRow(
                    "Aparelho",
                    "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}"
                )
            }
        }

        CalcMotCard {
            Column(
                modifier = Modifier.padding(CalcMotSpacing.CardPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CalcMotSwitchRow(
                    title = "Manter diagnóstico visível",
                    description = "Quando desligado, a tela volta a ficar oculta no app.",
                    checked = diagnosticsEnabled,
                    onCheckedChange = onDiagnosticsEnabledChange
                )
            }
        }

        if (!oemReviewed || (!snapshot.serviceConnected || snapshot.overlayStatus == ReadingPipelineRuntime.OverlayStatus.FAILED) && power.identifiedRestriction()) {
            Text(text = ExecutionPowerState.oemGuide(power.manufacturer), style = CalcMotTypography.Body,
                color = CalcMotColors.TextSecondary)
            CalcMotButton(text = "Abrir configurações de bateria", onClick = { ExecutionPowerState.openBatterySettings(context) },
                modifier = Modifier.fillMaxWidth(), variant = CalcMotButtonVariant.SECONDARY)
            if (!oemReviewed) {
                CalcMotButton(text = "Já conferi as configurações do fabricante", onClick = {
                    ExecutionPowerState.confirmOemReview(context)
                    oemReviewed = true
                }, modifier = Modifier.fillMaxWidth(), variant = CalcMotButtonVariant.SECONDARY)
            }
        }
        CalcMotButton(
            text = "Reiniciar leitura",
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth()
        )
        CalcMotButton(
            text = "Copiar diagnóstico",
            onClick = onCopy,
            modifier = Modifier.fillMaxWidth(),
            variant = CalcMotButtonVariant.SECONDARY
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Refresh, contentDescription = null, tint = CalcMotColors.TextSecondary)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "O reinício não altera suas metas ou cálculos.",
                style = CalcMotTypography.Caption,
                color = CalcMotColors.TextSecondary
            )
        }
        Spacer(Modifier.padding(bottom = 8.dp))
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = CalcMotTypography.Body,
            color = CalcMotColors.TextSecondary
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = CalcMotTypography.Body,
            color = CalcMotColors.TextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun pipelineColor(state: ReadingPipelineRuntime.PipelineState) = when (state) {
    ReadingPipelineRuntime.PipelineState.FAILED -> CalcMotColors.Danger
    ReadingPipelineRuntime.PipelineState.BUSY,
    ReadingPipelineRuntime.PipelineState.CAPTURING,
    ReadingPipelineRuntime.PipelineState.OCR -> CalcMotColors.Warning
    ReadingPipelineRuntime.PipelineState.IDLE -> CalcMotColors.Success
}

private fun activeLabel(active: Boolean): String = if (active) "Ativa" else "Inativa"

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return "Ainda não houve"
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date(timestamp))
}
