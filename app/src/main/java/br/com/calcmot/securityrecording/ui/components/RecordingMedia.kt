package br.com.calcmot.securityrecording.ui.components

import android.view.ViewGroup
import android.view.View
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotSpacing
import br.com.calcmot.ui.design.tokens.CalcMotTypography

internal data class RecordingCameraSwitch(
    val nextLens: String,
    val onSwitchCamera: () -> Unit,
    val enabled: Boolean,
)

internal sealed interface RecordingPreviewState {
    data class Loading(val message: String) : RecordingPreviewState
    data class Unavailable(val reason: String) : RecordingPreviewState
    /** Transformed buffer width / height; the platform adapter supplies the effective ratio. */
    data class Streaming(val aspectRatio: Float) : RecordingPreviewState {
        init { require(aspectRatio.isFinite() && aspectRatio > 0f) }
    }
}

/**
 * No CameraX acquisition. The adapter must fit its buffer into frame, never crop it.
 * Bounds come from the parent. In a scrolling parent, width caps the portrait viewport;
 * bounded hosts can impose a smaller height without changing the buffer's aspect ratio.
 * Before streaming, 4:3 describes only the SD viewport, never a confirmed camera buffer.
 */
@Composable
internal fun RecordingPreview(
    state: RecordingPreviewState,
    currentLens: String,
    modifier: Modifier = Modifier,
    cameraSwitch: RecordingCameraSwitch? = null,
    maxFrameHeight: Dp = Dp.Infinity,
    frame: @Composable BoxScope.() -> Unit,
) {
    val ratio = (state as? RecordingPreviewState.Streaming)?.aspectRatio ?: (4f / 3f)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)) {
        BoxWithConstraints(Modifier.fillMaxWidth().clip(RoundedCornerShape(RecordingTokens.panelRadius))
            .background(RecordingTokens.panel), contentAlignment = Alignment.Center) {
            val height = minOf(maxWidth / ratio, maxWidth, maxHeight, maxFrameHeight)
            Column(Modifier.fillMaxWidth().heightIn(min = height), verticalArrangement = Arrangement.Center) {
                when (state) {
                    is RecordingPreviewState.Loading -> Box(Modifier.align(Alignment.CenterHorizontally).width(height * ratio).height(height)) {
                        frame()
                        Box(Modifier.fillMaxSize().background(RecordingTokens.panel), contentAlignment = Alignment.Center) {
                            RecordingLoading(state.message, Modifier.padding(CalcMotSpacing.Xl).padding(bottom = if (cameraSwitch == null) 0.dp else RecordingTokens.touchTarget))
                        }
                    }
                    is RecordingPreviewState.Unavailable -> RecordingIssueInline(state.reason, Modifier.padding(CalcMotSpacing.Xl).padding(bottom = if (cameraSwitch == null) 0.dp else RecordingTokens.touchTarget))
                    is RecordingPreviewState.Streaming -> Box(Modifier.align(Alignment.CenterHorizontally)
                        .width(height * ratio).height(height), content = frame)
                }
            }
            cameraSwitch?.let { control ->
                FilledIconButton(
                    onClick = control.onSwitchCamera,
                    enabled = control.enabled,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(CalcMotSpacing.Md)
                        .size(RecordingTokens.touchTarget).semantics { stateDescription = "Câmera $currentLens" },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = CalcMotColors.SurfaceElevated, contentColor = CalcMotColors.TextPrimary,
                        disabledContainerColor = CalcMotColors.SurfaceElevated, disabledContentColor = CalcMotColors.TextMuted,
                    ),
                ) {
                    Icon(Icons.Outlined.Cameraswitch, "Alternar para câmera ${control.nextLens}", Modifier.size(RecordingTokens.iconSize))
                }
            }
        }
        Text("Câmera $currentLens", color = CalcMotColors.TextPrimary, style = CalcMotTypography.BodyStrong)
        Text("Prévia — não está gravando", color = CalcMotColors.TextSecondary, style = CalcMotTypography.Body)
    }
}

/**
 * Native Android/Media3 transport, time bar, focus and accessibility.
 * Caller owns preparation and lifecycle. This view never creates, starts or releases a player.
 * A null player displays the real native idle controls; it is not simulated playback.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun RecordingPlayer(
    player: Player?,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 4f / 3f,
) {
    require(aspectRatio.isFinite() && aspectRatio > 0f)
    // Reserve transport, seek and action rows plus two scaled time lines. Never cap font scale.
    val controlsHeight = RecordingTokens.touchTarget * 4 + with(LocalDensity.current) { 28.sp.toDp() * 2 }
    BoxWithConstraints(modifier.fillMaxWidth()) {
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(maxOf(maxWidth / aspectRatio, controlsHeight))
            .clip(RoundedCornerShape(RecordingTokens.panelRadius)),
        factory = { context ->
            RecordingPlayerView(context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                useController = true
                controllerAutoShow = true
                setControllerAnimationEnabled(false)
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                setShowPreviousButton(false)
                setShowNextButton(false)
                setShowRewindButton(true)
                setShowFastForwardButton(true)
            }
        },
        update = { view ->
            if (view.player !== player) view.player = player
            if (player == null) view.showController()
        },
        onRelease = { view -> view.player = null },
    )
    }
}

/** Keep the actual native controls visible in design tools instead of Media3's edit-mode logo. */
@androidx.annotation.OptIn(UnstableApi::class)
private class RecordingPlayerView(context: Context) : PlayerView(context) {
    override fun isInEditMode(): Boolean = false

    private var stacked: Boolean? = null
    private var stackedTimeHeight = -1
    private var originalBarHeight = 0
    private var originalProgressMargin = 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val time = findViewById<LinearLayout>(androidx.media3.ui.R.id.exo_time) ?: return
        val actions = findViewById<View>(androidx.media3.ui.R.id.exo_basic_controls) ?: return
        val bar = findViewById<FrameLayout>(androidx.media3.ui.R.id.exo_bottom_bar) ?: return
        val progress = findViewById<View>(androidx.media3.ui.R.id.exo_progress) ?: return
        val progressParams = progress.layoutParams as FrameLayout.LayoutParams
        if (originalBarHeight == 0) {
            originalBarHeight = bar.layoutParams.height
            originalProgressMargin = progressParams.bottomMargin
        }
        // Measure the native time group without the action row constraining its desired width.
        time.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
        val needsStack = time.measuredWidth + actions.measuredWidth > bar.measuredWidth
        val extraHeight = if (needsStack) time.measuredHeight else 0
        if (stacked == needsStack && stackedTimeHeight == extraHeight) {
            positionTransport(bar, progress)
            return
        }
        stacked = needsStack
        stackedTimeHeight = extraHeight
        bar.layoutParams = bar.layoutParams.apply { height = originalBarHeight + extraHeight }
        time.layoutParams = (time.layoutParams as FrameLayout.LayoutParams).apply {
            gravity = if (needsStack) Gravity.TOP or Gravity.START else Gravity.CENTER_VERTICAL or Gravity.START
        }
        actions.layoutParams = (actions.layoutParams as FrameLayout.LayoutParams).apply {
            gravity = if (needsStack) Gravity.BOTTOM or Gravity.END else Gravity.CENTER_VERTICAL or Gravity.END
        }
        findViewById<View>(androidx.media3.ui.R.id.exo_extra_controls_scroll_view)?.let { overflow ->
            overflow.layoutParams = (overflow.layoutParams as FrameLayout.LayoutParams).apply {
                gravity = if (needsStack) Gravity.BOTTOM or Gravity.END else Gravity.CENTER_VERTICAL or Gravity.END
            }
        }
        progress.layoutParams = progressParams.apply { bottomMargin = originalProgressMargin + extraHeight }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        positionTransport(bar, progress)
    }

    private fun positionTransport(bar: View, progress: View) {
        // Translate instead of adding margins: margins shrink the native control's measure bounds
        // and can trigger Media3's minimal mode. Keep native touch/focus and command listeners.
        findViewById<View>(androidx.media3.ui.R.id.exo_center_controls)?.translationY =
            -(bar.layoutParams.height + progress.measuredHeight) / 2f
    }
}
