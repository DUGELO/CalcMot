package br.com.calcmot.securityrecording.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.lifecycle.compose.*
import androidx.navigation.compose.*
import br.com.calcmot.securityrecording.application.*
import br.com.calcmot.securityrecording.data.*
import br.com.calcmot.securityrecording.domain.*
import br.com.calcmot.securityrecording.platform.RecordingSessionService
import br.com.calcmot.securityrecording.ui.components.*
import br.com.calcmot.ui.UiTestTags
import br.com.calcmot.ui.design.tokens.*
import br.com.calcmot.ui.theme.MetricaTheme
import kotlinx.coroutines.launch

class SecurityRecordingActivity : ComponentActivity() {
    private var requestedSession by mutableStateOf<String?>(null)
    private var requestRevision by mutableIntStateOf(0)
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestedSession = intent.getStringExtra(EXTRA_OPEN_SESSION)
        setContent { MetricaTheme { RecordingExperience({ finish() }, requestedSession, requestRevision) } }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent); setIntent(intent); requestRevision++
        requestedSession = intent.getStringExtra(EXTRA_OPEN_SESSION)
    }
    companion object { const val EXTRA_OPEN_SESSION = "securityrecording.open_session" }
}

@Composable
internal fun RecordingExperience(onClose: () -> Unit, requestedSession: String? = null, requestRevision: Int = 0) {
    val nav = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val setup = remember { RecordingSetupRepository(context) }
    var prefs by remember { mutableStateOf<RecordingPreferences?>(null) }
    var issue by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    RecordingResumeEffect { refresh++ }
    LaunchedEffect(refresh) {
        runCatching { setup.read() }.onSuccess { prefs = it; issue = null }
            .onFailure { issue = "Não foi possível carregar os ajustes. Tente novamente." }
        SecurityRecordingBootstrap.awaitReady(context)
    }
    LaunchedEffect(requestedSession, requestRevision) {
        if(requestedSession != null) nav.navigate("detail/${Uri.encode(requestedSession)}") { launchSingleTop = true }
        else if(requestRevision > 0) nav.navigate("main") { launchSingleTop = true; popUpTo("main") { inclusive = false } }
    }
    fun go(route: String) { nav.navigate(route) { launchSingleTop = true } }
    fun back() { if (!nav.popBackStack()) onClose() }
    fun persist(change: (RecordingPreferences) -> RecordingPreferences, done: () -> Unit = {}) {
        scope.launch {
            runCatching { setup.update(change) }.onSuccess { prefs = it; done() }
                .onFailure { issue = "Não foi possível salvar o ajuste. Tente novamente." }
        }
    }
    NavHost(nav, startDestination = "main") {
        composable("main") { RecordingMainRoute(onClose, prefs, issue,
            onPrepare = {
                when {
                    prefs?.disclosureVersion != RecordingSetupRepository.DISCLOSURE_VERSION -> go("first-use")
                    !RecordingSessionService.permissionsReady(context) -> go("permissions")
                    else -> go("framing")
                }
            }, onFraming = { go("framing") }, onHistory = { go("library") }, onSettings = { go("settings") },
            onStorage = { go("storage") }, onDetail = { go("detail/${Uri.encode(it)}") }, onRetry = { refresh++ }) }
        composable("first-use") { RecordingFirstUseRoute(::back,
            onPrepare = { persist({ it.copy(disclosureVersion = RecordingSetupRepository.DISCLOSURE_VERSION) }) { go("permissions") } }) }
        composable("permissions") { RecordingPermissionsRoute(::back, prefs,
            onRememberRequest = { permission -> runCatching { setup.update { it.copy(requestedPermissions = it.requestedPermissions + permission) } }.onSuccess { prefs = it }.isSuccess },
            onContinue = { if (prefs?.framed == true) go("main") else go("framing") }) }
        composable("framing") { RecordingFramingRoute(::back, prefs,
            onConfirmed = { lens, orientation -> persist({ it.copy(lens = lens, framed = true, framedOrientation = orientation) }) {
                nav.popBackStack("main", false)
            } }, onPermissions = { go("permissions") }) }
        composable("settings") { RecordingSettingsRoute(::back, prefs, issue,
            onSegment = { minutes -> persist({ it.copy(segmentMinutes = minutes) }) },
            onRetention = { days -> persist({ it.copy(retentionDays = days) }) },
            onFraming = { go("framing") }, onPermissions = { go("permissions") }, onStorage = { go("storage") }) }
        composable("library") { RecordingHistoryRoute(::back, { go("main") }, { go("storage") }, { go("detail/${Uri.encode(it)}") }) }
        composable("detail/{id}") { entry -> RecordingDetailRoute(entry.arguments?.getString("id") ?: "", ::back) }
        composable("storage") { RecordingStorageRoute(::back, { go("detail/${Uri.encode(it)}") }) }
    }
}

@Composable
private fun RecordingMainRoute(onBack: () -> Unit, prefs: RecordingPreferences?, issue: String?,
    onPrepare: () -> Unit, onFraming: () -> Unit, onHistory: () -> Unit, onSettings: () -> Unit,
    onStorage: () -> Unit, onDetail: (String) -> Unit, onRetry: () -> Unit) {
    val context = LocalContext.current
    val snapshot by RecordingRuntimeStore.snapshot.collectAsStateWithLifecycle()
    val bootstrap by SecurityRecordingBootstrap.state.collectAsStateWithLifecycle()
    LaunchedEffect(bootstrap) {
        if(bootstrap == SecurityRecordingBootstrap.State.CHECKING && !RecordingResourceCoordinator.isCapturing())
            SecurityRecordingBootstrap.awaitReady(context)
    }
    val outcome by RecordingRuntimeStore.outcome.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var dismissedResult by rememberSaveable { mutableStateOf<String?>(null) }
    var starting by remember { mutableStateOf(false) }
    val active = snapshot.phase.name in ACTIVE_PHASES
    val result = snapshot.sessionId != null && snapshot.sessionId != dismissedResult && snapshot.phase in setOf(RecordingPhase.VERIFIED, RecordingPhase.FAILED)
    var resultSession by remember { mutableStateOf<RecordingSessionEntity?>(null) }
    val resultNow by rememberRecordingClock(listOfNotNull(resultSession?.expiresAtUtcMs))
    LaunchedEffect(starting) {
        if(starting) { kotlinx.coroutines.delay(30_000); starting = false; RecordingRuntimeStore.publishOutcome("O início não foi confirmado. Confira os recursos e tente novamente.") }
    }
    LaunchedEffect(outcome) { if(outcome != null) starting = false }
    LaunchedEffect(snapshot.sessionId, snapshot.phase) {
        resultSession = snapshot.sessionId?.let { RecordingFilesRepository(context).session(it) }
        if (snapshot.phase != RecordingPhase.IDLE) starting = false
    }
    RecordingScaffold("Gravação", onBack = {
        if (active) Toast.makeText(context, "Controle a gravação pela notificação do CalcMot.", Toast.LENGTH_LONG).show()
        onBack()
    }, accessibleTitle = "Gravação de segurança", modifier = Modifier.testTag(UiTestTags.SECURITY_RECORDING_HUB_SCREEN),
        navigationActions = { RecordingNavigationActions(onHistory, onSettings, settingsEnabled = !active) }) { padding ->
        RecordingScrollContent(padding) {
            when {
                active -> RecordingActivePresentation(snapshot.copy(failureMessage = snapshot.failureMessage ?: outcome),
                    onStop = { RecordingSessionService.stop(context, snapshot) },
                    onPause = { RecordingSessionService.send(context, snapshot, RecordingCommand.Action.PAUSE) },
                    onResume = { RecordingSessionService.send(context, snapshot, RecordingCommand.Action.RESUME) })
                bootstrap != SecurityRecordingBootstrap.State.READY -> RecordingOperationalContent(
                    RecordingHeaderState(RecordingVisualState.PENDING,
                        if (bootstrap == SecurityRecordingBootstrap.State.CHECKING) "Verificando gravações anteriores…" else "Não foi possível confirmar o estado"),
                    primaryAction = if (bootstrap == SecurityRecordingBootstrap.State.FAILED) RecordingAction("Verificar estado", onRetry) else null)
                prefs == null -> { RecordingLoading("Carregando ajustes…"); issue?.let { RecordingIssueInline(it) } }
                result -> {
                    val value = resultSession
                    val confirmed = snapshot.phase == RecordingPhase.VERIFIED
                    RecordingOperationalContent(RecordingHeaderState(
                        if (confirmed) RecordingVisualState.READY else RecordingVisualState.FAILURE,
                        if (confirmed) { if(value?.expiresAtUtcMs?.let { it <= resultNow } == true || value?.temporaryAvailability in setOf("EXPIRED", "DELETED")) "Cópia temporária indisponível" else "Gravação disponível" } else if (value?.failureCode == "cancelled") "Início cancelado" else "Nenhum trecho pôde ser confirmado",
                        capturedDuration = snapshot.capturedDurationMs?.let(::recordingDuration)),
                        retentionLabel = value?.expiresAtUtcMs?.let { "Temporário · até ${recordingDate(it)}" },
                        issue = snapshot.failureMessage,
                        primaryAction = RecordingAction(if (confirmed) "Ver gravação" else "Resolver problema", {
                            if (confirmed) snapshot.sessionId?.let(onDetail) else { dismissedResult = snapshot.sessionId; if(value?.failureCode in setOf("storage", "insufficient_storage")) onStorage() else onPrepare() }
                        }), secondaryAction = RecordingAction("Nova gravação", { dismissedResult = snapshot.sessionId }))
                }
                else -> {
                    val ready = prefs.disclosureVersion == RecordingSetupRepository.DISCLOSURE_VERSION && prefs.framed &&
                        prefs.framedOrientation == context.recordingOrientation() && RecordingSessionService.permissionsReady(context)
                    RecordingOperationalContent(RecordingHeaderState(RecordingVisualState.READY,
                        if (ready) "Pronto para gravar" else "Prepare sua gravação",
                        if (ready) "Câmera ${prefs.lens.label.lowercase()} · áudio autorizado" else null),
                        retentionLabel = "Temporário por ${recordingRetentionLabel(prefs.retentionDays)} após encerrar",
                        issue = issue ?: outcome ?: snapshot.failureMessage,
                        primaryAction = RecordingAction(if (starting) "Iniciando gravação…" else if (ready) "Iniciar gravação" else "Preparar gravação", {
                            if (!ready) onPrepare() else {
                                starting = true
                                scope.launch {
                                    val started = RecordingSessionService.startConfirmed(context, RecordingSetup(prefs.lens, prefs.segmentMinutes))
                                    if (!started) { starting = false; RecordingRuntimeStore.publishOutcome("Não foi possível iniciar. Feche a reprodução e confira os recursos da gravação.") }
                                }
                            }
                        }, Icons.Default.PlayArrow, enabled = !starting, testTag = UiTestTags.SECURITY_RECORDING_START),
                        secondaryAction = if (ready) RecordingAction("Ver enquadramento", onFraming, Icons.Outlined.CameraAlt) else null)
                }
            }
            if (active && outcome != null) RecordingIssueInline(outcome!!)
        }
    }
}

@Composable
internal fun RecordingActivePresentation(snapshot: RecordingRuntimeSnapshot, onStop: () -> Unit,
    onPause: () -> Unit = {}, onResume: () -> Unit = {}) {
    val phase = snapshot.phase
    val state = when {
        snapshot.isRecVisible -> RecordingVisualState.RECORDING
        phase == RecordingPhase.PAUSED -> RecordingVisualState.PAUSED
        phase == RecordingPhase.FINALIZING -> RecordingVisualState.FINALIZING
        phase == RecordingPhase.FAILED -> RecordingVisualState.FAILURE
        else -> RecordingVisualState.STARTING
    }
    val title = when(phase) {
        RecordingPhase.PREPARING -> "Iniciando gravação…"
        RecordingPhase.RECORDING -> if(snapshot.isRecVisible) "Gravando" else "Não foi possível confirmar o estado"
        RecordingPhase.PAUSING -> "Pausando…"
        RecordingPhase.PAUSED -> "Pausado"
        RecordingPhase.RESUMING -> "Retomando gravação…"
        RecordingPhase.ROTATING -> "Trocando arquivo…"
        RecordingPhase.FINALIZING -> "Finalizando gravação…"
        RecordingPhase.VERIFIED -> "Gravação disponível"
        RecordingPhase.FAILED -> "Gravação interrompida"
        else -> "Prepare sua gravação"
    }
    val explanation = when {
        snapshot.isRecVisible -> "Áudio e vídeo ativos"
        phase == RecordingPhase.PAUSED -> "Áudio e vídeo não estão sendo gravados"
        phase == RecordingPhase.ROTATING -> "Gravação temporariamente interrompida"
        phase == RecordingPhase.FINALIZING -> "Você pode sair desta tela enquanto a gravação é concluída."
        else -> null
    }
    val stop = RecordingAction(if(phase == RecordingPhase.PREPARING) "Cancelar início" else "Parar gravação",
        onStop, Icons.Default.Stop, tone = RecordingActionTone.STOP, testTag = UiTestTags.SECURITY_RECORDING_STOP)
    RecordingOperationalContent(RecordingHeaderState(state, title, explanation,
        snapshot.capturedDurationMs?.let(::recordingDuration)), issue = snapshot.failureMessage,
        primaryAction = when {
            phase == RecordingPhase.FINALIZING -> null
            phase == RecordingPhase.PAUSED -> RecordingAction("Retomar gravação", onResume, Icons.Default.PlayArrow)
            phase.name in ACTIVE_PHASES -> stop
            else -> null
        }, secondaryAction = when {
            phase == RecordingPhase.PAUSED -> stop
            snapshot.isRecVisible -> RecordingAction("Pausar", onPause, Icons.Default.Pause)
            else -> null
        })
}

@Composable
internal fun RecordingFirstUseRoute(onBack: () -> Unit, onPrepare: () -> Unit) {
    RecordingScaffold("Antes de gravar", onBack) { padding -> RecordingScrollContent(padding) {
        Text("Grave áudio e vídeo para seu registro de segurança.", style = CalcMotTypography.ScreenTitle, color = CalcMotColors.TextPrimary)
        Text("Os arquivos ficam neste aparelho. O prazo padrão é de 24 horas após encerrar. Salvar na galeria cria uma cópia independente.", style = CalcMotTypography.Body, color = CalcMotColors.TextSecondary)
        Text("A gravação pode continuar fora do CalcMot. Se o Android encerrar o app ou o aparelho reiniciar, ela não retoma sozinha.", style = CalcMotTypography.Body, color = CalcMotColors.TextSecondary)
        Text("Respeite a privacidade das pessoas e informe que está gravando. Este aviso não representa consentimento de passageiros.", style = CalcMotTypography.Body, color = CalcMotColors.TextSecondary)
        RecordingActionBar(RecordingAction("Preparar gravação", onPrepare), secondary = RecordingAction("Agora não", onBack))
    } }
}

@Composable
private fun RecordingPermissionsRoute(onBack: () -> Unit, prefs: RecordingPreferences?,
    onRememberRequest: suspend (String) -> Boolean, onContinue: () -> Unit) {
    val context = LocalContext.current
    val activity = context.recordingActivity()
    var refresh by remember { mutableIntStateOf(0) }
    var issue by remember { mutableStateOf<String?>(null) }
    RecordingResumeEffect { refresh++ }
    val scope = rememberCoroutineScope()
    var requesting by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { requesting = false; refresh++ }
    val permission = remember(refresh, prefs) {
        when(RecordingPermissionPolicy.next(recordingHasPermission(context, Manifest.permission.CAMERA),
            recordingHasPermission(context, Manifest.permission.RECORD_AUDIO), RecordingSessionService.permissionsReady(context))) {
            RecordingPermissionPolicy.Resource.CAMERA -> Manifest.permission.CAMERA
            RecordingPermissionPolicy.Resource.MICROPHONE -> Manifest.permission.RECORD_AUDIO
            RecordingPermissionPolicy.Resource.NOTIFICATIONS -> Manifest.permission.POST_NOTIFICATIONS
            null -> null
        }
    }
    val label = when(permission) {
        Manifest.permission.CAMERA -> "câmera"
        Manifest.permission.RECORD_AUDIO -> "microfone"
        else -> "notificações"
    }
    val permanent = permission != null && activity != null && RecordingPermissionPolicy.permanentlyDenied(
        permission in prefs?.requestedPermissions.orEmpty(), ActivityCompat.shouldShowRequestPermissionRationale(activity, permission))
    val needsSettings = permanent || (permission == Manifest.permission.POST_NOTIFICATIONS &&
        (Build.VERSION.SDK_INT < 33 || recordingHasPermission(context, permission)))
    RecordingScaffold("Autorizar recursos", onBack) { padding -> RecordingScrollContent(padding) {
        Text(if(permission == null) "Recursos autorizados" else "Autorizar $label", style = CalcMotTypography.ScreenTitle, color = CalcMotColors.TextPrimary)
        Text(when(permission) {
            Manifest.permission.CAMERA -> "Autorize a câmera para confirmar o enquadramento."
            Manifest.permission.RECORD_AUDIO -> "O áudio é necessário para esta gravação."
            null -> "Continue para confirmar o enquadramento. A gravação só começa quando você tocar em Iniciar gravação."
            else -> "As notificações mantêm os controles de gravação disponíveis fora do CalcMot."
        }, style = CalcMotTypography.Body, color = CalcMotColors.TextSecondary)
        issue?.let { RecordingIssueInline(it) }
        RecordingActionBar(RecordingAction(if(permission == null) "Continuar" else if(needsSettings) "Abrir configurações" else "Autorizar $label", {
            if(permission == null) onContinue() else if(needsSettings) {
                val intent = if(permission == Manifest.permission.POST_NOTIFICATIONS && Build.VERSION.SDK_INT >= 26)
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                if(runCatching { context.startActivity(intent) }.isFailure) issue = "Abra Configurações do Android → Aplicativos → CalcMot → Permissões."
            } else {
                requesting = true
                scope.launch {
                    if(onRememberRequest(permission)) launcher.launch(permission)
                    else { requesting = false; issue = "Não foi possível preparar a autorização. Tente novamente." }
                }
            }
        }, enabled = !requesting && prefs != null), secondary = RecordingAction("Agora não", onBack))
    } }
}

@Composable
internal fun RecordingSettingsRoute(onBack: () -> Unit, prefs: RecordingPreferences?, issue: String?,
    onSegment: (Int) -> Unit, onRetention: (Int) -> Unit, onFraming: () -> Unit, onPermissions: () -> Unit, onStorage: () -> Unit) {
    val snapshot by RecordingRuntimeStore.snapshot.collectAsStateWithLifecycle()
    val active = snapshot.phase.name in ACTIVE_PHASES
    var select by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableIntStateOf(10) }
    RecordingScaffold("Ajustes", onBack, accessibleTitle = "Ajustes de gravação") { padding -> RecordingScrollContent(padding) {
        if(prefs == null) RecordingLoading("Carregando ajustes…") else {
            if(active) RecordingIssueInline("Encerre a gravação para alterar as opções de captura.")
            RecordingSettingRow("Câmera", "${prefs.lens.label} · Ver enquadramento", onClick = if(active) null else onFraming)
            RecordingSettingRow("Qualidade", "480p")
            RecordingSettingRow("Divisão de arquivos", "${prefs.segmentMinutes} minutos", onClick = if(active) null else ({ selected = prefs.segmentMinutes; select = "segment" }))
            RecordingSettingRow("Prazo das próximas gravações", recordingRetentionLabel(prefs.retentionDays), onClick = { selected = prefs.retentionDays; select = "retention" })
            Text("Alterar o prazo padrão vale somente para novas gravações. O prazo conta desde o encerramento.", color = CalcMotColors.TextSecondary, style = CalcMotTypography.Body)
            RecordingSettingRow("Permissões", "Revisar recursos", onClick = onPermissions)
            RecordingSettingRow("Armazenamento", "Gerenciar gravações", onClick = onStorage)
        }
        issue?.let { RecordingIssueInline(it) }
    } }
    if(select != null && prefs != null) AlertDialog(onDismissRequest = { select = null }, title = { Text(if(select == "segment") "Divisão de arquivos" else "Prazo das próximas gravações") },
        text = { Column {
            val options = if(select == "segment") RecordingSetup.SEGMENT_DURATION_OPTIONS_MINUTES else RecordingRetention.options
            options.forEach { value -> Row(Modifier.fillMaxWidth()) {
                RadioButton(value == selected, onClick = { selected = value })
                TextButton(onClick = { selected = value }) { Text(if(select == "segment") "$value minutos" else recordingRetentionLabel(value)) }
            } }
        } }, confirmButton = { TextButton(onClick = { if(select == "segment") onSegment(selected) else onRetention(selected); select = null }) { Text("Confirmar") } },
        dismissButton = { TextButton(onClick = { select = null }) { Text("Cancelar") } })
}

@Composable
internal fun RecordingResumeEffect(onResume: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    val callback by rememberUpdatedState(onResume)
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if(event == Lifecycle.Event.ON_RESUME) callback() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}
internal fun recordingHasPermission(context: Context, permission: String) = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
internal fun Context.recordingActivity(): Activity? = when(this) { is Activity -> this; is ContextWrapper -> baseContext.takeUnless { it === this }?.recordingActivity(); else -> null }
@Suppress("DEPRECATION") internal fun Context.recordingOrientation(): Int = recordingActivity()?.windowManager?.defaultDisplay?.rotation ?: 0
internal fun recordingDuration(ms: Long) = "%d:%02d".format(ms / 60_000, ms / 1_000 % 60)
internal fun recordingDate(ms: Long) = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT).format(java.util.Date(ms))
internal fun recordingRetentionLabel(days: Int) = if(days == 1) "24h" else "$days dias"
internal val ACTIVE_PHASES = setOf("PREPARING", "RECORDING", "PAUSING", "PAUSED", "RESUMING", "ROTATING", "FINALIZING")
