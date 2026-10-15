package br.com.calcmot.ui

/**
 * Public Play Store invitation. No account, offer, location or contact data is included.
 * An attributed referral link may replace this URL only after its own launch is approved.
 */
internal object CalcMotShareMessage {
    const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=br.com.calcmot"

    fun text(): String =
        "Conhece o CalcMot? Ele ajuda motoristas de Uber e 99 a avaliar ofertas " +
            "por hora e por quilômetro. Compartilhe com quem também dirige!\n" +
            PLAY_STORE_URL
}
