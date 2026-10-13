package br.com.calcmot.identity

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

internal class FirebaseIdentityAuth(private val auth: FirebaseAuth) : IdentityAuthPort {
    override fun currentSession(): IdentitySession? = auth.currentUser?.toSession()

    override suspend fun signIn(googleIdToken: String): IdentitySession = operation {
        // Recheck after asynchronous credential selection, before mutating Firebase state.
        if (auth.currentUser != null) throw IdentityOperationException(IdentityFailure.COLLISION)
        auth.signInWithCredential(GoogleAuthProvider.getCredential(googleIdToken, null)).await()
            .user?.toSession() ?: throw IdentityOperationException(IdentityFailure.UNAVAILABLE)
    }

    override suspend fun link(uid: String, googleIdToken: String): IdentitySession = operation {
        val user = requireOwner(uid)
        user.linkWithCredential(GoogleAuthProvider.getCredential(googleIdToken, null)).await()
            .user?.toSession() ?: throw IdentityOperationException(IdentityFailure.UNAVAILABLE)
    }

    override suspend fun reauthenticate(uid: String, googleIdToken: String): IdentitySession = operation {
        val user = requireOwner(uid)
        user.reauthenticate(GoogleAuthProvider.getCredential(googleIdToken, null)).await()
        requireOwner(uid).toSession()
    }

    override suspend fun firebaseIdToken(forceRefresh: Boolean): String = operation {
        val user = auth.currentUser ?: throw IdentityOperationException(IdentityFailure.SESSION_EXPIRED)
        val token = user.getIdToken(forceRefresh).await().token
        requireOwner(user.uid)
        token?.takeIf { it.isNotBlank() } ?: throw IdentityOperationException(IdentityFailure.SESSION_EXPIRED)
    }

    private fun requireOwner(uid: String): FirebaseUser = auth.currentUser?.takeIf { it.uid == uid }
        ?: throw IdentityOperationException(IdentityFailure.COLLISION)

    private fun FirebaseUser.toSession() = IdentitySession(uid,
        providerData.map { it.providerId }.filter { it != "firebase" }.toSet())

    private suspend fun <T> operation(block: suspend () -> T): T = try { block() }
    catch (cancel: CancellationException) { throw cancel }
    catch (error: IdentityOperationException) { throw error }
    catch (_: FirebaseAuthUserCollisionException) { throw IdentityOperationException(IdentityFailure.COLLISION) }
    catch (_: FirebaseNetworkException) { throw IdentityOperationException(IdentityFailure.OFFLINE) }
    catch (error: FirebaseAuthInvalidUserException) {
        throw IdentityOperationException(if (error.errorCode == "ERROR_USER_MISMATCH")
            IdentityFailure.COLLISION else IdentityFailure.SESSION_EXPIRED)
    }
    catch (error: FirebaseAuthInvalidCredentialsException) {
        throw IdentityOperationException(invalidCredentialFailure(error.errorCode))
    }
    catch (error: FirebaseAuthException) {
        throw IdentityOperationException(if (error.errorCode in setOf("ERROR_USER_MISMATCH",
            "ERROR_CREDENTIAL_ALREADY_IN_USE", "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL"))
            IdentityFailure.COLLISION else IdentityFailure.UNAVAILABLE)
    }
    catch (_: Exception) { throw IdentityOperationException(IdentityFailure.UNAVAILABLE) }
}

/** Firebase reports a Google reauthentication owner mismatch as invalid credentials too. */
internal fun invalidCredentialFailure(errorCode: String): IdentityFailure =
    if (errorCode == "ERROR_USER_MISMATCH") IdentityFailure.COLLISION else IdentityFailure.SESSION_EXPIRED
