package br.com.calcmot.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import br.com.calcmot.ui.design.components.CalcMotScaffold
import br.com.calcmot.ui.design.components.CalcMotTopBar
import br.com.calcmot.ui.design.tokens.CalcMotSpacing

@Composable
internal fun FinancialFeatureScreen(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    imePadding: Boolean = false,
    content: LazyListScope.() -> Unit
) {
    CalcMotScaffold(
        modifier = modifier
            .fillMaxSize()
            .then(if (imePadding) Modifier.imePadding() else Modifier),
        topBar = {
            CalcMotTopBar(
                title = title,
                modifier = Modifier.statusBarsPadding(),
                onBack = onBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(
                start = CalcMotSpacing.ScreenHorizontal,
                top = CalcMotSpacing.Sm,
                end = CalcMotSpacing.ScreenHorizontal,
                bottom = CalcMotSpacing.Xl
            ),
            verticalArrangement = Arrangement.spacedBy(CalcMotSpacing.CardGap),
            content = content
        )
    }
}
