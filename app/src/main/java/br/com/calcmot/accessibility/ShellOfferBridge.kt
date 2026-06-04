package br.com.calcmot.accessibility

import br.com.calcmot.model.OfferCandidate
import br.com.calcmot.model.TripData
import java.util.concurrent.atomic.AtomicReference

object ShellOfferBridge {
    private val handler = AtomicReference<((ShellOfferFrame) -> Unit)?>(null)

    fun register(handler: (ShellOfferFrame) -> Unit) {
        this.handler.set(handler)
    }

    fun unregister(handler: (ShellOfferFrame) -> Unit) {
        this.handler.compareAndSet(handler, null)
    }

    fun submit(frame: ShellOfferFrame): SubmitResult {
        val currentHandler = handler.get() ?: return SubmitResult.NOT_CONNECTED
        currentHandler(frame)
        return SubmitResult.HANDLED
    }

    enum class SubmitResult {
        HANDLED,
        NOT_CONNECTED
    }
}

sealed class ShellOfferFrame {
    object InvalidFrame : ShellOfferFrame()
    data class Candidate(val candidate: OfferCandidate) : ShellOfferFrame()
    data class StableTrip(val tripData: TripData) : ShellOfferFrame()
}
