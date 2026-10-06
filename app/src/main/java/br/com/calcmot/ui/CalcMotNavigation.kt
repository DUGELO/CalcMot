package br.com.calcmot.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.com.calcmot.AppPermissionState
import br.com.calcmot.AppSettings
import br.com.calcmot.ReadingPipelineRuntime

internal object CalcMotRoute {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val GOAL = "goal"
    const val SETTINGS = "settings"
    const val OVERLAY_POSITION = "overlay-position"
    const val OVERLAY_THEME = "overlay-theme"
    const val HELP = "help"
    const val PRIVACY = "privacy"
    const val FEEDBACK = "feedback"
    const val DIAGNOSTICS = "diagnostics"
    const val SECURITY_TOOLS = "security-tools"
    const val SECURITY_HUB = "security-hub"
    const val OPEN_DASHBOARD_FINANCE = "open-dashboard-finance"
    const val FINANCIAL_ACTIVITY = "financial-activity"
    const val FINANCIAL_STATEMENT = "financial-statement"
    const val FINANCIAL_INSIGHTS = "financial-insights"
    const val FINANCIAL_SOURCES = "financial-sources"
    const val FINANCIAL_DATA_QUALITY = "financial-data-quality"
    const val FINANCIAL_OFFER_DETAIL = "financial-offer/{offerId}"
    const val FINANCIAL_COSTS = "financial-costs"
    const val DAILY_GROSS_GOAL = "daily-gross-goal"
    const val FINANCIAL_REPORTS = "financial-reports"
    const val FINANCIAL_DATA = "financial-data"
    const val FINANCIAL_SESSIONS = "financial-sessions"
    const val FINANCIAL_SESSION_DETAIL = "financial-session/{sessionId}"

    fun financialOfferDetail(offerId: String) = "financial-offer/$offerId"
    fun financialSessionDetail(sessionId: String) = "financial-session/$sessionId"
}

@Composable
fun CalcMotNavHost(
    permissionState: AppPermissionState,
    onboardingCompleted: Boolean,
    onPermissionsRefresh: () -> Unit,
    onOnboardingCompleted: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    var monitoringEnabled by remember { mutableStateOf(AppSettings.isMonitoringEnabled(context)) }
    var financialImpactEnabled by remember { mutableStateOf(AppSettings.isFinancialImpactEnabled(context)) }
    var overlayPosition by remember { mutableStateOf(AppSettings.getOverlayPosition(context)) }
    var overlayTheme by remember { mutableStateOf(AppSettings.getOverlayTheme(context)) }
    var driverGoal by remember { mutableStateOf(AppSettings.getDriverGoal(context)) }
    var diagnosticsEnabled by remember { mutableStateOf(AppSettings.isDiagnosticsEnabled(context)) }
    var showFinancialHistoryDisclosure by remember { mutableStateOf(false) }
    val startDestination = remember {
        if (onboardingCompleted || permissionState.hasAccessibilityService) {
            CalcMotRoute.HOME
        } else {
            CalcMotRoute.ONBOARDING
        }
    }

    fun setMonitoringEnabled(enabled: Boolean) {
        AppSettings.setMonitoringEnabled(context, enabled)
        monitoringEnabled = enabled
    }

    fun setFinancialImpactEnabled(enabled: Boolean) {
        AppSettings.setFinancialImpactEnabled(context, enabled)
        financialImpactEnabled = enabled
    }

    fun setOverlayPosition(position: br.com.calcmot.OverlayPositionPreference) {
        AppSettings.setOverlayPosition(context, position)
        overlayPosition = position
    }

    fun setOverlayTheme(theme: br.com.calcmot.OverlayThemePreference) {
        AppSettings.setOverlayTheme(context, theme)
        overlayTheme = theme
    }

    fun navigate(route: String) {
        navController.navigate(route) { launchSingleTop = true }
    }

    LaunchedEffect(permissionState.hasAccessibilityService, currentBackStackEntry?.destination?.route) {
        if (permissionState.hasAccessibilityService) {
            if (!onboardingCompleted) onOnboardingCompleted()
            val currentRoute = currentBackStackEntry?.destination?.route
            if (currentRoute == CalcMotRoute.ONBOARDING ||
                (currentRoute == CalcMotRoute.PRIVACY && startDestination == CalcMotRoute.ONBOARDING)
            ) {
                navController.navigate(CalcMotRoute.HOME) {
                    popUpTo(CalcMotRoute.ONBOARDING) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    LaunchedEffect(onboardingCompleted, permissionState.hasAccessibilityService) {
        if ((onboardingCompleted || permissionState.hasAccessibilityService) &&
            !AppSettings.hasFinancialHistoryDisclosureDecision(context)
        ) {
            showFinancialHistoryDisclosure = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable(CalcMotRoute.ONBOARDING) {
            OnboardingScreen(
                permissionState = permissionState,
                onPermissionsRefresh = onPermissionsRefresh,
                onOpenPrivacy = { navigate(CalcMotRoute.PRIVACY) }
            )
        }

        composable(CalcMotRoute.HOME) {
            HomeScreen(
                permissionState = permissionState,
                monitoringEnabled = monitoringEnabled,
                driverGoal = driverGoal,
                onMonitoringChange = ::setMonitoringEnabled,
                onOpenAccessibility = { openAccessibilitySettings(context) },
                onPermissionsRefresh = onPermissionsRefresh,
                onOpenGoal = { navigate(CalcMotRoute.GOAL) },
                onOpenSettings = { navigate(CalcMotRoute.SETTINGS) },
                onOpenHelp = { navigate(CalcMotRoute.HELP) },
                onOpenPrivacy = { navigate(CalcMotRoute.PRIVACY) },
                onOpenFeedback = { navigate(CalcMotRoute.FEEDBACK) },
                onOpenDashboardFinance = { navigate(CalcMotRoute.OPEN_DASHBOARD_FINANCE) },
                onOpenTools = { navigate(CalcMotRoute.SECURITY_TOOLS) },
                diagnosticsEnabled = diagnosticsEnabled,
                onUnlockDiagnostics = {
                    diagnosticsEnabled = true
                    navigate(CalcMotRoute.DIAGNOSTICS)
                },
                onOpenDiagnostics = { navigate(CalcMotRoute.DIAGNOSTICS) }
            )
        }

        composable(CalcMotRoute.GOAL) {
            FinanceScreen(
                modifier = Modifier,
                onBack = { navController.popBackStack() },
                onGoalSaved = { driverGoal = it }
            )
        }

        composable(CalcMotRoute.SETTINGS) {
            SettingsScreen(
                modifier = Modifier,
                monitoringEnabled = monitoringEnabled,
                financialImpactEnabled = financialImpactEnabled,
                permissionState = permissionState,
                overlayPosition = overlayPosition,
                overlayTheme = overlayTheme,
                onBack = { navController.popBackStack() },
                onMonitoringChange = ::setMonitoringEnabled,
                onFinancialImpactChange = ::setFinancialImpactEnabled,
                onOpenAccessibility = { openAccessibilitySettings(context) },
                onOpenOverlayPosition = { navigate(CalcMotRoute.OVERLAY_POSITION) },
                onOpenOverlayTheme = { navigate(CalcMotRoute.OVERLAY_THEME) },
                onOpenPrivacy = { navigate(CalcMotRoute.PRIVACY) },
                onOpenHelp = { navigate(CalcMotRoute.HELP) },
                diagnosticsEnabled = diagnosticsEnabled,
                onOpenDiagnostics = { navigate(CalcMotRoute.DIAGNOSTICS) }
            )
        }

        composable(CalcMotRoute.OVERLAY_POSITION) {
            OverlayPositionScreen(
                currentPosition = overlayPosition,
                onBack = { navController.popBackStack() },
                onSave = { position ->
                    setOverlayPosition(position)
                    navController.popBackStack()
                }
            )
        }

        composable(CalcMotRoute.OVERLAY_THEME) {
            OverlayThemeScreen(
                currentTheme = overlayTheme,
                onBack = { navController.popBackStack() },
                onSave = { theme ->
                    setOverlayTheme(theme)
                    navController.popBackStack()
                }
            )
        }

        composable(CalcMotRoute.HELP) {
            HelpScreen(
                modifier = Modifier,
                onBack = { navController.popBackStack() },
                onOpenPrivacy = { navigate(CalcMotRoute.PRIVACY) },
                onSupport = { uriHandler.openUri("mailto:$CALCMOT_SUPPORT_EMAIL") }
            )
        }

        composable(CalcMotRoute.PRIVACY) {
            PrivacyPolicyScreen(
                modifier = Modifier,
                onBack = { navController.popBackStack() },
                onSupport = { uriHandler.openUri(CALCMOT_PRIVACY_POLICY_URL) }
            )
        }

        composable(CalcMotRoute.FEEDBACK) {
            FeedbackScreen(
                onBack = { navController.popBackStack() },
                onSubmit = { draft ->
                    FeedbackEmailLauncher.launch(
                        context = context,
                        draft = draft,
                        accessibilityEnabled = permissionState.hasAccessibilityService,
                        monitoringEnabled = monitoringEnabled
                    )
                }
            )
        }

        composable(CalcMotRoute.DIAGNOSTICS) {
            ReadingDiagnosticsScreen(
                accessibilityActive = permissionState.hasAccessibilityService,
                batteryOptimization = ReadingPipelineRuntime.batteryOptimizationLabel(context),
                diagnosticsEnabled = diagnosticsEnabled,
                onBack = { navController.popBackStack() },
                onDiagnosticsEnabledChange = { enabled ->
                    AppSettings.setDiagnosticsEnabled(context, enabled)
                    diagnosticsEnabled = enabled
                    if (!enabled) navController.popBackStack()
                },
                onCopy = {
                    val text = ReadingPipelineRuntime.diagnosticsText(
                        context,
                        permissionState.hasAccessibilityService
                    )
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Diagnóstico CalcMot", text))
                    Toast.makeText(context, "Diagnóstico copiado", Toast.LENGTH_SHORT).show()
                },
                onRestart = {
                    val restarted = ReadingPipelineRuntime.manualRestart(context)
                    Toast.makeText(
                        context,
                        if (restarted) "Leitura reiniciada" else "Ative a acessibilidade para reiniciar",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }

        composable(CalcMotRoute.SECURITY_TOOLS) {
            br.com.calcmot.securityrecording.ui.SecurityToolsRoute(
                onBack = { navController.popBackStack() },
                onOpenSecurityHub = { navigate(CalcMotRoute.SECURITY_HUB) }
            )
        }

        composable(CalcMotRoute.SECURITY_HUB) {
            br.com.calcmot.securityrecording.ui.SecurityRecordingHubRoute(
                onBack = { navController.popBackStack() },
                onConfigureRecording = {},
                onOpenRecordings = {}
            )
        }

        composable(CalcMotRoute.OPEN_DASHBOARD_FINANCE) {
            OpenDashboardFinanceRoute(
                onBack = { navController.popBackStack() },
                onOpenActivity = { navigate(CalcMotRoute.FINANCIAL_ACTIVITY) },
                onOpenInsights = { navigate(CalcMotRoute.FINANCIAL_INSIGHTS) },
                onOpenSources = { navigate(CalcMotRoute.FINANCIAL_SOURCES) },
                onOpenReports = { navigate(CalcMotRoute.FINANCIAL_REPORTS) }
            )
        }

        composable(CalcMotRoute.FINANCIAL_ACTIVITY) {
            FinancialActivityRoute(
                onBack = { navController.popBackStack() },
                onOpenStatement = { navigate(CalcMotRoute.FINANCIAL_STATEMENT) },
                onOpenCosts = { navigate(CalcMotRoute.FINANCIAL_COSTS) },
                onOpenSessions = { navigate(CalcMotRoute.FINANCIAL_SESSIONS) }
            )
        }

        composable(CalcMotRoute.FINANCIAL_STATEMENT) {
            FinancialStatementRoute(
                onBack = { navController.popBackStack() },
                onOpenOffer = { navigate(CalcMotRoute.financialOfferDetail(it)) }
            )
        }

        composable(CalcMotRoute.FINANCIAL_INSIGHTS) {
            FinancialInsightsRoute(
                onBack = { navController.popBackStack() },
                onOpenDailyGoal = { navigate(CalcMotRoute.DAILY_GROSS_GOAL) }
            )
        }

        composable(CalcMotRoute.FINANCIAL_SOURCES) {
            FinancialSourcesRoute(
                onBack = { navController.popBackStack() },
                onOpenDataQuality = { navigate(CalcMotRoute.FINANCIAL_DATA_QUALITY) },
                onOpenDataManagement = { navigate(CalcMotRoute.FINANCIAL_DATA) }
            )
        }

        composable(CalcMotRoute.FINANCIAL_DATA_QUALITY) {
            FinancialDataQualityRoute(
                onBack = { navController.popBackStack() },
                onOpenDataManagement = { navigate(CalcMotRoute.FINANCIAL_DATA) }
            )
        }

        composable(CalcMotRoute.FINANCIAL_OFFER_DETAIL) { entry ->
            OfferCorrectionRoute(
                offerId = entry.arguments?.getString("offerId").orEmpty(),
                onBack = { navController.popBackStack() }
            )
        }

        composable(CalcMotRoute.FINANCIAL_COSTS) {
            FinancialCostsRoute(onBack = { navController.popBackStack() })
        }

        composable(CalcMotRoute.DAILY_GROSS_GOAL) {
            DailyGrossGoalRoute(onBack = { navController.popBackStack() })
        }

        composable(CalcMotRoute.FINANCIAL_REPORTS) {
            FinancialReportsRoute(onBack = { navController.popBackStack() })
        }

        composable(CalcMotRoute.FINANCIAL_DATA) {
            FinancialDataManagementRoute(onBack = { navController.popBackStack() })
        }

        composable(CalcMotRoute.FINANCIAL_SESSIONS) {
            FinancialSessionsRoute(
                onBack = { navController.popBackStack() },
                onOpenSession = { navigate(CalcMotRoute.financialSessionDetail(it)) }
            )
        }

        composable(CalcMotRoute.FINANCIAL_SESSION_DETAIL) { entry ->
            FinancialSessionDetailRoute(
                sessionId = entry.arguments?.getString("sessionId").orEmpty(),
                onBack = { navController.popBackStack() }
            )
        }
    }

    if (showFinancialHistoryDisclosure) {
        br.com.calcmot.ui.design.components.CalcMotFeedbackAlertDialog(
            title = "Criar seu histórico financeiro?",
            message = "O CalcMot pode guardar neste aparelho as informações confirmadas das ofertas. Não salva imagens, endereços ou passageiros. Você pode pausar ou apagar tudo quando quiser.",
            tone = br.com.calcmot.ui.design.components.CalcMotFeedbackTone.INFO,
            primaryAction = br.com.calcmot.ui.design.components.CalcMotFeedbackAction(
                label = "Aceitar e ativar",
                onClick = {
                    AppSettings.setFinancialHistoryConsent(context, true)
                    showFinancialHistoryDisclosure = false
                    br.com.calcmot.telemetry.TelemetryProvider.analytics.track(
                        br.com.calcmot.telemetry.AnalyticsEvents.FINANCIAL_HISTORY_CONSENT,
                        mapOf(
                            br.com.calcmot.telemetry.AnalyticsParams.REASON to "active",
                            br.com.calcmot.telemetry.AnalyticsParams.RECORD_TYPE to "history"
                        )
                    )
                }
            ),
            secondaryAction = br.com.calcmot.ui.design.components.CalcMotFeedbackAction(
                label = "Agora não",
                onClick = {
                    AppSettings.setFinancialHistoryConsent(context, false)
                    showFinancialHistoryDisclosure = false
                },
                style = br.com.calcmot.ui.design.components.CalcMotFeedbackActionStyle.TEXT
            ),
            onDismissRequest = {}
        )
    }
}
