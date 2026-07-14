package br.com.calcmot.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.calcmot.ui.design.theme.CalcMotTheme
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotShape

const val CALCMOT_PRIVACY_POLICY_URL = "https://dugelo.github.io/calcmot-privacy-policy/"

@Composable
fun PrivacyPolicyScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onSupport: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CalcMotColors.AppBackground)
            .testTag(UiTestTags.PRIVACY_POLICY_SCREEN)
    ) {
        PrivacyBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PrivacyTopBar(onBack = onBack)
            PrivacyHeroCard(modifier = Modifier.fillMaxWidth())
            PrivacySectionCard(
                title = "O que o CalcMot identifica",
                leadingIcon = Icons.Outlined.Search,
                items = listOf(
                    PrivacyRow(Icons.Outlined.AttachMoney, "Valor da oferta"),
                    PrivacyRow(Icons.Outlined.LocationOn, "Distância da corrida"),
                    PrivacyRow(Icons.Outlined.AccessTime, "Tempo estimado"),
                    PrivacyRow(Icons.Outlined.Visibility, "Informações visíveis necessárias para o cálculo")
                )
            )
            PrivacySectionCard(
                title = "O que o CalcMot nao faz",
                leadingIcon = Icons.Outlined.Security,
                items = listOf(
                    PrivacyRow(Icons.Outlined.Security, "Não coleta senhas"),
                    PrivacyRow(Icons.Outlined.Security, "Não aceita ou recusa corridas"),
                    PrivacyRow(Icons.Outlined.Security, "Não toca na tela"),
                    PrivacyRow(Icons.Outlined.Security, "Não controla outros apps"),
                    PrivacyRow(Icons.Outlined.Security, "Não acessa apps bancários"),
                    PrivacyRow(Icons.Outlined.Security, "Não compartilha informações da oferta com terceiros")
                )
            )
            PrivacyInfoCard(
                icon = Icons.Outlined.HourglassEmpty,
                title = "Quando o app fica em espera",
                body = "Fora dos apps de motorista compatíveis, o CalcMot não calcula ofertas e permanece em espera."
            )
            PrivacyInfoCard(
                icon = Icons.Outlined.Email,
                title = "Contato",
                body = CALCMOT_SUPPORT_EMAIL
            )
            TextButton(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTag(UiTestTags.PRIVACY_POLICY_SUPPORT_BUTTON),
                onClick = onSupport
            ) {
                Icon(
                    imageVector = Icons.Outlined.Security,
                    contentDescription = null,
                    tint = CalcMotColors.BrandSecondary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = "Ver política completa",
                    color = CalcMotColors.BrandSecondary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = CalcMotColors.BrandSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun PrivacyTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            modifier = Modifier
                .size(48.dp)
                .testTag(UiTestTags.PRIVACY_POLICY_BACK_BUTTON),
            onClick = onBack
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Voltar",
                tint = CalcMotColors.TextPrimary,
                modifier = Modifier.size(31.dp)
            )
        }
        Text(
            text = "Privacidade",
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
private fun PrivacyHeroCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .border(
                BorderStroke(1.2.dp, CalcMotColors.Success.copy(alpha = 0.62f)),
                RoundedCornerShape(21.dp)
            )
            .background(
                Brush.verticalGradient(
                    listOf(
                        CalcMotColors.SurfaceElevated.copy(alpha = 0.76f),
                        CalcMotColors.Surface.copy(alpha = 0.86f)
                    )
                ),
                RoundedCornerShape(21.dp)
            )
            .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            listOf(CalcMotColors.Success.copy(alpha = 0.22f), Color.Transparent)
                        )
                    )
            )
            Icon(
                imageVector = Icons.Filled.Security,
                contentDescription = null,
                tint = CalcMotColors.Success,
                modifier = Modifier.size(62.dp)
            )
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = CalcMotColors.AppBackground,
                modifier = Modifier.size(31.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 17.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Sua decisão, seus dados",
                color = CalcMotColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 24.sp
            )
            Text(
                text = "O CalcMot usa informações visíveis da oferta apenas para calcular e mostrar o semáforo de lucro.",
                color = CalcMotColors.TextSecondary,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }
        Icon(
            imageVector = Icons.Outlined.Speed,
            contentDescription = null,
            tint = CalcMotColors.Success.copy(alpha = 0.28f),
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
private fun PrivacySectionCard(
    title: String,
    leadingIcon: ImageVector,
    items: List<PrivacyRow>,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .border(
                BorderStroke(1.dp, CalcMotColors.BorderSubtle),
                RoundedCornerShape(CalcMotShape.Lg)
            )
            .background(CalcMotColors.Surface.copy(alpha = 0.68f), RoundedCornerShape(CalcMotShape.Lg))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .semantics {
                    role = Role.Button
                    stateDescription = if (expanded) "Seção expandida" else "Seção recolhida"
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = CalcMotColors.Success,
                modifier = Modifier.size(31.dp)
            )
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = CalcMotColors.TextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = CalcMotColors.TextSecondary,
                modifier = Modifier.size(28.dp)
            )
        }
        if (expanded) {
            items.forEachIndexed { index, item ->
                PrivacyListLine(row = item)
                if (index != items.lastIndex) PrivacyDivider()
            }
        }
    }
}

@Composable
private fun PrivacyListLine(row: PrivacyRow) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Icon(
            imageVector = row.icon,
            contentDescription = null,
            tint = CalcMotColors.Success,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = row.text,
            color = CalcMotColors.TextSecondary,
            fontSize = 15.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun PrivacyInfoCard(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(
                BorderStroke(1.dp, CalcMotColors.BorderSubtle),
                RoundedCornerShape(CalcMotShape.Lg)
            )
            .background(CalcMotColors.Surface.copy(alpha = 0.68f), RoundedCornerShape(CalcMotShape.Lg))
            .padding(horizontal = 18.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CalcMotColors.Success,
            modifier = Modifier.size(34.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = title,
                color = CalcMotColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = body,
                color = CalcMotColors.TextSecondary,
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun PrivacyDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 46.dp)
            .height(1.dp)
            .background(CalcMotColors.BorderSubtle)
    )
}

@Composable
private fun PrivacyBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(
                        CalcMotColors.Success.copy(alpha = 0.07f),
                        CalcMotColors.AppBackground.copy(alpha = 0.76f),
                        CalcMotColors.AppBackground
                    )
                )
            )
    )
}

private data class PrivacyRow(
    val icon: ImageVector,
    val text: String
)

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun PrivacyPolicyScreenPreview() {
    CalcMotTheme {
        PrivacyPolicyScreen(
            onBack = {},
            onSupport = {}
        )
    }
}
