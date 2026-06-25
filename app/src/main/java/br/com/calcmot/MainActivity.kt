package br.com.calcmot

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import br.com.calcmot.accessibility.UberAccessibilityService
import br.com.calcmot.ui.HomeScreen
import br.com.calcmot.ui.OnboardingScreen
import br.com.calcmot.ui.theme.MetricaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MetricaTheme {
                CalcMotApp()
            }
        }
    }
}

@Composable
private fun CalcMotApp() {
    val context = LocalContext.current
    var permissionState by remember {
        mutableStateOf(readAppPermissionState(context))
    }

    fun refreshPermissions() {
        permissionState = readAppPermissionState(context)
    }

    val lifecycleOwner = context as? LifecycleOwner
    DisposableEffect(lifecycleOwner) {
        if (lifecycleOwner == null) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    refreshPermissions()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    }

    CalcMotAppContent(
        permissionState = permissionState,
        onPermissionsRefresh = ::refreshPermissions
    )
}

@Composable
fun CalcMotAppContent(
    permissionState: AppPermissionState,
    onPermissionsRefresh: () -> Unit
) {
    if (permissionState.hasAllRequiredPermissions) {
        HomeScreen(
            permissionState = permissionState,
            onPermissionsRefresh = onPermissionsRefresh
        )
    } else {
        OnboardingScreen(
            permissionState = permissionState,
            onPermissionsRefresh = onPermissionsRefresh
        )
    }
}

data class AppPermissionState(
    val hasAccessibilityService: Boolean
) {
    val hasAllRequiredPermissions: Boolean
        get() = hasAccessibilityService
}

fun readAppPermissionState(context: Context): AppPermissionState {
    return AppPermissionState(
        hasAccessibilityService = isAccessibilityServiceEnabled(context)
    )
}

fun hasRequiredPermissions(context: Context): Boolean {
    return readAppPermissionState(context).hasAllRequiredPermissions
}

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val accessibilityService = ComponentName(context, UberAccessibilityService::class.java)
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    val splitter = TextUtils.SimpleStringSplitter(':')
    splitter.setString(enabledServices)
    while (splitter.hasNext()) {
        val enabledService = ComponentName.unflattenFromString(splitter.next())
        if (enabledService == accessibilityService) return true
    }
    return false
}
