package br.com.calcmot.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.com.calcmot.AppSettings
import br.com.calcmot.finance.FinanceEntry
import br.com.calcmot.finance.FinanceEntryType
import br.com.calcmot.finance.FinanceFormatter
import br.com.calcmot.finance.FinanceRepository
import br.com.calcmot.finance.toFinanceSummary
import br.com.calcmot.model.ProfitabilitySettings
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
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Finanças",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Anote ganhos e custos do seu dia de trabalho.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
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

        if (entries.isEmpty()) {
            item {
                Text(
                    modifier = Modifier.padding(vertical = 12.dp),
                    text = "Nenhum lançamento ainda.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary
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
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Conta da corrida",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Preencha uma vez para o aviso mostrar lucro líquido, já descontando o custo do carro.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "Custo atual: ${formatMoneyPerKm(settings.operatingCostPerKm)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(UiTestTags.PROFIT_EFFICIENCY_INPUT),
                value = efficiencyText,
                onValueChange = onEfficiencyChange,
                label = { Text("Rendimento do carro") },
                placeholder = { Text("Ex: 10 km/l") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(UiTestTags.PROFIT_INPUT_PRICE_INPUT),
                value = inputPriceText,
                onValueChange = onInputPriceChange,
                label = { Text("Preço do combustível") },
                placeholder = { Text("Ex: 5,89") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(UiTestTags.PROFIT_MAINTENANCE_INPUT),
                value = maintenanceText,
                onValueChange = onMaintenanceChange,
                label = { Text("Manutenção por km") },
                placeholder = { Text("Ex: 0,35") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .weight(1f)
                        .testTag(UiTestTags.PROFIT_GOOD_KM_INPUT),
                    value = goodKmText,
                    onValueChange = onGoodKmChange,
                    label = { Text("Boa R$/km") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    modifier = Modifier
                        .weight(1f)
                        .testTag(UiTestTags.PROFIT_MEDIUM_KM_INPUT),
                    value = mediumKmText,
                    onValueChange = onMediumKmChange,
                    label = { Text("Média R$/km") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(UiTestTags.PROFIT_HOUR_INPUT),
                value = hourText,
                onValueChange = onHourChange,
                label = { Text("Meta R$/h líquido") },
                placeholder = { Text("Opcional") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
            errorText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            savedText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(UiTestTags.PROFIT_SAVE_BUTTON),
                onClick = onSave
            ) {
                Text("Salvar conta")
            }
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
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Resultado líquido",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = FinanceFormatter.formatSignedMoney(net),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Ganhos: ${FinanceFormatter.formatMoney(earnings)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Custos: ${FinanceFormatter.formatMoney(costs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Text(
                text = if (count == 1) "1 lançamento" else "$count lançamentos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
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

private fun formatMoneyPerKm(value: Double): String {
    return String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f/km", value)
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
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FinanceEntryType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { onTypeChange(type) },
                        label = { Text(type.label) }
                    )
                }
            }
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(UiTestTags.FINANCE_AMOUNT_INPUT),
                value = amountText,
                onValueChange = onAmountChange,
                label = { Text("Valor") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = errorText != null,
                supportingText = { errorText?.let { Text(it) } }
            )
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(UiTestTags.FINANCE_DESCRIPTION_INPUT),
                value = descriptionText,
                onValueChange = onDescriptionChange,
                label = { Text("Descrição") },
                singleLine = true
            )
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(UiTestTags.FINANCE_ADD_BUTTON),
                onClick = onAdd
            ) {
                Text("Adicionar")
            }
        }
    }
}

@Composable
private fun FinanceEntryRow(
    entry: FinanceEntry,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column {
                Text(
                    text = "${entry.type.label} - ${FinanceFormatter.formatMoney(entry.amountCents)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = entry.description.ifBlank { FinanceFormatter.formatDate(entry.dateMillis) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            TextButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(UiTestTags.FINANCE_DELETE_BUTTON),
                onClick = onDelete
            ) {
                Text("Excluir")
            }
        }
    }
}
