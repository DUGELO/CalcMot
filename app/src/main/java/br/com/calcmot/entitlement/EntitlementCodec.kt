package br.com.calcmot.entitlement

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.Strictness
import com.google.gson.stream.JsonReader
import java.io.StringReader
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Strict wire shape + S01 cross-field invariants; never permissive Boolean/string coercion. */
internal object EntitlementCodec {
    fun decode(json: String): EntitlementSnapshot {
        require(json.toByteArray(Charsets.UTF_8).size <= 65536)
        val reader = JsonReader(StringReader(json)).apply { strictness = Strictness.STRICT }
        val root = JsonParser.parseReader(reader).asJsonObject
        require(reader.peek() == com.google.gson.stream.JsonToken.END_DOCUMENT)
        root.shape("contractVersion revision serverNow checkedAt subscriptionRequired legacyCore coreAllowed proAllowed proFeatureSet legacy play promo review nextAction")
        require(root.string("contractVersion") == "1.0.0")
        val legacy = root.getAsJsonObject("legacy").run {
            shape("state legacyGrantedAt legacyEvidenceType legacyCoreVersionBaseline migrationVersion grantRevision")
            LegacySource(enum("state", "UNKNOWN UNDER_REVIEW GRANTED REJECTED"), instant("legacyGrantedAt"),
                nullableString("legacyEvidenceType"), nullableString("legacyCoreVersionBaseline"),
                nullableString("migrationVersion"), if (get("grantRevision").isJsonNull) null else integer("grantRevision"))
        }
        val play = root.getAsJsonObject("play").run {
            shape("state billingPhase validUntil checkedAt productId basePlanId")
            PlaySource(enum("state", "NONE PENDING ACTIVE CANCELED_WITH_ACCESS GRACE ON_HOLD PAUSED EXPIRED REVOKED UNKNOWN"),
                enum("billingPhase", "NONE TRIAL PAID_PERIOD UNKNOWN"), instant("validUntil"), instant("checkedAt"),
                nullableString("productId"), nullableString("basePlanId"))
        }
        val promo = root.getAsJsonObject("promo").run {
            shape("state startAt validUntil")
            PromoSource(enum("state", "NONE ACTIVE EXPIRED"), instant("startAt"), instant("validUntil"))
        }
        val review = root.getAsJsonObject("review").run {
            shape("claimId provisionalUntil"); ReviewSource(nullableString("claimId"), instant("provisionalUntil"))
        }
        val features = root.getAsJsonArray("proFeatureSet").map { it.strictString() }
        require(features.distinct().size == features.size && features.all { it == "PRO_INSIGHTS_WEEKLY" })
        val result = EntitlementSnapshot(root.integer("revision"), root.instant("serverNow")!!,
            root.instant("checkedAt")!!, root.bool("subscriptionRequired"), root.bool("legacyCore"),
            root.bool("coreAllowed"), root.bool("proAllowed"), features.toSet(), legacy, play, promo, review,
            root.enum("nextAction", "NONE LINK_ACCOUNT LEGACY_CLAIM RESTORE_PURCHASE MANAGE_SUBSCRIPTION CHOOSE_PLAN RETRY CONTACT_SUPPORT"))
        val now = result.serverNow
        require(result.checkedAt <= now)
        require(result.legacyCore == (legacy.state == "GRANTED"))
        if (legacy.state == "GRANTED") require(legacy.grantedAt != null && legacy.grantedAt <= now &&
            legacy.evidence != null && legacy.baseline != null && legacy.migration != null && legacy.revision != null)
        if (play.state in activePlay) require(play.until != null && play.checkedAt != null && play.product != null && play.plan != null)
        if (play.state == "NONE") require(play.phase == "NONE" && play.until == null && play.product == null && play.plan == null)
        require(play.checkedAt == null || play.checkedAt <= now)
        if (promo.state != "NONE") require(promo.start != null && promo.until != null && promo.start < promo.until)
        else require(promo.start == null && promo.until == null)
        val playValid = play.state in activePlay && play.until!! > now
        val promoValid = promo.state == "ACTIVE" && promo.start!! <= now && now < promo.until!!
        val reviewValid = legacy.state == "UNDER_REVIEW" && review.until != null && now < review.until
        require(result.proAllowed == (playValid || promoValid))
        require(result.coreAllowed == (!result.subscriptionRequired || result.legacyCore || playValid || promoValid || reviewValid))
        require(result.proAllowed || features.isEmpty())
        return result
    }

    val activePlay = setOf("ACTIVE", "CANCELED_WITH_ACCESS", "GRACE")
    private fun JsonObject.shape(keys: String) = require(keySet() == keys.split(" ").toSet())
    private fun JsonElement.strictString(): String {
        require(isJsonPrimitive && asJsonPrimitive.isString); return asString.also { require(it.isNotBlank()) }
    }
    private fun JsonObject.string(key: String) = get(key).strictString()
    private fun JsonObject.nullableString(key: String) = if (get(key).isJsonNull) null else string(key)
    private fun JsonObject.enum(key: String, values: String) = string(key).also { require(it in values.split(" ")) }
    private fun JsonObject.bool(key: String): Boolean {
        val value = get(key); require(value.isJsonPrimitive && value.asJsonPrimitive.isBoolean); return value.asBoolean
    }
    private fun JsonObject.integer(key: String): Long {
        val value = get(key); require(value.isJsonPrimitive && value.asJsonPrimitive.isNumber)
        require(value.toString().matches(Regex("(?:0|[1-9][0-9]*)")))
        return value.asLong.also { require(it in 0..9007199254740991L) }
    }
    private fun JsonObject.instant(key: String): Long? {
        val text = nullableString(key) ?: return null
        return utc(text)
    }
    fun utc(text: String): Long {
        require(text.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}(\\.[0-9]{1,9})?Z")))
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC"); isLenient = false
        }
        val position = ParsePosition(0)
        val seconds = format.parse(text.substring(0, 19), position)
        require(seconds != null && position.index == 19)
        val fraction = if (text[19] == '.') text.substring(20, text.length - 1).padEnd(3, '0').take(3).toLong() else 0
        return seconds.time + fraction
    }
}
