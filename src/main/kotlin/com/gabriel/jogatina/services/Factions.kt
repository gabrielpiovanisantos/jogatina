package com.gabriel.jogatina.services

/** Facções oficiais, com os nomes e a ordem do manual brasileiro "As Leis de Root". */
object Factions {
    val OFFICIAL = listOf(
        "Marqueses",
        "Dinastia das Rapinas",
        "Aliança da Floresta",
        "Malandro",
        "Lagartos Cultistas",
        "Companhia Ribeirinha",
        "Ducado Subterrâneo",
        "Conspiração Corvídea",
        "Senhor das Centenas",
        "Guardiões de Ferro"
    )

    private val byKey = OFFICIAL.associateBy { normalizeFaction(it) }

    /** Grafia oficial, ignorando maiúsculas, minúsculas e espaços nas pontas; null se não for oficial. */
    fun official(faction: String): String? = byKey[normalizeFaction(faction)]
}
