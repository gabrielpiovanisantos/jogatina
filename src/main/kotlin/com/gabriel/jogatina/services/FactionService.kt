package com.gabriel.jogatina.services

import com.gabriel.jogatina.dtos.FactionDetailDTO
import com.gabriel.jogatina.dtos.FactionPlayerStatsDTO
import com.gabriel.jogatina.dtos.FactionStatsDTO
import com.gabriel.jogatina.models.GameResult
import com.gabriel.jogatina.repositories.ResultRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FactionService(
    private val resultRepository: ResultRepository
) {

    /**
     * Estatísticas por facção, ordenadas por porcentagem de vitórias, vitórias,
     * partidas e nome.
     *
     * A facção é gravada como o usuário digitou, então "Stark", "stark" e
     * " Stark " contam como a mesma. O nome exibido é a grafia mais usada.
     */
    @Transactional(readOnly = true)
    fun getFactions(): List<FactionStatsDTO> {
        return groupByFaction(resultRepository.findAll())
            .map { (name, results) -> buildStats(name, results) }
            .sortedWith(
                compareByDescending<FactionStatsDTO> { it.winPercentage }
                    .thenByDescending { it.totalVictories }
                    .thenByDescending { it.totalGames }
                    .thenBy { it.faction }
            )
    }

    /** Detalhe de uma facção (ignora maiúsculas e minúsculas), ou null se ninguém jogou com ela. */
    @Transactional(readOnly = true)
    fun getFaction(faction: String): FactionDetailDTO? {
        val key = normalize(faction)
        val results = resultRepository.findAll().filter { normalize(it.faction) == key }
        if (results.isEmpty()) return null

        val stats = buildStats(displayName(results), results)
        val players = results.groupBy { it.player!!.id }
            .map { (_, playerResults) ->
                val player = playerResults.first().player!!
                val victories = playerResults.count { it.won }
                val score = playerResults.sumOf { it.score }
                FactionPlayerStatsDTO(
                    playerId = player.id ?: 0,
                    playerName = player.name,
                    totalGames = playerResults.size,
                    totalVictories = victories,
                    winPercentage = percentage(victories, playerResults.size),
                    totalScore = score,
                    averageScore = score.toDouble() / playerResults.size
                )
            }
            .sortedWith(
                compareByDescending<FactionPlayerStatsDTO> { it.winPercentage }
                    .thenByDescending { it.totalVictories }
                    .thenByDescending { it.totalGames }
                    .thenBy { it.playerName }
            )

        return FactionDetailDTO(
            faction = stats.faction,
            totalGames = stats.totalGames,
            totalVictories = stats.totalVictories,
            winPercentage = stats.winPercentage,
            totalScore = stats.totalScore,
            averageScore = stats.averageScore,
            distinctPlayers = stats.distinctPlayers,
            players = players
        )
    }

    private fun groupByFaction(results: List<GameResult>): Map<String, List<GameResult>> =
        results.filter { it.faction.isNotBlank() }
            .groupBy { normalize(it.faction) }
            .values
            .associateBy { displayName(it) }

    private fun buildStats(name: String, results: List<GameResult>): FactionStatsDTO {
        val victories = results.count { it.won }
        val score = results.sumOf { it.score }
        return FactionStatsDTO(
            faction = name,
            totalGames = results.size,
            totalVictories = victories,
            winPercentage = percentage(victories, results.size),
            totalScore = score,
            averageScore = score.toDouble() / results.size,
            distinctPlayers = results.mapNotNull { it.player?.id }.distinct().size
        )
    }

    /** Grafia mais usada; no empate, a primeira em ordem alfabética. */
    private fun displayName(results: List<GameResult>): String =
        results.map { it.faction.trim() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .first()
            .key

    private fun normalize(faction: String) = faction.trim().lowercase()

    private fun percentage(victories: Int, games: Int) = (victories.toDouble() / games) * 100
}
