package br.com.calcmot

enum class DriverApp(
    val id: String,
    val displayName: String,
    val packageNames: List<String>,
    val actionLabels: Set<String>,
    val serviceLabels: Set<String>
) {
    UBER(
        id = "uber",
        displayName = "Uber Driver",
        packageNames = listOf("com.ubercab.driver", "com.ubercab"),
        actionLabels = setOf("aceitar", "selecionar"),
        serviceLabels = setOf("uberx", "priority", "comfort", "black")
    ),
    NINETY_NINE(
        id = "99",
        displayName = "99 Motorista",
        packageNames = listOf("com.app99.driver", "br.com.taxis99"),
        actionLabels = setOf("aceitar", "selecionar", "aceitar corrida", "escolher"),
        serviceLabels = setOf("pop", "99pop", "99comfort", "99plus", "99entrega", "99moto")
    ),
    UNKNOWN(
        id = "unknown",
        displayName = "App de motorista",
        packageNames = emptyList(),
        actionLabels = emptySet(),
        serviceLabels = emptySet()
    );

    val preferredPackageName: String?
        get() = packageNames.firstOrNull()

    fun ownsPackage(packageName: CharSequence?): Boolean {
        val normalized = packageName?.toString()?.trim().orEmpty()
        return normalized.isNotEmpty() && normalized in packageNames
    }

    companion object {
        val supported: List<DriverApp> = listOf(UBER, NINETY_NINE)

        fun fromId(id: String?): DriverApp {
            return entries.firstOrNull { it.id == id } ?: UNKNOWN
        }

        fun fromPackage(packageName: CharSequence?): DriverApp {
            return supported.firstOrNull { it.ownsPackage(packageName) } ?: UNKNOWN
        }
    }
}
