package br.com.calcmot.ui

import android.widget.Toast
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import br.com.calcmot.AppPermissionState
import br.com.calcmot.AppSettings
import br.com.calcmot.DriverAppLauncher
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
    onOpenFeedback: () -> Unit = {}
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val status = when {
        !permissionState.hasAccessibilityService -> HomeStatus.PERMISSION_PENDING
        monitoringEnabled -> HomeStatus.READY
        else -> HomeStatus.PAUSED
    }

    fun closeDrawerAnd(action: () -> Unit) {
        scope.launch {
            drawerState.close()
            action()
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
                onFeedback = { closeDrawerAnd(onOpenFeedback) }
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
            onOpenPrivacy = onOpenPrivacy
        )
    }
}
