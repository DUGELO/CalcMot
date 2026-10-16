package br.com.calcmot.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import br.com.calcmot.ui.design.tokens.CalcMotColors

/**
 * Single source of truth for the CalcMot wordmark used in Home, drawer and Growth.
 * "Calc" stays white; "Mot" stays green, both italic and bold.
 */
internal fun AnnotatedString.Builder.appendCalcMotWordmark() {
    withStyle(
        SpanStyle(
            color = CalcMotColors.TextPrimary,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Black
        )
    ) { append("Calc") }
    withStyle(
        SpanStyle(
            color = CalcMotColors.Success,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Black
        )
    ) { append("Mot") }
}
