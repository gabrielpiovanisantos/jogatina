package com.gabriel.jogatina.integration

import com.gabriel.jogatina.models.Player
import com.gabriel.jogatina.repositories.PlayerRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * GET /api/factions (o dropdown do RegistrarPartida) e a validação da facção
 * no POST /api/games/finish, que só aceita as facções dessa lista.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FactionsIntegrationTest {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer(
            DockerImageName.parse("docker.io/library/postgres:17-alpine").asCompatibleSubstituteFor("postgres")
        )

        val OFICIAIS = listOf(
            "Marqueses", "Dinastia das Rapinas", "Aliança da Floresta", "Malandro", "Lagartos Cultistas",
            "Companhia Ribeirinha", "Ducado Subterrâneo", "Conspiração Corvídea", "Senhor das Centenas",
            "Guardiões de Ferro"
        )
    }

    @LocalServerPort
    var port: Int = 0

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var playerRepository: PlayerRepository

    private lateinit var client: WebTestClient

    @BeforeEach
    fun setUp() {
        client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()
        jdbcTemplate.execute("TRUNCATE games, game_results, players RESTART IDENTITY CASCADE")
    }

    private fun listarFaccoes(): List<String> = client.get().uri("/api/factions").exchange()
        .expectStatus().isOk
        .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .expectBody(object : ParameterizedTypeReference<List<String>>() {})
        .returnResult().responseBody!!

    private fun finalizar(vararg resultados: Pair<Long, String>) = client.post().uri("/api/games/finish")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            mapOf(
                "dateTime" to "2026-10-01T20:00:00Z",
                "results" to resultados.mapIndexed { i, (playerId, faction) ->
                    mapOf("playerId" to playerId, "faction" to faction, "points" to 10, "won" to (i == 0))
                }
            )
        )
        .exchange()

    private fun jogadores(n: Int) = (1..n).map { playerRepository.save(Player(name = "Jogador $it")).id!! }

    private fun faccoesGravadas() =
        jdbcTemplate.queryForList("select faction from game_results order by id", String::class.java)

    private fun partidasGravadas() = jdbcTemplate.queryForObject("select count(*) from games", Int::class.java)

    @Test
    fun `when listing factions then returns the official names in the manual order`() {
        assertEquals(OFICIAIS, listarFaccoes())
    }

    @Test
    fun `given every listed faction in another case and with spaces when finishing a game then accepts all and saves the official spelling`() {
        val faccoes = listarFaccoes()
        val ids = jogadores(faccoes.size)
        val digitadas = faccoes.mapIndexed { i, f -> if (i % 2 == 0) "  ${f.uppercase()} " else f.lowercase() }

        finalizar(*ids.zip(digitadas).toTypedArray())
            .expectStatus().isCreated

        assertEquals(faccoes, faccoesGravadas())
    }

    @Test
    fun `given a faction outside the list when finishing a game then responds 400 without saving anything`() {
        val (ana, bruno) = jogadores(2)

        finalizar(ana to "Marqueses", bruno to " Stark ")
            .expectStatus().isBadRequest
            .expectBody(String::class.java).isEqualTo("Facção inválida: Stark")

        assertEquals(0, partidasGravadas())
        assertEquals(emptyList<String>(), faccoesGravadas())
    }

    @Test
    fun `given several invalid factions when finishing a game then lists each one once`() {
        val (ana, bruno, carla) = jogadores(3)

        finalizar(ana to "Stark", bruno to "Lannister", carla to "stark")
            .expectStatus().isBadRequest
            .expectBody(String::class.java).isEqualTo("Facção inválida: Stark, Lannister")
    }

    @Test
    fun `given a faction without its accents when finishing a game then responds 400`() {
        val (ana) = jogadores(1)

        finalizar(ana to "Conspiracao Corvidea")
            .expectStatus().isBadRequest
            .expectBody(String::class.java).isEqualTo("Facção inválida: Conspiracao Corvidea")

        assertEquals(0, partidasGravadas())
    }
}
