package br.com.calcmot.ui.design.components

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import br.com.calcmot.ui.design.tokens.CalcMotColors

@Composable
fun CalcMotDivider(
    modifier: Modifier = Modifier
) {
    HorizontalDivider(
        modifier = modifier,
        color = CalcMotColors.BorderSubtle
    )
}
