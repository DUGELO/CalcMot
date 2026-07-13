package br.com.calcmot.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
fun HomePausedScreen(
    onMenu: () -> Unit,
    onResume: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    goalPerKm: String = "R$ 1,80/km",
    goalPerHour: String = "R$ 35/h"
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CalcMotColors.AppBackground)
            .testTag(UiTestTags.HOME_SCREEN)
    ) {
        HomePausedBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HomePausedTopBar(onMenu = onMenu)
            HomePausedHeroCard(modifier = Modifier.fillMaxWidth())
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .testTag(UiTestTags.HOME_PRIMARY_ACTION),
                onClick = onResume,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CalcMotColors.PrimaryActionBlue,
                    contentColor = CalcMotColors.TextPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = "Ligar cálculo automático",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .testTag(UiTestTags.HOME_SETTINGS_ACTION),
                onClick = onOpenSettings,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CalcMotColors.BorderSubtle),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CalcMotColors.BrandSecondary)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(23.dp)
                )
                Text(
                    text = "Configurações",
                    modifier = Modifier.padding(start = 10.dp),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            HomePausedGoalCard(
                goalPerKm = goalPerKm,
                goalPerHour = goalPerHour,
                modifier = Modifier.fillMaxWidth()
            )
            HomePausedFooter(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 20.dp)
            )
        }
    }
}

@Composable
private fun HomePausedTopBar(onMenu: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onMenu,
            modifier = Modifier
                .size(48.dp)
                .testTag(UiTestTags.DRAWER_MENU_BUTTON)
        ) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = "Abrir menu",
                tint = CalcMotColors.TextPrimary,
                modifier = Modifier.size(31.dp)
            )
        }
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
            modifier = Modifier.padding(start = 22.dp),
            color = CalcMotColors.TextPrimary,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun HomePausedHeroCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .testTag(UiTestTags.HOME_HERO_CARD)
            .border(BorderStroke(1.dp, CalcMotColors.BorderSubtle), RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        CalcMotColors.SurfaceElevated.copy(alpha = 0.72f),
                        CalcMotColors.Surface.copy(alpha = 0.86f)
                    )
                ),
                RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 22.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(142.dp), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = R.drawable.calcmot_logo_hero),
                contentDescription = null,
                modifier = Modifier.size(126.dp),
                contentScale = ContentScale.Fit,
                alpha = 0.44f
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(CalcMotColors.Surface.copy(alpha = 0.92f))
                    .border(3.dp, CalcMotColors.BrandSecondary.copy(alpha = 0.82f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    tint = CalcMotColors.BrandSecondary,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
        Text(
            text = buildAnnotatedString {
                append("Cálculo ")
                withStyle(SpanStyle(color = CalcMotColors.BrandSecondary)) {
                    append("pausado")
                }
            },
            modifier = Modifier.padding(top = 12.dp),
            color = CalcMotColors.TextPrimary,
            fontSize = 29.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 34.sp
        )
        Text(
            text = "O CalcMot não vai mostrar avisos\nenquanto estiver pausado.",
            modifier = Modifier.padding(top = 12.dp),
            color = CalcMotColors.TextSecondary,
            fontSize = 17.sp,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        Box(
            modifier = Modifier
                .padding(top = 20.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(CalcMotColors.BorderSubtle)
        )
        Text(
            text = "Você pode ligar novamente quando for começar a dirigir.",
            modifier = Modifier.padding(top = 18.dp),
            color = CalcMotColors.TextSecondary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 23.sp
        )
    }
}

@Composable
private fun HomePausedGoalCard(
    goalPerKm: String,
    goalPerHour: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .testTag(UiTestTags.HOME_GOAL_CARD)
            .border(BorderStroke(1.dp, CalcMotColors.BorderSubtle), RoundedCornerShape(CalcMotShape.Lg))
            .background(CalcMotColors.Surface.copy(alpha = 0.74f), RoundedCornerShape(CalcMotShape.Lg))
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.MyLocation,
            contentDescription = null,
            tint = CalcMotColors.Success,
            modifier = Modifier.size(38.dp)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Meta atual",
                color = CalcMotColors.TextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$goalPerKm · $goalPerHour",
                color = CalcMotColors.TextSecondary,
                fontSize = 16.sp
            )
        }
        Icon(
            imageVector = Icons.Outlined.AccessTime,
            contentDescription = null,
            tint = CalcMotColors.Success,
            modifier = Modifier.size(34.dp)
        )
    }
}

@Composable
private fun HomePausedFooter(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Security,
            contentDescription = null,
            tint = CalcMotColors.Success,
            modifier = Modifier.size(31.dp)
        )
        Text(
            text = "Permissão ativa. Você mantém o controle.",
            modifier = Modifier.padding(start = 14.dp),
            color = CalcMotColors.TextSecondary,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun HomePausedBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(
                        CalcMotColors.BrandSecondary.copy(alpha = 0.08f),
                        CalcMotColors.AppBackground.copy(alpha = 0.74f),
                        CalcMotColors.AppBackground
                    )
                )
            )
    )
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun HomePausedScreenPreview() {
    CalcMotTheme {
        HomePausedScreen(
            onMenu = {},
            onResume = {},
            onOpenSettings = {}
        )
    }
}

@Preview(widthDp = 360, heightDp = 800, fontScale = 1.2f)
@Composable
private fun HomePausedSmallFontScalePreview() {
    CalcMotTheme {
        HomePausedScreen(
            onMenu = {},
            onResume = {},
            onOpenSettings = {}
        )
    }
}
