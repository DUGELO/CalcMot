package br.com.calcmot.securityrecording.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import br.com.calcmot.securityrecording.domain.RecordingRuntimeSnapshot
import br.com.calcmot.securityrecording.ui.components.*
import br.com.calcmot.ui.UiTestTags
import br.com.calcmot.ui.design.tokens.*

@Composable fun SecurityToolsRoute(onBack: () -> Unit, onOpenHub: () -> Unit) {
    RecordingScaffold("Ferramentas", onBack, modifier = Modifier.testTag(UiTestTags.SECURITY_TOOLS_SCREEN)) { padding ->
        RecordingScrollContent(padding) {
            Row(Modifier.fillMaxWidth().heightIn(min = RecordingTokens.touchTarget).clickable(onClick = onOpenHub).padding(vertical = CalcMotSpacing.Lg),
                horizontalArrangement = Arrangement.spacedBy(CalcMotSpacing.Md)) {
                Icon(Icons.Outlined.Videocam, null, tint = CalcMotColors.Success)
                Column(Modifier.weight(1f)) {
                    Text("Gravação de segurança", color = CalcMotColors.TextPrimary, style = CalcMotTypography.CardTitle)
                    Text("Grave áudio e vídeo neste aparelho.", color = CalcMotColors.TextSecondary, style = CalcMotTypography.Body)
                }
            }
        }
    }
}
// Compatibility destinations all project the same operational flow.
@Composable fun SecurityRecordingHubRoute(onBack: () -> Unit, onConfigure: () -> Unit, onLibrary: () -> Unit) = RecordingExperience(onBack)
@Composable fun SecurityRecordingConfigurationRoute(onBack: () -> Unit, onRecordingRequested: () -> Unit) = RecordingExperience(onBack)
@Composable fun SecurityRecordingActiveRoute(onBack: () -> Unit, onOpenLibrary: () -> Unit) = RecordingExperience(onBack)
@Composable internal fun SecurityRecordingActiveContent(snapshot: RecordingRuntimeSnapshot, onStop: () -> Unit, onOpenLibrary: () -> Unit) = RecordingActivePresentation(snapshot, onStop)
@Composable internal fun SecurityPage(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    RecordingScaffold(title, onBack, modifier) { padding -> RecordingScrollContent(padding) { content() } }
}
