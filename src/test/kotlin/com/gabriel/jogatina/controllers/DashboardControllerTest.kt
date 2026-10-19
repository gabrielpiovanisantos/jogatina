package com.gabriel.jogatina.controllers

import com.gabriel.jogatina.RankingService
import com.gabriel.jogatina.dtos.PlayerRankingDTO
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatus

@ExtendWith(MockitoExtension::class)
class DashboardControllerTest {

    @Mock
    lateinit var rankingService: RankingService

    @Test
    fun `given a service ranking when getting the ranking then returns it`() {
        val controller = DashboardController(rankingService)
        val ranking = listOf(
            PlayerRankingDTO(
                playerId = 1,
                playerName = "Alice",
                totalVictories = 2,
                totalGames = 2,
                winPercentage = 100.0,
                differentFactions = 2,
                totalScore = 30
            )
        )
        `when`(rankingService.getRanking()).thenReturn(ranking)

        val result = controller.getRanking()

        assertEquals(ranking, result)
    }

    @Test
    fun `given a service player ranking when getting a player ranking then returns it`() {
        val controller = DashboardController(rankingService)
        val playerRanking = PlayerRankingDTO(
            playerId = 1,
            playerName = "Alice",
            totalVictories = 2,
            totalGames = 2,
            winPercentage = 100.0,
            differentFactions = 2,
            totalScore = 30
        )
        `when`(rankingService.getPlayerRanking(1L)).thenReturn(playerRanking)

        val result = controller.getPlayerRanking(1L)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(playerRanking, result.body)
    }

    @Test
    fun `given an unknown player when getting a player ranking then responds 404`() {
        val controller = DashboardController(rankingService)
        `when`(rankingService.getPlayerRanking(99L)).thenReturn(null)

        val result = controller.getPlayerRanking(99L)

        assertEquals(HttpStatus.NOT_FOUND, result.statusCode)
        assertNull(result.body)
    }
}
