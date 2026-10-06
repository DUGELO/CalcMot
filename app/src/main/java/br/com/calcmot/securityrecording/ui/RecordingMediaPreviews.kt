package br.com.calcmot.securityrecording.ui

import androidx.core.graphics.createBitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import br.com.calcmot.securityrecording.ui.components.*
import br.com.calcmot.ui.design.theme.CalcMotTheme
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotSpacing

/** Calibration image for design tools only; never presented as captured camera content. */
private fun calibrationFrame() = createBitmap(640, 480).apply {
    val canvas = Canvas(this)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    canvas.drawColor(CalcMotColors.SurfaceElevated.toArgb())
    paint.color = CalcMotColors.TextMuted.toArgb()
    paint.strokeWidth = 2f
    for (x in 0..640 step 80) canvas.drawLine(x.toFloat(), 0f, x.toFloat(), 480f, paint)
    for (y in 0..480 step 80) canvas.drawLine(0f, y.toFloat(), 640f, y.toFloat(), paint)
    paint.color = CalcMotColors.BrandAccent.toArgb()
    canvas.drawRect(0f, 0f, 40f, 40f, paint)
    canvas.drawRect(600f, 440f, 640f, 480f, paint)
    paint.color = CalcMotColors.TextPrimary.toArgb()
    paint.textSize = 28f
    canvas.drawText("FRAME DE PREVIEW · 4:3", 125f, 235f, paint)
}.asImageBitmap()

@Preview(name = "Histórico · thumbnail carregada", group = "CAM-001 Mídia", widthDp = 393, heightDp = 852)
@Preview(name = "Histórico · thumbnail · fonte 2", group = "CAM-001 Mídia", widthDp = 320, heightDp = 568, fontScale = 2f)
@Composable
internal fun RecordingLoadedThumbnailPreview() = CalcMotTheme {
    val frame = remember { calibrationFrame() }
    RecordingScaffold("Gravações", {}) { padding ->
        RecordingHistoryContent(listOf(RecordingSessionGroup("today", "Hoje", listOf(
            RecordingSessionItem("loaded", "18:42", "12 min", RecordingAvailabilityState("Temporário · até amanhã, 18:54"), thumbnail = frame)
        ))), {}, Modifier.padding(padding).padding(horizontal = CalcMotSpacing.ScreenHorizontal))
    }
}

@Preview(name = "Enquadramento · imagem · lente única", group = "CAM-001 Mídia", widthDp = 320, heightDp = 568)
@Preview(name = "Enquadramento · imagem · paisagem", group = "CAM-001 Mídia", widthDp = 852, heightDp = 393)
@Preview(name = "Enquadramento · imagem · fonte 2", group = "CAM-001 Mídia", widthDp = 320, heightDp = 568, fontScale = 2f)
@Composable
internal fun RecordingVisibleFramePreview() = CalcMotTheme {
    val frame = remember { calibrationFrame() }
    RecordingScaffold("Enquadramento", {}) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
        val frameHeight = maxHeight / 2
        RecordingScrollContent(PaddingValues()) {
        RecordingFramingContent(RecordingPreviewState.Streaming(4f / 3f), "traseira",
            RecordingAction("Usar este enquadramento", {}), maxFrameHeight = frameHeight) {
            Image(frame, "Imagem de calibração do preview", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
    } } }
}

@Preview(name = "Estado · sem explicação", group = "CAM-001 Acessibilidade", widthDp = 320, fontScale = 2f)
@Composable
internal fun RecordingHeaderWithoutExplanationPreview() = CalcMotTheme {
    Surface(color = CalcMotColors.Surface) {
        RecordingStateHeader(RecordingHeaderState(RecordingVisualState.RECORDING, "Gravando",
            capturedDuration = "12:43", spokenDuration = "12 minutos e 43 segundos"), Modifier.padding(CalcMotSpacing.Xl))
    }
}
