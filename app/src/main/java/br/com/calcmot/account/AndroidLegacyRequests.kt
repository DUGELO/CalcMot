package br.com.calcmot.account

import android.app.Application
import android.content.Context
import br.com.calcmot.AppSettings
import br.com.calcmot.BuildConfig
import br.com.calcmot.ReadingPipelineRuntime
import br.com.calcmot.identity.IdentityCoordinator
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL
import java.security.MessageDigest
import java.util.UUID
import javax.net.ssl.HttpsURLConnection

/** Production stays OFF. This opt-in accepts only the explicitly configured DEV origin. */
internal fun createLegacyRequests(application: Application, identity: IdentityCoordinator): LegacyRequests {
    if (BuildConfig.BUILD_TYPE != "identityDev" || !BuildConfig.LEGACY_INTAKE_ENABLED) return UnavailableLegacyRequests
    val project = FirebaseApp.getInstance().options.projectId ?: return UnavailableLegacyRequests
    return LegacyClaimClient(project, identity, AndroidLegacyClaimKeys(application),
        HttpsLegacyClaimTransport(BuildConfig.ACCOUNT_API_BASE_URL), workActive = {
            // Technical admission check only; not OQ-10 proof of a drained driving session.
            AppSettings.isMonitoringEnabled(application) || ReadingPipelineRuntime.snapshot.value.pipelineState in setOf(
                ReadingPipelineRuntime.PipelineState.CAPTURING, ReadingPipelineRuntime.PipelineState.OCR,
                ReadingPipelineRuntime.PipelineState.BUSY)
        })
}

internal class AndroidLegacyClaimKeys(context: Context) : LegacyClaimKeys {
    private val preferences = context.getSharedPreferences("legacy_claim_requests_v1", Context.MODE_PRIVATE)
    private fun slot(project: String, uid: String) = MessageDigest.getInstance("SHA-256")
        .digest("$project\u0000$uid".toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    override fun existing(project: String, uid: String): String? = preferences.getString(slot(project, uid), null)
    @Synchronized override fun getOrCreate(project: String, uid: String): String = existing(project, uid)
        ?: UUID.randomUUID().toString().also {
            if (!preferences.edit().putString(slot(project, uid), it).commit()) throw IOException("Request key unavailable")
        }
}

internal class HttpsLegacyClaimTransport(baseUrl: String) : LegacyClaimTransport {
    private val endpoint = URL(baseUrl.trimEnd('/') + LegacyClaimContract.path).also {
        require(it.protocol == "https" && it.userInfo == null && it.query == null && it.ref == null && it.port == -1)
        require(it.path == LegacyClaimContract.path && it.host.matches(Regex("calcmot-api-dev\\.[a-z0-9-]+\\.workers\\.dev")))
    }
    override suspend fun post(token: String, key: String): LegacyClaimResponse = withContext(Dispatchers.IO) {
        require(token.isNotBlank() && '\r' !in token && '\n' !in token)
        val connection = endpoint.openConnection() as HttpsURLConnection
        try {
            connection.requestMethod = LegacyClaimContract.method
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 10000; connection.readTimeout = 10000; connection.useCaches = false
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")
            LegacyClaimContract.headers(key).forEach { (name, value) -> connection.setRequestProperty(name, value) }
            val body = LegacyClaimContract.body.toByteArray(Charsets.UTF_8)
            connection.setFixedLengthStreamingMode(body.size)
            connection.outputStream.use { it.write(body) }
            val status = connection.responseCode
            if (status != 200) return@withContext LegacyClaimResponse(status, "")
            require(connection.contentType?.substringBefore(';')?.trim() == "application/json")
            val bytes = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= 4096)
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            LegacyClaimResponse(status, bytes.toString(Charsets.UTF_8))
        } finally { connection.disconnect() }
    }
}
