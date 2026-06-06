package br.com.calcmot.ui.screens

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
import br.com.calcmot.ui.design.components.CalcMotScaffold
import br.com.calcmot.ui.design.components.CalcMotSectionHeader
import br.com.calcmot.ui.design.components.CalcMotStatusBadge
import br.com.calcmot.ui.design.components.CalcMotStatusVariant
import br.com.calcmot.ui.design.components.CalcMotSwitchRow
import br.com.calcmot.ui.design.components.CalcMotTopBar
import br.com.calcmot.ui.design.domain.DailySummaryCard
import br.com.calcmot.ui.design.domain.FinancialImpactSummaryCard
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotSpacing
import br.com.calcmot.ui.design.tokens.CalcMotTypography
import br.com.calcmot.ui.CALCMOT_SUPPORT_EMAIL
import br.com.calcmot.ui.UiTestTags
import kotlinx.coroutines.launch
import br.com.calcmot.ui.CALCMOT_SUPPORT_EMAIL
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
        CalcMotScaffold(
            topBar = {
                CalcMotTopBar(
                    title = destination.title,
                    onBackClick = if (destination != HomeDestination.START) {
                        { navigate(HomeDestination.START) }
                    } else null,
                    actions = {
                        CalcMotButton(
                            text = "Menu",
                            onClick = { scope.launch { drawerState.open() } },
                            variant = CalcMotButtonVariant.Ghost
                        )
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
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
                        onOpenDriverApp = {
                            context.packageManager
                                .getLaunchIntentForPackage(DRIVER_PACKAGE)
                                ?.let(context::startActivity)
                        }
                    )

                    HomeDestination.FINANCE -> FinanceScreen()
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
private fun AppDrawer(
    selected: HomeDestination,
    onSelect: (HomeDestination) -> Unit,
    onSupport: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = CalcMotColors.Surface,
        drawerContentColor = CalcMotColors.TextPrimary
    ) {
        Column(
            modifier = Modifier.padding(CalcMotSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)
        ) {
            Text(
                text = "CalcMot",
                style = CalcMotTypography.ScreenTitle,
                color = CalcMotColors.BrandPrimary
            )
            Spacer(modifier = Modifier.height(CalcMotSpacing.Md))
            NavigationDrawerItem(
                modifier = Modifier.testTag(UiTestTags.DRAWER_HOME_ITEM),
                label = { Text("Início") },
                selected = selected == HomeDestination.START,
                onClick = { onSelect(HomeDestination.START) }
            )
            NavigationDrawerItem(
                modifier = Modifier.testTag(UiTestTags.DRAWER_FINANCE_ITEM),
                label = { Text("Finanças") },
                selected = selected == HomeDestination.FINANCE,
                onClick = { onSelect(HomeDestination.FINANCE) }
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
        
        MonitoringCard(
            status = status,
            monitoringEnabled = monitoringEnabled,
            onMonitoringChange = onMonitoringChange,
            permissionState = permissionState,
            onOpenAccessibility = onOpenAccessibility,
            onPermissionsRefresh = onPermissionsRefresh
        )

        CalcMotButton(
            text = "Abrir app de motorista",
            onClick = onOpenDriverApp,
            modifier = Modifier.fillMaxWidth(),
            enabled = permissionState.hasAccessibilityService
        )

        DailySummaryCard(
            offersAnalyzed = 0, // In dynamic implementation this would come from a view model
            avgKm = "R$ 0,00",
            avgHour = "R$ 0,00"
        )

        OverlayPositionCard(
            selected = overlayPosition,
            onSelected = onOverlayPositionChange
        )
        
        OverlayPreviewCard()
    }
}

@Composable
private fun Header(status: HomeStatus) {
    Column(verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "CalcMot",
                style = CalcMotTypography.ScreenTitle,
                color = CalcMotColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )
            CalcMotStatusBadge(
                text = status.label,
                variant = status.badgeVariant
            )
        }
        Text(
            text = "Ligue, abra seu app de motorista e veja se a corrida vale a pena.",
            style = CalcMotTypography.Body,
            color = CalcMotColors.TextSecondary
        )
    }
}

@Composable
private fun MonitoringCard(
    status: HomeStatus,
    monitoringEnabled: Boolean,
    onMonitoringChange: (Boolean) -> Unit,
    permissionState: AppPermissionState,
    onOpenAccessibility: () -> Unit,
    onPermissionsRefresh: () -> Unit
) {
    CalcMotCard {
        CalcMotSwitchRow(
            title = status.title,
            description = status.description,
            checked = monitoringEnabled,
            onCheckedChange = onMonitoringChange
        )

        if (!permissionState.hasAccessibilityService) {
            Spacer(modifier = Modifier.height(CalcMotSpacing.Md))
            CalcMotButton(
                text = "Ativar acessibilidade",
                onClick = onOpenAccessibility,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(CalcMotSpacing.Sm))
        CalcMotButton(
            text = "Atualizar estado",
            onClick = onPermissionsRefresh,
            modifier = Modifier.fillMaxWidth(),
            variant = CalcMotButtonVariant.Secondary
        )
    }
}

@Composable
private fun OverlayPositionCard(
    selected: OverlayPositionPreference,
    onSelected: (OverlayPositionPreference) -> Unit
) {
    CalcMotCard {
        Text(
            text = "Posição do aviso",
            style = CalcMotTypography.CardTitle,
            color = CalcMotColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(CalcMotSpacing.Md))
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
        Spacer(modifier = Modifier.height(CalcMotSpacing.Sm))
        Text(
            text = "Você também pode arrastar o aviso na tela quando ele aparecer.",
            style = CalcMotTypography.Caption,
            color = CalcMotColors.TextMuted
        )
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

    CalcMotCard {
        Text(
            text = "Prévia do Overlay",
            style = CalcMotTypography.CardTitle,
            color = CalcMotColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(CalcMotSpacing.Md))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(188.dp)
                .background(
                    color = CalcMotColors.AppBackground,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(CalcMotSpacing.Md)
                )
                .padding(CalcMotSpacing.Md),
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
        CalcMotSectionHeader(title = "Diagnóstico")
        Text(
            text = "Dados técnicos para teste de campo.",
            style = CalcMotTypography.Body,
            color = CalcMotColors.TextSecondary
        )
        CalcMotButton(text = "Atualizar", onClick = onRefresh, variant = CalcMotButtonVariant.Secondary)
        
        DiagnosticLine("Último status", snapshot.lastStage.label)
        DiagnosticLine("Eventos da Uber", snapshot.eventCount.toString())
        DiagnosticLine("Cards na acessibilidade", snapshot.treeCandidateCount.toString())
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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = CalcMotTypography.Body, color = CalcMotColors.TextMuted)
            Text(
                text = value,
                style = CalcMotTypography.BodyStrong,
                color = CalcMotColors.TextPrimary
            )
        }
    }
}

private enum class HomeDestination(val title: String) {
    START("Início"),
    FINANCE("Finanças"),
    PRIVACY("Privacidade"),
    DIAGNOSTICS("Diagnóstico")
}

private enum class HomeStatus(
    val label: String,
    val title: String,
    val description: String,
    val badgeVariant: CalcMotStatusVariant
) {
    READY(
        label = "Pronto",
        title = "Monitoramento ativo",
        description = "Pode abrir o app de motorista. O aviso aparece quando a oferta estiver completa.",
        badgeVariant = CalcMotStatusVariant.Active
    ),
    PAUSED(
        label = "Pausado",
        title = "Monitoramento pausado",
        description = "Ligue o switch para voltar a analisar ofertas.",
        badgeVariant = CalcMotStatusVariant.Attention
    ),
    PERMISSION_PENDING(
        label = "Falta ativar",
        title = "Ative a acessibilidade",
        description = "Essa permissão permite ler ofertas visíveis e mostrar o aviso.",
        badgeVariant = CalcMotStatusVariant.Error
    )
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
