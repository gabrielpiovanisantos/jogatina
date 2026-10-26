package com.gabriel.jogatina.services

import com.gabriel.jogatina.controllers.FinishGameResultDTO
import com.gabriel.jogatina.models.Game
import com.gabriel.jogatina.models.GameResult
import com.gabriel.jogatina.repositories.GameRepository
import com.gabriel.jogatina.repositories.PlayerRepository
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Instant

@Service
class GameService(
    private val gameRepository: GameRepository,
    private val playerRepository: PlayerRepository,
    meterRegistry: MeterRegistry
) {

    /** Motivos de recusa do finish, usados como tag da métrica. */
    enum class RejectionReason(val tag: String) {
        NO_RESULTS("no_results"),
        BLANK_FACTION("blank_faction"),
        INVALID_FACTION("invalid_faction"),
        NEGATIVE_SCORE("negative_score"),
        DUPLICATE_PLAYER("duplicate_player"),
        UNKNOWN_PLAYER("unknown_player")
    }

    private val finished = Counter.builder("jogatina.games.finished")
        .description("Partidas registradas pelo finish")
        .register(meterRegistry)

    // Registrados de saída, para cada motivo aparecer com 0 no Prometheus.
    private val rejected = RejectionReason.entries.associateWith {
        Counter.builder("jogatina.games.finish.rejected")
            .description("Finish recusado por validação")
            .tag("reason", it.tag)
            .register(meterRegistry)
    }

    private fun validate(condition: Boolean, reason: RejectionReason, message: () -> String) {
        if (!condition) {
            rejected.getValue(reason).increment()
            throw IllegalArgumentException(message())
        }
    }

    /** Partidas com data em [from, to), da mais antiga para a mais recente. */
    @Transactional(readOnly = true)
    fun listBetween(from: Instant, to: Instant): List<Game> {
        require(from.isBefore(to)) { "'from' precisa ser anterior a 'to'" }
        return gameRepository.findByDateGreaterThanEqualAndDateLessThanOrderByDateAsc(from, to)
    }

    @Transactional
    fun finish(dateTime: Instant, results: List<FinishGameResultDTO>): Game {
        validate(results.isNotEmpty(), RejectionReason.NO_RESULTS) { "A partida precisa ter pelo menos um resultado" }
        validate(results.all { it.faction.isNotBlank() }, RejectionReason.BLANK_FACTION) { "Facção não pode ser vazia" }
        val invalid = results.map { it.faction.trim() }.filter { Factions.official(it) == null }.distinctBy { normalizeFaction(it) }
        validate(invalid.isEmpty(), RejectionReason.INVALID_FACTION) { "Facção inválida: ${invalid.joinToString()}" }
        validate(results.all { it.points >= 0 }, RejectionReason.NEGATIVE_SCORE) { "Pontuação não pode ser negativa" }

        val playerIds = results.map { it.playerId }
        validate(playerIds.size == playerIds.distinct().size, RejectionReason.DUPLICATE_PLAYER) { "Jogador repetido na mesma partida" }

        val players = playerRepository.findAllById(playerIds).associateBy { it.id }
        val missing = playerIds.filterNot { it in players }
        validate(missing.isEmpty(), RejectionReason.UNKNOWN_PLAYER) { "Jogador(es) não encontrado(s): ${missing.joinToString()}" }

        val game = Game(date = dateTime)
        results.forEach {
            game.gameResults.add(
                GameResult(
                    game = game,
                    player = players.getValue(it.playerId),
                    score = it.points,
                    faction = Factions.official(it.faction)!!,
                    won = it.won
                )
            )
        }
        val saved = gameRepository.save(game)
        // Conta só depois do commit, para um rollback não inflar a métrica.
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() = finished.increment()
        })
        return saved
    }
}
