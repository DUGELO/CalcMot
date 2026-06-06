package br.com.calcmot.ui.design.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotSpacing
import br.com.calcmot.ui.design.tokens.CalcMotTypography

@Composable
fun CalcMotSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = CalcMotSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = CalcMotTypography.CardTitle,
                color = CalcMotColors.TextPrimary
            )
            Text(
                text = description,
                style = CalcMotTypography.Caption,
                color = CalcMotColors.TextMuted
            )
        }
        Spacer(modifier = Modifier.width(CalcMotSpacing.Md))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CalcMotColors.TextPrimary,
                checkedTrackColor = CalcMotColors.BrandPrimary,
                uncheckedThumbColor = CalcMotColors.TextMuted,
                uncheckedTrackColor = CalcMotColors.SurfaceElevated
            )
        )
    }
}
