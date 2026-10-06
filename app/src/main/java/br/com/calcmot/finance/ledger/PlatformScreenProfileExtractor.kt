package br.com.calcmot.finance.ledger

/**
 * A screen profile is enabled only after positive/negative fixtures and physical validation.
 * Unknown screens must never be converted into financial facts.
 */
interface PlatformScreenProfileExtractor<Input, Output> {
    val profileId: String
    val enabledInProduction: Boolean
    fun extract(input: Input): PlatformProfileResult<Output>
}

sealed interface PlatformProfileResult<out T> {
    data class Confirmed<T>(val value: T) : PlatformProfileResult<T>
    data class Partial<T>(val value: T) : PlatformProfileResult<T>
    data class Insufficient(val reason: String) : PlatformProfileResult<Nothing>
}

object DisabledPlatformScreenProfiles {
    val productionEnabledProfileIds: Set<String> = emptySet()
}
