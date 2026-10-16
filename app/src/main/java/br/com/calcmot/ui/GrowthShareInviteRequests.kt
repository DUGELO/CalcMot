package br.com.calcmot.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf

/** Consumed only while the home route is visible; a notification never starts a share by itself. */
internal object GrowthShareInviteRequests {
    val requests: MutableState<Int> = mutableIntStateOf(0)
    fun request() {
        requests.value += 1
    }
}
