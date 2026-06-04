package br.com.calcmot.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Base64
import android.util.Log
import br.com.calcmot.accessibility.ShellOfferBridge
import br.com.calcmot.accessibility.ShellOfferBridge.SubmitResult
import br.com.calcmot.accessibility.ShellOfferFrame
import br.com.calcmot.processor.OfferParser
import br.com.calcmot.processor.OfferStabilityGate

class UiAutomatorOfferReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return

        if (intent.getBooleanExtra(EXTRA_INVALID, false)) {
            stabilityGate.reset()
            when (ShellOfferBridge.submit(ShellOfferFrame.InvalidFrame)) {
                SubmitResult.HANDLED -> Log.d(TAG, "Invalid shell frame submitted; overlay reset")
                SubmitResult.NOT_CONNECTED -> Log.d(TAG, "Invalid shell frame ignored; accessibility service not connected")
            }
            return
        }

        val offerText = intent.getStringExtra(EXTRA_OFFER_TEXT)
            ?: intent.getStringExtra(EXTRA_OFFER_TEXT_B64)?.decodeBase64()

        if (offerText.isNullOrBlank()) {
            stabilityGate.reset()
            ShellOfferBridge.submit(ShellOfferFrame.InvalidFrame)
            Log.d(TAG, "Empty shell frame received")
            return
        }

        val candidate = OfferParser.parse(offerText)
        if (candidate == null) {
            stabilityGate.reset()
            ShellOfferBridge.submit(ShellOfferFrame.InvalidFrame)
            Log.d(TAG, "Shell offer rejected by parser")
            return
        }

        val tripData = stabilityGate.accept(candidate)
        if (tripData == null) {
            Log.i(TAG, "Shell offer awaiting stable frame: ${candidate.fingerprint}")
            return
        }

        when (ShellOfferBridge.submit(ShellOfferFrame.StableTrip(tripData))) {
            SubmitResult.HANDLED -> Log.i(TAG, "Stable shell offer submitted: ${candidate.fingerprint}")
            SubmitResult.NOT_CONNECTED -> Log.w(TAG, "Shell offer parsed but accessibility service is not connected")
        }
    }

    private fun String.decodeBase64(): String? {
        return runCatching {
            String(Base64.decode(this, Base64.DEFAULT), Charsets.UTF_8)
        }.getOrNull()
    }

    companion object {
        const val ACTION = "br.com.calcmot.DEBUG_UIAUTOMATOR_OFFER"
        private const val EXTRA_INVALID = "invalid"
        private const val EXTRA_OFFER_TEXT = "offer_text"
        private const val EXTRA_OFFER_TEXT_B64 = "offer_text_b64"
        private const val TAG = "UiAutomatorBridge"
        private val stabilityGate = OfferStabilityGate(requiredMatchingFrames = 1)
    }
}
