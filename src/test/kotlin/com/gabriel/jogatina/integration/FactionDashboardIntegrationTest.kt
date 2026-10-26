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
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.Instant

/**
 * GET /dashboard/factions e GET /dashboard/factions/{faction}. A facção é
 * gravada como o usuário digitou, então a comparação ignora maiúsculas,
 * minúsculas e espaços nas pontas.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FactionDashboardIntegrationTest {

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

    private data class Resultado(val jogador: Player, val faccao: String, val pontos: Int, val venceu: Boolean)

    @BeforeEach
    fun setUp() {
        client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()
        jdbcTemplate.execute("TRUNCATE games, game_results, players RESTART IDENTITY CASCADE")
    }

    private fun partida(data: String, vararg resultados: Resultado) {
        val game = Game(date = Instant.parse(data))
        resultados.forEach {
            game.gameResults.add(
                GameResult(game = game, player = it.jogador, score = it.pontos, faction = it.faccao, won = it.venceu)
            )
        }
        gameRepository.save(game)
    }

    private fun cenario(): Map<String, Player> {
        val jogadores = listOf("Ana", "Bruno", "Carla").associateWith { playerRepository.save(Player(name = it)) }
        val ana = jogadores.getValue("Ana")
        val bruno = jogadores.getValue("Bruno")
        val carla = jogadores.getValue("Carla")

        partida(
            "2026-09-01T20:00:00Z",
            Resultado(ana, "Marqueses", 30, true),
            Resultado(bruno, "Malandro", 20, false),
            Resultado(carla, "Dinastia das Rapinas", 10, false)
        )
        partida(
            "2026-09-08T20:00:00Z",
            Resultado(ana, "Malandro", 25, true),
            Resultado(bruno, "marqueses", 22, false),
            Resultado(carla, "Dinastia das Rapinas", 18, false)
        )
        partida(
            "2026-09-15T20:00:00Z",
            Resultado(ana, "Dinastia das Rapinas", 15, false),
            Resultado(bruno, " Marqueses ", 28, true),
            Resultado(carla, "Malandro", 12, false)
        )
        return jogadores
    }

    @Test
    fun `given games with several factions when listing factions then groups ignoring case and orders by win percentage`() {
        cenario()

        // Marqueses: 3 partidas, 2 vitórias (66,7%). Malandro: 3 partidas, 1 vitória (33,3%).
        // Dinastia das Rapinas: 3 partidas, 0 vitórias.
        client.get().uri("/dashboard/factions").exchange()
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.length()").isEqualTo(3)
            .jsonPath("$[*].faction").isEqualTo(listOf("Marqueses", "Malandro", "Dinastia das Rapinas"))
            .jsonPath("$[0].totalGames").isEqualTo(3)
            .jsonPath("$[0].totalVictories").isEqualTo(2)
            .jsonPath("$[0].winPercentage").isEqualTo((2.0 / 3) * 100)
            .jsonPath("$[0].totalScore").isEqualTo(80)
            .jsonPath("$[0].averageScore").isEqualTo(80.0 / 3)
            .jsonPath("$[0].distinctPlayers").isEqualTo(2)
            .jsonPath("$[2].totalVictories").isEqualTo(0)
            .jsonPath("$[2].winPercentage").isEqualTo(0.0)
            .jsonPath("$[2].distinctPlayers").isEqualTo(2)
    }

    @Test
    fun `given no games when listing factions then returns an empty list`() {
        client.get().uri("/dashboard/factions").exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.length()").isEqualTo(0)
    }

    @Test
    fun `given a faction in another case when getting its detail then returns stats and each player's performance`() {
        val jogadores = cenario()

        client.get().uri("/dashboard/factions/{faction}", "MARQUESES").exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.faction").isEqualTo("Marqueses")
            .jsonPath("$.totalGames").isEqualTo(3)
            .jsonPath("$.totalVictories").isEqualTo(2)
            .jsonPath("$.totalScore").isEqualTo(80)
            .jsonPath("$.distinctPlayers").isEqualTo(2)
            .jsonPath("$.players.length()").isEqualTo(2)
            // Ana: 1 de 1 (100%). Bruno: 1 de 2 (50%).
            .jsonPath("$.players[0].playerId").isEqualTo(jogadores.getValue("Ana").id!!.toInt())
            .jsonPath("$.players[0].playerName").isEqualTo("Ana")
            .jsonPath("$.players[0].totalGames").isEqualTo(1)
            .jsonPath("$.players[0].winPercentage").isEqualTo(100.0)
            .jsonPath("$.players[0].averageScore").isEqualTo(30.0)
            .jsonPath("$.players[1].playerName").isEqualTo("Bruno")
            .jsonPath("$.players[1].totalGames").isEqualTo(2)
            .jsonPath("$.players[1].totalVictories").isEqualTo(1)
            .jsonPath("$.players[1].winPercentage").isEqualTo(50.0)
            .jsonPath("$.players[1].totalScore").isEqualTo(50)
            .jsonPath("$.players[1].averageScore").isEqualTo(25.0)
    }

    @Test
    fun `given an accented faction when getting its detail in lower case then finds it`() {
        val ana = playerRepository.save(Player(name = "Ana"))
        partida("2026-09-01T20:00:00Z", Resultado(ana, "Conspiração Corvídea", 20, true))

        client.get().uri("/dashboard/factions/{faction}", "conspiração corvídea").exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.faction").isEqualTo("Conspiração Corvídea")
            .jsonPath("$.players[0].playerName").isEqualTo("Ana")
    }

    @Test
    fun `given an unknown faction when getting its detail then responds 404`() {
        cenario()

        client.get().uri("/dashboard/factions/{faction}", "Lagartos Cultistas").exchange()
            .expectStatus().isNotFound
            .expectBody().isEmpty
    }
}
