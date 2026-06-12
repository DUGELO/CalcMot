package br.com.calcmot.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import br.com.calcmot.accessibility.ShellOfferBridge
import br.com.calcmot.accessibility.ShellOfferFrame
import br.com.calcmot.telemetry.OverlayLatencyTrace

class LatencyVisualProbeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return

        val elapsedRealtime = intent.getLongExtra(EXTRA_ELAPSED_REALTIME, SystemClock.elapsedRealtime())
        val probe = OverlayLatencyTrace.VisualProbe(
            elapsedRealtime = elapsedRealtime,
            label = intent.getStringExtra(EXTRA_LABEL),
            source = intent.getStringExtra(EXTRA_SOURCE) ?: "debug-broadcast"
        )

        when (ShellOfferBridge.submit(ShellOfferFrame.VisualProbe(probe))) {
            ShellOfferBridge.SubmitResult.HANDLED -> {
                Log.w(
                    TAG,
                    "CALCMOT_LATENCY_VISUAL_PROBE_SUBMITTED elapsed=$elapsedRealtime " +
                        "label=${probe.label ?: "none"} source=${probe.source ?: "none"}"
                )
            }

            ShellOfferBridge.SubmitResult.NOT_CONNECTED -> {
                Log.w(TAG, "CALCMOT_LATENCY_VISUAL_PROBE_DROPPED service=not_connected")
            }
        }
    }

    companion object {
        const val ACTION = "br.com.calcmot.DEBUG_LATENCY_VISUAL_PROBE"
        const val EXTRA_ELAPSED_REALTIME = "elapsed_realtime"
        const val EXTRA_LABEL = "label"
        const val EXTRA_SOURCE = "source"
        private const val TAG = "CalcMotLatency"
    }
}
