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

    suspend fun acceptGoogleCredential(idToken: String): IdentityResult = mutex.withLock {
        if (idToken.isBlank()) return@withLock IdentityResult.Failure(IdentityFailure.CONFIGURATION)
        val before = auth.currentSession()
        try {
            val result = when {
                before == null -> auth.signIn(idToken)
                "google.com" in before.linkedProviders -> auth.reauthenticate(before.uid, idToken)
                else -> auth.link(before.uid, idToken)
            }
            // A linked session must keep ownership; benefits cannot follow a credential collision.
            if (before != null && before.uid != result.uid) {
                return@withLock IdentityResult.Failure(IdentityFailure.COLLISION)
            }
            mutableSession.value = result
            IdentityResult.Success(result)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (failure: IdentityOperationException) {
            IdentityResult.Failure(failure.reason)
        }
    }

    suspend fun firebaseIdToken(forceRefresh: Boolean = false): String =
        auth.firebaseIdToken(forceRefresh)

    fun refreshSession() { mutableSession.value = auth.currentSession() }
}
