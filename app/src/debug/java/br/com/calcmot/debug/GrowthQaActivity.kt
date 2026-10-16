package br.com.calcmot.debug

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.calcmot.growth.GrowthShareNotification
import br.com.calcmot.growth.PlayRatingLauncher
import br.com.calcmot.growth.PlayReviewPolicy
import br.com.calcmot.growth.PlayReviewState
import br.com.calcmot.ui.CalcMotSharing
import br.com.calcmot.ui.GrowthShareBottomSheet
import br.com.calcmot.ui.theme.MetricaTheme

/**
 * Debug-only visual laboratory. NEVER alters streak counters, review quotas, or
 * production permissions. Play's native review dialog cannot be forced to display.
 */
class GrowthQaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MetricaTheme {
                GrowthQaLab(
                    onShareReal = { CalcMotSharing.share(this, "home") },
                    onOpenPlayStore = { PlayRatingLauncher.openPlayStore(this) },
                    onRequestNativeReview = {
                        PlayRatingLauncher.requestNativeReview(this) {
                            Toast.makeText(this, "Solicitação concluída. O Google decide se mostra o diálogo.", Toast.LENGTH_LONG).show()
                        }
                    },
                    onNotificationReal = {
                        val ok = GrowthShareNotification.show(this)
                        Toast.makeText(
                            this,
                            if (ok) "Notificação silenciosa enviada." else "Notificações não autorizadas ou canal bloqueado.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        }
    }
}

internal object GrowthQaTags {
    const val SCREEN = "growth_qa_screen"
    const val SHARE_PREVIEW = "growth_qa_share_preview"
    const val SHARE_REAL = "growth_qa_share_real"
    const val SHARE_FEEDBACK = "growth_qa_share_feedback"
    const val RATE_PREVIEW = "growth_qa_rate_preview"
    const val RATE_DIALOG = "growth_qa_rate_dialog"
    const val NOTIFICATION_PREVIEW = "growth_qa_notification_preview"
    const val NOTIFICATION_DIALOG = "growth_qa_notification_dialog"
    const val RATING_STATUS = "growth_qa_rating_status"
    const val STREAK_0 = "growth_qa_streak_0"
    const val STREAK_2 = "growth_qa_streak_2"
    const val STREAK_5 = "growth_qa_streak_5"
    const val STREAK_10 = "growth_qa_streak_10"
    const val STREAK_30 = "growth_qa_streak_30"
}

@Composable
internal fun GrowthQaLab(
    onShareReal: () -> Unit = {},
    onOpenPlayStore: () -> Unit = {},
    onRequestNativeReview: () -> Unit = {},
    onNotificationReal: () -> Unit = {}
) {
    var days by remember { mutableIntStateOf(2) }
    var showShare by remember { mutableStateOf(false) }
    var showRating by remember { mutableStateOf(false) }
    var showNotification by remember { mutableStateOf(false) }
    var shareFeedback by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val previousRequest = PlayReviewState(lastAttemptAt = now - 20L * 86_400_000L, attempts = 9)
    val eligible = PlayReviewPolicy.eligible(previousRequest, days, now)
    val reviewWaitDays = PlayReviewPolicy.cooldownMillis(days) / 86_400_000L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag(GrowthQaTags.SCREEN)
            .padding(horizontal = 18.dp, vertical = 30.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Text("CalcMot · Growth QA", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Laboratório visual DEBUG. Nenhum botão de prévia altera os dias reais nem a frequência de convites.")
        Text("Simule dias seguidos de ofertas exibidas:", style = MaterialTheme.typography.titleSmall)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0 to GrowthQaTags.STREAK_0, 2 to GrowthQaTags.STREAK_2, 5 to GrowthQaTags.STREAK_5)
                    .forEach { (value, tag) ->
                        FilterChip(
                            selected = days == value,
                            onClick = { days = value },
                            label = { Text("$value dias") },
                            modifier = Modifier.testTag(tag)
                        )
                    }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(10 to GrowthQaTags.STREAK_10, 30 to GrowthQaTags.STREAK_30)
                    .forEach { (value, tag) ->
                        FilterChip(
                            selected = days == value,
                            onClick = { days = value },
                            label = { Text("$value dias") },
                            modifier = Modifier.testTag(tag)
                        )
                    }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Compartilhamento · arte 3D real", style = MaterialTheme.typography.titleMedium)
                Button(
                    modifier = Modifier.fillMaxWidth().testTag(GrowthQaTags.SHARE_PREVIEW),
                    onClick = { shareFeedback = false; showShare = true }
                ) { Text("Visualizar bottom sheet") }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth().testTag(GrowthQaTags.SHARE_REAL),
                    onClick = onShareReal
                ) { Text("Testar compartilhador Android real") }
                if (shareFeedback) {
                    Text("Toque no CTA validado (modo prévia). Nenhuma mensagem foi enviada.",
                        modifier = Modifier.testTag(GrowthQaTags.SHARE_FEEDBACK))
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Avaliação Google Play", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Simulação: $days dias de uso • intervalo atual de $reviewWaitDays dias • " +
                        if (eligible) "elegível após 20 dias desde tentativa anterior" else "não elegível após 20 dias",
                    modifier = Modifier.testTag(GrowthQaTags.RATING_STATUS)
                )
                Button(
                    modifier = Modifier.fillMaxWidth().testTag(GrowthQaTags.RATE_PREVIEW),
                    onClick = { showRating = true }
                ) {
                    Icon(Icons.Outlined.StarOutline, contentDescription = null)
                    Text("  Ver explicação visual da avaliação")
                }
                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onOpenPlayStore) {
                    Text("Abrir ficha real na Play Store")
                }
                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onRequestNativeReview) {
                    Text("Solicitar diálogo oficial (pode não aparecer)")
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Notificação silenciosa", style = MaterialTheme.typography.titleMedium)
                Button(
                    modifier = Modifier.fillMaxWidth().testTag(GrowthQaTags.NOTIFICATION_PREVIEW),
                    onClick = { showNotification = true }
                ) {
                    Icon(Icons.Outlined.NotificationsNone, contentDescription = null)
                    Text("  Prévia do conteúdo")
                }
                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onNotificationReal) {
                    Text("Enviar notificação real (se autorizada)")
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("As prévias não garantem que o Google Play exibirá um pedido real. " +
            "Não usa dados de Uber/99, permissões nem persistência do Growth.")
    }

    if (showShare) {
        GrowthShareBottomSheet(
            streakDays = days,
            onShare = { showShare = false; shareFeedback = true },
            onDismiss = { showShare = false }
        )
    }

    if (showRating) {
        AlertDialog(
            modifier = Modifier.testTag(GrowthQaTags.RATE_DIALOG),
            onDismissRequest = { showRating = false },
            icon = { Icon(Icons.Outlined.StarOutline, contentDescription = null) },
            title = { Text("Prévia de avaliação") },
            text = {
                Text("Esta é uma SIMULAÇÃO para inspeção visual. O diálogo real é controlado " +
                    "pelo Google Play e não pode ser personalizado nem forçado no APK Debug.")
            },
            confirmButton = { TextButton(onClick = { showRating = false }) { Text("Entendi") } }
        )
    }

    if (showNotification) {
        AlertDialog(
            modifier = Modifier.testTag(GrowthQaTags.NOTIFICATION_DIALOG),
            onDismissRequest = { showNotification = false },
            icon = { Icon(Icons.Outlined.NotificationsNone, contentDescription = null) },
            title = { Text("Gostando do CalcMot?") },
            text = {
                Text("Se ele está ajudando, compartilhe com outro motorista.\n\n" +
                    "SIMULAÇÃO do conteúdo: a aparência real da notificação depende do Android.")
            },
            confirmButton = { TextButton(onClick = { showNotification = false }) { Text("Fechar") } }
        )
    }
}
