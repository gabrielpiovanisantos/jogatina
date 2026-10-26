package com.gabriel.jogatina.controllers

import com.gabriel.jogatina.dtos.FactionDetailDTO
import com.gabriel.jogatina.dtos.FactionStatsDTO
import com.gabriel.jogatina.services.FactionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/dashboard/factions")
class FactionDashboardController(
    private val factionService: FactionService
) {
    @GetMapping
    fun getFactions(): List<FactionStatsDTO> {
        return factionService.getFactions()
    }

    @GetMapping("/{faction}")
    fun getFaction(@PathVariable faction: String): ResponseEntity<FactionDetailDTO> {
        val detail = factionService.getFaction(faction) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(detail)
    }
}
