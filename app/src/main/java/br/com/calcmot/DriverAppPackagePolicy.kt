package br.com.calcmot

enum class PackageDecision {
    DRIVER_APP,
    ALLOWED_USER_APP,
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
        "org.mozilla.fenix",
        "com.brave.browser",
        "com.microsoft.emmx",
        "com.opera.browser",
        "com.sec.android.app.sbrowser",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.samsung.android.packageinstaller",
        "com.x8bit.bitwarden",
        "com.agilebits.onepassword",
        "com.lastpass.lpandroid",
        "com.google.android.apps.walletnfcrel",
        "com.samsung.android.spay",
        "com.paypal.android.p2pmobile",
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
        "com.mercadopago",
        "br.com.intermedium",
        "com.intermedium",
        "br.com.c6bank",
        "com.c6bank",
        "br.com.neon",
        "com.neon",
        "br.com.original",
        "br.com.bancopan",
        "br.com.banrisul",
        "br.com.sicredi",
        "br.com.sicoob",
        "com.btgpactual",
        "br.com.xp",
        "com.xpinc"
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
            else -> PackageDecision.ALLOWED_USER_APP
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
            PackageDecision.ALLOWED_USER_APP,
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
