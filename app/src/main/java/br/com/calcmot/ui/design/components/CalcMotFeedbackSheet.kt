package br.com.calcmot.ui.design.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.SecureFlagPolicy
import br.com.calcmot.ui.design.theme.CalcMotTheme
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotShape
import br.com.calcmot.ui.design.tokens.CalcMotSpacing
import br.com.calcmot.ui.design.tokens.CalcMotTypography

enum class CalcMotFeedbackTone {
    SUCCESS,
    WARNING,
    ERROR,
    INFO,
    NEUTRAL
}

enum class CalcMotFeedbackNavigation {
    NONE,
    CLOSE,
    BACK
}

enum class CalcMotFeedbackActionStyle {
    FILLED,
    OUTLINED,
    TEXT,
    DANGER
}

data class CalcMotFeedbackAction(
    val label: String,
    val onClick: () -> Unit,
    val style: CalcMotFeedbackActionStyle = CalcMotFeedbackActionStyle.FILLED,
    val enabled: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalcMotFeedbackSheet(
    visible: Boolean,
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    tone: CalcMotFeedbackTone = CalcMotFeedbackTone.INFO,
    navigation: CalcMotFeedbackNavigation = CalcMotFeedbackNavigation.CLOSE,
    heroIcon: ImageVector = tone.defaultIcon,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    primaryAction: CalcMotFeedbackAction? = null,
    secondaryAction: CalcMotFeedbackAction? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    if (!visible) return

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = CalcMotColors.SurfaceElevated,
        contentColor = CalcMotColors.TextPrimary,
        dragHandle = { CalcMotFeedbackDragHandle() },
        properties = ModalBottomSheetProperties(securePolicy = SecureFlagPolicy.Inherit)
    ) {
        CalcMotFeedbackContent(
            title = title,
            subtitle = subtitle,
            tone = tone,
            navigation = navigation,
            heroIcon = heroIcon,
            onNavigation = onDismissRequest,
            primaryAction = primaryAction,
            secondaryAction = secondaryAction,
            footer = footer,
            content = content
        )
    }
}

@Composable
fun CalcMotFeedbackAlertDialog(
    title: String,
    message: String,
    primaryAction: CalcMotFeedbackAction,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    tone: CalcMotFeedbackTone = CalcMotFeedbackTone.WARNING,
    secondaryAction: CalcMotFeedbackAction = CalcMotFeedbackAction(
        label = "Cancelar",
        onClick = onDismissRequest,
        style = CalcMotFeedbackActionStyle.TEXT
    )
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        containerColor = CalcMotColors.SurfaceElevated,
        iconContentColor = tone.color,
        titleContentColor = CalcMotColors.TextPrimary,
        textContentColor = CalcMotColors.TextSecondary,
        icon = {
            Icon(
                imageVector = tone.defaultIcon,
                contentDescription = null,
                modifier = Modifier.size(34.dp)
            )
        },
        title = {
            Text(
                text = title,
                style = CalcMotTypography.SectionTitle,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = message,
                style = CalcMotTypography.Body,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            CalcMotFeedbackActionButton(action = primaryAction)
        },
        dismissButton = {
            CalcMotFeedbackActionButton(action = secondaryAction)
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun CalcMotSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    tone: CalcMotFeedbackTone = CalcMotFeedbackTone.SUCCESS
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
        snackbar = { data ->
            CalcMotFeedbackSnackbar(data = data, tone = tone)
        }
    )
}

@Composable
fun CalcMotFeedbackInfoCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Security,
    tone: CalcMotFeedbackTone = CalcMotFeedbackTone.INFO
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, CalcMotColors.BorderSubtle), RoundedCornerShape(18.dp))
            .background(CalcMotColors.Surface.copy(alpha = 0.64f), RoundedCornerShape(18.dp))
            .padding(CalcMotSpacing.Md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CalcMotSpacing.Md)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tone.color,
            modifier = Modifier.size(34.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, color = CalcMotColors.TextPrimary, style = CalcMotTypography.CardTitle)
            Text(text = body, color = CalcMotColors.TextSecondary, style = CalcMotTypography.Body)
        }
    }
}

@Composable
fun CalcMotFeedbackListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    tone: CalcMotFeedbackTone = CalcMotFeedbackTone.SUCCESS,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CalcMotSpacing.Md)
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = tone.color,
                modifier = Modifier.size(28.dp)
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, color = CalcMotColors.TextPrimary, style = CalcMotTypography.CardTitle)
            subtitle?.let {
                Text(text = it, color = CalcMotColors.TextSecondary, style = CalcMotTypography.Body)
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun CalcMotFeedbackDivider(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(CalcMotColors.BorderSubtle)
    )
}

@Composable
private fun CalcMotFeedbackContent(
    title: String,
    subtitle: String?,
    tone: CalcMotFeedbackTone,
    navigation: CalcMotFeedbackNavigation,
    heroIcon: ImageVector,
    onNavigation: () -> Unit,
    primaryAction: CalcMotFeedbackAction?,
    secondaryAction: CalcMotFeedbackAction?,
    footer: (@Composable ColumnScope.() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        CalcMotFeedbackHeader(navigation = navigation, onNavigation = onNavigation)
        CalcMotFeedbackHeroIcon(icon = heroIcon, tone = tone)
        Text(
            text = title,
            color = CalcMotColors.TextPrimary,
            style = CalcMotTypography.ScreenTitle,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        subtitle?.let {
            Text(
                text = it,
                color = CalcMotColors.TextSecondary,
                style = CalcMotTypography.ScreenSubtitle,
                textAlign = TextAlign.Center
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)
        ) {
            content()
        }
        CalcMotFeedbackActions(primaryAction = primaryAction, secondaryAction = secondaryAction)
        footer?.invoke(this)
    }
}

@Composable
private fun CalcMotFeedbackHeader(
    navigation: CalcMotFeedbackNavigation,
    onNavigation: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = when (navigation) {
            CalcMotFeedbackNavigation.NONE -> Arrangement.Center
            else -> Arrangement.End
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (navigation) {
            CalcMotFeedbackNavigation.NONE -> Unit
            CalcMotFeedbackNavigation.CLOSE -> {
                IconButton(onClick = onNavigation, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Fechar",
                        tint = CalcMotColors.TextSecondary
                    )
                }
            }

            CalcMotFeedbackNavigation.BACK -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    IconButton(onClick = onNavigation, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = CalcMotColors.TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalcMotFeedbackHeroIcon(
    icon: ImageVector,
    tone: CalcMotFeedbackTone
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tone.color,
        modifier = Modifier
            .size(76.dp)
            .background(tone.color.copy(alpha = 0.14f), CircleShape)
            .padding(18.dp)
    )
}

@Composable
private fun CalcMotFeedbackActions(
    primaryAction: CalcMotFeedbackAction?,
    secondaryAction: CalcMotFeedbackAction?
) {
    if (primaryAction == null && secondaryAction == null) return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Xs)
    ) {
        primaryAction?.let {
            CalcMotFeedbackActionButton(action = it, modifier = Modifier.fillMaxWidth())
        }
        secondaryAction?.let {
            CalcMotFeedbackActionButton(action = it, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun CalcMotFeedbackActionButton(
    action: CalcMotFeedbackAction,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    when (action.style) {
        CalcMotFeedbackActionStyle.FILLED -> Button(
            modifier = modifier.heightIn(min = 54.dp),
            onClick = action.onClick,
            enabled = action.enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = CalcMotColors.PrimaryActionBlue,
                contentColor = CalcMotColors.TextPrimary
            )
        ) {
            Text(text = action.label, style = CalcMotTypography.Button)
        }

        CalcMotFeedbackActionStyle.DANGER -> Button(
            modifier = modifier.heightIn(min = 54.dp),
            onClick = action.onClick,
            enabled = action.enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = CalcMotColors.Danger,
                contentColor = CalcMotColors.TextPrimary
            )
        ) {
            Text(text = action.label, style = CalcMotTypography.Button)
        }

        CalcMotFeedbackActionStyle.OUTLINED -> OutlinedButton(
            modifier = modifier.heightIn(min = 50.dp),
            onClick = action.onClick,
            enabled = action.enabled,
            shape = shape,
            border = BorderStroke(1.dp, CalcMotColors.BorderStrong),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CalcMotColors.TextPrimary)
        ) {
            Text(text = action.label, style = CalcMotTypography.Button)
        }

        CalcMotFeedbackActionStyle.TEXT -> TextButton(
            modifier = modifier.heightIn(min = 48.dp),
            onClick = action.onClick,
            enabled = action.enabled,
            shape = shape
        ) {
            Text(text = action.label, style = CalcMotTypography.Button, color = CalcMotColors.BrandSecondary)
        }
    }
}

@Composable
private fun CalcMotFeedbackSnackbar(
    data: SnackbarData,
    tone: CalcMotFeedbackTone
) {
    Snackbar(
        containerColor = CalcMotColors.SurfaceElevated,
        contentColor = CalcMotColors.TextPrimary,
        actionContentColor = CalcMotColors.BrandSecondary,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm)
        ) {
            Icon(
                imageVector = tone.defaultIcon,
                contentDescription = null,
                tint = tone.color,
                modifier = Modifier.size(22.dp)
            )
            Text(text = data.visuals.message, style = CalcMotTypography.Body, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun CalcMotFeedbackDragHandle() {
    Spacer(
        modifier = Modifier
            .padding(top = 12.dp, bottom = 8.dp)
            .size(width = 44.dp, height = 4.dp)
            .background(CalcMotColors.BorderStrong, RoundedCornerShape(CalcMotShape.Pill))
    )
}

private val CalcMotFeedbackTone.color: Color
    get() = when (this) {
        CalcMotFeedbackTone.SUCCESS -> CalcMotColors.Success
        CalcMotFeedbackTone.WARNING -> CalcMotColors.Warning
        CalcMotFeedbackTone.ERROR -> CalcMotColors.Danger
        CalcMotFeedbackTone.INFO -> CalcMotColors.BrandSecondary
        CalcMotFeedbackTone.NEUTRAL -> CalcMotColors.TextSecondary
    }

private val CalcMotFeedbackTone.defaultIcon: ImageVector
    get() = when (this) {
        CalcMotFeedbackTone.SUCCESS -> Icons.Outlined.CheckCircle
        CalcMotFeedbackTone.WARNING -> Icons.Outlined.WarningAmber
        CalcMotFeedbackTone.ERROR -> Icons.Outlined.ErrorOutline
        CalcMotFeedbackTone.INFO -> Icons.Outlined.Info
        CalcMotFeedbackTone.NEUTRAL -> Icons.Outlined.Security
    }

@OptIn(ExperimentalMaterial3Api::class)
@Preview(widthDp = 393, heightDp = 852, backgroundColor = 0xFF0A0D12, showBackground = true)
@Composable
private fun CalcMotFeedbackSheetPreview() {
    CalcMotTheme {
        CalcMotFeedbackSheet(
            visible = true,
            title = "Meta salva",
            subtitle = "O CalcMot vai usar esses valores para classificar as próximas ofertas.",
            tone = CalcMotFeedbackTone.SUCCESS,
            onDismissRequest = {},
            primaryAction = CalcMotFeedbackAction(label = "Voltar ao início", onClick = {}),
            secondaryAction = CalcMotFeedbackAction(
                label = "Editar novamente",
                onClick = {},
                style = CalcMotFeedbackActionStyle.TEXT
            )
        ) {
            CalcMotFeedbackInfoCard(
                title = "Você mantém o controle",
                body = "A meta pode ser alterada quando quiser.",
                tone = CalcMotFeedbackTone.SUCCESS
            )
        }
    }
}

@Preview(widthDp = 393, heightDp = 852, backgroundColor = 0xFF0A0D12, showBackground = true)
@Composable
private fun CalcMotFeedbackAlertDialogPreview() {
    CalcMotTheme {
        CalcMotFeedbackAlertDialog(
            title = "Limpar histórico?",
            message = "As ofertas registradas serão removidas deste aparelho.",
            primaryAction = CalcMotFeedbackAction(
                label = "Limpar histórico",
                onClick = {},
                style = CalcMotFeedbackActionStyle.DANGER
            ),
            onDismissRequest = {}
        )
    }
}
