package com.gabriel.jogatina.controllers

import com.gabriel.jogatina.RankingService
import com.gabriel.jogatina.dtos.PlayerRankingDTO
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class DashboardControllerTest {

    @Mock
    lateinit var rankingService: RankingService

    @Test
    fun testGetRankingReturnsServiceResult() {
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
    fun testGetPlayerRankingReturnsServiceResult() {
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

        assertEquals(playerRanking, result)
    }

    @Test
    fun testGetPlayerRankingReturnsNullForUnknownPlayer() {
        val controller = DashboardController(rankingService)
        `when`(rankingService.getPlayerRanking(99L)).thenReturn(null)

        val result = controller.getPlayerRanking(99L)

        assertNull(result)
    }
}
