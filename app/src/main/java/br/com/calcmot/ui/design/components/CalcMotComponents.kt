package br.com.calcmot.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotShape
import br.com.calcmot.ui.design.tokens.CalcMotSpacing
import br.com.calcmot.ui.design.tokens.CalcMotTypography

enum class CalcMotCardVariant {
    DEFAULT,
    HIGHLIGHT,
    DANGER,
    SUCCESS,
    PREMIUM
}

enum class CalcMotButtonVariant {
    PRIMARY,
    SECONDARY,
    GHOST,
    DANGER,
    PREMIUM
}

enum class CalcMotBannerVariant {
    INFO,
    WARNING,
    DANGER,
    SUCCESS,
    PREMIUM
}

@Composable
fun CalcMotScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable (() -> Unit)? = null,
    bottomBar: @Composable (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier.background(CalcMotColors.AppBackground),
        containerColor = CalcMotColors.AppBackground,
        topBar = { topBar?.invoke() },
        bottomBar = { bottomBar?.invoke() },
        content = content
    )
}

@Composable
fun CalcMotTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigation: @Composable (() -> Unit)? = null,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CalcMotSpacing.Md, vertical = CalcMotSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        navigation?.invoke()
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = if (navigation == null) 0.dp else CalcMotSpacing.Sm),
            color = CalcMotColors.TextPrimary,
            style = CalcMotTypography.SectionTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        action?.invoke()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalcMotCard(
    modifier: Modifier = Modifier,
    variant: CalcMotCardVariant = CalcMotCardVariant.DEFAULT,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val colors = CardDefaults.cardColors(containerColor = variant.containerColor)
    val shape = RoundedCornerShape(CalcMotShape.Sm)
    val borderedModifier = modifier
        .fillMaxWidth()
        .border(
            width = 1.dp,
            color = variant.borderColor,
            shape = shape
        )

    if (onClick == null) {
        Card(
            modifier = borderedModifier,
            colors = colors,
            shape = shape,
            content = { content() }
        )
    } else {
        Card(
            modifier = borderedModifier,
            onClick = onClick,
            colors = colors,
            shape = shape,
            content = { content() }
        )
    }
}

@Composable
fun CalcMotButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: CalcMotButtonVariant = CalcMotButtonVariant.PRIMARY
) {
    val shape = RoundedCornerShape(CalcMotShape.Sm)
    when (variant) {
        CalcMotButtonVariant.GHOST -> TextButton(
            modifier = modifier.heightIn(min = 44.dp),
            onClick = onClick,
            enabled = enabled,
            shape = shape
        ) {
            Text(text = text, style = CalcMotTypography.Button)
        }

        CalcMotButtonVariant.SECONDARY -> OutlinedButton(
            modifier = modifier.heightIn(min = 44.dp),
            onClick = onClick,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CalcMotColors.TextPrimary)
        ) {
            Text(text = text, style = CalcMotTypography.Button)
        }

        else -> Button(
            modifier = modifier.heightIn(min = 44.dp),
            onClick = onClick,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = variant.containerColor,
                contentColor = variant.contentColor,
                disabledContainerColor = variant.containerColor.copy(alpha = 0.45f),
                disabledContentColor = variant.contentColor.copy(alpha = 0.7f)
            )
        ) {
            Text(text = text, style = CalcMotTypography.Button)
        }
    }
}

@Composable
fun CalcMotIconButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        modifier = modifier.heightIn(min = 40.dp),
        onClick = onClick,
        shape = RoundedCornerShape(CalcMotShape.Sm)
    ) {
        Text(text = text, style = CalcMotTypography.Button)
    }
}

@Composable
fun CalcMotSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    switchModifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Xs)
        ) {
            Text(text = title, style = CalcMotTypography.CardTitle, color = CalcMotColors.TextPrimary)
            Text(text = description, style = CalcMotTypography.Body, color = CalcMotColors.TextSecondary)
        }
        Switch(
            modifier = switchModifier,
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
fun CalcMotTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        keyboardOptions = keyboardOptions,
        singleLine = singleLine
    )
}

@Composable
fun CalcMotNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isError: Boolean = false,
    supportingText: String? = null
) {
    CalcMotTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        placeholder = placeholder,
        isError = isError,
        supportingText = supportingText,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

@Composable
fun CalcMotSectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Xs)
    ) {
        Text(text = title, style = CalcMotTypography.ScreenTitle, color = CalcMotColors.TextPrimary)
        subtitle?.let {
            Text(text = it, style = CalcMotTypography.ScreenSubtitle, color = CalcMotColors.TextSecondary)
        }
    }
}

@Composable
fun CalcMotInfoBanner(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    variant: CalcMotBannerVariant = CalcMotBannerVariant.INFO
) {
    CalcMotCard(
        modifier = modifier,
        variant = variant.cardVariant
    ) {
        Column(
            modifier = Modifier.padding(CalcMotSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Xs)
        ) {
            Text(text = title, style = CalcMotTypography.CardTitle, color = CalcMotColors.TextPrimary)
            Text(text = body, style = CalcMotTypography.Body, color = CalcMotColors.TextSecondary)
        }
    }
}

@Composable
fun CalcMotStatusBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = color.copy(alpha = 0.16f),
                shape = RoundedCornerShape(CalcMotShape.Pill)
            )
            .padding(horizontal = CalcMotSpacing.Md, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = CalcMotTypography.Caption
        )
    }
}

@Composable
fun CalcMotEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    CalcMotCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(CalcMotSpacing.CardPadding),
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.Xs)
        ) {
            Text(text = title, style = CalcMotTypography.CardTitle, color = CalcMotColors.TextPrimary)
            Text(text = body, style = CalcMotTypography.Body, color = CalcMotColors.TextSecondary)
        }
    }
}

@Composable
fun CalcMotDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(CalcMotColors.BorderSubtle)
            .heightIn(min = 1.dp)
    )
}

@Composable
fun CalcMotBottomActionBar(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CalcMotColors.Surface)
            .padding(CalcMotSpacing.Md),
        horizontalArrangement = Arrangement.spacedBy(CalcMotSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}

private val CalcMotCardVariant.containerColor: Color
    get() = when (this) {
        CalcMotCardVariant.DEFAULT -> CalcMotColors.Surface
        CalcMotCardVariant.HIGHLIGHT -> CalcMotColors.SurfaceElevated
        CalcMotCardVariant.DANGER -> CalcMotColors.Danger.copy(alpha = 0.16f)
        CalcMotCardVariant.SUCCESS -> CalcMotColors.Success.copy(alpha = 0.16f)
        CalcMotCardVariant.PREMIUM -> CalcMotColors.Great.copy(alpha = 0.18f)
    }

private val CalcMotCardVariant.borderColor: Color
    get() = when (this) {
        CalcMotCardVariant.DEFAULT -> CalcMotColors.BorderSubtle
        CalcMotCardVariant.HIGHLIGHT -> CalcMotColors.BorderStrong
        CalcMotCardVariant.DANGER -> CalcMotColors.Danger.copy(alpha = 0.42f)
        CalcMotCardVariant.SUCCESS -> CalcMotColors.Success.copy(alpha = 0.42f)
        CalcMotCardVariant.PREMIUM -> CalcMotColors.Great.copy(alpha = 0.54f)
    }

private val CalcMotButtonVariant.containerColor: Color
    get() = when (this) {
        CalcMotButtonVariant.PRIMARY -> CalcMotColors.BrandPrimary
        CalcMotButtonVariant.SECONDARY -> CalcMotColors.SurfaceElevated
        CalcMotButtonVariant.GHOST -> Color.Transparent
        CalcMotButtonVariant.DANGER -> CalcMotColors.Danger
        CalcMotButtonVariant.PREMIUM -> CalcMotColors.Great
    }

private val CalcMotButtonVariant.contentColor: Color
    get() = when (this) {
        CalcMotButtonVariant.SECONDARY -> CalcMotColors.TextPrimary
        CalcMotButtonVariant.GHOST -> CalcMotColors.BrandPrimary
        else -> CalcMotColors.TextPrimary
    }

private val CalcMotBannerVariant.cardVariant: CalcMotCardVariant
    get() = when (this) {
        CalcMotBannerVariant.INFO -> CalcMotCardVariant.HIGHLIGHT
        CalcMotBannerVariant.WARNING -> CalcMotCardVariant.HIGHLIGHT
        CalcMotBannerVariant.DANGER -> CalcMotCardVariant.DANGER
        CalcMotBannerVariant.SUCCESS -> CalcMotCardVariant.SUCCESS
        CalcMotBannerVariant.PREMIUM -> CalcMotCardVariant.PREMIUM
    }
