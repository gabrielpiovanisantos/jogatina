package com.gabriel.jogatina

import com.gabriel.jogatina.dtos.PlayerFactionStatsDTO
import com.gabriel.jogatina.dtos.PlayerRankingDTO
import com.gabriel.jogatina.dtos.PlayerRankingDetailDTO
import com.gabriel.jogatina.models.GameResult
import com.gabriel.jogatina.models.Player
import com.gabriel.jogatina.repositories.PlayerRepository
import com.gabriel.jogatina.repositories.ResultRepository
import com.gabriel.jogatina.services.groupByFaction
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RankingService(
    private val playerRepository: PlayerRepository,
    private val resultRepository: ResultRepository
) {

    /**
     * Calcula o ranking de todos os jogadores baseado em:
     * 1. Número de vitórias
     * 2. Pontuação total
     * 3. Porcentagem de vitórias
     * 4. Quantidade de facções diferentes que jogou
     *
     * Ordenado por número de vitórias (decrescente)
     */
    fun getRanking(): List<PlayerRankingDTO> {
        val allPlayers = playerRepository.findAll().toList()
        val allResults = resultRepository.findAll().toList()

        return allPlayers.map { player ->
            val playerResults = allResults.filter { it.player?.id == player.id }
            buildPlayerRanking(player, playerResults)
        }.sortedWith(
            compareBy<PlayerRankingDTO> { -it.totalVictories }
                .thenBy { -it.totalScore }
                .thenBy { -it.winPercentage }
                .thenBy { -it.differentFactions }
        )
    }

    /**
     * Obtém o ranking de um jogador específico, com o desempenho em cada facção,
     * na mesma ordem dos dashboards de facção.
     */
    @Transactional(readOnly = true)
    fun getPlayerRanking(playerId: Long): PlayerRankingDetailDTO? {
        val player = playerRepository.findById(playerId).orElse(null) ?: return null
        val allResults = resultRepository.findAll().toList()
        val playerResults = allResults.filter { it.player?.id == playerId }

        val ranking = buildPlayerRanking(player, playerResults)
        val factions = groupByFaction(playerResults)
            .map { (faction, results) ->
                val victories = results.count { it.won }
                val score = results.sumOf { it.score }
                PlayerFactionStatsDTO(
                    faction = faction,
                    totalGames = results.size,
                    totalVictories = victories,
                    winPercentage = (victories.toDouble() / results.size) * 100,
                    totalScore = score,
                    averageScore = score.toDouble() / results.size
                )
            }
            .sortedWith(
                compareByDescending<PlayerFactionStatsDTO> { it.winPercentage }
                    .thenByDescending { it.totalVictories }
                    .thenByDescending { it.totalGames }
                    .thenBy { it.faction }
            )

        return PlayerRankingDetailDTO(
            playerId = ranking.playerId,
            playerName = ranking.playerName,
            totalVictories = ranking.totalVictories,
            totalGames = ranking.totalGames,
            winPercentage = ranking.winPercentage,
            differentFactions = ranking.differentFactions,
            totalScore = ranking.totalScore,
            factions = factions
        )
    }

    /**
     * Constrói o DTO de ranking para um jogador baseado em seus resultados
     */
    private fun buildPlayerRanking(player: Player, playerResults: List<GameResult>): PlayerRankingDTO {
        val totalVictories = playerResults.count { it.won }
        val totalGames = playerResults.size
        val winPercentage = if (totalGames > 0) {
            (totalVictories.toDouble() / totalGames) * 100
        } else {
            0.0
        }
        val differentFactions = groupByFaction(playerResults).size
        val totalScore = playerResults.sumOf { it.score }

        return PlayerRankingDTO(
            playerId = player.id ?: 0,
            playerName = player.name,
            totalVictories = totalVictories,
            totalGames = totalGames,
            winPercentage = winPercentage,
            differentFactions = differentFactions,
            totalScore = totalScore
        )
    }
}