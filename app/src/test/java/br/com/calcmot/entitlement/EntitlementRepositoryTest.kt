package br.com.calcmot.entitlement

import br.com.calcmot.identity.*
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
internal class EntitlementRepositoryTest {
    private class Auth : IdentityAuthPort {
        var user: IdentitySession? = IdentitySession("synthetic-a", setOf("google.com"))
        var error: IdentityFailure? = null
        var calls = 0
        override fun currentSession() = user
        override suspend fun firebaseIdToken(forceRefresh: Boolean): String {
            calls++; error?.let { throw IdentityOperationException(it) }; return "synthetic-token-never-real"
        }
        override suspend fun signIn(googleIdToken: String) = error("unused")
        override suspend fun link(uid: String, googleIdToken: String) = error("unused")
        override suspend fun reauthenticate(uid: String, googleIdToken: String) = error("unused")
    }
    private class Clock(private val schedulerTime: () -> Long) : EntitlementClock {
        var time = 1000L; var bootId: String? = "boot-1"
        override fun elapsed() = time + schedulerTime()
        override fun boot() = bootId
    }
    private class Store : EntitlementStore {
        val rows = mutableMapOf<EntitlementOwner, EntitlementCache>()
        var reads = 0; var writes = 0; var failWrite = false; var failRead = false
        override suspend fun read(owner: EntitlementOwner): EntitlementCache? {
            reads++; if (failRead) throw IllegalArgumentException("synthetic invalid MAC"); return rows[owner]
        }
        override suspend fun write(cache: EntitlementCache) {
            if (failWrite) throw IOException("synthetic disk failure")
            writes++; rows[cache.owner] = cache
        }
    }
    private class Remote : EntitlementRemote {
        var response = EntitlementHttpResponse(200, fixture()); var calls = 0
        var operation: (suspend () -> Unit)? = null
        override suspend fun get(token: String): EntitlementHttpResponse { calls++; operation?.invoke(); return response }
    }
    private class Rig(val dispatcher: kotlinx.coroutines.CoroutineDispatcher, policy: OfflinePolicy? = null,
        allowPromoProof: Boolean = false) {
        val auth = Auth(); val clock = Clock { (dispatcher as? TestDispatcher)?.scheduler?.currentTime ?: 0L }
        val store = Store(); val remote = Remote()
        var promoProof: ((EntitlementOwner, PromoSource) -> PromoVerification?)? = if (allowPromoProof)
            { owner, source -> PromoVerification(owner, source, EntitlementCodec.decode(remote.response.body).serverNow) } else null
        val repo = EntitlementRepository("calcmot-test", auth, remote, store, clock, policy, dispatcher,
            PromoVerifier { _, owner, source -> promoProof?.invoke(owner, source) })
        fun restarted(project: String = "calcmot-test", policy: OfflinePolicy? = null) =
            EntitlementRepository(project, auth, remote, store, clock, policy, dispatcher)
    }

    @Test fun durableLegacySurvivesOffline503AndExpiredSessionWithoutBecomingPro() = runTest {
        val r = Rig(StandardTestDispatcher(testScheduler)); r.repo.refresh()
        assertTrue(r.repo.state.value.legacyCore); assertFalse(r.repo.state.value.proAllowed)
        for ((code, status) in listOf(401 to EntitlementStatus.UNAUTHORIZED, 503 to EntitlementStatus.BACKEND_UNAVAILABLE)) {
            r.remote.response = EntitlementHttpResponse(code, "")
            r.repo.refresh(); assertEquals(status, r.repo.state.value.status)
            assertTrue(r.repo.state.value.legacyCore); assertTrue(r.repo.state.value.currentFreeCorePreserved)
        }
        r.auth.error = IdentityFailure.OFFLINE; r.repo.refresh()
        assertEquals(EntitlementStatus.OFFLINE, r.repo.state.value.status)
        r.auth.error = IdentityFailure.SESSION_EXPIRED; r.repo.refresh()
        assertEquals(EntitlementStatus.SESSION_EXPIRED, r.repo.state.value.status)
        assertTrue(r.repo.state.value.legacyCore)
    }

    @Test fun emptyOrNewInstallNeverInventsLegacyOrPremium() = runTest {
        val r = Rig(StandardTestDispatcher(testScheduler)); r.repo.restore()
        assertFalse(r.repo.state.value.legacyCore)
        r.remote.response = EntitlementHttpResponse(503, ""); r.repo.refresh()
        assertFalse(r.repo.state.value.proAllowed); assertTrue(r.repo.state.value.currentFreeCorePreserved)
        r.remote.response = EntitlementHttpResponse(200, fixture(legacy = false)); r.repo.refresh()
        assertFalse(r.repo.state.value.legacyCore); assertEquals(EntitlementStatus.READY, r.repo.state.value.status)
    }

    @Test fun accountAndProjectIsolationDoesNotReuseOrOverwriteOtherCache() = runTest {
        val r = Rig(StandardTestDispatcher(testScheduler)); r.repo.refresh()
        val a = r.auth.user
        r.auth.user = IdentitySession("synthetic-b", setOf("google.com")); r.repo.identityChanged()
        assertFalse(r.repo.state.value.legacyCore)
        r.repo.restore(); assertFalse(r.repo.state.value.legacyCore)
        r.auth.user = a; r.repo.identityChanged(); r.repo.restore()
        assertTrue(r.repo.state.value.legacyCore)
        val otherProject = r.restarted("other-project"); otherProject.restore()
        assertFalse(otherProject.state.value.legacyCore)
    }

    @Test fun inFlightResponseCannotCrossIdentityOrAbaSessionEpoch() = runTest {
        val r = Rig(StandardTestDispatcher(testScheduler))
        val began = CompletableDeferred<Unit>(); val finish = CompletableDeferred<Unit>()
        r.remote.operation = { began.complete(Unit); finish.await() }
        val work = launch { r.repo.refresh() }; began.await()
        val a = r.auth.user
        r.auth.user = IdentitySession("synthetic-b", emptySet()); r.repo.identityChanged()
        r.auth.user = a; r.repo.identityChanged()
        finish.complete(Unit); work.join()
        assertEquals(0, r.store.writes); assertFalse(r.repo.state.value.legacyCore)
    }

    @Test fun identitySwitchWithoutListenerIsCheckedAfterNetwork() = runTest {
        val r = Rig(StandardTestDispatcher(testScheduler))
        r.remote.operation = { r.auth.user = IdentitySession("synthetic-b", emptySet()) }
        r.repo.refresh()
        assertEquals(0, r.store.writes); assertFalse(r.repo.state.value.legacyCore)
    }

    @Test fun rebootAndClockAnchorLossPreserveOnlyPermanentLegacy() = runTest {
        val r = Rig(StandardTestDispatcher(testScheduler), testPolicy)
        r.remote.response = EntitlementHttpResponse(200, fixture(play = true)); r.repo.refresh()
        assertTrue(r.repo.state.value.proAllowed)
        r.clock.bootId = "boot-2"; r.clock.time = 400000L
        r.repo.reevaluate()
        assertEquals(TemporalStatus.REVALIDATION_REQUIRED, r.repo.state.value.play)
        assertFalse(r.repo.state.value.proAllowed); assertTrue(r.repo.state.value.legacyCore)
        r.clock.bootId = null; r.repo.reevaluate(); assertFalse(r.repo.state.value.proAllowed)
    }

    @Test fun elapsedRegressionCannotExtendSubscription() = runTest {
        val r = Rig(StandardTestDispatcher(testScheduler), testPolicy)
        r.remote.response = EntitlementHttpResponse(200, fixture(play = true)); r.repo.refresh()
        r.clock.time = 1; r.repo.reevaluate()
        assertFalse(r.repo.state.value.proAllowed); assertTrue(r.repo.state.value.legacyCore)
    }

    @Test fun serverTimeAndMonotonicTimeIgnoreCivilClockEntirely() = runTest {
        val r = Rig(StandardTestDispatcher(testScheduler), testPolicy)
        r.remote.response = EntitlementHttpResponse(200, fixture(play = true)); r.repo.refresh()
        // No wall-clock API in repository/clock port: changing device date cannot grant time.
        r.clock.time += 1000; r.repo.reevaluate(); assertTrue(r.repo.state.value.proAllowed)
        r.clock.time += 60000; r.repo.reevaluate()
        assertEquals(TemporalStatus.EXPIRED, r.repo.state.value.play)
        assertFalse(r.repo.state.value.proAllowed); assertTrue(r.repo.state.value.legacyCore)
    }

    @Test fun noCommercialPolicyNeverEnablesTemporaryPremium() = runTest {
        val r = Rig(StandardTestDispatcher(testScheduler))
        r.remote.response = EntitlementHttpResponse(200, fixture(play = true)); r.repo.refresh()
        assertEquals(TemporalStatus.POLICY_REQUIRED, r.repo.state.value.play)
        assertFalse(r.repo.state.value.proAllowed)
        r.clock.time += 60000; r.repo.reevaluate()
        assertEquals(TemporalStatus.EXPIRED, r.repo.state.value.play)
        assertTrue(r.repo.state.value.currentFreeCorePreserved)
    }

    @Test fun finiteTestPolicyExpiresBeforePurchasedPeriodWithoutRenewingIt() = runTest {
        val r = Rig(StandardTestDispatcher(testScheduler), testPolicy.copy(paidMaxAgeMs = 1000))
        r.remote.response = EntitlementHttpResponse(200, fixture(play = true)); r.repo.refresh()
        assertTrue(r.repo.state.value.proAllowed)
        r.clock.time += 1000; r.repo.reevaluate()
        assertEquals(TemporalStatus.REVALIDATION_REQUIRED, r.repo.state.value.play)
        assertFalse(r.repo.state.value.proAllowed)
    }

    @Test fun sameRevisionLaterServerNowDoesNotResurrectExpiredPaidOrTrial() = runTest {
        for (phase in listOf("TRIAL", "PAID_PERIOD")) {
            val r = Rig(StandardTestDispatcher(testScheduler), testPolicy)
            r.remote.response = EntitlementHttpResponse(200, fixture(play = true, phase = phase)); r.repo.refresh()
            val first = r.repo.state.value.revision
            r.remote.response = EntitlementHttpResponse(200, fixture(play = true, phase = phase, later = true))
            r.repo.refresh()
            assertEquals(first, r.repo.state.value.revision)
            assertEquals(EntitlementStatus.READY, r.repo.state.value.status)
            assertFalse(r.repo.state.value.proAllowed); assertTrue(r.repo.state.value.legacyCore)
        }
    }

    @Test fun freshProjectionCannotRefreshOldPlayCheckedAt() = runTest {
        val r = Rig(StandardTestDispatcher(testScheduler), testPolicy.copy(paidMaxAgeMs = 1000))
        val raw = JsonParser.parseString(fixture(play = true)).asJsonObject
        raw.addProperty("serverNow", "2026-10-14T12:00:02Z")
        r.remote.response = EntitlementHttpResponse(200, raw.toString()); r.repo.refresh()
        assertEquals(TemporalStatus.REVALIDATION_REQUIRED, r.repo.state.value.play)
        assertFalse(r.repo.state.value.proAllowed)
    }

    @Test fun futurePromoAndReviewDoNotCreateTrialOrProFromLegacy() = runTest {
        val r = Rig(StandardTestDispatcher(testScheduler), testPolicy)
        val raw = JsonParser.parseString(fixture(legacy = false)).asJsonObject
        raw.getAsJsonObject("legacy").addProperty("state", "UNDER_REVIEW")
        raw.getAsJsonObject("review").addProperty("claimId", "synthetic-claim")
        raw.getAsJsonObject("review").addProperty("provisionalUntil", "2026-10-14T12:01:00Z")
        r.remote.response = EntitlementHttpResponse(200, raw.toString()); r.repo.refresh()
        assertEquals(TemporalStatus.DISABLED, r.repo.state.value.review); assertFalse(r.repo.state.value.proAllowed)
        raw.getAsJsonObject("promo").apply {
            addProperty("state", "ACTIVE"); addProperty("startAt", "2026-10-14T12:00:30Z")
            addProperty("validUntil", "2026-10-14T12:01:00Z")
        }
        r.remote.response = EntitlementHttpResponse(200, raw.toString()); r.repo.refresh()
        // A changed source without changed revision is inconsistent.
        assertEquals(EntitlementStatus.INCONSISTENT, r.repo.state.value.status)
        raw.addProperty("revision", 8); r.remote.response = EntitlementHttpResponse(200, raw.toString()); r.repo.refresh()
        assertFalse(r.repo.state.value.proAllowed)
        r.clock.time += 60000; r.repo.reevaluate(); assertEquals(TemporalStatus.DISABLED, r.repo.state.value.review)
    }

    @Test fun malformedAndRollbackSnapshotsNeverRevokeExistingLegacy() = runTest {
        val r = Rig(StandardTestDispatcher(testScheduler)); r.repo.refresh()
        for (raw in listOf("{}", fixture(legacy = false), fixture(revision = 6),
            fixture().replace("\"legacyCore\":true", "\"legacyCore\":\"true\""))) {
            r.remote.response = EntitlementHttpResponse(200, raw); r.repo.refresh()
            assertEquals(EntitlementStatus.INCONSISTENT, r.repo.state.value.status)
            assertTrue(r.repo.state.value.legacyCore)
        }
        assertEquals(1, r.store.writes)
    }

    @Test fun restartRestoresGrantAndRefreshCannotBypassDiskRevision() = runTest {
        val r = Rig(StandardTestDispatcher(testScheduler)); r.repo.refresh()
        val restart = r.restarted(); restart.restore(); assertTrue(restart.state.value.legacyCore)
        r.remote.response = EntitlementHttpResponse(200, fixture(revision = 1))
        val refreshFirst = r.restarted(); refreshFirst.refresh()
        assertTrue(refreshFirst.state.value.legacyCore)
        assertEquals(EntitlementStatus.INCONSISTENT, refreshFirst.state.value.status)
    }

    @Test fun reinstallEmptyCacheRecoversOnlyFromServerSameIdentity() = runTest {
        val r = Rig(StandardTestDispatcher(testScheduler)); r.repo.refresh()
        // Model loss of local cache, not deletion of real device/application data.
        r.store.rows.clear()
        val reinstall = r.restarted(); reinstall.restore(); assertFalse(reinstall.state.value.legacyCore)
        reinstall.refresh(); assertTrue(reinstall.state.value.legacyCore)
    }

    @Test fun offerReadAndReevaluationMakeZeroDiskNetworkOrAuthRequests() = runTest {
        val r = Rig(StandardTestDispatcher(testScheduler)); r.repo.refresh()
        val before = listOf(r.auth.calls, r.store.reads, r.store.writes, r.remote.calls)
        repeat(1000) { r.repo.reevaluate(); assertTrue(r.repo.state.value.currentFreeCorePreserved) }
        assertEquals(before, listOf(r.auth.calls, r.store.reads, r.store.writes, r.remote.calls))
    }

    @Test fun interruptedConnectionAndCancellationPreserveGrant() = runTest {
        val r = Rig(StandardTestDispatcher(testScheduler)); r.repo.refresh()
        r.remote.operation = { throw IOException("synthetic interruption") }; r.repo.refresh()
        assertEquals(EntitlementStatus.OFFLINE, r.repo.state.value.status); assertTrue(r.repo.state.value.legacyCore)
        r.remote.operation = { throw CancellationException() }
        try { r.repo.refresh(); fail("cancellation swallowed") } catch (_: CancellationException) { }
        assertTrue(r.repo.state.value.legacyCore); assertEquals(1, r.store.writes)
    }

    @Test fun failedPersistenceLeavesLastConfirmedCacheRecoverable() = runTest {
        val r = Rig(StandardTestDispatcher(testScheduler)); r.repo.refresh()
        r.store.failWrite = true
        r.remote.response = EntitlementHttpResponse(200, fixture(revision = 8)); r.repo.refresh()
        assertEquals(EntitlementStatus.STORAGE_UNAVAILABLE, r.repo.state.value.status)
        assertEquals(7L, r.repo.state.value.revision)
        val restart = r.restarted(); restart.restore(); assertTrue(restart.state.value.legacyCore)
    }

    @Test fun noSessionDoesNotStartAuthOrRemote() = runTest {
        val r = Rig(StandardTestDispatcher(testScheduler)); r.auth.user = null
        r.repo.refresh(); assertEquals(0, r.remote.calls); assertEquals(0, r.auth.calls)
        assertEquals(EntitlementStatus.SESSION_EXPIRED, r.repo.state.value.status)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test fun stateFlowExpiresAtBoundaryWithoutAnotherRequestOrConsumerPoll() = runTest {
        val r = Rig(StandardTestDispatcher(testScheduler), testPolicy)
        r.remote.response = EntitlementHttpResponse(200, fixture(play = true)); r.repo.refresh()
        assertTrue(r.repo.state.value.proAllowed)
        advanceTimeBy(60000); runCurrent()
        assertFalse(r.repo.state.value.proAllowed)
        assertEquals(TemporalStatus.EXPIRED, r.repo.state.value.play)
        assertEquals(1, r.remote.calls); assertTrue(r.repo.state.value.legacyCore)
        r.repo.close()
    }

    @Test fun transientMissingFileDoesNotEraseLegacyAlreadyInMemory() = runTest {
        val r = Rig(StandardTestDispatcher(testScheduler)); r.repo.refresh()
        r.store.rows.clear(); r.repo.restore()
        assertTrue(r.repo.state.value.legacyCore)
        assertEquals(EntitlementStatus.STORAGE_UNAVAILABLE, r.repo.state.value.status)
    }

    @Test fun damagedCacheOrLostKeyCanRecoverOnlyByValidAuthenticatedServerProjection() = runTest {
        val r = Rig(StandardTestDispatcher(testScheduler)); r.store.failRead = true
        r.repo.restore(); assertEquals(EntitlementStatus.STORAGE_UNAVAILABLE, r.repo.state.value.status)
        r.remote.response = EntitlementHttpResponse(503, ""); r.repo.refresh()
        assertFalse(r.repo.state.value.legacyCore); assertTrue(r.repo.state.value.currentFreeCorePreserved)
        assertEquals(0, r.store.writes)
        r.remote.response = EntitlementHttpResponse(200, fixture()); r.repo.refresh()
        assertTrue(r.repo.state.value.legacyCore); assertEquals(1, r.store.writes)
    }

    @Test fun policyChangeAfterRestartRequiresRevalidationButNotForLegacy() = runTest {
        val r = Rig(StandardTestDispatcher(testScheduler), testPolicy)
        r.remote.response = EntitlementHttpResponse(200, fixture(play = true)); r.repo.refresh()
        val restarted = r.restarted(policy = testPolicy.copy(version = "SYNTHETIC-NEW-POLICY"))
        restarted.restore()
        assertTrue(restarted.state.value.legacyCore); assertFalse(restarted.state.value.proAllowed)
        assertEquals(TemporalStatus.REVALIDATION_REQUIRED, restarted.state.value.play)
    }

    @Test fun combinedSourcesRemainIndependentAndNoSourceRestartsTrial() = runTest {
        val r = Rig(StandardTestDispatcher(testScheduler), testPolicy, allowPromoProof = true)
        val raw = JsonParser.parseString(fixture(play = true)).asJsonObject
        raw.getAsJsonObject("promo").apply {
            addProperty("state", "ACTIVE"); addProperty("startAt", "2026-10-14T11:00:00Z")
            addProperty("validUntil", "2026-10-14T12:02:00Z")
        }
        r.remote.response = EntitlementHttpResponse(200, raw.toString()); r.repo.refresh()
        r.clock.time += 60000; r.repo.reevaluate()
        assertEquals(TemporalStatus.EXPIRED, r.repo.state.value.play)
        assertEquals(TemporalStatus.VALID, r.repo.state.value.promo); assertTrue(r.repo.state.value.proAllowed)
        r.clock.time += 60000; r.repo.reevaluate()
        assertFalse(r.repo.state.value.proAllowed); assertTrue(r.repo.state.value.legacyCore)
    }

    @Test fun playStatesDistinguishPendingFromExpiredAndHonorCanceledAndGrace() = runTest {
        for (state in listOf("ACTIVE", "CANCELED_WITH_ACCESS", "GRACE", "PENDING", "ON_HOLD", "PAUSED", "REVOKED", "EXPIRED", "UNKNOWN")) {
            val r = Rig(StandardTestDispatcher(testScheduler), testPolicy)
            val raw = JsonParser.parseString(fixture(play = true)).asJsonObject
            raw.getAsJsonObject("play").addProperty("state", state)
            raw.addProperty("proAllowed", state in EntitlementCodec.activePlay)
            r.remote.response = EntitlementHttpResponse(200, raw.toString()); r.repo.refresh()
            assertEquals(state, r.repo.state.value.playState)
            assertEquals(state in EntitlementCodec.activePlay, r.repo.state.value.proAllowed)
            assertTrue(r.repo.state.value.legacyCore)
        }
    }

    @Test fun proposedPaid72AndTrial24HoursAreFiniteAndDoNotChangeRuntimePolicy() = runTest {
        // User-proposed commercial values, TEST ONLY. The production adapter still has null policy.
        for ((phase, hours) in listOf("PAID_PERIOD" to 72L, "TRIAL" to 24L)) {
            val limit = hours * 60 * 60 * 1000
            val r = Rig(StandardTestDispatcher(testScheduler), OfflinePolicy.APPROVED_V1)
            val raw = JsonParser.parseString(fixture(play = true, phase = phase)).asJsonObject
            raw.getAsJsonObject("play").addProperty("validUntil", "2026-10-20T12:00:00Z")
            r.remote.response = EntitlementHttpResponse(200, raw.toString()); r.repo.refresh()
            r.clock.time += limit - 1; r.repo.reevaluate(); assertTrue(r.repo.state.value.proAllowed)
            r.clock.time++; r.repo.reevaluate(); assertFalse(r.repo.state.value.proAllowed)
            assertEquals(TemporalStatus.REVALIDATION_REQUIRED, r.repo.state.value.play)
            assertTrue(r.repo.state.value.legacyCore); assertEquals(1, r.remote.calls)
            r.repo.close()
        }
    }

    @Test fun proposedPromo24HoursEndsAtSourceExpiryIfEarlier() = runTest {
        val r = Rig(StandardTestDispatcher(testScheduler), OfflinePolicy.APPROVED_V1, allowPromoProof = true)
        val raw = JsonParser.parseString(fixture()).asJsonObject
        raw.getAsJsonObject("promo").apply {
            addProperty("state", "ACTIVE"); addProperty("startAt", "2026-10-14T12:00:00Z")
            addProperty("validUntil", "2026-10-14T13:00:00Z")
        }
        raw.addProperty("proAllowed", true)
        r.remote.response = EntitlementHttpResponse(200, raw.toString()); r.repo.refresh()
        r.clock.time += 3600000; r.repo.reevaluate()
        assertEquals(TemporalStatus.EXPIRED, r.repo.state.value.promo)
        assertFalse(r.repo.state.value.proAllowed); assertTrue(r.repo.state.value.legacyCore)
        r.repo.close()
        val wholeWindow = Rig(StandardTestDispatcher(testScheduler), OfflinePolicy.APPROVED_V1, allowPromoProof = true)
        raw.getAsJsonObject("promo").addProperty("validUntil", "2026-10-20T12:00:00Z")
        wholeWindow.remote.response = EntitlementHttpResponse(200, raw.toString()); wholeWindow.repo.refresh()
        wholeWindow.clock.time += 86400000 - 1; wholeWindow.repo.reevaluate()
        assertTrue(wholeWindow.repo.state.value.proAllowed)
        wholeWindow.clock.time++; wholeWindow.repo.reevaluate()
        assertEquals(TemporalStatus.REVALIDATION_REQUIRED, wholeWindow.repo.state.value.promo)
        assertFalse(wholeWindow.repo.state.value.proAllowed); wholeWindow.repo.close()
    }

    @Test fun approvedPolicyDistinguishesTrialFromPaidAtTheSameThirtyHourAge() = runTest {
        for ((phase, allowed) in listOf("TRIAL" to false, "PAID_PERIOD" to true)) {
            val r = Rig(StandardTestDispatcher(testScheduler), OfflinePolicy.APPROVED_V1)
            val raw = JsonParser.parseString(fixture(play = true, phase = phase)).asJsonObject
            raw.getAsJsonObject("play").addProperty("validUntil", "2026-10-20T12:00:00Z")
            r.remote.response = EntitlementHttpResponse(200, raw.toString()); r.repo.refresh()
            r.clock.time += 30L * 3600000; r.repo.reevaluate()
            assertEquals(allowed, r.repo.state.value.proAllowed); assertTrue(r.repo.state.value.legacyCore)
            r.repo.close()
        }
    }

    @Test fun freshGetAndRootCheckedAtCannotVerifyPromoOrOldPlay() = runTest {
        val r = Rig(StandardTestDispatcher(testScheduler), OfflinePolicy.APPROVED_V1)
        val raw = JsonParser.parseString(fixture(play = true)).asJsonObject
        raw.getAsJsonObject("play").apply {
            addProperty("checkedAt", "2026-10-10T12:00:00Z"); addProperty("validUntil", "2026-10-20T12:00:00Z")
        }
        raw.getAsJsonObject("promo").apply {
            addProperty("state", "ACTIVE"); addProperty("startAt", "2026-10-10T12:00:00Z")
            addProperty("validUntil", "2026-10-20T12:00:00Z")
        }
        r.remote.response = EntitlementHttpResponse(200, raw.toString()); r.repo.refresh()
        assertEquals(TemporalStatus.REVALIDATION_REQUIRED, r.repo.state.value.play)
        assertEquals(TemporalStatus.REVALIDATION_REQUIRED, r.repo.state.value.promo)
        assertFalse(r.repo.state.value.proAllowed); r.repo.close()
    }

    @Test fun promoReceiptSurvivesRestartButNotSourceChangeOrAnotherOwner() = runTest {
        val r = Rig(StandardTestDispatcher(testScheduler), OfflinePolicy.APPROVED_V1, allowPromoProof = true)
        val raw = JsonParser.parseString(fixture()).asJsonObject
        raw.getAsJsonObject("promo").apply {
            addProperty("state", "ACTIVE"); addProperty("startAt", "2026-10-10T12:00:00Z")
            addProperty("validUntil", "2026-10-20T12:00:00Z")
        }
        raw.addProperty("proAllowed", true); r.remote.response = EntitlementHttpResponse(200, raw.toString()); r.repo.refresh()
        val restored = r.restarted(policy = OfflinePolicy.APPROVED_V1); restored.restore()
        assertTrue(restored.state.value.proAllowed); restored.close()
        r.promoProof = null
        raw.addProperty("serverNow", "2026-10-15T12:00:00Z")
        r.remote.response = EntitlementHttpResponse(200, raw.toString()); r.repo.refresh()
        assertFalse(r.repo.state.value.proAllowed) // GET doesn't reset 24h verification age.
        raw.addProperty("revision", 8)
        raw.getAsJsonObject("promo").addProperty("validUntil", "2026-10-21T12:00:00Z")
        r.promoProof = { _, source -> PromoVerification(EntitlementOwner("calcmot-test", "another"), source, 0) }
        r.remote.response = EntitlementHttpResponse(200, raw.toString()); r.repo.refresh()
        assertNull(r.store.rows.values.single().promoVerifiedAt)
        assertFalse(r.repo.state.value.proAllowed); assertTrue(r.repo.state.value.legacyCore)
        r.repo.close()
    }

    @Test fun concurrentRefreshAndAccountChangeDuringPromoVerificationCannotPublishToNewUid() = runTest {
        val r = Rig(StandardTestDispatcher(testScheduler), OfflinePolicy.APPROVED_V1)
        val raw = JsonParser.parseString(fixture()).asJsonObject
        raw.getAsJsonObject("promo").apply {
            addProperty("state", "ACTIVE"); addProperty("startAt", "2026-10-10T12:00:00Z")
            addProperty("validUntil", "2026-10-20T12:00:00Z")
        }
        raw.addProperty("proAllowed", true); r.remote.response = EntitlementHttpResponse(200, raw.toString())
        r.promoProof = { owner, source ->
            r.auth.user = IdentitySession("synthetic-b", emptySet()); r.repo.identityChanged()
            PromoVerification(owner, source, EntitlementCodec.decode(raw.toString()).serverNow)
        }
        val first = launch { r.repo.refresh() }; val second = launch { r.repo.refresh() }
        first.join(); second.join()
        assertFalse(r.repo.current().legacyCore); assertFalse(r.repo.current().proAllowed)
        assertTrue(r.store.rows.keys.all { it.uid != "synthetic-a" }); r.repo.close()
    }

    @Test fun approvedRebootAndReviewRulesCannotRevokeLegacyOrCreateProvisionalAccess() = runTest {
        val r = Rig(StandardTestDispatcher(testScheduler), OfflinePolicy.APPROVED_V1)
        r.remote.response = EntitlementHttpResponse(200, fixture(play = true)); r.repo.refresh()
        r.clock.bootId = "rebooted"; r.repo.reevaluate()
        assertTrue(r.repo.state.value.legacyCore); assertFalse(r.repo.state.value.proAllowed)
        r.repo.close()
        val review = Rig(StandardTestDispatcher(testScheduler), OfflinePolicy.APPROVED_V1)
        val raw = JsonParser.parseString(fixture(legacy = false)).asJsonObject
        raw.getAsJsonObject("legacy").addProperty("state", "UNDER_REVIEW")
        raw.getAsJsonObject("review").addProperty("provisionalUntil", "2026-10-20T12:00:00Z")
        review.remote.response = EntitlementHttpResponse(200, raw.toString()); review.repo.refresh()
        assertEquals(TemporalStatus.DISABLED, review.repo.state.value.review)
        assertFalse(review.repo.state.value.proAllowed); review.repo.close()
    }

    companion object {
        // TEST ONLY, not an approved business tolerance. No fixture policy is installed at runtime.
        private val testPolicy = OfflinePolicy("S08-SYNTHETIC-ONLY", 120000, 120000, 120000)
        fun fixture(legacy: Boolean = true, play: Boolean = false, revision: Int = 7,
            phase: String = "PAID_PERIOD", later: Boolean = false): String = """
            {"contractVersion":"1.0.0","revision":$revision,
             "serverNow":"2026-10-14T12:${if (later) "01" else "00"}:00Z","checkedAt":"2026-10-14T12:00:00Z",
             "subscriptionRequired":false,"legacyCore":$legacy,"coreAllowed":true,
             "proAllowed":${play && !later},"proFeatureSet":[],
             "legacy":{"state":"${if (legacy) "GRANTED" else "UNKNOWN"}",
             "legacyGrantedAt":${if (legacy) "\"2026-10-14T11:00:00Z\"" else "null"},
             "legacyEvidenceType":${if (legacy) "\"ASSISTED_ASSESSMENT\"" else "null"},
             "legacyCoreVersionBaseline":${if (legacy) "\"calcmot-core-3.5-vc12\"" else "null"},
             "migrationVersion":${if (legacy) "\"1\"" else "null"},"grantRevision":${if (legacy) "1" else "null"}},
             "play":{"state":"${if (play) "ACTIVE" else "NONE"}","billingPhase":"${if (play) phase else "NONE"}",
             "validUntil":${if (play) "\"2026-10-14T12:01:00Z\"" else "null"},"checkedAt":"2026-10-14T12:00:00Z",
             "productId":${if (play) "\"synthetic-product\"" else "null"},"basePlanId":${if (play) "\"synthetic-plan\"" else "null"}},
             "promo":{"state":"NONE","startAt":null,"validUntil":null},
             "review":{"claimId":null,"provisionalUntil":null},"nextAction":"NONE"}
        """.trimIndent()
    }
}
