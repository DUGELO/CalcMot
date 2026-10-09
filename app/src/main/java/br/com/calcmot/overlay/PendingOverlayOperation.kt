package br.com.calcmot.overlay

import java.util.concurrent.atomic.AtomicBoolean

/** A queued operation has a deadline and may be revoked before it enters WindowManager. */
internal class PendingOverlayOperation(private val deadlineElapsed: Long) {
    private val pending = AtomicBoolean(true)
    private val cancelled = AtomicBoolean(false)
    fun tryStart(nowElapsed: Long, contextCurrent: Boolean): Boolean =
        isValid(nowElapsed, contextCurrent) && pending.compareAndSet(true, false)
    fun isValid(nowElapsed: Long, contextCurrent: Boolean): Boolean =
        nowElapsed < deadlineElapsed && contextCurrent && !cancelled.get()
    fun cancel() { cancelled.set(true); pending.set(false) }
}
