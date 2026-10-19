package com.gabriel.jogatina.services

import com.gabriel.jogatina.controllers.FinishGameResultDTO
import com.gabriel.jogatina.models.Game
import com.gabriel.jogatina.models.GameResult
import com.gabriel.jogatina.repositories.GameRepository
import com.gabriel.jogatina.repositories.PlayerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class GameService(
    private val gameRepository: GameRepository,
    private val playerRepository: PlayerRepository
) {

    @Transactional
    fun finish(dateTime: Instant, results: List<FinishGameResultDTO>): Game {
        require(results.isNotEmpty()) { "A partida precisa ter pelo menos um resultado" }
        require(results.all { it.faction.isNotBlank() }) { "Facção não pode ser vazia" }
        require(results.all { it.points >= 0 }) { "Pontuação não pode ser negativa" }

        val playerIds = results.map { it.playerId }
        require(playerIds.size == playerIds.distinct().size) { "Jogador repetido na mesma partida" }

        val players = playerRepository.findAllById(playerIds).associateBy { it.id }
        val missing = playerIds.filterNot { it in players }
        require(missing.isEmpty()) { "Jogador(es) não encontrado(s): ${missing.joinToString()}" }

        val game = Game(date = dateTime)
        results.forEach {
            game.gameResults.add(
                GameResult(
                    game = game,
                    player = players.getValue(it.playerId),
                    score = it.points,
                    faction = it.faction,
                    won = it.won
                )
            )
        }
        return gameRepository.save(game)
    }
}
