package br.com.calcmot.securityrecording.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.Role
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotSpacing
import br.com.calcmot.ui.design.tokens.CalcMotTypography

/** Dates and eligibility are supplied by the caller; no clock or retention policy in UI. */
internal data class RecordingAvailabilityState(
    val privateLabel: String,
    val galleryLabel: String? = null,
    val expiresSoon: Boolean = false,
)

@Composable
internal fun RecordingAvailability(state: RecordingAvailabilityState, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Xs)) {
        if (state.expiresSoon) Text("Expira em breve", color = CalcMotColors.Warning, style = CalcMotTypography.BodyStrong)
        Text(state.privateLabel, color = CalcMotColors.TextSecondary, style = CalcMotTypography.Body)
        state.galleryLabel?.let { Text(it, color = CalcMotColors.TextSecondary, style = CalcMotTypography.Body) }
    }
}

internal data class RecordingSessionItem(
    val id: String,
    val startLabel: String,
    val durationLabel: String,
    val availability: RecordingAvailabilityState,
    val interruption: String? = null,
    val thumbnail: ImageBitmap? = null,
)

@Composable
internal fun RecordingSessionRow(item: RecordingSessionItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val largeText = LocalDensity.current.fontScale >= 1.3f
    Column(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().heightIn(min = RecordingTokens.touchTarget)
            .clickable(role = Role.Button, onClickLabel = "Abrir gravação de ${item.startLabel}", onClick = onClick)
            .padding(vertical = CalcMotSpacing.Lg), verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CalcMotSpacing.Md),
        ) {
            RecordingThumbnail(item.thumbnail)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Xs)) {
                Text(item.startLabel, color = CalcMotColors.TextPrimary, style = CalcMotTypography.CardTitle)
                Text(item.durationLabel, color = CalcMotColors.TextSecondary, style = CalcMotTypography.Body)
                if (!largeText) SessionAvailability(item)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                Modifier.size(RecordingTokens.iconSize).align(Alignment.CenterVertically), tint = CalcMotColors.TextMuted)
        }
        if (largeText) SessionAvailability(item)
        }
        HorizontalDivider(color = CalcMotColors.BorderSubtle)
    }
}

@Composable
private fun SessionAvailability(item: RecordingSessionItem) {
    RecordingAvailability(item.availability)
    item.interruption?.let { Text(it, color = CalcMotColors.Warning, style = CalcMotTypography.Body) }
}

/** Frame extraction belongs to the caller. No image is substituted for unavailable media. */
@Composable
internal fun RecordingThumbnail(image: ImageBitmap?, modifier: Modifier = Modifier) {
    Box(modifier.width(RecordingTokens.thumbnailWidth).aspectRatio(4f / 3f)
        .clip(RoundedCornerShape(RecordingTokens.buttonRadius)).background(RecordingTokens.panel),
        contentAlignment = Alignment.Center) {
        if (image != null) Image(image, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(Icons.Outlined.VideoLibrary, "Miniatura indisponível", Modifier.size(RecordingTokens.iconSize), tint = CalcMotColors.TextMuted)
    }
}

@Composable
internal fun RecordingSettingRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val stacked = LocalDensity.current.fontScale >= 1.3f
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = RecordingTokens.touchTarget)
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .padding(vertical = CalcMotSpacing.Lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CalcMotSpacing.Md),
        ) {
            if (stacked) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Xs)) {
                    Text(label, color = CalcMotColors.TextPrimary, style = CalcMotTypography.CardTitle)
                    Text(value, color = CalcMotColors.TextSecondary, style = CalcMotTypography.Body)
                }
            } else {
                Text(label, Modifier.weight(1f), color = CalcMotColors.TextPrimary, style = CalcMotTypography.CardTitle)
                Text(value, Modifier.weight(1f), color = CalcMotColors.TextSecondary, style = CalcMotTypography.Body, textAlign = TextAlign.End)
            }
            if (onClick != null) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                Modifier.size(RecordingTokens.iconSize), tint = CalcMotColors.TextMuted)
        }
        HorizontalDivider(color = CalcMotColors.BorderSubtle)
    }
}
