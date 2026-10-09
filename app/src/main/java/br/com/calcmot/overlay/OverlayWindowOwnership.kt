package br.com.calcmot.overlay

/** Main-thread ownership, independent of offer fingerprint (which may repeat). */
internal class OverlayWindowOwnership {
    private var request: PendingOverlayOperation? = null
    fun attach(request: PendingOverlayOperation) { this.request = request }
    fun owns(request: PendingOverlayOperation): Boolean = this.request === request
    fun clear() { request = null }
}
