package br.com.calcmot.securityrecording.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import br.com.calcmot.ui.design.components.CalcMotScaffold
import br.com.calcmot.ui.design.components.CalcMotTopBar
import br.com.calcmot.ui.design.tokens.CalcMotColors

/** Owns safe drawing insets once. Content owns scrolling, including its action bar. */
@Composable
internal fun RecordingScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    accessibleTitle: String = title,
    navigationActions: @Composable (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    CalcMotScaffold(
        modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            .semantics { paneTitle = accessibleTitle },
        topBar = {
            CalcMotTopBar(title, navigation = {
                IconButton(onClick = onBack, modifier = Modifier.size(RecordingTokens.touchTarget)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = CalcMotColors.TextPrimary)
                }
            }, action = navigationActions)
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().consumeWindowInsets(padding)) { content(padding) }
    }
}

/** Collapses secondary destinations at large font sizes, preserving the route title. */
@Composable
internal fun RecordingNavigationActions(onHistory: () -> Unit, onSettings: () -> Unit, settingsEnabled: Boolean = true) {
    val compact = LocalDensity.current.fontScale >= 1.3f
    var expanded by remember { mutableStateOf(false) }
    Row {
        if (!compact) IconButton(onHistory, Modifier.size(RecordingTokens.touchTarget)) {
            Icon(Icons.Outlined.VideoLibrary, "Gravações", tint = CalcMotColors.TextPrimary)
        }
        Box {
            IconButton({ expanded = true }, Modifier.size(RecordingTokens.touchTarget)) {
                Icon(Icons.Default.MoreVert, "Mais opções", tint = CalcMotColors.TextPrimary)
            }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }, containerColor = CalcMotColors.SurfaceElevated) {
                if (compact) DropdownMenuItem(text = { Text("Gravações", color = CalcMotColors.TextPrimary) },
                    onClick = { expanded = false; onHistory() })
                DropdownMenuItem(enabled = settingsEnabled, text = { Text("Ajustes de gravação", color = CalcMotColors.TextPrimary) },
                    onClick = { expanded = false; onSettings() })
            }
        }
    }
}
