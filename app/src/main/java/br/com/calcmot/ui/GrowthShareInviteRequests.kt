package br.com.calcmot.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/**
 * A one-shot signal from a user-tapped sharing notification.
 * Never replays an already consumed action when the Home is recomposed or recreated.
 * Multiple taps before the Home is visible are intentionally coalesced into one invite.
 */
internal object GrowthShareInviteRequests {
    val pending: MutableState<Boolean> = mutableStateOf(false)

    fun request() {
        pending.value = true
    }

    fun consumePending(): Boolean {
        if (!pending.value) return false
        pending.value = false
        return true
    }
}
