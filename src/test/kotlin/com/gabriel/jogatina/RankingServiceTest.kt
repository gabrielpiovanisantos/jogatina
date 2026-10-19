package com.gabriel.jogatina

import com.gabriel.jogatina.models.Game
import com.gabriel.jogatina.models.GameResult
import com.gabriel.jogatina.models.Player
import com.gabriel.jogatina.repositories.PlayerRepository
import com.gabriel.jogatina.repositories.ResultRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Instant
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class RankingServiceTest {

    @Mock
    lateinit var playerRepository: PlayerRepository

    @Mock
    lateinit var resultRepository: ResultRepository

    private lateinit var rankingService: RankingService

    private val game = Game(id = 1, date = Instant.now())

    private fun result(player: Player, score: Int, faction: String, won: Boolean) =
        GameResult(game = game, player = player, score = score, faction = faction, won = won)

    @BeforeEach
    fun setUp() {
        rankingService = RankingService(playerRepository, resultRepository)
    }

    @Test
    fun `given played games when getting the ranking then orders by victories then score`() {
        val alice = Player(id = 1, name = "Alice")
        val bob = Player(id = 2, name = "Bob")

        `when`(playerRepository.findAll()).thenReturn(listOf(alice, bob))
        `when`(resultRepository.findAll()).thenReturn(
            listOf(
                result(alice, score = 10, faction = "A", won = true),
                result(alice, score = 20, faction = "B", won = true),
                result(bob, score = 5, faction = "A", won = true),
                result(bob, score = 5, faction = "A", won = false),
                result(bob, score = 5, faction = "B", won = false)
            )
        )

        val ranking = rankingService.getRanking()

        assertEquals(2, ranking.size)
        assertEquals("Alice", ranking[0].playerName)
        assertEquals(2, ranking[0].totalVictories)
        assertEquals(30, ranking[0].totalScore)
        assertEquals(100.0, ranking[0].winPercentage)
        assertEquals(2, ranking[0].differentFactions)

        assertEquals("Bob", ranking[1].playerName)
        assertEquals(1, ranking[1].totalVictories)
        assertEquals(3, ranking[1].totalGames)
        assertEquals(15, ranking[1].totalScore)
        assertEquals(2, ranking[1].differentFactions)
    }

    @Test
    fun `given a player with no games when getting the ranking then returns zeroed statistics`() {
        val carol = Player(id = 3, name = "Carol")

        `when`(playerRepository.findAll()).thenReturn(listOf(carol))
        `when`(resultRepository.findAll()).thenReturn(emptyList())

        val ranking = rankingService.getRanking()

        assertEquals(1, ranking.size)
        assertEquals(0, ranking[0].totalVictories)
        assertEquals(0, ranking[0].totalGames)
        assertEquals(0.0, ranking[0].winPercentage)
        assertEquals(0, ranking[0].differentFactions)
        assertEquals(0, ranking[0].totalScore)
    }

    @Test
    fun `given an existing player when getting a player ranking then returns that player statistics`() {
        val alice = Player(id = 1, name = "Alice")

        `when`(playerRepository.findById(1L)).thenReturn(Optional.of(alice))
        `when`(resultRepository.findAll()).thenReturn(
            listOf(result(alice, score = 10, faction = "A", won = true))
        )

        val ranking = rankingService.getPlayerRanking(1L)

        assertNotNull(ranking)
        assertEquals("Alice", ranking?.playerName)
        assertEquals(1, ranking?.totalVictories)
    }

    @Test
    fun `given an unknown player when getting a player ranking then returns null`() {
        `when`(playerRepository.findById(99L)).thenReturn(Optional.empty())

        val ranking = rankingService.getPlayerRanking(99L)

        assertNull(ranking)
    }
}
