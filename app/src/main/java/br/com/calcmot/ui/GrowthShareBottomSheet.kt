package br.com.calcmot.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import br.com.calcmot.R
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ExperimentalMaterial3Api
import br.com.calcmot.ui.design.tokens.CalcMotColors

/**
 * Single invitation design for both manual sharing and occasional organic growth nudges.
 * Deliberately Activity-scoped: never placed inside the AccessibilityService or offer overlay.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GrowthShareBottomSheet(
    streakDays: Int,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ModalBottomSheet(
        modifier = Modifier.testTag(UiTestTags.GROWTH_SHARE_PROMPT),
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = shape,
        containerColor = CalcMotColors.SurfaceElevated,
        contentColor = CalcMotColors.TextPrimary,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 2.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(CalcMotColors.TextMuted.copy(alpha = 0.55f), CircleShape)
            )
        }
    ) {
        GrowthShareSheetContent(
            streakDays = streakDays,
            onShare = onShare,
            onDismiss = onDismiss
        )
    }
}

@Composable
internal fun GrowthShareSheetContent(
    streakDays: Int,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 22.dp)
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(130.dp),
            contentAlignment = Alignment.Center
        ) {
            GrowthShareCommunityIllustration()
            IconButton(
                modifier = Modifier.align(Alignment.TopEnd)
                    .size(48.dp).testTag(UiTestTags.GROWTH_SHARE_PROMPT_CLOSE),
                onClick = onDismiss
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Fechar convite",
                    modifier = Modifier.size(20.dp),
                    tint = CalcMotColors.TextMuted
                )
            }
        }
        val streakLabel = when {
            streakDays >= 2 -> "$streakDays dias seguidos"
            streakDays == 1 -> "1 dia de uso"
            else -> "Convide outros motoristas"
        }
        Box(
            modifier = Modifier
                .background(CalcMotColors.Success.copy(alpha = 0.09f), RoundedCornerShape(100.dp))
                .border(
                    BorderStroke(1.dp, CalcMotColors.Success.copy(alpha = 0.70f)),
                    RoundedCornerShape(100.dp)
                )
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .testTag(UiTestTags.GROWTH_SHARE_STREAK),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = streakLabel,
                color = CalcMotColors.Success,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = buildAnnotatedString {
                append("Leve o ")
                appendCalcMotWordmark()
                append(" para seu grupo")
            },
            color = CalcMotColors.TextPrimary,
            textAlign = TextAlign.Center,
            fontSize = 22.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Compartilhe com seu grupo e ajude outros motoristas a avaliar ofertas.",
            color = CalcMotColors.TextSecondary,
            textAlign = TextAlign.Center,
            fontSize = 15.sp,
            lineHeight = 20.sp
        )
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .testTag(UiTestTags.GROWTH_SHARE_PROMPT_ACTION),
            onClick = onShare,
            colors = ButtonDefaults.buttonColors(
                containerColor = CalcMotColors.PrimaryActionBlue,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Share,
                contentDescription = null,
                modifier = Modifier.size(23.dp)
            )
            Text(
                "Compartilhar com motoristas",
                modifier = Modifier.padding(start = 9.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        TextButton(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag(UiTestTags.GROWTH_SHARE_PROMPT_DISMISS)
        ) {
            Text(
                "Depois",
                color = CalcMotColors.TextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun GrowthShareCommunityIllustration() {
    Image(
        painter = painterResource(id = R.drawable.growth_share_community_3d),
        contentDescription = null,
        modifier = Modifier
            .size(130.dp)
            .testTag(UiTestTags.GROWTH_SHARE_ART),
        contentScale = ContentScale.Fit
    )
}
