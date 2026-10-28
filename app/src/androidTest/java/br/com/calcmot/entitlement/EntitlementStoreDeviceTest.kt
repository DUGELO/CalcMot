package br.com.calcmot.entitlement

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.MessageDigest
import br.com.calcmot.identity.IdentityAuthPort
import br.com.calcmot.identity.IdentitySession
import com.google.gson.JsonParser

/** Run only against the separate identityDev package, never the installed Play app.
 * Synthetic local records, no Firebase identity/network/benefits or key deletion.
 */
class EntitlementStoreDeviceTest {
    private val fixture = """{"contractVersion":"1.0.0","revision":1,"serverNow":"2026-10-14T12:00:00Z","checkedAt":"2026-10-14T12:00:00Z","subscriptionRequired":false,"legacyCore":true,"coreAllowed":true,"proAllowed":false,"proFeatureSet":[],"legacy":{"state":"GRANTED","legacyGrantedAt":"2026-10-14T11:00:00Z","legacyEvidenceType":"ASSISTED_ASSESSMENT","legacyCoreVersionBaseline":"calcmot-core-3.5-vc12","migrationVersion":"synthetic","grantRevision":1},"play":{"state":"NONE","billingPhase":"NONE","validUntil":null,"checkedAt":null,"productId":null,"basePlanId":null},"promo":{"state":"NONE","startAt":null,"validUntil":null},"review":{"claimId":null,"provisionalUntil":null},"nextAction":"NONE"}"""
    @Test fun realAtomicFileKeystoreIsolationAndTamperDetection() = runBlocking(Dispatchers.IO) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName == "br.com.calcmot.identitydev")
        val app = context.applicationContext as Application
        val a = EntitlementOwner("s08-synthetic-project", "s08-device-store-test-only")
        val b = EntitlementOwner("s08-synthetic-project", "s08-device-other-test-only")
        val raw = fixture
        val cache = EntitlementCache(a, raw, EntitlementCodec.decode(raw), 1000, "synthetic-boot", "OQ-03-PENDING")
        val filename = MessageDigest.getInstance("SHA-256").digest((a.project + "\u0000" + a.uid).toByteArray())
            .joinToString("") { "%02x".format(it) } + ".json"
        val file = File(File(app.noBackupFilesDir, "entitlements-v1"), filename)
        try {
            val store = AndroidEntitlementStore(app); store.write(cache)
            assertEquals(cache, AndroidEntitlementStore(app).read(a))
            assertNull(store.read(b))
            // Backward-compatible v1 envelope: no source-specific promo verification exists.
            // Re-sign ONLY this synthetic record with its existing local DEV key; never export it.
            val envelope = JsonParser.parseString(file.readText()).asJsonObject
            val oldPayload = JsonParser.parseString(envelope.get("payload").asString).asJsonObject
            oldPayload.addProperty("storageVersion", 1)
            // v1 had no promotional provenance; even a signed unknown field must not migrate it.
            oldPayload.addProperty("promoVerifiedAt", 1)
            val text = oldPayload.toString()
            val keys = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val mac = javax.crypto.Mac.getInstance("HmacSHA256").apply {
                init(keys.getKey("calcmot.entitlement.cache.v1", null))
            }.doFinal(text.toByteArray())
            envelope.addProperty("payload", text)
            envelope.addProperty("mac", android.util.Base64.encodeToString(mac, android.util.Base64.NO_WRAP))
            file.writeText(envelope.toString())
            assertEquals(cache, AndroidEntitlementStore(app).read(a))
            file.writeText(file.readText().replace("s08-device-store-test-only", "s08-device-forged-test-only"))
            try { store.read(a); fail("tampered cache accepted") } catch (_: IllegalArgumentException) { }
        } finally {
            // Only this synthetic fixture; no user records or shared Keystore key are removed.
            file.delete(); File(file.path + ".bak").delete(); File(file.path + ".new").delete()
        }
    }
    @Test fun realStoreBootMismatchExpirationAndIdentityRecovery() = runBlocking(Dispatchers.IO) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName == "br.com.calcmot.identitydev")
        val app = context.applicationContext as Application
        val owner = EntitlementOwner("s08-synthetic-project", "s08-device-policy-test-only")
        var user: IdentitySession? = IdentitySession(owner.uid, emptySet())
        val auth = object : IdentityAuthPort {
            override fun currentSession() = user
            override suspend fun firebaseIdToken(forceRefresh: Boolean): String = error("no network in restore")
            override suspend fun signIn(googleIdToken: String): IdentitySession = error("unused")
            override suspend fun link(uid: String, googleIdToken: String): IdentitySession = error("unused")
            override suspend fun reauthenticate(uid: String, googleIdToken: String): IdentitySession = error("unused")
        }
        val clock = AndroidEntitlementClock(app)
        assertNotNull("Device must expose a boot anchor for this controlled test", clock.boot())
        val store = AndroidEntitlementStore(app)
        val policy = OfflinePolicy.APPROVED_V1
        val root = JsonParser.parseString(fixture).asJsonObject
        root.addProperty("proAllowed", true)
        root.getAsJsonObject("play").apply {
            addProperty("state", "ACTIVE"); addProperty("billingPhase", "PAID_PERIOD")
            addProperty("checkedAt", "2026-10-14T12:00:00Z"); addProperty("validUntil", "2026-10-20T12:00:00Z")
            addProperty("productId", "synthetic"); addProperty("basePlanId", "synthetic")
        }
        fun repository() = EntitlementRepository(owner.project, auth,
            object : EntitlementRemote { override suspend fun get(token: String): EntitlementHttpResponse = error("no request") },
            store, clock, policy)
        val name = MessageDigest.getInstance("SHA-256").digest((owner.project + "\u0000" + owner.uid).toByteArray())
            .joinToString("") { "%02x".format(it) } + ".json"
        val file = File(File(app.noBackupFilesDir, "entitlements-v1"), name)
        var repo: EntitlementRepository? = null
        try {
            val raw = root.toString()
            store.write(EntitlementCache(owner, raw, EntitlementCodec.decode(raw), clock.elapsed(),
                "controlled-old-boot", policy.version))
            repo = repository(); repo.restore()
            assertEquals(TemporalStatus.REVALIDATION_REQUIRED, repo.current().play)
            assertTrue(repo.current().legacyCore); assertFalse(repo.current().proAllowed); repo.close()
            root.getAsJsonObject("play").addProperty("checkedAt", "2026-10-11T12:00:00Z")
            val aged = root.toString()
            store.write(EntitlementCache(owner, aged, EntitlementCodec.decode(aged), clock.elapsed(), clock.boot(), policy.version))
            repo = repository(); repo.restore()
            assertFalse(repo.current().proAllowed); assertTrue(repo.current().legacyCore)
            user = IdentitySession("s08-device-other-policy-test-only", emptySet()); repo.identityChanged(); repo.restore()
            assertFalse(repo.current().legacyCore)
            user = IdentitySession(owner.uid, emptySet()); repo.identityChanged(); repo.restore()
            assertTrue(repo.current().legacyCore); assertFalse(repo.current().proAllowed)
        } finally {
            repo?.close()
            file.delete(); File(file.path + ".bak").delete(); File(file.path + ".new").delete()
        }
    }
}
