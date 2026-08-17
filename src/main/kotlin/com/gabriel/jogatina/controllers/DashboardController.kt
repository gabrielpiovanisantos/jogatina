package com.gabriel.jogatina.controllers

import com.gabriel.jogatina.RankingService
import com.gabriel.jogatina.dtos.PlayerRankingDTO
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/dashboard")
class DashboardController(
    private val rankingService: RankingService
) {
    @GetMapping("/ranking")
    fun getRanking(): List<PlayerRankingDTO> {
        return rankingService.getRanking()
    }

    @GetMapping("/ranking/{playerId}")
    fun getPlayerRanking(@PathVariable playerId: Long): PlayerRankingDTO? {
        return rankingService.getPlayerRanking(playerId)
    }
}