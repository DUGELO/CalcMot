package br.com.calcmot.ui.design.domain

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import br.com.calcmot.ui.design.components.CalcMotButton
import br.com.calcmot.ui.design.components.CalcMotButtonVariant
import br.com.calcmot.ui.design.components.CalcMotCard
import br.com.calcmot.ui.design.components.CalcMotCardVariant
import br.com.calcmot.ui.design.components.CalcMotStatusBadge
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotSpacing
import br.com.calcmot.ui.design.tokens.CalcMotTypography

enum class PermissionStatus {
    ACTIVE,
    PENDING,
    REQUIRED,
    ERROR
}

data class DailySummaryUiState(
    val offersAnalyzed: Int,
    val offersAboveGoal: Int,
    val offersBelowGoal: Int,
    val averagePerKm: String,
    val averagePerHour: String
)

@Composable
fun PermissionStatusCard(
    title: String,
    description: String,
    status: PermissionStatus,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    actionModifier: Modifier = Modifier.fillMaxWidth()
) {
    CalcMotCard(
        modifier = modifier,
        variant = if (status == PermissionStatus.ACTIVE) CalcMotCardVariant.SUCCESS else CalcMotCardVariant.HIGHLIGHT
    ) {
        Column(
            modifier = Modifier.padding(CalcMotSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Xs)
                ) {
                    Text(text = title, style = CalcMotTypography.CardTitle, color = CalcMotColors.TextPrimary)
                    Text(text = description, style = CalcMotTypography.Body, color = CalcMotColors.TextSecondary)
                }
                CalcMotStatusBadge(text = status.label, color = status.color)
            }
            CalcMotButton(
                text = actionLabel,
                onClick = onAction,
                modifier = actionModifier,
                enabled = status != PermissionStatus.ACTIVE,
                variant = if (status == PermissionStatus.ERROR) CalcMotButtonVariant.DANGER else CalcMotButtonVariant.PRIMARY
            )
        }
    }
}

@Composable
fun GoalPresetCard(
    title: String,
    perKm: String,
    perHour: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CalcMotCard(
        modifier = modifier,
        variant = if (selected) CalcMotCardVariant.PREMIUM else CalcMotCardVariant.DEFAULT,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(CalcMotSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)
        ) {
            Text(text = title, style = CalcMotTypography.CardTitle, color = CalcMotColors.TextPrimary)
            Text(text = "$perKm/km • $perHour/h", style = CalcMotTypography.Body, color = CalcMotColors.TextSecondary)
        }
    }
}

@Composable
fun DailySummaryCard(
    state: DailySummaryUiState,
    modifier: Modifier = Modifier
) {
    CalcMotCard(modifier = modifier, variant = CalcMotCardVariant.HIGHLIGHT) {
        Column(
            modifier = Modifier.padding(CalcMotSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Md)
        ) {
            Text(text = "Resumo de hoje", style = CalcMotTypography.CardTitle, color = CalcMotColors.TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(CalcMotSpacing.Md)) {
                MetricColumn("Ofertas", state.offersAnalyzed.toString(), Modifier.weight(1f))
                MetricColumn("Acima", state.offersAboveGoal.toString(), Modifier.weight(1f))
                MetricColumn("Abaixo", state.offersBelowGoal.toString(), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(CalcMotSpacing.Md)) {
                MetricColumn("Média km", state.averagePerKm, Modifier.weight(1f))
                MetricColumn("Média hora", state.averagePerHour, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun FinancialImpactSummaryCard(
    title: String,
    body: String,
    positive: Boolean,
    modifier: Modifier = Modifier
) {
    CalcMotCard(
        modifier = modifier,
        variant = if (positive) CalcMotCardVariant.SUCCESS else CalcMotCardVariant.DANGER
    ) {
        Column(
            modifier = Modifier.padding(CalcMotSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Xs)
        ) {
            Text(text = title, style = CalcMotTypography.CardTitle, color = CalcMotColors.TextPrimary)
            Text(text = body, style = CalcMotTypography.Body, color = CalcMotColors.TextSecondary)
        }
    }
}

@Composable
fun OfferHistoryItem(
    time: String,
    value: String,
    perKm: String,
    perHour: String,
    classification: String,
    impact: String,
    modifier: Modifier = Modifier
) {
    CalcMotCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(CalcMotSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = value, style = CalcMotTypography.CardTitle, color = CalcMotColors.TextPrimary)
                CalcMotStatusBadge(text = classification, color = CalcMotColors.BrandPrimary)
            }
            Text(text = "$time • $perKm/km • $perHour/h", style = CalcMotTypography.Body, color = CalcMotColors.TextSecondary)
            Text(text = impact, style = CalcMotTypography.Caption, color = CalcMotColors.TextMuted)
        }
    }
}

@Composable
fun ServiceHealthCard(
    serviceStatus: String,
    overlayStatus: String,
    lastCapture: String,
    lastOverlay: String,
    modifier: Modifier = Modifier
) {
    CalcMotCard(modifier = modifier, variant = CalcMotCardVariant.HIGHLIGHT) {
        Column(
            modifier = Modifier.padding(CalcMotSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)
        ) {
            Text(text = "Saúde do serviço", style = CalcMotTypography.CardTitle, color = CalcMotColors.TextPrimary)
            HealthLine("Serviço", serviceStatus)
            HealthLine("Overlay", overlayStatus)
            HealthLine("Última captura", lastCapture)
            HealthLine("Último overlay", lastOverlay)
        }
    }
}

@Composable
fun BetaFeedbackCard(
    onPositive: () -> Unit,
    onNegative: () -> Unit,
    modifier: Modifier = Modifier
) {
    CalcMotCard(modifier = modifier, variant = CalcMotCardVariant.PREMIUM) {
        Column(
            modifier = Modifier.padding(CalcMotSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Md)
        ) {
            Text(text = "O app te ajudou hoje?", style = CalcMotTypography.CardTitle, color = CalcMotColors.TextPrimary)
            Text(
                text = "Seu retorno ajuda a priorizar histórico, metas e diagnóstico beta.",
                style = CalcMotTypography.Body,
                color = CalcMotColors.TextSecondary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)) {
                CalcMotButton(text = "Sim", onClick = onPositive, modifier = Modifier.weight(1f))
                CalcMotButton(
                    text = "Não",
                    onClick = onNegative,
                    modifier = Modifier.weight(1f),
                    variant = CalcMotButtonVariant.SECONDARY
                )
            }
        }
    }
}

@Composable
private fun MetricColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Xs)) {
        Text(text = value, style = CalcMotTypography.MetricValue, color = CalcMotColors.TextPrimary)
        Text(text = label, style = CalcMotTypography.MetricLabel, color = CalcMotColors.TextSecondary)
    }
}

@Composable
private fun HealthLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = CalcMotTypography.Body, color = CalcMotColors.TextSecondary)
        Text(text = value, style = CalcMotTypography.BodyStrong, color = CalcMotColors.TextPrimary)
    }
}

private val PermissionStatus.label: String
    get() = when (this) {
        PermissionStatus.ACTIVE -> "Ativo"
        PermissionStatus.PENDING -> "Pendente"
        PermissionStatus.REQUIRED -> "Necessário"
        PermissionStatus.ERROR -> "Erro"
    }

private val PermissionStatus.color
    get() = when (this) {
        PermissionStatus.ACTIVE -> CalcMotColors.Success
        PermissionStatus.PENDING -> CalcMotColors.Warning
        PermissionStatus.REQUIRED -> CalcMotColors.Warning
        PermissionStatus.ERROR -> CalcMotColors.Danger
    }
