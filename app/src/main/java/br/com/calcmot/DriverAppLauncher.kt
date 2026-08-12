package br.com.calcmot

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

data class InstalledDriverApp(
    val driverApp: DriverApp,
    val packageName: String,
    val launchIntent: Intent
)

object DriverAppLauncher {

    fun installedApps(context: Context): List<InstalledDriverApp> {
        return DriverApp.supported.mapNotNull { driverApp ->
            driverApp.packageNames.firstNotNullOfOrNull { packageName ->
                runCatching {
                    context.packageManager.getLaunchIntentForPackage(packageName)
                }.onFailure { error ->
                    Log.w(TAG, "Unable to resolve driver app package=$packageName", error)
                }.getOrNull()?.let { launchIntent ->
                    InstalledDriverApp(
                        driverApp = driverApp,
                        packageName = packageName,
                        launchIntent = launchIntent
                    )
                }
            }
        }
    }

    fun resolve(
        installedApps: List<InstalledDriverApp>,
        driverApp: DriverApp
    ): InstalledDriverApp? {
        return installedApps.firstOrNull { it.driverApp == driverApp }
    }

    fun launch(context: Context, driverApp: DriverApp): DriverApp? {
        val resolved = resolve(
            installedApps = installedApps(context),
            driverApp = driverApp
        ) ?: return null

        ReadingPipelineRuntime.selectPlatform(context, resolved.driverApp)
        resolved.launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val launched = runCatching {
            context.startActivity(resolved.launchIntent)
        }.onFailure { error ->
            Log.e(TAG, "Unable to launch driver app package=${resolved.packageName}", error)
        }.isSuccess
        if (!launched) return null

        AppSettings.setLastDriverApp(context, resolved.driverApp)
        return resolved.driverApp
    }

    fun isInstalled(packageManager: PackageManager, packageName: String): Boolean {
        return runCatching {
            packageManager.getLaunchIntentForPackage(packageName) != null
        }.getOrDefault(false)
    }

    private const val TAG = "DriverAppLauncher"
}
