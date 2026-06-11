package br.com.calcmot.model

import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

data class DriverGoal(
    val minValuePerKm: Double = DEFAULT_MIN_VALUE_PER_KM,
    val minValuePerHour: Double = DEFAULT_MIN_VALUE_PER_HOUR,
    val mode: GoalMode = GoalMode.BALANCED
) {
    fun normalized(): DriverGoal {
        return copy(
            minValuePerKm = minValuePerKm.coerceAtLeast(MIN_ALLOWED_GOAL),
            minValuePerHour = minValuePerHour.coerceAtLeast(MIN_ALLOWED_GOAL)
        )
    }

    companion object {
        const val DEFAULT_MIN_VALUE_PER_KM = 1.50
        const val DEFAULT_MIN_VALUE_PER_HOUR = 35.0
        const val MIN_ALLOWED_GOAL = 0.01
    }
}

enum class GoalMode {
    BALANCED,
    PRIORITIZE_KM,
    PRIORITIZE_HOUR
}

enum class ImpactMetric {
    KM,
    HOUR
}

enum class OfferClassification {
    GREAT,
    GOOD,
    WARNING,
    BAD
}

data class OfferFinancialImpact(
    val targetValueByKm: Double,
    val impactByKm: Double,
    val targetValueByHour: Double,
    val impactByHour: Double,
    val finalImpact: Double,
    val finalMetric: ImpactMetric,
    val classification: OfferClassification,
    val message: String,
    val subtext: String
)

object FinancialImpactCalculator {
    fun calculate(tripData: TripData, goal: DriverGoal): OfferFinancialImpact? {
        if (tripData.valor <= 0.0 || tripData.distanciaKm <= 0.0 || tripData.minutosTotais <= 0) {
            return null
        }

        val normalizedGoal = goal.normalized()
        val hours = tripData.minutosTotais / 60.0
        if (hours <= 0.0) return null

        val targetByKm = roundMoney(tripData.distanciaKm * normalizedGoal.minValuePerKm)
        val impactByKm = roundMoney(tripData.valor - targetByKm)
        val targetByHour = roundMoney(hours * normalizedGoal.minValuePerHour)
        val impactByHour = roundMoney(tripData.valor - targetByHour)
        val finalMetric = chooseFinalMetric(impactByKm, impactByHour, normalizedGoal.mode)
        val finalImpact = when (finalMetric) {
            ImpactMetric.KM -> impactByKm
            ImpactMetric.HOUR -> impactByHour
        }
        val classification = classify(tripData, normalizedGoal)

        return OfferFinancialImpact(
            targetValueByKm = targetByKm,
            impactByKm = impactByKm,
            targetValueByHour = targetByHour,
            impactByHour = impactByHour,
            finalImpact = finalImpact,
            finalMetric = finalMetric,
            classification = classification,
            message = buildMessage(classification, finalImpact),
            subtext = buildSubtext(classification, impactByKm, impactByHour, finalMetric)
        )
    }

    fun classify(tripData: TripData, goal: DriverGoal): OfferClassification {
        if (tripData.distanciaKm <= 0.0 || tripData.minutosTotais <= 0) {
            return OfferClassification.BAD
        }

        val normalizedGoal = goal.normalized()
        val meetsKm = tripData.valorPorKm >= normalizedGoal.minValuePerKm
        val meetsHour = tripData.valorPorHora >= normalizedGoal.minValuePerHour
        val greatKm = tripData.valorPorKm >= normalizedGoal.minValuePerKm * 1.2
        val greatHour = tripData.valorPorHora >= normalizedGoal.minValuePerHour * 1.2

        return when {
            greatKm && greatHour -> OfferClassification.GREAT
            meetsKm && meetsHour -> OfferClassification.GOOD
            meetsKm || meetsHour -> OfferClassification.WARNING
            else -> OfferClassification.BAD
        }
    }

    private fun chooseFinalMetric(
        impactByKm: Double,
        impactByHour: Double,
        mode: GoalMode
    ): ImpactMetric {
        return when (mode) {
            GoalMode.PRIORITIZE_KM -> ImpactMetric.KM
            GoalMode.PRIORITIZE_HOUR -> ImpactMetric.HOUR
            GoalMode.BALANCED -> if (impactByKm <= impactByHour) ImpactMetric.KM else ImpactMetric.HOUR
        }
    }

    private fun buildMessage(classification: OfferClassification, finalImpact: Double): String {
        return when {
            classification == OfferClassification.GREAT -> "Excelente: acima da sua meta"
            finalImpact < 0.0 -> "${formatMoney(abs(finalImpact))} abaixo da sua meta"
            else -> "${formatMoney(finalImpact)} acima da sua meta"
        }
    }

    private fun buildSubtext(
        classification: OfferClassification,
        impactByKm: Double,
        impactByHour: Double,
        finalMetric: ImpactMetric
    ): String {
        return when (classification) {
            OfferClassification.GREAT -> "+${formatMoney(min(impactByKm, impactByHour))} estimado"
            OfferClassification.GOOD -> "Boa por km e por hora"
            OfferClassification.WARNING -> when {
                impactByKm < 0.0 -> "${formatMoney(abs(impactByKm))} abaixo por km"
                impactByHour < 0.0 -> "${formatMoney(abs(impactByHour))} abaixo por hora"
                else -> "Confira sua meta"
            }
            OfferClassification.BAD -> when (finalMetric) {
                ImpactMetric.KM -> "Abaixo por km"
                ImpactMetric.HOUR -> "Abaixo por hora"
            }
        }
    }

    private fun roundMoney(value: Double): Double {
        return kotlin.math.round(value * 100.0) / 100.0
    }

    private fun formatMoney(value: Double): String {
        return String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", value)
    }
}
