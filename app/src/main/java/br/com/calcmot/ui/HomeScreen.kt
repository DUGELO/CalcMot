package br.com.calcmot.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.calcmot.AppDiagnostics
import br.com.calcmot.AppPermissionState
import br.com.calcmot.AppSettings
import br.com.calcmot.BuildConfig
import br.com.calcmot.DriverApp
import br.com.calcmot.DriverAppLauncher
import br.com.calcmot.OverlayPositionPreference
import br.com.calcmot.ui.design.components.CalcMotButton
import br.com.calcmot.ui.design.components.CalcMotButtonVariant
import br.com.calcmot.ui.design.components.CalcMotCard
import br.com.calcmot.ui.design.tokens.CalcMotSpacing
import br.com.calcmot.ui.design.tokens.CalcMotTypography
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    permissionState: AppPermissionState,
    onPermissionsRefresh: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var destination by remember { mutableStateOf(HomeDestination.START) }
    var diagnosticsRefreshKey by remember { mutableStateOf(0) }
    var monitoringEnabled by remember { mutableStateOf(AppSettings.isMonitoringEnabled(context)) }
    var overlayPosition by remember { mutableStateOf(AppSettings.getOverlayPosition(context)) }

    fun navigate(next: HomeDestination) {
        destination = next
        if (next == HomeDestination.DIAGNOSTICS) diagnosticsRefreshKey++
        scope.launch { drawerState.close() }
    }

    fun setMonitoringEnabled(enabled: Boolean) {
        AppSettings.setMonitoringEnabled(context, enabled)
        monitoringEnabled = enabled
    }

    fun setOverlayPosition(position: OverlayPositionPreference) {
        AppSettings.setOverlayPosition(context, position)
        overlayPosition = position
    }

    val status = when {
        !permissionState.hasAccessibilityService -> HomeStatus.PERMISSION_PENDING
        monitoringEnabled -> HomeStatus.READY
        else -> HomeStatus.PAUSED
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                selected = destination,
                onSelect = ::navigate
            )
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text("CalcMot") },
                    navigationIcon = {
                        IconButton(
                            modifier = Modifier.testTag(UiTestTags.DRAWER_MENU_BUTTON),
                            onClick = { scope.launch { drawerState.open() } }
                        ) {
                            Text("☰", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                )
            }
        ) { innerPadding ->
            when (destination) {
                HomeDestination.START -> HomeContent(
                    modifier = Modifier.padding(innerPadding),
                    status = status,
                    permissionState = permissionState,
                    onMonitoringChange = ::setMonitoringEnabled,
                    onOpenAccessibility = {
                        openAccessibilitySettings(context)
                    },
                    onPermissionsRefresh = {
                        onPermissionsRefresh()
                        diagnosticsRefreshKey++
                    },
                    onOpenDriverApp = { driverApp ->
                        if (DriverAppLauncher.launch(context, driverApp) == null) {
                            Toast.makeText(
                                context,
                                "${driverApp.displayName} nao esta instalado.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onEditGoal = { navigate(HomeDestination.FINANCE) },
                    onOpenHelp = { navigate(HomeDestination.HELP) },
                    onOpenPrivacy = { navigate(HomeDestination.PRIVACY) }
                )

                HomeDestination.FINANCE -> FinanceScreen(modifier = Modifier.padding(innerPadding))
                HomeDestination.RESULT -> ResultScreen(modifier = Modifier.padding(innerPadding))
                HomeDestination.SETTINGS -> SettingsScreen(
                    modifier = Modifier.padding(innerPadding),
                    monitoringEnabled = monitoringEnabled,
                    permissionState = permissionState,
                    overlayPosition = overlayPosition,
                    onMonitoringChange = ::setMonitoringEnabled,
                    onOverlayPositionChange = ::setOverlayPosition,
                    onOpenAccessibility = {
                        openAccessibilitySettings(context)
                    },
                    onOpenGoal = { navigate(HomeDestination.FINANCE) },
                    onOpenPrivacy = { navigate(HomeDestination.PRIVACY) },
                    onOpenAdvanced = {
                        if (BuildConfig.DEBUG) navigate(HomeDestination.DIAGNOSTICS)
                    }
                )

                HomeDestination.HELP -> HelpScreen(
                    modifier = Modifier.padding(innerPadding),
                    onOpenPrivacy = { navigate(HomeDestination.PRIVACY) },
                    onSupport = { uriHandler.openUri("mailto:$CALCMOT_SUPPORT_EMAIL") }
                )

                HomeDestination.PRIVACY -> PrivacyPolicyScreen(
                    modifier = Modifier.padding(innerPadding),
                    onBack = { navigate(HomeDestination.HELP) },
                    onSupport = { uriHandler.openUri("mailto:$CALCMOT_SUPPORT_EMAIL") }
                )

                HomeDestination.DIAGNOSTICS -> DiagnosticsScreen(
                    modifier = Modifier.padding(innerPadding),
                    snapshot = remember(diagnosticsRefreshKey) { AppDiagnostics.read(context) },
                    onRefresh = { diagnosticsRefreshKey++ }
                )
            }
        }
    }
}

@Composable
private fun AppDrawer(
    selected: HomeDestination,
    onSelect: (HomeDestination) -> Unit
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "CalcMot",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            NavigationDrawerItem(
                modifier = Modifier.testTag(UiTestTags.DRAWER_HOME_ITEM),
                label = { Text("Início") },
                selected = selected == HomeDestination.START,
                onClick = { onSelect(HomeDestination.START) }
            )
            NavigationDrawerItem(
                modifier = Modifier.testTag(UiTestTags.DRAWER_FINANCE_ITEM),
                label = { Text("Metas") },
                selected = selected == HomeDestination.FINANCE,
                onClick = { onSelect(HomeDestination.FINANCE) }
            )
            NavigationDrawerItem(
                modifier = Modifier.testTag(UiTestTags.DRAWER_RESULT_ITEM),
                label = { Text("Resultado") },
                selected = selected == HomeDestination.RESULT,
                onClick = { onSelect(HomeDestination.RESULT) }
            )
            NavigationDrawerItem(
                modifier = Modifier.testTag(UiTestTags.DRAWER_SETTINGS_ITEM),
                label = { Text("Configurações") },
                selected = selected == HomeDestination.SETTINGS,
                onClick = { onSelect(HomeDestination.SETTINGS) }
            )
            NavigationDrawerItem(
                modifier = Modifier.testTag(UiTestTags.DRAWER_HELP_ITEM),
                label = { Text("Ajuda") },
                selected = selected == HomeDestination.HELP || selected == HomeDestination.PRIVACY,
                onClick = { onSelect(HomeDestination.HELP) }
            )
            if (BuildConfig.DEBUG) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Avançado",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp)
                )
                NavigationDrawerItem(
                    modifier = Modifier.testTag(UiTestTags.DRAWER_DIAGNOSTICS_ITEM),
                    label = { Text("Diagnóstico") },
                    selected = selected == HomeDestination.DIAGNOSTICS,
                    onClick = { onSelect(HomeDestination.DIAGNOSTICS) }
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    modifier: Modifier,
    status: HomeStatus,
    permissionState: AppPermissionState,
    onMonitoringChange: (Boolean) -> Unit,
    onOpenAccessibility: () -> Unit,
    onPermissionsRefresh: () -> Unit,
    onOpenDriverApp: (DriverApp) -> Unit,
    onEditGoal: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenPrivacy: () -> Unit
) {
    val context = LocalContext.current
    val driverGoal = remember { AppSettings.getDriverGoal(context) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(UiTestTags.HOME_SCREEN)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Calculador de ganhos",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Veja se a corrida compensa em poucos segundos.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        CalcMotHeroStatus(
            status = status,
            permissionState = permissionState,
            onMonitoringChange = onMonitoringChange,
            onOpenAccessibility = onOpenAccessibility,
            onPermissionsRefresh = onPermissionsRefresh,
            onOpenDriverApp = onOpenDriverApp
        )
        CalcMotMetricSummary(
            perKm = driverGoal.minValuePerKm,
            perHour = driverGoal.minValuePerHour,
            onEditGoal = onEditGoal
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                modifier = Modifier.testTag(UiTestTags.HOME_FOOTER_HELP),
                onClick = onOpenHelp
            ) {
                Text("Como funciona")
            }
            TextButton(
                modifier = Modifier.testTag(UiTestTags.HOME_FOOTER_PRIVACY),
                onClick = onOpenPrivacy
            ) {
                Text("Privacidade")
            }
        }
    }
}

@Composable
private fun CalcMotHeroStatus(
    status: HomeStatus,
    permissionState: AppPermissionState,
    onMonitoringChange: (Boolean) -> Unit,
    onOpenAccessibility: () -> Unit,
    onPermissionsRefresh: () -> Unit,
    onOpenDriverApp: (DriverApp) -> Unit
) {
    ElevatedCard(modifier = Modifier.testTag(UiTestTags.HOME_HERO_CARD)) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AssistChip(
                modifier = Modifier.testTag(UiTestTags.STATUS_PILL),
                onClick = {},
                label = { Text(status.label) },
                enabled = false
            )
            Text(
                text = status.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = status.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            when (status) {
                HomeStatus.PERMISSION_PENDING -> {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(UiTestTags.HOME_PRIMARY_ACTION),
                        onClick = onOpenAccessibility
                    ) {
                        Text("Ativar permissão")
                    }
                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(UiTestTags.HOME_SECONDARY_ACTION)
                            .testTag(UiTestTags.REFRESH_PERMISSIONS_BUTTON),
                        onClick = onPermissionsRefresh
                    ) {
                        Text("Já ativei")
                    }
                    Text(
                        text = "Fora dos apps de motorista, o CalcMot fica em espera.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HomeStatus.READY -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            modifier = Modifier
                                .weight(1f)
                                .testTag(UiTestTags.OPEN_UBER_DRIVER_BUTTON),
                            onClick = { onOpenDriverApp(DriverApp.UBER) }
                        ) {
                            Text("Abrir Uber")
                        }
                        OutlinedButton(
                            modifier = Modifier
                                .weight(1f)
                                .testTag(UiTestTags.OPEN_99_DRIVER_BUTTON),
                            onClick = { onOpenDriverApp(DriverApp.NINETY_NINE) }
                        ) {
                            Text("Abrir 99")
                        }
                    }
                }

                HomeStatus.PAUSED -> {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(UiTestTags.HOME_PRIMARY_ACTION),
                        onClick = { onMonitoringChange(true) }
                    ) {
                        Text("Ligar cálculo automático")
                    }
                }
            }
        }
    }
}

@Composable
private fun CalcMotMetricSummary(
    perKm: Double,
    perHour: Double,
    onEditGoal: () -> Unit
) {
    OutlinedCard(modifier = Modifier.testTag(UiTestTags.HOME_GOAL_CARD)) {
        ListItem(
            headlineContent = { Text("Meta") },
            supportingContent = { Text("${perKm.toGoalMoney()}/km · ${perHour.toGoalMoneyNoCentsIfRound()}/h") },
            trailingContent = {
                TextButton(onClick = onEditGoal) {
                    Text("Editar meta")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    modifier: Modifier,
    monitoringEnabled: Boolean,
    permissionState: AppPermissionState,
    overlayPosition: OverlayPositionPreference,
    onMonitoringChange: (Boolean) -> Unit,
    onOverlayPositionChange: (OverlayPositionPreference) -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenGoal: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAdvanced: () -> Unit
) {
    val context = LocalContext.current
    val goal = remember { AppSettings.getDriverGoal(context) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(UiTestTags.SETTINGS_SCREEN)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Configurações",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
        )
        ListItem(
            headlineContent = { Text("Cálculo automático") },
            supportingContent = {
                Text(if (monitoringEnabled) "Ligado para apps de motorista" else "Pausado por você")
            },
            trailingContent = {
                Switch(
                    modifier = Modifier.testTag(UiTestTags.MONITORING_SWITCH),
                    checked = monitoringEnabled,
                    enabled = permissionState.hasAccessibilityService,
                    onCheckedChange = onMonitoringChange
                )
            }
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text("Acessibilidade do Android") },
            supportingContent = {
                Text(
                    if (permissionState.hasAccessibilityService) {
                        "Ativa. Abra apenas se quiser revisar a permissao."
                    } else {
                        "Pendente. Necessaria para ler ofertas visiveis."
                    }
                )
            },
            trailingContent = {
                TextButton(
                    modifier = Modifier.testTag(UiTestTags.OPEN_ACCESSIBILITY_BUTTON),
                    onClick = onOpenAccessibility
                ) {
                    Text(if (permissionState.hasAccessibilityService) "Abrir" else "Ativar")
                }
            }
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text("Posição do aviso") },
            supportingContent = {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OverlayPositionPreference.entries.forEachIndexed { index, position ->
                        SegmentedButton(
                            modifier = Modifier.testTag(position.testTag),
                            selected = overlayPosition == position,
                            onClick = { onOverlayPositionChange(position) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = OverlayPositionPreference.entries.size
                            )
                        ) {
                            Text(position.label)
                        }
                    }
                }
            }
        )
        HorizontalDivider()
        ListItem(
            modifier = Modifier.testTag(UiTestTags.SETTINGS_GOAL_ROW),
            headlineContent = { Text("Meta atual") },
            supportingContent = {
                Text("${goal.minValuePerKm.toGoalMoney()}/km · ${goal.minValuePerHour.toGoalMoneyNoCentsIfRound()}/h")
            },
            trailingContent = {
                TextButton(onClick = onOpenGoal) {
                    Text("Editar")
                }
            }
        )
        HorizontalDivider()
        ListItem(
            modifier = Modifier.testTag(UiTestTags.SETTINGS_PRIVACY_ROW),
            headlineContent = { Text("Privacidade e segurança") },
            supportingContent = { Text("Resumo de uso e dados") },
            trailingContent = {
                TextButton(onClick = onOpenPrivacy) {
                    Text("Abrir")
                }
            }
        )
        if (BuildConfig.DEBUG) {
            HorizontalDivider()
            ListItem(
                modifier = Modifier.testTag(UiTestTags.SETTINGS_ADVANCED_ROW),
                headlineContent = { Text("Avançado") },
                supportingContent = { Text("Diagnóstico para teste") },
                trailingContent = {
                    TextButton(onClick = onOpenAdvanced) {
                        Text("Abrir")
                    }
                }
            )
        }
    }
}

@Composable
private fun DiagnosticsScreen(
    modifier: Modifier,
    snapshot: AppDiagnostics.Snapshot,
    onRefresh: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(UiTestTags.DIAGNOSTICS_SCREEN)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = CalcMotSpacing.ScreenHorizontal, vertical = CalcMotSpacing.ScreenVertical),
        verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Md)
    ) {
        Text("Diagnóstico", style = MaterialTheme.typography.headlineSmall)
        CalcMotButton(text = "Atualizar", onClick = onRefresh, variant = CalcMotButtonVariant.SECONDARY)
        DiagnosticLine("Último status", snapshot.lastStage.label)
        DiagnosticLine("Eventos da Uber", snapshot.eventCount.toString())
        DiagnosticLine("Cards na acessibilidade", snapshot.treeCandidateCount.toString())
        DiagnosticLine(
            "Rotas completas",
            "UIA ${snapshot.uiautomatorCompleteCards} / arvore ${snapshot.internalTreeCompleteCards}"
        )
        DiagnosticLine(
            "Rotas rejeitadas",
            "UIA ${snapshot.uiautomatorRejectedFrames} / arvore ${snapshot.internalTreeRejectedFrames}"
        )
        DiagnosticLine("Roots vistos", snapshot.treeRootsSeenCount.toString())
        DiagnosticLine("Textos vistos", snapshot.treeTextsSeenCount.toString())
        DiagnosticLine(
            "Preço/botão/blocos",
            "${snapshot.treePriceSeenCount}/${snapshot.treeButtonSeenCount}/${snapshot.treeBlocksSeenCount}"
        )
        DiagnosticLine("Confirmações", snapshot.stableOfferCount.toString())
        DiagnosticLine("Overlays exibidos", snapshot.overlayShownCount.toString())
        DiagnosticLine("Erros de overlay", snapshot.overlayErrorCount.toString())
        DiagnosticLine("Frames rejeitados", snapshot.frameRejectedCount.toString())
    }
}

@Composable
private fun DiagnosticLine(label: String, value: String) {
    CalcMotCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CalcMotSpacing.Md),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = CalcMotTypography.Body)
            Text(
                text = value,
                style = CalcMotTypography.Body
            )
        }
    }
}

private enum class HomeDestination {
    START,
    FINANCE,
    RESULT,
    SETTINGS,
    HELP,
    PRIVACY,
    DIAGNOSTICS
}

private enum class HomeStatus(
    val label: String,
    val title: String,
    val description: String
) {
    READY(
        label = "Pronto",
        title = "Pronto para calcular",
        description = "Abra a Uber Driver ou 99 Driver. Quando aparecer uma oferta, o CalcMot mostra o semáforo de lucro."
    ),
    PAUSED(
        label = "Pausado",
        title = "Cálculo automático pausado",
        description = "Pausado por você. O CalcMot não analisa ofertas até você ligar novamente."
    ),
    PERMISSION_PENDING(
        label = "Necessário",
        title = "Falta ativar o cálculo automático",
        description = "Ative uma vez no Android para o CalcMot calcular ofertas nos apps de motorista."
    )
}

private val OverlayPositionPreference.testTag: String
    get() = when (this) {
        OverlayPositionPreference.HIGH -> UiTestTags.OVERLAY_POSITION_HIGH
        OverlayPositionPreference.MEDIUM -> UiTestTags.OVERLAY_POSITION_MEDIUM
        OverlayPositionPreference.LOW -> UiTestTags.OVERLAY_POSITION_LOW
    }

private fun Double.toGoalMoney(): String {
    return String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", this)
}

private fun Double.toGoalMoneyNoCentsIfRound(): String {
    return if (this % 1.0 == 0.0) {
        String.format(Locale.forLanguageTag("pt-BR"), "R$ %.0f", this)
    } else {
        toGoalMoney()
    }
}
