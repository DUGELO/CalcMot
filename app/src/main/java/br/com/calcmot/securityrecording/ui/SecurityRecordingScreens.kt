package br.com.calcmot.securityrecording.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import br.com.calcmot.ui.UiTestTags
import br.com.calcmot.ui.design.components.CalcMotInfoBanner
import br.com.calcmot.ui.design.components.CalcMotListItem
import br.com.calcmot.ui.design.components.CalcMotScaffold
import br.com.calcmot.ui.design.components.CalcMotSectionHeader
import br.com.calcmot.ui.design.components.CalcMotTopBar
import br.com.calcmot.ui.design.theme.CalcMotTheme
import br.com.calcmot.ui.design.tokens.CalcMotSpacing

@Composable
fun SecurityToolsRoute(
    onBack: () -> Unit,
    onOpenSecurityHub: () -> Unit
) {
    CalcMotScaffold(
        topBar = { CalcMotTopBar(title = "Ferramentas", onBack = onBack) }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(horizontal = CalcMotSpacing.ScreenHorizontal, vertical = CalcMotSpacing.ScreenVertical)
                .testTag(UiTestTags.SECURITY_TOOLS_SCREEN)
                .semantics { stateDescription = "Ferramentas disponíveis" },
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.SectionGap)
        ) {
            CalcMotSectionHeader(
                title = "Ferramentas",
                subtitle = "Recursos preparados para sua jornada."
            )
            CalcMotListItem(
                title = "Câmera secreta",
                description = "Prepare a gravação de segurança neste aparelho.",
                icon = Icons.Outlined.PhotoCamera,
                onClick = onOpenSecurityHub,
                modifier = Modifier
                    .testTag(UiTestTags.SECURITY_CAMERA_CARD)
                    .semantics { stateDescription = "Abre o Hub de Gravação de Segurança" }
            )
        }
    }
}

@Composable
fun SecurityRecordingHubRoute(
    onBack: () -> Unit,
    onConfigureRecording: () -> Unit,
    onOpenRecordings: () -> Unit
) {
    CalcMotScaffold(
        topBar = { CalcMotTopBar(title = "Gravação de Segurança", onBack = onBack) }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(horizontal = CalcMotSpacing.ScreenHorizontal, vertical = CalcMotSpacing.ScreenVertical)
                .testTag(UiTestTags.SECURITY_SCREEN)
                .semantics { stateDescription = "Configuração pendente" },
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.SectionGap)
        ) {
            CalcMotSectionHeader(
                title = "Hub de Gravação de Segurança",
                subtitle = "Prepare a gravação antes de iniciar uma sessão."
            )
            CalcMotInfoBanner(
                title = "Configuração pendente",
                body = "Nenhuma câmera ou microfone é usado nesta etapa."
            )
            CalcMotListItem(
                title = "Configurar gravação",
                description = "Escolha as opções antes de uma futura gravação.",
                icon = Icons.Outlined.Settings,
                onClick = onConfigureRecording,
                modifier = Modifier.testTag(UiTestTags.SECURITY_CONFIGURE_ACTION)
            )
            CalcMotListItem(
                title = "Gravações existentes",
                description = "Consulte sessões verificadas quando elas estiverem disponíveis.",
                icon = Icons.Outlined.VideoLibrary,
                onClick = onOpenRecordings,
                modifier = Modifier.testTag(UiTestTags.SECURITY_RECORDINGS_ACTION)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF04060A)
@Composable
private fun SecurityRecordingHubPreview() {
    CalcMotTheme {
        SecurityRecordingHubRoute(
            onBack = {},
            onConfigureRecording = {},
            onOpenRecordings = {}
        )
    }
}
