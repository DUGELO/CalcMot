package br.com.calcmot.securityrecording.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotSpacing
import br.com.calcmot.ui.design.tokens.CalcMotTypography

@Composable
internal fun RecordingIssueInline(cause: String, modifier: Modifier = Modifier, action: RecordingAction? = null) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm),
            modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
            Icon(Icons.Outlined.ErrorOutline, null, Modifier.size(RecordingTokens.iconSize), tint = CalcMotColors.Warning)
            Text(cause, color = CalcMotColors.TextPrimary, style = CalcMotTypography.Body, modifier = Modifier.weight(1f))
        }
        action?.let { RecordingActionButton(it, outlined = true) }
    }
}

@Composable
internal fun RecordingEmptyState(title: String, explanation: String, action: RecordingAction, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = CalcMotSpacing.Xl),
        verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)) {
        Icon(Icons.Outlined.VideoLibrary, null, Modifier.size(RecordingTokens.iconSize), tint = CalcMotColors.TextMuted)
        Text(title, Modifier.semantics { heading() }, color = CalcMotColors.TextPrimary, style = CalcMotTypography.ScreenTitle)
        Text(explanation, color = CalcMotColors.TextSecondary, style = CalcMotTypography.Body)
        RecordingActionBar(action, Modifier.padding(top = CalcMotSpacing.Lg))
    }
}

@Composable
internal fun RecordingLoading(task: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(CalcMotSpacing.Md), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(RecordingTokens.iconSize), color = CalcMotColors.TextSecondary)
        Text(task, color = CalcMotColors.TextSecondary, style = CalcMotTypography.Body, modifier = Modifier.weight(1f))
    }
}
