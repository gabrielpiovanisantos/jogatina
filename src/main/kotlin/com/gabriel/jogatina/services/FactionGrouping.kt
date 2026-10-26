package com.gabriel.jogatina.services

import com.gabriel.jogatina.models.GameResult

// A facção é gravada como o usuário digitou, então "Marqueses", "marqueses" e
// " Marqueses " contam como a mesma.

fun normalizeFaction(faction: String) = faction.trim().lowercase()

/** Agrupa os resultados por facção; a chave é a grafia exibida (ver [factionDisplayName]). */
fun groupByFaction(results: List<GameResult>): Map<String, List<GameResult>> =
    results.filter { it.faction.isNotBlank() }
        .groupBy { normalizeFaction(it.faction) }
        .values
        .associateBy { factionDisplayName(it) }

/** Grafia mais usada; no empate, a primeira em ordem alfabética. */
fun factionDisplayName(results: List<GameResult>): String =
    results.map { it.faction.trim() }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .first()
        .key
