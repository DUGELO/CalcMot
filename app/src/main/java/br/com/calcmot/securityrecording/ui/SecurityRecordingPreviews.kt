package br.com.calcmot.securityrecording.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import br.com.calcmot.securityrecording.ui.components.*
import br.com.calcmot.ui.design.theme.CalcMotTheme
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotSpacing

// Synthetic presentation fixtures only. No routes, camera, service, DAO or domain snapshots.
@Preview(name = "Pronto · 393", group = "CAM-001 Principal", widthDp = 393, heightDp = 852)
@Preview(name = "Pronto · 320 · fonte 2", group = "CAM-001 Acessibilidade", widthDp = 320, heightDp = 568, fontScale = 2f)
@Preview(name = "Pronto · paisagem", group = "CAM-001 Acessibilidade", widthDp = 852, heightDp = 393)
@Composable
internal fun RecordingReadyPreview() = OperationalPreview(
    RecordingHeaderState(RecordingVisualState.READY, "Pronto para gravar", "Câmera frontal · Áudio e vídeo"),
    primary = RecordingAction("Iniciar gravação", {}, Icons.Default.PlayArrow),
    secondary = RecordingAction("Ver enquadramento", {}, Icons.Outlined.CameraAlt),
    retention = "Temporário por 24h\napós encerrar", navigation = true,
)

@Preview(name = "Gravando · 393", group = "CAM-001 Principal", widthDp = 393, heightDp = 852)
@Preview(name = "Gravando · 360 · fonte 1,3", group = "CAM-001 Acessibilidade", widthDp = 360, heightDp = 800, fontScale = 1.3f)
@Composable
internal fun RecordingCapturingPreview() = OperationalPreview(
    RecordingHeaderState(RecordingVisualState.RECORDING, "Gravando", capturedDuration = "12:43", spokenDuration = "12 minutos e 43 segundos"),
    primary = stopAction(), secondary = RecordingAction("Pausar", {}, Icons.Default.Pause),
)

@Preview(name = "Pausado", group = "CAM-001 Principal", widthDp = 393, heightDp = 852)
@Composable
internal fun RecordingPausedPreview() = OperationalPreview(
    RecordingHeaderState(RecordingVisualState.PAUSED, "Pausado", "Áudio e vídeo não estão sendo gravados", "1:02:43", "1 hora, 2 minutos e 43 segundos"),
    primary = RecordingAction("Retomar gravação", {}, Icons.Default.PlayArrow), secondary = stopAction(),
)

@Preview(name = "Iniciando", group = "CAM-001 Principal", widthDp = 393, heightDp = 852)
@Composable
internal fun RecordingStartingPreview() = OperationalPreview(
    RecordingHeaderState(RecordingVisualState.STARTING, "Iniciando gravação…"),
    primary = RecordingAction("Cancelar início", {}),
)

@Preview(name = "Finalizando", group = "CAM-001 Principal", widthDp = 393, heightDp = 852)
@Composable
internal fun RecordingFinalizingPreview() = OperationalPreview(
    RecordingHeaderState(RecordingVisualState.FINALIZING, "Finalizando gravação…", "Você pode sair desta tela enquanto a gravação é concluída."),
)

@Preview(name = "Falha", group = "CAM-001 Principal", widthDp = 393, heightDp = 852)
@Preview(name = "Falha · causa longa · fonte 2", group = "CAM-001 Acessibilidade", widthDp = 320, heightDp = 568, fontScale = 2f)
@Composable
internal fun RecordingFailurePreview() = OperationalPreview(
    RecordingHeaderState(RecordingVisualState.FAILURE, "Gravação interrompida", "O resultado ainda precisa ser confirmado."),
    primary = RecordingAction("Resolver problema", {}),
    issue = "O microfone ficou indisponível. Verifique a autorização de áudio antes de preparar uma nova gravação.",
)

@Composable
private fun OperationalPreview(
    header: RecordingHeaderState,
    primary: RecordingAction? = null,
    secondary: RecordingAction? = null,
    retention: String? = null,
    issue: String? = null,
    navigation: Boolean = false,
) = CalcMotTheme {
    RecordingScaffold("Gravação", {}, accessibleTitle = "Gravação de segurança", navigationActions = {
        if (navigation) RecordingNavigationActions({}, {})
    }) { padding ->
        RecordingScrollContent(padding) {
            RecordingOperationalContent(header, retentionLabel = retention, issue = issue, primaryAction = primary, secondaryAction = secondary)
        }
    }
}

private fun stopAction() = RecordingAction("Parar gravação", {}, Icons.Default.Stop, tone = RecordingActionTone.STOP)

@Preview(name = "Enquadramento · abrindo", group = "CAM-001 Família", widthDp = 393, heightDp = 852)
@Preview(name = "Enquadramento · paisagem", group = "CAM-001 Acessibilidade", widthDp = 852, heightDp = 393)
@Composable
internal fun RecordingFramingPreview() = FramingPreview(RecordingPreviewState.Loading("Abrindo câmera…"))

@Preview(name = "Enquadramento · indisponível", group = "CAM-001 Família", widthDp = 320, heightDp = 568, fontScale = 1.3f)
@Composable
internal fun RecordingFramingUnavailablePreview() = FramingPreview(
    RecordingPreviewState.Unavailable("Esta câmera está indisponível. Tente a outra lente."),
)

@Composable
private fun FramingPreview(state: RecordingPreviewState) = CalcMotTheme {
    var front by remember { mutableStateOf(true) }
    RecordingScaffold("Enquadramento", {}) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
        val frameHeight = maxHeight / 2
        RecordingScrollContent(PaddingValues()) {
            RecordingFramingContent(
                state,
                currentLens = if (front) "frontal" else "traseira",
                cameraSwitch = RecordingCameraSwitch(
                    nextLens = if (front) "traseira" else "frontal",
                    onSwitchCamera = { front = !front },
                    enabled = state !is RecordingPreviewState.Loading,
                ),
                confirmAction = RecordingAction("Usar este enquadramento", {}, enabled = false),
                maxFrameHeight = frameHeight,
                frame = {},
            )
        }
        }
    }
}

private val sampleAvailability = RecordingAvailabilityState("Temporário · até amanhã, 18:54")
private val sampleGroups = listOf(
    RecordingSessionGroup("today", "Hoje", listOf(
        RecordingSessionItem("session-1", "18:42", "12 min", sampleAvailability),
        RecordingSessionItem("session-2", "17:10", "8 min", RecordingAvailabilityState("Temporário · até amanhã, 17:18", "Cópia salva na galeria"), "Interrompida · ver trechos"),
    )),
    RecordingSessionGroup("yesterday", "Ontem", listOf(
        RecordingSessionItem("session-3", "21:05", "23 min", RecordingAvailabilityState("Temporário · até hoje, 21:28", expiresSoon = true)),
    )),
)

@Preview(name = "Histórico", group = "CAM-001 Família", widthDp = 393, heightDp = 852)
@Preview(name = "Histórico · fonte 2", group = "CAM-001 Acessibilidade", widthDp = 320, heightDp = 568, fontScale = 2f)
@Composable
internal fun RecordingHistoryPreview() = CalcMotTheme {
    RecordingScaffold("Gravações", {}) { padding ->
        RecordingHistoryContent(sampleGroups, {}, Modifier.padding(padding).padding(horizontal = CalcMotSpacing.ScreenHorizontal),
            contentPadding = PaddingValues(bottom = CalcMotSpacing.ScreenVertical))
    }
}

@Preview(name = "Histórico vazio", group = "CAM-001 Família", widthDp = 360, heightDp = 800)
@Composable
internal fun RecordingEmptyPreview() = CalcMotTheme {
    RecordingScaffold("Gravações", {}) { padding -> RecordingScrollContent(padding) {
        RecordingEmptyState("Nenhuma gravação ainda", "Suas sessões aparecerão aqui depois de encerrar.", RecordingAction("Nova gravação", {}))
    } }
}

@Preview(name = "Detalhe", group = "CAM-001 Família", widthDp = 393, heightDp = 852)
@Preview(name = "Detalhe · fonte 2", group = "CAM-001 Acessibilidade", widthDp = 320, heightDp = 568, fontScale = 2f)
@Composable
internal fun RecordingDetailPreview() = CalcMotTheme {
    RecordingScaffold("Gravação", {}) { padding -> RecordingScrollContent(padding) {
        RecordingDetailContent("Hoje, 18:42", "Gravação disponível", "12 min", sampleAvailability,
            primaryAction = RecordingAction("Salvar na galeria", {}), secondaryAction = RecordingAction("Compartilhar", {})) {
            RecordingPlayer(player = null)
        }
    } }
}

@Preview(name = "Ajustes · linhas", group = "CAM-001 Componentes", widthDp = 320, fontScale = 2f)
@Composable
internal fun RecordingSettingsPreview() = CalcMotTheme {
    Surface(color = CalcMotColors.AppBackground) {
        Column(Modifier.padding(CalcMotSpacing.ScreenHorizontal)) {
            RecordingSettingRow("Câmera preferida", "Frontal", onClick = {})
            RecordingSettingRow("Qualidade", "480p")
            RecordingSettingRow("Prazo padrão", "24 horas", onClick = {})
        }
    }
}
