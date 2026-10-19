package com.gabriel.jogatina.integration

import com.gabriel.jogatina.models.Game
import com.gabriel.jogatina.models.GameResult
import com.gabriel.jogatina.models.Player
import com.gabriel.jogatina.repositories.GameRepository
import com.gabriel.jogatina.repositories.PlayerRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
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
 * Testes de integração do contrato HTTP consumido pelo frontend
 * (repo irmão ../frontend-jogatina, src/services/api.js).
 *
 * Sobe a aplicação inteira contra um Postgres real (mesma imagem do
 * docker-compose), então também valida as migrations do Flyway contra o
 * mapeamento JPA (ddl-auto: validate).
 *
 * O cenário de ranking é o mesmo de tests/support/cenario.js no frontend, para
 * que os dois lados concordem sobre o resultado esperado.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FrontendContractIntegrationTest {

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

    private data class Resultado(val jogador: String, val faccao: String, val pontos: Int, val venceu: Boolean)

    private fun semearCenarioRanking(): Map<String, Long> {
        // Eva é inserida primeiro para garantir que a ordem vem do ranking, não da inserção.
        val jogadores = listOf("Eva", "Carla", "Ana", "Diego", "Bruno")
            .associateWith { playerRepository.save(Player(name = it)) }

        fun partida(data: String, vararg resultados: Resultado) {
            val game = Game(date = Instant.parse(data))
            resultados.forEach {
                game.gameResults.add(
                    GameResult(game = game, player = jogadores.getValue(it.jogador), score = it.pontos, faction = it.faccao, won = it.venceu)
                )
            }
            gameRepository.save(game)
        }

        partida(
            "2026-09-01T20:00:00Z",
            Resultado("Ana", "Marquise de Cat", 30, true),
            Resultado("Bruno", "Eyrie Dynasties", 22, false),
            Resultado("Carla", "Woodland Alliance", 18, false)
        )
        partida(
            "2026-09-08T20:00:00Z",
            Resultado("Ana", "Eyrie Dynasties", 25, false),
            Resultado("Bruno", "Vagabond", 30, true),
            Resultado("Carla", "Marquise de Cat", 20, false),
            Resultado("Diego", "Lizard Cult", 10, false)
        )
        partida(
            "2026-09-15T20:00:00Z",
            Resultado("Ana", "Vagabond", 28, true),
            Resultado("Carla", "Riverfolk Company", 15, false),
            Resultado("Diego", "Lizard Cult", 12, false)
        )

        return jogadores.mapValues { it.value.id!! }
    }

    private fun organizar(body: String) = client.post().uri("/api/setup/organize")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .exchange()

    private fun jogadoresJson(n: Int) =
        (1..n).joinToString(",", "[", "]") { """{"id":$it,"name":"Jogador $it"}""" }

    @Nested
    inner class OrganizeTables {

        @Test
        fun `given seven players when organizing then returns the groups in the format SetupMesa reads`() {
            organizar("""{"players":${jogadoresJson(7)}}""")
                .expectStatus().isOk
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.groups.length()").isEqualTo(2)
                .jsonPath("$.groups[0].length()").isEqualTo(4)
                .jsonPath("$.groups[1].length()").isEqualTo(3)
                .jsonPath("$.groups[0][0].id").isEqualTo(1)
                .jsonPath("$.groups[0][0].name").isEqualTo("Jogador 1")
        }

        @Test
        fun `given new names with Date now ids when organizing then registers them and returns the database ids`() {
            val base = 1_790_000_000_000L
            organizar("""{"players":[{"id":$base,"name":"Ana"},{"id":${base + 1},"name":"Bruno"},{"id":${base + 2},"name":"Carla"}]}""")
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.groups[0][*].id").isEqualTo(listOf(1, 2, 3))
                .jsonPath("$.groups[0][2].name").isEqualTo("Carla")

            assertEquals(listOf("Ana", "Bruno", "Carla"), playerRepository.findAll().sortedBy { it.id }.map { it.name })
        }

        @Test
        fun `given a registered player when organizing with the name in another case then reuses the player`() {
            val ana = playerRepository.save(Player(name = "Ana"))

            organizar("""{"players":[{"id":1,"name":" ana "},{"id":2,"name":"Bruno"},{"id":3,"name":"Carla"}]}""")
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.groups[0][0].id").isEqualTo(ana.id!!)
                .jsonPath("$.groups[0][0].name").isEqualTo("Ana")

            assertEquals(3, playerRepository.count())
        }

        @Test
        fun `given fewer than three players when organizing then responds 400 with the plain text message the frontend shows`() {
            organizar("""{"players":${jogadoresJson(2)}}""")
                .expectStatus().isBadRequest
                .expectBody(String::class.java).isEqualTo("É necessário pelo menos 3 jogadores para organizar mesas")
        }

        @Test
        fun `given a blank name when organizing then responds 400`() {
            organizar("""{"players":[{"id":1,"name":"Ana"},{"id":2,"name":"Bruno"},{"id":3,"name":"  "}]}""")
                .expectStatus().isBadRequest
                .expectBody(String::class.java).isEqualTo("Nome de jogador não pode ser vazio")
        }

        @Test
        fun `given a repeated name when organizing then responds 400 without registering anyone`() {
            organizar("""{"players":[{"id":1,"name":"Ana"},{"id":2,"name":"ANA"},{"id":3,"name":"Carla"}]}""")
                .expectStatus().isBadRequest
                .expectBody(String::class.java).isEqualTo("Jogador repetido na lista")

            assertEquals(0, playerRepository.count())
        }
    }

    @Nested
    inner class FinishGame {

        private fun finalizar(body: String) = client.post().uri("/api/games/finish")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()

        private fun resultado(playerId: Long, faction: String, points: Int, won: Boolean) =
            """{"playerId":$playerId,"faction":"$faction","points":$points,"won":$won}"""

        @Test
        fun `given registered players when finishing a game in the RegistrarPartida format then saves it and counts it in the ranking`() {
            val ana = playerRepository.save(Player(name = "Ana")).id!!
            val bruno = playerRepository.save(Player(name = "Bruno")).id!!

            finalizar(
                """{"dateTime":"2026-10-01T20:00:00.000Z","results":[
                    ${resultado(ana, "Marquise de Cat", 30, true)},
                    ${resultado(bruno, "Vagabond", 20, false)}
                ]}"""
            )
                .expectStatus().isCreated
                .expectBody().jsonPath("$.gameId").isNumber

            assert(jdbcTemplate.queryForObject("select count(*) from games", Int::class.java) == 1)
            client.get().uri("/dashboard/ranking").exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$[*].playerName").isEqualTo(listOf("Ana", "Bruno"))
                .jsonPath("$[0].totalVictories").isEqualTo(1)
                .jsonPath("$[0].totalScore").isEqualTo(30)
                .jsonPath("$[1].totalGames").isEqualTo(1)
        }

        @Test
        fun `given an unknown player when finishing a game then responds 400 without saving anything`() {
            val ana = playerRepository.save(Player(name = "Ana")).id!!

            finalizar(
                """{"dateTime":"2026-10-01T20:00:00Z","results":[
                    ${resultado(ana, "Vagabond", 30, true)},
                    ${resultado(999, "Lizard Cult", 10, false)}
                ]}"""
            )
                .expectStatus().isBadRequest
                .expectBody(String::class.java).isEqualTo("Jogador(es) não encontrado(s): 999")

            assert(jdbcTemplate.queryForObject("select count(*) from games", Int::class.java) == 0)
        }

        @Test
        fun `given the same player twice when finishing a game then responds 400`() {
            val ana = playerRepository.save(Player(name = "Ana")).id!!

            finalizar(
                """{"dateTime":"2026-10-01T20:00:00Z","results":[
                    ${resultado(ana, "Vagabond", 30, true)},
                    ${resultado(ana, "Lizard Cult", 10, false)}
                ]}"""
            )
                .expectStatus().isBadRequest
                .expectBody(String::class.java).isEqualTo("Jogador repetido na mesma partida")
        }

        @Test
        fun `given no results when finishing a game then responds 400`() {
            finalizar("""{"dateTime":"2026-10-01T20:00:00Z","results":[]}""")
                .expectStatus().isBadRequest
                .expectBody(String::class.java).isEqualTo("A partida precisa ter pelo menos um resultado")
        }

        @Test
        fun `given a negative score when finishing a game then responds 400`() {
            val ana = playerRepository.save(Player(name = "Ana")).id!!

            finalizar("""{"dateTime":"2026-10-01T20:00:00Z","results":[${resultado(ana, "Vagabond", -1, true)}]}""")
                .expectStatus().isBadRequest
                .expectBody(String::class.java).isEqualTo("Pontuação não pode ser negativa")
        }

        @Test
        fun `given an empty faction when finishing a game then responds 400 without saving anything`() {
            val ana = playerRepository.save(Player(name = "Ana")).id!!

            finalizar("""{"dateTime":"2026-10-01T20:00:00Z","results":[${resultado(ana, "", 30, true)}]}""")
                .expectStatus().isBadRequest
                .expectBody(String::class.java).isEqualTo("Facção não pode ser vazia")

            assert(jdbcTemplate.queryForObject("select count(*) from games", Int::class.java) == 0)
        }

        @Test
        fun `given a faction with only spaces when finishing a game then responds 400 without saving anything`() {
            // O campo de facção do frontend é required, mas aceita só espaços.
            val ana = playerRepository.save(Player(name = "Ana")).id!!

            finalizar("""{"dateTime":"2026-10-01T20:00:00Z","results":[${resultado(ana, "   ", 30, true)}]}""")
                .expectStatus().isBadRequest
                .expectBody(String::class.java).isEqualTo("Facção não pode ser vazia")

            assert(jdbcTemplate.queryForObject("select count(*) from games", Int::class.java) == 0)
        }
    }

    @Nested
    inner class ListPlayers {

        @Test
        fun `given registered players when listing players then returns id and name in alphabetical order`() {
            val bruno = playerRepository.save(Player(name = "Bruno")).id!!
            val ana = playerRepository.save(Player(name = "ana")).id!!
            val carla = playerRepository.save(Player(name = "Carla")).id!!

            client.get().uri("/api/players").exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$[*].name").isEqualTo(listOf("ana", "Bruno", "Carla"))
                .jsonPath("$[*].id").isEqualTo(listOf(ana.toInt(), bruno.toInt(), carla.toInt()))
        }

        @Test
        fun `given no players when listing players then returns an empty list`() {
            client.get().uri("/api/players").exchange()
                .expectStatus().isOk
                .expectBody().json("[]")
        }
    }

    @Nested
    inner class Ranking {

        @Test
        fun `given no players when getting the ranking then returns an empty list`() {
            client.get().uri("/dashboard/ranking").exchange()
                .expectStatus().isOk
                .expectBody().json("[]")
        }

        @Test
        fun `given played games when getting the ranking then returns it sorted with the fields Ranking and Dashboard read`() {
            val ids = semearCenarioRanking()

            client.get().uri("/dashboard/ranking").exchange()
                .expectStatus().isOk
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$[*].playerName").isEqualTo(listOf("Ana", "Bruno", "Carla", "Diego", "Eva"))
                .jsonPath("$[*].totalVictories").isEqualTo(listOf(2, 1, 0, 0, 0))
                .jsonPath("$[*].totalGames").isEqualTo(listOf(3, 2, 3, 2, 0))
                .jsonPath("$[*].differentFactions").isEqualTo(listOf(3, 2, 3, 1, 0))
                .jsonPath("$[*].totalScore").isEqualTo(listOf(83, 52, 53, 22, 0))
                .jsonPath("$[0].playerId").isEqualTo(ids.getValue("Ana"))
                .jsonPath("$[0].winPercentage").value(Double::class.java) { assert(it in 66.66..66.67) }
                .jsonPath("$[0].length()").isEqualTo(7)
        }

        @Test
        fun `given played games when getting a player ranking then returns that player statistics`() {
            val ids = semearCenarioRanking()

            client.get().uri("/dashboard/ranking/{id}", ids.getValue("Bruno")).exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.playerId").isEqualTo(ids.getValue("Bruno"))
                .jsonPath("$.playerName").isEqualTo("Bruno")
                .jsonPath("$.totalVictories").isEqualTo(1)
                .jsonPath("$.totalGames").isEqualTo(2)
                .jsonPath("$.winPercentage").isEqualTo(50.0)
                .jsonPath("$.differentFactions").isEqualTo(2)
                .jsonPath("$.totalScore").isEqualTo(52)
        }

        @Test
        fun `given an unknown player when getting a player ranking then responds 404 with an empty body`() {
            // O frontend rejeita qualquer status fora de 2xx, então trata o 404 como erro.
            client.get().uri("/dashboard/ranking/999").exchange()
                .expectStatus().isNotFound
                .expectBody().isEmpty
        }
    }
}
