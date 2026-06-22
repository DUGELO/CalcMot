package br.com.calcmot

enum class PackageDecision {
    DRIVER_APP,
    OWN_APP,
    UNKNOWN,
    TRANSIENT_SYSTEM,
    BLOCKED_USER_APP
}

object DriverAppPackagePolicy {
    const val OWN_PACKAGE = "br.com.calcmot"

    val allowedDriverPackages: Set<String> = DriverApp.supported
        .flatMapTo(linkedSetOf()) { it.packageNames }

    private val transientSystemPackages: Set<String> = setOf(
        "android",
        "com.android.systemui",
        "com.samsung.android.app.smartcapture"
    )

    private val criticalUserPackages: Set<String> = setOf(
        "com.android.settings",
        "com.android.chrome",
        "com.google.android.apps.chrome",
        "com.google.android.webview",
        "org.mozilla.firefox",
        "com.microsoft.emmx",
        "com.opera.browser",
        "com.whatsapp",
        "com.whatsapp.w4b",
        "com.google.android.apps.photos",
        "com.sec.android.gallery3d",
        "com.samsung.android.gallery3d",
        "com.miui.gallery",
        "com.nu.production",
        "br.com.digio",
        "br.com.digio.uber",
        "br.com.bb.android",
        "br.com.caixa.tem",
        "br.gov.caixa.tem",
        "br.com.santander.way",
        "br.com.santander.app"
    )

    private val criticalUserPackagePrefixes: Set<String> = setOf(
        "br.com.itau",
        "com.itau",
        "br.com.bradesco",
        "com.bradesco",
        "br.com.santander",
        "com.santander",
        "br.com.bb.",
        "br.gov.caixa",
        "com.picpay",
        "br.com.uol.ps",
        "com.mercadopago"
    )

    fun classify(packageName: CharSequence?): PackageDecision {
        val normalized = normalize(packageName) ?: return PackageDecision.UNKNOWN
        return when {
            normalized.equals("unknown", ignoreCase = true) -> PackageDecision.UNKNOWN
            normalized == OWN_PACKAGE -> PackageDecision.OWN_APP
            normalized in allowedDriverPackages -> PackageDecision.DRIVER_APP
            normalized in transientSystemPackages -> PackageDecision.TRANSIENT_SYSTEM
            normalized in criticalUserPackages -> PackageDecision.BLOCKED_USER_APP
            criticalUserPackagePrefixes.any { normalized.startsWith(it) } -> PackageDecision.BLOCKED_USER_APP
            else -> PackageDecision.BLOCKED_USER_APP
        }
    }

    fun isAllowedDriverPackage(packageName: CharSequence?): Boolean {
        return isDriverPackage(packageName)
    }

    fun isDriverPackage(packageName: CharSequence?): Boolean {
        return classify(packageName) == PackageDecision.DRIVER_APP
    }

    fun driverAppForPackage(packageName: CharSequence?): DriverApp {
        return DriverApp.fromPackage(packageName)
    }

    fun packagesFor(driverApp: DriverApp): List<String> {
        return driverApp.packageNames
    }

    fun isCaptureBlockedUserApp(packageName: CharSequence?): Boolean {
        return when (classify(packageName)) {
            PackageDecision.BLOCKED_USER_APP -> true
            PackageDecision.DRIVER_APP,
            PackageDecision.OWN_APP,
            PackageDecision.UNKNOWN,
            PackageDecision.TRANSIENT_SYSTEM -> false
        }
    }

    fun isCriticalUserApp(packageName: CharSequence?): Boolean {
        return isBlockedUserApp(packageName)
    }

    fun isBlockedUserApp(packageName: CharSequence?): Boolean {
        return classify(packageName) == PackageDecision.BLOCKED_USER_APP
    }

    fun isUnknownPackage(packageName: CharSequence?): Boolean {
        return classify(packageName) == PackageDecision.UNKNOWN
    }

    fun describe(packageName: CharSequence?): String {
        return normalize(packageName) ?: "unknown"
    }

    fun normalize(packageName: CharSequence?): String? {
        return packageName
            ?.toString()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }
}
