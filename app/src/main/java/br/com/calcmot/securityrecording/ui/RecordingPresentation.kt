package br.com.calcmot.securityrecording.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import br.com.calcmot.securityrecording.ui.components.*
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotSpacing
import br.com.calcmot.ui.design.tokens.CalcMotTypography

/** Shared presentation for CAM-012. No runtime observation, permission request or media owner. */
@Composable
internal fun RecordingOperationalContent(
    header: RecordingHeaderState,
    modifier: Modifier = Modifier,
    retentionLabel: String? = null,
    issue: String? = null,
    primaryAction: RecordingAction? = null,
    secondaryAction: RecordingAction? = null,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.SectionGap)) {
        Surface(color = RecordingTokens.panel, shape = RoundedCornerShape(RecordingTokens.panelRadius)) {
            Column(Modifier.fillMaxWidth().padding(CalcMotSpacing.Xl),
                verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.SectionGap),
                horizontalAlignment = Alignment.CenterHorizontally) {
                RecordingStateHeader(header)
                retentionLabel?.let {
                    Text(it, color = CalcMotColors.TextSecondary, style = CalcMotTypography.Body, textAlign = TextAlign.Center)
                }
            }
        }
        issue?.let { RecordingIssueInline(it) }
        primaryAction?.let { RecordingActionBar(it, modifier = Modifier.padding(top = CalcMotSpacing.Xs), secondary = secondaryAction) }
    }
}

/** Scroll and actions share one flow, so even 2x text in landscape can reach the CTA. */
@Composable
internal fun RecordingScrollContent(padding: PaddingValues, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
        .padding(horizontal = CalcMotSpacing.ScreenHorizontal, vertical = CalcMotSpacing.ScreenVertical),
        verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.SectionGap), content = content)
}

@Composable
internal fun RecordingFramingContent(
    preview: RecordingPreviewState,
    currentLens: String,
    confirmAction: RecordingAction,
    modifier: Modifier = Modifier,
    cameraSwitch: RecordingCameraSwitch? = null,
    maxFrameHeight: Dp = Dp.Infinity,
    frame: @Composable BoxScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.SectionGap)) {
        RecordingPreview(preview, currentLens, cameraSwitch = cameraSwitch, maxFrameHeight = maxFrameHeight, frame = frame)
        RecordingActionBar(confirmAction, modifier = Modifier.padding(top = CalcMotSpacing.Xs))
    }
}

internal data class RecordingSessionGroup(val id: String, val label: String, val sessions: List<RecordingSessionItem>)

/** UI data with stable IDs, ready for the reactive repository in CAM-015. */
@Composable
internal fun RecordingHistoryContent(
    groups: List<RecordingSessionGroup>,
    onOpenSession: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = contentPadding) {
        groups.forEach { group ->
            item(key = "group:${group.id}", contentType = "heading") {
                Text(group.label, modifier = Modifier.padding(top = CalcMotSpacing.SectionGap, bottom = CalcMotSpacing.Sm)
                    .semantics { heading() }, color = CalcMotColors.TextPrimary, style = CalcMotTypography.SectionTitle)
            }
            items(group.sessions, key = { "session:${it.id}" }, contentType = { "session" }) { session ->
                RecordingSessionRow(session, onClick = { onOpenSession(session.id) })
            }
        }
    }
}

/** Player is a slot to keep the details composition independent of Media3 lifecycle. */
@Composable
internal fun RecordingDetailContent(
    title: String,
    result: String,
    capturedDuration: String,
    availability: RecordingAvailabilityState,
    modifier: Modifier = Modifier,
    interruption: String? = null,
    primaryAction: RecordingAction? = null,
    secondaryAction: RecordingAction? = null,
    player: @Composable () -> Unit,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.SectionGap)) {
        Column(verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)) {
            Text(title, Modifier.semantics { heading() }, color = CalcMotColors.TextPrimary, style = CalcMotTypography.ScreenTitle)
            Text(result, color = CalcMotColors.TextSecondary, style = CalcMotTypography.Body)
        }
        interruption?.let { RecordingIssueInline(it) }
        player()
        Text(capturedDuration, color = CalcMotColors.TextPrimary, style = CalcMotTypography.Body)
        RecordingAvailability(availability)
        primaryAction?.let { RecordingActionBar(it, modifier = Modifier.padding(top = CalcMotSpacing.Xs), secondary = secondaryAction) }
    }
}
