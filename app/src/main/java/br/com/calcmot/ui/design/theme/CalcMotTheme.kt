package br.com.calcmot.ui.design.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import br.com.calcmot.ui.design.tokens.CalcMotColors

private val DarkColorScheme = darkColorScheme(
    primary = CalcMotColors.BrandPrimary,
    onPrimary = CalcMotColors.TextPrimary,
    secondary = CalcMotColors.BrandSecondary,
    onSecondary = CalcMotColors.TextPrimary,
    tertiary = CalcMotColors.BrandAccent,
    onTertiary = CalcMotColors.TextPrimary,
    background = CalcMotColors.AppBackground,
    onBackground = CalcMotColors.TextPrimary,
    surface = CalcMotColors.Surface,
    onSurface = CalcMotColors.TextPrimary,
    surfaceVariant = CalcMotColors.SurfaceElevated,
    onSurfaceVariant = CalcMotColors.TextSecondary,
    error = CalcMotColors.Danger,
    onError = CalcMotColors.TextPrimary,
    outline = CalcMotColors.BorderSubtle,
    outlineVariant = CalcMotColors.BorderStrong
)

private val LightColorScheme = lightColorScheme(
    primary = CalcMotColors.BrandPrimary,
    onPrimary = CalcMotColors.TextInverse,
    secondary = CalcMotColors.BrandSecondary,
    onSecondary = CalcMotColors.TextInverse,
    tertiary = CalcMotColors.BrandAccent,
    onTertiary = CalcMotColors.TextInverse,
    background = CalcMotColors.AppBackgroundLight,
    onBackground = CalcMotColors.TextInverse,
    surface = CalcMotColors.TextPrimary, // Inverse behavior for light mode
    onSurface = CalcMotColors.TextInverse,
    error = CalcMotColors.Danger,
    onError = CalcMotColors.TextPrimary
)

@Composable
fun CalcMotTheme(
    darkTheme: Boolean = true, // MVP mandates dark mode by default
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
