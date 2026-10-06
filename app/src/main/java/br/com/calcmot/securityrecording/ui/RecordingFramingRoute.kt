package br.com.calcmot.securityrecording.ui

import android.Manifest
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.Recorder
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.calcmot.securityrecording.application.RecordingResourceCoordinator
import br.com.calcmot.securityrecording.data.RecordingPreferences
import br.com.calcmot.securityrecording.domain.RecordingLens
import br.com.calcmot.securityrecording.ui.components.*
import br.com.calcmot.ui.design.tokens.*

@Composable
internal fun RecordingFramingRoute(onBack: () -> Unit, preferences: RecordingPreferences?,
    onConfirmed: (RecordingLens, Int) -> Unit, onPermissions: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var lens by remember { mutableStateOf(preferences?.lens ?: RecordingLens.FRONT) }
    var lenses by remember { mutableStateOf<List<RecordingLens>>(emptyList()) }
    var state by remember { mutableStateOf<RecordingPreviewState>(RecordingPreviewState.Loading("Abrindo câmera…")) }
    var retry by remember { mutableIntStateOf(0) }
    var foreground by remember { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    LaunchedEffect(preferences?.lens) { preferences?.let { lens = it.lens } }
    LaunchedEffect(lens, retry, foreground) {
        if(foreground) { kotlinx.coroutines.delay(30_000); if(state is RecordingPreviewState.Loading) state = RecordingPreviewState.Unavailable("A câmera demorou demais para responder. Tente novamente.") }
    }
    val view = remember(context) { PreviewView(context).apply {
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        scaleType = PreviewView.ScaleType.FIT_CENTER
    } }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if(event == Lifecycle.Event.ON_START) foreground = true
            if(event == Lifecycle.Event.ON_STOP) { foreground = false; state = RecordingPreviewState.Loading("Abrindo câmera…") }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(lens, retry, foreground, preferences?.lens) {
        var disposed = false
        var bound: Preview? = null
        var provider: ProcessCameraProvider? = null
        var reader = false
        state = RecordingPreviewState.Loading("Abrindo câmera…")
        val streamObserver = Observer<PreviewView.StreamState> { stream ->
            if(!disposed && stream == PreviewView.StreamState.STREAMING) {
                val info = bound?.resolutionInfo
                if(info != null) {
                    val width = if(info.rotationDegrees % 180 == 0) info.resolution.width else info.resolution.height
                    val height = if(info.rotationDegrees % 180 == 0) info.resolution.height else info.resolution.width
                    state = RecordingPreviewState.Streaming(width.toFloat() / height)
                }
            } else if(!disposed && stream == PreviewView.StreamState.IDLE && bound != null) {
                state = RecordingPreviewState.Loading("Aguardando a câmera…")
            }
        }
        if(!recordingHasPermission(context, Manifest.permission.CAMERA)) {
            state = RecordingPreviewState.Unavailable("Autorize a câmera para confirmar o enquadramento.")
        } else if(foreground && preferences != null) {
            reader = RecordingResourceCoordinator.acquireReader("preview")
            if(!reader) state = RecordingPreviewState.Unavailable("Encerre a gravação antes de ajustar o enquadramento.")
            else {
                val future = ProcessCameraProvider.getInstance(context)
                future.addListener({
                    if(!disposed) {
                        runCatching {
                            val cameraProvider = future.get(); provider = cameraProvider
                            lenses = RecordingLens.entries.filter { candidate ->
                                val selector = candidate.selector()
                                cameraProvider.availableCameraInfos.any { info ->
                                    selector.filter(listOf(info)).isNotEmpty() && Quality.SD in Recorder.getVideoCapabilities(info).getSupportedQualities(DynamicRange.SDR)
                                }
                            }
                            if(lens !in lenses) {
                                state = RecordingPreviewState.Unavailable(if(lenses.isEmpty()) "Nenhuma câmera disponível para gravação." else "A câmera ${lens.label.lowercase()} não está disponível. Escolha outra câmera.")
                            } else {
                                val preview = Preview.Builder().setTargetRotation(context.recordingOrientation()).build()
                                preview.setSurfaceProvider(view.surfaceProvider)
                                cameraProvider.bindToLifecycle(owner, lens.selector(), preview)
                                bound = preview
                                view.previewStreamState.observe(owner, streamObserver)
                            }
                        }.onFailure { state = RecordingPreviewState.Unavailable("A câmera não está disponível. Tente novamente.") }
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        }
        onDispose {
            disposed = true
            view.previewStreamState.removeObserver(streamObserver)
            bound?.let { provider?.unbind(it) }
            if(reader) RecordingResourceCoordinator.releaseReader("preview")
        }
    }
    RecordingScaffold("Enquadramento", onBack) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val maxFrameHeight = maxHeight / 2
            RecordingScrollContent(PaddingValues()) {
                Text("Ajuste o celular antes de dirigir.", color = CalcMotColors.TextSecondary, style = CalcMotTypography.Body)
                RecordingFramingContent(state, lens.label.lowercase(),
                    RecordingAction("Usar este enquadramento", { onConfirmed(lens, context.recordingOrientation()) }, enabled = state is RecordingPreviewState.Streaming),
                    cameraSwitch = if(lenses.any { it != lens }) RecordingCameraSwitch(lenses.first { it != lens }.label.lowercase(), {
                        lens = lenses.first { it != lens }
                    }, enabled = foreground) else null,
                    maxFrameHeight = maxFrameHeight,
                    frame = { AndroidView(factory = { view }, modifier = Modifier.fillMaxSize()) })
                if(state is RecordingPreviewState.Unavailable) RecordingActionButton(
                    RecordingAction(if(recordingHasPermission(context, Manifest.permission.CAMERA)) "Tentar novamente" else "Autorizar câmera",
                        { if(recordingHasPermission(context, Manifest.permission.CAMERA)) retry++ else onPermissions() }), outlined = true)
            }
        }
    }
}
private fun RecordingLens.selector() = if(this == RecordingLens.FRONT) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
