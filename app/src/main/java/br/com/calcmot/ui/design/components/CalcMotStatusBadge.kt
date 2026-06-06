package br.com.calcmot.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotShape
import br.com.calcmot.ui.design.tokens.CalcMotSpacing
import br.com.calcmot.ui.design.tokens.CalcMotTypography

enum class CalcMotStatusVariant {
    Active,
    Pending,
    Attention,
    Error,
    Beta
}

@Composable
fun CalcMotStatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    variant: CalcMotStatusVariant = CalcMotStatusVariant.Active
) {
    val (backgroundColor, textColor) = when (variant) {
        CalcMotStatusVariant.Active -> CalcMotColors.Success to CalcMotColors.TextPrimary
        CalcMotStatusVariant.Pending -> CalcMotColors.Warning to CalcMotColors.TextInverse
        CalcMotStatusVariant.Attention -> CalcMotColors.Warning to CalcMotColors.TextInverse
        CalcMotStatusVariant.Error -> CalcMotColors.Danger to CalcMotColors.TextPrimary
        CalcMotStatusVariant.Beta -> CalcMotColors.Info to CalcMotColors.TextPrimary
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(CalcMotShape.Pill))
            .background(backgroundColor)
            .padding(horizontal = CalcMotSpacing.Sm, vertical = CalcMotSpacing.Xxs),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            style = CalcMotTypography.Caption,
            color = textColor
        )
    }
}
