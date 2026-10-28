package br.com.calcmot.entitlement

/** A server projection, not an Android command to issue rights. No token is persisted. */
internal data class EntitlementOwner(val project: String, val uid: String) {
    init { require(project.isNotBlank() && uid.isNotBlank() && uid.length <= 128) }
}

internal data class LegacySource(val state: String, val grantedAt: Long?, val evidence: String?,
    val baseline: String?, val migration: String?, val revision: Long?)
internal data class PlaySource(val state: String, val phase: String, val until: Long?,
    val checkedAt: Long?, val product: String?, val plan: String?)
internal data class PromoSource(val state: String, val start: Long?, val until: Long?)
internal data class ReviewSource(val claim: String?, val until: Long?)
internal data class EntitlementSnapshot(val revision: Long, val serverNow: Long, val checkedAt: Long,
    val subscriptionRequired: Boolean, val legacyCore: Boolean, val coreAllowed: Boolean,
    val proAllowed: Boolean, val features: Set<String>, val legacy: LegacySource,
    val play: PlaySource, val promo: PromoSource, val review: ReviewSource, val nextAction: String)

internal data class EntitlementCache(val owner: EntitlementOwner, val snapshotJson: String,
    val snapshot: EntitlementSnapshot, val elapsed: Long, val boot: String?, val policyVersion: String,
    val promoVerifiedAt: Long? = null)

/** Approved OQ-03, independently of commercial gates. No purchased period is extended.
 * Play age always starts at play.checkedAt; a GET cannot renew Play verification.
 */
internal data class OfflinePolicy(val version: String, val paidMaxAgeMs: Long,
    val trialMaxAgeMs: Long, val promoMaxAgeMs: Long) {
    init { require(version.isNotBlank()); require(listOf(paidMaxAgeMs, trialMaxAgeMs, promoMaxAgeMs).all { it > 0 }) }
    fun playAge(phase: String): Long? = when (phase) {
        "PAID_PERIOD" -> paidMaxAgeMs
        "TRIAL" -> trialMaxAgeMs
        else -> null
    }
    companion object {
        val APPROVED_V1 = OfflinePolicy("OQ03-V1-72H-24H", 72L * 3600000, 24L * 3600000, 24L * 3600000)
    }
}

internal enum class EntitlementStatus { EMPTY, READY, REFRESHING, OFFLINE, SESSION_EXPIRED,
    UNAUTHORIZED, BACKEND_UNAVAILABLE, INCONSISTENT, STORAGE_UNAVAILABLE, IDENTITY_CHANGED }
internal enum class TemporalStatus { NO_SOURCE, POLICY_REQUIRED, REVALIDATION_REQUIRED, VALID, EXPIRED, DISABLED }

/** A diagnostic/read model only. No commercial gate is connected to current Core. */
internal data class EntitlementState(val owner: EntitlementOwner? = null,
    val status: EntitlementStatus = EntitlementStatus.EMPTY, val legacyCore: Boolean = false,
    val proAllowed: Boolean = false, val proFeatures: Set<String> = emptySet(),
    val play: TemporalStatus = TemporalStatus.NO_SOURCE, val promo: TemporalStatus = TemporalStatus.NO_SOURCE,
    val review: TemporalStatus = TemporalStatus.NO_SOURCE,
    val billingPhase: String = "NONE", val revision: Long? = null,
    val playState: String = "NONE", val legacyState: String = "UNKNOWN", val nextAction: String = "RETRY",
    val claimId: String? = null,
    val currentFreeCorePreserved: Boolean = true)

internal interface EntitlementClock { fun elapsed(): Long; fun boot(): String? }
internal interface EntitlementStore {
    suspend fun read(owner: EntitlementOwner): EntitlementCache?
    suspend fun write(cache: EntitlementCache)
}
internal data class EntitlementHttpResponse(val code: Int, val body: String)
internal interface EntitlementRemote { suspend fun get(token: String): EntitlementHttpResponse }

/** Separate trusted source adapter. Never infer this receipt from local dates or root checkedAt.
 * No production adapter exists until the promotional source has an authoritative verifier.
 * It cannot grant a promo: the authenticated S01 snapshot must already contain the same source.
 */
internal data class PromoVerification(val owner: EntitlementOwner, val source: PromoSource,
    val verifiedAt: Long)
internal fun interface PromoVerifier {
    suspend fun verify(token: String, owner: EntitlementOwner, source: PromoSource): PromoVerification?
}
