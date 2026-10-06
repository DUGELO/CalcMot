package br.com.calcmot.analytics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
internal fun ClarityScreenTracking(namespace: String, route: String?) {
    val owner = LocalLifecycleOwner.current
    val latestRoute by rememberUpdatedState(route)
    LaunchedEffect(namespace, route) { route?.let { ClarityIntegration.screenView(namespace, it) } }
    DisposableEffect(owner, namespace) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) latestRoute?.let { ClarityIntegration.screenView(namespace, it) }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}
