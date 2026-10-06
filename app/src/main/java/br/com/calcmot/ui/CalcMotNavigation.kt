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
import br.com.calcmot.analytics.ClarityIntegration
import br.com.calcmot.analytics.ClarityScreenTracking
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
    const val SECURITY_CONFIGURE = "security-configure"
    const val SECURITY_ACTIVE = "security-active"
    const val SECURITY_LIBRARY = "security-library"
    const val SECURITY_PLAYER = "security-player/{sessionId}"
    fun securityPlayer(sessionId: String) = "security-player/$sessionId"
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
    ClarityScreenTracking("app", currentBackStackEntry?.destination?.route)
    var monitoringEnabled by remember { mutableStateOf(AppSettings.isMonitoringEnabled(context)) }
    var financialImpactEnabled by remember { mutableStateOf(AppSettings.isFinancialImpactEnabled(context)) }
    var overlayPosition by remember { mutableStateOf(AppSettings.getOverlayPosition(context)) }
    var overlayTheme by remember { mutableStateOf(AppSettings.getOverlayTheme(context)) }
    var driverGoal by remember { mutableStateOf(AppSettings.getDriverGoal(context)) }
    var diagnosticsEnabled by remember { mutableStateOf(AppSettings.isDiagnosticsEnabled(context)) }
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
        ClarityIntegration.track(if (enabled) "monitoring_enabled" else "monitoring_disabled")
    }

    fun setFinancialImpactEnabled(enabled: Boolean) {
        AppSettings.setFinancialImpactEnabled(context, enabled)
        financialImpactEnabled = enabled
        ClarityIntegration.track(if (enabled) "financial_impact_enabled" else "financial_impact_disabled")
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
                onGoalSaved = { driverGoal = it; ClarityIntegration.track("financial_goal_saved") }
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

        // The recording entry remains passive until the driver deliberately opens configuration.
        composable(CalcMotRoute.SECURITY_TOOLS) {
            br.com.calcmot.securityrecording.ui.SecurityToolsRoute(
                onBack = { navController.popBackStack() },
                onOpenHub = { context.startActivity(android.content.Intent(context, br.com.calcmot.securityrecording.ui.SecurityRecordingActivity::class.java)) }
            )
        }

        composable(CalcMotRoute.SECURITY_HUB) {
            br.com.calcmot.securityrecording.ui.SecurityRecordingHubRoute(
                onBack = { navController.popBackStack() },
                onConfigure = { navigate(CalcMotRoute.SECURITY_CONFIGURE) },
                onLibrary = { navigate(CalcMotRoute.SECURITY_LIBRARY) }
            )
        }

        composable(CalcMotRoute.SECURITY_CONFIGURE) {
            br.com.calcmot.securityrecording.ui.SecurityRecordingConfigurationRoute(
                onBack = { navController.popBackStack() },
                onRecordingRequested = { navigate(CalcMotRoute.SECURITY_ACTIVE) }
            )
        }

        composable(CalcMotRoute.SECURITY_ACTIVE) {
            br.com.calcmot.securityrecording.ui.SecurityRecordingActiveRoute(
                onBack = { navController.popBackStack() },
                onOpenLibrary = { navigate(CalcMotRoute.SECURITY_LIBRARY) }
            )
        }

        composable(CalcMotRoute.SECURITY_LIBRARY) {
            br.com.calcmot.securityrecording.ui.SecurityRecordingLibraryRoute(
                onBack = { navController.popBackStack() },
                onOpenSession = { sessionId -> navigate(CalcMotRoute.securityPlayer(sessionId)) }
            )
        }

        composable(CalcMotRoute.SECURITY_PLAYER) { entry ->
            val sessionId = entry.arguments?.getString("sessionId") ?: return@composable
            br.com.calcmot.securityrecording.ui.SecurityRecordingPlayerRoute(
                sessionId = sessionId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
