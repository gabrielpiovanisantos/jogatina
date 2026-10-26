package com.gabriel.jogatina.integration

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * V3 traduz as facções já gravadas para os nomes do manual brasileiro.
 * Migra o banco até a V2, grava os nomes em inglês do jeito que o finish
 * gravaria (com maiúsculas e espaços variados) e então aplica a V3.
 */
@Testcontainers
class FactionNamesMigrationTest {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer(
            DockerImageName.parse("docker.io/library/postgres:17-alpine").asCompatibleSubstituteFor("postgres")
        )
    }

    private val dataSource by lazy { DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password) }

    private fun migrate(target: String) =
        Flyway.configure().dataSource(dataSource).target(target).load().migrate()

    @Test
    fun `given english faction names when migrating to V3 then renames them to portuguese and keeps unknown ones`() {
        migrate("2")
        val jdbc = JdbcTemplate(dataSource)
        jdbc.update("insert into players (name) values ('Ana')")
        jdbc.update("insert into games (date) values (now())")

        val gravadas = listOf(
            "Marquise de Cat", "EYRIE DYNASTIES", " woodland alliance ", "Vagabond", "vagabond",
            "Lizard Cult", "Riverfolk Company", "Underground Duchy", "Corvid Conspiracy",
            "Lord of the Hundreds", "Keepers in Iron", "Facção Caseira", "Marquise"
        )
        gravadas.forEach {
            jdbc.update("insert into game_results (game_id, player_id, score, faction, won) values (1, 1, 0, ?, false)", it)
        }

        migrate("3")

        val faccoes = jdbc.queryForList("select faction from game_results order by id", String::class.java)
        assertEquals(
            listOf(
                "Marqueses", "Dinastia das Rapinas", "Aliança da Floresta", "Malandro", "Malandro",
                "Lagartos Cultistas", "Companhia Ribeirinha", "Ducado Subterrâneo", "Conspiração Corvídea",
                "Senhor das Centenas", "Guardiões de Ferro", "Facção Caseira", "Marquise"
            ),
            faccoes
        )
    }
}
