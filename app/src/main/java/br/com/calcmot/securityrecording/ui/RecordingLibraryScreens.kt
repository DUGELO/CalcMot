package br.com.calcmot.securityrecording.ui

import android.Manifest
import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.StatFs
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import br.com.calcmot.securityrecording.application.*
import br.com.calcmot.securityrecording.data.*
import br.com.calcmot.securityrecording.domain.RecordingRetention
import br.com.calcmot.securityrecording.ui.components.*
import br.com.calcmot.ui.UiTestTags
import br.com.calcmot.ui.design.tokens.*
import java.io.File
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch

private sealed interface LibraryState {
    data object Loading : LibraryState
    data class Content(val sessions: List<RecordingSessionEntity>) : LibraryState
    data object Failure : LibraryState
}

@Composable
internal fun RecordingHistoryRoute(onBack: () -> Unit, onNew: () -> Unit, onStorage: () -> Unit, onOpenSession: (String) -> Unit) {
    val context = LocalContext.current
    val repository = remember { RecordingFilesRepository(context) }
    var reload by remember { mutableIntStateOf(0) }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var showFilter by remember { mutableStateOf(false) }
    val state by produceState<LibraryState>(LibraryState.Loading, reload) {
        value = LibraryState.Loading
        if(!SecurityRecordingBootstrap.awaitReady(context)) { value = LibraryState.Failure; return@produceState }
        RecordingOperationRunner.start { runCatching { repository.cleanup() } }
        repository.observeSessions().catch { value = LibraryState.Failure }.collect { value = LibraryState.Content(it) }
    }
    val now by rememberRecordingClock((state as? LibraryState.Content)?.sessions?.mapNotNull { it.expiresAtUtcMs }.orEmpty())
    val runtime by RecordingRuntimeStore.snapshot.collectAsStateWithLifecycle()
    RecordingScaffold("Gravações", onBack, modifier = Modifier.testTag(UiTestTags.SECURITY_RECORDING_LIBRARY_SCREEN),
        navigationActions = { TextButton(onClick = onStorage) { Text("Espaço") } }) { padding ->
        when(val current = state) {
            LibraryState.Loading -> RecordingScrollContent(padding) { RecordingLoading("Carregando gravações…") }
            LibraryState.Failure -> RecordingScrollContent(padding) {
                RecordingIssueInline("Não foi possível carregar suas gravações.")
                RecordingActionButton(RecordingAction("Tentar novamente", { reload++ }))
            }
            is LibraryState.Content -> {
                val beginning = if(filter == 0) Long.MIN_VALUE else dayStart(now) - (if(filter == 7) 6L else 0L) * RecordingRetention.DAY_MS
                val sessions = current.sessions.filter { it.createdAtUtcMs >= beginning }
                LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(CalcMotSpacing.ScreenHorizontal)) {
                    if(runtime.phase.name in ACTIVE_PHASES) item("active") {
                        RecordingSettingRow("Gravação em andamento", "Voltar aos controles", onClick = onNew)
                    }
                    item("filter") { RecordingSettingRow("Período", when(filter) { 1 -> "Hoje"; 7 -> "Últimos 7 dias"; else -> "Todas" }, onClick = { showFilter = true }) }
                    if(sessions.isEmpty()) item("empty") {
                        RecordingEmptyState(if(filter == 0) "Nenhuma gravação ainda" else "Nenhuma gravação neste período",
                            if(filter == 0) "Suas sessões aparecerão aqui depois de encerrar." else "Escolha outro período para consultar.",
                            RecordingAction(if(filter == 0) "Nova gravação" else "Limpar filtro", { if(filter == 0) onNew() else filter = 0 }))
                    }
                    sessions.groupBy { dayStart(it.createdAtUtcMs) }.forEach { (day, group) ->
                        item("day:$day") { Text(dayLabel(day, now), Modifier.padding(vertical = CalcMotSpacing.Md), color = CalcMotColors.TextPrimary, style = CalcMotTypography.SectionTitle) }
                        items(group, key = { it.id }) { session ->
                            val thumbnail by rememberRecordingThumbnail(repository, session, now)
                            RecordingSessionRow(RecordingSessionItem(session.id,
                                DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(session.createdAtUtcMs)),
                                session.capturedDurationMs?.let(::recordingDuration) ?: "Ainda não confirmado",
                                recordingAvailability(session, now),
                                interruption = sessionIssue(session), thumbnail = thumbnail), { onOpenSession(session.id) })
                        }
                    }
                }
            }
        }
    }
    if(showFilter) AlertDialog(onDismissRequest = { showFilter = false }, title = { Text("Período") },
        text = { Column { listOf(0 to "Todas", 1 to "Hoje", 7 to "Últimos 7 dias").forEach { (value, label) ->
            TextButton(onClick = { filter = value; showFilter = false }) { Text(label) }
        } } }, confirmButton = { TextButton(onClick = { showFilter = false }) { Text("Cancelar") } })
}

@Composable
internal fun RecordingDetailRoute(sessionId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { RecordingFilesRepository(context) }
    val scope = rememberCoroutineScope()
    var session by remember { mutableStateOf<RecordingSessionEntity?>(null) }
    var loading by remember { mutableStateOf(true) }
    var issue by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var gaps by remember { mutableStateOf<List<RecordingGapEntity>>(emptyList()) }
    var hasConfirmedMedia by remember { mutableStateOf(false) }
    var action by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var selectedDays by remember { mutableIntStateOf(3) }
    LaunchedEffect(sessionId, reload) {
        loading = true
        if(!SecurityRecordingBootstrap.awaitReady(context)) { issue = "Não foi possível confirmar o resultado. Tente novamente."; loading = false; return@LaunchedEffect }
        repository.observeSession(sessionId).catch { issue = "Não foi possível carregar esta gravação."; loading = false }.collect {
            val confirmed = repository.segments(sessionId).any { segment -> segment.verified }
            gaps = repository.gaps(sessionId)
            session = it; hasConfirmedMedia = confirmed; loading = false
        }
    }
    val now by rememberRecordingClock(listOfNotNull(session?.expiresAtUtcMs))
    val snapshot by RecordingRuntimeStore.snapshot.collectAsStateWithLifecycle()
    val available = hasConfirmedMedia && session?.let { repository.isAvailable(it, now) } == true
    var releasePlayback by remember { mutableStateOf<(suspend () -> Unit)?>(null) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val canPlay = available && snapshot.phase.name !in ACTIVE_PHASES
    fun execute(name: String, operation: suspend () -> Unit) {
        if(busy) return
        busy = true; issue = null; action = null
        val work = RecordingOperationRunner.start(operation)
        scope.launch {
            try { work.await() }
            catch(cancelled: CancellationException) { throw cancelled }
            catch(_: Exception) { issue = if(name == "excluir") "Uma parte da exclusão ficou pendente. Confira o estado antes de tentar novamente." else "Não foi possível $name. A gravação original foi preservada; verifique o estado e tente novamente." }
            finally { busy = false }
        }
    }
    val savePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if(granted) execute("salvar na galeria") { repository.saveToGallery(sessionId) }
        else issue = "Autorize salvar na galeria para continuar. Sua gravação permanece privada."
    }
    RecordingScaffold("Gravação", onBack, accessibleTitle = "Detalhe da gravação", modifier = Modifier.testTag(UiTestTags.SECURITY_RECORDING_PLAYER_SCREEN)) { padding ->
        RecordingScrollContent(padding) {
            val value = session
            when {
                loading -> RecordingLoading("Carregando gravação…")
                value == null -> { RecordingIssueInline(issue ?: "Gravação indisponível."); RecordingActionButton(RecordingAction("Tentar novamente", { reload++ })) }
                else -> {
                    RecordingDetailContent(recordingDate(value.createdAtUtcMs),
                        if(available) "Gravação disponível" else "Resultado da gravação",
                        value.capturedDurationMs?.let(::recordingDuration) ?: "Duração ainda não confirmada",
                        recordingAvailability(value, now), interruption = sessionIssue(value),
                        primaryAction = if(available) RecordingAction(if(busy) "Preparando cópia…" else "Salvar na galeria", {
                            if(Build.VERSION.SDK_INT <= 28 && !recordingHasPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) savePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            else execute("salvar na galeria") { repository.saveToGallery(sessionId) }
                        }, enabled = !busy) else null,
                        secondaryAction = if(available) RecordingAction("Compartilhar", { action = "share" }, enabled = !busy) else null,
                        player = {
                            if(canPlay) RecordingPlaybackHost(repository, value) { releasePlayback = it } else RecordingIssueInline(
                                if(snapshot.phase.name in ACTIVE_PHASES) "Encerre a captura para reproduzir." else "A cópia temporária não está disponível para reprodução.")
                        })
                    gaps.forEachIndexed { index, gap -> Text("Intervalo ${index + 1}: ${recordingDuration(gap.durationMs)} · ${if(gap.reason == "pause") "pausa" else "interrupção"}", style = CalcMotTypography.Body, color = CalcMotColors.Warning) }
                    if(busy || value.galleryState == "COPYING" || value.exportState == "BUILDING") RecordingLoading("Preparando cópia…")
                    issue?.let { RecordingIssueInline(it) }
                    if(value.galleryState == "PUBLISHED" && value.galleryUri != null) RecordingActionButton(RecordingAction("Abrir na galeria", {
                        val uri = Uri.parse(value.galleryUri)
                        scope.launch {
                            val readable = withContext(Dispatchers.IO) { br.com.calcmot.securityrecording.platform.RecordingGalleryAdapter(context).isReadable(uri) }
                            if(readable) runCatching { context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "video/mp4").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
                                .onFailure { issue = "Nenhum aplicativo disponível conseguiu abrir a cópia." }
                            else issue = "A cópia na galeria não está mais disponível."
                        }
                    }), outlined = true)
                    if(available && value.retentionDays != null && value.retentionDays < 30) RecordingSettingRow("Manter por mais tempo", "Alterar prazo total", onClick = {
                        selectedDays = RecordingRetention.options.first { it > value.retentionDays }; action = "extend"
                    })
                    if(value.temporaryAvailability != "DELETED") RecordingSettingRow("Excluir cópia temporária", "A galeria permanece", onClick = if(busy) null else ({ action = "delete" }))
                }
            }
        }
    }
    val value = session
    if(action == "share") AlertDialog(onDismissRequest = { action = null }, title = { Text("Compartilhar não salva esta gravação") },
        text = { Text("O aplicativo escolhido poderá manter ou redistribuir a cópia. O prazo da gravação temporária permanece igual.") },
        confirmButton = { TextButton(onClick = {
            if(!busy) {
                busy = true; issue = null; action = null
                val work = RecordingOperationRunner.start { repository.prepareShare(sessionId) }
                scope.launch {
                    try {
                        val uri = work.await()
                        if(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            repository.markShareLaunch(sessionId)
                            if(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                                val intent = Intent(Intent.ACTION_SEND).setType("video/mp4").putExtra(Intent.EXTRA_STREAM, uri).apply {
                                    clipData = ClipData.newUri(context.contentResolver, "Gravação", uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Compartilhar gravação"))
                            }
                        }
                    } catch(cancelled: CancellationException) { throw cancelled }
                    catch(_: Exception) { issue = "Não foi possível compartilhar. Tente novamente." }
                    finally { busy = false }
                }
            }
        }) { Text("Continuar para compartilhar") } }, dismissButton = { TextButton(onClick = { action = null }) { Text("Cancelar") } })
    if(action == "delete") AlertDialog(onDismissRequest = { action = null }, title = { Text("Excluir gravação?") },
        text = { Text("A cópia temporária será excluída. Uma cópia já salva na galeria permanece.") },
        confirmButton = { TextButton(onClick = { val release = releasePlayback; execute("excluir") { withContext(Dispatchers.Main) { release?.invoke() }; repository.deletePrivate(sessionId) } }) { Text("Excluir gravação") } },
        dismissButton = { TextButton(onClick = { action = null }) { Text("Cancelar") } })
    if(action == "extend" && value != null) AlertDialog(onDismissRequest = { action = null }, title = { Text("Manter por mais tempo") },
        text = { Column {
            Text("O prazo conta desde o encerramento.")
            RecordingRetention.options.filter { it > (value.retentionDays ?: 30) }.forEach { days -> Row(Modifier.fillMaxWidth().clickable { selectedDays = days }) {
                RadioButton(days == selectedDays, onClick = { selectedDays = days }); Text(recordingRetentionLabel(days), Modifier.padding(top = CalcMotSpacing.Md))
            } }
            value.finalizedAtUtcMs?.let { Text("Até ${recordingDate(RecordingRetention.expiresAt(it, selectedDays))}") }
        } }, confirmButton = { TextButton(onClick = { execute("alterar o prazo") { repository.extend(sessionId, selectedDays) } }) { Text("Confirmar prazo") } },
        dismissButton = { TextButton(onClick = { action = null }) { Text("Cancelar") } })
}

@Composable
private fun RecordingPlaybackHost(repository: RecordingFilesRepository, session: RecordingSessionEntity, onRelease: ((suspend () -> Unit)?) -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var foreground by remember { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var issue by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var position by rememberSaveable(session.id) { mutableLongStateOf(0L) }
    var index by rememberSaveable(session.id) { mutableIntStateOf(0) }
    var releaseCurrent by remember { mutableStateOf<(() -> Unit)?>(null) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if(event == Lifecycle.Event.ON_STOP) { releaseCurrent?.invoke(); foreground = false }
            if(event == Lifecycle.Event.ON_START) foreground = true
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(session.id, foreground) {
        var lease = false
        var disposed = false
        var job: Job? = null
        fun release() {
            disposed = true; job?.cancel()
            player?.let { index = it.currentMediaItemIndex.coerceAtLeast(0); position = it.currentPosition; it.pause(); it.release() }
            player = null
            if(job?.isCompleted != false && lease) { RecordingResourceCoordinator.releaseReader(session.id); lease = false }
        }
        releaseCurrent = ::release
        onRelease { release(); job?.join() }
        job = if(foreground) scope.launch {
            try {
                check(RecordingResourceCoordinator.acquireReader(session.id))
                lease = true
                val segments = repository.playableSegments(session.id)
                if(segments.size < repository.segments(session.id).size) issue = "Alguns trechos estão ausentes ou alterados. Reproduzindo somente os trechos confirmados."
                if(disposed) return@launch
                val native = ExoPlayer.Builder(context).build()
                player = native
                native.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) { loading = state == Player.STATE_BUFFERING || state == Player.STATE_IDLE }
                    override fun onPlayerError(error: PlaybackException) { issue = "Não foi possível reproduzir este trecho."; loading = false }
                })
                native.setMediaItems(segments.map { MediaItem.fromUri(Uri.fromFile(File(it.path))) })
                native.seekTo(index.coerceIn(0, segments.lastIndex), position)
                native.playWhenReady = false
                native.prepare()
            } catch(cancelled: CancellationException) { throw cancelled }
            catch(_: Exception) { issue = "Não foi possível confirmar os trechos para reprodução."; loading = false }
            finally { if((disposed || player == null) && lease) { RecordingResourceCoordinator.releaseReader(session.id); lease = false } }
        } else null
        onDispose {
            release()
            releaseCurrent = null
            onRelease(null)
        }
    }
    br.com.calcmot.securityrecording.ui.components.RecordingPlayer(player)
    if(loading) RecordingLoading("Carregando vídeo…")
    issue?.let { RecordingIssueInline(it) }
}

private data class StorageItem(val session: RecordingSessionEntity, val bytes: Long, val eligible: Boolean)
private data class StorageSnapshot(val used: Long, val free: Long, val checkedAt: Long, val sessions: List<StorageItem>)

@Composable
internal fun RecordingStorageRoute(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val repository = remember { RecordingFilesRepository(context) }
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf<StorageSnapshot?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var issue by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    RecordingResumeEffect { refresh++ }
    LaunchedEffect(refresh) {
        runCatching { withContext(Dispatchers.IO) {
            check(SecurityRecordingBootstrap.awaitReady(context))
            val root = File(context.filesDir, "security-recording")
            fun bytes(file: File) = if(file.exists()) file.walkTopDown().filter { it.isFile }.sumOf { it.length() } else 0L
            val rows = repository.databaseSessions().map { session ->
                val files = bytes(File(root, "sessions/${session.id}")) + listOf("work/${session.id}.mp4", "work/${session.id}.pending").sumOf { File(root, it).length() }
                StorageItem(session, files, session.phase in RecordingFilesRepository.TERMINAL && repository.operation(session.id) == null && !RecordingResourceCoordinator.isCapturing())
            }.filter { it.bytes > 0 }.sortedByDescending { it.bytes }
            StorageSnapshot(bytes(root), StatFs(context.filesDir.absolutePath).availableBytes, System.currentTimeMillis(), rows)
        } }.onSuccess { snapshot = it; selected = selected.filter { id -> it.sessions.any { row -> row.session.id == id && row.eligible } } }
            .onFailure { issue = "Não foi possível verificar o espaço. Tente novamente." }
    }
    RecordingScaffold("Armazenamento", onBack) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(CalcMotSpacing.ScreenHorizontal)) {
            item("summary") {
                val value = snapshot
                if(value == null) RecordingLoading("Calculando espaço…") else {
                    Text("Gravações privadas: ${recordingBytes(value.used)}", color = CalcMotColors.TextPrimary, style = CalcMotTypography.CardTitle)
                    Text("Livre no aparelho: ${recordingBytes(value.free)}", color = CalcMotColors.TextSecondary, style = CalcMotTypography.Body)
                    Text("Verificado em ${recordingDate(value.checkedAt)}", color = CalcMotColors.TextMuted, style = CalcMotTypography.Body)
                    Text("Excluir a cópia temporária preserva as cópias já salvas na galeria. Arquivos compartilhados podem ocupar espaço até o fim do acesso temporário.", color = CalcMotColors.TextSecondary, style = CalcMotTypography.Body)
                    if(value.sessions.isEmpty()) RecordingEmptyState("Nenhuma gravação temporária ocupa espaço", "Você pode voltar à gravação.", RecordingAction("Voltar", onBack))
                }
                issue?.let { RecordingIssueInline(it) }
                if(snapshot == null && issue != null) RecordingActionButton(RecordingAction("Tentar novamente", { refresh++ }))
            }
            items(snapshot?.sessions.orEmpty(), key = { it.session.id }) { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = CalcMotSpacing.Md)) {
                    Checkbox(row.session.id in selected, enabled = row.eligible && !busy, onCheckedChange = { checked ->
                        selected = if(checked) selected + row.session.id else selected - row.session.id
                    })
                    Column(Modifier.weight(1f).clickable(role = Role.Button) { onOpen(row.session.id) }) {
                        Text(recordingDate(row.session.createdAtUtcMs), color = CalcMotColors.TextPrimary, style = CalcMotTypography.BodyStrong)
                        Text(recordingBytes(row.bytes), color = CalcMotColors.TextSecondary, style = CalcMotTypography.Body)
                        if(!row.eligible) Text("Gravação ou operação em andamento", color = CalcMotColors.Warning, style = CalcMotTypography.Body)
                    }
                }
                HorizontalDivider(color = CalcMotColors.BorderSubtle)
            }
            item("delete") {
                if(selected.isNotEmpty()) RecordingActionButton(RecordingAction(if(busy) "Excluindo…" else "Excluir ${selected.size} gravações", { confirm = true }, enabled = !busy, tone = RecordingActionTone.STOP))
            }
        }
    }
    if(confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Excluir ${selected.size} gravações?") },
        text = { Text("As cópias temporárias serão excluídas. Uma cópia já salva na galeria permanece.") },
        confirmButton = { TextButton(onClick = {
            confirm = false; busy = true
            val ids = selected.toList()
            val work = RecordingOperationRunner.start {
                var completed = 0
                ids.forEach { if(runCatching { repository.deletePrivate(it) }.isSuccess) completed++ }
                completed
            }
            scope.launch { try {
                val completed = work.await()
                issue = "$completed excluídas · ${ids.size - completed} pendentes"
                selected = emptyList(); refresh++
            } finally { busy = false } }
        }) { Text("Excluir") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancelar") } })
}

@Composable
internal fun rememberRecordingClock(deadlines: List<Long>): State<Long> = produceState(System.currentTimeMillis(), deadlines) {
    value = System.currentTimeMillis()
    while(true) {
        val midnight = Calendar.getInstance().apply { timeInMillis = value; add(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        val deadline = (deadlines + deadlines.map { it - 3_600_000L } + midnight).filter { it > value }.minOrNull()!!
        delay((deadline - System.currentTimeMillis()).coerceAtLeast(1))
        value = System.currentTimeMillis()
    }
}

@Composable
private fun rememberRecordingThumbnail(repository: RecordingFilesRepository, session: RecordingSessionEntity, now: Long): State<ImageBitmap?> = produceState<ImageBitmap?>(null, session.id, session.revision, repository.isAvailable(session, now)) {
    if(!repository.isAvailable(session, now) || !RecordingResourceCoordinator.acquireReader(session.id)) return@produceState
    try {
        value = withContext(Dispatchers.IO) {
            val file = repository.thumbnailFile(session.id) ?: return@withContext null
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(file.absolutePath)
                val frame = if(Build.VERSION.SDK_INT >= 27) retriever.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 192, 144)
                    else retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                frame?.let { bitmap ->
                    val thumbnail = Bitmap.createScaledBitmap(bitmap, 192, 144, true)
                    if(thumbnail !== bitmap) bitmap.recycle()
                    thumbnail.asImageBitmap()
                }
            } finally { retriever.release() }
        }
    } catch(cancelled: CancellationException) { throw cancelled }
    catch(_: Exception) { value = null }
    finally { RecordingResourceCoordinator.releaseReader(session.id) }
}

private fun recordingAvailability(session: RecordingSessionEntity, now: Long): RecordingAvailabilityState = RecordingAvailabilityState(
    privateLabel = when {
        session.temporaryAvailability == "DELETED" -> "Cópia temporária excluída"
        session.temporaryAvailability == "EXPIRED" || session.expiresAtUtcMs?.let { it <= now } == true -> "Prazo da cópia temporária encerrado"
        session.phase in ACTIVE_PHASES -> "Resultado ainda não confirmado"
        session.expiresAtUtcMs != null -> "Temporário · até ${recordingDate(session.expiresAtUtcMs)}"
        else -> "Prazo ainda não confirmado"
    }, galleryLabel = when(session.galleryState) { "PUBLISHED" -> "Cópia na galeria"; "COPYING" -> "Salvando na galeria…"; "MISSING" -> "Cópia na galeria indisponível"; else -> null },
    expiresSoon = session.temporaryAvailability == "AVAILABLE" && session.expiresAtUtcMs?.let { it > now && it - now <= 3_600_000L } == true)
private fun sessionIssue(session: RecordingSessionEntity): String? = when {
    session.phase in ACTIVE_PHASES -> "Resultado ainda não confirmado"
    session.failureCode != null -> recordingFailureMessage(session.failureCode)
    else -> null
}
private fun dayStart(time: Long): Long = Calendar.getInstance().apply { timeInMillis = time; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
private fun dayLabel(day: Long, now: Long) = when(day) { dayStart(now) -> "Hoje"; dayStart(now - RecordingRetention.DAY_MS) -> "Ontem"; else -> DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(day)) }
private fun recordingBytes(bytes: Long) = if(bytes >= 1_073_741_824L) "%.1f GB".format(bytes / 1_073_741_824.0) else "%.1f MB".format(bytes / 1_048_576.0)

@Composable fun SecurityRecordingLibraryRoute(onBack: () -> Unit, onOpenSession: (String) -> Unit) = RecordingHistoryRoute(onBack, onBack, onBack, onOpenSession)
@Composable fun SecurityRecordingPlayerRoute(sessionId: String, onBack: () -> Unit) = RecordingDetailRoute(sessionId, onBack)
internal fun recordingFailureMessage(code: String?): String = when(code) {
    "interrupted_recovered", "unexpected_end" -> "Interrompida; trechos disponíveis"
    "interrupted_no_valid_segment" -> "Interrompida sem arquivo válido"
    "permission" -> "Permissões indisponíveis"
    "camera" -> "Câmera indisponível"
    "microphone" -> "Microfone indisponível"
    "foreground" -> "Gravação em primeiro plano bloqueada"
    "storage", "insufficient_storage" -> "Armazenamento insuficiente"
    "thermal" -> "Interrompida por temperatura"
    "cancelled" -> "Preparação cancelada"
    "preparation_timeout" -> "A câmera demorou demais para responder"
    "finalize" -> "Encerramento inesperado; confira os trechos disponíveis"
    "integrity" -> "Um trecho não pôde ser confirmado"
    else -> "Sessão indisponível para reprodução"
}
