package br.com.calcmot.securityrecording.platform

import android.media.MediaMetadataRetriever
import android.media.MediaExtractor
import android.media.MediaFormat
import br.com.calcmot.securityrecording.domain.RecordingArtifact
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/** A pending file has no public meaning until it is closed, synced, readable and atomically promoted. */
class VerifiedSegmentPromoter {
    fun promote(pending: File, destination: File, sessionId: String): Result<RecordingArtifact> = runCatching {
        require(pending.extension == "pending") { "Only pending artifacts may be promoted" }
        FileInputStream(pending).use { it.fd.sync() }
        val artifact = verify(pending, sessionId)
        require(artifact.isVerified) { "Integrity validation failed" }
        destination.parentFile?.mkdirs()
        require(!destination.exists()) { "Verified destination already exists" }
        check(pending.renameTo(destination)) { "Atomic promotion failed" }
        artifact.copy(fileName = destination.name)
    }

    fun verify(file: File, sessionId: String): RecordingArtifact {
        require(file.isFile && file.length() > 0L)
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val extractor = MediaExtractor()
            val tracks = try {
                extractor.setDataSource(file.absolutePath)
                (0 until extractor.trackCount).mapNotNull { index ->
                    val mime = extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME).orEmpty()
                    if (!mime.startsWith("audio/") && !mime.startsWith("video/")) return@mapNotNull null
                    extractor.selectTrack(index)
                    extractor.seekTo(0L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                    val sample = java.nio.ByteBuffer.allocate(4 * 1024 * 1024)
                    val readable = extractor.sampleTime >= 0L && extractor.readSampleData(sample, 0) > 0
                    extractor.unselectTrack(index)
                    mime.takeIf { readable }
                }
            } finally { extractor.release() }
            val hasVideo = tracks.any { it.startsWith("video/") }
            val hasAudio = tracks.any { it.startsWith("audio/") }
            RecordingArtifact(sessionId, file.name, duration, file.length(), hasAudio, hasVideo, file.sha256())
        } finally { retriever.release() }
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }
}
