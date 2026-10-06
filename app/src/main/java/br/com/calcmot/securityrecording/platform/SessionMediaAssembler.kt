package br.com.calcmot.securityrecording.platform

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.*
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Ordered A/V composition. Gaps are preserved in metadata, not invented video frames. */
@androidx.annotation.OptIn(UnstableApi::class)
class SessionMediaAssembler(private val context: Context) {
    suspend fun assemble(inputs: List<File>, output: File) {
        require(inputs.isNotEmpty())
        if (inputs.size == 1) {
            withContext(Dispatchers.IO) {
                inputs.single().inputStream().use { input -> output.outputStream().use { out -> input.copyTo(out); out.fd.sync() } }
            }
            return
        }
        withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine<Unit> { continuation ->
                val sequence = EditedMediaItemSequence.Builder(inputs.map {
                    EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(it))).build()
                }).build()
                val composition = Composition.Builder(sequence).build()
                val transformer = Transformer.Builder(context).addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                    override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                        if (continuation.isActive) continuation.resumeWithException(IllegalStateException("Não foi possível montar a gravação."))
                    }
                }).build()
                continuation.invokeOnCancellation { Handler(Looper.getMainLooper()).post { transformer.cancel() } }
                try { transformer.start(composition, output.absolutePath) }
                catch (_: Exception) { if (continuation.isActive) continuation.resumeWithException(IllegalStateException("Não foi possível montar a gravação.")) }
            }
        }
    }
}
