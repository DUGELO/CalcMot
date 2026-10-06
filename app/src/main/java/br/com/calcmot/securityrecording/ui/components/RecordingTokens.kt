package br.com.calcmot.securityrecording.ui.components

import androidx.compose.ui.unit.dp
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotShape

/** Camera UX spec §6. Aliases only; other CalcMot consumers keep their defaults. */
internal object RecordingTokens {
    val action = CalcMotColors.BrandPrimaryDark
    val onAction = CalcMotColors.TextPrimary
    val stop = CalcMotColors.Danger
    val onStop = CalcMotColors.TextInverse
    val panel = CalcMotColors.Surface
    val panelRadius = CalcMotShape.Lg
    val buttonRadius = CalcMotShape.Sm
    val actionHeight = 56.dp
    val touchTarget = 48.dp
    val iconSize = 24.dp
    val dividerWidth = 1.dp
    // Compact video identification, leaving the session text dominant at 320dp.
    val thumbnailWidth = 64.dp
}
