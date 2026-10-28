package br.com.calcmot.account

import br.com.calcmot.entitlement.*
import br.com.calcmot.identity.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AccountViewModelTest {
    @Test fun sourceEpochRejectsSuccessfulLoginAfterConflatedABAEvenWithoutCollector() = runTest {
        val r = Rig(this, true)
        r.controller.login {
            val result = r.identity.acceptGoogleCredential("synthetic-google")
            r.auth.user = IdentitySession("test-b", setOf("google.com")); r.identity.refreshSession()
            r.auth.user = IdentitySession("test-a", setOf("google.com")); r.identity.refreshSession()
            result
        }
        try { assertEquals(0, r.requests); assertNull(r.controller.state.value.request) }
        finally { r.repo.close() }
    }
    private class Auth(var user: IdentitySession? = null) : IdentityAuthPort {
        var nextUid = "test-a"
        var failure: IdentityFailure? = null
        override fun currentSession() = user
        override suspend fun signIn(googleIdToken: String) = IdentitySession(nextUid, setOf("google.com")).also { user = it }
        override suspend fun link(uid: String, googleIdToken: String) = IdentitySession(uid, setOf("google.com")).also { user = it }
        override suspend fun reauthenticate(uid: String, googleIdToken: String) = user!!
        override suspend fun firebaseIdToken(forceRefresh: Boolean): String {
            failure?.let { throw IdentityOperationException(it) }; return "synthetic-token"
        }
        override fun signOut() { user = null }
    }
    private class Rig(scope: TestScope, signed: Boolean = false) {
        val auth = Auth(if (signed) IdentitySession("test-a", setOf("google.com")) else null)
        val identity = IdentityCoordinator(auth)
        var response = EntitlementHttpResponse(503, "")
        var requests = 0
        var signInFailure: IdentityFailure? = null
        var beforeLogin: (suspend () -> Unit)? = null
        var beforeGet: (suspend () -> Unit)? = null
        var claimAvailable = false
        var claimResponse = LegacyRequestView("synthetic-protocol", "UNDER_REVIEW")
        var beforeClaim: (() -> Unit)? = null
        val rows = mutableMapOf<EntitlementOwner, EntitlementCache>()
        val repo = EntitlementRepository("test-project", auth, object : EntitlementRemote {
            override suspend fun get(token: String): EntitlementHttpResponse { requests++; beforeGet?.invoke(); return response }
        }, object : EntitlementStore {
            override suspend fun read(owner: EntitlementOwner) = rows[owner]
            override suspend fun write(cache: EntitlementCache) { rows[cache.owner] = cache }
        }, object : EntitlementClock {
            override fun elapsed() = scope.testScheduler.currentTime + 1000
            override fun boot() = "same-boot"
        }, OfflinePolicy.APPROVED_V1, StandardTestDispatcher(scope.testScheduler))
        val signIn: suspend () -> IdentityResult = {
            beforeLogin?.invoke()
            signInFailure?.let { IdentityResult.Failure(it) } ?: identity.acceptGoogleCredential("synthetic-google")
        }
        val controller = AccountViewModel(identity, { identity.signOut() }, repo, object : LegacyRequests {
            override val available get() = claimAvailable
            override suspend fun submit(): LegacyRequestView { beforeClaim?.invoke(); return claimResponse }
        }, providedScope = scope.backgroundScope)
        fun grant() { response = EntitlementHttpResponse(200, EntitlementRepositoryTest.fixture()) }
    }
    @Test fun constructionAndContinueWithoutLoginNeverStartNetwork() = runTest {
        val r = Rig(this); runCurrent()
        assertFalse(r.controller.state.value.signedIn); assertEquals(0, r.requests)
        r.controller.open(); assertEquals(0, r.requests); assertFalse(r.controller.state.value.access.legacyCore)
        r.repo.close()
    }
    @Test fun loginAnd503DoNotInventEntitlement() = runTest {
        val r = Rig(this); r.controller.login(r.signIn)
        assertTrue(r.controller.state.value.linked)
        assertEquals(EntitlementStatus.BACKEND_UNAVAILABLE, r.controller.state.value.access.status)
        assertFalse(r.controller.state.value.access.legacyCore)
        assertTrue(accountMessage(r.controller.state.value)!!.contains("indisponível")); r.repo.close()
    }
    @Test fun cancelAndCollisionPreserveConfirmedLegacyAndUid() = runTest {
        val r = Rig(this, true); r.grant(); r.controller.open()
        for (failure in listOf(IdentityFailure.CANCELED, IdentityFailure.COLLISION)) {
            r.signInFailure = failure; r.controller.login(r.signIn)
            assertTrue(r.controller.state.value.access.legacyCore)
            assertEquals("test-a", r.identity.session.value!!.uid)
        }
        assertEquals(AccountNotice.COLLISION, r.controller.state.value.notice); r.repo.close()
    }
    @Test fun logoutClearsUiAndDifferentUidCannotInheritCache() = runTest {
        val r = Rig(this, true); r.grant(); r.controller.open(); r.controller.logout()
        assertFalse(r.controller.state.value.signedIn); assertFalse(r.controller.state.value.access.legacyCore)
        r.auth.nextUid = "test-b"; r.response = EntitlementHttpResponse(503, ""); r.controller.login(r.signIn)
        assertFalse(r.controller.state.value.access.legacyCore)
        assertTrue(r.rows.keys.any { it.uid == "test-a" }); r.repo.close()
    }
    @Test fun logoutClearsProfileAsWellAsRightsBeforeAnotherAccountIsShown() = runTest {
        val r = Rig(this, true)
        r.auth.user = IdentitySession("test-a", setOf("google.com"), "Conta A", "account-a@example.test", "https://example.test/photo-a.png")
        r.controller.open()
        assertEquals("account-a@example.test", r.controller.state.value.email)
        assertEquals(AccountProfilePhoto("test-a", "https://example.test/photo-a.png"), r.controller.state.value.profilePhoto)
        r.controller.logout()
        assertNull(r.controller.state.value.email); assertNull(r.controller.state.value.displayName)
        assertNull(r.controller.state.value.profilePhoto)
        r.auth.nextUid = "test-b"; r.controller.login(r.signIn)
        assertNull(r.controller.state.value.email); assertFalse(r.controller.state.value.access.legacyCore)
        assertNull(r.controller.state.value.profilePhoto)
        r.repo.close()
    }
    @Test fun changingOwnerPartitionsEvenAnIdenticalPhotoUrl() = runTest {
        val r = Rig(this, true)
        val url = "https://example.test/shared-photo.png"
        r.auth.user = IdentitySession("test-a", setOf("google.com"), googlePhotoUrl = url)
        r.controller.open()
        assertEquals(AccountProfilePhoto("test-a", url), r.controller.state.value.profilePhoto)
        r.auth.user = IdentitySession("test-b", setOf("google.com"), googlePhotoUrl = url)
        r.repo.reevaluate(); runCurrent()
        assertEquals(AccountProfilePhoto("test-b", url), r.controller.state.value.profilePhoto)
        r.auth.user = IdentitySession("test-c", setOf("google.com"))
        r.repo.reevaluate(); runCurrent()
        assertNull(r.controller.state.value.profilePhoto)
        r.repo.close()
    }
    @Test fun avatarRejectsMissingInsecureAndMalformedUrlsWithoutNetwork() {
        for (url in listOf(null, "", "http://example.test/a.png", "file:///private/a.png",
            "content://profile/a", "https://user:password@example.test/a", "https://example.test:8443/a", "not a URL")) {
            assertNull(accountProfilePhoto("synthetic-owner", url))
        }
        assertNotNull(accountProfilePhoto("synthetic-owner", "https://example.test/a.png"))
        assertNull(accountProfilePhoto("", "https://example.test/a.png"))
    }
    @Test fun sameUidRecoversConfirmedLegacyEvenDuringBackendOutage() = runTest {
        val r = Rig(this, true); r.grant(); r.controller.open(); r.controller.logout()
        r.response = EntitlementHttpResponse(503, ""); r.controller.login(r.signIn)
        assertTrue(r.controller.state.value.access.legacyCore); assertFalse(r.controller.state.value.access.proAllowed)
        r.repo.close()
    }
    @Test fun offline401And503KeepExistingGrantAndCore() = runTest {
        val r = Rig(this, true); r.grant(); r.controller.open()
        for (code in listOf(401, 503)) {
            r.response = EntitlementHttpResponse(code, ""); r.controller.refresh()
            assertTrue(r.controller.state.value.access.legacyCore)
            assertNotNull(accountMessage(r.controller.state.value))
        }
        r.auth.failure = IdentityFailure.OFFLINE; r.controller.refresh()
        assertEquals(EntitlementStatus.OFFLINE, r.controller.state.value.access.status)
        assertTrue(r.controller.state.value.access.currentFreeCorePreserved); r.repo.close()
    }
    @Test fun sameOwnerNotificationKeepsErrorAndGrantWhileRealIdentityChangeClearsBoth() = runTest {
        val r = Rig(this, true); r.grant(); r.controller.open()
        r.response = EntitlementHttpResponse(503, ""); r.controller.refresh()
        r.repo.reevaluate(); runCurrent()
        assertEquals(EntitlementStatus.BACKEND_UNAVAILABLE, r.controller.state.value.access.status)
        assertTrue(r.controller.state.value.access.legacyCore)
        r.auth.user = IdentitySession("test-b", setOf("google.com"))
        r.repo.reevaluate(); runCurrent()
        assertFalse(r.controller.state.value.access.legacyCore)
        assertEquals("test-b", r.controller.state.value.access.owner?.uid)
        r.repo.close()
    }
    @Test fun reviewIsPreparedButDisabledUntilOperationalEnvironmentExists() = runTest {
        val r = Rig(this, true); r.controller.requestRecognition()
        assertEquals(AccountNotice.REVIEW_UNAVAILABLE, r.controller.state.value.notice)
        assertNull(r.controller.state.value.request); assertEquals(0, r.requests); r.repo.close()
    }
    @Test fun claimGrantedDoesNotBecomeEntitlementWithoutS07Confirmation() = runTest {
        val r = Rig(this, true); r.claimAvailable = true
        r.claimResponse = LegacyRequestView("synthetic-granted-protocol", "GRANTED")
        r.controller.requestRecognition()
        assertEquals("GRANTED", r.controller.state.value.request!!.state)
        assertFalse(r.controller.state.value.access.legacyCore); assertFalse(r.controller.state.value.access.proAllowed)
        r.repo.close()
    }
    @Test fun responseFromOldIdentityCannotDisplayProtocolForAnother() = runTest {
        val r = Rig(this, true); r.claimAvailable = true
        r.beforeClaim = { r.auth.user = IdentitySession("test-b", setOf("google.com")); r.identity.refreshSession() }
        r.controller.requestRecognition()
        assertNull(r.controller.state.value.request); r.repo.close()
    }
    @Test fun lateLoginFailureCannotBecomeTheNewOwnersFeedback() = runTest {
        val r = Rig(this, true)
        val started = CompletableDeferred<Unit>(); val finish = CompletableDeferred<Unit>()
        val action = launch { r.controller.login {
            started.complete(Unit); finish.await(); IdentityResult.Failure(IdentityFailure.CANCELED)
        } }
        started.await()
        r.auth.user = IdentitySession("test-b", setOf("synthetic-provider"))
        r.identity.refreshSession(); runCurrent()
        val generation = r.controller.state.value.sessionGeneration
        assertTrue(generation > 0)
        finish.complete(Unit); action.join(); runCurrent()
        assertEquals(AccountNotice.NONE, r.controller.state.value.notice)
        assertEquals(setOf("synthetic-provider"), r.controller.state.value.linkedProviders)
        assertFalse(r.controller.state.value.busy); assertEquals(0, r.requests)
        r.repo.close()
    }
    @Test fun lateExceptionCannotLeakAcrossOwnersOrReuseAnOldGeneration() = runTest {
        val r = Rig(this, true)
        val started = CompletableDeferred<Unit>(); val finish = CompletableDeferred<Unit>()
        val action = launch { r.controller.login {
            started.complete(Unit); finish.await(); throw java.io.IOException("synthetic")
        } }
        started.await()
        r.auth.user = IdentitySession("test-b", setOf("google.com")); r.identity.refreshSession(); runCurrent()
        r.auth.user = IdentitySession("test-a", setOf("google.com")); r.identity.refreshSession(); runCurrent()
        assertTrue(r.controller.state.value.sessionGeneration >= 2)
        finish.complete(Unit); action.join()
        assertEquals(AccountNotice.NONE, r.controller.state.value.notice)
        r.repo.close()
    }
    @Test fun oldSuccessfulLoginDoesNotRefreshOrAttachRightsToTheNewOwner() = runTest {
        val r = Rig(this, true)
        val started = CompletableDeferred<Unit>(); val finish = CompletableDeferred<Unit>()
        val old = r.auth.user!!
        val action = launch { r.controller.login { started.complete(Unit); finish.await(); IdentityResult.Success(old) } }
        started.await()
        r.auth.user = IdentitySession("test-b", setOf("google.com")); r.identity.refreshSession(); runCurrent()
        finish.complete(Unit); action.join()
        assertEquals(AccountNotice.NONE, r.controller.state.value.notice)
        assertFalse(r.controller.state.value.access.legacyCore); assertEquals(0, r.requests)
        r.repo.close()
    }
    @Test fun ownerChangeDuringPostClaimRefreshDropsProtocolAndSuccessNotice() = runTest {
        val r = Rig(this, true); r.claimAvailable = true
        val started = CompletableDeferred<Unit>(); val finish = CompletableDeferred<Unit>()
        r.beforeGet = { started.complete(Unit); finish.await() }
        val action = launch { r.controller.requestRecognition() }
        started.await()
        r.auth.user = IdentitySession("test-b", setOf("google.com")); r.identity.refreshSession(); runCurrent()
        finish.complete(Unit); action.join(); runCurrent()
        assertNull(r.controller.state.value.request)
        assertEquals(AccountNotice.NONE, r.controller.state.value.notice)
        assertFalse(r.controller.state.value.access.legacyCore); r.repo.close()
    }
    @Test fun doubleActionDuringGoogleSelectionDoesNotRaceLogoutOrQuery() = runTest {
        val r = Rig(this); val began = CompletableDeferred<Unit>(); val finish = CompletableDeferred<Unit>()
        r.beforeLogin = { began.complete(Unit); finish.await() }
        val login = launch { r.controller.login(r.signIn) }; began.await()
        r.controller.logout(); r.controller.refresh(); assertEquals(0, r.requests)
        finish.complete(Unit); login.join()
        assertTrue(r.controller.state.value.signedIn); assertFalse(r.controller.state.value.busy); r.repo.close()
    }
    @Test fun claimContractNeverSendsUidVersionOrAutomaticGrant() {
        assertEquals("POST", LegacyClaimContract.method)
        assertEquals("/v1/legacy/claim", LegacyClaimContract.path)
        val root = com.google.gson.JsonParser.parseString(LegacyClaimContract.body).asJsonObject
        assertEquals(setOf("evidenceType", "evidenceHash"), root.keySet())
        assertEquals("USER_DECLARATION", root.get("evidenceType").asString)
        assertTrue(root.get("evidenceHash").isJsonNull)
        assertEquals("synthetic_request_key_01", LegacyClaimContract.headers("synthetic_request_key_01")["Idempotency-Key"])
    }
    @Test fun validatesS01ProtocolAndRejectsClientInventedApprovalFields() {
        val raw = """{"claimId":"synthetic-01","state":"UNDER_REVIEW","openedAt":"2026-10-14T12:00:00Z","resolvedAt":null,"provisionalUntil":null,"revision":0,"serverNow":"2026-10-14T12:01:00Z"}"""
        assertEquals("UNDER_REVIEW", LegacyClaimContract.decodeView(raw).state)
        try { LegacyClaimContract.decodeView(raw.replace("\"revision\":0", "\"revision\":0,\"legacyCore\":true")); fail() }
        catch (_: IllegalArgumentException) { }
    }

    @Test fun auditSuccessfulLoginAfterObservedABAReturnMustBeDiscarded() = runTest {
        val r = Rig(this, true)
        val started = CompletableDeferred<Unit>(); val finish = CompletableDeferred<Unit>()
        val old = r.auth.user!!
        val action = launch { r.controller.login { started.complete(Unit); finish.await(); IdentityResult.Success(old) } }
        started.await()
        r.auth.user = IdentitySession("test-b", setOf("google.com")); r.identity.refreshSession(); runCurrent()
        r.auth.user = IdentitySession("test-a", setOf("google.com")); r.identity.refreshSession(); runCurrent()
        assertTrue(r.controller.state.value.sessionGeneration >= 2)
        finish.complete(Unit); action.join()
        try { assertEquals("Superseded login must not query for a new session generation", 0, r.requests) }
        finally { r.repo.close() }
    }

    @Test fun firstLoginDiagnosticOwnObservedTransitionRemainsValid() = runTest {
        val r = Rig(this); runCurrent()
        val signed = CompletableDeferred<Unit>(); val finish = CompletableDeferred<Unit>()
        val action = launch { r.controller.login {
            val result = r.identity.acceptGoogleCredential("synthetic-google")
            signed.complete(Unit); finish.await(); result
        } }
        signed.await(); runCurrent()
        assertEquals(1L, r.controller.state.value.sessionGeneration)
        finish.complete(Unit); action.join()
        try { assertEquals(1, r.requests); assertTrue(r.controller.state.value.signedIn) }
        finally { r.repo.close() }
    }
    @Test fun firstLoginDiagnosticExternalOwnersInvalidateSuspendedSuccess() = runTest {
        val r = Rig(this); runCurrent()
        val started = CompletableDeferred<Unit>(); val finish = CompletableDeferred<Unit>()
        val action = launch { r.controller.login {
            started.complete(Unit); finish.await(); IdentityResult.Success(IdentitySession("test-a", setOf("google.com")))
        } }
        started.await()
        r.auth.user = IdentitySession("test-b", setOf("google.com")); r.identity.refreshSession(); runCurrent()
        r.auth.user = IdentitySession("test-a", setOf("google.com")); r.identity.refreshSession(); runCurrent()
        assertEquals(2L, r.controller.state.value.sessionGeneration)
        finish.complete(Unit); action.join()
        try { assertEquals("First-login exception must not authorize a superseded result", 0, r.requests) }
        finally { r.repo.close() }
    }
    @Test fun firstLoginDiagnosticExternalLogoutAndReturnInvalidateSuccess() = runTest {
        val r = Rig(this); runCurrent()
        val started = CompletableDeferred<Unit>(); val finish = CompletableDeferred<Unit>()
        val action = launch { r.controller.login {
            started.complete(Unit); finish.await(); IdentityResult.Success(IdentitySession("test-a", setOf("google.com")))
        } }
        started.await()
        r.auth.user = IdentitySession("test-b", setOf("google.com")); r.identity.refreshSession(); runCurrent()
        r.auth.user = null; r.identity.refreshSession(); runCurrent()
        r.auth.user = IdentitySession("test-a", setOf("google.com")); r.identity.refreshSession(); runCurrent()
        assertEquals(3L, r.controller.state.value.sessionGeneration)
        finish.complete(Unit); action.join()
        try { assertEquals(0, r.requests) } finally { r.repo.close() }
    }

    @Test fun firstLoginDiagnosticSynchronizesTheLastTransitionBeforeCheckingGeneration() = runTest {
        val r = Rig(this); runCurrent()
        val started = CompletableDeferred<Unit>(); val finish = CompletableDeferred<Unit>()
        val action = launch { r.controller.login {
            started.complete(Unit); finish.await()
            r.auth.user = IdentitySession("test-a", setOf("google.com"))
            r.identity.refreshSession() // No suspension: collector cannot update generation yet.
            IdentityResult.Success(r.auth.user!!)
        } }
        started.await()
        r.auth.user = IdentitySession("test-b", setOf("google.com")); r.identity.refreshSession(); runCurrent()
        assertEquals(1L, r.controller.state.value.sessionGeneration)
        finish.complete(Unit); action.join()
        try { assertEquals(0, r.requests) } finally { r.repo.close() }
    }
}
