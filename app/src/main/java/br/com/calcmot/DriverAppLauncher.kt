package br.com.calcmot

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

data class InstalledDriverApp(
    val driverApp: DriverApp,
    val packageName: String,
    val launchIntent: Intent
)

object DriverAppLauncher {

    fun installedApps(context: Context): List<InstalledDriverApp> {
        return DriverApp.supported.mapNotNull { driverApp ->
            driverApp.packageNames.firstNotNullOfOrNull { packageName ->
                context.packageManager.getLaunchIntentForPackage(packageName)?.let { launchIntent ->
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
        preferredDriverApp: DriverApp
    ): InstalledDriverApp? {
        return installedApps.firstOrNull { it.driverApp == preferredDriverApp }
            ?: installedApps.firstOrNull()
    }

    fun launchPreferred(context: Context): DriverApp? {
        val resolved = resolve(
            installedApps = installedApps(context),
            preferredDriverApp = AppSettings.getLastDriverApp(context)
        ) ?: return null

        resolved.launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(resolved.launchIntent)
        AppSettings.setLastDriverApp(context, resolved.driverApp)
        return resolved.driverApp
    }

    fun isInstalled(packageManager: PackageManager, packageName: String): Boolean {
        return packageManager.getLaunchIntentForPackage(packageName) != null
    }
}
