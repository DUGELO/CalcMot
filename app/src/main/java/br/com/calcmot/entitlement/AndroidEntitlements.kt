package br.com.calcmot.entitlement

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import br.com.calcmot.identity.FirebaseIdentityAuth
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URL
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.net.ssl.HttpsURLConnection

internal class AndroidEntitlementClock(private val application: Application) : EntitlementClock {
    // A reboot restarts this process. Read the provider once during construction off the offer
    // path, never during RAM evaluation/timers; elapsedRealtime itself performs no disk/network I/O.
    private val bootId = runCatching {
        Settings.Global.getInt(application.contentResolver, Settings.Global.BOOT_COUNT).toString()
    }.getOrNull()
    override fun elapsed() = SystemClock.elapsedRealtime()
    override fun boot(): String? = bootId
}

/** HTTPS only; redirects refused so bearer never follows another origin. Technical I/O limits,
 * not a commercial TTL. Tokens stay in memory and are never attached to exceptions/logs.
 */
internal class HttpsEntitlementRemote(baseUrl: String) : EntitlementRemote {
    private val endpoint = URL(baseUrl.trimEnd('/') + "/v1/me/entitlement").also {
        require(it.protocol == "https" && it.userInfo == null && it.query == null && it.ref == null)
        require(it.path == "/v1/me/entitlement" && it.host.isNotBlank())
    }
    override suspend fun get(token: String): EntitlementHttpResponse = withContext(Dispatchers.IO) {
        require(token.isNotBlank() && '\r' !in token && '\n' !in token)
        val connection = endpoint.openConnection() as HttpsURLConnection
        try {
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.useCaches = false
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")
            val status = connection.responseCode
            if (status != 200) return@withContext EntitlementHttpResponse(status, "")
            require(connection.contentType?.substringBefore(';')?.trim() == "application/json")
            val bytes = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (true) {
                    val size = input.read(buffer)
                    if (size < 0) break
                    require(output.size() + size <= 65536)
                    output.write(buffer, 0, size)
                }
                output.toByteArray()
            }
            EntitlementHttpResponse(status, bytes.toString(Charsets.UTF_8))
        } finally { connection.disconnect() }
    }
}

/** Atomic, installation-local and excluded from Auto Backup. HMAC is local tamper detection,
 * not a backend signature or proof on a rooted device. Key loss requires server recovery.
 */
internal class AndroidEntitlementStore(application: Application) : EntitlementStore {
    private val directory = File(application.noBackupFilesDir, "entitlements-v1")
    private fun file(owner: EntitlementOwner): AtomicFile {
        val name = MessageDigest.getInstance("SHA-256")
            .digest((owner.project + "\u0000" + owner.uid).toByteArray()).joinToString("") { "%02x".format(it) }
        return AtomicFile(File(directory, "$name.json"))
    }
    private fun key(): SecretKey {
        val keys = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val alias = "calcmot.entitlement.cache.v1"
        (keys.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                .setDigests(KeyProperties.DIGEST_SHA256).build())
        }.generateKey()
    }
    private fun sign(text: String): ByteArray = Mac.getInstance("HmacSHA256").run {
        init(key()); doFinal(text.toByteArray(Charsets.UTF_8))
    }
    override suspend fun read(owner: EntitlementOwner): EntitlementCache? = readRecord(owner)
    @Synchronized private fun readRecord(owner: EntitlementOwner): EntitlementCache? {
        val bytes = try { file(owner).openRead().use { input ->
            val result = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            while (true) {
                val size = input.read(buffer)
                if (size < 0) break
                require(result.size() + size <= 262144)
                result.write(buffer, 0, size)
            }
            result.toByteArray()
        } } catch (_: FileNotFoundException) { return null }
        val envelope = JsonParser.parseString(bytes.toString(Charsets.UTF_8)).asJsonObject
        val payload = envelope.get("payload").asString
        require(MessageDigest.isEqual(sign(payload), Base64.decode(envelope.get("mac").asString, Base64.NO_WRAP)))
        val stored = JsonParser.parseString(payload).asJsonObject
        val storageVersion = stored.get("storageVersion").asInt
        require(storageVersion in 1..2)
        require(stored.get("project").asString == owner.project && stored.get("uid").asString == owner.uid)
        val cached = stored.getAsJsonObject("cache")
        val raw = cached.get("snapshot").toString()
        return EntitlementCache(owner, raw, EntitlementCodec.decode(raw), cached.get("cachedAtElapsedRealtime").asLong,
            stored.get("boot").takeUnless { it.isJsonNull }?.asString, cached.get("policyVersion").asString,
            if (storageVersion == 2) stored.get("promoVerifiedAt")?.takeUnless { it.isJsonNull }?.asLong else null)
    }
    override suspend fun write(cache: EntitlementCache) = writeRecord(cache)
    @Synchronized private fun writeRecord(cache: EntitlementCache) {
        require(cache.snapshot == EntitlementCodec.decode(cache.snapshotJson))
        require(cache.elapsed >= 0 && cache.policyVersion.isNotBlank())
        require(cache.promoVerifiedAt == null || (cache.snapshot.promo.state == "ACTIVE" &&
            cache.promoVerifiedAt in 0..cache.snapshot.serverNow))
        require(directory.isDirectory || directory.mkdirs())
        val payload = JsonObject().apply {
            addProperty("storageVersion", 2); addProperty("project", cache.owner.project)
            addProperty("promoVerifiedAt", cache.promoVerifiedAt)
            addProperty("uid", cache.owner.uid); addProperty("boot", cache.boot)
            add("cache", JsonObject().apply {
                add("snapshot", JsonParser.parseString(cache.snapshotJson))
                addProperty("cachedAtElapsedRealtime", cache.elapsed); addProperty("policyVersion", cache.policyVersion)
            })
        }.toString()
        val envelope = JsonObject().apply {
            addProperty("payload", payload); addProperty("mac", Base64.encodeToString(sign(payload), Base64.NO_WRAP))
        }.toString()
        val atomic = file(cache.owner)
        val output = atomic.startWrite()
        try { output.write(envelope.toByteArray(Charsets.UTF_8)); atomic.finishWrite(output) }
        catch (error: Exception) { atomic.failWrite(output); throw error }
    }
}

/** Explicit environment wiring. Not instantiated in production or attached to offer processing.
 * One instance per FirebaseApp/environment; caller closes it with its application test scope.
 * Starts refresh only on Activity resume, never in background services. No polling/retry loop.
 */
internal class AndroidEntitlements(application: Application, firebase: FirebaseApp, baseUrl: String) : Closeable {
    companion object {
        fun createDefault(application: Application, baseUrl: String) =
            AndroidEntitlements(application, FirebaseApp.getInstance(), baseUrl)
    }
    private val auth = FirebaseAuth.getInstance(firebase)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var refreshJob: Job? = null
    val repository = EntitlementRepository(requireNotNull(firebase.options.projectId), FirebaseIdentityAuth(auth),
        HttpsEntitlementRemote(baseUrl), AndroidEntitlementStore(application), AndroidEntitlementClock(application),
        OfflinePolicy.APPROVED_V1)
    // Firebase also notifies immediately after registration. A same-owner notification must
    // not discard an already restored record or the outcome of an in-flight account query.
    // reevaluate clears synchronously when the actual project/UID owner changes.
    private val listener = FirebaseAuth.AuthStateListener { repository.reevaluate() }
    private val app = application
    private val lifecycle = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) {
            if (refreshJob?.isActive != true) refreshJob = scope.launch { repository.restore(); repository.refresh() }
        }
        override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }
    init { auth.addAuthStateListener(listener); app.registerActivityLifecycleCallbacks(lifecycle) }
    override fun close() {
        auth.removeAuthStateListener(listener); app.unregisterActivityLifecycleCallbacks(lifecycle)
        repository.close(); scope.cancel()
    }
}
