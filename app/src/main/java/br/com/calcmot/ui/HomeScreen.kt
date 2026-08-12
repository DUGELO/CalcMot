package br.com.calcmot.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.calcmot.AppDiagnostics
import br.com.calcmot.AppPermissionState
import br.com.calcmot.AppSettings
import br.com.calcmot.BuildConfig
import br.com.calcmot.DriverApp
import br.com.calcmot.DriverAppLauncher
import br.com.calcmot.OverlayPositionPreference
import br.com.calcmot.OverlayThemePreference
import br.com.calcmot.R
import br.com.calcmot.model.DriverGoal
import br.com.calcmot.ui.design.tokens.CalcMotColors
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LegacyHomeScreen(
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
    var financialImpactEnabled by remember { mutableStateOf(AppSettings.isFinancialImpactEnabled(context)) }
    var overlayPosition by remember { mutableStateOf(AppSettings.getOverlayPosition(context)) }
    var overlayTheme by remember { mutableStateOf(AppSettings.getOverlayTheme(context)) }

    fun navigate(next: HomeDestination) {
        destination = next
        if (next == HomeDestination.DIAGNOSTICS) diagnosticsRefreshKey++
        scope.launch { drawerState.close() }
    }

    fun setMonitoringEnabled(enabled: Boolean) {
        AppSettings.setMonitoringEnabled(context, enabled)
        monitoringEnabled = enabled
    }

    fun setFinancialImpactEnabled(enabled: Boolean) {
        AppSettings.setFinancialImpactEnabled(context, enabled)
        financialImpactEnabled = enabled
    }

    fun setOverlayPosition(position: OverlayPositionPreference) {
        AppSettings.setOverlayPosition(context, position)
        overlayPosition = position
    }

    fun setOverlayTheme(theme: OverlayThemePreference) {
        AppSettings.setOverlayTheme(context, theme)
        overlayTheme = theme
    }

    val status = when {
        !permissionState.hasAccessibilityService -> HomeStatus.PERMISSION_PENDING
        monitoringEnabled -> HomeStatus.READY
        else -> HomeStatus.PAUSED
    }
    val usesPrototypeChrome = destination == HomeDestination.FEEDBACK ||
        destination == HomeDestination.FEEDBACK_SUCCESS ||
        destination == HomeDestination.HELP ||
        destination == HomeDestination.FINANCE ||
        destination == HomeDestination.SETTINGS ||
        destination == HomeDestination.OVERLAY_POSITION ||
        destination == HomeDestination.OVERLAY_THEME ||
        destination == HomeDestination.PRIVACY ||
        (destination == HomeDestination.START && status == HomeStatus.READY) ||
        (destination == HomeDestination.START && status == HomeStatus.PAUSED) ||
        (destination == HomeDestination.START && status == HomeStatus.PERMISSION_PENDING)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                status = status,
                onHome = { navigate(HomeDestination.START) },
                onGoal = { navigate(HomeDestination.FINANCE) },
                onSettings = { navigate(HomeDestination.SETTINGS) },
                onHelp = { navigate(HomeDestination.HELP) },
                onPrivacy = { navigate(HomeDestination.PRIVACY) },
                onFeedback = { navigate(HomeDestination.FEEDBACK) }
            )
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                if (!usesPrototypeChrome) {
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
            }
        ) { innerPadding ->
            when (destination) {
                HomeDestination.START -> HomeContent(
                    modifier = if (usesPrototypeChrome) Modifier else Modifier.padding(innerPadding),
                    status = status,
                    permissionState = permissionState,
                    onMonitoringChange = ::setMonitoringEnabled,
                    onOpenAccessibility = {
                        openAccessibilitySettings(context)
                    },
                    onOpenMenu = {
                        scope.launch { drawerState.open() }
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
                    onOpenSettings = { navigate(HomeDestination.SETTINGS) },
                    onOpenHelp = { navigate(HomeDestination.HELP) },
                    onOpenPrivacy = { navigate(HomeDestination.PRIVACY) }
                )

                HomeDestination.FINANCE -> FinanceScreen(
                    modifier = Modifier,
                    onBack = { navigate(HomeDestination.START) }
                )
                HomeDestination.SETTINGS -> SettingsScreen(
                    modifier = Modifier,
                    monitoringEnabled = monitoringEnabled,
                    financialImpactEnabled = financialImpactEnabled,
                    permissionState = permissionState,
                    overlayPosition = overlayPosition,
                    overlayTheme = overlayTheme,
                    onBack = { navigate(HomeDestination.START) },
                    onMonitoringChange = ::setMonitoringEnabled,
                    onFinancialImpactChange = ::setFinancialImpactEnabled,
                    onOpenAccessibility = {
                        openAccessibilitySettings(context)
                    },
                    onOpenOverlayPosition = { navigate(HomeDestination.OVERLAY_POSITION) },
                    onOpenOverlayTheme = { navigate(HomeDestination.OVERLAY_THEME) },
                    onOpenPrivacy = { navigate(HomeDestination.PRIVACY) },
                    onOpenHelp = { navigate(HomeDestination.HELP) }
                )

                HomeDestination.OVERLAY_POSITION -> OverlayPositionScreen(
                    currentPosition = overlayPosition,
                    onBack = { navigate(HomeDestination.SETTINGS) },
                    onSave = { position ->
                        setOverlayPosition(position)
                        navigate(HomeDestination.SETTINGS)
                    }
                )

                HomeDestination.OVERLAY_THEME -> OverlayThemeScreen(
                    currentTheme = overlayTheme,
                    onBack = { navigate(HomeDestination.SETTINGS) },
                    onSave = { theme ->
                        setOverlayTheme(theme)
                        navigate(HomeDestination.SETTINGS)
                    }
                )

                HomeDestination.HELP -> HelpScreen(
                    modifier = Modifier,
                    onBack = { navigate(HomeDestination.START) },
                    onOpenPrivacy = { navigate(HomeDestination.PRIVACY) },
                    onSupport = { uriHandler.openUri("mailto:$CALCMOT_SUPPORT_EMAIL") }
                )

                HomeDestination.PRIVACY -> PrivacyPolicyScreen(
                    modifier = Modifier,
                    onBack = { navigate(HomeDestination.HELP) },
                    onSupport = { uriHandler.openUri(CALCMOT_PRIVACY_POLICY_URL) }
                )

                HomeDestination.DIAGNOSTICS -> DiagnosticsScreen(
                    snapshot = remember(diagnosticsRefreshKey) { AppDiagnostics.read(context) },
                    hasAccessibility = permissionState.hasAccessibilityService,
                    monitoringEnabled = monitoringEnabled,
                    goalPerKm = "${AppSettings.getDriverGoal(context).minValuePerKm.toGoalMoney()}/km",
                    goalPerHour = "${AppSettings.getDriverGoal(context).minValuePerHour.toGoalMoneyNoCentsIfRound()}/h",
                    onBack = { navigate(HomeDestination.SETTINGS) },
                    onOpenDriverApp = {
                        if (DriverAppLauncher.launch(context, DriverApp.UBER) == null) {
                            Toast.makeText(
                                context,
                                "${DriverApp.UBER.displayName} nao esta instalado.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onOpenHelp = { navigate(HomeDestination.HELP) }
                )

                HomeDestination.FEEDBACK -> FeedbackScreen(
                    onBack = { navigate(HomeDestination.START) },
                    onSubmit = {
                        navigate(HomeDestination.FEEDBACK_SUCCESS)
                        FeedbackSubmitResult.OPENED
                    }
                )

                HomeDestination.FEEDBACK_SUCCESS -> FeedbackSuccessScreen(
                    onBack = { navigate(HomeDestination.FEEDBACK) },
                    onHome = { navigate(HomeDestination.START) },
                    onSendAnotherFeedback = { navigate(HomeDestination.FEEDBACK) }
                )
            }
        }
    }
}

@Composable
internal fun AppDrawer(
    status: HomeStatus,
    onHome: () -> Unit,
    onGoal: () -> Unit,
    onSettings: () -> Unit,
    onHelp: () -> Unit,
    onPrivacy: () -> Unit,
    onFeedback: () -> Unit,
    diagnosticsEnabled: Boolean = false,
    onVersionTap: () -> Unit = {},
    onDiagnostics: () -> Unit = {}
) {
    val drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxHeight()
            .width(296.dp)
            .clip(drawerShape)
            .border(BorderStroke(1.dp, CalcMotColors.BorderSubtle), drawerShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        CalcMotColors.SurfaceElevated.copy(alpha = 0.98f),
                        CalcMotColors.Surface.copy(alpha = 0.98f),
                        CalcMotColors.AppBackground.copy(alpha = 0.99f)
                    )
                ),
                drawerShape
            )
            .testTag(UiTestTags.DRAWER_PANEL),
        drawerShape = drawerShape,
        drawerContainerColor = Color.Transparent,
        drawerContentColor = CalcMotColors.TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 28.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            DrawerHeader()
            DrawerStatusCard(status = status)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DrawerMenuItem(
                    text = "Início",
                    icon = Icons.Outlined.Home,
                    selected = true,
                    testTag = UiTestTags.DRAWER_HOME_ITEM,
                    onClick = onHome
                )
                DrawerMenuItem(
                    text = "Minha meta",
                    icon = Icons.Outlined.MyLocation,
                    selected = false,
                    testTag = UiTestTags.DRAWER_FINANCE_ITEM,
                    onClick = onGoal
                )
                DrawerMenuItem(
                    text = "Configurações",
                    icon = Icons.Outlined.Settings,
                    selected = false,
                    testTag = UiTestTags.DRAWER_SETTINGS_ITEM,
                    onClick = onSettings
                )
                DrawerMenuItem(
                    text = "Ajuda",
                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
                    selected = false,
                    testTag = UiTestTags.DRAWER_HELP_ITEM,
                    onClick = onHelp
                )
                DrawerMenuItem(
                    text = "Privacidade",
                    icon = Icons.Outlined.Lock,
                    selected = false,
                    testTag = UiTestTags.DRAWER_PRIVACY_ITEM,
                    onClick = onPrivacy
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 4.dp),
                color = CalcMotColors.BorderSubtle
            )
            DrawerFooter(
                selectedFeedback = false,
                onFeedback = onFeedback,
                diagnosticsEnabled = diagnosticsEnabled,
                onVersionTap = onVersionTap,
                onDiagnostics = onDiagnostics
            )
        }
    }
}

@Composable
private fun DrawerHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.calcmot_logo_hero),
            contentDescription = null,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(18.dp)),
            contentScale = ContentScale.Fit
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            DrawerWordmark(fontSize = 32)
            Text(
                text = "Semáforo de lucro para motoristas",
                color = CalcMotColors.TextSecondary,
                fontSize = 13.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun DrawerWordmark(fontSize: Int = 20) {
    Text(
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = CalcMotColors.TextPrimary,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Black
                )
            ) {
                append("Calc")
            }
            withStyle(
                SpanStyle(
                    color = CalcMotColors.Success,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Black
                )
            ) {
                append("Mot")
            }
        },
        fontSize = fontSize.sp,
        lineHeight = (fontSize + 2).sp
    )
}

@Composable
private fun DrawerStatusCard(status: HomeStatus) {
    val active = status == HomeStatus.READY
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, CalcMotColors.BorderSubtle), RoundedCornerShape(14.dp))
            .background(CalcMotColors.SurfaceElevated.copy(alpha = 0.66f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.calcmot_logo_hero),
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Fit
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = if (active) "Pronto para calcular" else status.title,
                color = CalcMotColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = if (active) "Cálculo automático ativo" else status.label,
                color = CalcMotColors.TextSecondary,
                fontSize = 14.sp,
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(
                    if (active) CalcMotColors.Success else CalcMotColors.Warning,
                    CircleShape
                )
        )
    }
}

@Composable
private fun DrawerMenuItem(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(
                if (selected) CalcMotColors.Success.copy(alpha = 0.12f) else Color.Transparent,
                shape
            )
            .clickable(onClick = onClick)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(42.dp)
                .background(if (selected) CalcMotColors.Success else Color.Transparent)
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .padding(start = 16.dp)
                .size(27.dp),
            tint = if (selected) CalcMotColors.Success else CalcMotColors.TextPrimary
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 20.dp),
            color = if (selected) CalcMotColors.Success else CalcMotColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun DrawerFooter(
    selectedFeedback: Boolean,
    onFeedback: () -> Unit,
    diagnosticsEnabled: Boolean,
    onVersionTap: () -> Unit,
    onDiagnostics: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DrawerWordmark(fontSize = 18)
            Text(
                text = "Versão ${BuildConfig.VERSION_NAME}",
                modifier = Modifier.clickable(onClick = onVersionTap),
                color = CalcMotColors.TextSecondary,
                fontSize = 15.sp
            )
        }
        if (diagnosticsEnabled) {
            DrawerMenuItem(
                text = "Diagnóstico",
                icon = Icons.Outlined.Security,
                selected = false,
                testTag = UiTestTags.DRAWER_DIAGNOSTICS_ITEM,
                onClick = onDiagnostics
            )
        }
        DrawerMenuItem(
            text = "Enviar feedback",
            icon = Icons.Outlined.ChatBubbleOutline,
            selected = selectedFeedback,
            testTag = UiTestTags.DRAWER_FEEDBACK_ITEM,
            onClick = onFeedback
        )
    }
}

@Composable
internal fun HomeContent(
    modifier: Modifier,
    status: HomeStatus,
    permissionState: AppPermissionState,
    driverGoalOverride: DriverGoal? = null,
    onMonitoringChange: (Boolean) -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenMenu: () -> Unit,
    onPermissionsRefresh: () -> Unit,
    onOpenDriverApp: (DriverApp) -> Unit,
    onEditGoal: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onRestartReading: () -> Unit = {}
) {
    val context = LocalContext.current
    val storedDriverGoal = remember { AppSettings.getDriverGoal(context) }
    val driverGoal = driverGoalOverride ?: storedDriverGoal

    if (status == HomeStatus.PERMISSION_PENDING) {
        HomePermissionRequiredScreen(
            modifier = modifier,
            onMenu = onOpenMenu,
            onActivatePermission = onOpenAccessibility,
            onHowItWorks = onOpenHelp,
            goalPerKm = "${driverGoal.minValuePerKm.toGoalMoney()}/km",
            goalPerHour = "${driverGoal.minValuePerHour.toGoalMoneyNoCentsIfRound()}/h"
        )
        return
    }

    if (status == HomeStatus.READY) {
        HomeReadyScreen(
            modifier = modifier,
            onMenu = onOpenMenu,
            onOpenUber = { onOpenDriverApp(DriverApp.UBER) },
            onOpen99 = { onOpenDriverApp(DriverApp.NINETY_NINE) },
            onOpenGoal = onEditGoal,
            onOpenSettings = onOpenSettings,
            onOpenHelp = onOpenHelp,
            onRestartReading = onRestartReading,
            goalPerKm = "${driverGoal.minValuePerKm.toGoalMoney()}/km",
            goalPerHour = "${driverGoal.minValuePerHour.toGoalMoneyNoCentsIfRound()}/h"
        )
        return
    }

    HomePausedScreen(
        modifier = modifier,
        onMenu = onOpenMenu,
        onResume = { onMonitoringChange(true) },
        onOpenSettings = onOpenSettings,
        goalPerKm = "${driverGoal.minValuePerKm.toGoalMoney()}/km",
        goalPerHour = "${driverGoal.minValuePerHour.toGoalMoneyNoCentsIfRound()}/h"
    )
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
internal fun SettingsScreen(
    modifier: Modifier,
    monitoringEnabled: Boolean,
    financialImpactEnabled: Boolean,
    permissionState: AppPermissionState,
    overlayPosition: OverlayPositionPreference,
    overlayTheme: OverlayThemePreference,
    onBack: () -> Unit,
    onMonitoringChange: (Boolean) -> Unit,
    onFinancialImpactChange: (Boolean) -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenOverlayPosition: () -> Unit,
    onOpenOverlayTheme: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenHelp: () -> Unit,
    diagnosticsEnabled: Boolean = false,
    onOpenDiagnostics: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CalcMotColors.AppBackground)
            .testTag(UiTestTags.SETTINGS_SCREEN)
    ) {
        SettingsBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            SettingsTopBar(onBack = onBack)
            Text(
                text = "Ajuste como o CalcMot funciona durante sua jornada.",
                color = CalcMotColors.TextSecondary,
                fontSize = 17.sp,
                lineHeight = 22.sp
            )

            SettingsSection(title = "Cálculo") {
                SettingsSwitchRow(
                    icon = Icons.Outlined.Speed,
                    title = "Cálculo automático",
                    subtitle = "Mostra o aviso quando uma oferta aparece",
                    checked = monitoringEnabled,
                    enabled = permissionState.hasAccessibilityService,
                    testTag = UiTestTags.MONITORING_SWITCH,
                    onCheckedChange = onMonitoringChange
                )
                SettingsDivider()
                SettingsSwitchRow(
                    icon = Icons.Outlined.TrendingUp,
                    title = "Mostrar impacto na meta",
                    subtitle = "Exibe quanto a oferta está acima ou abaixo da sua meta",
                    checked = financialImpactEnabled,
                    testTag = UiTestTags.FINANCIAL_IMPACT_SWITCH,
                    onCheckedChange = onFinancialImpactChange
                )
            }

            SettingsSection(title = "Aviso flutuante") {
                SettingsActionRow(
                    modifier = Modifier.testTag(UiTestTags.SETTINGS_POSITION_ROW),
                    icon = Icons.Outlined.CropSquare,
                    title = "Posição do aviso",
                    subtitle = "Escolha onde o semáforo aparece na tela",
                    value = overlayPosition.settingsLabel,
                    onClick = onOpenOverlayPosition
                )
                SettingsDivider()
                SettingsActionRow(
                    modifier = Modifier.testTag(UiTestTags.SETTINGS_OVERLAY_THEME_ROW),
                    icon = Icons.Outlined.Palette,
                    title = "Tema do aviso",
                    subtitle = "Escolha o estilo visual do semáforo",
                    value = overlayTheme.label,
                    onClick = onOpenOverlayTheme
                )
            }

            SettingsSection(title = "Segurança e suporte") {
                SettingsActionRow(
                    icon = Icons.Outlined.AccessibilityNew,
                    title = "Permissão de acessibilidade",
                    subtitle = if (permissionState.hasAccessibilityService) "Ativada" else "Desativada",
                    onClick = onOpenAccessibility
                )
                SettingsDivider()
                SettingsActionRow(
                    modifier = Modifier.testTag(UiTestTags.SETTINGS_PRIVACY_ROW),
                    icon = Icons.Outlined.Security,
                    title = "Privacidade",
                    subtitle = "Veja como o CalcMot usa as informações da tela",
                    onClick = onOpenPrivacy
                )
                SettingsDivider()
                SettingsActionRow(
                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
                    title = "Ajuda",
                    subtitle = "Entenda como o app funciona",
                    onClick = onOpenHelp
                )
                if (diagnosticsEnabled) {
                    SettingsDivider()
                    SettingsActionRow(
                        modifier = Modifier.testTag(UiTestTags.DRAWER_DIAGNOSTICS_ITEM),
                        icon = Icons.Outlined.Security,
                        title = "Diagnóstico de leitura",
                        subtitle = "Status técnico e recuperação da leitura",
                        onClick = onOpenDiagnostics
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 62.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            modifier = Modifier
                .size(48.dp)
                .testTag(UiTestTags.SETTINGS_BACK_BUTTON),
            onClick = onBack
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Voltar",
                tint = CalcMotColors.TextPrimary,
                modifier = Modifier.size(31.dp)
            )
        }
        Text(
            text = "Configurações",
            modifier = Modifier.padding(start = 18.dp),
            color = CalcMotColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Text(
            text = title,
            color = CalcMotColors.Success,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 18.dp, bottom = 10.dp)
        )
        SettingsDivider()
        content()
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 94.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CalcMotColors.Success.copy(alpha = if (enabled) 1f else 0.45f),
            modifier = Modifier.size(46.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 24.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = title,
                color = CalcMotColors.TextPrimary.copy(alpha = if (enabled) 1f else 0.55f),
                fontSize = 19.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = CalcMotColors.TextSecondary.copy(alpha = if (enabled) 1f else 0.55f),
                fontSize = 16.sp,
                lineHeight = 21.sp
            )
        }
        Switch(
            modifier = Modifier.testTag(testTag),
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 86.dp)
            .clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CalcMotColors.Success.copy(alpha = if (enabled) 1f else 0.42f),
            modifier = Modifier.size(45.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 24.dp, end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = title,
                color = CalcMotColors.TextPrimary.copy(alpha = if (enabled) 1f else 0.55f),
                fontSize = 19.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = CalcMotColors.TextSecondary.copy(alpha = if (enabled) 1f else 0.55f),
                fontSize = 16.sp,
                lineHeight = 21.sp
            )
        }
        value?.let {
            Text(
                text = it,
                color = CalcMotColors.TextSecondary.copy(alpha = if (enabled) 1f else 0.55f),
                fontSize = 18.sp,
                modifier = Modifier.padding(end = 10.dp)
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = CalcMotColors.TextSecondary.copy(alpha = if (enabled) 1f else 0.35f),
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun SettingsThemeRow(
    selected: SettingsThemeChoice,
    onSelected: (SettingsThemeChoice) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 98.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
            imageVector = Icons.Outlined.Palette,
            contentDescription = null,
            tint = CalcMotColors.Success,
            modifier = Modifier.size(46.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 24.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = "Tema",
                color = CalcMotColors.TextPrimary,
                fontSize = 19.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Preferência visual do app",
                color = CalcMotColors.TextSecondary,
                fontSize = 16.sp,
                lineHeight = 21.sp
            )
        }
        }
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SettingsThemeChoice.entries.forEachIndexed { index, choice ->
                SegmentedButton(
                    selected = selected == choice,
                    onClick = { onSelected(choice) },
                    shape = SegmentedButtonDefaults.itemShape(index, SettingsThemeChoice.entries.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = CalcMotColors.PrimaryActionBlue,
                        activeContentColor = CalcMotColors.TextPrimary,
                        inactiveContainerColor = CalcMotColors.Surface.copy(alpha = 0.34f),
                        inactiveContentColor = CalcMotColors.TextSecondary
                    ),
                    icon = {}
                ) {
                    Text(text = choice.label, maxLines = 1, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(CalcMotColors.BorderSubtle.copy(alpha = 0.72f))
    )
}

@Composable
private fun SettingsBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        CalcMotColors.Success.copy(alpha = 0.055f),
                        CalcMotColors.AppBackground.copy(alpha = 0.82f),
                        CalcMotColors.AppBackground
                    )
                )
            )
    )
}

private enum class SettingsThemeChoice(val label: String) {
    SYSTEM("Sistema"),
    DARK("Escuro"),
    LIGHT("Claro")
}

private val OverlayPositionPreference.settingsLabel: String
    get() = when (this) {
        OverlayPositionPreference.HIGH -> "Topo"
        OverlayPositionPreference.MEDIUM -> "Centro"
        OverlayPositionPreference.LOW -> "Inferior"
    }

private fun OverlayPositionPreference.next(): OverlayPositionPreference {
    return when (this) {
        OverlayPositionPreference.HIGH -> OverlayPositionPreference.MEDIUM
        OverlayPositionPreference.MEDIUM -> OverlayPositionPreference.LOW
        OverlayPositionPreference.LOW -> OverlayPositionPreference.HIGH
    }
}

private enum class HomeDestination {
    START,
    FINANCE,
    SETTINGS,
    OVERLAY_POSITION,
    OVERLAY_THEME,
    HELP,
    PRIVACY,
    DIAGNOSTICS,
    FEEDBACK,
    FEEDBACK_SUCCESS
}

internal enum class HomeStatus(
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
