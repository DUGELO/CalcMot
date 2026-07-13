package br.com.calcmot.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Traffic
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.calcmot.R
import br.com.calcmot.ui.design.theme.CalcMotTheme
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotShape

@Composable
fun PremiumScreen(
    onBack: () -> Unit,
    onStartNow: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CalcMotColors.AppBackground)
            .testTag(UiTestTags.PREMIUM_SCREEN)
    ) {
        PremiumBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PremiumTopBar(onBack = onBack)

            PremiumLogoMark(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .height(56.dp)
            )

            PremiumHeadline(modifier = Modifier.padding(top = 6.dp))

            PremiumPlanCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            )

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .testTag(UiTestTags.PREMIUM_START_BUTTON),
                onClick = onStartNow,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CalcMotColors.PrimaryActionBlue,
                    contentColor = CalcMotColors.TextPrimary
                )
            ) {
                Text(
                    text = "Começar agora",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            TextButton(
                modifier = Modifier.testTag(UiTestTags.PREMIUM_SKIP_BUTTON),
                onClick = onSkip
            ) {
                Text(
                    text = "Agora não",
                    color = CalcMotColors.BrandSecondary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            PremiumSafetyFooter(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 0.dp, bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun PremiumTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 46.dp),
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
            text = "Premium",
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
private fun PremiumLogoMark(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.calcmot_logo_hero),
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            contentScale = ContentScale.Fit
        )
        Text(
            text = buildAnnotatedString {
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
            },
            modifier = Modifier.padding(start = 10.dp),
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            color = CalcMotColors.TextPrimary
        )
    }
}

@Composable
private fun PremiumHeadline(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = buildAnnotatedString {
                append("Continue usando o\n")
                withStyle(SpanStyle(color = CalcMotColors.Success)) {
                    append("semáforo de lucro")
                }
            },
            color = CalcMotColors.TextPrimary,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 32.sp
        )
        Text(
            text = "Veja R$/km, R$/h e a classificação\nda oferta enquanto trabalha.",
            color = CalcMotColors.TextSecondary,
            fontSize = 17.sp,
            textAlign = TextAlign.Center,
            lineHeight = 23.sp
        )
    }
}

@Composable
private fun PremiumPlanCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .border(
                width = 1.35.dp,
                color = CalcMotColors.Success.copy(alpha = 0.86f),
                shape = RoundedCornerShape(22.dp)
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        CalcMotColors.SurfaceElevated.copy(alpha = 0.82f),
                        CalcMotColors.Surface.copy(alpha = 0.9f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 18.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = buildAnnotatedString {
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
                    append(" ")
                    withStyle(SpanStyle(color = CalcMotColors.Success, fontWeight = FontWeight.Bold)) {
                        append("Premium")
                    }
                },
                color = CalcMotColors.TextPrimary,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Filled.WorkspacePremium,
                contentDescription = null,
                tint = CalcMotColors.Success,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(24.dp)
            )
        }

        Row(
            modifier = Modifier.padding(top = 17.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "R$",
                color = CalcMotColors.TextSecondary,
                fontSize = 22.sp,
                modifier = Modifier.padding(bottom = 7.dp)
            )
            Text(
                text = " 9,90",
                color = CalcMotColors.TextPrimary,
                fontSize = 45.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 48.sp
            )
            Text(
                text = "/mês",
                color = CalcMotColors.TextSecondary,
                fontSize = 22.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 7.dp)
            )
        }

        Text(
            text = "Cancele quando quiser.",
            color = CalcMotColors.TextSecondary,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 6.dp)
        )

        PremiumDivider(modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))

        PremiumBenefitRow(
            icon = Icons.Outlined.FlashOn,
            text = "Aviso automático nas ofertas"
        )
        PremiumBenefitRow(
            icon = Icons.Outlined.Flag,
            text = "Meta personalizada por km e por hora"
        )
        PremiumBenefitRow(
            icon = Icons.Outlined.Traffic,
            text = "Classificação Boa, Média ou Ruim"
        )
        PremiumBenefitRow(
            icon = Icons.Outlined.Timer,
            text = "Apoio rápido para decidir\nantes da oferta sumir",
            showDivider = false
        )
    }
}

@Composable
private fun PremiumBenefitRow(
    icon: ImageVector,
    text: String,
    showDivider: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CalcMotColors.Success,
                modifier = Modifier.size(30.dp)
            )
            Text(
                text = text,
                color = CalcMotColors.TextPrimary,
                fontSize = 16.sp,
                lineHeight = 21.sp
            )
        }
        if (showDivider) {
            PremiumDivider()
        }
    }
}

@Composable
private fun PremiumSafetyFooter(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Security,
            contentDescription = null,
            tint = CalcMotColors.Success,
            modifier = Modifier.size(36.dp)
        )
        Text(
            text = "O CalcMot não aceita corridas por você\ne não controla outros apps.",
            modifier = Modifier.padding(start = 16.dp),
            color = CalcMotColors.TextSecondary,
            fontSize = 14.sp,
            lineHeight = 19.sp
        )
    }
}

@Composable
private fun PremiumDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(CalcMotColors.BorderSubtle)
    )
}

@Composable
private fun PremiumBackground() {
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
private fun PremiumScreenPreview() {
    CalcMotTheme {
        PremiumScreen(
            onBack = {},
            onStartNow = {},
            onSkip = {}
        )
    }
}
