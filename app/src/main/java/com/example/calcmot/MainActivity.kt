package com.example.calcmot

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.calcmot.accessibility.UberAccessibilityService
import com.example.calcmot.ui.theme.MetricaTheme
import com.example.calcmot.ui.OnboardingScreen
import com.example.calcmot.ui.HomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MetricaTheme {
                val context = LocalContext.current
                var hasAllPermissions by remember {
                    mutableStateOf(hasRequiredPermissions(context))
                }

                if (hasAllPermissions) {
                    HomeScreen()
                } else {
                    OnboardingScreen {
                        hasAllPermissions = hasRequiredPermissions(context)
                    }
                }
            }
        }
    }
}

fun hasRequiredPermissions(context: Context): Boolean {
    return hasOverlayPermission(context) && isAccessibilityServiceEnabled(context)
}

fun hasOverlayPermission(context: Context): Boolean {
    return Settings.canDrawOverlays(context)
}

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val accessibilityService = ComponentName(context, UberAccessibilityService::class.java)
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    )
    return enabledServices?.contains(accessibilityService.flattenToString()) == true
}
