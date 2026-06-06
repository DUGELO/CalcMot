package br.com.calcmot.ui.design.domain

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import br.com.calcmot.ui.design.components.CalcMotCard
import br.com.calcmot.ui.design.components.CalcMotStatusBadge
import br.com.calcmot.ui.design.components.CalcMotStatusVariant
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotSpacing
import br.com.calcmot.ui.design.tokens.CalcMotTypography

@Composable
fun ServiceHealthCard(
    serviceActive: Boolean,
    overlayPermitted: Boolean,
    lastCapture: String,
    modifier: Modifier = Modifier
) {
    CalcMotCard(modifier = modifier) {
        Text(
            text = "Saúde do Serviço",
            style = CalcMotTypography.CardTitle,
            color = CalcMotColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(CalcMotSpacing.Md))
        HealthRow(
            label = "Serviço Ativo",
            status = if (serviceActive) "Sim" else "Não",
            variant = if (serviceActive) CalcMotStatusVariant.Active else CalcMotStatusVariant.Error
        )
        Spacer(modifier = Modifier.height(CalcMotSpacing.Sm))
        HealthRow(
            label = "Sobreposição",
            status = if (overlayPermitted) "OK" else "Pendente",
            variant = if (overlayPermitted) CalcMotStatusVariant.Active else CalcMotStatusVariant.Attention
        )
        Spacer(modifier = Modifier.height(CalcMotSpacing.Sm))
        Text(
            text = "Última captura: $lastCapture",
            style = CalcMotTypography.Caption,
            color = CalcMotColors.TextMuted
        )
    }
}

@Composable
private fun HealthRow(
    label: String,
    status: String,
    variant: CalcMotStatusVariant
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = CalcMotTypography.Body,
            color = CalcMotColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        CalcMotStatusBadge(text = status, variant = variant)
    }
}
