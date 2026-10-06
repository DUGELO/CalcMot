package br.com.calcmot.securityrecording.platform

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import android.os.StatFs
import android.os.SystemClock
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.room.withTransaction
import br.com.calcmot.R
import br.com.calcmot.securityrecording.ui.SecurityRecordingActivity
import br.com.calcmot.securityrecording.application.*
import br.com.calcmot.securityrecording.data.*
import br.com.calcmot.securityrecording.domain.*
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

/** Commands, CameraX callbacks and durable commits share one serial consumer. */
class RecordingSessionService : LifecycleService() {
    private sealed interface Event {
        data class Command(val command: RecordingCommand, val authorized: Boolean, val setup: RecordingSetup?, val orientation: Int) : Event
        data class Bound(val sessionId: String, val cameraProvider: ProcessCameraProvider?, val failed: Boolean) : Event
        data class Video(val sessionId: String, val ordinal: Int, val value: VideoRecordEvent) : Event
        data class Timeout(val sessionId: String, val finalizing: Boolean) : Event
        data object Thermal : Event
        data object Monitor : Event
    }
    private val queue = Channel<Event>(Channel.UNLIMITED)
    private lateinit var database: RecordingDatabase
    private val dao get() = database.recordingDao()
    private var session: RecordingSessionEntity? = null
    private var recording: Recording? = null
    private var provider: ProcessCameraProvider? = null
    private var capture: VideoCapture<Recorder>? = null
    private var pending: File? = null
    private var ordinal = 0
    private var finalizedBytes = 0L
    private var bytesPerMinuteEstimate = RecordingStoragePolicy.INITIAL_BYTES_PER_MINUTE
    private var finalizedDuration = 0L
    private var segmentDuration = 0L
    private var progressBaseline = 0L
    private var startObserved = false
    private var audioConfirmed = false
    private var gapStarted: Long? = null
    private var gapReason: String? = null
    private var timeout: Job? = null
    private var lastCheckpoint = 0L
    private var lastEncodedProgress = 0L
    private var latestBytes = 0L
    private var finalizing = false
    private val handledCommands = mutableSetOf<String>()
    private var thermalListener: PowerManager.OnThermalStatusChangedListener? = null

    override fun onCreate() {
        super.onCreate()
        database = RecordingDatabaseProvider.get(this)
        createChannel()
        if (Build.VERSION.SDK_INT >= 29) {
            val listener = PowerManager.OnThermalStatusChangedListener {
                if (it >= PowerManager.THERMAL_STATUS_SEVERE) queue.trySend(Event.Thermal)
            }
            thermalListener = listener
            getSystemService(PowerManager::class.java).addThermalStatusListener(ContextCompat.getMainExecutor(this), listener)
        }
        lifecycleScope.launch {
            for (event in queue) {
                try {
                    when (event) {
                        is Event.Command -> command(event)
                        is Event.Bound -> if (event.sessionId == session?.id && session?.phase in OPENING) {
                            if (event.failed || event.cameraProvider == null) terminate(FailureCode.CAMERA)
                            else bind(event.cameraProvider)
                        }
                        is Event.Video -> if (event.sessionId == session?.id && event.ordinal == ordinal && session?.phase in ACTIVE) video(event.value)
                        is Event.Timeout -> if (event.sessionId == session?.id) {
                            if (event.finalizing && finalizing) deferResult(FailureCode.FINALIZE)
                            else if (session?.phase in OPENING || session?.phase == "PAUSING") terminate(FailureCode.PREPARATION_TIMEOUT)
                        }
                        Event.Thermal -> if (session?.phase in ACTIVE) terminate(FailureCode.THERMAL)
                        Event.Monitor -> monitorResources()
                    }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { deferResult(FailureCode.STORAGE) }
            }
        }
        lifecycleScope.launch { while(isActive) { delay(2_000); queue.send(Event.Monitor) } }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val command = intent?.toCommand() ?: run { if (session == null) stopSelf(); return START_NOT_STICKY }
        val authorized = command.action != RecordingCommand.Action.START ||
            (SecurityRecordingBootstrap.isReady && RecordingStartAuthorization.consume(intent.getStringExtra(EXTRA_START_TOKEN)))
        val setup = if (command.action == RecordingCommand.Action.START) runCatching {
            RecordingSetup(RecordingLens.valueOf(intent.getStringExtra(EXTRA_LENS) ?: "FRONT"),
                intent.getIntExtra(EXTRA_SEGMENT_MINUTES, RecordingSetup.DEFAULT_SEGMENT_MINUTES))
        }.getOrNull() else null
        if (command.action == RecordingCommand.Action.START && session == null && authorized && setup != null) {
            // Meet the foreground deadline before the actor performs disk I/O or acquires resources.
            if (runCatching { foreground(notification(RecordingRuntimeSnapshot(null, RecordingPhase.PREPARING, 0))) }.isFailure) {
                RecordingRuntimeStore.publish(RecordingRuntimeSnapshot(null, RecordingPhase.FAILED, 0, FailureCode.FOREGROUND.userMessage))
                stopSelf(); return START_NOT_STICKY
            }
        }
        queue.trySend(Event.Command(command, authorized, setup, intent.getIntExtra(EXTRA_ORIENTATION, 0)))
        return START_NOT_STICKY
    }

    private suspend fun command(event: Event.Command) {
        val command = event.command
        // Dedupe surrounds effects, including inserts, CameraX acquisition and stop.
        if (!handledCommands.add(command.commandId)) return
        if (command.action == RecordingCommand.Action.START) {
            if (!event.authorized || event.setup == null || session != null || command.expectedSessionRevision != 0L) {
                reject(); if (session == null) stopSelf(); return
            }
            if (!permissionsReady(this)) { reject(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return }
            val setup = event.setup
            val preferences = RecordingSetupRepository(this).read()
            check(preferences.disclosureVersion == RecordingSetupRepository.DISCLOSURE_VERSION && preferences.framed && preferences.lens == setup.lens && preferences.framedOrientation == event.orientation)
            val id = UUID.randomUUID().toString()
            val value = RecordingSessionEntity(id, 1, "PREPARING", createdAtUtcMs = System.currentTimeMillis(),
                processEpoch = SecurityRecordingBootstrap.processEpoch, ownerId = UUID.randomUUID().toString(),
                lens = setup.lens.name, orientation = event.orientation, quality = "P480", segmentMinutes = setup.segmentMinutes,
                retentionDays = preferences.retentionDays, retentionOrigin = "SESSION_SNAPSHOT")
            withContext(Dispatchers.IO) { dao.insertSession(value) }
            session = value
            RecordingResourceCoordinator.registerOwner(value.ownerId!!)
            publish()
            if (!hasAdmissionSpace()) { terminate(FailureCode.INSUFFICIENT_STORAGE); return }
            if (Build.VERSION.SDK_INT >= 29 && getSystemService(PowerManager::class.java).currentThermalStatus >= PowerManager.THERMAL_STATUS_SEVERE) {
                terminate(FailureCode.THERMAL); return
            }
            openCamera()
            return
        }
        val current = session ?: run { reject(); stopSelf(); return }
        if (!RecordingCaptureEvidence.matches(command, current.id, current.revision)) { reject(); publish(); return }
        RecordingRuntimeStore.publishOutcome(null)
        when (command.action) {
            RecordingCommand.Action.STOP -> if (current.phase in STOPPABLE) terminate(null) else reject()
            RecordingCommand.Action.PAUSE -> if (current.phase == "RECORDING") {
                write(current.copy(phase = "PAUSING")); audioConfirmed = false; publish(); recording?.pause(); scheduleTimeout(false)
            } else reject()
            RecordingCommand.Action.RESUME -> if (current.phase == "PAUSED" && permissionsReady(this)) {
                startObserved = false; progressBaseline = segmentDuration
                write(current.copy(phase = "RESUMING")); publish(); recording?.resume(); scheduleTimeout(false)
            } else reject()
            else -> Unit
        }
    }

    private fun reject() {
        RecordingRuntimeStore.publishOutcome("O estado mudou. Confira os controles e tente novamente.")
    }

    private fun openCamera() {
        val id = session?.id ?: return
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val result = runCatching { future.get() }
            queue.trySend(Event.Bound(id, result.getOrNull(), result.isFailure))
        }, ContextCompat.getMainExecutor(this))
        scheduleTimeout(false)
    }

    private suspend fun bind(cameraProvider: ProcessCameraProvider) {
        val current = session ?: return
        provider = cameraProvider
        val selector = if (current.lens == "FRONT") CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        val info = cameraProvider.availableCameraInfos.firstOrNull { selector.filter(listOf(it)).isNotEmpty() }
        if (info == null || Quality.SD !in Recorder.getVideoCapabilities(info).getSupportedQualities(androidx.camera.core.DynamicRange.SDR)) {
            terminate(FailureCode.CAMERA); return
        }
        try {
            val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.SD)).build()
            val useCase = VideoCapture.withOutput(recorder)
            useCase.targetRotation = current.orientation ?: 0
            cameraProvider.bindToLifecycle(this, selector, useCase)
            capture = useCase
            openSegment()
        } catch (_: Exception) { terminate(FailureCode.CAMERA) }
    }

    private suspend fun openSegment() {
        val current = session ?: return
        if (!permissionsReady(this)) { terminate(FailureCode.PERMISSION); return }
        if (!hasAdmissionSpace()) { terminate(FailureCode.INSUFFICIENT_STORAGE); return }
        val file = withContext(Dispatchers.IO) {
            val directory = File(filesDir, "security-recording/sessions/${current.id}")
            check(directory.isDirectory || directory.mkdirs())
            File(directory, "%05d-%s.pending".format(ordinal, UUID.randomUUID()))
        }
        pending = file
        segmentDuration = 0L; latestBytes = 0L; progressBaseline = 0L
        lastEncodedProgress = SystemClock.elapsedRealtime()
        startObserved = false
        audioConfirmed = false
        val recorder = capture?.output ?: run { terminate(FailureCode.CAMERA); return }
        val segmentOrdinal = ordinal
        val output = FileOutputOptions.Builder(file).setFileSizeLimit(RecordingStoragePolicy.MAX_SEGMENT_BYTES)
            .setDurationLimitMillis((current.segmentMinutes ?: 10) * 60_000L).build()
        val outputLease = "${current.id}:${file.name}"
        RecordingResourceCoordinator.registerOpenOutput(outputLease)
        try {
            recording = recorder.prepareRecording(this, output).withAudioEnabled().start(ContextCompat.getMainExecutor(this)) {
                if(it is VideoRecordEvent.Finalize) RecordingResourceCoordinator.releaseOpenOutput(outputLease)
                val delivered = queue.trySend(Event.Video(current.id, segmentOrdinal, it)).isSuccess
                if(!delivered && it is VideoRecordEvent.Finalize) {
                    // A late Finalize can outlive LifecycleService. Only now may recovery read this output.
                    val appContext = applicationContext
                    RecordingOperationRunner.start {
                        SecurityRecordingBootstrap.invalidate()
                        if(SecurityRecordingBootstrap.awaitReady(appContext)) runCatching { RecordingFilesRepository(appContext).cleanup() }
                    }
                }
            }
            scheduleTimeout(false)
        } catch (_: SecurityException) { RecordingResourceCoordinator.releaseOpenOutput(outputLease); terminate(FailureCode.PERMISSION) }
        catch (_: Exception) { RecordingResourceCoordinator.releaseOpenOutput(outputLease); terminate(FailureCode.CAMERA) }
    }

    private suspend fun video(event: VideoRecordEvent) {
        val current = session ?: return
        when (event) {
            is VideoRecordEvent.Start -> { startObserved = true; observeCapture(event) }
            is VideoRecordEvent.Resume -> { startObserved = true; observeCapture(event) }
            is VideoRecordEvent.Status -> observeCapture(event)
            is VideoRecordEvent.Pause -> if (current.phase == "PAUSING") {
                segmentDuration = maxOf(segmentDuration, event.recordingStats.recordedDurationNanos / 1_000_000L)
                timeout?.cancel(); audioConfirmed = false
                gapStarted = SystemClock.elapsedRealtime(); gapReason = "pause"
                write(current.copy(phase = "PAUSED", capturedDurationMs = finalizedDuration + segmentDuration)); publish()
            }
            is VideoRecordEvent.Finalize -> finalizeSegment(event)
        }
    }

    private suspend fun observeCapture(event: VideoRecordEvent) {
        val current = session ?: return
        if (current.phase !in setOf("PREPARING", "RESUMING", "ROTATING", "RECORDING")) return
        val activeAudio = event.recordingStats.audioStats.audioState == AudioStats.AUDIO_STATE_ACTIVE
        if (current.phase == "RECORDING" && (!activeAudio || !permissionsReady(this))) {
            audioConfirmed = false
            terminate(if (activeAudio) FailureCode.PERMISSION else FailureCode.MICROPHONE)
            return
        }
        val duration = event.recordingStats.recordedDurationNanos / 1_000_000L
        if (!RecordingCaptureEvidence.confirmed(startObserved, activeAudio, duration, progressBaseline)) return // Wait for actual encoded progress, not merely permission or Start.
        audioConfirmed = true
        if(duration > segmentDuration) lastEncodedProgress = SystemClock.elapsedRealtime()
        latestBytes = event.recordingStats.numBytesRecorded
        if(duration >= 1_000) bytesPerMinuteEstimate = maxOf(bytesPerMinuteEstimate, Math.multiplyExact(latestBytes, 60_000L) / duration + 1)
        segmentDuration = maxOf(segmentDuration, duration)
        if (current.phase != "RECORDING") {
            timeout?.cancel(); closeGap()
            write(session!!.copy(phase = "RECORDING", capturedDurationMs = finalizedDuration + segmentDuration)); publish()
        }
        val now = SystemClock.elapsedRealtime()
        if (now - lastCheckpoint >= 1_000L) {
            lastCheckpoint = now
            val bytes = event.recordingStats.numBytesRecorded
            val available = withContext(Dispatchers.IO) { StatFs(filesDir.absolutePath).availableBytes }
            if (!RecordingStoragePolicy.canContinue(available, Math.addExact(finalizedBytes, bytes))) {
                terminate(FailureCode.INSUFFICIENT_STORAGE); return
            }
            write(session!!.copy(capturedDurationMs = finalizedDuration + segmentDuration)); publish()
        }
    }

    private suspend fun monitorResources() {
        val current = session ?: return
        if(current.phase !in STOPPABLE) return
        if(!permissionsReady(this)) { terminate(FailureCode.PERMISSION); return }
        val available = withContext(Dispatchers.IO) { StatFs(filesDir.absolutePath).availableBytes }
        if(!RecordingStoragePolicy.canContinue(available, Math.addExact(finalizedBytes, latestBytes))) {
            terminate(FailureCode.INSUFFICIENT_STORAGE); return
        }
        if(current.phase == "RECORDING" && SystemClock.elapsedRealtime() - lastEncodedProgress > 15_000L) {
            terminate(FailureCode.CAMERA)
        }
    }

    private suspend fun terminate(reason: FailureCode?) {
        val current = session ?: return
        if (current.phase !in ACTIVE || finalizing) return
        audioConfirmed = false
        finalizing = true
        closeGap()
        timeout?.cancel()
        write(current.copy(phase = "FINALIZING", completionReason = reason?.persistedCode ?: "user_stop"))
        publish()
        if (recording != null) { recording?.stop(); scheduleTimeout(true) }
        else completeSession(reason ?: FailureCode.CANCELLED)
    }

    private suspend fun finalizeSegment(event: VideoRecordEvent.Finalize) {
        recording = null; audioConfirmed = false; timeout?.cancel()
        val current = session ?: return
        val limit = event.error == VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED ||
            event.error == VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED
        val rotate = limit && !finalizing && current.phase == "RECORDING"
        write(current.copy(phase = if (rotate) "ROTATING" else "FINALIZING",
            completionReason = current.completionReason ?: if (event.hasError() && !limit) "finalize" else null))
        publish()
        if(rotate) { gapStarted = gapStarted ?: SystemClock.elapsedRealtime(); gapReason = gapReason ?: "rotation" }
        val file = pending
        val artifact = if (file == null) null else withContext(Dispatchers.IO) {
            VerifiedSegmentPromoter().promote(file, File(file.parentFile, file.nameWithoutExtension + ".mp4"), current.id).getOrNull()
        }
        if (artifact != null) {
            val confirmedSegmentDuration = maxOf(artifact.durationMs, segmentDuration)
            val state = session!!
            withContext(Dispatchers.IO) {
                database.withTransaction {
                    val segmentId = UUID.randomUUID().toString()
                    val path = File(file!!.parentFile, artifact.fileName).absolutePath
                    check(dao.insertSegment(RecordingSegmentEntity(segmentId, state.id, ordinal, path,
                        artifact.durationMs, artifact.bytes, true, artifact.sha256)) != -1L)
                    dao.insertArtifact(RecordingArtifactEntity(UUID.randomUUID().toString(), state.id, segmentId, path, "VERIFIED", artifact.sha256))
                    check(dao.writeOwned(state.revision, state.copy(revision = state.revision + 1,
                        capturedDurationMs = finalizedDuration + confirmedSegmentDuration)))
                }
            }
            finalizedBytes = Math.addExact(finalizedBytes, artifact.bytes)
            finalizedDuration = Math.addExact(finalizedDuration, confirmedSegmentDuration)
            session = state.copy(revision = state.revision + 1, capturedDurationMs = finalizedDuration)
            pending = null
        }
        // Valid media stays at its deterministic session path if a DB commit throws.
        if (rotate && artifact != null) {
            ordinal++
            if (!hasAdmissionSpace()) { finalizing = false; terminate(FailureCode.INSUFFICIENT_STORAGE) }
            else openSegment()
        } else completeSession(if (artifact == null) FailureCode.INTEGRITY else null)
    }

    private suspend fun completeSession(reason: FailureCode?) {
        val current = session ?: return
        closeGap()
        val finalizedAt = System.currentTimeMillis()
        val valid = withContext(Dispatchers.IO) { dao.segmentsForSession(current.id).any { it.verified } }
        write(session!!.copy(phase = if (valid) "VERIFIED" else "FAILED",
            failureCode = if (valid) (current.completionReason ?: reason?.persistedCode)?.takeUnless { it == "user_stop" }
                else if(current.completionReason == "user_stop" && current.capturedDurationMs == null) FailureCode.CANCELLED.persistedCode
                else current.completionReason?.takeUnless { it == "user_stop" } ?: (reason ?: FailureCode.CANCELLED).persistedCode,
            capturedDurationMs = if(valid) finalizedDuration else null,
            completionReason = current.completionReason ?: reason?.persistedCode ?: "completed",
            finalizedAtUtcMs = finalizedAt,
            expiresAtUtcMs = RecordingRetention.expiresAt(finalizedAt, current.retentionDays ?: 1)))
        publish()
        releaseCapture()
        stopForeground(STOP_FOREGROUND_REMOVE)
        notifyResult(session!!)
        stopSelf()
    }

    private suspend fun closeGap() {
        val start = gapStarted ?: return
        val current = session ?: return
        withContext(Dispatchers.IO) {
            dao.insertGap(RecordingGapEntity(UUID.randomUUID().toString(), current.id, start,
                (SystemClock.elapsedRealtime() - start).coerceAtLeast(0), gapReason ?: "interruption"))
        }
        gapStarted = null; gapReason = null
    }

    private suspend fun write(value: RecordingSessionEntity) {
        val before = session ?: return
        val next = value.copy(revision = before.revision + 1)
        check(withContext(Dispatchers.IO) { dao.writeOwned(before.revision, next) }) { "writer_conflict" }
        session = next
    }

    private suspend fun hasAdmissionSpace(): Boolean = withContext(Dispatchers.IO) {
        StatFs(filesDir.absolutePath).availableBytes >= RecordingStoragePolicy.requiredBytes(session?.segmentMinutes ?: 10, finalizedBytes, bytesPerMinuteEstimate)
    }

    private fun scheduleTimeout(finalizing: Boolean) {
        timeout?.cancel()
        val id = session?.id ?: return
        timeout = lifecycleScope.launch { delay(30_000L); queue.send(Event.Timeout(id, finalizing)) }
    }

    /** Pending is an honest result; never quarantine a readable file after a database error. */
    private fun deferResult(reason: FailureCode) {
        timeout?.cancel(); audioConfirmed = false
        val current = session
        RecordingRuntimeStore.publish(RecordingRuntimeSnapshot(current?.id, RecordingPhase.FAILED,
            current?.revision ?: 0L, "Resultado pendente de verificação. ${reason.userMessage}", current?.capturedDurationMs, false))
        RecordingRuntimeStore.publishOutcome("Resultado pendente de verificação. ${reason.userMessage}")
        releaseCapture()
        stopForeground(STOP_FOREGROUND_REMOVE); current?.let(::notifyResult); stopSelf()
    }

    private fun snapshot(): RecordingRuntimeSnapshot {
        val value = session ?: return RecordingRuntimeSnapshot(null, RecordingPhase.IDLE, 0)
        return RecordingRuntimeSnapshot(value.id, RecordingPhase.valueOf(value.phase), value.revision,
            value.failureCode?.let { code -> FailureCode.entries.firstOrNull { it.persistedCode == code }?.userMessage },
            value.capturedDurationMs, audioConfirmed)
    }
    private fun publish() {
        val snapshot = snapshot()
        RecordingRuntimeStore.publish(snapshot)
        if (snapshot.phase.name in ACTIVE) foreground(notification(snapshot))
    }
    private fun releaseCapture() {
        recording?.close(); recording = null
        capture?.let { provider?.unbind(it) }; capture = null; provider = null
    }
    override fun onDestroy() {
        timeout?.cancel(); releaseCapture()
        if (Build.VERSION.SDK_INT >= 29) thermalListener?.let { getSystemService(PowerManager::class.java).removeThermalStatusListener(it) }
        if (session?.phase in ACTIVE && RecordingRuntimeStore.snapshot.value.phase != RecordingPhase.FAILED) {
            RecordingRuntimeStore.publish(snapshot().copy(phase = RecordingPhase.FAILED, audioVideoConfirmed = false,
                failureMessage = "A gravação foi interrompida. O resultado precisa ser verificado."))
        }
        session?.ownerId?.let(RecordingResourceCoordinator::releaseOwner)
        if (session?.phase in ACTIVE) SecurityRecordingBootstrap.invalidate()
        RecordingResourceCoordinator.releaseCapture()
        val appContext = applicationContext
        RecordingOperationRunner.start {
            if(SecurityRecordingBootstrap.awaitReady(appContext)) runCatching { RecordingFilesRepository(appContext).cleanup() }
        }
        queue.close(); super.onDestroy()
    }
    private fun foreground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= 30) startForeground(NOTIFICATION_ID, notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        else startForeground(NOTIFICATION_ID, notification)
    }
    private fun notification(snapshot: RecordingRuntimeSnapshot): Notification {
        val text = when (snapshot.phase) {
            RecordingPhase.RECORDING -> "Gravando · ${formatTime(snapshot.capturedDurationMs)}"
            RecordingPhase.PAUSED -> "Pausado · ${formatTime(snapshot.capturedDurationMs)}"
            RecordingPhase.PAUSING -> "Pausando…"
            RecordingPhase.RESUMING -> "Retomando gravação…"
            RecordingPhase.ROTATING -> "Trocando arquivo…"
            RecordingPhase.FINALIZING -> "Finalizando gravação…"
            else -> "Iniciando gravação…"
        }
        val returnIntent = Intent(this, SecurityRecordingActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_recording_notification).setContentTitle("CalcMot").setContentText(text)
            .setOnlyAlertOnce(true).setOngoing(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(PendingIntent.getActivity(this, NOTIFICATION_ID, returnIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        val actions = when(snapshot.phase) {
            RecordingPhase.RECORDING -> listOf(RecordingCommand.Action.PAUSE, RecordingCommand.Action.STOP)
            RecordingPhase.PAUSED -> listOf(RecordingCommand.Action.RESUME, RecordingCommand.Action.STOP)
            else -> if (snapshot.phase.name in STOPPABLE) listOf(RecordingCommand.Action.STOP) else emptyList()
        }
        actions.forEach { action ->
            val intent = commandIntent(this, RecordingCommand(expectedSessionRevision = snapshot.sessionRevision,
                action = action, sessionId = snapshot.sessionId))
            intent.action = "securityrecording.${snapshot.sessionId}.${snapshot.sessionRevision}.${action.name}"
            val pending = PendingIntent.getService(this, action.ordinal + NOTIFICATION_ID, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            builder.addAction(when(action) {
                RecordingCommand.Action.PAUSE -> android.R.drawable.ic_media_pause
                RecordingCommand.Action.RESUME -> android.R.drawable.ic_media_play
                else -> android.R.drawable.ic_menu_close_clear_cancel
            }, when(action) { RecordingCommand.Action.PAUSE -> "Pausar"; RecordingCommand.Action.RESUME -> "Retomar"; else -> "Parar" }, pending)
        }
        return builder.build()
    }
    private fun notifyResult(value: RecordingSessionEntity) {
        val intent = Intent(this, SecurityRecordingActivity::class.java)
            .putExtra(SecurityRecordingActivity.EXTRA_OPEN_SESSION, value.id)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.action = "securityrecording.result.${value.id}"
        val pending = PendingIntent.getActivity(this, NOTIFICATION_ID + 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val result = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_recording_notification).setContentTitle("CalcMot")
            .setContentText(if(value.phase == "VERIFIED") "Gravação encerrada · ver resultado" else "Gravação encerrada · verificar resultado")
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setAutoCancel(true).setOnlyAlertOnce(true)
            .setContentIntent(pending).build()
        try { NotificationManagerCompat.from(this).notify(NOTIFICATION_ID + 1, result) }
        catch (_: SecurityException) { /* A revoked notification permission never changes media truth. */ }
    }
    private fun createChannel() { if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java)
        .createNotificationChannel(NotificationChannel(CHANNEL_ID, "Gravação de segurança", NotificationManager.IMPORTANCE_LOW)) }
    private fun Intent.toCommand(): RecordingCommand? = runCatching {
        RecordingCommand(getStringExtra(EXTRA_COMMAND_ID) ?: return null, getLongExtra(EXTRA_EXPECTED_REVISION, -1),
            RecordingCommand.Action.valueOf(getStringExtra(EXTRA_ACTION) ?: return null), getStringExtra(EXTRA_SESSION_ID))
    }.getOrNull()

    enum class FailureCode(val persistedCode: String, val userMessage: String) {
        PERMISSION("permission", "Revise as permissões de câmera e microfone."),
        CAMERA("camera", "A câmera selecionada deixou de estar disponível."),
        MICROPHONE("microphone", "O microfone não pôde ser usado."),
        FOREGROUND("foreground", "Não foi possível manter o controle da gravação em primeiro plano."),
        STORAGE("storage", "O armazenamento local não pôde concluir a operação."),
        INSUFFICIENT_STORAGE("insufficient_storage", "Libere espaço no aparelho para gravar."),
        THERMAL("thermal", "A gravação foi interrompida por aquecimento."),
        CANCELLED("cancelled", "A preparação foi cancelada."),
        PREPARATION_TIMEOUT("preparation_timeout", "A câmera demorou demais para responder. Tente novamente."),
        FINALIZE("finalize", "O fechamento da gravação não pôde ser confirmado."),
        INTEGRITY("integrity", "Não foi possível confirmar um trecho com áudio e vídeo.")
    }
    companion object {
        const val EXTRA_COMMAND_ID = "securityrecording.command_id"
        const val EXTRA_EXPECTED_REVISION = "securityrecording.expected_revision"
        const val EXTRA_ACTION = "securityrecording.action"
        const val EXTRA_SESSION_ID = "securityrecording.session_id"
        const val EXTRA_START_TOKEN = "securityrecording.start_token"
        const val EXTRA_LENS = "securityrecording.lens"
        const val EXTRA_SEGMENT_MINUTES = "securityrecording.segment_minutes"
        const val EXTRA_ORIENTATION = "securityrecording.orientation"
        const val CHANNEL_ID = "calcmot_security_recording"
        private const val NOTIFICATION_ID = 731
        private val OPENING = setOf("PREPARING", "ROTATING", "RESUMING")
        private val STOPPABLE = setOf("PREPARING", "RECORDING", "PAUSING", "PAUSED", "RESUMING", "ROTATING")
        private val ACTIVE = STOPPABLE + "FINALIZING"
        fun startConfirmed(context: Context, setup: RecordingSetup): Boolean {
            val activity = context.resumedActivity() as? SecurityRecordingActivity ?: return false
            if (!SecurityRecordingBootstrap.isReady || !permissionsReady(context) || RecordingRuntimeStore.snapshot.value.phase.name in ACTIVE) return false
            @Suppress("DEPRECATION") val orientation = activity.windowManager.defaultDisplay.rotation
            val intent = commandIntent(context, RecordingCommand(expectedSessionRevision = 0, action = RecordingCommand.Action.START))
                .putExtra(EXTRA_START_TOKEN, RecordingStartAuthorization.issue()).putExtra(EXTRA_LENS, setup.lens.name)
                .putExtra(EXTRA_SEGMENT_MINUTES, setup.segmentMinutes).putExtra(EXTRA_ORIENTATION, orientation)
            if (!RecordingResourceCoordinator.acquireCapture()) return false
            RecordingRuntimeStore.publishOutcome(null)
            return runCatching { ContextCompat.startForegroundService(context, intent) }.isSuccess.also {
                if (!it) RecordingResourceCoordinator.releaseCapture()
            }
        }
        fun stop(context: Context, snapshot: RecordingRuntimeSnapshot) = send(context, snapshot, RecordingCommand.Action.STOP)
        fun send(context: Context, snapshot: RecordingRuntimeSnapshot, action: RecordingCommand.Action) {
            if (snapshot.sessionId == null) return
            runCatching { context.startService(commandIntent(context, RecordingCommand(expectedSessionRevision = snapshot.sessionRevision,
                action = action, sessionId = snapshot.sessionId))) }.onFailure {
                RecordingRuntimeStore.publishOutcome("Não foi possível enviar o comando. Reabra a gravação.")
            }
        }
        fun permissionsReady(context: Context): Boolean {
            if (listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO).any {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }) return false
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
            if (Build.VERSION.SDK_INT >= 26 && context.getSystemService(NotificationManager::class.java)
                    .getNotificationChannel(CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE) return false
            return true
        }
        private fun commandIntent(context: Context, command: RecordingCommand) = Intent(context, RecordingSessionService::class.java)
            .putExtra(EXTRA_COMMAND_ID, command.commandId).putExtra(EXTRA_EXPECTED_REVISION, command.expectedSessionRevision)
            .putExtra(EXTRA_ACTION, command.action.name).putExtra(EXTRA_SESSION_ID, command.sessionId)
        private fun formatTime(ms: Long?): String = if (ms == null) "Ainda não confirmado" else "%d:%02d".format(ms / 60_000, ms / 1_000 % 60)
    }
}

private fun Context.resumedActivity(): Activity? {
    var value: Context = this
    while (value is ContextWrapper) {
        if (value is Activity) return value.takeIf { (it as? LifecycleOwner)?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true }
        val next = value.baseContext
        if (next === value) return null
        value = next
    }
    return null
}
