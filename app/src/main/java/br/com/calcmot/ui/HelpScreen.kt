package br.com.calcmot.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.calcmot.ui.design.theme.CalcMotTheme
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotShape

@Composable
fun HelpScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onSupport: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CalcMotColors.AppBackground)
            .testTag(UiTestTags.HELP_SCREEN)
    ) {
        HelpBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HelpTopBar(onBack = onBack)
            HelpHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            )
            HelpFaqCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                onOpenPrivacy = onOpenPrivacy
            )
        }
    }
}

@Composable
private fun HelpTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Voltar",
                tint = CalcMotColors.TextPrimary,
                modifier = Modifier.size(30.dp)
            )
        }
        Text(
            text = "Ajuda",
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
private fun HelpHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HelpHeroIcon()
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = buildAnnotatedString {
                    append("Como o ")
                    withStyle(
                        SpanStyle(
                            color = CalcMotColors.TextPrimary,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Black
                        )
                    ) {
                        append("Calc")
                    }
                    withStyle(
                        SpanStyle(
                            color = CalcMotColors.Success,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Black
                        )
                    ) {
                        append("Mot")
                    }
                    append(" ajuda você")
                },
                color = CalcMotColors.TextPrimary,
                fontSize = 21.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 26.sp
            )
            Text(
                text = "Entenda as principais funções do app\nem poucos segundos.",
                color = CalcMotColors.TextSecondary,
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun HelpHeroIcon() {
    Box(
        modifier = Modifier.size(76.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .border(3.dp, CalcMotColors.Success, CircleShape)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
            contentDescription = null,
            tint = CalcMotColors.Success,
            modifier = Modifier.size(58.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(0.35f)
                .height(18.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            CalcMotColors.BorderStrong.copy(alpha = 0.34f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
private fun HelpFaqCard(
    modifier: Modifier = Modifier,
    onOpenPrivacy: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(BooleanArray(5)) }

    fun toggle(index: Int) {
        expanded = expanded.copyOf().also { it[index] = !it[index] }
    }
    Column(
        modifier = modifier
            .border(
                BorderStroke(1.dp, CalcMotColors.BorderSubtle),
                RoundedCornerShape(22.dp)
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        CalcMotColors.SurfaceElevated.copy(alpha = 0.74f),
                        CalcMotColors.Surface.copy(alpha = 0.86f)
                    )
                ),
                RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        HelpFaqItem(
            number = 1,
            title = "O que o CalcMot faz?",
            body = "Ele mostra R$/km, R$/h e uma classificação simples para ajudar você a decidir se uma oferta compensa.",
            expanded = expanded[0],
            onToggle = { toggle(0) }
        )
        HelpFaqItem(
            number = 2,
            title = "Por que preciso ativar acessibilidade?",
            body = "As ofertas aparecem em outro app e duram poucos segundos. A acessibilidade permite que o CalcMot identifique informações visíveis da oferta e mostre o cálculo de forma mais clara.",
            expanded = expanded[1],
            onToggle = { toggle(1) }
        )
        HelpFaqItem(
            number = 3,
            title = "O CalcMot aceita corrida sozinho?",
            body = "Não. O CalcMot não toca na tela, não aceita e não recusa corridas.",
            expanded = expanded[2],
            onToggle = { toggle(2) }
        )
        HelpFaqItem(
            number = 4,
            title = "O CalcMot funciona fora dos apps de motorista?",
            body = "Fora dos apps compatíveis, ele fica em espera.",
            expanded = expanded[3],
            onToggle = { toggle(3) }
        )
        HelpFaqItem(
            number = 5,
            title = "Como a classificação é calculada?",
            body = "O app compara R$/km e R$/h com a meta que você definiu.",
            expanded = expanded[4],
            onToggle = { toggle(4) },
            showDivider = false
        )

        HelpControlCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        )

        TextButton(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp)
                .testTag(UiTestTags.HELP_PRIVACY_BUTTON),
            onClick = onOpenPrivacy
        ) {
            Icon(
                imageVector = Icons.Outlined.Security,
                contentDescription = null,
                tint = CalcMotColors.BrandSecondary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = "Ver política de privacidade",
                modifier = Modifier.padding(start = 12.dp),
                color = CalcMotColors.BrandSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = CalcMotColors.BrandSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun HelpFaqItem(
    number: Int,
    title: String,
    body: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    showDivider: Boolean = true
) {
    Column(
        modifier = Modifier
            .testTag(UiTestTags.FAQ_ITEM)
            .clickable(onClick = onToggle)
            .semantics {
                role = Role.Button
                stateDescription = if (expanded) "Resposta expandida" else "Resposta recolhida"
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .border(1.3.dp, CalcMotColors.Success, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number.toString(),
                    color = CalcMotColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        color = CalcMotColors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 19.sp
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = CalcMotColors.TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                if (expanded) {
                    Text(
                        text = body,
                        color = CalcMotColors.TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 10.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(CalcMotColors.BorderSubtle)
            )
        }
    }
}

@Composable
private fun HelpControlCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .border(
                BorderStroke(1.dp, CalcMotColors.BorderSubtle),
                RoundedCornerShape(CalcMotShape.Lg)
            )
            .background(
                CalcMotColors.Surface.copy(alpha = 0.52f),
                RoundedCornerShape(CalcMotShape.Lg)
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Security,
            contentDescription = null,
            tint = CalcMotColors.Success,
            modifier = Modifier.size(38.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = "Você mantém o controle",
                color = CalcMotColors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "O CalcMot não controla outros apps e\nnão toma decisões por você.",
                color = CalcMotColors.TextSecondary,
                fontSize = 13.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun HelpBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CalcMotColors.Success.copy(alpha = 0.07f),
                        CalcMotColors.AppBackground.copy(alpha = 0.74f),
                        CalcMotColors.AppBackground
                    )
                )
            )
    )
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun HelpScreenPreview() {
    CalcMotTheme {
        HelpScreen(
            onBack = {},
            onOpenPrivacy = {},
            onSupport = {}
        )
    }
}
