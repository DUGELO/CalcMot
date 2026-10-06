package br.com.calcmot.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import br.com.calcmot.AppSettings
import br.com.calcmot.finance.ledger.ExpenseCategory
import br.com.calcmot.finance.ledger.FinanceLedgerProvider
import br.com.calcmot.finance.ledger.LedgerPeriod
import br.com.calcmot.finance.ledger.LocalReportExporter
import br.com.calcmot.finance.ledger.LockedPremiumEntitlementProvider
import br.com.calcmot.finance.ledger.OfferCorrection
import br.com.calcmot.finance.ledger.OfferRecordEntity
import br.com.calcmot.finance.ledger.WorkSessionEntity
import br.com.calcmot.telemetry.AnalyticsEvents
import br.com.calcmot.telemetry.AnalyticsParams
import br.com.calcmot.telemetry.MetricsResearchProvider
import br.com.calcmot.telemetry.TelemetryProvider
import br.com.calcmot.ui.design.components.CalcMotButton
import br.com.calcmot.ui.design.components.CalcMotButtonVariant
import br.com.calcmot.ui.design.components.CalcMotBannerVariant
import br.com.calcmot.ui.design.components.CalcMotCard
import br.com.calcmot.ui.design.components.CalcMotCardVariant
import br.com.calcmot.ui.design.components.CalcMotEmptyState
import br.com.calcmot.ui.design.components.CalcMotFeedbackAction
import br.com.calcmot.ui.design.components.CalcMotFeedbackActionStyle
import br.com.calcmot.ui.design.components.CalcMotFeedbackAlertDialog
import br.com.calcmot.ui.design.components.CalcMotFeedbackTone
import br.com.calcmot.ui.design.components.CalcMotNumberField
import br.com.calcmot.ui.design.components.CalcMotInfoBanner
import br.com.calcmot.ui.design.components.CalcMotSwitchRow
import br.com.calcmot.ui.design.components.CalcMotTextField
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotSpacing
import br.com.calcmot.ui.design.tokens.CalcMotTypography
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

@Composable
internal fun FinancialStatementRoute(onBack: () -> Unit, onOpenOffer: (String) -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { FinanceLedgerProvider.repository(context) }
    val period = remember { LedgerPeriod.day() }
    val offers by remember(repository, period) { repository.observeRecentOffers(period, 100) }
        .collectAsState(initial = emptyList())

    FinancialFeatureScreen(title = "Extrato do motorista", onBack = onBack) {
        if (offers.isEmpty()) {
            item {
                CalcMotEmptyState(
                    title = "Seu extrato está vazio",
                    body = "As ofertas confirmadas aparecerão aqui sem guardar detalhes de endereços ou passageiros."
                )
            }
        } else {
            items(offers, key = { it.id }) { offer ->
                OfferStatementItem(offer = offer, onClick = { onOpenOffer(offer.id) })
            }
        }
    }
}

@Composable
private fun OfferStatementItem(offer: OfferRecordEntity, onClick: () -> Unit) {
    CalcMotCard(onClick = onClick) {
        Row(
            modifier = Modifier.padding(CalcMotSpacing.CardPadding),
            horizontalArrangement = Arrangement.spacedBy(CalcMotSpacing.Md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DataSourceBadge(if (offer.platform == "uber") DashboardDataSource.UBER else DashboardDataSource.NINETY_NINE)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Xs)) {
                Text(
                    "${offer.observedAtMillis.asClock()} · ${offer.fareCents.asMoney()}",
                    style = CalcMotTypography.CardTitle,
                    color = CalcMotColors.TextPrimary
                )
                Text(
                    "${offer.totalDistanceKm().formatOne()} km · ${offer.totalMinutes()} min · ${offer.classification.asClassification()}",
                    style = CalcMotTypography.Body,
                    color = CalcMotColors.TextSecondary
                )
                ReadingConfidenceBadge(
                    if (offer.confidence == "DRIVER_CORRECTED") ReadingConfidence.DRIVER_CORRECTED
                    else ReadingConfidence.CONFIRMED
                )
            }
            Icon(Icons.Outlined.Edit, contentDescription = "Revisar registro", tint = CalcMotColors.BrandPrimary)
        }
    }
}

@Composable
internal fun OfferCorrectionRoute(offerId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { FinanceLedgerProvider.repository(context) }
    val scope = rememberCoroutineScope()
    var offer by remember { mutableStateOf<OfferRecordEntity?>(null) }
    var fare by rememberSaveable { mutableStateOf("") }
    var pickupKm by rememberSaveable { mutableStateOf("") }
    var pickupMin by rememberSaveable { mutableStateOf("") }
    var tripKm by rememberSaveable { mutableStateOf("") }
    var tripMin by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(offerId) {
        offer = repository.getOffer(offerId)
        offer?.let {
            fare = (it.fareCents / 100.0).formatTwo()
            pickupKm = (it.pickupDistanceMeters / 1000.0).formatOne()
            pickupMin = (it.pickupTimeSeconds / 60).toString()
            tripKm = (it.tripDistanceMeters / 1000.0).formatOne()
            tripMin = (it.tripTimeSeconds / 60).toString()
        }
    }

    FinancialFeatureScreen(title = "Revisar oferta", onBack = onBack, imePadding = true) {
        item {
            CalcMotInfoBanner(
                title = "Revise somente o necessário",
                body = "Corrija o que estiver diferente da oferta. A leitura original continua registrada."
            )
        }
        item { CalcMotNumberField(fare, { fare = it }, "Valor da oferta (R$)", Modifier.fillMaxWidth()) }
        item { CalcMotNumberField(pickupKm, { pickupKm = it }, "Distância até o passageiro (km)", Modifier.fillMaxWidth()) }
        item { CalcMotNumberField(pickupMin, { pickupMin = it }, "Tempo até o passageiro (min)", Modifier.fillMaxWidth()) }
        item { CalcMotNumberField(tripKm, { tripKm = it }, "Distância da viagem (km)", Modifier.fillMaxWidth()) }
        item { CalcMotNumberField(tripMin, { tripMin = it }, "Tempo da viagem (min)", Modifier.fillMaxWidth(), imeAction = ImeAction.Done) }
        error?.let { message -> item { Text(message, color = CalcMotColors.Danger, style = CalcMotTypography.Body) } }
        item {
            CalcMotButton(
                text = "Salvar correção",
                onClick = {
                    val correction = OfferCorrection(
                        fareCents = fare.toCentsOrNull() ?: -1L,
                        pickupDistanceMeters = pickupKm.toMetersOrNull() ?: -1L,
                        pickupTimeSeconds = (pickupMin.toIntOrNull() ?: -1) * 60,
                        tripDistanceMeters = tripKm.toMetersOrNull() ?: -1L,
                        tripTimeSeconds = (tripMin.toIntOrNull() ?: -1) * 60
                    )
                    scope.launch {
                        val saved = repository.correctOffer(offerId, correction)
                        if (saved) {
                            TelemetryProvider.analytics.track(
                                AnalyticsEvents.FINANCIAL_RECORD_CORRECTED,
                                mapOf(AnalyticsParams.RECORD_TYPE to "correction", AnalyticsParams.SOURCE to "system")
                            )
                            onBack()
                        } else {
                            error = "Revise os valores informados."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
internal fun FinancialCostsRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { FinanceLedgerProvider.repository(context) }
    val scope = rememberCoroutineScope()
    var amount by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var categoryName by rememberSaveable { mutableStateOf(ExpenseCategory.FUEL.name) }
    var affectsProfit by rememberSaveable { mutableStateOf(true) }
    var savedMessage by remember { mutableStateOf<FinancialUiMessage?>(null) }

    FinancialFeatureScreen(title = "Adicionar custo", onBack = onBack, imePadding = true) {
        item { CalcMotNumberField(amount, { amount = it }, "Valor (R$)", Modifier.fillMaxWidth()) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)
            ) {
                ExpenseCategory.entries.forEach { category ->
                    FilterChip(
                        selected = categoryName == category.name,
                        onClick = { categoryName = category.name },
                        label = { Text(category.label()) }
                    )
                }
            }
        }
        item { CalcMotTextField(description, { description = it.take(80) }, "Descrição curta", Modifier.fillMaxWidth()) }
        item {
            CalcMotCard {
                CalcMotSwitchRow(
                    title = "Considerar no lucro operacional",
                    description = "Desative para acompanhar apenas no fluxo de caixa.",
                    checked = affectsProfit,
                    onCheckedChange = { affectsProfit = it },
                    modifier = Modifier.padding(CalcMotSpacing.CardPadding)
                )
            }
        }
        savedMessage?.let { item { FinancialUiMessageBanner(it) } }
        item {
            CalcMotButton(
                text = "Salvar custo",
                onClick = {
                    val cents = amount.toCentsOrNull()
                    if (cents == null || cents <= 0L) {
                        savedMessage = FinancialUiMessage.error("Informe um valor maior que zero.")
                    } else {
                        scope.launch {
                            repository.addExpense(
                                cents,
                                ExpenseCategory.valueOf(categoryName),
                                description,
                                affectsProfit,
                                System.currentTimeMillis()
                            )
                            amount = ""
                            description = ""
                            savedMessage = FinancialUiMessage.success("Custo salvo no histórico deste aparelho.")
                            TelemetryProvider.analytics.track(
                                AnalyticsEvents.FINANCIAL_RECORD_CREATED,
                                mapOf(AnalyticsParams.RECORD_TYPE to "expense", AnalyticsParams.SOURCE to "system")
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
internal fun DailyGrossGoalRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { FinanceLedgerProvider.repository(context) }
    val scope = rememberCoroutineScope()
    var value by rememberSaveable { mutableStateOf((AppSettings.getDailyGrossGoalCents(context) / 100.0).formatTwo()) }
    var message by remember { mutableStateOf<FinancialUiMessage?>(null) }

    FinancialFeatureScreen(title = "Meta diária", onBack = onBack, imePadding = true) {
        item {
            CalcMotInfoBanner(
                title = "Meta de faturamento bruto",
                body = "Ela é separada das metas de R$/km e R$/h usadas para classificar ofertas."
            )
        }
        item { CalcMotNumberField(value, { value = it }, "Meta do dia (R$)", Modifier.fillMaxWidth()) }
        message?.let { item { FinancialUiMessageBanner(it) } }
        item {
            CalcMotButton(
                text = "Salvar meta diária",
                onClick = {
                    val cents = value.toCentsOrNull()
                    if (cents == null || cents <= 0) {
                        message = FinancialUiMessage.error("Informe uma meta maior que zero.")
                    } else scope.launch {
                        repository.setDailyGrossGoal(cents)
                        message = FinancialUiMessage.success("Meta diária salva.")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
internal fun FinancialSessionsRoute(
    onBack: () -> Unit,
    onOpenSession: (String) -> Unit
) {
    val context = LocalContext.current
    val repository = remember(context) { FinanceLedgerProvider.repository(context) }
    val sessions by remember(repository) { repository.observeSessions(LedgerPeriod.day()) }
        .collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    var message by remember { mutableStateOf<FinancialUiMessage?>(null) }

    FinancialFeatureScreen(title = "Turnos", onBack = onBack) {
        if (sessions.isEmpty()) {
            item {
                CalcMotEmptyState(
                    title = "Nenhum turno hoje",
                    body = "O primeiro turno começa com uma atividade confirmada e encerra após 90 minutos sem novos dados."
                )
            }
        } else {
            item {
                CalcMotInfoBanner(
                    title = "Horários estimados",
                    body = "Confira os turnos e ajuste somente quando necessário."
                )
            }
            items(sessions, key = WorkSessionEntity::id) { session ->
                CalcMotCard(onClick = { onOpenSession(session.id) }) {
                    Column(Modifier.padding(CalcMotSpacing.CardPadding), verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Xs)) {
                        Text(session.sessionPeriodLabel(), style = CalcMotTypography.CardTitle, color = CalcMotColors.TextPrimary)
                        Text(
                            if (session.endedAtMillis == null) "Turno em andamento" else "Turno encerrado",
                            style = CalcMotTypography.BodyStrong,
                            color = if (session.endedAtMillis == null) CalcMotColors.Success else CalcMotColors.TextSecondary
                        )
                        ReadingConfidenceBadge(ReadingConfidence.ESTIMATED)
                        FilterChip(
                            selected = session.id in selectedIds,
                            onClick = {
                                selectedIds = if (session.id in selectedIds) {
                                    selectedIds - session.id
                                } else {
                                    selectedIds + session.id
                                }
                            },
                            label = { Text(if (session.id in selectedIds) "Selecionado" else "Selecionar para unir") }
                        )
                    }
                }
            }
            if (selectedIds.size >= 2) {
                item {
                    CalcMotButton(
                        text = "Unir turnos selecionados",
                        onClick = {
                            scope.launch {
                                val merged = repository.mergeSessions(selectedIds)
                                message = if (merged != null) {
                                    FinancialUiMessage.success("Turnos unidos.")
                                } else {
                                    FinancialUiMessage.error("Não foi possível unir os turnos.")
                                }
                                if (merged != null) {
                                    selectedIds = emptySet()
                                    trackSessionUpdate()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            message?.let { item { FinancialUiMessageBanner(it) } }
        }
    }
}

@Composable
internal fun FinancialSessionDetailRoute(sessionId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { FinanceLedgerProvider.repository(context) }
    val scope = rememberCoroutineScope()
    var session by remember { mutableStateOf<WorkSessionEntity?>(null) }
    var startText by rememberSaveable(sessionId) { mutableStateOf("") }
    var endText by rememberSaveable(sessionId) { mutableStateOf("") }
    var splitText by rememberSaveable(sessionId) { mutableStateOf("") }
    var message by remember { mutableStateOf<FinancialUiMessage?>(null) }

    androidx.compose.runtime.LaunchedEffect(sessionId) {
        session = repository.getSession(sessionId)
        session?.let {
            startText = it.startedAtMillis.asClock()
            endText = (it.endedAtMillis ?: it.lastActivityAtMillis).asClock()
        }
    }

    FinancialFeatureScreen(title = "Ajustar turno", onBack = onBack, imePadding = true) {
        item {
            CalcMotInfoBanner(
                title = "Horários do turno",
                body = "Use o formato HH:mm. O painel identificará que este horário foi corrigido por você."
            )
        }
        item { CalcMotTextField(startText, { startText = it.take(5) }, "Início (HH:mm)", Modifier.fillMaxWidth()) }
        item { CalcMotTextField(endText, { endText = it.take(5) }, "Fim (HH:mm)", Modifier.fillMaxWidth()) }
        item {
            CalcMotButton(
                text = "Salvar horários",
                onClick = {
                    val current = session
                    if (current == null) {
                        message = FinancialUiMessage.error("Turno não encontrado.")
                    } else {
                        val start = parseClockOnSessionDate(startText, current.startedAtMillis)
                        val parsedEnd = parseClockOnSessionDate(endText, current.startedAtMillis)
                        val end = if (start != null && parsedEnd != null && parsedEnd <= start) {
                            parsedEnd + 24L * 60L * 60L * 1000L
                        } else {
                            parsedEnd
                        }
                        scope.launch {
                            val saved = start != null && end != null &&
                                repository.adjustSession(sessionId, start, end)
                            message = if (saved) {
                                FinancialUiMessage.success("Turno atualizado.")
                            } else {
                                FinancialUiMessage.error("Revise os horários informados.")
                            }
                            if (saved) {
                                session = repository.getSession(sessionId)
                                trackSessionUpdate()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            CalcMotCard {
                Column(Modifier.padding(CalcMotSpacing.CardPadding), verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)) {
                    Text("Dividir turno", style = CalcMotTypography.CardTitle, color = CalcMotColors.TextPrimary)
                    Text("Os registros a partir deste horário irão para um novo turno.", style = CalcMotTypography.Body, color = CalcMotColors.TextSecondary)
                    CalcMotTextField(splitText, { splitText = it.take(5) }, "Dividir em (HH:mm)", Modifier.fillMaxWidth())
                    CalcMotButton(
                        text = "Dividir turno",
                        onClick = {
                            val current = session
                            val split = current?.let { parseClockOnSessionDate(splitText, it.startedAtMillis) }
                            scope.launch {
                                val newId = if (split != null) repository.splitSession(sessionId, split) else null
                                message = if (newId != null) {
                                    FinancialUiMessage.success("Turno dividido.")
                                } else {
                                    FinancialUiMessage.error("Escolha um horário dentro do turno.")
                                }
                                if (newId != null) {
                                    trackSessionUpdate()
                                    onBack()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        variant = CalcMotButtonVariant.SECONDARY
                    )
                }
            }
        }
        message?.let { item { FinancialUiMessageBanner(it) } }
    }
}

@Composable
internal fun FinancialReportsRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { FinanceLedgerProvider.repository(context) }
    val exporter = remember { LocalReportExporter() }
    val period = remember { LedgerPeriod.day() }
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<FinancialUiMessage?>(null) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        TelemetryProvider.analytics.track(
            AnalyticsEvents.FINANCIAL_REPORT_OPENED,
            mapOf(AnalyticsParams.RECORD_TYPE to "report", AnalyticsParams.SOURCE to "system")
        )
    }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            status = runCatching {
                withContext(Dispatchers.IO) {
                    val snapshot = repository.exportSnapshot(period)
                    context.contentResolver.openOutputStream(uri)?.use { exporter.exportDailyPdf(snapshot, it) }
                        ?: error("output unavailable")
                }
                TelemetryProvider.analytics.track(
                    AnalyticsEvents.FINANCIAL_REPORT_EXPORTED,
                    mapOf(AnalyticsParams.RECORD_TYPE to "report", AnalyticsParams.SOURCE to "system")
                )
                FinancialUiMessage.success("PDF diário exportado.")
            }.getOrElse { FinancialUiMessage.error("Não foi possível exportar o PDF.") }
        }
    }
    val xlsxLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            status = runCatching {
                withContext(Dispatchers.IO) {
                    val snapshot = repository.exportSnapshot(period)
                    context.contentResolver.openOutputStream(uri)?.use { exporter.exportDailyXlsx(snapshot, it) }
                        ?: error("output unavailable")
                }
                TelemetryProvider.analytics.track(
                    AnalyticsEvents.FINANCIAL_REPORT_EXPORTED,
                    mapOf(AnalyticsParams.RECORD_TYPE to "report", AnalyticsParams.SOURCE to "system")
                )
                FinancialUiMessage.success("Planilha diária exportada.")
            }.getOrElse { FinancialUiMessage.error("Não foi possível exportar a planilha.") }
        }
    }

    FinancialFeatureScreen(title = "Relatórios", onBack = onBack) {
        item {
            CalcMotCard(variant = CalcMotCardVariant.HIGHLIGHT) {
                Column(Modifier.padding(CalcMotSpacing.CardPadding), verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)) {
                    Icon(Icons.Outlined.ReceiptLong, contentDescription = null, tint = CalcMotColors.Success)
                    Text("Relatório diário", style = CalcMotTypography.CardTitle, color = CalcMotColors.TextPrimary)
                    Text("Leve o resumo do dia em PDF ou planilha. Detalhes de endereços e passageiros não entram no arquivo.", style = CalcMotTypography.Body, color = CalcMotColors.TextSecondary)
                    CalcMotButton("Exportar PDF", { pdfLauncher.launch("calcmot-${LocalDate.now()}.pdf") }, Modifier.fillMaxWidth())
                    CalcMotButton("Exportar XLSX", { xlsxLauncher.launch("calcmot-${LocalDate.now()}.xlsx") }, Modifier.fillMaxWidth(), variant = CalcMotButtonVariant.SECONDARY)
                }
            }
        }
        item {
            PremiumReportCard("Relatórios semanais, mensais e comparativos")
        }
        status?.let { item { FinancialUiMessageBanner(it) } }
    }
}

@Composable
private fun PremiumReportCard(title: String) {
    val unlocked = LockedPremiumEntitlementProvider.hasPremiumAccess()
    CalcMotCard {
        Row(
            Modifier.padding(CalcMotSpacing.CardPadding),
            horizontalArrangement = Arrangement.spacedBy(CalcMotSpacing.Md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = CalcMotColors.Warning)
            Column(Modifier.weight(1f)) {
                Text(title, style = CalcMotTypography.BodyStrong, color = CalcMotColors.TextPrimary)
                Text(if (unlocked) "Disponível" else "Premium · em breve", style = CalcMotTypography.Caption, color = CalcMotColors.TextMuted)
            }
        }
    }
}

@Composable
internal fun FinancialDataManagementRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { FinanceLedgerProvider.repository(context) }
    val scope = rememberCoroutineScope()
    var enabled by remember { mutableStateOf(AppSettings.isFinancialHistoryEnabled(context)) }
    var metricsResearchEnabled by remember { mutableStateOf(AppSettings.isMetricsResearchEnabled(context)) }
    var confirmMetricsResearch by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<FinancialUiMessage?>(null) }

    FinancialFeatureScreen(title = "Seus dados", onBack = onBack) {
        item {
            CalcMotCard {
                CalcMotSwitchRow(
                    title = "Criar histórico financeiro",
                    description = "Pausar não apaga o que já foi registrado.",
                    checked = enabled,
                    onCheckedChange = {
                        AppSettings.setFinancialHistoryEnabled(context, it)
                        enabled = AppSettings.isFinancialHistoryEnabled(context)
                    },
                    modifier = Modifier.padding(CalcMotSpacing.CardPadding)
                )
            }
        }
        item {
            CalcMotInfoBanner(
                title = "Privacidade local",
                body = "Seu histórico fica neste aparelho por até seis meses, sem imagens, endereços, passageiros ou localização exata.",
                variant = CalcMotBannerVariant.SUCCESS
            )
        }
        item {
            CalcMotCard {
                CalcMotSwitchRow(
                    title = "Ajudar a melhorar as métricas",
                    description = "Compara métodos no aparelho e envia só contagens agrupadas. Nunca envia imagens, textos da tela, endereços, passageiros ou valores exatos.",
                    checked = metricsResearchEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            confirmMetricsResearch = true
                        } else {
                            MetricsResearchProvider.updateConsent(context, false)
                            metricsResearchEnabled = false
                        }
                    },
                    modifier = Modifier.padding(CalcMotSpacing.CardPadding)
                )
            }
        }
        item {
            CalcMotButton(
                text = "Apagar todo o histórico",
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth(),
                variant = CalcMotButtonVariant.DANGER
            )
        }
        message?.let { item { FinancialUiMessageBanner(it) } }
    }

    if (confirmDelete) {
        CalcMotFeedbackAlertDialog(
            title = "Apagar todo o histórico?",
            message = "Ofertas, ganhos, custos, turnos e correções serão removidos deste aparelho. Suas metas e configurações serão mantidas.",
            tone = CalcMotFeedbackTone.ERROR,
            primaryAction = CalcMotFeedbackAction(
                label = "Apagar histórico",
                onClick = {
                    scope.launch {
                        repository.deleteAllFinancialHistory()
                        confirmDelete = false
                        message = FinancialUiMessage.success("Histórico financeiro apagado.")
                        TelemetryProvider.analytics.track(
                            AnalyticsEvents.FINANCIAL_HISTORY_DELETED,
                            mapOf(AnalyticsParams.RECORD_TYPE to "history", AnalyticsParams.SOURCE to "system")
                        )
                    }
                },
                style = CalcMotFeedbackActionStyle.DANGER
            ),
            onDismissRequest = { confirmDelete = false }
        )
    }

    if (confirmMetricsResearch) {
        CalcMotFeedbackAlertDialog(
            title = "Ajudar a melhorar as métricas?",
            message = "O CalcMot comparará formas de reconhecer cards repetidos no seu aparelho. Será enviado apenas um resumo com contagens e faixas, sem imagens, textos, endereços, passageiros ou valores exatos.",
            tone = CalcMotFeedbackTone.INFO,
            primaryAction = CalcMotFeedbackAction(
                label = "Aceitar e participar",
                onClick = {
                    MetricsResearchProvider.updateConsent(context, true)
                    metricsResearchEnabled = true
                    confirmMetricsResearch = false
                }
            ),
            secondaryAction = CalcMotFeedbackAction(
                label = "Agora não",
                onClick = { confirmMetricsResearch = false },
                style = CalcMotFeedbackActionStyle.TEXT
            ),
            onDismissRequest = { confirmMetricsResearch = false }
        )
    }
}

private data class FinancialUiMessage(
    val title: String,
    val body: String,
    val variant: CalcMotBannerVariant
) {
    companion object {
        fun success(body: String) = FinancialUiMessage(
            title = "Tudo certo",
            body = body,
            variant = CalcMotBannerVariant.SUCCESS
        )

        fun error(body: String) = FinancialUiMessage(
            title = "Revise as informações",
            body = body,
            variant = CalcMotBannerVariant.DANGER
        )
    }
}

@Composable
private fun FinancialUiMessageBanner(message: FinancialUiMessage) {
    CalcMotInfoBanner(
        title = message.title,
        body = message.body,
        variant = message.variant
    )
}

private fun String.toDecimalOrNull(): Double? = replace(',', '.').trim().toDoubleOrNull()
private fun String.toCentsOrNull(): Long? = toDecimalOrNull()?.times(100.0)?.toLong()
private fun String.toMetersOrNull(): Long? = toDecimalOrNull()?.times(1000.0)?.toLong()
private fun Double.formatOne(): String = "%.1f".format(Locale.US, this)
private fun Double.formatTwo(): String = "%.2f".format(Locale.US, this)
private fun Long.asMoney(): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(this / 100.0)
private fun Long.asClock(): String = SimpleDateFormat("HH:mm", Locale.forLanguageTag("pt-BR")).format(Date(this))
private fun parseClockOnSessionDate(value: String, anchorMillis: Long): Long? {
    val parts = value.trim().split(':')
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull()?.takeIf { it in 0..23 } ?: return null
    val minute = parts[1].toIntOrNull()?.takeIf { it in 0..59 } ?: return null
    val zone = java.time.ZoneId.systemDefault()
    val date = java.time.Instant.ofEpochMilli(anchorMillis).atZone(zone).toLocalDate()
    return date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
}
private fun OfferRecordEntity.totalDistanceKm(): Double = (pickupDistanceMeters + tripDistanceMeters) / 1000.0
private fun OfferRecordEntity.totalMinutes(): Int = (pickupTimeSeconds + tripTimeSeconds) / 60
private fun WorkSessionEntity.sessionPeriodLabel(): String {
    val end = endedAtMillis ?: lastActivityAtMillis
    return "${startedAtMillis.asClock()} - ${end.asClock()}"
}
private fun String.asClassification(): String = when (this) {
    "GREAT" -> "Ótima"
    "GOOD" -> "Boa"
    "WARNING" -> "Média"
    "BAD" -> "Ruim"
    else -> "Pendente"
}

private fun ExpenseCategory.label(): String = when (this) {
    ExpenseCategory.FUEL -> "Combustível"
    ExpenseCategory.MAINTENANCE -> "Manutenção"
    ExpenseCategory.FOOD -> "Alimentação"
    ExpenseCategory.TOLL -> "Pedágio"
    ExpenseCategory.RENTAL -> "Aluguel"
    ExpenseCategory.OTHER -> "Outro"
}

private fun trackSessionUpdate() {
    TelemetryProvider.analytics.track(
        AnalyticsEvents.FINANCIAL_SESSION_UPDATED,
        mapOf(AnalyticsParams.RECORD_TYPE to "session", AnalyticsParams.SOURCE to "system")
    )
}
