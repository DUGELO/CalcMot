package br.com.calcmot.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.calcmot.accessibility.AccessibilityDebugOverlayState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DebugOverlayView(state: AccessibilityDebugOverlayState) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xDD101820))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "debug: aguardando card",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        DebugLine("serviço", if (state.serviceActive) "ativo" else "inativo")
        DebugLine("Uber", state.uberForeground.toString())
        DebugLine("evento", state.lastEventType)
        DebugLine("root", state.rootStatus)
        DebugLine("janelas", state.windowCount.toString())
        DebugLine("nós", state.nodesScanned.toString())
        DebugLine("text/desc", "${state.textNodeCount}/${state.contentDescriptionNodeCount}")
        DebugLine("candidatos", state.candidateCount.toString())
        DebugLine("delay", state.bestDelayMs?.let { "${it}ms" } ?: "-")
        DebugLine("falha", state.lastFailureReason.label)
        DebugLine("hora", timeFormatter.format(Date(state.updatedAtMs)))
    }
}

@Composable
private fun DebugLine(label: String, value: String) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodySmall,
        color = Color.White.copy(alpha = 0.92f)
    )
}

private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.forLanguageTag("pt-BR"))
