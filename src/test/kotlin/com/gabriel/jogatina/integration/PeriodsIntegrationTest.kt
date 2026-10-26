package com.gabriel.jogatina.integration

import org.junit.jupiter.api.Assertions.assertEquals
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
 * CRUD de /api/periods: períodos com nome (temporadas) que o frontend passa
 * como from/to para os dashboards. "to" é exclusivo e pode ser nulo.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PeriodsIntegrationTest {

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

    private lateinit var client: WebTestClient

    @BeforeEach
    fun setUp() {
        client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()
        jdbcTemplate.execute("TRUNCATE periods RESTART IDENTITY")
    }

    private fun criar(body: String) = client.post().uri("/api/periods")
        .contentType(MediaType.APPLICATION_JSON).bodyValue(body).exchange()

    private fun atualizar(id: Any, body: String) = client.put().uri("/api/periods/{id}", id)
        .contentType(MediaType.APPLICATION_JSON).bodyValue(body).exchange()

    private fun criarId(body: String): Int {
        val id = criar(body).expectStatus().isCreated
            .expectBody(Map::class.java).returnResult().responseBody!!["id"]
        return id as Int
    }

    private fun periodosGravados() = jdbcTemplate.queryForObject("select count(*) from periods", Int::class.java)

    private fun esperar400(response: WebTestClient.ResponseSpec, mensagem: String) {
        response.expectStatus().isBadRequest
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
            .expectBody(String::class.java).isEqualTo(mensagem)
    }

    @Test
    fun `given a valid period when creating it then responds 201 with the trimmed name and its location`() {
        criar("""{"name":"  Temporada 1 ","from":"2026-01-01T03:00:00Z","to":"2026-04-01T03:00:00Z"}""")
            .expectStatus().isCreated
            .expectHeader().valueEquals("Location", "/api/periods/1")
            .expectBody()
            .jsonPath("$.id").isEqualTo(1)
            .jsonPath("$.name").isEqualTo("Temporada 1")
            .jsonPath("$.from").isEqualTo("2026-01-01T03:00:00Z")
            .jsonPath("$.to").isEqualTo("2026-04-01T03:00:00Z")
    }

    @Test
    fun `given a period in progress when creating it then accepts a null or missing to`() {
        criar("""{"name":"Temporada atual","from":"2026-09-01T03:00:00Z","to":null}""")
            .expectStatus().isCreated
            .expectBody().jsonPath("$.to").isEmpty

        criar("""{"name":"Outra","from":"2026-09-01T03:00:00Z"}""")
            .expectStatus().isCreated
            .expectBody().jsonPath("$.to").isEmpty
    }

    @Test
    fun `given no saved periods when listing them then responds 200 with an empty list`() {
        client.get().uri("/api/periods").exchange()
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$").isArray
            .jsonPath("$.length()").isEqualTo(0)
    }

    @Test
    fun `given saved periods when listing them then returns newest first`() {
        criarId("""{"name":"Antiga","from":"2025-01-01T03:00:00Z","to":"2025-06-01T03:00:00Z"}""")
        criarId("""{"name":"Atual","from":"2026-09-01T03:00:00Z"}""")
        criarId("""{"name":"Meio","from":"2026-01-01T03:00:00Z","to":"2026-09-01T03:00:00Z"}""")

        client.get().uri("/api/periods").exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$[*].name").isEqualTo(listOf("Atual", "Meio", "Antiga"))
            .jsonPath("$[0].to").isEmpty
            .jsonPath("$[1].id").isNumber
    }

    @Test
    fun `given an existing period when replacing it then responds 200 and a missing to becomes null`() {
        val id = criarId("""{"name":"Temporada 1","from":"2026-01-01T03:00:00Z","to":"2026-04-01T03:00:00Z"}""")

        atualizar(id, """{"name":"temporada 1 (fechada)","from":"2026-01-02T03:00:00Z"}""")
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(id)
            .jsonPath("$.name").isEqualTo("temporada 1 (fechada)")
            .jsonPath("$.from").isEqualTo("2026-01-02T03:00:00Z")
            .jsonPath("$.to").isEmpty
    }

    @Test
    fun `given a period when replacing it keeping its own name in another case then accepts it`() {
        val id = criarId("""{"name":"Temporada 1","from":"2026-01-01T03:00:00Z"}""")

        atualizar(id, """{"name":"TEMPORADA 1","from":"2026-01-01T03:00:00Z"}""")
            .expectStatus().isOk
            .expectBody().jsonPath("$.name").isEqualTo("TEMPORADA 1")
    }

    @Test
    fun `given an existing period when deleting it then responds 204 and it disappears`() {
        val id = criarId("""{"name":"Temporada 1","from":"2026-01-01T03:00:00Z"}""")

        client.delete().uri("/api/periods/{id}", id).exchange()
            .expectStatus().isNoContent
        client.get().uri("/api/periods").exchange()
            .expectBody().jsonPath("$.length()").isEqualTo(0)
    }

    @Test
    fun `given an unknown id when replacing or deleting then responds 404`() {
        atualizar(999, """{"name":"X","from":"2026-01-01T03:00:00Z"}""")
            .expectStatus().isNotFound
            .expectBody().isEmpty
        client.delete().uri("/api/periods/999").exchange()
            .expectStatus().isNotFound
            .expectBody().isEmpty
    }

    @Test
    fun `given an empty, blank or missing name when creating a period then responds 400 without saving`() {
        listOf(
            """{"name":"","from":"2026-01-01T03:00:00Z"}""",
            """{"name":"   ","from":"2026-01-01T03:00:00Z"}""",
            """{"from":"2026-01-01T03:00:00Z"}"""
        ).forEach { esperar400(criar(it), "Nome do período não pode ser vazio") }

        assertEquals(0, periodosGravados())
    }

    @Test
    fun `given a name longer than 100 characters when creating a period then responds 400`() {
        esperar400(
            criar("""{"name":"${"a".repeat(101)}","from":"2026-01-01T03:00:00Z"}"""),
            "Nome do período pode ter no máximo 100 caracteres"
        )
        criar("""{"name":"${"a".repeat(100)}","from":"2026-01-01T03:00:00Z"}""")
            .expectStatus().isCreated
    }

    @Test
    fun `given a name already used in another case and with spaces when creating a period then responds 400`() {
        criarId("""{"name":"Temporada 1","from":"2026-01-01T03:00:00Z"}""")

        esperar400(
            criar("""{"name":"  temporada 1 ","from":"2026-05-01T03:00:00Z"}"""),
            "Já existe um período com o nome: temporada 1"
        )
        assertEquals(1, periodosGravados())
    }

    @Test
    fun `given the name of another period when replacing a period then responds 400 without changing it`() {
        criarId("""{"name":"Temporada 1","from":"2026-01-01T03:00:00Z"}""")
        val id = criarId("""{"name":"Temporada 2","from":"2026-05-01T03:00:00Z"}""")

        esperar400(
            atualizar(id, """{"name":"TEMPORADA 1","from":"2026-05-01T03:00:00Z"}"""),
            "Já existe um período com o nome: TEMPORADA 1"
        )
        assertEquals(
            "Temporada 2",
            jdbcTemplate.queryForObject("select name from periods where id = ?", String::class.java, id)
        )
    }

    @Test
    fun `given a missing from when creating a period then responds 400`() {
        esperar400(criar("""{"name":"Temporada 1","to":"2026-04-01T03:00:00Z"}"""), "'from' é obrigatório")
    }

    @Test
    fun `given from not before to when creating or replacing a period then responds 400`() {
        esperar400(
            criar("""{"name":"Temporada 1","from":"2026-04-01T03:00:00Z","to":"2026-01-01T03:00:00Z"}"""),
            "'from' precisa ser anterior a 'to'"
        )
        esperar400(
            criar("""{"name":"Temporada 1","from":"2026-04-01T03:00:00Z","to":"2026-04-01T03:00:00Z"}"""),
            "'from' precisa ser anterior a 'to'"
        )
        val id = criarId("""{"name":"Temporada 1","from":"2026-01-01T03:00:00Z"}""")
        esperar400(
            atualizar(id, """{"name":"Temporada 1","from":"2026-04-01T03:00:00Z","to":"2026-01-01T03:00:00Z"}"""),
            "'from' precisa ser anterior a 'to'"
        )
    }

    @Test
    fun `given a malformed date when creating a period then responds 400`() {
        criar("""{"name":"Temporada 1","from":"ontem"}""")
            .expectStatus().isBadRequest
    }

    @Test
    fun `when listing exported repositories then saved periods are not exposed by Spring Data REST`() {
        client.get().uri("/periods").exchange().expectStatus().isNotFound
        client.get().uri("/savedPeriods").exchange().expectStatus().isNotFound
    }
}
