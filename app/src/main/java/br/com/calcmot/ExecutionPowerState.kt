package br.com.calcmot

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Own-app, on-demand observations. Unknown values are never interpreted as permission. */
object ExecutionPowerState {
    enum class Trigger { APP_RESUMED, SERVICE_CONNECTED, READING_START, POWER_CHANGED, DIAGNOSTICS }
    data class Snapshot(
        val dozeExempt: Boolean? = null,
        val backgroundRestricted: Boolean? = null,
        val powerSave: Boolean? = null,
        val charging: Boolean? = null,
        val standbyBucket: Int? = null,
        val manufacturer: String = "unknown",
        val model: String = "unknown",
        val androidApi: Int = 0,
        val systemBuild: String = "unknown"
    ) {
        fun optimizationLabel(): String = when (dozeExempt) {
            true -> "Isento de Doze; OEM não verificado"
            false -> "Otimização Doze ativa"
            null -> "Otimização desconhecida"
        }
        fun identifiedRestriction(): Boolean = dozeExempt == false || backgroundRestricted == true || powerSave == true
        fun summary(): String = "Doze: ${optimizationLabel()}; background restrito: ${label(backgroundRestricted)}; " +
            "economia: ${label(powerSave)}; carregando: ${label(charging)}; standby: ${standbyBucket ?: "desconhecido"}; " +
            "OEM: não verificável; aparelho: $manufacturer $model; API: $androidApi; build: $systemBuild"
        private fun label(value: Boolean?): String = when (value) { true -> "sim"; false -> "não"; null -> "desconhecido" }
    }
    private val mutable = MutableStateFlow(Snapshot())
    val snapshot = mutable.asStateFlow()

    private val queryGate = SingleFlightQuery()
    private val queryExecutor = java.util.concurrent.Executors.newSingleThreadExecutor { task ->
        Thread(task, "CalcMot-PowerState").apply { isDaemon = true }
    }

    /** Never wait for Binder or another query on an Activity/service callback. */
    fun refresh(context: Context, trigger: Trigger): Snapshot {
        val app = context.applicationContext
        if (queryGate.begin()) {
            queryExecutor.execute {
                try { query(app, trigger) } finally { queryGate.end() }
            }
        }
        return mutable.value
    }

    private fun query(app: Context, trigger: Trigger) {
        val power = app.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val activity = app.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val usage = app.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        val battery = runCatching { app.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) }.getOrNull()
        val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val next = Snapshot(
            dozeExempt = if (Build.VERSION.SDK_INT >= 23) runCatching { power?.isIgnoringBatteryOptimizations(app.packageName) }.getOrNull() else null,
            backgroundRestricted = if (Build.VERSION.SDK_INT >= 28) runCatching { activity?.isBackgroundRestricted }.getOrNull() else null,
            powerSave = runCatching { power?.isPowerSaveMode }.getOrNull(),
            charging = status?.takeIf { it >= 0 }?.let { it == BatteryManager.BATTERY_STATUS_CHARGING || it == BatteryManager.BATTERY_STATUS_FULL },
            standbyBucket = if (Build.VERSION.SDK_INT >= 28) runCatching { usage?.appStandbyBucket }.getOrNull() else null,
            manufacturer = Build.MANUFACTURER, model = Build.MODEL,
            androidApi = Build.VERSION.SDK_INT, systemBuild = Build.DISPLAY
        )
        if (next != mutable.value) {
            Log.i("CalcMotPower", "POWER_STATE_CHANGED trigger=${trigger.name} doze_exempt=${next.dozeExempt} " +
                "background_restricted=${next.backgroundRestricted} power_save=${next.powerSave} charging=${next.charging} " +
                "standby=${next.standbyBucket} oem=unknown")
            mutable.value = next
        }
    }

    fun oemGuide(manufacturer: String): String = when (manufacturer.lowercase(java.util.Locale.ROOT)) {
        "xiaomi", "redmi", "poco" -> "Xiaomi/Redmi/Poco: confira Sem restrições, início automático e economia de bateria. Se disponível, mantenha o CalcMot bloqueado nos recentes. Os menus variam por HyperOS/modelo."
        "motorola" -> "Motorola: confira o gerenciamento de background. Se disponível, selecione Always allow/Sempre permitir em vez de Smart use/Uso inteligente."
        "realme", "oppo", "vivo" -> "Confira atividade em segundo plano e início automático/auto-launch. Esses controles podem ser separados da otimização de bateria."
        "samsung" -> "Samsung: se a leitura estiver sendo interrompida, confira Apps nunca suspensos/Never sleeping."
        else -> "Confira o gerenciamento de bateria do CalcMot nas configurações do aparelho."
    } + " O CalcMot não consegue verificar nem alterar esses controles automaticamente."

    private fun confirmationKey() = "oem_review_${Build.MANUFACTURER}_${Build.DISPLAY}"
    fun oemReviewed(context: Context): Boolean = context.getSharedPreferences("reliability_preferences", Context.MODE_PRIVATE)
        .getBoolean(confirmationKey(), false)
    fun confirmOemReview(context: Context) { context.getSharedPreferences("reliability_preferences", Context.MODE_PRIVATE)
        .edit().putBoolean(confirmationKey(), true).apply() }
    fun openBatterySettings(context: Context) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        if (runCatching { context.startActivity(intent) }.isFailure) {
            runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                android.net.Uri.parse("package:${context.packageName}"))) }
        }
    }
}
