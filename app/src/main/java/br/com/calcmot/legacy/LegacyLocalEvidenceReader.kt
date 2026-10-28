package br.com.calcmot.legacy

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat

/** Explicit future migration preparation, off the offer path. No network, writes,
 * identity creation, Room scans, recording/finance contents or eligibility decisions.
 * Intentionally not wired into Application, services or navigation in S05.
 */
class LegacyLocalEvidenceReader(context: Context) {
    private val appContext = context.applicationContext

    fun read(): LegacyLocalEvidence {
        val info = runCatching {
            appContext.packageManager.getPackageInfo(appContext.packageName, 0)
        }.getOrNull()
        val onboarding = runCatching {
            val prefs = appContext.getSharedPreferences("calcmot_settings", Context.MODE_PRIVATE)
            if (prefs.contains("onboarding_completed")) prefs.getBoolean("onboarding_completed", false) else null
        }.getOrNull()
        return LegacyLocalEvidence(
            firstInstallTimeMs = info?.firstInstallTime?.takeIf { it > 0 },
            lastUpdateTimeMs = info?.lastUpdateTime?.takeIf { it > 0 },
            currentVersionCode = info?.let(PackageInfoCompat::getLongVersionCode),
            storedOnboardingCompleted = onboarding
        )
    }
}
