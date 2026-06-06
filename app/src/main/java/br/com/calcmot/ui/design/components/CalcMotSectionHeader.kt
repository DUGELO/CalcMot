package br.com.calcmot.ui.design.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotSpacing
import br.com.calcmot.ui.design.tokens.CalcMotTypography

@Composable
fun CalcMotSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = CalcMotTypography.SectionTitle,
        color = CalcMotColors.TextPrimary,
        modifier = modifier.padding(vertical = CalcMotSpacing.Md)
    )
}
