package br.com.calcmot.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.calcmot.ui.design.tokens.CalcMotColors

/**
 * Home-only, non-modal invitation. The driver must explicitly tap Share to open Android Sharesheet.
 * No action is injected into a driver app or into the floating offer overlay.
 */
@Composable
internal fun GrowthShareInviteCard(
    onShare: () -> Unit,
    onDismiss: () -> Unit,
    onOptOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(14.dp, shape)
            .background(CalcMotColors.SurfaceElevated, shape)
            .border(BorderStroke(1.dp, CalcMotColors.Success.copy(alpha = 0.65f)), shape)
            .testTag(UiTestTags.GROWTH_SHARE_PROMPT)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "O CalcMot está te ajudando?",
            color = CalcMotColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Indique para outro motorista! É rápido e ajuda mais gente a avaliar ofertas.",
            color = CalcMotColors.TextSecondary,
            fontSize = 14.sp,
            lineHeight = 19.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onShare,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag(UiTestTags.GROWTH_SHARE_PROMPT_ACTION),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CalcMotColors.PrimaryActionBlue,
                    contentColor = CalcMotColors.TextPrimary
                )
            ) {
                Icon(Icons.Outlined.Share, contentDescription = null)
                Text("Compartilhar", modifier = Modifier.padding(start = 8.dp))
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp).testTag(UiTestTags.GROWTH_SHARE_PROMPT_DISMISS)
            ) {
                Text("Agora não", color = CalcMotColors.TextSecondary)
            }
        }
        TextButton(
            onClick = onOptOut,
            modifier = Modifier
                .align(Alignment.End)
                .heightIn(min = 48.dp)
                .testTag(UiTestTags.GROWTH_SHARE_PROMPT_OPT_OUT)
        ) {
            Text("Não mostrar novamente", color = CalcMotColors.TextSecondary, fontSize = 12.sp)
        }
    }
}
