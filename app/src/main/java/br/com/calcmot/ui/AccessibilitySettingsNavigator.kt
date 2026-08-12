package br.com.calcmot.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import br.com.calcmot.accessibility.UberAccessibilityService

private const val SETTINGS_FRAGMENT_ARGS = ":settings:show_fragment_args"
private const val SETTINGS_FRAGMENT_ARGS_KEY = ":settings:fragment_args_key"

fun openAccessibilitySettings(context: Context): Boolean {
    val intent = accessibilitySettingsIntent(context)
    if (context !is Activity) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return runCatching {
        context.startActivity(intent)
    }.onFailure { error ->
        Log.e("AccessibilityNav", "Unable to open accessibility settings", error)
    }.isSuccess
}

fun accessibilitySettingsIntent(context: Context): Intent {
    val serviceComponent = ComponentName(context, UberAccessibilityService::class.java)
    val serviceKey = serviceComponent.flattenToString()
    val args = Bundle().apply {
        putString(SETTINGS_FRAGMENT_ARGS_KEY, serviceKey)
        putString(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }

    return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        putExtra(SETTINGS_FRAGMENT_ARGS_KEY, serviceKey)
        putExtra(SETTINGS_FRAGMENT_ARGS, args)
    }
}
