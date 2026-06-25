package br.com.calcmot.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import br.com.calcmot.AppPermissionState
import br.com.calcmot.R
import br.com.calcmot.ui.design.tokens.CalcMotColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    permissionState: AppPermissionState,
    onPermissionsRefresh: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    val pages = remember { accessibilityOnboardingPages() }
    val lastPage = pages.lastIndex
    val pagerState = rememberPagerState(
        initialPage = if (permissionState.hasAccessibilityService) lastPage else 0,
        pageCount = { pages.size }
    )

    val lifecycleOwner = context as? LifecycleOwner
    DisposableEffect(lifecycleOwner) {
        if (lifecycleOwner == null) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    onPermissionsRefresh()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }

    LaunchedEffect(permissionState.hasAccessibilityService) {
        if (permissionState.hasAccessibilityService && pagerState.currentPage != lastPage) {
            pagerState.animateScrollToPage(lastPage)
        }
    }

    fun openAccessibilitySettings() {
        runCatching {
            openAccessibilitySettings(context)
        }
    }

    if (showPrivacyPolicy) {
        PrivacyPolicyScreen(
            onBack = { showPrivacyPolicy = false },
            onSupport = { uriHandler.openUri("mailto:$CALCMOT_SUPPORT_EMAIL") }
        )
        return
    }

    Scaffold(containerColor = CalcMotColors.HeroBackground) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag(UiTestTags.ONBOARDING_SCREEN)
        ) {
            CalcMotDarkHeroBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HorizontalPager(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag(UiTestTags.ONBOARDING_PAGER),
                    state = pagerState
                ) { page ->
                    OnboardingPage(
                        page = pages[page],
                        permissionState = permissionState
                    )
                }

                OnboardingPageIndicator(
                    currentPage = pagerState.currentPage,
                    pageCount = pages.size
                )

                OnboardingActions(
                    currentPage = pagerState.currentPage,
                    lastPage = lastPage,
                    permissionState = permissionState,
                    onBack = {
                        scope.launch {
                            pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                        }
                    },
                    onNext = {
                        scope.launch {
                            pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(lastPage))
                        }
                    },
                    onOpenAccessibilitySettings = ::openAccessibilitySettings,
                    onPermissionsRefresh = onPermissionsRefresh
                )

                if (pagerState.currentPage > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(
                            modifier = Modifier.testTag(UiTestTags.PRIVACY_LINK),
                            onClick = { showPrivacyPolicy = true }
                        ) {
                            Text("Privacidade")
                        }
                        TextButton(
                            modifier = Modifier.testTag(UiTestTags.SUPPORT_LINK),
                            onClick = { uriHandler.openUri("mailto:$CALCMOT_SUPPORT_EMAIL") }
                        ) {
                            Text("Suporte")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "CalcMot",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Veja se a corrida compensa antes de aceitar.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CalcMotDarkHeroBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(CalcMotColors.HeroBackground)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    CalcMotColors.HeroGlowGreen.copy(alpha = 0.11f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.55f, size.height * 0.48f),
                radius = size.minDimension * 0.72f
            ),
            radius = size.minDimension * 0.72f,
            center = Offset(size.width * 0.55f, size.height * 0.48f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                center = Offset(size.width * 0.48f, size.height * 0.42f),
                radius = size.minDimension * 0.48f
            ),
            radius = size.minDimension * 0.48f,
            center = Offset(size.width * 0.48f, size.height * 0.42f)
        )
        repeat(7) { index ->
            val y = size.height * (0.18f + index * 0.014f)
            drawLine(
                color = CalcMotColors.HeroGlowGreen.copy(alpha = 0.10f - index * 0.008f),
                start = Offset(size.width * 0.30f, y),
                end = Offset(size.width * 0.74f, y + size.height * 0.006f),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        repeat(8) { index ->
            val y = size.height * (0.22f + index * 0.018f)
            drawLine(
                color = CalcMotColors.HeroGlowGreen.copy(alpha = 0.14f - index * 0.010f),
                start = Offset(size.width * 0.76f, y + index * 6f),
                end = Offset(size.width * 1.10f, y + size.height * 0.055f + index * 2f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun OnboardingPage(
    page: OnboardingPageData,
    permissionState: AppPermissionState
) {
    if (page.kind == OnboardingPageKind.INTRO) {
        CalcMotOnboardingIntroPage(testTag = page.testTag)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(page.testTag)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        text = page.stepLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = page.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = page.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (page.steps.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        page.steps.forEachIndexed { index, text ->
                            OnboardingInstruction(index = index + 1, text = text)
                        }
                    }
                }
            }
        }

        if (page.kind == OnboardingPageKind.OPEN_SETTINGS) {
            AccessibilitySettingsCard()
        }

        if (page.kind == OnboardingPageKind.CONFIRM) {
            AccessibilityStatusCard(permissionState)
        }
    }
}

@Composable
private fun OnboardingInstruction(index: Int, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = index.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CalcMotOnboardingIntroPage(testTag: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(18.dp))
        CalcMotHeroLogo()
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Decida corridas com\nmais clareza",
            color = CalcMotColors.TextPrimary,
            fontSize = 29.sp,
            lineHeight = 35.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Veja rapidamente se a corrida\ncompensa antes de aceitar.",
            color = CalcMotColors.TextSecondary,
            fontSize = 17.sp,
            lineHeight = 23.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(36.dp))
        CalcMotBenefitCard(modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun CalcMotHeroLogo() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.calcmot_logo_hero),
            contentDescription = "Logo CalcMot",
            modifier = Modifier
                .size(132.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = CalcMotColors.TextPrimary)) {
                    append("Calc")
                }
                withStyle(SpanStyle(color = CalcMotColors.HeroGlowGreen)) {
                    append("Mot")
                }
            },
            fontSize = 39.sp,
            lineHeight = 41.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Semáforo de lucro para motoristas",
            color = CalcMotColors.TextSecondary,
            fontSize = 16.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CalcMotBenefitCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = CalcMotColors.HeroSurface,
        contentColor = CalcMotColors.TextPrimary,
        border = BorderStroke(1.dp, CalcMotColors.HeroBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp)
        ) {
            CalcMotBenefitRow(
                icon = CalcMotBenefitIcon.GAUGE,
                text = "Calcule R$/km e R$/h"
            )
            CalcMotBenefitDivider()
            CalcMotBenefitRow(
                icon = CalcMotBenefitIcon.TRAFFIC,
                text = "Veja Boa, Média ou Ruim"
            )
            CalcMotBenefitDivider()
            CalcMotBenefitRow(
                icon = CalcMotBenefitIcon.CHECK,
                text = "Tenha resposta rápida\nna hora da oferta"
            )
        }
    }
}

@Composable
private fun CalcMotBenefitDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)
            .height(1.dp)
            .background(CalcMotColors.BorderSubtle)
    )
}

@Composable
private fun CalcMotBenefitRow(
    icon: CalcMotBenefitIcon,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CalcMotBenefitIconView(icon = icon)
        Box(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .size(width = 1.dp, height = 36.dp)
                .background(CalcMotColors.BorderStrong.copy(alpha = 0.45f))
        )
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            color = CalcMotColors.TextPrimary,
            fontSize = 17.sp,
            lineHeight = 23.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CalcMotBenefitIconView(icon: CalcMotBenefitIcon) {
    val imageVector = when (icon) {
        CalcMotBenefitIcon.GAUGE -> Icons.Filled.Speed
        CalcMotBenefitIcon.TRAFFIC -> Icons.Filled.Traffic
        CalcMotBenefitIcon.CHECK -> Icons.Filled.CheckCircle
    }
    Icon(
        modifier = Modifier.size(44.dp),
        imageVector = imageVector,
        contentDescription = null,
        tint = CalcMotColors.HeroGlowGreen
    )
}

@Composable
private fun AccessibilitySettingsCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Na tela do Android",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "No Samsung testado, toque em Aplicativos instalados, depois CalcMot, ligue a chave e confirme em Permitir.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun AccessibilityStatusCard(permissionState: AppPermissionState) {
    val active = permissionState.hasAccessibilityService
    val containerColor = if (active) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (active) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (active) "Permissao ativa" else "Ainda falta ativar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (active) {
                    "Tudo certo. Agora o CalcMot consegue ler as ofertas visiveis nos apps de motorista."
                } else {
                    "Depois de ativar a permissao, volte para ca e toque em Ja ativei."
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun OnboardingPageIndicator(currentPage: Int, pageCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(UiTestTags.ONBOARDING_PAGE_INDICATOR)
            .semantics {
                contentDescription = "Etapa ${currentPage + 1} de $pageCount"
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 5.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) CalcMotColors.HeroGlowGreen
                        else CalcMotColors.SurfaceSoft.copy(alpha = 0.88f)
                    )
            )
        }
    }
}

@Composable
private fun OnboardingActions(
    currentPage: Int,
    lastPage: Int,
    permissionState: AppPermissionState,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onPermissionsRefresh: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (permissionState.hasAccessibilityService) {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag(UiTestTags.FINISH_ONBOARDING_BUTTON),
                onClick = onPermissionsRefresh
            ) {
                Text("Continuar")
            }
            return@Column
        }

        when (currentPage) {
            0 -> {
                CalcMotOnboardingPrimaryButton(
                    text = "Começar",
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(UiTestTags.ONBOARDING_NEXT_BUTTON)
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Você configura tudo em poucos passos.",
                    color = CalcMotColors.TextSecondary,
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            1 -> {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag(UiTestTags.OPEN_ACCESSIBILITY_BUTTON),
                    onClick = onOpenAccessibilitySettings
                ) {
                    Text("Abrir Acessibilidade")
                }
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag(UiTestTags.ONBOARDING_NEXT_BUTTON),
                    onClick = onNext
                ) {
                    Text("Ja abri, continuar")
                }
            }

            lastPage -> {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag(UiTestTags.REFRESH_PERMISSIONS_BUTTON),
                    onClick = onPermissionsRefresh
                ) {
                    Text("Ja ativei")
                }
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag(UiTestTags.OPEN_ACCESSIBILITY_BUTTON),
                    onClick = onOpenAccessibilitySettings
                ) {
                    Text("Abrir Configuracoes")
                }
            }

            else -> {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag(UiTestTags.ONBOARDING_NEXT_BUTTON),
                    onClick = onNext
                ) {
                    Text("Começar")
                }
            }
        }

        if (currentPage > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    modifier = Modifier.testTag(UiTestTags.ONBOARDING_BACK_BUTTON),
                    onClick = onBack
                ) {
                    Text("Voltar")
                }
                Text(
                    text = "Etapa ${currentPage + 1} de ${lastPage + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun CalcMotOnboardingPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF2485FF),
                        CalcMotColors.PrimaryActionBlue,
                        Color(0xFF0F64F6)
                    )
                )
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = Color.White.copy(alpha = 0.18f),
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
            drawRoundRect(
                color = Color(0xFF0A44C4).copy(alpha = 0.52f),
                topLeft = Offset(0f, size.height - 2.dp.toPx()),
                size = Size(size.width, 2.dp.toPx()),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
            )
        }
        Text(
            text = text,
            color = CalcMotColors.TextPrimary,
            fontSize = 22.sp,
            lineHeight = 27.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

private fun accessibilityOnboardingPages(): List<OnboardingPageData> {
    return listOf(
        OnboardingPageData(
            stepLabel = "1",
            title = "Decida corridas com mais clareza",
            body = "Veja rapidamente se a corrida compensa antes de aceitar.",
            steps = listOf(
                "Calcule R$/km e R$/h",
                "Veja Boa, Média ou Ruim",
                "Tenha resposta rápida na hora da oferta"
            ),
            kind = OnboardingPageKind.INTRO,
            testTag = UiTestTags.ACCESSIBILITY_DISCLOSURE
        ),
        OnboardingPageData(
            stepLabel = "2",
            title = "Ative o CalcMot no Android",
            body = "Vamos abrir a tela oficial de Acessibilidade do aparelho. O Android bloqueia molduras de outros apps nessa tela por seguranca, entao siga os nomes abaixo.",
            steps = listOf(
                "Toque em Apps instalados ou Servicos instalados.",
                "Escolha CalcMot na lista.",
                "Ative Usar CalcMot e confirme em Permitir."
            ),
            kind = OnboardingPageKind.OPEN_SETTINGS,
            testTag = UiTestTags.ACCESSIBILITY_PERMISSION_ITEM
        ),
        OnboardingPageData(
            stepLabel = "3",
            title = "Volte para confirmar",
            body = "Ao retornar das Configuracoes, o CalcMot confere automaticamente. Se ainda aparecer pendente, toque em Ja ativei.",
            steps = listOf(
                "Abra Uber ou 99 pelo botao do CalcMot.",
                "Deixe uma oferta aparecer.",
                "O aviso mostra R$/km, R$/h e a classificacao da corrida."
            ),
            kind = OnboardingPageKind.CONFIRM,
            testTag = UiTestTags.ACCESSIBILITY_CONFIRMATION_PAGE
        )
    )
}

private data class OnboardingPageData(
    val stepLabel: String,
    val title: String,
    val body: String,
    val steps: List<String>,
    val kind: OnboardingPageKind,
    val testTag: String
)

private enum class CalcMotBenefitIcon {
    GAUGE,
    TRAFFIC,
    CHECK
}

private enum class OnboardingPageKind {
    INTRO,
    OPEN_SETTINGS,
    CONFIRM
}
