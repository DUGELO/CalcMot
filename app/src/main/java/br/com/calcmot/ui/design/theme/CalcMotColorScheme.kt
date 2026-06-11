package br.com.calcmot.ui.design.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import br.com.calcmot.ui.design.tokens.CalcMotColors

object CalcMotColorScheme {
    val Dark: ColorScheme = darkColorScheme(
        background = CalcMotColors.AppBackground,
        onBackground = CalcMotColors.TextPrimary,
        surface = CalcMotColors.Surface,
        onSurface = CalcMotColors.TextPrimary,
        surfaceVariant = CalcMotColors.SurfaceSoft,
        onSurfaceVariant = CalcMotColors.TextSecondary,
        primary = CalcMotColors.BrandPrimary,
        onPrimary = CalcMotColors.TextPrimary,
        secondary = CalcMotColors.TextSecondary,
        onSecondary = CalcMotColors.TextInverse,
        tertiary = CalcMotColors.BrandAccent,
        onTertiary = CalcMotColors.TextInverse,
        error = CalcMotColors.Danger,
        onError = CalcMotColors.TextPrimary,
        outline = CalcMotColors.BorderSubtle
    )

    val Light: ColorScheme = lightColorScheme(
        background = CalcMotColors.AppBackgroundLight,
        onBackground = CalcMotColors.TextInverse,
        surface = ColorPaletteLightSurface,
        onSurface = CalcMotColors.TextInverse,
        surfaceVariant = ColorPaletteLightSurfaceSoft,
        onSurfaceVariant = ColorPaletteLightTextSecondary,
        primary = CalcMotColors.BrandPrimaryDark,
        onPrimary = CalcMotColors.TextPrimary,
        secondary = ColorPaletteLightTextSecondary,
        onSecondary = CalcMotColors.TextPrimary,
        tertiary = CalcMotColors.BrandAccent,
        onTertiary = CalcMotColors.TextInverse,
        error = CalcMotColors.Danger,
        onError = CalcMotColors.TextPrimary,
        outline = ColorPaletteLightBorder
    )
}

private val ColorPaletteLightSurface = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
private val ColorPaletteLightSurfaceSoft = androidx.compose.ui.graphics.Color(0xFFECEFF4)
private val ColorPaletteLightTextSecondary = androidx.compose.ui.graphics.Color(0xFF505762)
private val ColorPaletteLightBorder = androidx.compose.ui.graphics.Color(0x22000000)
