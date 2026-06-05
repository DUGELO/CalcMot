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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
        Scaffold { innerPadding ->
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
private fun TopBar(
    title: String,
    onMenu: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            modifier = Modifier.testTag(UiTestTags.DRAWER_MENU_BUTTON),
            onClick = onMenu
        ) {
            Text("Menu", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
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
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
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
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Header(status = status)
        StatusCard(
            status = status,
            monitoringEnabled = monitoringEnabled,
            onMonitoringChange = onMonitoringChange,
            permissionState = permissionState,
            onOpenAccessibility = onOpenAccessibility,
            onPermissionsRefresh = onPermissionsRefresh
        )
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(UiTestTags.OPEN_DRIVER_APP_BUTTON),
            onClick = onOpenDriverApp,
            enabled = permissionState.hasAccessibilityService
        ) {
            Text("Abrir app de motorista")
        }
        OverlayPositionCard(
            selected = overlayPosition,
            onSelected = onOverlayPositionChange
        )
        OverlayPreviewCard()
    }
}

@Composable
private fun Header(status: HomeStatus) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "CalcMot",
            style = MaterialTheme.typography.displayLarge
        )
        Text(
            text = "Ligue, abra seu app de motorista e veja se a corrida vale a pena.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary
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
    onPermissionsRefresh: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = status.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = status.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Switch(
                    modifier = Modifier.testTag(UiTestTags.MONITORING_SWITCH),
                    checked = monitoringEnabled,
                    onCheckedChange = onMonitoringChange,
                    enabled = permissionState.hasAccessibilityService
                )
            }

            if (!permissionState.hasAccessibilityService) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(UiTestTags.OPEN_ACCESSIBILITY_BUTTON),
                    onClick = onOpenAccessibility
                ) {
                    Text("Ativar acessibilidade")
                }
            }

            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(UiTestTags.REFRESH_PERMISSIONS_BUTTON),
                onClick = onPermissionsRefresh
            ) {
                Text("Atualizar estado")
            }
        }
    }
}

@Composable
private fun OverlayPositionCard(
    selected: OverlayPositionPreference,
    onSelected: (OverlayPositionPreference) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Posição do aviso",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
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

    Card(
        modifier = Modifier.testTag(UiTestTags.OVERLAY_PREVIEW),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Prévia",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(188.dp)
                    .background(
                        color = Color(0xFF22252B),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(18.dp),
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
private fun DiagnosticsScreen(
    snapshot: AppDiagnostics.Snapshot,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(UiTestTags.DIAGNOSTICS_SCREEN)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Diagnóstico",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Dados técnicos para teste de campo.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary
        )
        OutlinedButton(onClick = onRefresh) {
            Text("Atualizar")
        }
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
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun StatusPill(status: HomeStatus) {
    Surface(
        modifier = Modifier.testTag(UiTestTags.STATUS_PILL),
        color = status.color.copy(alpha = 0.16f),
        contentColor = status.color,
        shape = RoundedCornerShape(100.dp)
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            text = status.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
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
    val color: Color
) {
    READY(
        label = "Pronto",
        title = "Monitoramento ativo",
        description = "Pode abrir o app de motorista. O aviso aparece quando a oferta estiver completa.",
        color = Color(0xFF3DDC84)
    ),
    PAUSED(
        label = "Pausado",
        title = "Monitoramento pausado",
        description = "Ligue o switch para voltar a analisar ofertas.",
        color = Color(0xFFFFC453)
    ),
    PERMISSION_PENDING(
        label = "Falta ativar",
        title = "Ative a acessibilidade",
        description = "Essa permissão permite ler ofertas visíveis e mostrar o aviso.",
        color = Color(0xFFFF6670)
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
