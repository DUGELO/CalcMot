package com.example.calcmot.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    background = SurfaceBackground,
    surface = SurfacePrimary,
    onSurface = TextPrimary,
    primary = InteractiveAccent,
    onPrimary = TextPrimary,
    secondary = TextSecondary,
    onSecondary = TextPrimary,
)

@Composable
fun MetricaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = DarkColorScheme // Forçando o Dark Mode por padrão

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // Procura por uma Activity no contexto. Se não achar, não faz nada.
            val activity = view.context.findActivity()
            if (activity != null) {
                val window = activity.window
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MetricaTypography,
        content = content
    )
}

// Função utilitária para encontrar a Activity a partir de um Context
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}