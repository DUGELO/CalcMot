package br.com.calcmot.ninetynine

internal object NinetyNineRecognitionConfig {
    val supportedPackages = setOf(
        "com.app99.driver",
        "br.com.taxis99"
    )

    val triggerViewIds = setOf(
        "com.app99.driver:id/main_flutter_flutter_root",
        "com.app99.driver:id/flutter_deal_gesture_container",
        "com.app99.driver:id/broad_order_container",
        "com.app99.driver:id/flutter_home_page_container",
        "br.com.taxis99:id/main_flutter_flutter_root",
        "br.com.taxis99:id/flutter_deal_gesture_container",
        "br.com.taxis99:id/broad_order_container",
        "br.com.taxis99:id/flutter_home_page_container"
    )

    val inactiveFrameMarkers = setOf(
        "buscando",
        "carregando",
        "de corrida",
        "expirou",
        "aguarde",
        "motorista aceitou",
        "outro motorista aceitou",
        "corrida nao esta mais disponivel",
        "chamada nao esta mais disponivel"
    )

    val offerMarkers = setOf(
        "aceitar",
        "aceitar por",
        "escolher",
        "negocia",
        "taxa de deslocamento",
        "perfil essencial",
        "perfil premium",
        "corridas"
    )

    val actionMarkers = setOf(
        "aceitar",
        "aceitar por",
        "aceitar corrida",
        "escolher",
        "selecionar"
    )
}
