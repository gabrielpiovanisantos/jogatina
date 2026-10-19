package com.gabriel.jogatina.controllers

import com.gabriel.jogatina.services.GameService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

data class FinishGameResultDTO(
    val playerId: Long,
    val faction: String,
    val points: Int,
    val won: Boolean
)

data class FinishGameRequest(
    val dateTime: Instant,
    val results: List<FinishGameResultDTO>
)

data class FinishGameResponse(
    val gameId: Long
)

@RestController
@RequestMapping("/api/games")
class GameController(private val gameService: GameService) {

    @PostMapping("/finish")
    fun finish(@RequestBody request: FinishGameRequest): ResponseEntity<Any> {
        return try {
            val game = gameService.finish(request.dateTime, request.results)
            ResponseEntity.status(HttpStatus.CREATED).body(FinishGameResponse(gameId = game.id!!))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(e.message ?: "Entrada inválida")
        }
    }
}
