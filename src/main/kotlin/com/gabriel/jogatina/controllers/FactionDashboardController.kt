package com.gabriel.jogatina.controllers

import com.gabriel.jogatina.dtos.FactionDetailDTO
import com.gabriel.jogatina.dtos.FactionStatsDTO
import com.gabriel.jogatina.services.FactionService
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
@RequestMapping("/dashboard/factions")
class FactionDashboardController(
    private val factionService: FactionService
) {
    @GetMapping
    fun getFactions(
        @RequestParam(required = false) from: Instant? = null,
        @RequestParam(required = false) to: Instant? = null
    ): List<FactionStatsDTO> {
        return factionService.getFactions(Period(from, to))
    }

    @GetMapping("/{faction}")
    fun getFaction(
        @PathVariable faction: String,
        @RequestParam(required = false) from: Instant? = null,
        @RequestParam(required = false) to: Instant? = null
    ): ResponseEntity<FactionDetailDTO> {
        val detail = factionService.getFaction(faction, Period(from, to)) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(detail)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun badRequest(e: IllegalArgumentException): ResponseEntity<String> =
        ResponseEntity.badRequest()
            .contentType(MediaType("text", "plain", Charsets.UTF_8))
            .body(e.message ?: "Entrada inválida")
}
