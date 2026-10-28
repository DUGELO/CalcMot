package br.com.calcmot.identity

/** Private identity only: never an entitlement, legacy proof, or Play payer. */
data class IdentitySession(val uid: String, val linkedProviders: Set<String>,
    val displayName: String? = null, val email: String? = null, val googlePhotoUrl: String? = null) {
    val identityLinked: Boolean get() = linkedProviders.isNotEmpty()
}

enum class IdentityFailure { CANCELED, OFFLINE, SESSION_EXPIRED, COLLISION, CONFIGURATION, UNAVAILABLE }

sealed interface IdentityResult {
    data class Success(val session: IdentitySession, val operation: IdentityLoginStamp? = null) : IdentityResult
    data class Failure(val reason: IdentityFailure) : IdentityResult
}

/** Local correlation only; never sent to the backend or used as eligibility evidence. */
data class IdentityLoginStamp(val startedAtRevision: Long, val completedAtRevision: Long)

class IdentityOperationException(val reason: IdentityFailure) : Exception()

/** Existing UIDs are linked or reauthenticated. Never silently switch/merge a session. */
interface IdentityAuthPort {
    fun currentSession(): IdentitySession?
    suspend fun signIn(googleIdToken: String): IdentitySession
    suspend fun link(uid: String, googleIdToken: String): IdentitySession
    suspend fun reauthenticate(uid: String, googleIdToken: String): IdentitySession
    suspend fun firebaseIdToken(forceRefresh: Boolean): String
    fun signOut() { throw IdentityOperationException(IdentityFailure.CONFIGURATION) }
}
