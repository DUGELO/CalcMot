package br.com.calcmot.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.calcmot.ui.design.theme.CalcMotTheme
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotShape

@Composable
fun FeedbackScreen(
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    onEmail: () -> Unit,
    modifier: Modifier = Modifier,
    supportEmail: String = CALCMOT_SUPPORT_EMAIL
) {
    var selectedType by remember { mutableStateOf(FeedbackType.SUGGESTION) }
    var message by remember { mutableStateOf("") }
    var includeAppInfo by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CalcMotColors.AppBackground)
            .testTag(UiTestTags.FEEDBACK_FORM_SCREEN)
    ) {
        FeedbackFormBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            FeedbackFormTopBar(onBack = onBack)
            FeedbackFormHero()
            FeedbackFormCard(
                selectedType = selectedType,
                onTypeSelected = { selectedType = it },
                message = message,
                onMessageChange = { message = it.take(MAX_FEEDBACK_LENGTH) },
                includeAppInfo = includeAppInfo,
                onIncludeAppInfoChange = { includeAppInfo = it },
                modifier = Modifier.fillMaxWidth()
            )
            FeedbackPrivacySummary(modifier = Modifier.fillMaxWidth())
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 58.dp)
                    .testTag(UiTestTags.FEEDBACK_SUBMIT_BUTTON),
                onClick = onSubmit,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CalcMotColors.PrimaryActionBlue,
                    contentColor = CalcMotColors.TextPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = null,
                    modifier = Modifier.size(25.dp)
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = "Enviar feedback",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            TextButton(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTag(UiTestTags.FEEDBACK_EMAIL_BUTTON),
                onClick = onEmail
            ) {
                Text(
                    text = "Enviar por e-mail",
                    color = CalcMotColors.BrandSecondary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Email,
                    contentDescription = null,
                    tint = CalcMotColors.TextMuted,
                    modifier = Modifier.size(23.dp)
                )
                Text(
                    text = supportEmail,
                    color = CalcMotColors.TextSecondary,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FeedbackFormTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Voltar",
                tint = CalcMotColors.TextPrimary,
                modifier = Modifier.size(31.dp)
            )
        }
        Text(
            text = "Enviar feedback",
            modifier = Modifier.padding(start = 14.dp),
            color = CalcMotColors.TextPrimary,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun FeedbackFormHero() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(78.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            listOf(CalcMotColors.Success.copy(alpha = 0.2f), Color.Transparent)
                        )
                    )
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                contentDescription = null,
                tint = CalcMotColors.Success,
                modifier = Modifier.size(62.dp)
            )
            Icon(
                imageVector = Icons.Outlined.Speed,
                contentDescription = null,
                tint = CalcMotColors.TextPrimary,
                modifier = Modifier.size(30.dp)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(31.dp)
                    .background(CalcMotColors.Success, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = CalcMotColors.AppBackground,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
        Text(
            text = "Como podemos melhorar?",
            modifier = Modifier.padding(top = 8.dp),
            color = CalcMotColors.TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 30.sp
        )
        Text(
            text = "Conte o que aconteceu ou envie uma sugestão\npara deixar o CalcMot mais útil na sua jornada.",
            modifier = Modifier.padding(top = 6.dp),
            color = CalcMotColors.TextSecondary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 23.sp
        )
    }
}

@Composable
private fun FeedbackFormCard(
    selectedType: FeedbackType,
    onTypeSelected: (FeedbackType) -> Unit,
    message: String,
    onMessageChange: (String) -> Unit,
    includeAppInfo: Boolean,
    onIncludeAppInfoChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .border(
                BorderStroke(1.dp, CalcMotColors.BorderSubtle),
                RoundedCornerShape(CalcMotShape.Lg)
            )
            .background(CalcMotColors.Surface.copy(alpha = 0.72f), RoundedCornerShape(CalcMotShape.Lg))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Tipo de feedback",
            color = CalcMotColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            FeedbackType.entries.forEach { type ->
                FeedbackTypeChip(
                    type = type,
                    selected = selectedType == type,
                    onClick = { onTypeSelected(type) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag(type.testTag)
                )
            }
        }
        DividerLine()
        Text(
            text = "Mensagem",
            color = CalcMotColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        OutlinedTextField(
            value = message,
            onValueChange = onMessageChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .testTag(UiTestTags.FEEDBACK_MESSAGE_INPUT),
            placeholder = {
                Text(
                    text = "Escreva aqui sua dúvida, sugestão ou problema...",
                    color = CalcMotColors.TextMuted,
                    fontSize = 15.sp
                )
            },
            supportingText = {
                Text(
                    text = "${message.length}/$MAX_FEEDBACK_LENGTH",
                    modifier = Modifier.fillMaxWidth(),
                    color = CalcMotColors.TextSecondary,
                    textAlign = TextAlign.End
                )
            },
            minLines = 4,
            maxLines = 5,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = CalcMotColors.TextPrimary,
                unfocusedTextColor = CalcMotColors.TextPrimary,
                focusedBorderColor = CalcMotColors.BorderStrong,
                unfocusedBorderColor = CalcMotColors.BorderSubtle,
                focusedContainerColor = CalcMotColors.Surface.copy(alpha = 0.42f),
                unfocusedContainerColor = CalcMotColors.Surface.copy(alpha = 0.42f),
                cursorColor = CalcMotColors.BrandSecondary
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(CalcMotColors.SurfaceSoft.copy(alpha = 0.76f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                    tint = CalcMotColors.TextPrimary,
                    modifier = Modifier.size(23.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "Incluir informações do app",
                    color = CalcMotColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Envia versão do app e estado das permissões para ajudar no suporte. Não enviamos dados sensíveis da corrida.",
                    color = CalcMotColors.TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 15.sp
                )
            }
            Switch(
                modifier = Modifier.testTag(UiTestTags.FEEDBACK_INCLUDE_APP_INFO_SWITCH),
                checked = includeAppInfo,
                onCheckedChange = onIncludeAppInfoChange
            )
        }
    }
}

@Composable
private fun FeedbackTypeChip(
    type: FeedbackType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .heightIn(min = 50.dp)
            .border(
                BorderStroke(
                    width = 1.dp,
                    color = if (selected) CalcMotColors.Success else CalcMotColors.BorderSubtle
                ),
                RoundedCornerShape(50.dp)
            )
            .background(
                if (selected) CalcMotColors.Success.copy(alpha = 0.08f) else Color.Transparent,
                RoundedCornerShape(50.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = type.icon,
            contentDescription = null,
            tint = if (selected) CalcMotColors.Success else CalcMotColors.TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = type.label,
            modifier = Modifier.padding(start = 7.dp),
            color = if (selected) CalcMotColors.Success else CalcMotColors.TextSecondary,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun FeedbackPrivacySummary(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .border(
                BorderStroke(1.dp, CalcMotColors.BorderSubtle),
                RoundedCornerShape(CalcMotShape.Lg)
            )
            .background(CalcMotColors.Surface.copy(alpha = 0.72f), RoundedCornerShape(CalcMotShape.Lg))
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(CalcMotColors.Success.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Security,
                contentDescription = null,
                tint = CalcMotColors.Success,
                modifier = Modifier.size(32.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Privacidade",
                color = CalcMotColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "O CalcMot não envia senhas, dados bancários,\nendereço de passageiro ou conteúdo de\noutros apps.",
                color = CalcMotColors.TextSecondary,
                fontSize = 14.sp,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(CalcMotColors.BorderSubtle)
    )
}

@Composable
private fun FeedbackFormBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(
                        CalcMotColors.Success.copy(alpha = 0.07f),
                        CalcMotColors.AppBackground.copy(alpha = 0.72f),
                        CalcMotColors.AppBackground
                    )
                )
            )
    )
}

private enum class FeedbackType(
    val label: String,
    val icon: ImageVector,
    val testTag: String
) {
    PROBLEM("Problema", Icons.Outlined.ReportProblem, UiTestTags.FEEDBACK_TYPE_PROBLEM),
    SUGGESTION("Sugestão", Icons.Outlined.Lightbulb, UiTestTags.FEEDBACK_TYPE_SUGGESTION),
    QUESTION("Dúvida", Icons.AutoMirrored.Outlined.HelpOutline, UiTestTags.FEEDBACK_TYPE_QUESTION)
}

private const val MAX_FEEDBACK_LENGTH = 1000

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun FeedbackScreenPreview() {
    CalcMotTheme {
        FeedbackScreen(
            onBack = {},
            onSubmit = {},
            onEmail = {}
        )
    }
}
