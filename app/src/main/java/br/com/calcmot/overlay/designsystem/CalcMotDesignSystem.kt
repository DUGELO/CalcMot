package br.com.calcmot.overlay.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object CalcMotColors {
    val Bad = Color(0xFFE53935)
    val Warning = Color(0xFFFFB300)
    val Good = Color(0xFF2E7D32)
    val Great = Color(0xFF6D3BFF) // Roxo premium

    val RoyalBlueAlternative = Color(0xFF2457FF)

    val OverlayBackground = Color(0xE61A1A1A) // ~90% opacidade
    val OverlayBackgroundSoft = Color(0xD91A1A1A) // ~85% opacidade
    val SurfaceElevated = Color(0xF2232323)

    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFE0E0E0)
    val TextMuted = Color(0xFFBDBDBD)

    val Divider = Color(0x33FFFFFF)
}

object CalcMotOpacity {
    const val OverlayStrong = 0.90f
    const val OverlayMedium = 0.86f
    const val OverlaySoft = 0.82f

    const val AccentFull = 1.00f
    const val SecondaryText = 0.82f
    const val Disabled = 0.45f
}

object CalcMotTypography {
    val ValuePrimary = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
    )

    val MetricLabel = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
    )

    val MetricValue = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold
    )

    val ImpactMessage = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold
    )

    val ImpactSubMessage = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
    )
}

object CalcMotSpacing {
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp

    val OverlayPadding = 10.dp
    val MetricGap = 6.dp
    val SectionGap = 8.dp
}

object CalcMotShape {
    val SmallRadius = 8.dp
    val OverlayRadius = 16.dp
    val BadgeRadius = 999.dp
    val CardRadius = 20.dp
}

object CalcMotElevation {
    val Overlay = 8.dp
    val Floating = 12.dp
}
