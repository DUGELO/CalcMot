package br.com.calcmot.processor

import br.com.calcmot.model.TripData

object TextProcessor {
    fun processText(rawText: String): TripData? {
        return OfferParser.parse(rawText)?.toTripData()
    }
}
