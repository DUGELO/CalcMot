package br.com.calcmot.ui

import android.widget.Toast
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.calcmot.growth.GrowthShareStore
import br.com.calcmot.growth.GrowthShareNotification
import br.com.calcmot.growth.GrowthShareSurface
import br.com.calcmot.growth.PlayReviewStore
import br.com.calcmot.growth.PlayRatingLauncher
import android.app.Activity
import br.com.calcmot.AppPermissionState
import br.com.calcmot.AppSettings
import br.com.calcmot.DriverAppLauncher
import br.com.calcmot.ReadingPipelineRuntime
import br.com.calcmot.model.DriverGoal
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    permissionState: AppPermissionState,
    onPermissionsRefresh: () -> Unit
) {
    val context = LocalContext.current
    var monitoringEnabled by remember { mutableStateOf(AppSettings.isMonitoringEnabled(context)) }

    HomeScreen(
        permissionState = permissionState,
        monitoringEnabled = monitoringEnabled,
        driverGoal = AppSettings.getDriverGoal(context),
        onMonitoringChange = { enabled ->
            AppSettings.setMonitoringEnabled(context, enabled)
            monitoringEnabled = enabled
        },
        onOpenAccessibility = { openAccessibilitySettings(context) },
        onPermissionsRefresh = onPermissionsRefresh
    )
}

@Composable
internal fun HomeScreen(
    permissionState: AppPermissionState,
    monitoringEnabled: Boolean,
    driverGoal: DriverGoal,
    onMonitoringChange: (Boolean) -> Unit,
    onOpenAccessibility: () -> Unit,
    onPermissionsRefresh: () -> Unit,
    onOpenGoal: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenHelp: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenFeedback: () -> Unit = {},
    onOpenTools: () -> Unit = {},
    diagnosticsEnabled: Boolean = false,
    onUnlockDiagnostics: () -> Unit = {},
    onOpenDiagnostics: () -> Unit = {}
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var growthPromptVisible by remember { mutableStateOf(false) }
    var reviewRequestRunning by remember { mutableStateOf(false) }
    var sharePromptClaimedThisVisit by remember { mutableStateOf(false) }
    var growthPromptProactive by remember { mutableStateOf(false) }
    var shareStreakDays by remember { mutableStateOf(0) }
    val notificationRequestPending = GrowthShareInviteRequests.pending.value
    LaunchedEffect(notificationRequestPending) {
        if (notificationRequestPending && GrowthShareInviteRequests.consumePending()) {
            shareStreakDays = GrowthShareStore.currentStreak(context)
            growthPromptProactive = false
            growthPromptVisible = true
        }
    }
    var versionTapCount by remember { mutableStateOf(0) }
    var lastVersionTapAt by remember { mutableStateOf(0L) }
    val status = when {
        !permissionState.hasAccessibilityService -> HomeStatus.PERMISSION_PENDING
        monitoringEnabled -> HomeStatus.READY
        else -> HomeStatus.PAUSED
    }

    fun checkGrowthPrompt(): Boolean {
        if (growthPromptVisible || sharePromptClaimedThisVisit ||
            GrowthShareInviteRequests.pending.value
        ) return true
        if (status != HomeStatus.READY) return false
        val surface = GrowthShareStore.claimPrompt(context, GrowthShareNotification.canNotify(context))
        if (surface != null) sharePromptClaimedThisVisit = true
        when (surface) {
            GrowthShareSurface.CARD -> {
                shareStreakDays = GrowthShareStore.currentStreak(context)
                growthPromptProactive = true
                growthPromptVisible = true
            }
            GrowthShareSurface.NOTIFICATION -> if (!GrowthShareNotification.show(context)) {
                shareStreakDays = GrowthShareStore.currentStreak(context)
                growthPromptProactive = true
                growthPromptVisible = true
            }
            null -> Unit
        }
        return surface != null
    }

    fun maybeRequestReview() {
        if (status != HomeStatus.READY || growthPromptVisible || reviewRequestRunning ||
            drawerState.currentValue != DrawerValue.Closed
        ) return
        val activity = context as? Activity ?: return
        if (!PlayReviewStore.claimAutomaticPrompt(context, GrowthShareStore.currentStreak(context))) return
        reviewRequestRunning = true
        PlayRatingLauncher.requestNativeReview(activity) {
            reviewRequestRunning = false
        }
    }

    LaunchedEffect(status) {
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            if (!checkGrowthPrompt()) maybeRequestReview()
        } else {
            growthPromptVisible = false
        }
    }
    DisposableEffect(lifecycleOwner, status) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (!checkGrowthPrompt()) maybeRequestReview()
                }
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    growthPromptVisible = false
                    sharePromptClaimedThisVisit = false
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun openShareInvite() {
        shareStreakDays = GrowthShareStore.currentStreak(context)
        growthPromptProactive = false
        growthPromptVisible = true
    }

    fun dismissShareInvite() {
        growthPromptVisible = false
        if (growthPromptProactive) GrowthShareStore.dismiss(context)
        growthPromptProactive = false
    }

    fun confirmShareInvite() {
        growthPromptVisible = false
        GrowthShareStore.shared(context)
        GrowthShareNotification.cancel(context)
        growthPromptProactive = false
        CalcMotSharing.share(context, "home")
    }

    fun closeDrawerAnd(action: () -> Unit) {
        scope.launch {
            drawerState.close()
            action()
        }
    }

    fun handleVersionTap() {
        val now = android.os.SystemClock.elapsedRealtime()
        versionTapCount = if (now - lastVersionTapAt <= 1_500L) versionTapCount + 1 else 1
        lastVersionTapAt = now
        if (versionTapCount >= 5) {
            versionTapCount = 0
            AppSettings.setDiagnosticsEnabled(context, true)
            Toast.makeText(context, "Diagnóstico ativado", Toast.LENGTH_SHORT).show()
            closeDrawerAnd(onUnlockDiagnostics)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                status = status,
                onHome = { closeDrawerAnd {} },
                onGoal = { closeDrawerAnd(onOpenGoal) },
                onSettings = { closeDrawerAnd(onOpenSettings) },
                onHelp = { closeDrawerAnd(onOpenHelp) },
                onPrivacy = { closeDrawerAnd(onOpenPrivacy) },
                onFeedback = { closeDrawerAnd(onOpenFeedback) },
                onTools = { closeDrawerAnd(onOpenTools) },
                onShare = { closeDrawerAnd(::openShareInvite) },
                onRate = { closeDrawerAnd { PlayRatingLauncher.openPlayStore(context) } },
                diagnosticsEnabled = diagnosticsEnabled,
                onVersionTap = ::handleVersionTap,
                onDiagnostics = { closeDrawerAnd(onOpenDiagnostics) }
            )
        }
    ) {
        HomeContent(
            modifier = Modifier,
            status = status,
            permissionState = permissionState,
            driverGoalOverride = driverGoal,
            onMonitoringChange = onMonitoringChange,
            onOpenAccessibility = onOpenAccessibility,
            onOpenMenu = { scope.launch { drawerState.open() } },
            onPermissionsRefresh = onPermissionsRefresh,
            onOpenDriverApp = { driverApp ->
                if (DriverAppLauncher.launch(context, driverApp) == null) {
                    Toast.makeText(
                        context,
                        "${driverApp.displayName} nao esta instalado.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onEditGoal = onOpenGoal,
            onOpenSettings = onOpenSettings,
            onOpenHelp = onOpenHelp,
            onOpenPrivacy = onOpenPrivacy,
            onShare = ::openShareInvite,
            onRestartReading = {
                val restarted = ReadingPipelineRuntime.manualRestart(context)
                Toast.makeText(
                    context,
                    if (restarted) "Leitura reiniciada" else "Ative a acessibilidade para reiniciar",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
        if (growthPromptVisible) {
            GrowthShareBottomSheet(
                streakDays = shareStreakDays,
                onShare = ::confirmShareInvite,
                onDismiss = ::dismissShareInvite
            )
        }
    }
}
