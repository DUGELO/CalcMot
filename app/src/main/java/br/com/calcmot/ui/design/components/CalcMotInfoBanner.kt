package br.com.calcmot.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotShape
import br.com.calcmot.ui.design.tokens.CalcMotSpacing
import br.com.calcmot.ui.design.tokens.CalcMotTypography

enum class CalcMotBannerVariant {
    Info,
    Warning,
    Danger,
    Success,
    Premium
}

@Composable
fun CalcMotInfoBanner(
    text: String,
    modifier: Modifier = Modifier,
    variant: CalcMotBannerVariant = CalcMotBannerVariant.Info
) {
    val backgroundColor = when (variant) {
        CalcMotBannerVariant.Info -> CalcMotColors.Info.copy(alpha = 0.15f)
        CalcMotBannerVariant.Warning -> CalcMotColors.Warning.copy(alpha = 0.15f)
        CalcMotBannerVariant.Danger -> CalcMotColors.Danger.copy(alpha = 0.15f)
        CalcMotBannerVariant.Success -> CalcMotColors.Success.copy(alpha = 0.15f)
        CalcMotBannerVariant.Premium -> CalcMotColors.BrandPrimary.copy(alpha = 0.15f)
    }

    val contentColor = when (variant) {
        CalcMotBannerVariant.Info -> CalcMotColors.Info
        CalcMotBannerVariant.Warning -> CalcMotColors.Warning
        CalcMotBannerVariant.Danger -> CalcMotColors.Danger
        CalcMotBannerVariant.Success -> CalcMotColors.Success
        CalcMotBannerVariant.Premium -> CalcMotColors.BrandPrimary
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(CalcMotShape.Sm))
            .padding(CalcMotSpacing.Md)
    ) {
        Text(
            text = text,
            style = CalcMotTypography.Caption,
            color = contentColor
        )
    }
}
