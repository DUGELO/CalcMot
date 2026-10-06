package br.com.calcmot.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import br.com.calcmot.AppSettings
import br.com.calcmot.finance.ledger.DashboardRepositoryProvider
import br.com.calcmot.ui.design.components.CalcMotSectionHeader
import br.com.calcmot.ui.design.components.CalcMotListItem
import br.com.calcmot.ui.design.components.CalcMotEmptyState
import br.com.calcmot.ui.design.components.CalcMotInfoBanner
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotSpacing
import br.com.calcmot.ui.theme.MetricaTheme

@Composable
internal fun FinancialActivityRoute(
    onBack: () -> Unit,
    onOpenStatement: () -> Unit,
    onOpenCosts: () -> Unit,
    onOpenSessions: () -> Unit
) {
    FinancialFeatureScreen(
        title = "Atividade",
        onBack = onBack,
        modifier = Modifier.testTag(UiTestTags.FINANCIAL_ACTIVITY_SCREEN)
    ) {
        item {
            CalcMotSectionHeader(
                title = "Seu turno",
                subtitle = "Revise o turno e complete o que faltar."
            )
        }
        item {
            CalcMotListItem(
                title = "Extrato do motorista",
                description = "Veja as ofertas confirmadas e revise uma leitura.",
                icon = Icons.Outlined.ReceiptLong,
                onClick = onOpenStatement
            )
        }
        item {
            CalcMotListItem(
                title = "Custos",
                description = "Registre combustível, manutenção e outros gastos.",
                icon = Icons.Outlined.AccountBalanceWallet,
                onClick = onOpenCosts
            )
        }
        item {
            CalcMotListItem(
                title = "Turnos",
                description = "Confira, una ou ajuste os horários registrados.",
                icon = Icons.Outlined.CalendarToday,
                onClick = onOpenSessions
            )
        }
    }
}

@Composable
internal fun FinancialInsightsRoute(
    onBack: () -> Unit,
    onOpenDailyGoal: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember(context) { DashboardRepositoryProvider.get(context) }
    val overview by remember(repository) { repository.observeOverview() }.collectAsState(initial = null)
    val state = overview?.let {
        OpenDashboardUiState.from(
            overview = it,
            historyEnabled = AppSettings.isFinancialHistoryEnabled(context)
        )
    } ?: OpenDashboardUiState(historyEnabled = AppSettings.isFinancialHistoryEnabled(context))

    FinancialInsightsScreen(
        state = state,
        onBack = onBack,
        onOpenDailyGoal = onOpenDailyGoal
    )
}

@Composable
internal fun FinancialInsightsScreen(
    state: OpenDashboardUiState,
    onBack: () -> Unit,
    onOpenDailyGoal: () -> Unit
) {
    FinancialFeatureScreen(
        title = "Desempenho",
        onBack = onBack,
        modifier = Modifier.testTag(UiTestTags.FINANCIAL_INSIGHTS_SCREEN)
    ) {
        if (state.hasAnyData) {
            item {
                CalcMotSectionHeader(
                    title = "Visão do turno",
                    subtitle = "Médias e volume das ofertas confirmadas de hoje."
                )
            }
            item {
                DashboardMetricsDetail(state)
            }

            if (state.hasUberData && state.hasNinetyNineData) {
                item {
                    CalcMotSectionHeader(
                        title = "Uber x 99",
                        subtitle = "Compare as plataformas usando somente leituras confirmadas."
                    )
                }
                item { PlatformComparisonCard(state) }
            }

            if (state.analysis.bestHourByKm != null && state.analysis.worstHourByKm != null) {
                item {
                    CalcMotSectionHeader(
                        title = "Horários do dia",
                        subtitle = "Faixas com pelo menos três leituras confirmadas."
                    )
                }
                item { ShiftAnalysisCard(state.analysis) }
            }
        } else {
            item {
                CalcMotSectionHeader(
                    title = "Desempenho do turno",
                    subtitle = "Médias, comparativos e melhores horários em um só lugar."
                )
            }
            item {
                CalcMotEmptyState(
                    title = "Aguardando dados do turno",
                    body = "Use o Cockpit durante seu turno. As análises aparecem quando houver leituras confirmadas."
                )
            }
        }

        item {
            CalcMotSectionHeader(
                title = "Planejamento",
                subtitle = "Defina quanto deseja faturar no dia."
            )
        }
        item {
            CalcMotListItem(
                title = "Meta diária",
                description = "Acompanhe o progresso do faturamento confirmado.",
                icon = Icons.Outlined.Flag,
                onClick = onOpenDailyGoal
            )
        }
    }
}

@Composable
internal fun FinancialSourcesRoute(
    onBack: () -> Unit,
    onOpenDataQuality: () -> Unit,
    onOpenDataManagement: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember(context) { DashboardRepositoryProvider.get(context) }
    val overview by remember(repository) { repository.observeOverview() }.collectAsState(initial = null)
    val state = overview?.let {
        OpenDashboardUiState.from(
            overview = it,
            historyEnabled = AppSettings.isFinancialHistoryEnabled(context)
        )
    } ?: OpenDashboardUiState(historyEnabled = AppSettings.isFinancialHistoryEnabled(context))

    FinancialSourcesScreen(
        state = state,
        onBack = onBack,
        onOpenDataQuality = onOpenDataQuality,
        onOpenDataManagement = onOpenDataManagement
    )
}

@Composable
internal fun FinancialSourcesScreen(
    state: OpenDashboardUiState,
    onBack: () -> Unit,
    onOpenDataQuality: () -> Unit,
    onOpenDataManagement: () -> Unit
) {
    FinancialFeatureScreen(
        title = "Fontes e dados",
        onBack = onBack,
        modifier = Modifier.testTag(UiTestTags.FINANCIAL_SOURCES_SCREEN)
    ) {
        item {
            CalcMotSectionHeader(
                title = "Apps compatíveis",
                subtitle = "Veja de onde vêm as informações que formam seu painel."
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)) {
                PlatformDataSourceCard(
                    platform = "Uber",
                    status = "Compatível",
                    source = "Ofertas lidas quando aparecem no app",
                    detail = confirmedReadingsLabel(state.uberConfirmedReadings),
                    icon = Icons.Outlined.DirectionsCar,
                    accent = CalcMotColors.Success
                )
                PlatformDataSourceCard(
                    platform = "99",
                    status = "Compatível",
                    source = "Leitura visual feita no próprio aparelho",
                    detail = confirmedReadingsLabel(state.ninetyNineConfirmedReadings),
                    icon = Icons.Outlined.Visibility,
                    accent = CalcMotColors.Warning
                )
            }
        }
        item {
            CalcMotInfoBanner(
                title = "Outros apps em breve",
                body = "inDrive, entregas, corridas particulares e registros manuais estão em estudo."
            )
        }

        item {
            CalcMotSectionHeader(
                title = "Controle",
                subtitle = "Entenda a leitura ou gerencie o histórico salvo."
            )
        }
        item {
            CalcMotListItem(
                title = "Confiança da leitura",
                description = "Saiba quando um dado está confirmado, parcial ou estimado.",
                icon = Icons.Outlined.Shield,
                onClick = onOpenDataQuality
            )
        }
        item {
            CalcMotListItem(
                title = "Seus dados",
                description = "Pause ou apague o histórico financeiro deste aparelho.",
                icon = Icons.Outlined.Lock,
                onClick = onOpenDataManagement
            )
        }
    }
}

@Composable
internal fun FinancialDataQualityRoute(
    onBack: () -> Unit,
    onOpenDataManagement: () -> Unit
) {
    FinancialFeatureScreen(
        title = "Confiança da leitura",
        onBack = onBack,
        modifier = Modifier.testTag(UiTestTags.FINANCIAL_DATA_QUALITY_SCREEN)
    ) {
        item {
            CalcMotSectionHeader(
                title = "Como interpretar",
                subtitle = "O Cockpit identifica a origem e a qualidade de cada informação."
            )
        }
        item { ReadingTrustCard() }
        item {
            CalcMotSectionHeader(
                title = "Privacidade e controle",
                subtitle = "O histórico fica no aparelho e não guarda imagens, endereços ou passageiros."
            )
        }
        item { PrivacyControlCard(onOpenDataManagement) }
    }
}

@Preview(name = "Financeiro - atividade", widthDp = 360, heightDp = 800)
@Composable
private fun FinancialActivityPreview() {
    MetricaTheme {
        FinancialActivityRoute(
            onBack = {},
            onOpenStatement = {},
            onOpenCosts = {},
            onOpenSessions = {}
        )
    }
}

@Preview(name = "Financeiro - desempenho", widthDp = 393, heightDp = 852, fontScale = 1.2f)
@Composable
private fun FinancialInsightsPreview() {
    MetricaTheme {
        FinancialInsightsScreen(
            state = OpenDashboardUiState(),
            onBack = {},
            onOpenDailyGoal = {}
        )
    }
}
