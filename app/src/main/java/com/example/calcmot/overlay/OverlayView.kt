package com.example.calcmot.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.calcmot.model.TripData
import com.example.calcmot.ui.theme.SemanticAttention
import com.example.calcmot.ui.theme.SemanticBad
import com.example.calcmot.ui.theme.SemanticGood

@Composable
fun OverlayView(tripData: TripData) {
    val quality = getTripQuality(tripData)

    Box(modifier = Modifier.clip(RoundedCornerShape(24.dp))) {
        Column(
            modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
        ) {
            // Nível 1: Decisão
            OverlayHeader(quality)

            // Nível 2: Métricas
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = "R$ %.2f/km".format(tripData.valorPorKm),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "R$ %.2f/h".format(tripData.valorPorHora),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            // Nível 3: Dados de Suporte
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoChip("%.1f km".format(tripData.distanciaKm))
                InfoChip("%d min".format(tripData.minutosTotais))
                InfoChip("%.2f ★".format(tripData.nota))
            }
        }
    }
}

@Composable
private fun OverlayHeader(quality: TripQuality) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(quality.color)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = quality.icon, style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = quality.text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun InfoChip(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
}

enum class TripQuality(val text: String, val icon: String, val color: Color) {
    GOOD("BOA", "✅", SemanticGood),
    MEDIUM("ATENÇÃO", "⚠️", SemanticAttention),
    BAD("NÃO COMPENSA", "❌", SemanticBad)
}

// TODO: Mover esta lógica para um ViewModel ou para as configurações do usuário
fun getTripQuality(tripData: TripData): TripQuality {
    val valorPorKm = tripData.valorPorKm
    return when {
        valorPorKm >= 2.5 -> TripQuality.GOOD
        valorPorKm >= 1.8 -> TripQuality.MEDIUM
        else -> TripQuality.BAD
    }
}