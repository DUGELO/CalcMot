package br.com.calcmot.legacy

/** Auxiliary installation facts only. No entitlement, historical proof or Firebase UID. */
data class LegacyLocalEvidence(
    val evidenceVersion: Int = 1,
    val firstInstallTimeMs: Long?,
    val lastUpdateTimeMs: Long?,
    val currentVersionCode: Long?,
    val storedOnboardingCompleted: Boolean?
) {
    // Even a plausible update is not enough to authorize a grant.
    val hasAuxiliaryIndication: Boolean
        get() = storedOnboardingCompleted == true || (
            firstInstallTimeMs != null && firstInstallTimeMs > 0 &&
                lastUpdateTimeMs != null && lastUpdateTimeMs > firstInstallTimeMs
            )
}
