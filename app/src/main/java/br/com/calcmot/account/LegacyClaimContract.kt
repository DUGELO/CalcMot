package br.com.calcmot.account

import br.com.calcmot.entitlement.EntitlementCodec
import com.google.gson.JsonParser
import com.google.gson.Strictness
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.StringReader

/** S01 intake preparation only. A declaration never grants legacy; no user-selected UID. */
internal object LegacyClaimContract {
    const val path = "/v1/legacy/claim"
    const val method = "POST"
    const val body = "{\"evidenceType\":\"USER_DECLARATION\",\"evidenceHash\":null}"
    fun headers(idempotencyKey: String): Map<String, String> {
        require(idempotencyKey.isNotBlank() && '\r' !in idempotencyKey && '\n' !in idempotencyKey)
        return mapOf("Content-Type" to "application/json", "Idempotency-Key" to idempotencyKey)
    }
    fun decodeView(json: String): LegacyRequestView {
        require(json.toByteArray().size <= 4096)
        val reader = JsonReader(StringReader(json)).apply { strictness = Strictness.STRICT }
        val root = JsonParser.parseReader(reader).asJsonObject
        require(reader.peek() == JsonToken.END_DOCUMENT)
        require(root.keySet() == setOf("claimId", "state", "openedAt", "resolvedAt", "provisionalUntil", "revision", "serverNow"))
        fun string(key: String): String {
            val v = root.get(key); require(v.isJsonPrimitive && v.asJsonPrimitive.isString && v.asString.isNotBlank())
            return v.asString
        }
        val state = string("state"); require(state in setOf("UNDER_REVIEW", "GRANTED", "REJECTED"))
        val now = EntitlementCodec.utc(string("serverNow"))
        val opened = EntitlementCodec.utc(string("openedAt")); require(opened <= now)
        val revision = root.get("revision")
        require(revision.isJsonPrimitive && revision.asJsonPrimitive.isNumber &&
            revision.toString().matches(Regex("(?:0|[1-9][0-9]*)")) && revision.asLong in 0..9007199254740991L)
        if (state == "UNDER_REVIEW") require(root.get("resolvedAt").isJsonNull)
        else { require(EntitlementCodec.utc(string("resolvedAt")) in opened..now); require(root.get("provisionalUntil").isJsonNull) }
        if (!root.get("provisionalUntil").isJsonNull) EntitlementCodec.utc(string("provisionalUntil"))
        return LegacyRequestView(string("claimId"), state)
    }
}

/** No deployment/retention approval for operational S06 intake. No background POST is issued. */
internal object UnavailableLegacyRequests : LegacyRequests {
    override val available = false
    override suspend fun submit(): LegacyRequestView = error("Legacy intake not enabled")
}
