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
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Refresh
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
fun HomeReadyScreen(
    onMenu: () -> Unit,
    onOpenUber: () -> Unit,
    onOpen99: () -> Unit,
    onOpenGoal: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    onRestartReading: () -> Unit = {},
    modifier: Modifier = Modifier,
    goalPerKm: String = "R$ 1,80/km",
    goalPerHour: String = "R$ 35/h"
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CalcMotColors.AppBackground)
            .testTag(UiTestTags.HOME_READY_SCREEN)
    ) {
        HomeReadyBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HomeReadyTopBar(onMenu = onMenu)
            HomeReadyHeroCard(modifier = Modifier.fillMaxWidth())
            HomeReadyGoalCard(
                goalPerKm = goalPerKm,
                goalPerHour = goalPerHour,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DriverAppButton(
                    text = "Abrir Uber",
                    modifier = Modifier
                        .weight(1f)
                        .testTag(UiTestTags.OPEN_UBER_DRIVER_BUTTON),
                    onClick = onOpenUber
                )
                DriverAppButton(
                    text = "Abrir 99",
                    modifier = Modifier
                        .weight(1f)
                        .testTag(UiTestTags.OPEN_99_DRIVER_BUTTON),
                    onClick = onOpen99
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HomeReadySecondaryButton(
                    text = "Minha meta",
                    icon = Icons.Outlined.MyLocation,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(UiTestTags.HOME_GOAL_ACTION),
                    onClick = onOpenGoal
                )
                HomeReadySecondaryButton(
                    text = "Configurações",
                    icon = Icons.Outlined.Settings,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(UiTestTags.HOME_SETTINGS_ACTION),
                    onClick = onOpenSettings
                )
            }
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag(UiTestTags.RESTART_READING_BUTTON),
                onClick = onRestartReading,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, CalcMotColors.BorderSubtle),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CalcMotColors.BrandSecondary)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Reiniciar leitura",
                    modifier = Modifier.padding(start = 8.dp),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag(UiTestTags.HOME_FOOTER_HELP),
                onClick = onOpenHelp,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, CalcMotColors.BorderSubtle),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CalcMotColors.BrandSecondary)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Como funciona",
                    modifier = Modifier.padding(start = 8.dp),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
            HomeReadySafetyFooter(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 18.dp)
            )
        }
    }
}

@Composable
private fun HomeReadyTopBar(onMenu: () -> Unit) {
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
private fun HomeReadyHeroCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .testTag(UiTestTags.HOME_HERO_CARD)
            .border(
                BorderStroke(1.25.dp, CalcMotColors.Success.copy(alpha = 0.62f)),
                RoundedCornerShape(22.dp)
            )
            .background(
                Brush.verticalGradient(
                    listOf(
                        CalcMotColors.SurfaceElevated.copy(alpha = 0.76f),
                        CalcMotColors.Surface.copy(alpha = 0.88f)
                    )
                ),
                RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 22.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.calcmot_logo_hero),
            contentDescription = null,
            modifier = Modifier.size(140.dp),
            contentScale = ContentScale.Fit
        )
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = CalcMotColors.Success)) {
                    append("Pronto")
                }
                append(" para calcular")
            },
            modifier = Modifier.padding(top = 12.dp),
            color = CalcMotColors.TextPrimary,
            fontSize = 29.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 34.sp
        )
        Text(
            text = "Abra seu app de motorista.\nQuando uma oferta aparecer, o CalcMot\nmostra o aviso automaticamente.",
            modifier = Modifier.padding(top = 12.dp),
            color = CalcMotColors.TextSecondary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 23.sp
        )
        Row(
            modifier = Modifier
                .padding(top = 22.dp)
                .border(
                    BorderStroke(1.dp, CalcMotColors.Success.copy(alpha = 0.4f)),
                    RoundedCornerShape(48.dp)
                )
                .background(CalcMotColors.Success.copy(alpha = 0.08f), RoundedCornerShape(48.dp))
                .padding(horizontal = 22.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(CalcMotColors.Success)
            )
            Text(
                text = "Cálculo automático ativo",
                color = CalcMotColors.Success,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun HomeReadyGoalCard(
    goalPerKm: String,
    goalPerHour: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .testTag(UiTestTags.HOME_GOAL_CARD)
            .border(
                BorderStroke(1.dp, CalcMotColors.BorderSubtle),
                RoundedCornerShape(CalcMotShape.Lg)
            )
            .background(CalcMotColors.Surface.copy(alpha = 0.74f), RoundedCornerShape(CalcMotShape.Lg))
            .padding(horizontal = 22.dp, vertical = 18.dp)
    ) {
        Text(
            text = "Meta atual",
            color = CalcMotColors.TextPrimary,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GoalMetric(
                icon = Icons.Outlined.Route,
                value = goalPerKm,
                label = "Meta por km",
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .size(width = 1.dp, height = 48.dp)
                    .background(CalcMotColors.BorderSubtle)
            )
            GoalMetric(
                icon = Icons.Outlined.AccessTime,
                value = goalPerHour,
                label = "Meta por hora",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun GoalMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CalcMotColors.Success,
            modifier = Modifier.size(34.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = value,
                color = CalcMotColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = label,
                color = CalcMotColors.TextSecondary,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun DriverAppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        modifier = modifier.heightIn(min = 56.dp),
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = CalcMotColors.PrimaryActionBlue,
            contentColor = CalcMotColors.TextPrimary
        )
    ) {
        Icon(
            imageVector = Icons.Outlined.DirectionsCar,
            contentDescription = null,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 8.dp),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun HomeReadySecondaryButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        modifier = modifier.heightIn(min = 54.dp),
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, CalcMotColors.BorderSubtle),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = CalcMotColors.BrandSecondary
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 8.dp),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun HomeReadySafetyFooter(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Security,
            contentDescription = null,
            tint = CalcMotColors.Success,
            modifier = Modifier.size(34.dp)
        )
        Text(
            text = "O CalcMot está ativo e pronto para te ajudar\na decidir melhor e aumentar seu lucro.",
            modifier = Modifier.padding(start = 16.dp),
            color = CalcMotColors.TextSecondary,
            fontSize = 14.sp,
            lineHeight = 19.sp
        )
    }
}

@Composable
private fun HomeReadyBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(
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
private fun HomeReadyScreenPreview() {
    CalcMotTheme {
        HomeReadyScreen(
            onMenu = {},
            onOpenUber = {},
            onOpen99 = {},
            onOpenGoal = {},
            onOpenSettings = {},
            onOpenHelp = {}
        )
    }
}
