package com.gabriel.jogatina.integration

import com.gabriel.jogatina.models.Player
import com.gabriel.jogatina.repositories.PlayerRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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

/**
 * /actuator/prometheus para o compose de estudo (Prometheus, Grafana e k6) do
 * jogatina-infra. Só health, info e prometheus ficam expostos.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ActuatorMetricsIntegrationTest {

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

    private lateinit var client: WebTestClient

    @BeforeEach
    fun setUp() {
        client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()
        jdbcTemplate.execute("TRUNCATE games, game_results, players RESTART IDENTITY CASCADE")
    }

    private fun scrape(): String = client.get().uri("/actuator/prometheus").exchange()
        .expectStatus().isOk
        .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
        .expectBody(String::class.java).returnResult().responseBody!!

    /** Valor da primeira série de [metric] cujas labels contêm todos os [labels]. */
    private fun valor(texto: String, metric: String, vararg labels: String): Double =
        texto.lines()
            .filter { it.startsWith("$metric{") && labels.all { l -> it.contains(l) } }
            .map { it.substringAfterLast(' ').toDouble() }
            .firstOrNull() ?: error("série não encontrada: $metric $labels")

    private fun finalizar(playerId: Long, faction: String) = client.post().uri("/api/games/finish")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""{"dateTime":"2026-10-01T20:00:00Z","results":[{"playerId":$playerId,"faction":"$faction","points":10,"won":true}]}""")
        .exchange()

    @Test
    fun `given a request when scraping then exposes the http histogram with the slo buckets and the application tag`() {
        client.get().uri("/api/factions").exchange().expectStatus().isOk

        val texto = scrape()

        val bucket = texto.lines().filter {
            it.startsWith("http_server_requests_seconds_bucket{") && it.contains("""uri="/api/factions"""")
        }
        assertTrue(bucket.isNotEmpty()) { "sem buckets de /api/factions" }
        assertTrue(bucket.all { it.contains("""application="jogatina"""") })
        listOf("0.05", "0.1", "0.25", "0.5", "1.0").forEach { le ->
            assertTrue(bucket.any { it.contains("""le="$le"""") }) { "sem bucket le=$le" }
        }
    }

    @Test
    fun `when scraping then exposes jvm, hikari and tomcat metrics`() {
        val texto = scrape()

        listOf(
            "jvm_memory_used_bytes{",
            "jvm_threads_live_threads{",
            "hikaricp_connections_active{",
            "hikaricp_connections_pending{",
            "tomcat_threads_busy_threads{",
            "tomcat_sessions_active_current_sessions{"
        ).forEach { assertTrue(texto.contains(it)) { "métrica ausente: $it" } }
    }

    @Test
    fun `given finished and rejected games when scraping then counts them by reason`() {
        val ana = playerRepository.save(Player(name = "Ana")).id!!
        val antes = scrape()
        val finalizadasAntes = valor(antes, "jogatina_games_finished_total")
        val invalidasAntes = valor(antes, "jogatina_games_finish_rejected_total", """reason="invalid_faction"""")
        // Todos os motivos aparecem desde o início, mesmo zerados.
        listOf("no_results", "blank_faction", "invalid_faction", "negative_score", "duplicate_player", "unknown_player")
            .forEach { assertTrue(antes.contains("""reason="$it"""")) { "motivo ausente: $it" } }

        finalizar(ana, "Marqueses").expectStatus().isCreated
        finalizar(ana, "Stark").expectStatus().isBadRequest
        finalizar(ana, "Stark").expectStatus().isBadRequest

        val depois = scrape()
        assertEquals(finalizadasAntes + 1, valor(depois, "jogatina_games_finished_total"))
        assertEquals(invalidasAntes + 2, valor(depois, "jogatina_games_finish_rejected_total", """reason="invalid_faction""""))
    }

    @Test
    fun `when calling actuator endpoints then only health, info and prometheus are exposed`() {
        client.get().uri("/actuator/health").exchange().expectStatus().isOk
        client.get().uri("/actuator/info").exchange().expectStatus().isOk
        listOf("env", "beans", "heapdump", "configprops", "metrics", "loggers", "threaddump", "mappings").forEach {
            client.get().uri("/actuator/$it").exchange().expectStatus().isNotFound
        }
    }
}
