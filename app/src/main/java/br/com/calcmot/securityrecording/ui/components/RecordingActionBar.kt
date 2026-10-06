package br.com.calcmot.securityrecording.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotSpacing
import br.com.calcmot.ui.design.tokens.CalcMotTypography

internal enum class RecordingActionTone { PRIMARY, STOP }

/** Intention supplied by the caller; components never infer engine capabilities. */
internal data class RecordingAction(
    val label: String,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
    val enabled: Boolean = true,
    val tone: RecordingActionTone = RecordingActionTone.PRIMARY,
    val testTag: String? = null,
)

/** One filled action, one optional outlined action. Intrinsic heights support large text. */
@Composable
internal fun RecordingActionBar(
    primary: RecordingAction,
    modifier: Modifier = Modifier,
    secondary: RecordingAction? = null,
) {
    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Md),
    ) {
        secondary?.let { RecordingActionButton(it, outlined = true) }
        RecordingActionButton(primary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecordingActionButton(
    action: RecordingAction,
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
) {
    val container = if (action.tone == RecordingActionTone.STOP) RecordingTokens.stop else RecordingTokens.action
    val foreground = if (action.tone == RecordingActionTone.STOP) RecordingTokens.onStop else RecordingTokens.onAction
    val bounds = modifier.then(action.testTag?.let { Modifier.testTag(it) } ?: Modifier).fillMaxWidth().heightIn(min = RecordingTokens.actionHeight)
    val shape = RoundedCornerShape(RecordingTokens.buttonRadius)
    val padding = PaddingValues(horizontal = CalcMotSpacing.Lg, vertical = CalcMotSpacing.Md)
    // Cap state layers locally: the primary label stays above 4.5:1 during interaction.
    CompositionLocalProvider(LocalRippleConfiguration provides RippleConfiguration(
        rippleAlpha = RippleAlpha(draggedAlpha = 0.1f, focusedAlpha = 0.1f, hoveredAlpha = 0.08f, pressedAlpha = 0.1f),
    )) {
    if (outlined) {
        OutlinedButton(
            onClick = action.onClick, enabled = action.enabled, modifier = bounds,
            shape = shape, contentPadding = padding,
            border = BorderStroke(RecordingTokens.dividerWidth, CalcMotColors.TextMuted),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = CalcMotColors.TextPrimary,
                disabledContentColor = CalcMotColors.TextMuted,
            ),
        ) { ActionLabel(action) }
    } else {
        Button(
            onClick = action.onClick, enabled = action.enabled, modifier = bounds,
            shape = shape, contentPadding = padding,
            colors = ButtonDefaults.buttonColors(
                containerColor = container, contentColor = foreground,
                disabledContainerColor = CalcMotColors.SurfaceSoft,
                disabledContentColor = CalcMotColors.TextMuted,
            ),
        ) { ActionLabel(action) }
    }
    }
}

@Composable
private fun RowScope.ActionLabel(action: RecordingAction) {
    action.icon?.let {
        Icon(it, contentDescription = null, modifier = Modifier.size(RecordingTokens.iconSize))
        Spacer(Modifier.width(CalcMotSpacing.Sm))
    }
    Text(action.label, style = CalcMotTypography.Button, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
}
