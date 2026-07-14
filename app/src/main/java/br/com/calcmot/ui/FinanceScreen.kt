package br.com.calcmot.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.Route
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import br.com.calcmot.ui.design.components.CalcMotTextField
import br.com.calcmot.ui.design.domain.FinancialImpactSummaryCard
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotSpacing
import br.com.calcmot.ui.design.tokens.CalcMotTypography
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onGoalSaved: (DriverGoal) -> Unit = {}
) {
    val context = LocalContext.current
    var profitabilitySettings by remember { mutableStateOf(AppSettings.getProfitabilitySettings(context)) }
    var financialImpactEnabled by remember { mutableStateOf(AppSettings.isFinancialImpactEnabled(context)) }
    var driverGoal by remember { mutableStateOf(AppSettings.getDriverGoal(context)) }
    var goalKmText by rememberSaveable { mutableStateOf(driverGoal.minValuePerKm.toInputText()) }
    var goalHourText by rememberSaveable { mutableStateOf(driverGoal.minValuePerHour.toInputText()) }
    var goalMode by rememberSaveable { mutableStateOf(driverGoal.mode) }
    var goalErrorText by rememberSaveable { mutableStateOf<String?>(null) }
    var goalSavedText by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    DriverGoalPrototypeScreen(
        modifier = modifier,
        goalKmText = goalKmText,
        goalHourText = goalHourText,
        selectedPreset = GoalPreset.fromInputs(goalKmText, goalHourText),
        errorText = goalErrorText,
        savedText = null,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
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
                goalErrorText = "Digite metas maiores que zero."
                goalSavedText = null
            } else {
                AppSettings.setDriverGoal(context, parsed)
                driverGoal = parsed
                onGoalSaved(parsed)
                goalKmText = parsed.minValuePerKm.toInputText()
                goalHourText = parsed.minValuePerHour.toInputText()
                goalErrorText = null
                goalSavedText = null
                scope.launch { snackbarHostState.showSnackbar("Meta salva.") }
            }
        }
    )
    return
    var advancedCostsExpanded by remember { mutableStateOf(false) }
    var efficiencyText by remember {
        mutableStateOf(profitabilitySettings.vehicleEfficiencyKmPerUnit.toInputText(blankWhenZero = true))
    }
    var inputPriceText by remember {
        mutableStateOf(profitabilitySettings.inputPricePerUnit.toInputText(blankWhenZero = true))
    }
    var maintenanceText by remember {
        mutableStateOf(profitabilitySettings.maintenanceCostPerKm.toInputText(blankWhenZero = true))
    }
    var goodKmText by remember { mutableStateOf(profitabilitySettings.goodNetPerKm.toInputText()) }
    var mediumKmText by remember { mutableStateOf(profitabilitySettings.mediumNetPerKm.toInputText()) }
    var hourText by remember {
        mutableStateOf(profitabilitySettings.minimumNetPerHour.toInputText(blankWhenZero = true))
    }
    var profitabilityErrorText by remember { mutableStateOf<String?>(null) }
    var profitabilitySavedText by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .testTag(UiTestTags.FINANCE_SCREEN)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Minha meta",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Usamos sua meta para dizer se a corrida está boa, no limite ou ruim.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            DriverGoalSettings(
                selectedPreset = GoalPreset.fromInputs(goalKmText, goalHourText),
                goalKmText = goalKmText,
                goalHourText = goalHourText,
                errorText = goalErrorText,
                savedText = goalSavedText,
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
                        goalSavedText = "Meta salva."
                    }
                }
            )
        }

        item {
            GoalMeaningCompact()
        }

        item {
            ListItem(
                headlineContent = { Text("Mostrar impacto no aviso") },
                supportingContent = { Text("Mostra se a oferta passou ou ficou abaixo da meta.") },
                trailingContent = {
                    Switch(
                        modifier = Modifier.testTag(UiTestTags.FINANCIAL_IMPACT_SWITCH),
                        checked = financialImpactEnabled,
                        onCheckedChange = {
                            financialImpactEnabled = it
                            AppSettings.setFinancialImpactEnabled(context, it)
                            goalSavedText = if (it) "Impacto ligado." else "Impacto desligado."
                            goalErrorText = null
                        }
                    )
                }
            )
        }

        item {
            AdvancedCosts(
                settings = profitabilitySettings,
                expanded = advancedCostsExpanded,
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
                onToggleExpanded = {
                    advancedCostsExpanded = !advancedCostsExpanded
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
                        profitabilitySavedText = "Custos salvos."
                    }
                }
            )
        }
    }
}

@Composable
private fun DriverGoalPrototypeScreen(
    goalKmText: String,
    goalHourText: String,
    selectedPreset: GoalPreset?,
    errorText: String?,
    savedText: String?,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onGoalKmChange: (String) -> Unit,
    onGoalHourChange: (String) -> Unit,
    onPreset: (GoalPreset) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CalcMotColors.AppBackground)
            .testTag(UiTestTags.FINANCE_SCREEN)
    ) {
        GoalScreenBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GoalTopBar(onBack = onBack)
            GoalHeader()
            GoalProfileCard(
                selectedPreset = selectedPreset,
                goalKmText = goalKmText,
                goalHourText = goalHourText,
                errorText = errorText,
                savedText = savedText,
                onPreset = onPreset,
                onGoalKmChange = onGoalKmChange,
                onGoalHourChange = onGoalHourChange
            )
            GoalMeaningCard()
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 58.dp)
                    .testTag(UiTestTags.DRIVER_GOAL_SAVE_BUTTON),
                onClick = onSave,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CalcMotColors.PrimaryActionBlue,
                    contentColor = CalcMotColors.TextPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Save,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Salvar meta",
                    modifier = Modifier.padding(start = 14.dp),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Você pode alterar sua meta quando quiser.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                color = CalcMotColors.TextSecondary,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
        )
    }
}

@Composable
private fun GoalTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .testTag(UiTestTags.FINANCE_BACK_BUTTON)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Voltar",
                tint = CalcMotColors.TextPrimary,
                modifier = Modifier.size(31.dp)
            )
        }
        Text(
            text = "Minha meta",
            modifier = Modifier.padding(start = 18.dp),
            color = CalcMotColors.TextPrimary,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GoalHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Defina sua meta",
            color = CalcMotColors.TextPrimary,
            fontSize = 28.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = buildAnnotatedString {
                append("Escolha o mínimo que uma corrida\nprecisa pagar para ser considerada ")
                withStyle(SpanStyle(color = CalcMotColors.Success, fontWeight = FontWeight.Bold)) {
                    append("boa.")
                }
            },
            color = CalcMotColors.TextSecondary,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GoalProfileCard(
    selectedPreset: GoalPreset?,
    goalKmText: String,
    goalHourText: String,
    errorText: String?,
    savedText: String?,
    onPreset: (GoalPreset) -> Unit,
    onGoalKmChange: (String) -> Unit,
    onGoalHourChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, CalcMotColors.BorderStrong), RoundedCornerShape(22.dp))
            .background(CalcMotColors.Surface.copy(alpha = 0.74f), RoundedCornerShape(22.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(
                imageVector = Icons.Outlined.TrackChanges,
                contentDescription = null,
                tint = CalcMotColors.Success,
                modifier = Modifier.size(34.dp)
            )
            Text(
                text = "Perfil de meta",
                color = CalcMotColors.TextPrimary,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )
        }
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            GoalPreset.entries.forEachIndexed { index, preset ->
                SegmentedButton(
                    modifier = Modifier.testTag(preset.testTag),
                    selected = selectedPreset == preset,
                    onClick = { onPreset(preset) },
                    shape = SegmentedButtonDefaults.itemShape(index, GoalPreset.entries.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = CalcMotColors.PrimaryActionBlue,
                        activeContentColor = CalcMotColors.TextPrimary,
                        inactiveContainerColor = CalcMotColors.Surface.copy(alpha = 0.35f),
                        inactiveContentColor = CalcMotColors.TextSecondary
                    )
                ) {
                    Text(text = preset.displayLabel, maxLines = 1)
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, CalcMotColors.BorderSubtle), RoundedCornerShape(16.dp))
                .background(CalcMotColors.SurfaceElevated.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GoalInputRow(
                icon = Icons.Outlined.Route,
                label = "Meta por km",
                value = goalKmText,
                testTag = UiTestTags.DRIVER_GOAL_KM_INPUT,
                onValueChange = onGoalKmChange
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 1.dp)
                    .background(CalcMotColors.BorderSubtle)
            )
            GoalInputRow(
                icon = Icons.Outlined.Timelapse,
                label = "Meta por hora",
                value = goalHourText,
                testTag = UiTestTags.DRIVER_GOAL_HOUR_INPUT,
                onValueChange = onGoalHourChange
            )
        }
        errorText?.let {
            Text(text = it, color = CalcMotColors.Danger, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        savedText?.let {
            Text(text = it, color = CalcMotColors.Success, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun GoalInputRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    testTag: String,
    onValueChange: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CalcMotColors.Success,
            modifier = Modifier.size(38.dp)
        )
        CalcMotNumberField(
            modifier = Modifier
                .weight(1f)
                .testTag(testTag),
            value = value,
            onValueChange = onValueChange,
            label = label,
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )
    }
}

@Composable
private fun GoalMeaningCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(UiTestTags.GOAL_SEMAPHORE_PREVIEW)
            .border(BorderStroke(1.dp, CalcMotColors.BorderStrong), RoundedCornerShape(22.dp))
            .background(CalcMotColors.Surface.copy(alpha = 0.74f), RoundedCornerShape(22.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(
                imageVector = Icons.Outlined.Speed,
                contentDescription = null,
                tint = CalcMotColors.Success,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "Como o semáforo usa sua meta",
                color = CalcMotColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        GoalMeaningRow(
            color = CalcMotColors.PrimaryActionBlue,
            title = "Ótima:",
            body = "muito acima da meta"
        )
        GoalMeaningRow(color = CalcMotColors.Success, title = "Boa:", body = "dentro ou acima da meta")
        GoalMeaningRow(color = CalcMotColors.Warning, title = "Média:", body = "perto da meta")
        GoalMeaningRow(color = CalcMotColors.Danger, title = "Ruim:", body = "abaixo da meta")
    }
}

@Composable
private fun GoalMeaningRow(
    color: androidx.compose.ui.graphics.Color,
    title: String,
    body: String
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color, CircleShape)
        )
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold)) {
                    append(title)
                }
                append(" $body")
            },
            color = CalcMotColors.TextPrimary,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun GoalScreenBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(
                        CalcMotColors.Success.copy(alpha = 0.08f),
                        CalcMotColors.AppBackground.copy(alpha = 0.74f),
                        CalcMotColors.AppBackground
                    )
                )
            )
    )
}

@Composable
fun ResultScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember { FinanceRepository(context) }
    var entries by remember { mutableStateOf(repository.getEntries()) }
    var selectedType by remember { mutableStateOf(FinanceEntryType.EARNING) }
    var amountText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    val summary = entries.toFinanceSummary()

    LazyColumn(
        modifier = modifier
            .testTag(UiTestTags.RESULT_SCREEN)
            .padding(horizontal = CalcMotSpacing.ScreenHorizontal, vertical = CalcMotSpacing.ScreenVertical),
        verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Md)
    ) {
        item {
            CalcMotSectionHeader(
                title = "Resultado",
                subtitle = "Acompanhe ganhos e custos anotados no dia, sem misturar com suas metas."
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

        if (entries.isEmpty()) {
            item {
                CalcMotEmptyState(
                    title = "Nenhum ganho ou custo anotado hoje.",
                    body = "Quando quiser acompanhar o resultado do dia, adicione seus ganhos e custos aqui."
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DriverGoalSettings(
    selectedPreset: GoalPreset?,
    goalKmText: String,
    goalHourText: String,
    errorText: String?,
    savedText: String?,
    onGoalKmChange: (String) -> Unit,
    onGoalHourChange: (String) -> Unit,
    onPreset: (GoalPreset) -> Unit,
    onSave: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Escolha um perfil",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            GoalPreset.entries.forEachIndexed { index, preset ->
                SegmentedButton(
                    modifier = Modifier.testTag(preset.testTag),
                    selected = selectedPreset == preset,
                    onClick = { onPreset(preset) },
                    shape = SegmentedButtonDefaults.itemShape(index, GoalPreset.entries.size)
                ) {
                    Text(preset.label)
                }
            }
        }
        Text(
            text = "Ou ajuste manualmente",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CalcMotNumberField(
                modifier = Modifier
                    .weight(1f)
                    .testTag(UiTestTags.DRIVER_GOAL_KM_INPUT),
                value = goalKmText,
                onValueChange = onGoalKmChange,
                label = "R$/km"
            )
            CalcMotNumberField(
                modifier = Modifier
                    .weight(1f)
                    .testTag(UiTestTags.DRIVER_GOAL_HOUR_INPUT),
                value = goalHourText,
                onValueChange = onGoalHourChange,
                label = "R$/h"
            )
        }
        errorText?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        savedText?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(UiTestTags.DRIVER_GOAL_SAVE_BUTTON),
            onClick = onSave
        ) {
            Text("Salvar meta")
        }
    }
}

@Composable
private fun GoalMeaningCompact() {
    OutlinedCard(modifier = Modifier.testTag(UiTestTags.GOAL_SEMAPHORE_PREVIEW)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Semáforo", style = MaterialTheme.typography.titleMedium)
            Text("Boa: dentro da meta", style = MaterialTheme.typography.bodyMedium)
            Text("Média: no limite", style = MaterialTheme.typography.bodyMedium)
            Text("Ruim: abaixo da meta", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AdvancedCosts(
    settings: ProfitabilitySettings,
    expanded: Boolean,
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
    onToggleExpanded: () -> Unit,
    onSave: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ListItem(
            headlineContent = { Text("Avançado") },
            supportingContent = { Text("Custos do carro: ${formatMoneyPerKm(settings.operatingCostPerKm)}") },
            trailingContent = {
                OutlinedButton(
                    modifier = Modifier.testTag(UiTestTags.PROFIT_ADVANCED_TOGGLE),
                    onClick = onToggleExpanded
                ) {
                    Text(if (expanded) "Ocultar" else "Abrir")
                }
            }
        )
        if (expanded) {
            OutlinedCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                            label = "Média R$/km"
                        )
                    }
                    CalcMotNumberField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(UiTestTags.PROFIT_HOUR_INPUT),
                        value = hourText,
                        onValueChange = onHourChange,
                        label = "Meta líquida por hora",
                        placeholder = "Opcional"
                    )
                    errorText?.let {
                        Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    savedText?.let {
                        Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(UiTestTags.PROFIT_SAVE_BUTTON),
                        onClick = onSave
                    ) {
                        Text("Salvar custos")
                    }
                }
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

private fun formatMoneyPerKm(value: Double): String {
    return String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f/km", value)
}

private fun countLabel(count: Int): String {
    return if (count == 1) "1 lançamento" else "$count lançamentos"
}

private enum class GoalPreset(
    val label: String,
    val displayLabel: String,
    val km: Double,
    val hour: Double,
    val mode: GoalMode,
    val testTag: String
) {
    CONSERVADOR("Começando", "Conservador", 1.35, 30.0, GoalMode.BALANCED, UiTestTags.GOAL_PRESET_BEGINNER),
    EQUILIBRADO("Equilibrado", "Equilibrado", 1.50, 35.0, GoalMode.BALANCED, UiTestTags.GOAL_PRESET_BALANCED),
    EXIGENTE("Exigente", "Agressivo", 1.70, 42.0, GoalMode.BALANCED, UiTestTags.GOAL_PRESET_DEMANDING);

    companion object {
        fun fromInputs(kmText: String, hourText: String): GoalPreset? {
            return entries.firstOrNull {
                kmText == it.km.toInputText() && hourText == it.hour.toInputText()
            }
        }
    }
}
