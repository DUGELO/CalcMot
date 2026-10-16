package br.com.calcmot.securityrecording.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import br.com.calcmot.securityrecording.domain.RecordingRuntimeSnapshot
import br.com.calcmot.securityrecording.ui.components.*
import br.com.calcmot.ui.UiTestTags
import br.com.calcmot.ui.design.components.CalcMotCard
import br.com.calcmot.ui.design.components.CalcMotSectionHeader
import br.com.calcmot.ui.design.tokens.*

/** Discovery follows the same CalcMot DS card and tap-target patterns as other app routes. */
@Composable
fun SecurityToolsRoute(onBack: () -> Unit, onOpenHub: () -> Unit) {
    RecordingScaffold("Ferramentas", onBack, modifier = Modifier.testTag(UiTestTags.SECURITY_TOOLS_SCREEN)) { padding ->
        RecordingScrollContent(padding) {
            CalcMotSectionHeader(
                title = "Para sua segurança",
                subtitle = "Recursos adicionais para suas viagens."
            )
            CalcMotCard(
                modifier = Modifier.testTag(UiTestTags.SECURITY_TOOLS_RECORDING_ITEM),
                onClick = onOpenHub
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp)
                        .padding(CalcMotSpacing.CardPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CalcMotSpacing.Md)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Videocam,
                        contentDescription = null,
                        modifier = Modifier.size(RecordingTokens.iconSize),
                        tint = CalcMotColors.Success
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Xs)
                    ) {
                        Text("Gravação de segurança", color = CalcMotColors.TextPrimary, style = CalcMotTypography.CardTitle)
                        Text("Grave áudio e vídeo neste aparelho.", color = CalcMotColors.TextSecondary, style = CalcMotTypography.Body)
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(RecordingTokens.iconSize),
                        tint = CalcMotColors.TextMuted
                    )
                }
            }
        }
    }
}

// Compatibility destinations retain the isolated recording lifecycle.
@Composable fun SecurityRecordingHubRoute(onBack: () -> Unit, onConfigure: () -> Unit, onLibrary: () -> Unit) = RecordingExperience(onBack)
@Composable fun SecurityRecordingConfigurationRoute(onBack: () -> Unit, onRecordingRequested: () -> Unit) = RecordingExperience(onBack)
@Composable fun SecurityRecordingActiveRoute(onBack: () -> Unit, onOpenLibrary: () -> Unit) = RecordingExperience(onBack)
@Composable internal fun SecurityRecordingActiveContent(snapshot: RecordingRuntimeSnapshot, onStop: () -> Unit, onOpenLibrary: () -> Unit) = RecordingActivePresentation(snapshot, onStop)
@Composable internal fun SecurityPage(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    RecordingScaffold(title, onBack, modifier) { padding -> RecordingScrollContent(padding) { content() } }
}
