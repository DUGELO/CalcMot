package br.com.calcmot.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import br.com.calcmot.AppSettings
import br.com.calcmot.finance.ledger.DashboardOverview
import br.com.calcmot.finance.ledger.DashboardRepositoryProvider
import br.com.calcmot.finance.ledger.ExpenseSummaryRow
import br.com.calcmot.finance.ledger.FinancialAnalysis
import br.com.calcmot.finance.ledger.MoneySummaryRow
import br.com.calcmot.finance.ledger.OfferSummaryRow
import br.com.calcmot.finance.ledger.PlatformOfferSummaryRow
import br.com.calcmot.telemetry.AnalyticsEvents
import br.com.calcmot.telemetry.AnalyticsParams
import br.com.calcmot.telemetry.TelemetryProvider
import br.com.calcmot.ui.design.components.CalcMotCard
import br.com.calcmot.ui.design.components.CalcMotCardVariant
import br.com.calcmot.ui.design.components.CalcMotSectionHeader
import br.com.calcmot.ui.design.components.CalcMotStatusBadge
import br.com.calcmot.ui.design.components.CalcMotSwitchRow
import br.com.calcmot.ui.design.components.CalcMotFeedbackAction
import br.com.calcmot.ui.design.components.CalcMotFeedbackActionStyle
import br.com.calcmot.ui.design.components.CalcMotFeedbackAlertDialog
import br.com.calcmot.ui.design.components.CalcMotFeedbackTone
import br.com.calcmot.ui.design.components.CalcMotEmptyState
import br.com.calcmot.ui.design.components.CalcMotInfoBanner
import br.com.calcmot.ui.design.components.CalcMotListItem
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotSpacing
import br.com.calcmot.ui.design.tokens.CalcMotTypography
import br.com.calcmot.ui.theme.MetricaTheme
import java.text.NumberFormat
import java.util.Locale

internal data class OpenDashboardUiState(
    val offerSummary: OfferSummaryRow = OfferSummaryRow(0, 0, 0, 0, 0, null, null),
    val platformSummaries: List<PlatformOfferSummaryRow> = emptyList(),
    val earnings: MoneySummaryRow = MoneySummaryRow(0L, 0),
    val expenses: ExpenseSummaryRow = ExpenseSummaryRow(0L, 0L, 0),
    val analysis: FinancialAnalysis = FinancialAnalysis(emptyList(), null, null, null, null),
    val dailyGrossGoalCents: Long = 0L,
    val historyEnabled: Boolean = false
) {
    val uberConfirmedReadings: Long get() = platformSummaries.firstOrNull { it.platform == "uber" }?.offerCount?.toLong() ?: 0L
    val ninetyNineConfirmedReadings: Long get() = platformSummaries.firstOrNull { it.platform == "99" }?.offerCount?.toLong() ?: 0L
    val hasUberData: Boolean get() = uberConfirmedReadings > 0L
    val hasNinetyNineData: Boolean get() = ninetyNineConfirmedReadings > 0L
    val hasAnyData: Boolean get() = offerSummary.offerCount > 0 || earnings.itemCount > 0 || expenses.itemCount > 0
    val totalConfirmedReadings: Long get() = uberConfirmedReadings + ninetyNineConfirmedReadings
    val confirmedGrossCents: Long? get() = earnings.totalCents.takeIf { earnings.itemCount > 0 }
    val netCents: Long? get() = confirmedGrossCents?.minus(expenses.operatingTotalCents)

    companion object {
        fun from(overview: DashboardOverview, historyEnabled: Boolean) = OpenDashboardUiState(
            offerSummary = overview.offerSummary,
            platformSummaries = overview.platformSummaries,
            earnings = overview.earnings,
            expenses = overview.expenses,
            analysis = overview.analysis,
            dailyGrossGoalCents = overview.dailyGrossGoalCents,
            historyEnabled = historyEnabled
        )
    }
}

internal enum class ReadingConfidence(val label: String, val color: Color) {
    CONFIRMED("Confirmado", CalcMotColors.Success),
    PARTIAL("Leitura parcial", CalcMotColors.Warning),
    DRIVER_CORRECTED("Corrigido por você", CalcMotColors.BrandPrimary),
    ESTIMATED("Estimado", CalcMotColors.TextMuted),
    INSUFFICIENT("Dados insuficientes", CalcMotColors.TextMuted)
}

internal enum class DashboardDataSource(val label: String, val color: Color) {
    UBER("Fonte Uber", CalcMotColors.Success),
    NINETY_NINE("Fonte 99", CalcMotColors.Warning),
    DRIVER("Informado pelo motorista", CalcMotColors.BrandPrimary),
    FUTURE("Fonte futura", CalcMotColors.TextMuted)
}

@Composable
internal fun OpenDashboardFinanceRoute(
    onBack: () -> Unit,
    onOpenActivity: () -> Unit = {},
    onOpenInsights: () -> Unit = {},
    onOpenSources: () -> Unit = {},
    onOpenReports: () -> Unit = {},
) {
    val context = LocalContext.current
    val repository = remember(context) { DashboardRepositoryProvider.get(context) }
    val overview by remember(repository) { repository.observeOverview() }.collectAsState(initial = null)
    var historyEnabled by remember { mutableStateOf(AppSettings.isFinancialHistoryEnabled(context)) }
    var showDisclosure by remember { mutableStateOf(false) }
    val state = overview?.let { OpenDashboardUiState.from(it, historyEnabled) }
        ?: OpenDashboardUiState(historyEnabled = historyEnabled)

    LaunchedEffect(state.hasAnyData, state.hasUberData, state.hasNinetyNineData) {
        TelemetryProvider.analytics.track(
            AnalyticsEvents.OPEN_DASHBOARD_FINANCE_OPENED,
            mapOf(
                AnalyticsParams.HAS_ANY_DATA to state.hasAnyData.toString(),
                AnalyticsParams.HAS_UBER_DATA to state.hasUberData.toString(),
                AnalyticsParams.HAS_NINETY_NINE_DATA to state.hasNinetyNineData.toString()
            )
        )
    }

    OpenDashboardScreen(
        state = state,
        onBack = onBack,
        onHistoryEnabledChange = { enabled ->
            if (enabled && !AppSettings.hasFinancialHistoryConsent(context)) {
                showDisclosure = true
            } else {
                AppSettings.setFinancialHistoryEnabled(context, enabled)
                historyEnabled = AppSettings.isFinancialHistoryEnabled(context)
            }
        },
        onOpenActivity = onOpenActivity,
        onOpenInsights = onOpenInsights,
        onOpenSources = onOpenSources,
        onOpenReports = onOpenReports
    )

    if (showDisclosure) {
        CalcMotFeedbackAlertDialog(
            title = "Criar seu histórico financeiro?",
            message = "O CalcMot pode guardar neste aparelho as informações confirmadas das ofertas. Não salva imagens, endereços ou passageiros. Você pode pausar ou apagar tudo quando quiser.",
            tone = CalcMotFeedbackTone.INFO,
            primaryAction = CalcMotFeedbackAction(
                label = "Aceitar e ativar",
                onClick = {
                    AppSettings.setFinancialHistoryConsent(context, true)
                    historyEnabled = true
                    showDisclosure = false
                    TelemetryProvider.analytics.track(
                        AnalyticsEvents.FINANCIAL_HISTORY_CONSENT,
                        mapOf(
                            AnalyticsParams.REASON to "active",
                            AnalyticsParams.RECORD_TYPE to "history"
                        )
                    )
                }
            ),
            secondaryAction = CalcMotFeedbackAction(
                label = "Agora não",
                onClick = {
                    AppSettings.setFinancialHistoryConsent(context, false)
                    historyEnabled = false
                    showDisclosure = false
                },
                style = CalcMotFeedbackActionStyle.TEXT
            ),
            onDismissRequest = {}
        )
    }
}

@Composable
internal fun OpenDashboardScreen(
    state: OpenDashboardUiState,
    onBack: () -> Unit,
    onHistoryEnabledChange: (Boolean) -> Unit = {},
    onOpenActivity: () -> Unit = {},
    onOpenInsights: () -> Unit = {},
    onOpenSources: () -> Unit = {},
    onOpenReports: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    FinancialFeatureScreen(
        title = "Dashboard Financeiro",
        onBack = onBack,
        modifier = modifier.testTag(UiTestTags.OPEN_DASHBOARD_FINANCE_SCREEN)
    ) {
        item { OpenDashboardHeader() }
        item { FinancialHistoryStatusCard(state.historyEnabled, onHistoryEnabledChange) }

        item {
            CalcMotSectionHeader(
                title = "Hoje",
                subtitle = "O que o Cockpit já conseguiu organizar no seu turno."
            )
        }
        item {
            if (state.hasAnyData) {
                Column(verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)
                    ) {
                        UnifiedMetricCard(
                            label = "Ofertas analisadas",
                            value = state.offerSummary.offerCount.toString(),
                            supporting = "leituras confirmadas",
                            modifier = Modifier.weight(1f)
                        )
                        UnifiedMetricCard(
                            label = "Média por km",
                            value = state.offerSummary.averageValuePerKm?.let(::formatCurrency) ?: "--",
                            supporting = "nas ofertas de hoje",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    FinanceSummaryCard(state)
                }
            } else {
                CalcMotEmptyState(
                    title = "Seu resumo começa no próximo turno",
                    body = "Use o Cockpit com a Uber ou a 99 para preencher o painel com leituras confirmadas."
                )
            }
        }

        item {
            CalcMotSectionHeader(
                title = "Acesso rápido",
                subtitle = "Escolha o que deseja acompanhar ou ajustar."
            )
        }
        item {
            DashboardQuickActions(
                onOpenActivity = onOpenActivity,
                onOpenInsights = onOpenInsights,
                onOpenReports = onOpenReports,
                onOpenSources = onOpenSources
            )
        }
    }
}

@Composable
private fun DashboardQuickActions(
    onOpenActivity: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenSources: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)) {
        DashboardQuickActionRow(
            first = DashboardQuickAction(
                "Atividade",
                Icons.Outlined.ReceiptLong,
                UiTestTags.FINANCIAL_ACTIVITY_ACTION,
                onOpenActivity
            ),
            second = DashboardQuickAction(
                "Desempenho",
                Icons.Outlined.QueryStats,
                UiTestTags.FINANCIAL_INSIGHTS_ACTION,
                onOpenInsights
            )
        )
        DashboardQuickActionRow(
            first = DashboardQuickAction(
                "Relatórios",
                Icons.Outlined.ReceiptLong,
                UiTestTags.FINANCIAL_REPORTS_ACTION,
                onOpenReports
            ),
            second = DashboardQuickAction(
                "Fontes e dados",
                Icons.Outlined.Visibility,
                UiTestTags.FINANCIAL_SOURCES_ACTION,
                onOpenSources
            )
        )
    }
}

@Composable
private fun DashboardQuickActionRow(
    first: DashboardQuickAction,
    second: DashboardQuickAction
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)
    ) {
        DashboardQuickActionCard(first, Modifier.weight(1f).fillMaxHeight())
        DashboardQuickActionCard(second, Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun DashboardQuickActionCard(
    action: DashboardQuickAction,
    modifier: Modifier = Modifier
) {
    CalcMotListItem(
        title = action.title,
        description = null,
        icon = action.icon,
        onClick = action.onClick,
        modifier = modifier.testTag(action.testTag),
        compact = true
    )
}

private data class DashboardQuickAction(
    val title: String,
    val icon: ImageVector,
    val testTag: String,
    val onClick: () -> Unit
)

@Composable
internal fun OpenDashboardHeader() {
    CalcMotCard(variant = CalcMotCardVariant.HIGHLIGHT) {
        Row(
            modifier = Modifier.padding(CalcMotSpacing.CardPadding),
            horizontalArrangement = Arrangement.spacedBy(CalcMotSpacing.Md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.QueryStats,
                contentDescription = null,
                modifier = Modifier.size(CalcMotSpacing.Xl),
                tint = CalcMotColors.Success
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Xs)
            ) {
                Text(
                    text = "Seu painel financeiro",
                    style = CalcMotTypography.CardTitle,
                    color = CalcMotColors.TextPrimary
                )
                Text(
                    text = "Uber, 99, ganhos e custos organizados em um só lugar.",
                    style = CalcMotTypography.Body,
                    color = CalcMotColors.TextSecondary
                )
            }
        }
    }
}

@Composable
internal fun PlatformDataSourceCard(
    platform: String,
    status: String,
    source: String,
    detail: String,
    icon: ImageVector,
    accent: Color
) {
    CalcMotCard {
        Row(
            modifier = Modifier.padding(CalcMotSpacing.CardPadding),
            horizontalArrangement = Arrangement.spacedBy(CalcMotSpacing.Md),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(CalcMotSpacing.Xl),
                tint = accent
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Xs)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(platform, style = CalcMotTypography.CardTitle, color = CalcMotColors.TextPrimary)
                    CalcMotStatusBadge(text = status, color = accent)
                }
                Text(source, style = CalcMotTypography.Body, color = CalcMotColors.TextSecondary)
                Text(detail, style = CalcMotTypography.Caption, color = CalcMotColors.TextMuted)
            }
        }
    }
}

@Composable
internal fun DataSourceBadge(source: DashboardDataSource) {
    CalcMotStatusBadge(text = source.label, color = source.color)
}

@Composable
internal fun ReadingConfidenceBadge(confidence: ReadingConfidence) {
    CalcMotStatusBadge(text = confidence.label, color = confidence.color)
}

@Composable
internal fun ReadingTrustCard() {
    CalcMotCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CalcMotSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)
        ) {
            Text(
                text = "Qualidade dos dados",
                style = CalcMotTypography.CardTitle,
                color = CalcMotColors.TextPrimary
            )
            Text(
                text = "Dados confirmados entram nas métricas. Quando a leitura não estiver clara, o painel sinaliza em vez de completar a informação.",
                style = CalcMotTypography.Body,
                color = CalcMotColors.TextSecondary
            )
            Column(verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)) {
                Row(horizontalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)) {
                    ReadingConfidenceBadge(ReadingConfidence.CONFIRMED)
                    ReadingConfidenceBadge(ReadingConfidence.PARTIAL)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)) {
                    ReadingConfidenceBadge(ReadingConfidence.DRIVER_CORRECTED)
                    ReadingConfidenceBadge(ReadingConfidence.ESTIMATED)
                }
            }
        }
    }
}

@Composable
internal fun PartialDataWarning(text: String) {
    CalcMotInfoBanner(title = "Importante", body = text)
}

@Composable
internal fun UnifiedMetricCard(
    label: String,
    value: String,
    supporting: String,
    modifier: Modifier = Modifier
) {
    CalcMotCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(CalcMotSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Xs)
        ) {
            Text(value, style = CalcMotTypography.MetricValue, color = CalcMotColors.Success)
            Text(label, style = CalcMotTypography.BodyStrong, color = CalcMotColors.TextPrimary)
            Text(supporting, style = CalcMotTypography.Caption, color = CalcMotColors.TextMuted)
        }
    }
}

@Composable
internal fun PlatformComparisonCard(state: OpenDashboardUiState) {
    val uber = state.platformSummaries.firstOrNull { it.platform == "uber" }
    val ninetyNine = state.platformSummaries.firstOrNull { it.platform == "99" }
    if (uber == null || ninetyNine == null) {
        CalcMotEmptyState(
            title = "Dados insuficientes para comparar",
            body = "Quando houver dados confirmados das duas plataformas, o Cockpit mostrará qual está performando melhor para sua meta."
        )
        return
    }
    CalcMotCard {
        Column(
            modifier = Modifier.padding(CalcMotSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)
        ) {
            Text("Comparativo de ofertas", style = CalcMotTypography.CardTitle, color = CalcMotColors.TextPrimary)
            Text(
                "Melhor média por km: ${bestPlatformLabel(uber.averageValuePerKm, ninetyNine.averageValuePerKm)}",
                style = CalcMotTypography.BodyStrong,
                color = CalcMotColors.TextPrimary
            )
            Text(
                "Melhor média por hora: ${bestPlatformLabel(uber.averageValuePerHour, ninetyNine.averageValuePerHour)}",
                style = CalcMotTypography.BodyStrong,
                color = CalcMotColors.TextPrimary
            )
            Text(
                "Base: ${uber.offerCount} ofertas Uber e ${ninetyNine.offerCount} ofertas 99 hoje.",
                style = CalcMotTypography.Caption,
                color = CalcMotColors.TextMuted
            )
        }
    }
}

@Composable
private fun FinancialHistoryStatusCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    CalcMotCard(variant = if (enabled) CalcMotCardVariant.SUCCESS else CalcMotCardVariant.DEFAULT) {
        CalcMotSwitchRow(
            title = if (enabled) "Histórico do painel ativo" else "Histórico do painel pausado",
            description = if (enabled) {
                "Novas leituras confirmadas entram no seu painel."
            } else {
                "O histórico atual fica salvo, mas nenhuma leitura nova é adicionada."
            },
            checked = enabled,
            onCheckedChange = onEnabledChange,
            modifier = Modifier.padding(CalcMotSpacing.CardPadding)
        )
    }
}

@Composable
internal fun DashboardMetricsDetail(state: OpenDashboardUiState) {
    val averageKm = state.offerSummary.averageValuePerKm?.let(::formatCurrency)
    val averageHour = state.offerSummary.averageValuePerHour?.let(::formatCurrency)
    Column(verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)
        ) {
            UnifiedMetricCard(
                label = "Média por km",
                value = averageKm ?: "--",
                supporting = if (averageKm == null) "dados insuficientes" else "ofertas confirmadas",
                modifier = Modifier.weight(1f)
            )
            UnifiedMetricCard(
                label = "Média por hora",
                value = averageHour ?: "--",
                supporting = if (averageHour == null) "dados insuficientes" else "ofertas confirmadas",
                modifier = Modifier.weight(1f)
            )
        }
        InlineMetric(
            label = "Ofertas analisadas",
            value = state.offerSummary.offerCount.takeIf { it > 0 }?.toString() ?: "--",
            supporting = "cards únicos; não são ganhos",
            modifier = Modifier.fillMaxWidth()
        )
        PartialDataWarning(
            text = "Ofertas exibidas não são ganhos. O resumo financeiro usa apenas ganhos confirmados ou informados por você."
        )
    }
}

@Composable
private fun FinanceSummaryCard(
    state: OpenDashboardUiState
) {
    CalcMotCard {
        Column(
            modifier = Modifier.padding(CalcMotSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Md)
        ) {
            Text("Resumo de caixa", style = CalcMotTypography.CardTitle, color = CalcMotColors.TextPrimary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)
            ) {
                InlineMetric(
                    label = "Ganhos confirmados",
                    value = state.confirmedGrossCents?.let(::formatCurrencyCents) ?: "--",
                    supporting = if (state.confirmedGrossCents == null) "aguardando ganho real" else "hoje",
                    modifier = Modifier.weight(1f)
                )
                InlineMetric(
                    label = "Custos de caixa",
                    value = if (state.expenses.itemCount > 0) formatCurrencyCents(state.expenses.totalCents) else "--",
                    supporting = if (state.expenses.itemCount > 0) "hoje" else "nenhum custo",
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = state.netCents?.let { "Saldo estimado: ${formatCurrencyCents(it)}" }
                    ?: "Lucro estimado indisponível sem ganho confirmado.",
                style = CalcMotTypography.BodyStrong,
                color = if (state.netCents != null) CalcMotColors.Success else CalcMotColors.TextSecondary
            )
            if (state.expenses.itemCount > 0 && state.expenses.operatingTotalCents != state.expenses.totalCents) {
                Text(
                    text = "Impacto operacional: ${formatCurrencyCents(state.expenses.operatingTotalCents)}",
                    style = CalcMotTypography.Caption,
                    color = CalcMotColors.TextMuted
                )
            }
            Text(
                text = state.confirmedGrossCents?.let { gross ->
                    val progress = if (state.dailyGrossGoalCents > 0L) {
                        (gross * 100L / state.dailyGrossGoalCents).coerceAtMost(999L)
                    } else 0L
                    "Meta diária: ${formatCurrencyCents(state.dailyGrossGoalCents)} · $progress% confirmado"
                } ?: "Meta diária: ${formatCurrencyCents(state.dailyGrossGoalCents)} · aguardando ganhos",
                style = CalcMotTypography.Caption,
                color = CalcMotColors.TextSecondary
            )
        }
    }
}

@Composable
internal fun ShiftAnalysisCard(analysis: FinancialAnalysis) {
    val bestByKm = analysis.bestHourByKm
    val worstByKm = analysis.worstHourByKm
    CalcMotCard {
        Column(
            modifier = Modifier.padding(CalcMotSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)
        ) {
            if (bestByKm == null || worstByKm == null) {
                Text(
                    text = "Aguardando amostra suficiente",
                    style = CalcMotTypography.CardTitle,
                    color = CalcMotColors.TextPrimary
                )
                Text(
                    text = "São necessárias pelo menos 3 ofertas confirmadas na mesma faixa horária para comparar desempenho.",
                    style = CalcMotTypography.Body,
                    color = CalcMotColors.TextSecondary
                )
            } else {
                Text("Horários com dados suficientes", style = CalcMotTypography.CardTitle, color = CalcMotColors.TextPrimary)
                Text(
                    text = "Melhor por km: ${bestByKm.hourOfDay.asHourRange()} · ${formatCurrency(bestByKm.medianValuePerKm)}/km",
                    style = CalcMotTypography.BodyStrong,
                    color = CalcMotColors.Success
                )
                Text(
                    text = "Menor mediana por km: ${worstByKm.hourOfDay.asHourRange()} · ${formatCurrency(worstByKm.medianValuePerKm)}/km",
                    style = CalcMotTypography.Body,
                    color = CalcMotColors.TextSecondary
                )
            }
        }
    }
}

private fun Int.asHourRange(): String {
    val next = (this + 1) % 24
    return "%02dh-%02dh".format(this, next)
}

@Composable
private fun InlineMetric(
    label: String,
    value: String,
    supporting: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Xs)
    ) {
        Text(value, style = CalcMotTypography.MetricValue, color = CalcMotColors.Success)
        Text(label, style = CalcMotTypography.BodyStrong, color = CalcMotColors.TextPrimary)
        Text(supporting, style = CalcMotTypography.Caption, color = CalcMotColors.TextMuted)
    }
}

@Composable
internal fun PrivacyControlCard(onClick: () -> Unit = {}) {
    CalcMotListItem(
        title = "Gerenciar histórico",
        description = "Pause ou apague os dados financeiros salvos neste aparelho.",
        icon = Icons.Outlined.Lock,
        onClick = onClick,
        accent = CalcMotColors.Success
    )
}

internal fun confirmedReadingsLabel(count: Long): String = when (count) {
    0L -> "Aguardando dados reais"
    1L -> "1 leitura confirmada neste aparelho"
    else -> "$count leituras confirmadas neste aparelho"
}

private val dashboardCurrencyFormatter: NumberFormat =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))

private fun formatCurrency(value: Double): String = dashboardCurrencyFormatter.format(value)
private fun formatCurrencyCents(value: Long): String = dashboardCurrencyFormatter.format(value / 100.0)
private fun bestPlatformLabel(uber: Double?, ninetyNine: Double?): String = when {
    uber == null || ninetyNine == null -> "dados insuficientes"
    kotlin.math.abs(uber - ninetyNine) < 0.01 -> "empate"
    uber > ninetyNine -> "Uber (${formatCurrency(uber)})"
    else -> "99 (${formatCurrency(ninetyNine)})"
}

@Preview(name = "Open Dashboard vazio", widthDp = 393, heightDp = 852)
@Composable
private fun OpenDashboardEmptyPreview() {
    MetricaTheme {
        OpenDashboardScreen(state = OpenDashboardUiState(), onBack = {})
    }
}

@Preview(name = "Open Dashboard parcial", widthDp = 360, heightDp = 800, fontScale = 1.2f)
@Composable
private fun OpenDashboardPartialPreview() {
    MetricaTheme {
        OpenDashboardScreen(
            state = OpenDashboardUiState(
                offerSummary = OfferSummaryRow(25, 4, 9, 5, 7, 2.18, 41.50),
                platformSummaries = listOf(
                    PlatformOfferSummaryRow("uber", 18, 9, 4, 5, 2.20, 42.0),
                    PlatformOfferSummaryRow("99", 7, 4, 1, 2, 2.12, 39.8)
                ),
                historyEnabled = true
            ),
            onBack = {}
        )
    }
}
