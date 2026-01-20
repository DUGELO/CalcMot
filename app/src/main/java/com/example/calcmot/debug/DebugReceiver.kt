package com.example.calcmot.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.calcmot.processor.TextProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DebugReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_PROCESS_TEXT = "com.example.myapplication.ACTION_PROCESS_TEXT"
        const val EXTRA_TEXT = "text"
        const val TAG = "DebugReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (intent.action != ACTION_PROCESS_TEXT) return

        val text = intent.getStringExtra(EXTRA_TEXT) ?: ""
        Log.d(TAG, "[Broadcast] Texto recebido: $text")

        CoroutineScope(Dispatchers.Default).launch {
            TextProcessor.processText(text)
        }
    }
}
