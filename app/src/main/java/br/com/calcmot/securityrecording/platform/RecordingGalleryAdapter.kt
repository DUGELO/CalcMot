package br.com.calcmot.securityrecording.platform

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Public copies are independent. The stable opaque session filename makes retries idempotent. */
class RecordingGalleryAdapter(private val context: Context) {
    fun isReadable(uri: Uri): Boolean = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L) > 0L &&
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) == "yes" &&
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes"
        } finally { retriever.release() }
    }.getOrDefault(false)

    fun findPublished(id: String, expectedDigest: String? = null): Uri? = runCatching {
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val names = "(${MediaStore.Video.Media.DISPLAY_NAME}=? OR ${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?)"
        val selection = if (Build.VERSION.SDK_INT >= 29) "$names AND ${MediaStore.Video.Media.RELATIVE_PATH}=? AND ${MediaStore.Video.Media.IS_PENDING}=0"
            else names
        val args = if (Build.VERSION.SDK_INT >= 29) arrayOf("$id.mp4", "$id-copy-%.mp4", "Movies/CalcMot/")
            else arrayOf("$id.mp4", "$id-copy-%.mp4")
        context.contentResolver.query(collection, arrayOf(MediaStore.Video.Media._ID), selection, args, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val uri = ContentUris.withAppendedId(collection, cursor.getLong(0))
                if (isReadable(uri) && (expectedDigest == null || matchesDigest(uri, expectedDigest))) return@runCatching uri
            }
            null
        }
    }.getOrNull()

    suspend fun publish(id: String, input: File): Uri {
        findPublished(id, digest(input.inputStream()))?.let { return it }
        return if (Build.VERSION.SDK_INT >= 29) publishModern(id, input) else publishLegacy(id, input)
    }
    private fun publishModern(id: String, input: File): Uri {
        check(Build.VERSION.SDK_INT >= 29)
        val resolver = context.contentResolver
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        // Interrupted, unpublished rows belong to this app and this one opaque artifact.
        resolver.query(collection, arrayOf(MediaStore.Video.Media._ID),
            "${MediaStore.Video.Media.DISPLAY_NAME}=? AND ${MediaStore.Video.Media.RELATIVE_PATH}=? AND ${MediaStore.Video.Media.IS_PENDING}=1",
            arrayOf("$id.mp4", "Movies/CalcMot/"), null)?.use { cursor ->
            while(cursor.moveToNext()) resolver.delete(ContentUris.withAppendedId(collection, cursor.getLong(0)), null, null)
        }
        val uri = resolver.insert(collection, ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "$id.mp4"); put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CalcMot/"); put(MediaStore.Video.Media.IS_PENDING, 1)
        }) ?: error("Não foi possível criar a cópia na galeria.")
        var published = false
        try {
            resolver.openOutputStream(uri, "w")?.use { output -> input.inputStream().use { it.copyTo(output) } }
                ?: error("Não foi possível escrever a cópia na galeria.")
            verifyCopy(uri, input)
            check(resolver.update(uri, ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }, null, null) == 1)
            published = true
            check(isReadable(uri))
            return uri
        } catch (failure: Exception) {
            if (!published) resolver.delete(uri, null, null)
            throw failure
        }
    }
    @Suppress("DEPRECATION")
    private suspend fun publishLegacy(id: String, input: File): Uri {
        check(ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            "Autorize salvar na galeria nas configurações do Android."
        }
        val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "CalcMot")
        check(directory.isDirectory || directory.mkdirs())
        val expectedDigest = digest(input.inputStream())
        var destination = File(directory, "$id.mp4")
        var copy = 0
        // Published files are independent: preserve an edited copy and find a free name.
        while (destination.exists() && !runCatching {
            destination.isFile && digest(destination.inputStream()) == expectedDigest
        }.getOrDefault(false)) {
            copy++
            destination = File(directory, "$id-copy-$copy.mp4")
        }
        if (!destination.exists()) {
            val staging = File(directory, ".$id.pending")
            input.inputStream().use { source -> staging.outputStream().use { target -> source.copyTo(target); target.fd.sync() } }
            check(VerifiedSegmentPromoter().verify(staging, id).isVerified)
            check(digest(staging.inputStream()) == expectedDigest)
            check(!destination.exists())
            check(staging.renameTo(destination))
        }
        val uri = suspendCancellableCoroutine<Uri> { continuation ->
            MediaScannerConnection.scanFile(context, arrayOf(destination.absolutePath), arrayOf("video/mp4")) { _, uri ->
                if (continuation.isActive) {
                    if (uri != null) continuation.resume(uri)
                    else continuation.resumeWithException(IllegalStateException("A cópia ainda não apareceu na galeria."))
                }
            }
        }
        check(isReadable(uri))
        return uri
    }
    fun matchesDigest(uri: Uri, expected: String): Boolean = runCatching {
        context.contentResolver.openInputStream(uri)?.let(::digest) == expected
    }.getOrDefault(false)
    private fun verifyCopy(uri: Uri, source: File) {
        check(context.contentResolver.openInputStream(uri)?.let(::digest) == digest(source.inputStream()))
        check(isReadable(uri))
    }
    private fun digest(input: java.io.InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        input.use { source ->
            val buffer = ByteArray(65536)
            while(true) { val count = source.read(buffer); if(count < 0) break; digest.update(buffer, 0, count) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
