package br.com.calcmot.securityrecording.platform

import android.content.Context
import androidx.work.*
import br.com.calcmot.securityrecording.data.RecordingFilesRepository
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

/** Only private maintenance. Never acquires camera/microphone or restarts a service. */
class RecordingCleanupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        RecordingFilesRepository(applicationContext).cleanup()
        Result.success()
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { Result.retry() }
    companion object {
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("calcmot_security_recording_cleanup",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<RecordingCleanupWorker>(15, TimeUnit.MINUTES).build())
        }
    }
}
