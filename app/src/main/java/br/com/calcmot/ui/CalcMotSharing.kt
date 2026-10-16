package br.com.calcmot.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import br.com.calcmot.telemetry.AnalyticsEvents
import br.com.calcmot.telemetry.AnalyticsParams
import br.com.calcmot.telemetry.TelemetryProvider

/**
 * Voluntary ACTION_SEND only. Opening the Sharesheet is not proof that a message was sent.
 * Never called automatically or from the offer parsing / overlay pipelines.
 */
internal object CalcMotSharing {
    fun share(context: Context, entryPoint: String) {
        val message = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, CalcMotShareMessage.text())
        }
        val chooser = Intent.createChooser(message, "Compartilhar CalcMot").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(chooser)
            TelemetryProvider.analytics.track(
                AnalyticsEvents.SHARE_SHEET_OPENED,
                mapOf(AnalyticsParams.SOURCE to entryPoint)
            )
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Não foi possível abrir o compartilhamento.", Toast.LENGTH_SHORT).show()
        }
    }
}
