package br.com.calcmot.identity

import android.app.Activity
import android.content.MutableContextWrapper
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException

/** Optional foreground login entry point. Construct on the account flow, not Application startup. */
class GoogleIdentityClient private constructor(
    private val credentials: CredentialManager,
    val identity: IdentityCoordinator,
    private val webClientId: String
) {
    suspend fun signOut() {
        identity.signOut()
        // SDK logout is authoritative locally even if credential-provider cleanup is unavailable.
        try { credentials.clearCredentialState(ClearCredentialStateRequest()) }
        catch (cancel: CancellationException) { throw cancel }
        catch (_: Exception) { }
    }
    suspend fun signIn(activity: Activity): IdentityResult {
        if (webClientId.isBlank()) return IdentityResult.Failure(IdentityFailure.CONFIGURATION)
        val revision = identity.checkpoint()
        return try {
            val request = GetCredentialRequest.Builder().addCredentialOption(
                GetSignInWithGoogleOption.Builder(webClientId).build()
            ).build()
            val credential = credentials.getCredential(MutableContextWrapper(activity), request).credential
            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                IdentityResult.Failure(IdentityFailure.UNAVAILABLE)
            } else {
                identity.acceptGoogleCredential(GoogleIdTokenCredential.createFrom(credential.data).idToken, revision)
            }
        } catch (_: GetCredentialCancellationException) {
            IdentityResult.Failure(IdentityFailure.CANCELED)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: GetCredentialException) {
            IdentityResult.Failure(IdentityFailure.UNAVAILABLE)
        } catch (_: Exception) {
            IdentityResult.Failure(IdentityFailure.UNAVAILABLE)
        }
    }

    companion object {
        fun create(activity: Activity): GoogleIdentityClient {
            // Generated only by a real OAuth-enabled google-services.json. No placeholder credentials.
            val resource = activity.resources.getIdentifier("default_web_client_id", "string", activity.packageName)
            val clientId = if (resource == 0) "" else activity.getString(resource)
            return GoogleIdentityClient(CredentialManager.create(activity.applicationContext),
                IdentityCoordinator(FirebaseIdentityAuth(FirebaseAuth.getInstance())), clientId)
        }
    }
}
