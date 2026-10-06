package br.com.calcmot.securityrecording.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import br.com.calcmot.securityrecording.data.RecordingPreferences
import br.com.calcmot.securityrecording.domain.*
import br.com.calcmot.securityrecording.ui.components.*
import br.com.calcmot.ui.theme.MetricaTheme

@Preview(name = "Fluxo integrado · preparando", widthDp = 393, heightDp = 852)
@Composable internal fun IntegratedPreparingPreview() = ActualState(RecordingPhase.PREPARING)
@Preview(name = "Fluxo integrado · gravando", widthDp = 393, heightDp = 852)
@Composable internal fun IntegratedRecordingPreview() = ActualState(RecordingPhase.RECORDING)
@Preview(name = "Fluxo integrado · pausado", widthDp = 393, heightDp = 852)
@Composable internal fun IntegratedPausedPreview() = ActualState(RecordingPhase.PAUSED)
@Preview(name = "Fluxo integrado · rotação", widthDp = 393, heightDp = 852)
@Composable internal fun IntegratedRotatingPreview() = ActualState(RecordingPhase.ROTATING)
@Preview(name = "Fluxo integrado · finalizando", widthDp = 393, heightDp = 852)
@Composable internal fun IntegratedFinalizingPreview() = ActualState(RecordingPhase.FINALIZING)
@Preview(name = "Fluxo integrado · primeiro uso", widthDp = 393, heightDp = 852)
@Preview(name = "Primeiro uso · 320 · fonte 2", widthDp = 320, heightDp = 568, fontScale = 2f)
@Composable internal fun IntegratedFirstUsePreview() { MetricaTheme { RecordingFirstUseRoute({}, {}) } }
@Preview(name = "Fluxo integrado · ajustes", widthDp = 393, heightDp = 852)
@Preview(name = "Ajustes · 320 · fonte 2", widthDp = 320, heightDp = 568, fontScale = 2f)
@Composable internal fun IntegratedSettingsPreview() { MetricaTheme { RecordingSettingsRoute({}, RecordingPreferences(), null, {}, {}, {}, {}, {}) } }

@Composable private fun ActualState(phase: RecordingPhase) { MetricaTheme { RecordingScaffold("Gravação", {}, navigationActions = { RecordingNavigationActions({}, {}, settingsEnabled = false) }) { padding -> RecordingScrollContent(padding) {
    RecordingActivePresentation(RecordingRuntimeSnapshot("synthetic", phase, 2,
        capturedDurationMs = if(phase == RecordingPhase.PREPARING) null else 65_000, audioVideoConfirmed = phase == RecordingPhase.RECORDING), {})
} } } }
