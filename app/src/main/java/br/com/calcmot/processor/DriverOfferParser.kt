package br.com.calcmot.processor

import br.com.calcmot.DriverApp
import br.com.calcmot.model.OfferCandidate

object DriverOfferParser {

    fun parse(driverApp: DriverApp, rawText: String): OfferCandidate? {
        return when (driverApp) {
            DriverApp.UBER -> OfferParser.parse(rawText)
            DriverApp.NINETY_NINE -> NinetyNineOfferParser.parse(rawText)
            DriverApp.UNKNOWN -> null
        }
    }
}
