package br.com.calcmot.account

import br.com.calcmot.identity.IdentityCoordinator
import br.com.calcmot.identity.IdentityFailure
import br.com.calcmot.identity.IdentityOperationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class LegacyRequestException(val notice: AccountNotice) : Exception()
internal interface LegacyClaimKeys {
    fun existing(project: String, uid: String): String?
    fun getOrCreate(project: String, uid: String): String
}
internal data class LegacyClaimResponse(val status: Int, val body: String)
internal fun interface LegacyClaimTransport {
    suspend fun post(token: String, key: String): LegacyClaimResponse
}

/** Replays the same S01 declaration, never a GET or a client-selected UID/grant. */
internal class LegacyClaimClient(private val project: String, private val identity: IdentityCoordinator,
    private val keys: LegacyClaimKeys, private val transport: LegacyClaimTransport,
    private val workActive: () -> Boolean = { false }) : LegacyRequests {
    override val available = true
    private val mutex = Mutex()
    override suspend fun submit(): LegacyRequestView = mutex.withLock { send(create = true)!! }
    override suspend fun resume(): LegacyRequestView? = mutex.withLock { send(create = false) }
    private suspend fun send(create: Boolean): LegacyRequestView? {
        val revision = identity.checkpoint()
        val session = identity.session.value?.takeIf { it.identityLinked }
            ?: throw IdentityOperationException(IdentityFailure.SESSION_EXPIRED)
        val key = withContext(Dispatchers.IO) {
            if (create) keys.getOrCreate(project, session.uid) else keys.existing(project, session.uid)
        } ?: return null
        if (workActive()) throw LegacyRequestException(AccountNotice.REVIEW_DEFERRED)
        val token = identity.firebaseIdToken()
        if (identity.checkpoint() != revision) throw IdentityOperationException(IdentityFailure.COLLISION)
        if (workActive()) throw LegacyRequestException(AccountNotice.REVIEW_DEFERRED)
        val response = transport.post(token, key)
        if (identity.checkpoint() != revision || identity.session.value?.uid != session.uid)
            throw IdentityOperationException(IdentityFailure.COLLISION)
        return when (response.status) {
            200 -> LegacyClaimContract.decodeView(response.body)
            401 -> throw LegacyRequestException(AccountNotice.UNAUTHORIZED)
            409 -> throw LegacyRequestException(AccountNotice.REVIEW_CONFLICT)
            503, 429 -> throw LegacyRequestException(AccountNotice.REVIEW_UNAVAILABLE)
            else -> throw LegacyRequestException(AccountNotice.UNAVAILABLE)
        }
    }
}
