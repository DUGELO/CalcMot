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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.testTag
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
fun HomePermissionRequiredScreen(
    onMenu: () -> Unit,
    onActivatePermission: () -> Unit,
    onHowItWorks: () -> Unit,
    modifier: Modifier = Modifier,
    goalPerKm: String = "R$ 1,80/km",
    goalPerHour: String = "R$ 35/h"
) {
    var activationGuideVisible by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CalcMotColors.AppBackground)
            .testTag(UiTestTags.HOME_PERMISSION_REQUIRED_SCREEN)
    ) {
        PermissionHomeBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PermissionHomeTopBar(onMenu = onMenu)

            PermissionHeroCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp)
            )

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 58.dp)
                    .testTag(UiTestTags.HOME_PERMISSION_REQUIRED_PRIMARY_BUTTON),
                onClick = { activationGuideVisible = true },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CalcMotColors.PrimaryActionBlue,
                    contentColor = CalcMotColors.TextPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Security,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = "Ativar permissão",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            TextButton(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTag(UiTestTags.HOME_PERMISSION_REQUIRED_HELP_BUTTON),
                onClick = onHowItWorks
            ) {
                Text(
                    text = "Como funciona",
                    color = CalcMotColors.BrandSecondary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = CalcMotColors.BrandSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            PermissionGoalCard(
                goalPerKm = goalPerKm,
                goalPerHour = goalPerHour,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    AccessibilityActivationGuideSheet(
        visible = activationGuideVisible,
        onDismissRequest = { activationGuideVisible = false },
        onOpenSettings = {
            activationGuideVisible = false
            onActivatePermission()
        }
    )
}

@Composable
private fun PermissionHomeTopBar(onMenu: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            modifier = Modifier
                .size(48.dp)
                .testTag(UiTestTags.DRAWER_MENU_BUTTON),
            onClick = onMenu
        ) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = "Abrir menu",
                tint = CalcMotColors.TextPrimary,
                modifier = Modifier.size(30.dp)
            )
        }
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = CalcMotColors.TextPrimary)) {
                    append("Calc")
                }
                withStyle(SpanStyle(color = CalcMotColors.BrandAccent)) {
                    append("Mot")
                }
            },
            modifier = Modifier.padding(start = 22.dp),
            color = CalcMotColors.TextPrimary,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 30.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Black
            )
        )
    }
}

@Composable
private fun PermissionHeroCard(modifier: Modifier = Modifier) {
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
                        CalcMotColors.SurfaceElevated.copy(alpha = 0.76f),
                        CalcMotColors.Surface.copy(alpha = 0.84f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 22.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PermissionHeroIcon()

        Text(
            text = "Permissão necessária",
            modifier = Modifier.padding(top = 18.dp),
            color = CalcMotColors.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 34.sp,
            maxLines = 1
        )
        Text(
            text = "Ative a acessibilidade para o CalcMot\nconseguir identificar ofertas e mostrar\no semáforo de lucro.",
            modifier = Modifier.padding(top = 12.dp),
            color = CalcMotColors.TextSecondary,
            fontSize = 17.sp,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        PermissionShieldDivider(modifier = Modifier.padding(top = 20.dp, bottom = 16.dp))

        Text(
            text = "O CalcMot não aceita corridas sozinho\ne não controla outros apps.",
            color = CalcMotColors.TextSecondary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 23.sp
        )
    }
}

@Composable
private fun PermissionHeroIcon() {
    Box(
        modifier = Modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CalcMotColors.Success.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
        )
        Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = null,
            tint = CalcMotColors.BorderStrong.copy(alpha = 0.92f),
            modifier = Modifier.size(112.dp)
        )
        Icon(
            imageVector = Icons.Outlined.AccessibilityNew,
            contentDescription = null,
            tint = CalcMotColors.Success,
            modifier = Modifier
                .align(Alignment.Center)
                .size(58.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(58.dp)
                .clip(CircleShape)
                .background(CalcMotColors.Surface.copy(alpha = 0.92f))
                .border(3.dp, CalcMotColors.Success.copy(alpha = 0.72f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Speed,
                contentDescription = null,
                tint = CalcMotColors.Warning,
                modifier = Modifier.size(36.dp)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(top = 34.dp)
                .size(34.dp)
                .clip(CircleShape)
                .background(CalcMotColors.Warning),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = CalcMotColors.TextInverse,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun PermissionShieldDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(CalcMotColors.BorderSubtle)
        )
        Icon(
            imageVector = Icons.Outlined.Security,
            contentDescription = null,
            tint = CalcMotColors.Success,
            modifier = Modifier
                .padding(horizontal = 22.dp)
                .size(34.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(CalcMotColors.BorderSubtle)
        )
    }
}

@Composable
private fun PermissionGoalCard(
    goalPerKm: String,
    goalPerHour: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .border(
                BorderStroke(1.dp, CalcMotColors.BorderSubtle),
                RoundedCornerShape(CalcMotShape.Lg)
            )
            .background(
                CalcMotColors.Surface.copy(alpha = 0.72f),
                RoundedCornerShape(CalcMotShape.Lg)
            )
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Flag,
            contentDescription = null,
            tint = CalcMotColors.Success,
            modifier = Modifier.size(46.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Meta atual",
                color = CalcMotColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = goalPerKm, color = CalcMotColors.TextSecondary, fontSize = 17.sp)
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(CalcMotColors.Success)
                )
                Text(text = goalPerHour, color = CalcMotColors.TextSecondary, fontSize = 17.sp)
            }
        }
    }
}

@Composable
private fun PermissionHomeBackground() {
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
private fun HomePermissionRequiredScreenPreview() {
    CalcMotTheme {
        HomePermissionRequiredScreen(
            onMenu = {},
            onActivatePermission = {},
            onHowItWorks = {}
        )
    }
}
