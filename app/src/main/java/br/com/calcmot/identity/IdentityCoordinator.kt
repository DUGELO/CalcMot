package br.com.calcmot.identity

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Called only from explicit account operations, never from offer processing. */
class IdentityCoordinator(private val auth: IdentityAuthPort) {
    private val mutex = Mutex()
    private val mutableSession = MutableStateFlow(auth.currentSession())
    val session: StateFlow<IdentitySession?> = mutableSession.asStateFlow()
    private val mutableRevision = MutableStateFlow(0L)
    val revision: StateFlow<Long> = mutableRevision.asStateFlow()

    /** Advances at the source, even when collectors conflate A -> B -> A. */
    @Synchronized private fun publish(next: IdentitySession?, invalidateOperation: Boolean = false) {
        if (invalidateOperation || mutableSession.value?.uid != next?.uid) mutableRevision.value++
        mutableSession.value = next
    }

    fun checkpoint(): Long { refreshSession(); return revision.value }

    suspend fun acceptGoogleCredential(idToken: String, expectedRevision: Long = checkpoint()): IdentityResult = mutex.withLock {
        if (idToken.isBlank()) return@withLock IdentityResult.Failure(IdentityFailure.CONFIGURATION)
        refreshSession()
        if (revision.value != expectedRevision) return@withLock IdentityResult.Failure(IdentityFailure.COLLISION)
        val before = auth.currentSession()
        try {
            val result = when {
                before == null -> auth.signIn(idToken)
                "google.com" in before.linkedProviders -> auth.reauthenticate(before.uid, idToken)
                else -> auth.link(before.uid, idToken)
            }
            // A linked session must keep ownership; benefits cannot follow a credential collision.
            if (revision.value != expectedRevision ||
                (before != null && before.uid != result.uid) || auth.currentSession()?.uid != result.uid) {
                refreshSession()
                return@withLock IdentityResult.Failure(IdentityFailure.COLLISION)
            }
            publish(result, invalidateOperation = true)
            IdentityResult.Success(result, IdentityLoginStamp(expectedRevision, revision.value))
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (failure: IdentityOperationException) {
            IdentityResult.Failure(failure.reason)
        }
    }

    suspend fun firebaseIdToken(forceRefresh: Boolean = false): String =
        auth.firebaseIdToken(forceRefresh)

    fun refreshSession() { publish(auth.currentSession()) }

    suspend fun signOut() = mutex.withLock {
        auth.signOut()
        publish(auth.currentSession(), invalidateOperation = true)
        if (mutableSession.value != null) throw IdentityOperationException(IdentityFailure.UNAVAILABLE)
    }
}
