package br.com.calcmot.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.calcmot.ui.design.theme.CalcMotTheme
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotShape

@Composable
fun FeedbackSuccessScreen(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onSendAnotherFeedback: () -> Unit,
    modifier: Modifier = Modifier,
    supportEmail: String = CALCMOT_SUPPORT_EMAIL
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CalcMotColors.AppBackground)
            .testTag(UiTestTags.FEEDBACK_SUCCESS_SCREEN)
    ) {
        FeedbackDarkBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FeedbackTopBar(onBack = onBack)

            FeedbackSuccessHeroCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            )

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 58.dp)
                    .testTag(UiTestTags.FEEDBACK_SUCCESS_HOME_BUTTON),
                onClick = onHome,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CalcMotColors.PrimaryActionBlue,
                    contentColor = CalcMotColors.TextPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = "Voltar ao início",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            TextButton(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTag(UiTestTags.FEEDBACK_SUCCESS_SEND_ANOTHER_BUTTON),
                onClick = onSendAnotherFeedback
            ) {
                Text(
                    text = "Enviar outro feedback",
                    color = CalcMotColors.BrandSecondary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Email,
                    contentDescription = null,
                    tint = CalcMotColors.TextMuted,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = supportEmail,
                    color = CalcMotColors.TextSecondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun FeedbackTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Voltar",
                tint = CalcMotColors.TextPrimary,
                modifier = Modifier.size(30.dp)
            )
        }
        Text(
            text = "Feedback",
            modifier = Modifier.padding(start = 12.dp),
            color = CalcMotColors.TextPrimary,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun FeedbackSuccessHeroCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .border(
                width = 1.4.dp,
                color = CalcMotColors.BorderStrong,
                shape = RoundedCornerShape(22.dp)
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        CalcMotColors.SurfaceElevated.copy(alpha = 0.74f),
                        CalcMotColors.Surface.copy(alpha = 0.82f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 18.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                CalcMotColors.Success.copy(alpha = 0.28f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = CalcMotColors.Success,
                modifier = Modifier.size(84.dp)
            )
        }

        Text(
            text = buildAnnotatedString {
                append("Feedback ")
                withStyle(SpanStyle(color = CalcMotColors.Success)) {
                    append("enviado")
                }
            },
            modifier = Modifier.padding(top = 14.dp),
            color = CalcMotColors.TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 30.sp,
            maxLines = 1
        )
        Text(
            text = "Obrigado por ajudar a melhorar o CalcMot.",
            modifier = Modifier.padding(top = 10.dp),
            color = CalcMotColors.TextSecondary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            lineHeight = 21.sp
        )

        FeedbackDivider(modifier = Modifier.padding(top = 18.dp, bottom = 18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FeedbackIconBubble {
                Icon(
                    imageVector = Icons.Outlined.Email,
                    contentDescription = null,
                    tint = CalcMotColors.Success,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = "Sua mensagem foi registrada.\nSe for necessário, entraremos em contato pelo e-mail informado.",
                modifier = Modifier.weight(1f),
                color = CalcMotColors.TextSecondary,
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
        }

        FeedbackPrivacyCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp)
        )
    }
}

@Composable
private fun FeedbackPrivacyCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .border(
                BorderStroke(1.dp, CalcMotColors.BorderSubtle),
                RoundedCornerShape(CalcMotShape.Lg)
            )
            .background(
                CalcMotColors.Surface.copy(alpha = 0.58f),
                RoundedCornerShape(CalcMotShape.Lg)
            )
            .padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FeedbackIconBubble {
            Icon(
                imageVector = Icons.Outlined.Security,
                contentDescription = null,
                tint = CalcMotColors.Success,
                modifier = Modifier.size(30.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Recebemos apenas o necessário",
                color = CalcMotColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 21.sp
            )
            Text(
                text = "As informações enviadas servem para entender sua dúvida, sugestão ou problema.\nO CalcMot não envia senhas, dados bancários ou informações sensíveis de corrida.",
                color = CalcMotColors.TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun FeedbackIconBubble(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(CalcMotColors.Success.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun FeedbackDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(CalcMotColors.BorderSubtle)
    )
}

@Composable
private fun FeedbackDarkBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CalcMotColors.Success.copy(alpha = 0.08f),
                        CalcMotColors.AppBackground.copy(alpha = 0.72f),
                        CalcMotColors.AppBackground
                    )
                )
            )
    )
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun FeedbackSuccessScreenPreview() {
    CalcMotTheme {
        FeedbackSuccessScreen(
            onBack = {},
            onHome = {},
            onSendAnotherFeedback = {}
        )
    }
}
