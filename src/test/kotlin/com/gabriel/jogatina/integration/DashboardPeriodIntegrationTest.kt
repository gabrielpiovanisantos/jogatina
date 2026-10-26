package com.gabriel.jogatina.integration

import com.gabriel.jogatina.models.Game
import com.gabriel.jogatina.models.GameResult
import com.gabriel.jogatina.models.Player
import com.gabriel.jogatina.repositories.GameRepository
import com.gabriel.jogatina.repositories.PlayerRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
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
 * Filtro de período (from e to opcionais, intervalo [from, to) sobre a data da
 * partida) nos dashboards de ranking e de facções.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DashboardPeriodIntegrationTest {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer(
            DockerImageName.parse("docker.io/library/postgres:17-alpine").asCompatibleSubstituteFor("postgres")
        )

        const val SETEMBRO = "2026-09-01T03:00:00Z"
        const val OUTUBRO = "2026-10-01T03:00:00Z"
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

    private lateinit var ana: Player
    private lateinit var bruno: Player
    private lateinit var carla: Player

    private data class Resultado(val jogador: Player, val faccao: String, val pontos: Int, val venceu: Boolean)

    private fun partida(data: String, vararg resultados: Resultado) {
        val game = Game(date = Instant.parse(data))
        resultados.forEach {
            game.gameResults.add(
                GameResult(game = game, player = it.jogador, score = it.pontos, faction = it.faccao, won = it.venceu)
            )
        }
        gameRepository.save(game)
    }

    /**
     * Agosto: Ana vence de Marqueses contra Bruno de Malandro.
     * Setembro (a primeira exatamente no início do mês): Bruno vence duas de Marqueses.
     * Outubro (exatamente no início do mês, então fora de setembro): Ana vence de Malandro.
     * Carla nunca joga.
     */
    @BeforeEach
    fun setUp() {
        client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()
        jdbcTemplate.execute("TRUNCATE games, game_results, players RESTART IDENTITY CASCADE")

        ana = playerRepository.save(Player(name = "Ana"))
        bruno = playerRepository.save(Player(name = "Bruno"))
        carla = playerRepository.save(Player(name = "Carla"))

        partida("2026-08-15T20:00:00Z", Resultado(ana, "Marqueses", 30, true), Resultado(bruno, "Malandro", 20, false))
        partida(SETEMBRO, Resultado(bruno, "Marqueses", 25, true), Resultado(ana, "Malandro", 15, false))
        partida("2026-09-20T20:00:00Z", Resultado(bruno, "Marqueses", 35, true), Resultado(ana, "Malandro", 10, false))
        partida(OUTUBRO, Resultado(ana, "Malandro", 40, true), Resultado(bruno, "Marqueses", 5, false))
    }

    private fun get(path: String, from: String? = null, to: String? = null, vararg vars: Any) = client.get()
        .uri { b ->
            b.path(path)
            from?.let { b.queryParam("from", it) }
            to?.let { b.queryParam("to", it) }
            b.build(*vars)
        }
        .exchange()

    @Test
    fun `given a period when getting the ranking then counts only its games and keeps players who did not play zeroed`() {
        get("/dashboard/ranking", SETEMBRO, OUTUBRO)
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$[*].playerName").isEqualTo(listOf("Bruno", "Ana", "Carla"))
            .jsonPath("$[0].totalGames").isEqualTo(2)
            .jsonPath("$[0].totalVictories").isEqualTo(2)
            .jsonPath("$[0].totalScore").isEqualTo(60)
            .jsonPath("$[0].differentFactions").isEqualTo(1)
            .jsonPath("$[1].totalGames").isEqualTo(2)
            .jsonPath("$[1].totalVictories").isEqualTo(0)
            .jsonPath("$[1].winPercentage").isEqualTo(0.0)
            .jsonPath("$[2].totalGames").isEqualTo(0)
    }

    @Test
    fun `given no period when getting the ranking then counts every game`() {
        get("/dashboard/ranking")
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$[*].playerName").isEqualTo(listOf("Ana", "Bruno", "Carla"))
            .jsonPath("$[0].totalGames").isEqualTo(4)
            .jsonPath("$[0].totalVictories").isEqualTo(2)
            .jsonPath("$[1].totalGames").isEqualTo(4)
    }

    @Test
    fun `given only from or only to when getting the ranking then leaves the other bound open`() {
        // A partida de outubro, exatamente em 'from', entra.
        get("/dashboard/ranking", from = OUTUBRO)
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$[0].playerName").isEqualTo("Ana")
            .jsonPath("$[0].totalGames").isEqualTo(1)
            .jsonPath("$[0].totalScore").isEqualTo(40)

        // A partida do início de setembro, exatamente em 'to', fica de fora.
        get("/dashboard/ranking", to = SETEMBRO)
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$[0].playerName").isEqualTo("Ana")
            .jsonPath("$[0].totalGames").isEqualTo(1)
            .jsonPath("$[1].playerName").isEqualTo("Bruno")
            .jsonPath("$[1].totalGames").isEqualTo(1)
            .jsonPath("$[1].totalVictories").isEqualTo(0)
    }

    @Test
    fun `given a period when getting a player ranking then counts only its games including factions`() {
        get("/dashboard/ranking/{id}", SETEMBRO, OUTUBRO, vars = arrayOf(ana.id!!))
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.totalGames").isEqualTo(2)
            .jsonPath("$.totalVictories").isEqualTo(0)
            .jsonPath("$.totalScore").isEqualTo(25)
            .jsonPath("$.differentFactions").isEqualTo(1)
            .jsonPath("$.factions[*].faction").isEqualTo(listOf("Malandro"))
            .jsonPath("$.factions[0].averageScore").isEqualTo(12.5)
    }

    @Test
    fun `given a player without games in the period when getting a player ranking then returns zeros and no factions`() {
        get("/dashboard/ranking/{id}", from = "2026-11-01T03:00:00Z", vars = arrayOf(ana.id!!))
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.playerName").isEqualTo("Ana")
            .jsonPath("$.totalGames").isEqualTo(0)
            .jsonPath("$.totalVictories").isEqualTo(0)
            .jsonPath("$.winPercentage").isEqualTo(0.0)
            .jsonPath("$.differentFactions").isEqualTo(0)
            .jsonPath("$.totalScore").isEqualTo(0)
            .jsonPath("$.factions.length()").isEqualTo(0)
    }

    @Test
    fun `given an unknown player and a period when getting a player ranking then responds 404`() {
        get("/dashboard/ranking/{id}", SETEMBRO, OUTUBRO, vars = arrayOf(999))
            .expectStatus().isNotFound
            .expectBody().isEmpty
    }

    @Test
    fun `given a period when listing factions then counts only its games`() {
        // Setembro: Marqueses 2 de 2 (Bruno), Malandro 0 de 2 (Ana).
        get("/dashboard/factions", SETEMBRO, OUTUBRO)
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$[*].faction").isEqualTo(listOf("Marqueses", "Malandro"))
            .jsonPath("$[0].totalGames").isEqualTo(2)
            .jsonPath("$[0].totalVictories").isEqualTo(2)
            .jsonPath("$[0].distinctPlayers").isEqualTo(1)
            .jsonPath("$[0].averageScore").isEqualTo(30.0)
            .jsonPath("$[1].winPercentage").isEqualTo(0.0)
    }

    @Test
    fun `given a period without games when listing factions then returns an empty list`() {
        get("/dashboard/factions", from = "2026-11-01T03:00:00Z")
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.length()").isEqualTo(0)
    }

    @Test
    fun `given a period when getting a faction detail then counts only its games including players`() {
        // Marqueses fora de setembro: Ana em agosto (vitória), Bruno em outubro (derrota).
        get("/dashboard/factions/{faction}", to = SETEMBRO, vars = arrayOf("Marqueses"))
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.totalGames").isEqualTo(1)
            .jsonPath("$.distinctPlayers").isEqualTo(1)
            .jsonPath("$.players[*].playerName").isEqualTo(listOf("Ana"))
            .jsonPath("$.players[0].winPercentage").isEqualTo(100.0)

        get("/dashboard/factions/{faction}", SETEMBRO, OUTUBRO, vars = arrayOf("Marqueses"))
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.totalGames").isEqualTo(2)
            .jsonPath("$.players[*].playerName").isEqualTo(listOf("Bruno"))
            .jsonPath("$.players[0].totalScore").isEqualTo(60)
    }

    @Test
    fun `given a faction played only outside the period when getting its detail then responds 404`() {
        get("/dashboard/factions/{faction}", from = "2026-11-01T03:00:00Z", vars = arrayOf("Marqueses"))
            .expectStatus().isNotFound
            .expectBody().isEmpty
    }

    @ParameterizedTest
    @ValueSource(strings = ["/dashboard/ranking", "/dashboard/ranking/1", "/dashboard/factions", "/dashboard/factions/Marqueses"])
    fun `given from not before to when getting a dashboard then responds 400 as plain text`(path: String) {
        listOf(OUTUBRO to SETEMBRO, SETEMBRO to SETEMBRO).forEach { (from, to) ->
            get(path, from, to)
                .expectStatus().isBadRequest
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
                .expectBody(String::class.java).isEqualTo("'from' precisa ser anterior a 'to'")
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["/dashboard/ranking", "/dashboard/ranking/1", "/dashboard/factions", "/dashboard/factions/Marqueses"])
    fun `given a malformed date when getting a dashboard then responds 400`(path: String) {
        get(path, from = "ontem")
            .expectStatus().isBadRequest
    }
}
