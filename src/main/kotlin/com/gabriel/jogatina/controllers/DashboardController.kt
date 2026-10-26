package com.gabriel.jogatina.controllers

import com.gabriel.jogatina.RankingService
import com.gabriel.jogatina.dtos.PlayerRankingDTO
import com.gabriel.jogatina.dtos.PlayerRankingDetailDTO
import com.gabriel.jogatina.services.Period
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

// from e to são opcionais e têm a mesma semântica do GET /api/games: [from, to).
@RestController
@RequestMapping("/dashboard")
class DashboardController(
    private val rankingService: RankingService
) {
    @GetMapping("/ranking")
    fun getRanking(
        @RequestParam(required = false) from: Instant? = null,
        @RequestParam(required = false) to: Instant? = null
    ): List<PlayerRankingDTO> {
        return rankingService.getRanking(Period(from, to))
    }

    @GetMapping("/ranking/{playerId}")
    fun getPlayerRanking(
        @PathVariable playerId: Long,
        @RequestParam(required = false) from: Instant? = null,
        @RequestParam(required = false) to: Instant? = null
    ): ResponseEntity<PlayerRankingDetailDTO> {
        val ranking = rankingService.getPlayerRanking(playerId, Period(from, to))
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(ranking)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun badRequest(e: IllegalArgumentException): ResponseEntity<String> =
        ResponseEntity.badRequest()
            .contentType(MediaType("text", "plain", Charsets.UTF_8))
            .body(e.message ?: "Entrada inválida")
}
