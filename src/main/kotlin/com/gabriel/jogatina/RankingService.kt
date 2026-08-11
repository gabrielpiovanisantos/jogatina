package com.gabriel.jogatina

import com.gabriel.jogatina.dtos.PlayerRankingDTO
import com.gabriel.jogatina.models.GameResult
import com.gabriel.jogatina.models.Player
import com.gabriel.jogatina.repositories.PlayerRepository
import com.gabriel.jogatina.repositories.ResultRepository
import org.springframework.stereotype.Service

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
     * Obtém o ranking de um jogador específico
     */
    fun getPlayerRanking(playerId: Long): PlayerRankingDTO? {
        val player = playerRepository.findById(playerId).orElse(null) ?: return null
        val allResults = resultRepository.findAll().toList()
        val playerResults = allResults.filter { it.player?.id == playerId }

        return buildPlayerRanking(player, playerResults)
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
        val differentFactions = playerResults.map { it.faction }.distinct().size
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