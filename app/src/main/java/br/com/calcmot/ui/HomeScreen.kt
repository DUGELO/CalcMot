package br.com.calcmot.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.calcmot.AppDiagnostics
import br.com.calcmot.AppPermissionState
import br.com.calcmot.AppSettings
import br.com.calcmot.BuildConfig
import br.com.calcmot.OverlayPositionPreference
import br.com.calcmot.model.FinancialImpactCalculator
import br.com.calcmot.model.ProfitabilityCalculator
import br.com.calcmot.model.TripData
import br.com.calcmot.overlay.OverlayView
import br.com.calcmot.ui.design.components.CalcMotButton
import br.com.calcmot.ui.design.components.CalcMotButtonVariant
import br.com.calcmot.ui.design.components.CalcMotCard
import br.com.calcmot.ui.design.components.CalcMotCardVariant
import br.com.calcmot.ui.design.components.CalcMotIconButton
import br.com.calcmot.ui.design.components.CalcMotScaffold
import br.com.calcmot.ui.design.components.CalcMotSectionHeader
import br.com.calcmot.ui.design.components.CalcMotStatusBadge
import br.com.calcmot.ui.design.components.CalcMotSwitchRow
import br.com.calcmot.ui.design.components.CalcMotTopBar
import br.com.calcmot.ui.design.domain.DailySummaryCard
import br.com.calcmot.ui.design.domain.DailySummaryUiState
import br.com.calcmot.ui.design.domain.FinancialImpactSummaryCard
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotShape
import br.com.calcmot.ui.design.tokens.CalcMotSpacing
import br.com.calcmot.ui.design.tokens.CalcMotTypography
import kotlinx.coroutines.launch

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
    var monitoringEnabled by remember {
        mutableStateOf(AppSettings.isMonitoringEnabled(context))
    }
    var overlayPosition by remember {
        mutableStateOf(AppSettings.getOverlayPosition(context))
    }

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
                onSelect = ::navigate,
                onSupport = {
                    scope.launch { drawerState.close() }
                    uriHandler.openUri("mailto:$CALCMOT_SUPPORT_EMAIL")
                }
            )
        }
    ) {
        CalcMotScaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                TopBar(
                    title = destination.title,
                    onMenu = { scope.launch { drawerState.open() } }
                )

                when (destination) {
                    HomeDestination.START -> StartContent(
                        status = status,
                        monitoringEnabled = monitoringEnabled,
                        permissionState = permissionState,
                        overlayPosition = overlayPosition,
                        onMonitoringChange = ::setMonitoringEnabled,
                        onOverlayPositionChange = ::setOverlayPosition,
                        onOpenAccessibility = {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        onPermissionsRefresh = {
                            onPermissionsRefresh()
                            diagnosticsRefreshKey++
                        },
                        onOpenSafety = { navigate(HomeDestination.SECURITY) },
                        onOpenDriverApp = {
                            context.packageManager
                                .getLaunchIntentForPackage(DRIVER_PACKAGE)
                                ?.let(context::startActivity)
                        }
                    )

                    HomeDestination.FINANCE -> FinanceScreen()
                    HomeDestination.SECURITY -> SafetyScreen(
                        monitoringEnabled = monitoringEnabled,
                        permissionState = permissionState,
                        onMonitoringChange = ::setMonitoringEnabled,
                        onOpenAccessibility = {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    )
                    HomeDestination.PRIVACY -> PrivacyPolicyScreen(
                        onBack = { navigate(HomeDestination.START) },
                        onSupport = { uriHandler.openUri("mailto:$CALCMOT_SUPPORT_EMAIL") }
                    )

                    HomeDestination.DIAGNOSTICS -> DiagnosticsScreen(
                        snapshot = remember(diagnosticsRefreshKey) { AppDiagnostics.read(context) },
                        onRefresh = { diagnosticsRefreshKey++ }
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    title: String,
    onMenu: () -> Unit
) {
    CalcMotTopBar(
        title = title,
        navigation = {
            CalcMotIconButton(
                modifier = Modifier.testTag(UiTestTags.DRAWER_MENU_BUTTON),
                text = "Menu",
                onClick = onMenu
            )
        }
    )
}

@Composable
private fun AppDrawer(
    selected: HomeDestination,
    onSelect: (HomeDestination) -> Unit,
    onSupport: () -> Unit
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "CalcMot",
                style = CalcMotTypography.ScreenTitle,
                color = CalcMotColors.TextPrimary
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
                modifier = Modifier.testTag(UiTestTags.DRAWER_SECURITY_ITEM),
                label = { Text("Segurança") },
                selected = selected == HomeDestination.SECURITY,
                onClick = { onSelect(HomeDestination.SECURITY) }
            )
            NavigationDrawerItem(
                modifier = Modifier.testTag(UiTestTags.DRAWER_PRIVACY_ITEM),
                label = { Text("Privacidade") },
                selected = selected == HomeDestination.PRIVACY,
                onClick = { onSelect(HomeDestination.PRIVACY) }
            )
            NavigationDrawerItem(
                modifier = Modifier.testTag(UiTestTags.DRAWER_SUPPORT_ITEM),
                label = { Text("Suporte") },
                selected = false,
                onClick = onSupport
            )
            if (BuildConfig.DEBUG) {
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
private fun StartContent(
    status: HomeStatus,
    monitoringEnabled: Boolean,
    permissionState: AppPermissionState,
    overlayPosition: OverlayPositionPreference,
    onMonitoringChange: (Boolean) -> Unit,
    onOverlayPositionChange: (OverlayPositionPreference) -> Unit,
    onOpenAccessibility: () -> Unit,
    onPermissionsRefresh: () -> Unit,
    onOpenSafety: () -> Unit,
    onOpenDriverApp: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(UiTestTags.HOME_SCREEN)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = CalcMotSpacing.ScreenHorizontal, vertical = CalcMotSpacing.ScreenVertical),
        verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.SectionGap)
    ) {
        Header(status = status)
        StatusCard(
            status = status,
            monitoringEnabled = monitoringEnabled,
            onMonitoringChange = onMonitoringChange,
            permissionState = permissionState,
            onOpenAccessibility = onOpenAccessibility,
            onPermissionsRefresh = onPermissionsRefresh,
            onOpenDriverApp = onOpenDriverApp
        )
        SafetySummaryCard(onOpenSafety = onOpenSafety)
        DailySummaryCard(
            state = DailySummaryUiState(
                offersAnalyzed = 0,
                offersAboveGoal = 0,
                offersBelowGoal = 0,
                averagePerKm = "R$ 0,00",
                averagePerHour = "R$ 0"
            )
        )
        FinancialImpactSummaryCard(
            title = "Meta financeira no centro",
            body = "O CalcMot compara cada oferta com sua meta e mostra o impacto sem prometer ganho garantido.",
            positive = true
        )
        OverlayPreviewCard()
        OverlayPositionCard(
            selected = overlayPosition,
            onSelected = onOverlayPositionChange
        )
    }
}

@Composable
private fun SafetySummaryCard(onOpenSafety: () -> Unit) {
    CalcMotCard(
        modifier = Modifier.testTag(UiTestTags.SAFETY_SUMMARY_CARD),
        variant = CalcMotCardVariant.HIGHLIGHT
    ) {
        Column(
            modifier = Modifier.padding(CalcMotSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Md)
        ) {
            Text(
                text = "Seguro para usar no corre",
                style = CalcMotTypography.CardTitle,
                color = CalcMotColors.TextPrimary
            )
            Text(
                text = "Lê só a oferta visível, calcula no aparelho e não toca na tela por você.",
                style = CalcMotTypography.Body,
                color = CalcMotColors.TextSecondary
            )
            CalcMotButton(
                text = "Ver segurança",
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenSafety,
                variant = CalcMotButtonVariant.SECONDARY
            )
        }
    }
}

@Composable
private fun Header(status: HomeStatus) {
    Column(verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)) {
        CalcMotSectionHeader(
            title = "CalcMot",
            subtitle = "Veja rapidamente se a corrida compensa."
        )
        StatusPill(status = status)
    }
}

@Composable
private fun StatusCard(
    status: HomeStatus,
    monitoringEnabled: Boolean,
    onMonitoringChange: (Boolean) -> Unit,
    permissionState: AppPermissionState,
    onOpenAccessibility: () -> Unit,
    onPermissionsRefresh: () -> Unit,
    onOpenDriverApp: () -> Unit
) {
    CalcMotCard(variant = status.cardVariant) {
        Column(
            modifier = Modifier.padding(CalcMotSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Md)
        ) {
            CalcMotSwitchRow(
                title = status.title,
                description = status.description,
                checked = monitoringEnabled,
                onCheckedChange = onMonitoringChange,
                enabled = permissionState.hasAccessibilityService,
                switchModifier = Modifier.testTag(UiTestTags.MONITORING_SWITCH)
            )

            when (status) {
                HomeStatus.PERMISSION_PENDING -> {
                    CalcMotButton(
                        text = "Permitir leitura da oferta",
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(UiTestTags.OPEN_ACCESSIBILITY_BUTTON),
                        onClick = onOpenAccessibility
                    )
                    CalcMotButton(
                        text = "Já permiti, verificar novamente",
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(UiTestTags.REFRESH_PERMISSIONS_BUTTON),
                        onClick = onPermissionsRefresh,
                        variant = CalcMotButtonVariant.SECONDARY
                    )
                }

                HomeStatus.READY -> {
                    CalcMotButton(
                        text = "Abrir Uber Driver",
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(UiTestTags.OPEN_DRIVER_APP_BUTTON),
                        onClick = onOpenDriverApp
                    )
                }

                HomeStatus.PAUSED -> {
                    CalcMotButton(
                        text = "Ligar CalcMot",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onMonitoringChange(true) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OverlayPositionCard(
    selected: OverlayPositionPreference,
    onSelected: (OverlayPositionPreference) -> Unit
) {
    CalcMotCard {
        Column(
            modifier = Modifier.padding(CalcMotSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Md)
        ) {
            Text(
                text = "Posição do aviso",
                style = CalcMotTypography.CardTitle,
                color = CalcMotColors.TextPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)) {
                OverlayPositionPreference.entries.forEach { position ->
                    FilterChip(
                        modifier = Modifier.testTag(position.testTag),
                        selected = selected == position,
                        onClick = { onSelected(position) },
                        label = { Text(position.label) }
                    )
                }
            }
            Text(
                text = "Você também pode arrastar o aviso na tela quando ele aparecer.",
                style = CalcMotTypography.Caption,
                color = CalcMotColors.TextSecondary
            )
        }
    }
}

@Composable
private fun OverlayPreviewCard() {
    val context = LocalContext.current
    val profitability = remember {
        ProfitabilityCalculator.calculate(
            tripData = sampleTripData,
            settings = AppSettings.getProfitabilitySettings(context)
        )
    }
    val financialImpact = remember {
        if (AppSettings.isFinancialImpactEnabled(context)) {
            FinancialImpactCalculator.calculate(
                tripData = sampleTripData,
                goal = AppSettings.getDriverGoal(context)
            )
        } else {
            null
        }
    }

    CalcMotCard(modifier = Modifier.testTag(UiTestTags.OVERLAY_PREVIEW)) {
        Column(
            modifier = Modifier.padding(CalcMotSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Md)
        ) {
            Text(
                text = "Prévia",
                style = CalcMotTypography.CardTitle,
                color = CalcMotColors.TextPrimary
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(188.dp)
                    .background(
                        color = CalcMotColors.SurfaceSoft,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(CalcMotShape.Sm)
                    )
                    .padding(CalcMotSpacing.Lg),
                contentAlignment = Alignment.TopCenter
            ) {
                OverlayView(
                    tripData = sampleTripData,
                    profitability = profitability,
                    financialImpact = financialImpact
                )
            }
        }
    }
}

@Composable
private fun SafetyScreen(
    monitoringEnabled: Boolean,
    permissionState: AppPermissionState,
    onMonitoringChange: (Boolean) -> Unit,
    onOpenAccessibility: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(UiTestTags.SECURITY_SCREEN)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = CalcMotSpacing.ScreenHorizontal, vertical = CalcMotSpacing.ScreenVertical),
        verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.SectionGap)
    ) {
        CalcMotSectionHeader(
            title = "Segurança",
            subtitle = "O que o CalcMot lê, onde atua e como você mantém o controle."
        )
        SafetyControlCard(
            monitoringEnabled = monitoringEnabled,
            permissionState = permissionState,
            onMonitoringChange = onMonitoringChange,
            onOpenAccessibility = onOpenAccessibility
        )
        SafetyTextCard(
            title = "Onde o CalcMot atua",
            body = "Ele fica em espera e calcula quando encontra uma oferta completa no app de motorista."
        )
        SafetyTextCard(
            title = "Onde ele não deve atuar",
            body = "Em banco, conversa, foto, navegador ou outro app sensível, o CalcMot não mostra cálculo de oferta."
        )
        SafetyTextCard(
            title = "O que ele não faz",
            body = "Não aceita corrida, não recusa corrida, não toca na tela e não controla a Uber."
        )
        SafetyTextCard(
            title = "Dados no aparelho",
            body = "O cálculo usa a oferta visível e acontece localmente. O CalcMot não envia dados de corrida para servidor."
        )
    }
}

@Composable
private fun SafetyControlCard(
    monitoringEnabled: Boolean,
    permissionState: AppPermissionState,
    onMonitoringChange: (Boolean) -> Unit,
    onOpenAccessibility: () -> Unit
) {
    CalcMotCard(
        variant = if (monitoringEnabled && permissionState.hasAccessibilityService) {
            CalcMotCardVariant.SUCCESS
        } else {
            CalcMotCardVariant.HIGHLIGHT
        }
    ) {
        Column(
            modifier = Modifier.padding(CalcMotSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Md)
        ) {
            CalcMotSwitchRow(
                title = if (monitoringEnabled) "CalcMot em espera" else "CalcMot pausado por você",
                description = if (monitoringEnabled) {
                    "Use este controle quando quiser parar a leitura da oferta."
                } else {
                    "Enquanto estiver pausado, o CalcMot não analisa ofertas."
                },
                checked = monitoringEnabled,
                onCheckedChange = onMonitoringChange,
                enabled = permissionState.hasAccessibilityService,
                switchModifier = Modifier.testTag(UiTestTags.MONITORING_SWITCH)
            )
            if (!permissionState.hasAccessibilityService) {
                CalcMotButton(
                    text = "Permitir leitura da oferta",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(UiTestTags.OPEN_ACCESSIBILITY_BUTTON),
                    onClick = onOpenAccessibility
                )
            }
        }
    }
}

@Composable
private fun SafetyTextCard(
    title: String,
    body: String
) {
    CalcMotCard {
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
private fun DiagnosticsScreen(
    snapshot: AppDiagnostics.Snapshot,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(UiTestTags.DIAGNOSTICS_SCREEN)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = CalcMotSpacing.ScreenHorizontal, vertical = CalcMotSpacing.ScreenVertical),
        verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Md)
    ) {
        CalcMotSectionHeader(
            title = "Diagnóstico",
            subtitle = "Dados técnicos para teste de campo."
        )
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
            Text(label, style = CalcMotTypography.Body, color = CalcMotColors.TextPrimary)
            Text(
                text = value,
                style = CalcMotTypography.Body,
                color = CalcMotColors.TextSecondary
            )
        }
    }
}

@Composable
private fun StatusPill(status: HomeStatus) {
    CalcMotStatusBadge(
        modifier = Modifier.testTag(UiTestTags.STATUS_PILL),
        text = status.label,
        color = status.color
    )
}

private enum class HomeDestination(val title: String) {
    START("Início"),
    FINANCE("Metas"),
    SECURITY("Segurança"),
    PRIVACY("Privacidade"),
    DIAGNOSTICS("Diagnóstico")
}

private enum class HomeStatus(
    val label: String,
    val title: String,
    val description: String,
    val color: Color
) {
    READY(
        label = "Em espera",
        title = "CalcMot em espera",
        description = "Abra a Uber. O CalcMot calcula quando aparecer uma oferta completa.",
        color = CalcMotColors.Success
    ),
    PAUSED(
        label = "Pausado",
        title = "CalcMot pausado por você",
        description = "O CalcMot não vai analisar ofertas até você ligar novamente.",
        color = CalcMotColors.Warning
    ),
    PERMISSION_PENDING(
        label = "Falta permissão",
        title = "Falta permitir leitura da oferta",
        description = "O CalcMot precisa ler a oferta visível para calcular R$/km, R$/hora e impacto na sua meta.",
        color = CalcMotColors.Danger
    )
}

private val HomeStatus.cardVariant: CalcMotCardVariant
    get() = when (this) {
        HomeStatus.READY -> CalcMotCardVariant.SUCCESS
        HomeStatus.PAUSED -> CalcMotCardVariant.HIGHLIGHT
        HomeStatus.PERMISSION_PENDING -> CalcMotCardVariant.DANGER
    }

private const val DRIVER_PACKAGE = "com.ubercab.driver"

private val OverlayPositionPreference.testTag: String
    get() = when (this) {
        OverlayPositionPreference.HIGH -> UiTestTags.OVERLAY_POSITION_HIGH
        OverlayPositionPreference.MEDIUM -> UiTestTags.OVERLAY_POSITION_MEDIUM
        OverlayPositionPreference.LOW -> UiTestTags.OVERLAY_POSITION_LOW
    }

private val sampleTripData = TripData(
    valor = 30.0,
    distanciaKm = 12.0,
    minutosTotais = 43,
    valorPorKm = 2.5,
    valorPorHora = 41.86
)
