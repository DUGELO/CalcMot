package br.com.calcmot.identity

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class IdentityCoordinatorTest {
    @Test fun sourceRevisionSurvivesConflatedIdentityChanges() = runTest {
        val auth = FakeAuth(IdentitySession("a", setOf("google.com")))
        val coordinator = IdentityCoordinator(auth)
        val checkpoint = coordinator.checkpoint()
        auth.current = IdentitySession("b", setOf("google.com")); coordinator.refreshSession()
        auth.current = IdentitySession("a", setOf("google.com")); coordinator.refreshSession()
        assertEquals(checkpoint + 2, coordinator.revision.value)
        assertEquals(IdentityResult.Failure(IdentityFailure.COLLISION),
            coordinator.acceptGoogleCredential("old-picker-result", checkpoint))
        assertTrue(auth.calls.isEmpty())
    }
    @Test fun successfulFirstLoginHasExactOperationCorrelationAndLogoutInvalidatesIt() = runTest {
        val auth = FakeAuth(); val coordinator = IdentityCoordinator(auth)
        val started = coordinator.checkpoint()
        val result = coordinator.acceptGoogleCredential("google", started) as IdentityResult.Success
        assertEquals(IdentityLoginStamp(0, 1), result.operation)
        coordinator.signOut()
        coordinator.acceptGoogleCredential("google")
        assertEquals(3L, coordinator.revision.value)
        assertNotEquals(result.operation!!.completedAtRevision, coordinator.revision.value)
    }
    @Test fun refreshOfProfileOrSameOwnerDoesNotInvalidateAnOperation() {
        val auth = FakeAuth(IdentitySession("a", setOf("google.com")))
        val coordinator = IdentityCoordinator(auth)
        auth.current = auth.current!!.copy(displayName = "Test profile")
        coordinator.refreshSession()
        assertEquals(0L, coordinator.revision.value)
    }
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
        override fun signOut() { calls.add("signOut"); current = null }
    }
    @Test fun optionalIdentityDoesNotStartNetworkOrAnonymousLogin() {
        val auth = FakeAuth(); val coordinator = IdentityCoordinator(auth)
        assertNull(coordinator.session.value); assertTrue(auth.calls.isEmpty())
    }
    @Test fun anonymousLinkKeepsUid() = runTest {
        val auth = FakeAuth(IdentitySession("original", emptySet()))
        val coordinator = IdentityCoordinator(auth)
        assertEquals(IdentitySession("original", setOf("google.com")),
            (coordinator.acceptGoogleCredential("google") as IdentityResult.Success).session)
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
        for (failure in listOf(IdentityFailure.CANCELED, IdentityFailure.COLLISION, IdentityFailure.OFFLINE, IdentityFailure.SESSION_EXPIRED)) {
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
    @Test fun explicitLogoutClearsOnlyLocalIdentityThroughExistingAuthPort() = runTest {
        val auth = FakeAuth(IdentitySession("original", setOf("google.com"), "Motorista", "driver@example.test"))
        val coordinator = IdentityCoordinator(auth)
        coordinator.signOut()
        assertNull(coordinator.session.value); assertNull(auth.current)
        assertEquals(listOf("signOut"), auth.calls)
    }
    @Test fun profileMetadataDoesNotChangeLinkingOrUidOwnership() = runTest {
        val auth = FakeAuth(IdentitySession("original", emptySet(), "Motorista", "driver@example.test"))
        val coordinator = IdentityCoordinator(auth)
        assertTrue(coordinator.acceptGoogleCredential("google") is IdentityResult.Success)
        assertEquals("original", coordinator.session.value?.uid)
        assertEquals(listOf("link"), auth.calls)
    }
}
