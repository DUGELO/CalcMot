package br.com.calcmot.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import br.com.calcmot.AppSettings
import br.com.calcmot.finance.FinanceEntry
import br.com.calcmot.finance.FinanceEntryType
import br.com.calcmot.finance.FinanceFormatter
import br.com.calcmot.finance.FinanceRepository
import br.com.calcmot.finance.toFinanceSummary
import br.com.calcmot.model.DriverGoal
import br.com.calcmot.model.GoalMode
import br.com.calcmot.model.ProfitabilitySettings
import br.com.calcmot.ui.design.components.CalcMotButton
import br.com.calcmot.ui.design.components.CalcMotButtonVariant
import br.com.calcmot.ui.design.components.CalcMotCard
import br.com.calcmot.ui.design.components.CalcMotEmptyState
import br.com.calcmot.ui.design.components.CalcMotNumberField
import br.com.calcmot.ui.design.components.CalcMotSectionHeader
import br.com.calcmot.ui.design.components.CalcMotSwitchRow
import br.com.calcmot.ui.design.components.CalcMotTextField
import br.com.calcmot.ui.design.domain.FinancialImpactSummaryCard
import br.com.calcmot.ui.design.domain.GoalPresetCard
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotSpacing
import br.com.calcmot.ui.design.tokens.CalcMotTypography
import java.util.Locale

@Composable
fun FinanceScreen() {
    val context = LocalContext.current
    val repository = remember { FinanceRepository(context) }
    var entries by remember { mutableStateOf(repository.getEntries()) }
    var selectedType by remember { mutableStateOf(FinanceEntryType.EARNING) }
    var amountText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var profitabilitySettings by remember {
        mutableStateOf(AppSettings.getProfitabilitySettings(context))
    }
    var financialImpactEnabled by remember {
        mutableStateOf(AppSettings.isFinancialImpactEnabled(context))
    }
    var driverGoal by remember {
        mutableStateOf(AppSettings.getDriverGoal(context))
    }
    var goalKmText by remember {
        mutableStateOf(driverGoal.minValuePerKm.toInputText())
    }
    var goalHourText by remember {
        mutableStateOf(driverGoal.minValuePerHour.toInputText())
    }
    var goalMode by remember { mutableStateOf(driverGoal.mode) }
    var goalErrorText by remember { mutableStateOf<String?>(null) }
    var goalSavedText by remember { mutableStateOf<String?>(null) }
    var efficiencyText by remember {
        mutableStateOf(profitabilitySettings.vehicleEfficiencyKmPerUnit.toInputText(blankWhenZero = true))
    }
    var inputPriceText by remember {
        mutableStateOf(profitabilitySettings.inputPricePerUnit.toInputText(blankWhenZero = true))
    }
    var maintenanceText by remember {
        mutableStateOf(profitabilitySettings.maintenanceCostPerKm.toInputText(blankWhenZero = true))
    }
    var goodKmText by remember {
        mutableStateOf(profitabilitySettings.goodNetPerKm.toInputText())
    }
    var mediumKmText by remember {
        mutableStateOf(profitabilitySettings.mediumNetPerKm.toInputText())
    }
    var hourText by remember {
        mutableStateOf(profitabilitySettings.minimumNetPerHour.toInputText(blankWhenZero = true))
    }
    var profitabilityErrorText by remember { mutableStateOf<String?>(null) }
    var profitabilitySavedText by remember { mutableStateOf<String?>(null) }
    val summary = entries.toFinanceSummary()

    LazyColumn(
        modifier = Modifier
            .testTag(UiTestTags.FINANCE_SCREEN)
            .padding(horizontal = CalcMotSpacing.ScreenHorizontal, vertical = CalcMotSpacing.ScreenVertical),
        verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Md)
    ) {
        item {
            CalcMotSectionHeader(
                title = "Finanças",
                subtitle = "Metas, custos e lançamentos do seu dia de trabalho."
            )
        }

        item {
            FinanceSummaryCard(
                earnings = summary.earningsCents,
                costs = summary.costsCents,
                net = summary.netCents,
                count = summary.entryCount
            )
        }

        item {
            FinanceFormCard(
                selectedType = selectedType,
                amountText = amountText,
                descriptionText = descriptionText,
                errorText = errorText,
                onTypeChange = { selectedType = it },
                onAmountChange = {
                    amountText = it
                    errorText = null
                },
                onDescriptionChange = { descriptionText = it },
                onAdd = {
                    val amountCents = FinanceFormatter.parseMoneyToCents(amountText)
                    if (amountCents == null) {
                        errorText = "Digite um valor válido."
                    } else {
                        entries = repository.addEntry(
                            type = selectedType,
                            amountCents = amountCents,
                            description = descriptionText
                        )
                        amountText = ""
                        descriptionText = ""
                        errorText = null
                    }
                }
            )
        }

        item {
            DriverGoalSettingsCard(
                enabled = financialImpactEnabled,
                goalKmText = goalKmText,
                goalHourText = goalHourText,
                goalMode = goalMode,
                errorText = goalErrorText,
                savedText = goalSavedText,
                onEnabledChange = {
                    financialImpactEnabled = it
                    AppSettings.setFinancialImpactEnabled(context, it)
                    goalSavedText = if (it) {
                        "Impacto financeiro ligado para o aviso."
                    } else {
                        "Impacto financeiro desligado."
                    }
                    goalErrorText = null
                },
                onGoalKmChange = {
                    goalKmText = it
                    goalErrorText = null
                    goalSavedText = null
                },
                onGoalHourChange = {
                    goalHourText = it
                    goalErrorText = null
                    goalSavedText = null
                },
                onGoalModeChange = {
                    goalMode = it
                    goalErrorText = null
                    goalSavedText = null
                },
                onPreset = { preset ->
                    goalKmText = preset.km.toInputText()
                    goalHourText = preset.hour.toInputText()
                    goalMode = preset.mode
                    goalErrorText = null
                    goalSavedText = null
                },
                onSave = {
                    val parsed = parseDriverGoal(
                        goalKmText = goalKmText,
                        goalHourText = goalHourText,
                        goalMode = goalMode
                    )
                    if (parsed == null) {
                        goalErrorText = "Use metas maiores que zero."
                        goalSavedText = null
                    } else {
                        AppSettings.setDriverGoal(context, parsed)
                        driverGoal = parsed
                        goalKmText = parsed.minValuePerKm.toInputText()
                        goalHourText = parsed.minValuePerHour.toInputText()
                        goalErrorText = null
                        goalSavedText = "Metas salvas para o aviso."
                    }
                }
            )
        }

        item {
            ProfitabilitySettingsCard(
                settings = profitabilitySettings,
                efficiencyText = efficiencyText,
                inputPriceText = inputPriceText,
                maintenanceText = maintenanceText,
                goodKmText = goodKmText,
                mediumKmText = mediumKmText,
                hourText = hourText,
                errorText = profitabilityErrorText,
                savedText = profitabilitySavedText,
                onEfficiencyChange = {
                    efficiencyText = it
                    profitabilityErrorText = null
                    profitabilitySavedText = null
                },
                onInputPriceChange = {
                    inputPriceText = it
                    profitabilityErrorText = null
                    profitabilitySavedText = null
                },
                onMaintenanceChange = {
                    maintenanceText = it
                    profitabilityErrorText = null
                    profitabilitySavedText = null
                },
                onGoodKmChange = {
                    goodKmText = it
                    profitabilityErrorText = null
                    profitabilitySavedText = null
                },
                onMediumKmChange = {
                    mediumKmText = it
                    profitabilityErrorText = null
                    profitabilitySavedText = null
                },
                onHourChange = {
                    hourText = it
                    profitabilityErrorText = null
                    profitabilitySavedText = null
                },
                onSave = {
                    val parsed = parseProfitabilitySettings(
                        efficiencyText = efficiencyText,
                        inputPriceText = inputPriceText,
                        maintenanceText = maintenanceText,
                        goodKmText = goodKmText,
                        mediumKmText = mediumKmText,
                        hourText = hourText
                    )
                    if (parsed == null) {
                        profitabilityErrorText = "Revise os números. Use valores maiores ou iguais a zero."
                        profitabilitySavedText = null
                    } else {
                        AppSettings.setProfitabilitySettings(context, parsed)
                        profitabilitySettings = parsed
                        profitabilityErrorText = null
                        profitabilitySavedText = "Custos salvos. O aviso agora usa lucro líquido."
                    }
                }
            )
        }

        if (entries.isEmpty()) {
            item {
                CalcMotEmptyState(
                    title = "Nenhum lançamento ainda.",
                    body = "Anote ganhos e custos para acompanhar seu resultado líquido."
                )
            }
        } else {
            items(entries, key = { it.id }) { entry ->
                FinanceEntryRow(
                    entry = entry,
                    onDelete = { entries = repository.deleteEntry(entry.id) }
                )
            }
        }
    }
}

@Composable
private fun DriverGoalSettingsCard(
    enabled: Boolean,
    goalKmText: String,
    goalHourText: String,
    goalMode: GoalMode,
    errorText: String?,
    savedText: String?,
    onEnabledChange: (Boolean) -> Unit,
    onGoalKmChange: (String) -> Unit,
    onGoalHourChange: (String) -> Unit,
    onGoalModeChange: (GoalMode) -> Unit,
    onPreset: (GoalPreset) -> Unit,
    onSave: () -> Unit
) {
    CalcMotCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CalcMotSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Md)
        ) {
            CalcMotSwitchRow(
                title = "Mostrar impacto na meta",
                description = "Mostra quanto a oferta está acima ou abaixo da sua meta.",
                checked = enabled,
                onCheckedChange = onEnabledChange,
                switchModifier = Modifier.testTag(UiTestTags.FINANCIAL_IMPACT_SWITCH)
            )

            Text(text = "Escolha uma meta rápida", style = CalcMotTypography.CardTitle, color = CalcMotColors.TextPrimary)
            GoalPreset.entries.forEach { preset ->
                GoalPresetCard(
                    title = preset.label,
                    perKm = preset.km.toCurrency(),
                    perHour = preset.hour.toCurrency(),
                    selected = goalKmText == preset.km.toInputText() && goalHourText == preset.hour.toInputText(),
                    onClick = { onPreset(preset) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)
            ) {
                CalcMotNumberField(
                    modifier = Modifier
                        .weight(1f)
                        .testTag(UiTestTags.DRIVER_GOAL_KM_INPUT),
                    value = goalKmText,
                    onValueChange = onGoalKmChange,
                    label = "Quero ganhar pelo menos por km"
                )
                CalcMotNumberField(
                    modifier = Modifier
                        .weight(1f)
                        .testTag(UiTestTags.DRIVER_GOAL_HOUR_INPUT),
                    value = goalHourText,
                    onValueChange = onGoalHourChange,
                    label = "Quero ganhar pelo menos por hora"
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)) {
                GoalMode.entries.forEach { mode ->
                    FilterChip(
                        selected = goalMode == mode,
                        onClick = { onGoalModeChange(mode) },
                        label = { Text(mode.label()) }
                    )
                }
            }

            errorText?.let {
                Text(text = it, style = CalcMotTypography.Caption, color = CalcMotColors.Danger)
            }
            savedText?.let {
                Text(text = it, style = CalcMotTypography.Caption, color = CalcMotColors.BrandAccent)
            }

            CalcMotButton(
                text = "Salvar minha meta",
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(UiTestTags.DRIVER_GOAL_SAVE_BUTTON),
                onClick = onSave
            )
        }
    }
}

@Composable
private fun ProfitabilitySettingsCard(
    settings: ProfitabilitySettings,
    efficiencyText: String,
    inputPriceText: String,
    maintenanceText: String,
    goodKmText: String,
    mediumKmText: String,
    hourText: String,
    errorText: String?,
    savedText: String?,
    onEfficiencyChange: (String) -> Unit,
    onInputPriceChange: (String) -> Unit,
    onMaintenanceChange: (String) -> Unit,
    onGoodKmChange: (String) -> Unit,
    onMediumKmChange: (String) -> Unit,
    onHourChange: (String) -> Unit,
    onSave: () -> Unit
) {
    CalcMotCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CalcMotSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Md)
        ) {
            Text(text = "Custos do carro", style = CalcMotTypography.CardTitle, color = CalcMotColors.TextPrimary)
            Text(
                text = "Preencha uma vez para o aviso mostrar lucro líquido, já descontando o custo do carro.",
                style = CalcMotTypography.Body,
                color = CalcMotColors.TextSecondary
            )
            FinancialImpactSummaryCard(
                title = "Custo atual",
                body = "${formatMoneyPerKm(settings.operatingCostPerKm)} descontado de cada oferta.",
                positive = true
            )
            CalcMotNumberField(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(UiTestTags.PROFIT_EFFICIENCY_INPUT),
                value = efficiencyText,
                onValueChange = onEfficiencyChange,
                label = "Rendimento do carro",
                placeholder = "Ex: 10 km/l"
            )
            CalcMotNumberField(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(UiTestTags.PROFIT_INPUT_PRICE_INPUT),
                value = inputPriceText,
                onValueChange = onInputPriceChange,
                label = "Preço do combustível",
                placeholder = "Ex: 5,89"
            )
            CalcMotNumberField(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(UiTestTags.PROFIT_MAINTENANCE_INPUT),
                value = maintenanceText,
                onValueChange = onMaintenanceChange,
                label = "Manutenção por km",
                placeholder = "Ex: 0,35"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)
            ) {
                CalcMotNumberField(
                    modifier = Modifier
                        .weight(1f)
                        .testTag(UiTestTags.PROFIT_GOOD_KM_INPUT),
                    value = goodKmText,
                    onValueChange = onGoodKmChange,
                    label = "Boa R$/km"
                )
                CalcMotNumberField(
                    modifier = Modifier
                        .weight(1f)
                        .testTag(UiTestTags.PROFIT_MEDIUM_KM_INPUT),
                    value = mediumKmText,
                    onValueChange = onMediumKmChange,
                    label = "Atenção R$/km"
                )
            }
            CalcMotNumberField(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(UiTestTags.PROFIT_HOUR_INPUT),
                value = hourText,
                onValueChange = onHourChange,
                label = "Quero ganhar por hora líquido",
                placeholder = "Opcional"
            )
            errorText?.let {
                Text(text = it, style = CalcMotTypography.Caption, color = CalcMotColors.Danger)
            }
            savedText?.let {
                Text(text = it, style = CalcMotTypography.Caption, color = CalcMotColors.BrandAccent)
            }
            CalcMotButton(
                text = "Salvar conta",
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(UiTestTags.PROFIT_SAVE_BUTTON),
                onClick = onSave
            )
        }
    }
}

@Composable
private fun FinanceSummaryCard(
    earnings: Long,
    costs: Long,
    net: Long,
    count: Int
) {
    val positive = net >= 0
    FinancialImpactSummaryCard(
        title = "Resultado de hoje",
        body = "${FinanceFormatter.formatSignedMoney(net)} em ${countLabel(count)}. Ganhos: ${FinanceFormatter.formatMoney(earnings)}. Custos: ${FinanceFormatter.formatMoney(costs)}.",
        positive = positive
    )
}

@Composable
private fun FinanceFormCard(
    selectedType: FinanceEntryType,
    amountText: String,
    descriptionText: String,
    errorText: String?,
    onTypeChange: (FinanceEntryType) -> Unit,
    onAmountChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAdd: () -> Unit
) {
    CalcMotCard {
        Column(
            modifier = Modifier.padding(CalcMotSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Md)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)) {
                FinanceEntryType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { onTypeChange(type) },
                        label = { Text(type.label) }
                    )
                }
            }
            CalcMotNumberField(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(UiTestTags.FINANCE_AMOUNT_INPUT),
                value = amountText,
                onValueChange = onAmountChange,
                label = "Valor",
                isError = errorText != null,
                supportingText = errorText
            )
            CalcMotTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(UiTestTags.FINANCE_DESCRIPTION_INPUT),
                value = descriptionText,
                onValueChange = onDescriptionChange,
                label = "Descrição"
            )
            CalcMotButton(
                text = "Adicionar",
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(UiTestTags.FINANCE_ADD_BUTTON),
                onClick = onAdd
            )
        }
    }
}

@Composable
private fun FinanceEntryRow(
    entry: FinanceEntry,
    onDelete: () -> Unit
) {
    CalcMotCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CalcMotSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)
        ) {
            Column {
                Text(
                    text = "${entry.type.label} - ${FinanceFormatter.formatMoney(entry.amountCents)}",
                    style = CalcMotTypography.CardTitle,
                    color = CalcMotColors.TextPrimary
                )
                Text(
                    text = entry.description.ifBlank { FinanceFormatter.formatDate(entry.dateMillis) },
                    style = CalcMotTypography.Body,
                    color = CalcMotColors.TextSecondary
                )
            }
            CalcMotButton(
                text = "Excluir",
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(UiTestTags.FINANCE_DELETE_BUTTON),
                onClick = onDelete,
                variant = CalcMotButtonVariant.GHOST
            )
        }
    }
}

private fun parseProfitabilitySettings(
    efficiencyText: String,
    inputPriceText: String,
    maintenanceText: String,
    goodKmText: String,
    mediumKmText: String,
    hourText: String
): ProfitabilitySettings? {
    val settings = ProfitabilitySettings(
        vehicleEfficiencyKmPerUnit = parseDecimal(efficiencyText) ?: return null,
        inputPricePerUnit = parseDecimal(inputPriceText) ?: return null,
        maintenanceCostPerKm = parseDecimal(maintenanceText) ?: return null,
        goodNetPerKm = parseDecimal(goodKmText) ?: return null,
        mediumNetPerKm = parseDecimal(mediumKmText) ?: return null,
        minimumNetPerHour = parseDecimal(hourText) ?: return null
    ).normalized()

    if (settings.goodNetPerKm < settings.mediumNetPerKm) return null
    return settings
}

private fun parseDriverGoal(
    goalKmText: String,
    goalHourText: String,
    goalMode: GoalMode
): DriverGoal? {
    val minKm = parseDecimal(goalKmText)?.takeIf { it > 0.0 } ?: return null
    val minHour = parseDecimal(goalHourText)?.takeIf { it > 0.0 } ?: return null
    return DriverGoal(
        minValuePerKm = minKm,
        minValuePerHour = minHour,
        mode = goalMode
    ).normalized()
}

private fun parseDecimal(rawValue: String): Double? {
    val cleaned = rawValue
        .trim()
        .replace("R$", "")
        .replace(" ", "")
        .replace(Regex("[^0-9,.-]"), "")
    if (cleaned.isBlank()) return 0.0

    val normalized = when {
        cleaned.contains(",") -> cleaned.replace(".", "").replace(",", ".")
        cleaned.count { it == '.' } == 1 && cleaned.substringAfter('.').length in 1..3 -> cleaned
        else -> cleaned.replace(".", "")
    }

    return normalized.toDoubleOrNull()?.takeIf { it >= 0.0 && it.isFinite() }
}

private fun Double.toInputText(blankWhenZero: Boolean = false): String {
    if (blankWhenZero && this == 0.0) return ""
    return String.format(Locale.forLanguageTag("pt-BR"), "%.2f", this)
}

private fun Double.toCurrency(): String {
    return String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", this)
}

private fun formatMoneyPerKm(value: Double): String {
    return String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f/km", value)
}

private fun countLabel(count: Int): String {
    return if (count == 1) "1 lançamento" else "$count lançamentos"
}

private fun GoalMode.label(): String {
    return when (this) {
        GoalMode.BALANCED -> "Equilibrado"
        GoalMode.PRIORITIZE_KM -> "Prioriza km"
        GoalMode.PRIORITIZE_HOUR -> "Prioriza hora"
    }
}

private enum class GoalPreset(
    val label: String,
    val km: Double,
    val hour: Double,
    val mode: GoalMode
) {
    CONSERVADOR("Conservador", 1.50, 45.0, GoalMode.BALANCED),
    EQUILIBRADO("Equilibrado", 1.70, 50.0, GoalMode.BALANCED),
    EXIGENTE("Exigente", 2.10, 65.0, GoalMode.BALANCED)
}
