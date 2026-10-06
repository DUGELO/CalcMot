package br.com.calcmot.securityrecording.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import br.com.calcmot.securityrecording.application.RecordingResourceCoordinator
import br.com.calcmot.securityrecording.application.SecurityRecordingBootstrap
import br.com.calcmot.securityrecording.application.RecordingRuntimeStore
import br.com.calcmot.securityrecording.domain.*
import br.com.calcmot.securityrecording.platform.SessionMediaAssembler
import br.com.calcmot.securityrecording.platform.RecordingGalleryAdapter
import br.com.calcmot.securityrecording.platform.VerifiedSegmentPromoter
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Post-capture mutations use a durable session claim; media I/O never runs in a transaction. */
class RecordingFilesRepository(private val context: Context) {
    private val database = RecordingDatabaseProvider.get(context)
    private val dao = database.recordingDao()
    private val verifier = VerifiedSegmentPromoter()
    private val privateRoot = File(context.filesDir, "security-recording")
    fun observeSessions() = dao.observeSessions()
    fun observeSession(id: String) = dao.observeSession(id)
    suspend fun databaseSessions() = dao.sessions()
    suspend fun operation(id: String) = dao.operation(id)
    suspend fun session(id: String) = dao.session(id)
    suspend fun gaps(id: String) = dao.gaps(id)
    suspend fun segments(id: String) = dao.segmentsForSession(id)

    fun isAvailable(session: RecordingSessionEntity, now: Long = System.currentTimeMillis()): Boolean =
        session.temporaryAvailability == "AVAILABLE" && session.failureCode != "interrupted_no_valid_segment" &&
            (session.expiresAtUtcMs == null || now < session.expiresAtUtcMs) && session.phase in TERMINAL

    suspend fun playableSegments(id: String): List<RecordingSegmentEntity> = withContext(Dispatchers.IO) {
        check(SecurityRecordingBootstrap.awaitReady(context)) { "Verificação pendente. Tente novamente." }
        check(!captureActive()) { "Encerre a gravação antes de reproduzir." }
        val session = dao.session(id) ?: error("Gravação indisponível.")
        check(isAvailable(session)) { "A gravação temporária não está disponível." }
        dao.segmentsForSession(id).filter { segment ->
            segment.verified && runCatching {
                val artifact = verifier.verify(checkedPrivateFile(segment.path), id)
                artifact.isVerified && artifact.sha256 == segment.sha256
            }.getOrDefault(false)
        }.also { check(it.isNotEmpty()) { "Nenhum trecho confirmado está disponível." } }
    }

    private suspend fun reserve(id: String, kind: String, availableRequired: Boolean = true,
        change: (RecordingSessionEntity) -> RecordingSessionEntity = { it }): RecordingOperationClaimEntity {
        check(SecurityRecordingBootstrap.awaitReady(context)) { "Verificação pendente. Tente novamente." }
        check(!captureActive()) { "Encerre a gravação antes de gerenciar os arquivos." }
        return database.withTransaction {
            val current = dao.session(id) ?: error("Gravação indisponível.")
            check(current.phase in TERMINAL) { "A gravação ainda está sendo finalizada." }
            if (availableRequired) check(isAvailable(current)) { "O prazo desta gravação terminou." }
            val claim = RecordingOperationClaimEntity(id, UUID.randomUUID().toString(), kind, current.revision,
                SecurityRecordingBootstrap.processEpoch, "CLAIMED")
            check(dao.reserveOperation(claim)) { "Outra operação está em andamento. Tente novamente." }
            check(dao.updateSessionEntity(change(current).copy(revision = current.revision + 1)) == 1)
            claim
        }
    }

    private suspend fun commit(claim: RecordingOperationClaimEntity, change: (RecordingSessionEntity) -> RecordingSessionEntity) {
        database.withTransaction {
            val held = dao.operation(claim.sessionId)
            check(held?.operationId == claim.operationId && held.processEpoch == claim.processEpoch)
            val current = dao.session(claim.sessionId) ?: error("missing_session")
            check(current.revision == claim.baseRevision + 1)
            check(dao.changeOperationStatus(claim.sessionId, claim.operationId, claim.processEpoch, held.status, "COMMITTING") == 1)
            var updated = change(current)
            if (updated.temporaryAvailability == "AVAILABLE" && current.expiresAtUtcMs?.let { it <= System.currentTimeMillis() } == true) {
                updated = updated.copy(temporaryAvailability = "EXPIRED")
            }
            check(dao.updateSessionEntity(updated.copy(revision = current.revision + 1)) == 1)
            check(dao.releaseOperation(claim.sessionId, claim.operationId, claim.processEpoch) == 1)
        }
    }

    suspend fun extend(id: String, days: Int) = withLease(id, false) {
        val claim = reserve(id, "EXTEND")
        try {
            val current = dao.session(id)!!
            check(RecordingRetention.canExtend(current.retentionDays, days, current.expiresAtUtcMs, System.currentTimeMillis())) {
                "Escolha um prazo total maior, antes do vencimento."
            }
            val deadline = RecordingRetention.expiresAt(current.finalizedAtUtcMs ?: error("Prazo ainda não confirmado."), days)
            commit(claim) {
                check(RecordingRetention.canExtend(it.retentionDays, days, it.expiresAtUtcMs, System.currentTimeMillis())) { "O prazo terminou antes da confirmação." }
                it.copy(retentionDays = days, expiresAtUtcMs = deadline)
            }
        } catch (failure: Exception) { rollback(claim); throw failure }
    }

    suspend fun deletePrivate(id: String) = withLease(id, true) {
        val claim = reserve(id, "DELETE", availableRequired = false) { it.copy(temporaryAvailability = "DELETED") }
        // If interrupted, the durable claim and tombstone are resumed on bootstrap.
        erasePrivateSession(id)
        commit(claim) { it.copy(temporaryAvailability = "DELETED") }
    }

    suspend fun saveToGallery(id: String): Uri = withLease(id, false) {
        val existing = dao.session(id) ?: error("Gravação indisponível.")
        val gallery = RecordingGalleryAdapter(context)
        existing.galleryUri?.takeIf { existing.galleryState == "PUBLISHED" }?.let { uri ->
            if (gallery.isReadable(Uri.parse(uri)) && existing.gallerySha256?.let { gallery.matchesDigest(Uri.parse(uri), it) } == true) return@withLease Uri.parse(uri)
        }
        val claim = reserve(id, "PUBLISH") { it.copy(galleryState = "COPYING", exportState = "BUILDING",
            exportPath = File(privateRoot, "work/$id.mp4").absolutePath) }
        try {
            val artifact = assemble(id)
            val uri = gallery.publish(id, artifact)
            commit(claim) { it.copy(galleryState = "PUBLISHED", galleryUri = uri.toString(), gallerySha256 = verifier.verify(artifact, id).sha256, exportState = "READY", exportPath = artifact.absolutePath, exportSha256 = verifier.verify(artifact, id).sha256) }
            uri
        } catch (failure: Exception) {
            rollback(claim, galleryFailed = true)
            throw failure
        }
    }

    /** A fresh grant may be launched for every user action; canonical assembly is reused. */
    suspend fun prepareShare(id: String): Uri = withLease(id, false) {
        val claim = reserve(id, "EXPORT") { it.copy(exportState = "BUILDING",
            exportPath = File(privateRoot, "work/$id.mp4").absolutePath) }
        try {
            val canonical = assemble(id)
            val directory = File(privateRoot, "share").apply { check(isDirectory || mkdirs()) }
            val shared = File(directory, "$id.mp4")
            val digest = verifier.verify(canonical, id).sha256
            if (!shared.isFile || runCatching { verifier.verify(shared, id).sha256 == digest }.getOrDefault(false).not()) {
                val staging = File(directory, "$id.pending")
                canonical.inputStream().use { input -> staging.outputStream().use { output -> input.copyTo(output); output.fd.sync() } }
                check(verifier.verify(staging, id).sha256 == digest)
                check(!shared.exists() || shared.delete())
                check(staging.renameTo(shared))
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.securityrecording.files", shared)
            commit(claim) { it.copy(exportState = "READY", exportPath = canonical.absolutePath, exportSha256 = digest) }
            uri
        } catch (failure: Exception) { rollback(claim); throw failure }
    }

    /** Record a user initiated launch before granting access; failed launches conservatively retain grace. */
    suspend fun markShareLaunch(id: String) = withLease(id, false) {
        val claim = reserve(id, "SHARE_LAUNCH")
        try {
            check(File(privateRoot, "share/$id.mp4").isFile)
            commit(claim) { it.copy(lastShareLaunchedAtUtcMs = System.currentTimeMillis()) }
        } catch(failure: Exception) { rollback(claim); throw failure }
    }

    private suspend fun assemble(id: String): File {
        val segments = dao.segmentsForSession(id)
        check(segments.isNotEmpty()) { "Nenhum trecho confirmado está disponível." }
        val inputs = segments.map { segment ->
            checkedPrivateFile(segment.path).also { file ->
                val verified = verifier.verify(file, id)
                check(segment.verified && verified.isVerified && verified.sha256 == segment.sha256) { "Um trecho está ausente ou foi alterado." }
            }
        }
        val inputBytes = segments.sumOf { it.bytes }
        val required = Math.addExact(Math.multiplyExact(inputBytes, 2L), RecordingStoragePolicy.RESERVE_BYTES)
        check(android.os.StatFs(context.filesDir.absolutePath).availableBytes >= required) { "Libere espaço no aparelho antes de preparar a cópia." }
        val directory = File(privateRoot, "work").apply { check(isDirectory || mkdirs()) }
        val completed = File(directory, "$id.mp4")
        val current = dao.session(id)!!
        if (completed.isFile && current.exportSha256 != null && runCatching {
            verifier.verify(completed, id).let { it.isVerified && it.sha256 == current.exportSha256 }
        }.getOrDefault(false)) return completed
        check(!completed.exists() || completed.delete())
        val pending = File(directory, "$id.pending")
        check(!pending.exists() || pending.delete())
        SessionMediaAssembler(context).assemble(inputs, pending)
        verifier.promote(pending, completed, id).getOrThrow()
        return completed
    }

    private suspend fun rollback(claim: RecordingOperationClaimEntity, galleryFailed: Boolean = false) {
        // If even this commit fails, preserve the claim for recovery; never report success.
        commit(claim) { it.copy(exportState = if (claim.kind in setOf("PUBLISH", "EXPORT")) "FAILED" else it.exportState,
            galleryState = if (galleryFailed) "FAILED" else it.galleryState) }
    }

    /** Called only behind the bootstrap barrier, before accepting any new mutation. */
    internal suspend fun recoverOperations() = withContext(Dispatchers.IO) {
        dao.operations().filter { it.kind != "RECOVERY" }.forEach { previous ->
            check(!RecordingResourceCoordinator.hasOperations()) { "operation_still_owned" }
            check(dao.takeOverOperation(previous.sessionId, previous.processEpoch, SecurityRecordingBootstrap.processEpoch) == 1)
            val claim = previous.copy(processEpoch = SecurityRecordingBootstrap.processEpoch)
            when (claim.kind) {
                "DELETE", "EXPIRE" -> {
                    erasePrivateSession(claim.sessionId)
                    commit(claim) { it.copy(temporaryAvailability = if (claim.kind == "DELETE") "DELETED" else "EXPIRED") }
                }
                "REVOKE_SHARE" -> { eraseSharedFile(claim.sessionId); commit(claim) { it } }
                "PUBLISH" -> {
                    val canonical = File(privateRoot, "work/${claim.sessionId}.mp4")
                    val digest = runCatching { verifier.verify(canonical, claim.sessionId).takeIf { it.isVerified }?.sha256 }.getOrNull()
                    val uri = digest?.let { RecordingGalleryAdapter(context).findPublished(claim.sessionId, it) }
                    commit(claim) { it.copy(galleryUri = uri?.toString(), galleryState = if (uri == null) "FAILED" else "PUBLISHED",
                        gallerySha256 = if(uri == null) null else digest,
                        exportState = if(digest == null) "FAILED" else "READY", exportSha256 = digest) }
                }
                else -> rollback(claim)
            }
        }
    }

    suspend fun cleanup() = withContext(Dispatchers.IO) {
        if (!SecurityRecordingBootstrap.awaitReady(context) || captureActive()) return@withContext
        val now = System.currentTimeMillis()
        dao.sessions().filter { it.phase in TERMINAL && it.expiresAtUtcMs?.let { deadline -> deadline <= now } == true }
            .forEach { value ->
                val hasPrivateFiles = File(privateRoot, "sessions/${value.id}").exists() ||
                    File(privateRoot, "work/${value.id}.mp4").exists() || File(privateRoot, "work/${value.id}.pending").exists()
                if (dao.operation(value.id) == null && (value.temporaryAvailability == "AVAILABLE" || hasPrivateFiles)) {
                    withLease(value.id, true) {
                        val claim = reserve(value.id, "EXPIRE", false) {
                            it.copy(temporaryAvailability = if (it.temporaryAvailability == "DELETED") "DELETED" else "EXPIRED")
                        }
                        erasePrivateSession(value.id)
                        commit(claim) { it }
                    }
                }
            }
        dao.sessions().filter { it.temporaryAvailability != "AVAILABLE" }.forEach { value ->
            val physicalDeadline = RecordingRetention.shareCleanupAt(value.expiresAtUtcMs ?: now, value.lastShareLaunchedAtUtcMs)
            if (physicalDeadline <= now && dao.operation(value.id) == null) {
                if(File(privateRoot, "share/${value.id}.mp4").isFile) withLease(value.id, true) {
                    val current = dao.session(value.id) ?: return@withLease
                    // Recheck grace after acquiring the same session lease used by a share launch.
                    if(RecordingRetention.shareCleanupAt(current.expiresAtUtcMs ?: now, current.lastShareLaunchedAtUtcMs) <= System.currentTimeMillis()) {
                        val claim = reserve(value.id, "REVOKE_SHARE", false)
                        eraseSharedFile(value.id)
                        commit(claim) { it }
                    }
                }
            }
        }
    }

    private suspend fun <T> withLease(id: String, destructive: Boolean, block: suspend () -> T): T = withContext(Dispatchers.IO) {
        check(SecurityRecordingBootstrap.awaitReady(context)) { "Verificação pendente. Tente novamente." }
        check(RecordingResourceCoordinator.acquireOperation(id, destructive)) { "Encerre a captura ou a reprodução antes de continuar." }
        try { block() } finally {
            withContext(kotlinx.coroutines.NonCancellable) {
                // Close the recovery barrier before releasing the in-process lease.
                val pending = runCatching { dao.operation(id) != null }.getOrDefault(true)
                if (pending) SecurityRecordingBootstrap.invalidate()
                RecordingResourceCoordinator.releaseOperation(id)
                if (!RecordingResourceCoordinator.hasOperations() && !SecurityRecordingBootstrap.isReady) {
                    SecurityRecordingBootstrap.invalidate()
                }
            }
        }
    }

    private fun eraseSharedFile(id: String) {
        val shared = checkedPrivateFile(File(privateRoot, "share/$id.mp4").absolutePath)
        if(shared.isFile) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.securityrecording.files", shared)
            context.revokeUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            check(shared.delete())
        }
    }

    suspend fun thumbnailFile(id: String): File? = withContext(Dispatchers.IO) {
        val session = dao.session(id) ?: return@withContext null
        if(!isAvailable(session)) return@withContext null
        dao.segmentsForSession(id).firstNotNullOfOrNull { segment ->
            if(!segment.verified) null else runCatching {
                checkedPrivateFile(segment.path).takeIf { file -> verifier.verify(file, id).let { it.isVerified && it.sha256 == segment.sha256 } }
            }.getOrNull()
        }
    }

    private suspend fun erasePrivateSession(id: String) {
        val directory = checkedPrivateFile(File(privateRoot, "sessions/$id").absolutePath)
        if (directory.exists()) directory.walkBottomUp().forEach { file ->
            checkedPrivateFile(file.absolutePath)
            check(file.delete() || !file.exists()) { "Uma parte da exclusão ficou pendente." }
        }
        listOf("work/$id.pending", "work/$id.mp4").forEach { relative ->
            val file = checkedPrivateFile(File(privateRoot, relative).absolutePath)
            check(!file.exists() || file.delete())
        }
    }

    fun checkedPrivateFile(path: String): File {
        val file = File(path).canonicalFile
        val root = privateRoot.canonicalPath + File.separator
        check(file.path.startsWith(root)) { "invalid_private_path" }
        return file
    }
    companion object {
        val TERMINAL = setOf("VERIFIED", "FAILED")
        fun captureActive() = RecordingResourceCoordinator.isCapturing()
    }
}
