package br.com.calcmot.ui.design.tokens

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object CalcMotTypography {
    private val fontFamily = FontFamily.Default

    val ScreenTitle = TextStyle(fontFamily = fontFamily, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    val ScreenSubtitle = TextStyle(fontFamily = fontFamily, fontSize = 15.sp, fontWeight = FontWeight.Medium)

    val SectionTitle = TextStyle(fontFamily = fontFamily, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    val CardTitle = TextStyle(fontFamily = fontFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    val Body = TextStyle(fontFamily = fontFamily, fontSize = 14.sp, fontWeight = FontWeight.Normal)
    val BodyStrong = TextStyle(fontFamily = fontFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    val Caption = TextStyle(fontFamily = fontFamily, fontSize = 12.sp, fontWeight = FontWeight.Medium)

    val MetricHero = TextStyle(fontFamily = fontFamily, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    val MetricValue = TextStyle(fontFamily = fontFamily, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    val MetricLabel = TextStyle(fontFamily = fontFamily, fontSize = 12.sp, fontWeight = FontWeight.Medium)

    val Button = TextStyle(fontFamily = fontFamily, fontSize = 15.sp, fontWeight = FontWeight.Bold)

    val Material = Typography(
        displayLarge = ScreenTitle.copy(fontSize = 32.sp),
        displayMedium = MetricHero,
        displaySmall = ScreenTitle,
        headlineLarge = ScreenTitle,
        headlineMedium = SectionTitle,
        titleLarge = SectionTitle,
        titleMedium = CardTitle,
        bodyLarge = Body.copy(fontSize = 16.sp),
        bodyMedium = Body,
        bodySmall = Caption,
        labelLarge = Button,
        labelMedium = Caption.copy(fontWeight = FontWeight.Bold),
        labelSmall = Caption
    )
}
