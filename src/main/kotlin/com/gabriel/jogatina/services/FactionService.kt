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
     * Facções que diferem só em maiúsculas ou espaços contam como a mesma
     * (ver [groupByFaction]). Só contam as partidas do período.
     */
    @Transactional(readOnly = true)
    fun getFactions(period: Period = Period()): List<FactionStatsDTO> {
        return groupByFaction(resultRepository.findInPeriod(period.from, period.to))
            .map { (name, results) -> buildStats(name, results) }
            .sortedWith(
                compareByDescending<FactionStatsDTO> { it.winPercentage }
                    .thenByDescending { it.totalVictories }
                    .thenByDescending { it.totalGames }
                    .thenBy { it.faction }
            )
    }

    /** Detalhe de uma facção (ignora maiúsculas e minúsculas), ou null se ninguém jogou com ela no período. */
    @Transactional(readOnly = true)
    fun getFaction(faction: String, period: Period = Period()): FactionDetailDTO? {
        val key = normalizeFaction(faction)
        val results = resultRepository.findInPeriod(period.from, period.to).filter { normalizeFaction(it.faction) == key }
        if (results.isEmpty()) return null

        val stats = buildStats(factionDisplayName(results), results)
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

    private fun percentage(victories: Int, games: Int) = (victories.toDouble() / games) * 100
}
