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
    }
}
