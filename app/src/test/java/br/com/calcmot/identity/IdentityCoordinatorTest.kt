package br.com.calcmot.identity

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class IdentityCoordinatorTest {
    private class FakeAuth(var current: IdentitySession? = null) : IdentityAuthPort {
        var calls = mutableListOf<String>()
        var failure: IdentityFailure? = null
        var cancel = false
        override fun currentSession() = current
        private fun result(uid: String): IdentitySession {
            if (cancel) throw CancellationException()
            failure?.let { throw IdentityOperationException(it) }
            return IdentitySession(uid, setOf("google.com")).also { current = it }
        }
        override suspend fun signIn(googleIdToken: String): IdentitySession { calls.add("signIn"); return result("recoverable") }
        override suspend fun link(uid: String, googleIdToken: String): IdentitySession { calls.add("link"); return result(uid) }
        override suspend fun reauthenticate(uid: String, googleIdToken: String): IdentitySession { calls.add("reauth"); return result(uid) }
        override suspend fun firebaseIdToken(forceRefresh: Boolean): String {
            calls.add("token:$forceRefresh")
            failure?.let { throw IdentityOperationException(it) }
            return "firebase-id-token"
        }
    }
    @Test fun optionalIdentityDoesNotStartNetworkOrAnonymousLogin() {
        val auth = FakeAuth(); val coordinator = IdentityCoordinator(auth)
        assertNull(coordinator.session.value); assertTrue(auth.calls.isEmpty())
    }
    @Test fun anonymousLinkKeepsUid() = runTest {
        val auth = FakeAuth(IdentitySession("original", emptySet()))
        val coordinator = IdentityCoordinator(auth)
        assertEquals(IdentityResult.Success(IdentitySession("original", setOf("google.com"))), coordinator.acceptGoogleCredential("google"))
        assertEquals(listOf("link"), auth.calls)
    }
    @Test fun existingGoogleSessionReauthenticatesWithoutSwitchingUid() = runTest {
        val auth = FakeAuth(IdentitySession("original", setOf("google.com")))
        assertTrue(IdentityCoordinator(auth).acceptGoogleCredential("google") is IdentityResult.Success)
        assertEquals(listOf("reauth"), auth.calls); assertEquals("original", auth.current?.uid)
    }
    @Test fun reinstallSignsInToRecoverableIdentityWithoutLinkingOtherUid() = runTest {
        val auth = FakeAuth(); val coordinator = IdentityCoordinator(auth)
        coordinator.acceptGoogleCredential("google")
        assertEquals("recoverable", coordinator.session.value?.uid); assertEquals(listOf("signIn"), auth.calls)
    }
    @Test fun collisionOfflineAndExpirationPreserveCurrentIdentity() = runTest {
        for (failure in listOf(IdentityFailure.COLLISION, IdentityFailure.OFFLINE, IdentityFailure.SESSION_EXPIRED)) {
            val before = IdentitySession("original", emptySet()); val auth = FakeAuth(before).also {it.failure=failure}
            val coordinator = IdentityCoordinator(auth)
            assertEquals(IdentityResult.Failure(failure), coordinator.acceptGoogleCredential("google"))
            assertEquals(before, coordinator.session.value); assertEquals(before, auth.current)
        }
    }
    @Test fun emptyCredentialDoesNotReachFirebase() = runTest {
        val auth = FakeAuth()
        assertEquals(IdentityResult.Failure(IdentityFailure.CONFIGURATION), IdentityCoordinator(auth).acceptGoogleCredential(""))
        assertTrue(auth.calls.isEmpty())
    }
    @Test fun expiredTokenIsExplicitAndDoesNotSignOut() = runTest {
        val auth = FakeAuth(IdentitySession("original",setOf("google.com"))).also {it.failure=IdentityFailure.SESSION_EXPIRED}
        val coordinator = IdentityCoordinator(auth)
        try { coordinator.firebaseIdToken(true); fail("Expected expired session") }
        catch (error: IdentityOperationException) { assertEquals(IdentityFailure.SESSION_EXPIRED,error.reason) }
        assertEquals("original", coordinator.session.value?.uid)
    }
    @Test fun coroutineCancellationIsNotConvertedToSuccessOrError() = runTest {
        val auth = FakeAuth().also {it.cancel=true}
        try { IdentityCoordinator(auth).acceptGoogleCredential("google"); fail("Expected cancellation") }
        catch (_: CancellationException) { assertNull(auth.current) }
    }
}
