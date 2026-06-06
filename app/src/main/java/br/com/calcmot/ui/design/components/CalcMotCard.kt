package br.com.calcmot.ui.design.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotShape
import br.com.calcmot.ui.design.tokens.CalcMotSpacing

enum class CalcMotCardVariant {
    Default,
    Highlight,
    Danger,
    Success,
    Premium
}

@Composable
fun CalcMotCard(
    modifier: Modifier = Modifier,
    variant: CalcMotCardVariant = CalcMotCardVariant.Default,
    content: @Composable ColumnScope.() -> Unit
) {
    val containerColor = when (variant) {
        CalcMotCardVariant.Default -> CalcMotColors.Surface
        CalcMotCardVariant.Highlight -> CalcMotColors.SurfaceElevated
        CalcMotCardVariant.Danger -> CalcMotColors.Danger.copy(alpha = 0.15f)
        CalcMotCardVariant.Success -> CalcMotColors.Success.copy(alpha = 0.15f)
        CalcMotCardVariant.Premium -> CalcMotColors.BrandPrimary.copy(alpha = 0.15f)
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(CalcMotShape.Md),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = CalcMotColors.TextPrimary
        )
    ) {
        Column(
            modifier = Modifier.padding(CalcMotSpacing.CardPadding),
            content = content
        )
    }
}
