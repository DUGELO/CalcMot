package br.com.calcmot.ui.design.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import br.com.calcmot.ui.design.tokens.CalcMotColors
import br.com.calcmot.ui.design.tokens.CalcMotShape
import br.com.calcmot.ui.design.tokens.CalcMotSpacing
import br.com.calcmot.ui.design.tokens.CalcMotTypography

@Composable
fun CalcMotTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            prefix = prefix,
            suffix = suffix,
            isError = isError,
            textStyle = CalcMotTypography.Body,
            shape = RoundedCornerShape(CalcMotShape.Sm),
            keyboardOptions = keyboardOptions,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = CalcMotColors.TextPrimary,
                unfocusedTextColor = CalcMotColors.TextPrimary,
                focusedLabelColor = CalcMotColors.BrandPrimary,
                unfocusedLabelColor = CalcMotColors.TextMuted,
                focusedBorderColor = CalcMotColors.BrandPrimary,
                unfocusedBorderColor = CalcMotColors.BorderSubtle,
                errorBorderColor = CalcMotColors.Danger,
                errorLabelColor = CalcMotColors.Danger
            )
        )
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = CalcMotColors.Danger,
                style = CalcMotTypography.Caption,
                modifier = Modifier.padding(start = CalcMotSpacing.Sm, top = CalcMotSpacing.Xs)
            )
        }
    }
}

@Composable
fun CalcMotNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    prefixText: String? = null,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    CalcMotTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        prefix = prefixText?.let { { Text(it, style = CalcMotTypography.Body) } },
        isError = isError,
        errorMessage = errorMessage,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}
