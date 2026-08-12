package br.com.calcmot.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper

/** Debug-only ADB entry point used to complete Firebase Crashlytics setup. */
class CrashlyticsTestCrashReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Handler(Looper.getMainLooper()).post {
            throw RuntimeException(TEST_CRASH_MESSAGE)
        }
    }

    companion object {
        const val TEST_CRASH_MESSAGE = "CalcMot Crashlytics setup test"
    }
}
