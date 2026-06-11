package br.com.calcmot.ui.design.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import br.com.calcmot.ui.design.tokens.CalcMotTypography

@Composable
fun CalcMotTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CalcMotColorScheme.Dark else CalcMotColorScheme.Light

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CalcMotTypography.Material,
        content = content
    )
}

@Composable
fun rememberCalcMotDarkTheme(useSystem: Boolean = false): Boolean {
    return if (useSystem) isSystemInDarkTheme() else true
}
