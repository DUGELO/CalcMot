package br.com.calcmot.securityrecording.application

/** Process leases complement persisted claims; they never replace durable recovery. */
object RecordingResourceCoordinator {
    private var capture = false
    private var operationCount = 0
    private val operations = mutableSetOf<String>()
    private val readers = mutableMapOf<String, Int>()
    private val owners = mutableSetOf<String>()
    // close()/unbind requests are not proof that CameraX closed the output descriptor.
    private val openOutputs = mutableSetOf<String>()
    @Synchronized fun registerOpenOutput(id: String) { check(openOutputs.add(id)) }
    @Synchronized fun releaseOpenOutput(id: String) { openOutputs.remove(id) }
    @Synchronized fun hasOpenOutputs(): Boolean = openOutputs.isNotEmpty()
    @Synchronized fun acquireCapture(): Boolean {
        if (capture || openOutputs.isNotEmpty() || operationCount != 0 || readers.isNotEmpty()) return false
        capture = true
        return true
    }
    @Synchronized fun releaseCapture() { capture = false }
    @Synchronized fun isCapturing(): Boolean = capture
    @Synchronized fun registerOwner(id: String) { owners += id }
    @Synchronized fun releaseOwner(id: String) { owners -= id }
    @Synchronized fun isOwnerAlive(id: String?): Boolean = id != null && id in owners
    @Synchronized fun acquireOperation(sessionId: String, destructive: Boolean): Boolean {
        if (capture || openOutputs.isNotEmpty() || sessionId in operations || (destructive && sessionId in readers)) return false
        operations += sessionId
        operationCount++
        return true
    }
    @Synchronized fun hasOperations(): Boolean = operationCount != 0
    @Synchronized fun releaseOperation(sessionId: String) { check(operations.remove(sessionId)); operationCount-- }
    @Synchronized fun acquireReader(sessionId: String): Boolean {
        if (capture || openOutputs.isNotEmpty() || operationCount != 0) return false
        readers[sessionId] = (readers[sessionId] ?: 0) + 1
        return true
    }
    @Synchronized fun releaseReader(sessionId: String) {
        val count = readers[sessionId] ?: return
        if (count == 1) readers.remove(sessionId) else readers[sessionId] = count - 1
    }
}
