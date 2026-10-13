package br.com.calcmot.identity

/** Private identity only: never an entitlement, legacy proof, or Play payer. */
data class IdentitySession(val uid: String, val linkedProviders: Set<String>) {
    val identityLinked: Boolean get() = linkedProviders.isNotEmpty()
}

enum class IdentityFailure { CANCELED, OFFLINE, SESSION_EXPIRED, COLLISION, CONFIGURATION, UNAVAILABLE }

sealed interface IdentityResult {
    data class Success(val session: IdentitySession) : IdentityResult
    data class Failure(val reason: IdentityFailure) : IdentityResult
}

class IdentityOperationException(val reason: IdentityFailure) : Exception()

/** Existing UIDs are linked or reauthenticated. Never silently switch/merge a session. */
interface IdentityAuthPort {
    fun currentSession(): IdentitySession?
    suspend fun signIn(googleIdToken: String): IdentitySession
    suspend fun link(uid: String, googleIdToken: String): IdentitySession
    suspend fun reauthenticate(uid: String, googleIdToken: String): IdentitySession
    suspend fun firebaseIdToken(forceRefresh: Boolean): String
}
