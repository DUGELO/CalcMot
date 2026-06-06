package br.com.calcmot.ui.design.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import br.com.calcmot.ui.design.tokens.CalcMotColors

@Composable
fun CalcMotScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        containerColor = CalcMotColors.AppBackground,
        contentColor = CalcMotColors.TextPrimary,
        content = content
    )
}
