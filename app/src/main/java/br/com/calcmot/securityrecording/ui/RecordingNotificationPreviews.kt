package br.com.calcmot.securityrecording.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.calcmot.securityrecording.ui.components.RecordingTokens
import br.com.calcmot.ui.design.theme.CalcMotTheme
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotSpacing

/** Visual specification only: not a replacement for NotificationCompat or custom RemoteViews.
 * Standard notification anatomy: app header, one status line, expanded action strip (max 3).
 * Compact/lock-screen projections omit the strip; Android/OEM owns final expansion and styling.
 * No channel, notification, PendingIntent, recording command or background service is created.
 */
private enum class NotificationForm { COMPACT, EXPANDED, LOCK_SCREEN }

/** Presentation fixtures, not commands or domain states. */
private enum class NotificationPreviewAction(val accessibleName: String, val icon: ImageVector) {
    Pause("Pausar gravação", Icons.Filled.Pause),
    Resume("Retomar gravação", Icons.Filled.PlayArrow),
    Stop("Parar gravação", Icons.Filled.Stop),
    CancelStart("Cancelar início", Icons.Filled.Close),
    ViewRecording("Ver gravação", Icons.Outlined.PlayCircle),
}

private object NotificationPreviewMetrics {
    // Platform notification content bands: collapsed 64dp, expanded up to 256dp.
    val compactHeight = 64.dp
    val expandedMaxHeight = 256.dp
    val smallIcon = 16.dp
}

@Composable
private fun RecordingNotificationPreview(title: String, actions: List<NotificationPreviewAction>, form: NotificationForm) = CalcMotTheme {
    Surface(color = CalcMotColors.SurfaceElevated, shape = RoundedCornerShape(CalcMotSpacing.Xl)) {
        Column(Modifier.fillMaxWidth().heightIn(min = NotificationPreviewMetrics.compactHeight,
            max = NotificationPreviewMetrics.expandedMaxHeight).padding(horizontal = CalcMotSpacing.Lg, vertical = CalcMotSpacing.Sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)) {
                Icon(Icons.Outlined.Videocam, null, Modifier.size(NotificationPreviewMetrics.smallIcon), tint = CalcMotColors.TextSecondary)
                Text("CalcMot", Modifier.weight(1f), color = CalcMotColors.TextSecondary, style = MaterialTheme.typography.labelMedium)
                Icon(if (form == NotificationForm.LOCK_SCREEN) Icons.Outlined.Lock else Icons.Outlined.ExpandMore,
                    if (form == NotificationForm.LOCK_SCREEN) "Composição para tela bloqueada" else "Expandir notificação",
                    Modifier.size(NotificationPreviewMetrics.smallIcon), tint = CalcMotColors.TextSecondary)
            }
            Text(title, Modifier.padding(top = CalcMotSpacing.Xs), color = CalcMotColors.TextPrimary, style = MaterialTheme.typography.bodyMedium)
            if (form == NotificationForm.EXPANDED && actions.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(top = CalcMotSpacing.Xs),
                    horizontalArrangement = Arrangement.spacedBy(CalcMotSpacing.Lg, Alignment.End)) {
                    actions.forEach { action ->
                        // Visual target only: named for accessibility, with no recording callback.
                        Box(Modifier.size(RecordingTokens.touchTarget), contentAlignment = Alignment.Center) {
                            Icon(action.icon, action.accessibleName,
                                Modifier.size(RecordingTokens.iconSize), tint = CalcMotColors.TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Capturing · Compact", group = "CAM-001 Notificações nativas", widthDp = 320)
@Composable
internal fun NotificationCapturingCompactPreview() = RecordingNotificationPreview("Gravando · 12:43", listOf(NotificationPreviewAction.Pause, NotificationPreviewAction.Stop), NotificationForm.COMPACT)

@Preview(name = "Capturing · Expanded", group = "CAM-001 Notificações nativas", widthDp = 320)
@Composable
internal fun NotificationCapturingExpandedPreview() = RecordingNotificationPreview("Gravando · 12:43", listOf(NotificationPreviewAction.Pause, NotificationPreviewAction.Stop), NotificationForm.EXPANDED)

@Preview(name = "Capturing · LockScreen", group = "CAM-001 Notificações nativas", widthDp = 320)
@Composable
internal fun NotificationCapturingLockScreenPreview() = RecordingNotificationPreview("Gravando · 12:43", listOf(NotificationPreviewAction.Pause, NotificationPreviewAction.Stop), NotificationForm.LOCK_SCREEN)

@Preview(name = "Paused · Compact", group = "CAM-001 Notificações nativas", widthDp = 320)
@Composable
internal fun NotificationPausedCompactPreview() = RecordingNotificationPreview("Pausado · 12:43", listOf(NotificationPreviewAction.Resume, NotificationPreviewAction.Stop), NotificationForm.COMPACT)

@Preview(name = "Paused · Expanded", group = "CAM-001 Notificações nativas", widthDp = 320)
@Composable
internal fun NotificationPausedExpandedPreview() = RecordingNotificationPreview("Pausado · 12:43", listOf(NotificationPreviewAction.Resume, NotificationPreviewAction.Stop), NotificationForm.EXPANDED)

@Preview(name = "Paused · LockScreen", group = "CAM-001 Notificações nativas", widthDp = 320)
@Composable
internal fun NotificationPausedLockScreenPreview() = RecordingNotificationPreview("Pausado · 12:43", listOf(NotificationPreviewAction.Resume, NotificationPreviewAction.Stop), NotificationForm.LOCK_SCREEN)

@Preview(name = "Starting · Compact", group = "CAM-001 Notificações nativas", widthDp = 320)
@Composable
internal fun NotificationStartingCompactPreview() = RecordingNotificationPreview("Iniciando", listOf(NotificationPreviewAction.CancelStart), NotificationForm.COMPACT)

@Preview(name = "Starting · Expanded", group = "CAM-001 Notificações nativas", widthDp = 320)
@Composable
internal fun NotificationStartingExpandedPreview() = RecordingNotificationPreview("Iniciando", listOf(NotificationPreviewAction.CancelStart), NotificationForm.EXPANDED)

@Preview(name = "Starting · LockScreen", group = "CAM-001 Notificações nativas", widthDp = 320)
@Composable
internal fun NotificationStartingLockScreenPreview() = RecordingNotificationPreview("Iniciando", listOf(NotificationPreviewAction.CancelStart), NotificationForm.LOCK_SCREEN)

@Preview(name = "Pausing · Compact", group = "CAM-001 Notificações nativas", widthDp = 320)
@Composable
internal fun NotificationPausingCompactPreview() = RecordingNotificationPreview("Pausando", listOf(NotificationPreviewAction.Stop), NotificationForm.COMPACT)

@Preview(name = "Pausing · Expanded", group = "CAM-001 Notificações nativas", widthDp = 320)
@Composable
internal fun NotificationPausingExpandedPreview() = RecordingNotificationPreview("Pausando", listOf(NotificationPreviewAction.Stop), NotificationForm.EXPANDED)

@Preview(name = "Pausing · LockScreen", group = "CAM-001 Notificações nativas", widthDp = 320)
@Composable
internal fun NotificationPausingLockScreenPreview() = RecordingNotificationPreview("Pausando", listOf(NotificationPreviewAction.Stop), NotificationForm.LOCK_SCREEN)

@Preview(name = "Resuming · Compact", group = "CAM-001 Notificações nativas", widthDp = 320)
@Composable
internal fun NotificationResumingCompactPreview() = RecordingNotificationPreview("Retomando", listOf(NotificationPreviewAction.Stop), NotificationForm.COMPACT)

@Preview(name = "Resuming · Expanded", group = "CAM-001 Notificações nativas", widthDp = 320)
@Composable
internal fun NotificationResumingExpandedPreview() = RecordingNotificationPreview("Retomando", listOf(NotificationPreviewAction.Stop), NotificationForm.EXPANDED)

@Preview(name = "Resuming · LockScreen", group = "CAM-001 Notificações nativas", widthDp = 320)
@Composable
internal fun NotificationResumingLockScreenPreview() = RecordingNotificationPreview("Retomando", listOf(NotificationPreviewAction.Stop), NotificationForm.LOCK_SCREEN)

@Preview(name = "Finalizing · Compact", group = "CAM-001 Notificações nativas", widthDp = 320)
@Composable
internal fun NotificationFinalizingCompactPreview() = RecordingNotificationPreview("Finalizando", listOf(), NotificationForm.COMPACT)

@Preview(name = "Finalizing · Expanded", group = "CAM-001 Notificações nativas", widthDp = 320)
@Composable
internal fun NotificationFinalizingExpandedPreview() = RecordingNotificationPreview("Finalizando", listOf(), NotificationForm.EXPANDED)

@Preview(name = "Finalizing · LockScreen", group = "CAM-001 Notificações nativas", widthDp = 320)
@Composable
internal fun NotificationFinalizingLockScreenPreview() = RecordingNotificationPreview("Finalizando", listOf(), NotificationForm.LOCK_SCREEN)

@Preview(name = "Completed · Compact", group = "CAM-001 Notificações nativas", widthDp = 320)
@Composable
internal fun NotificationCompletedCompactPreview() = RecordingNotificationPreview("Gravação encerrada", listOf(NotificationPreviewAction.ViewRecording), NotificationForm.COMPACT)

@Preview(name = "Completed · Expanded", group = "CAM-001 Notificações nativas", widthDp = 320)
@Composable
internal fun NotificationCompletedExpandedPreview() = RecordingNotificationPreview("Gravação encerrada", listOf(NotificationPreviewAction.ViewRecording), NotificationForm.EXPANDED)

@Preview(name = "Completed · LockScreen", group = "CAM-001 Notificações nativas", widthDp = 320)
@Composable
internal fun NotificationCompletedLockScreenPreview() = RecordingNotificationPreview("Gravação encerrada", listOf(NotificationPreviewAction.ViewRecording), NotificationForm.LOCK_SCREEN)
