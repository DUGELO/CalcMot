package br.com.calcmot.securityrecording.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotSpacing
import br.com.calcmot.ui.design.tokens.CalcMotTypography

/** Presentation state only. It is deliberately not the recording domain's state machine. */
internal enum class RecordingVisualState { READY, RECORDING, PAUSED, STARTING, FINALIZING, FAILURE, PENDING }

internal data class RecordingHeaderState(
    val state: RecordingVisualState,
    val title: String,
    val explanation: String? = null,
    val capturedDuration: String? = null,
    val spokenDuration: String? = null,
)

@Composable
internal fun RecordingStateHeader(state: RecordingHeaderState, modifier: Modifier = Modifier) {
    val color = when (state.state) {
        RecordingVisualState.READY -> CalcMotColors.Success
        RecordingVisualState.RECORDING, RecordingVisualState.FAILURE -> CalcMotColors.Danger
        RecordingVisualState.PAUSED, RecordingVisualState.PENDING -> CalcMotColors.Warning
        else -> CalcMotColors.TextSecondary
    }
    val icon = when (state.state) {
        RecordingVisualState.RECORDING -> Icons.Default.FiberManualRecord
        RecordingVisualState.PAUSED -> Icons.Default.Pause
        RecordingVisualState.FAILURE -> Icons.Outlined.ErrorOutline
        RecordingVisualState.PENDING -> Icons.Outlined.Info
        else -> Icons.Outlined.Videocam
    }
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm),
            modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        ) {
            if (state.state == RecordingVisualState.STARTING || state.state == RecordingVisualState.FINALIZING) {
                CircularProgressIndicator(Modifier.size(RecordingTokens.iconSize).clearAndSetSemantics {}, color = color)
            } else {
                Icon(icon, null, Modifier.size(RecordingTokens.iconSize), tint = color)
            }
            if (state.state == RecordingVisualState.RECORDING) {
                Text("REC", color = CalcMotColors.Danger, style = CalcMotTypography.BodyStrong)
            }
            Text(state.title, Modifier.semantics { heading() }, color = CalcMotColors.TextPrimary,
                style = CalcMotTypography.ScreenTitle, textAlign = TextAlign.Center)
        }
        state.capturedDuration?.let { duration ->
            Text(duration,
                modifier = Modifier.padding(top = CalcMotSpacing.Sm).semantics {
                    contentDescription = "Duração capturada: ${state.spokenDuration ?: duration}"
                },
                color = CalcMotColors.TextPrimary,
                style = CalcMotTypography.MetricHero.copy(fontFeatureSettings = "tnum"),
                textAlign = TextAlign.Center)
        }
        state.explanation?.takeIf { it.isNotBlank() }?.let { explanation ->
            Text(explanation, modifier = Modifier.padding(top = CalcMotSpacing.Sm),
                color = CalcMotColors.TextSecondary, style = CalcMotTypography.Body, textAlign = TextAlign.Center)
        }
    }
}
