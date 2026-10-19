package com.gabriel.jogatina.integration

import com.gabriel.jogatina.models.Game
import com.gabriel.jogatina.repositories.GameRepository
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
 * GET /api/games?from=&to=, usado pelo RegistrarPartida do frontend para
 * numerar a partida do dia ("Partida 2 · 19/10/2026"). O frontend manda o
 * início e o fim do dia no fuso dele, então os limites aqui são instantes UTC.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ListGamesIntegrationTest {

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
    lateinit var gameRepository: GameRepository

    private lateinit var client: WebTestClient

    @BeforeEach
    fun setUp() {
        client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()
        jdbcTemplate.execute("TRUNCATE games, game_results, players RESTART IDENTITY CASCADE")
    }

    private fun partida(data: String) = gameRepository.save(Game(date = Instant.parse(data))).id!!

    private fun listar(from: String, to: String) = client.get()
        .uri { it.path("/api/games").queryParam("from", from).queryParam("to", to).build() }
        .exchange()

    @Test
    fun `given games on several days when listing one day then returns only that day's games oldest first`() {
        // Dia 19/10 em Brasília (UTC-3) = [2026-10-19T03:00Z, 2026-10-20T03:00Z)
        partida("2026-10-19T02:59:59Z") // 18/10 23:59:59 em Brasília
        val noite = partida("2026-10-19T23:30:00Z")
        val tarde = partida("2026-10-19T18:00:00Z")
        val limite = partida("2026-10-19T03:00:00Z") // 19/10 00:00 em Brasília: entra
        partida("2026-10-20T03:00:00Z") // 20/10 00:00 em Brasília: fica de fora

        listar("2026-10-19T03:00:00Z", "2026-10-20T03:00:00Z")
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.length()").isEqualTo(3)
            .jsonPath("$[*].id").isEqualTo(listOf(limite.toInt(), tarde.toInt(), noite.toInt()))
            .jsonPath("$[1].dateTime").isEqualTo("2026-10-19T18:00:00Z")
    }

    @Test
    fun `given no games in the range when listing then returns an empty list`() {
        partida("2026-10-01T20:00:00Z")

        listar("2026-10-19T03:00:00Z", "2026-10-20T03:00:00Z")
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.length()").isEqualTo(0)
    }

    @Test
    fun `given from after to when listing then responds 400`() {
        listar("2026-10-20T03:00:00Z", "2026-10-19T03:00:00Z")
            .expectStatus().isBadRequest
            .expectBody(String::class.java).isEqualTo("'from' precisa ser anterior a 'to'")
    }

    @Test
    fun `given a missing parameter when listing then responds 400`() {
        client.get().uri("/api/games?from=2026-10-19T03:00:00Z")
            .exchange()
            .expectStatus().isBadRequest
    }
}
