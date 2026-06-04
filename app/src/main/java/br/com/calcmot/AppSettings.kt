package br.com.calcmot

import android.content.Context
import br.com.calcmot.model.ProfitabilitySettings

object AppSettings {
    private const val PREFS_NAME = "calcmot_settings"
    private const val KEY_MONITORING_ENABLED = "monitoring_enabled"
    private const val KEY_OVERLAY_POSITION = "overlay_position"
    private const val KEY_OVERLAY_CUSTOM_ENABLED = "overlay_custom_enabled"
    private const val KEY_OVERLAY_CUSTOM_X = "overlay_custom_x"
    private const val KEY_OVERLAY_CUSTOM_Y = "overlay_custom_y"
    private const val KEY_VEHICLE_EFFICIENCY = "vehicle_efficiency_km_per_unit"
    private const val KEY_INPUT_PRICE = "input_price_per_unit"
    private const val KEY_MAINTENANCE_COST_PER_KM = "maintenance_cost_per_km"
    private const val KEY_GOOD_NET_PER_KM = "good_net_per_km"
    private const val KEY_MEDIUM_NET_PER_KM = "medium_net_per_km"
    private const val KEY_MINIMUM_NET_PER_HOUR = "minimum_net_per_hour"

    fun isMonitoringEnabled(context: Context): Boolean {
        return context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_MONITORING_ENABLED, true)
    }

    fun setMonitoringEnabled(context: Context, enabled: Boolean) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_MONITORING_ENABLED, enabled)
            .apply()
    }

    fun getOverlayPosition(context: Context): OverlayPositionPreference {
        val rawValue = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_OVERLAY_POSITION, OverlayPositionPreference.HIGH.name)

        return OverlayPositionPreference.entries
            .firstOrNull { it.name == rawValue }
            ?: OverlayPositionPreference.HIGH
    }

    fun setOverlayPosition(context: Context, position: OverlayPositionPreference) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_OVERLAY_POSITION, position.name)
            .putBoolean(KEY_OVERLAY_CUSTOM_ENABLED, false)
            .remove(KEY_OVERLAY_CUSTOM_X)
            .remove(KEY_OVERLAY_CUSTOM_Y)
            .apply()
    }

    fun getCustomOverlayPosition(context: Context): OverlayCustomPosition? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_OVERLAY_CUSTOM_ENABLED, false)) return null
        return OverlayCustomPosition(
            x = prefs.getInt(KEY_OVERLAY_CUSTOM_X, 0),
            y = prefs.getInt(KEY_OVERLAY_CUSTOM_Y, getOverlayPosition(context).offsetDp)
        )
    }

    fun setCustomOverlayPosition(context: Context, position: OverlayCustomPosition) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_OVERLAY_CUSTOM_ENABLED, true)
            .putInt(KEY_OVERLAY_CUSTOM_X, position.x)
            .putInt(KEY_OVERLAY_CUSTOM_Y, position.y)
            .apply()
    }

    fun getProfitabilitySettings(context: Context): ProfitabilitySettings {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return ProfitabilitySettings(
            vehicleEfficiencyKmPerUnit = prefs.getFloat(
                KEY_VEHICLE_EFFICIENCY,
                ProfitabilitySettings.DEFAULT_VEHICLE_EFFICIENCY_KM_PER_UNIT.toFloat()
            ).toDouble(),
            inputPricePerUnit = prefs.getFloat(
                KEY_INPUT_PRICE,
                ProfitabilitySettings.DEFAULT_INPUT_PRICE_PER_UNIT.toFloat()
            ).toDouble(),
            maintenanceCostPerKm = prefs.getFloat(
                KEY_MAINTENANCE_COST_PER_KM,
                ProfitabilitySettings.DEFAULT_MAINTENANCE_COST_PER_KM.toFloat()
            ).toDouble(),
            goodNetPerKm = prefs.getFloat(
                KEY_GOOD_NET_PER_KM,
                ProfitabilitySettings.DEFAULT_GOOD_NET_PER_KM.toFloat()
            ).toDouble(),
            mediumNetPerKm = prefs.getFloat(
                KEY_MEDIUM_NET_PER_KM,
                ProfitabilitySettings.DEFAULT_MEDIUM_NET_PER_KM.toFloat()
            ).toDouble(),
            minimumNetPerHour = prefs.getFloat(
                KEY_MINIMUM_NET_PER_HOUR,
                ProfitabilitySettings.DEFAULT_MINIMUM_NET_PER_HOUR.toFloat()
            ).toDouble()
        ).normalized()
    }

    fun setProfitabilitySettings(context: Context, settings: ProfitabilitySettings) {
        val normalized = settings.normalized()
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_VEHICLE_EFFICIENCY, normalized.vehicleEfficiencyKmPerUnit.toFloat())
            .putFloat(KEY_INPUT_PRICE, normalized.inputPricePerUnit.toFloat())
            .putFloat(KEY_MAINTENANCE_COST_PER_KM, normalized.maintenanceCostPerKm.toFloat())
            .putFloat(KEY_GOOD_NET_PER_KM, normalized.goodNetPerKm.toFloat())
            .putFloat(KEY_MEDIUM_NET_PER_KM, normalized.mediumNetPerKm.toFloat())
            .putFloat(KEY_MINIMUM_NET_PER_HOUR, normalized.minimumNetPerHour.toFloat())
            .apply()
    }
}

data class OverlayCustomPosition(
    val x: Int,
    val y: Int
)

enum class OverlayPositionPreference(
    val label: String,
    val offsetDp: Int
) {
    HIGH(label = "Alto", offsetDp = 72),
    MEDIUM(label = "Médio", offsetDp = 112),
    LOW(label = "Baixo", offsetDp = 152)
}
