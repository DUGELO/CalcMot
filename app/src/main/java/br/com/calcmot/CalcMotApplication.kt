package br.com.calcmot

import android.app.Application
import android.util.Log
import br.com.calcmot.securityrecording.application.SecurityRecordingBootstrap
import br.com.calcmot.telemetry.TelemetryProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CalcMotApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        TelemetryProvider.initialize(this)
        // Recovery never opens CameraX or a foreground service; it only verifies known local files.
        applicationScope.launch {
            if (!SecurityRecordingBootstrap.awaitReady(this@CalcMotApplication)) {
                Log.w("SecurityRecording", "recovery_pending")
            } else {
                runCatching { br.com.calcmot.securityrecording.data.RecordingFilesRepository(this@CalcMotApplication).cleanup() }
                    .onFailure { Log.w("SecurityRecording", "cleanup_pending") }
            }
            br.com.calcmot.securityrecording.platform.RecordingCleanupWorker.schedule(this@CalcMotApplication)
        }
    }
}
