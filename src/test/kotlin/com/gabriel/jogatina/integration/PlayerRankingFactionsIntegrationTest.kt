package com.gabriel.jogatina.integration

import com.gabriel.jogatina.models.Game
import com.gabriel.jogatina.models.GameResult
import com.gabriel.jogatina.models.Player
import com.gabriel.jogatina.repositories.GameRepository
import com.gabriel.jogatina.repositories.PlayerRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.Instant

/**
 * "factions" de GET /dashboard/ranking/{playerId}: com quais facções o jogador
 * jogou e com quais ganhou. Agrupa as facções como os dashboards de facção.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PlayerRankingFactionsIntegrationTest {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer(
            DockerImageName.parse("docker.io/library/postgres:17-alpine").asCompatibleSubstituteFor("postgres")
        )
    }

    @LocalServerPort
    var port: Int = 0

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var playerRepository: PlayerRepository

    @Autowired
    lateinit var gameRepository: GameRepository

    private lateinit var client: WebTestClient

    @BeforeEach
    fun setUp() {
        client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()
        jdbcTemplate.execute("TRUNCATE games, game_results, players RESTART IDENTITY CASCADE")
    }

    private fun partida(data: String, vararg resultados: Triple<Player, String, Pair<Int, Boolean>>) {
        val game = Game(date = Instant.parse(data))
        resultados.forEach { (jogador, faccao, resultado) ->
            game.gameResults.add(
                GameResult(game = game, player = jogador, score = resultado.first, faction = faccao, won = resultado.second)
            )
        }
        gameRepository.save(game)
    }

    @Test
    fun `given one faction written in different cases when getting a player ranking then groups it and orders factions by win percentage`() {
        val ana = playerRepository.save(Player(name = "Ana"))
        val bruno = playerRepository.save(Player(name = "Bruno"))

        partida("2026-09-01T20:00:00Z", Triple(ana, "Marqueses", 30 to true), Triple(bruno, "Malandro", 20 to false))
        partida("2026-09-08T20:00:00Z", Triple(ana, " marqueses ", 20 to false), Triple(bruno, "Marqueses", 25 to true))
        partida("2026-09-15T20:00:00Z", Triple(ana, "Aliança da Floresta", 28 to true), Triple(bruno, "Malandro", 10 to false))
        partida("2026-09-22T20:00:00Z", Triple(ana, "Malandro", 12 to false), Triple(bruno, "Marqueses", 30 to true))

        // Aliança: 1 de 1 (100%). Marqueses: 1 de 2 (50%). Malandro: 0 de 1.
        client.get().uri("/dashboard/ranking/{id}", ana.id).exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.playerName").isEqualTo("Ana")
            .jsonPath("$.totalGames").isEqualTo(4)
            .jsonPath("$.totalVictories").isEqualTo(2)
            .jsonPath("$.differentFactions").isEqualTo(3)
            .jsonPath("$.factions.length()").isEqualTo(3)
            .jsonPath("$.factions[*].faction").isEqualTo(listOf("Aliança da Floresta", "Marqueses", "Malandro"))
            .jsonPath("$.factions[1].totalGames").isEqualTo(2)
            .jsonPath("$.factions[1].totalVictories").isEqualTo(1)
            .jsonPath("$.factions[1].winPercentage").isEqualTo(50.0)
            .jsonPath("$.factions[1].totalScore").isEqualTo(50)
            .jsonPath("$.factions[1].averageScore").isEqualTo(25.0)
            .jsonPath("$.factions[2].totalVictories").isEqualTo(0)
            .jsonPath("$.factions[2].winPercentage").isEqualTo(0.0)

        // A lista usa o mesmo agrupamento para differentFactions e não traz "factions".
        client.get().uri("/dashboard/ranking").exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$[?(@.playerName == 'Ana')].differentFactions").isEqualTo(listOf(3))
            .jsonPath("$[0].factions").doesNotExist()
    }

    @Test
    fun `given equal win percentages when getting a player ranking then breaks ties by victories, games and name`() {
        val ana = playerRepository.save(Player(name = "Ana"))

        partida("2026-09-01T20:00:00Z", Triple(ana, "Malandro", 10 to true))
        partida("2026-09-02T20:00:00Z", Triple(ana, "Marqueses", 10 to true))
        partida("2026-09-03T20:00:00Z", Triple(ana, "Marqueses", 10 to true))
        partida("2026-09-04T20:00:00Z", Triple(ana, "Lagartos Cultistas", 10 to true))

        client.get().uri("/dashboard/ranking/{id}", ana.id).exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.factions[*].faction").isEqualTo(listOf("Marqueses", "Lagartos Cultistas", "Malandro"))
    }

    @Test
    fun `given a player with no games when getting a player ranking then returns an empty factions list`() {
        val carla = playerRepository.save(Player(name = "Carla"))

        client.get().uri("/dashboard/ranking/{id}", carla.id).exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.differentFactions").isEqualTo(0)
            .jsonPath("$.factions.length()").isEqualTo(0)
    }
}
