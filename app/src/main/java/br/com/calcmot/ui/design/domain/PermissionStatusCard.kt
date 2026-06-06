package br.com.calcmot.ui.design.domain

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import br.com.calcmot.ui.design.components.CalcMotButton
import br.com.calcmot.ui.design.components.CalcMotButtonVariant
import br.com.calcmot.ui.design.components.CalcMotCard
import br.com.calcmot.ui.design.components.CalcMotStatusBadge
import br.com.calcmot.ui.design.components.CalcMotStatusVariant
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotSpacing
import br.com.calcmot.ui.design.tokens.CalcMotTypography

@Composable
fun PermissionStatusCard(
    title: String,
    description: String,
    statusText: String,
    statusVariant: CalcMotStatusVariant,
    onActionClick: () -> Unit,
    actionLabel: String,
    modifier: Modifier = Modifier
) {
    CalcMotCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = CalcMotTypography.CardTitle,
                    color = CalcMotColors.TextPrimary
                )
            }
            CalcMotStatusBadge(text = statusText, variant = statusVariant)
        }
        Spacer(modifier = Modifier.height(CalcMotSpacing.Sm))
        Text(
            text = description,
            style = CalcMotTypography.Caption,
            color = CalcMotColors.TextMuted
        )
        Spacer(modifier = Modifier.height(CalcMotSpacing.Md))
        CalcMotButton(
            text = actionLabel,
            onClick = onActionClick,
            modifier = Modifier.fillMaxWidth(),
            variant = if (statusVariant == CalcMotStatusVariant.Active) CalcMotButtonVariant.Secondary else CalcMotButtonVariant.Primary
        )
    }
}
