package br.com.calcmot.ui.design.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotShape
import br.com.calcmot.ui.design.tokens.CalcMotTypography

enum class CalcMotButtonVariant {
    Primary,
    Secondary,
    Ghost,
    Danger,
    Premium
}

@Composable
fun CalcMotButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: CalcMotButtonVariant = CalcMotButtonVariant.Primary,
    enabled: Boolean = true
) {
    val containerColor = when (variant) {
        CalcMotButtonVariant.Primary -> CalcMotColors.BrandPrimary
        CalcMotButtonVariant.Secondary -> CalcMotColors.SurfaceElevated
        CalcMotButtonVariant.Ghost -> androidx.compose.ui.graphics.Color.Transparent
        CalcMotButtonVariant.Danger -> CalcMotColors.Danger
        CalcMotButtonVariant.Premium -> CalcMotColors.Great
    }

    val contentColor = when (variant) {
        CalcMotButtonVariant.Ghost -> CalcMotColors.BrandPrimary
        else -> CalcMotColors.TextPrimary
    }

    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(CalcMotShape.Md),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = CalcMotColors.SurfaceSoft,
            disabledContentColor = CalcMotColors.TextMuted
        )
    ) {
        Text(
            text = text,
            style = CalcMotTypography.Button
        )
    }
}
