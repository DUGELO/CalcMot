package br.com.calcmot.account

import br.com.calcmot.identity.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class LegacyClaimClientTest {
    private class Auth : IdentityAuthPort {
        var user: IdentitySession? = IdentitySession("synthetic-a", setOf("google.com"))
        override fun currentSession() = user
        override suspend fun signIn(googleIdToken: String) = user!!
        override suspend fun link(uid: String, googleIdToken: String) = user!!
        override suspend fun reauthenticate(uid: String, googleIdToken: String) = user!!
        override suspend fun firebaseIdToken(forceRefresh: Boolean) = "synthetic-token"
    }
    private class Keys : LegacyClaimKeys {
        val rows = mutableMapOf<Pair<String,String>, String>()
        override fun existing(project: String, uid: String) = rows[project to uid]
        override fun getOrCreate(project: String, uid: String) = rows.getOrPut(project to uid) { "synthetic-key-${rows.size}" }
    }
    private val body = """{"claimId":"synthetic-case","state":"UNDER_REVIEW","openedAt":"2026-10-14T12:00:00Z","resolvedAt":null,"provisionalUntil":null,"revision":0,"serverNow":"2026-10-14T12:01:00Z"}"""
    @Test fun resumesOnlyAnExistingIntentAndReusesItsKeyAcrossClients() = runTest {
        val auth=Auth(); val identity=IdentityCoordinator(auth); val keys=Keys(); val sent=mutableListOf<String>()
        val transport=LegacyClaimTransport { _, key -> sent.add(key); LegacyClaimResponse(200,body) }
        val client=LegacyClaimClient("project",identity,keys,transport)
        assertNull(client.resume()); assertTrue(sent.isEmpty())
        assertEquals("UNDER_REVIEW", client.submit().state)
        val recreated=LegacyClaimClient("project",identity,keys,transport)
        assertEquals("synthetic-case", recreated.resume()!!.claimId)
        assertEquals(1,sent.distinct().size)
        auth.user=IdentitySession("synthetic-b",setOf("google.com")); identity.refreshSession()
        assertNull(recreated.resume()); recreated.submit()
        assertEquals(2,sent.distinct().size)
        assertNull(LegacyClaimClient("other-project",identity,keys,transport).resume())
    }
    @Test fun connectionFailureKeepsKeyForSafeRetryAndStatusCodesRemainDistinct() = runTest {
        val identity=IdentityCoordinator(Auth()); val keys=Keys(); var status=503
        val client=LegacyClaimClient("project",identity,keys,LegacyClaimTransport {_,_->LegacyClaimResponse(status,body)})
        for((code,notice) in listOf(401 to AccountNotice.UNAUTHORIZED,409 to AccountNotice.REVIEW_CONFLICT,
            503 to AccountNotice.REVIEW_UNAVAILABLE,429 to AccountNotice.REVIEW_UNAVAILABLE)) {
            status=code
            try { client.submit(); fail() } catch(error:LegacyRequestException) { assertEquals(notice,error.notice) }
        }
        assertEquals(1,keys.rows.size); status=200
        assertEquals("synthetic-case",client.resume()!!.claimId)
    }
    @Test fun conflatedABADiscardsOldProtocolAndKnownWorkPreventsNetwork() = runTest {
        val auth=Auth(); val identity=IdentityCoordinator(auth); val keys=Keys()
        val started=CompletableDeferred<Unit>(); val finish=CompletableDeferred<Unit>()
        val client=LegacyClaimClient("project",identity,keys,LegacyClaimTransport {_,_->
            started.complete(Unit); finish.await(); LegacyClaimResponse(200,body)})
        val result=async {
            try { client.submit(); null } catch(error:IdentityOperationException) { error.reason }
        }
        started.await()
        auth.user=IdentitySession("b",setOf("google.com")); identity.refreshSession()
        auth.user=IdentitySession("synthetic-a",setOf("google.com")); identity.refreshSession()
        finish.complete(Unit)
        assertEquals(IdentityFailure.COLLISION,result.await())
        var calls=0
        val blocked=LegacyClaimClient("project",identity,keys,LegacyClaimTransport {_,_->calls++;LegacyClaimResponse(200,body)},{true})
        try { blocked.submit(); fail() } catch(_:LegacyRequestException) {}
        assertEquals(0,calls)
    }
}
