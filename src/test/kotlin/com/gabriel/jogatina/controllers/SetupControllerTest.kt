package com.gabriel.jogatina.controllers

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.mock
import org.springframework.http.HttpStatus
import com.gabriel.jogatina.models.Player
import com.gabriel.jogatina.repositories.PlayerRepository
import com.gabriel.jogatina.services.SetupService

class SetupControllerTest {

    // In-memory stand-in for the database: save() assigns ids, every other call
    // (findByNameIgnoreCase included) returns null, so every player is new.
    private var nextId = 1000L
    private val playerRepository: PlayerRepository = mock(PlayerRepository::class.java) { invocation ->
        if (invocation.method.name == "save") invocation.getArgument<Player>(0).apply { id = nextId++ } else null
    }

    private val controller = SetupController(SetupService(playerRepository))

    private fun createPlayers(count: Int): List<SetupPlayerDTO> {
        return (1..count).map { i ->
            SetupPlayerDTO(id = i.toLong(), name = "Player$i")
        }
    }

    private fun validatePartition(groups: List<List<SetupPlayerDTO>>, expectedTotal: Int) {
        // Verify total number of players
        val totalPlayers = groups.sumOf { it.size }
        assertEquals(expectedTotal, totalPlayers, "Total players mismatch")

        // Verify each group is between 3 and 5
        for (group in groups) {
            assertTrue(group.size in 3..5, "Group size ${group.size} is not between 3 and 5")
        }

        // Verify no duplicates
        val allPlayerIds = groups.flatMap { it.map { p -> p.id } }
        assertEquals(allPlayerIds.size, allPlayerIds.distinct().size, "Duplicate players found")
    }

    @Test
    fun `given fewer than 3 players when organizing then responds 400`() {
        val request1 = SetupRequest(players = createPlayers(1))
        val response1 = controller.organize(request1)
        assertEquals(HttpStatus.BAD_REQUEST, response1.statusCode)

        val request2 = SetupRequest(players = createPlayers(2))
        val response2 = controller.organize(request2)
        assertEquals(HttpStatus.BAD_REQUEST, response2.statusCode)
    }

    @Test
    fun `given 3 players when organizing then returns one table of 3`() {
        val request = SetupRequest(players = createPlayers(3))
        val response = controller.organize(request)
        assertEquals(HttpStatus.OK, response.statusCode)

        val result = (response.body as? SetupResponse) ?: error("Response body is null")
        assertEquals(1, result.groups.size, "Should have 1 group")
        assertEquals(3, result.groups[0].size, "Group should have 3 players")
        validatePartition(result.groups, 3)
    }

    @Test
    fun `given 4 players when organizing then returns one table of 4`() {
        val request = SetupRequest(players = createPlayers(4))
        val response = controller.organize(request)
        assertEquals(HttpStatus.OK, response.statusCode)

        val result = (response.body as? SetupResponse) ?: error("Response body is null")
        assertEquals(1, result.groups.size, "Should have 1 group")
        assertEquals(4, result.groups[0].size, "Group should have 4 players")
        validatePartition(result.groups, 4)
    }

    @Test
    fun `given 5 players when organizing then returns one table of 5`() {
        val request = SetupRequest(players = createPlayers(5))
        val response = controller.organize(request)
        assertEquals(HttpStatus.OK, response.statusCode)

        val result = (response.body as? SetupResponse) ?: error("Response body is null")
        assertEquals(1, result.groups.size, "Should have 1 group")
        assertEquals(5, result.groups[0].size, "Group should have 5 players")
        validatePartition(result.groups, 5)
    }

    @Test
    fun `given 6 players when organizing then returns two tables of 3`() {
        val request = SetupRequest(players = createPlayers(6))
        val response = controller.organize(request)
        assertEquals(HttpStatus.OK, response.statusCode)

        val result = (response.body as? SetupResponse) ?: error("Response body is null")
        assertEquals(2, result.groups.size, "Should have 2 groups")
        assertEquals(3, result.groups[0].size, "First group should have 3 players")
        assertEquals(3, result.groups[1].size, "Second group should have 3 players")
        validatePartition(result.groups, 6)
    }

    @Test
    fun `given 7 players when organizing then returns tables of 4 and 3`() {
        val request = SetupRequest(players = createPlayers(7))
        val response = controller.organize(request)
        assertEquals(HttpStatus.OK, response.statusCode)

        val result = (response.body as? SetupResponse) ?: error("Response body is null")
        assertEquals(2, result.groups.size, "Should have 2 groups")
        assertEquals(4, result.groups[0].size, "First group should have 4 players")
        assertEquals(3, result.groups[1].size, "Second group should have 3 players")
        validatePartition(result.groups, 7)
    }

    @Test
    fun `given 8 players when organizing then returns two tables of 4`() {
        val request = SetupRequest(players = createPlayers(8))
        val response = controller.organize(request)
        assertEquals(HttpStatus.OK, response.statusCode)

        val result = (response.body as? SetupResponse) ?: error("Response body is null")
        assertEquals(2, result.groups.size, "Should have 2 groups")
        assertEquals(4, result.groups[0].size, "First group should have 4 players")
        assertEquals(4, result.groups[1].size, "Second group should have 4 players")
        validatePartition(result.groups, 8)
    }

    @Test
    fun `given 9 players when organizing then returns tables of 5 and 4`() {
        val request = SetupRequest(players = createPlayers(9))
        val response = controller.organize(request)
        assertEquals(HttpStatus.OK, response.statusCode)

        val result = (response.body as? SetupResponse) ?: error("Response body is null")
        assertEquals(2, result.groups.size, "Should have 2 groups (prefer fewer groups)")
        assertEquals(5, result.groups[0].size, "First group should have 5 players")
        assertEquals(4, result.groups[1].size, "Second group should have 4 players")
        validatePartition(result.groups, 9)
    }

    @Test
    fun `given 10 players when organizing then returns two tables of 5`() {
        val request = SetupRequest(players = createPlayers(10))
        val response = controller.organize(request)
        assertEquals(HttpStatus.OK, response.statusCode)

        val result = (response.body as? SetupResponse) ?: error("Response body is null")
        assertEquals(2, result.groups.size, "Should have 2 groups")
        assertEquals(5, result.groups[0].size, "First group should have 5 players")
        assertEquals(5, result.groups[1].size, "Second group should have 5 players")
        validatePartition(result.groups, 10)
    }

    @Test
    fun `given 11 players when organizing then returns tables of 4, 4 and 3`() {
        val request = SetupRequest(players = createPlayers(11))
        val response = controller.organize(request)
        assertEquals(HttpStatus.OK, response.statusCode)

        val result = (response.body as? SetupResponse) ?: error("Response body is null")
        assertEquals(3, result.groups.size, "Should have 3 groups")
        assertEquals(4, result.groups[0].size, "First group should have 4 players")
        assertEquals(4, result.groups[1].size, "Second group should have 4 players")
        assertEquals(3, result.groups[2].size, "Third group should have 3 players")
        validatePartition(result.groups, 11)
    }

    @Test
    fun `given 12 players when organizing then returns three tables of 4`() {
        val request = SetupRequest(players = createPlayers(12))
        val response = controller.organize(request)
        assertEquals(HttpStatus.OK, response.statusCode)

        val result = (response.body as? SetupResponse) ?: error("Response body is null")
        assertEquals(3, result.groups.size, "Should have 3 groups")
        assertEquals(4, result.groups[0].size)
        assertEquals(4, result.groups[1].size)
        assertEquals(4, result.groups[2].size)
        validatePartition(result.groups, 12)
    }

    @Test
    fun `given 13 players when organizing then returns tables of 5, 4 and 4`() {
        val request = SetupRequest(players = createPlayers(13))
        val response = controller.organize(request)
        assertEquals(HttpStatus.OK, response.statusCode)

        val result = (response.body as? SetupResponse) ?: error("Response body is null")
        assertEquals(3, result.groups.size, "Should have 3 groups")
        assertEquals(5, result.groups[0].size, "First group should have 5 players")
        assertEquals(4, result.groups[1].size, "Second group should have 4 players")
        assertEquals(4, result.groups[2].size, "Third group should have 4 players")
        validatePartition(result.groups, 13)
    }

    @Test
    fun `given 14 players when organizing then returns tables of 5, 5 and 4`() {
        val request = SetupRequest(players = createPlayers(14))
        val response = controller.organize(request)
        assertEquals(HttpStatus.OK, response.statusCode)

        val result = (response.body as? SetupResponse) ?: error("Response body is null")
        assertEquals(3, result.groups.size, "Should have 3 groups")
        assertEquals(5, result.groups[0].size)
        assertEquals(5, result.groups[1].size)
        assertEquals(4, result.groups[2].size)
        validatePartition(result.groups, 14)
    }

    @Test
    fun `given 15 players when organizing then returns tables of 4, 4, 4 and 3`() {
        val request = SetupRequest(players = createPlayers(15))
        val response = controller.organize(request)
        assertEquals(HttpStatus.OK, response.statusCode)

        val result = (response.body as? SetupResponse) ?: error("Response body is null")
        assertEquals(4, result.groups.size, "Should have 4 groups")
        assertEquals(4, result.groups[0].size)
        assertEquals(4, result.groups[1].size)
        assertEquals(4, result.groups[2].size)
        assertEquals(3, result.groups[3].size)
        validatePartition(result.groups, 15)
    }

    @Test
    fun `given 20 players when organizing then returns five tables of 4`() {
        val request = SetupRequest(players = createPlayers(20))
        val response = controller.organize(request)
        assertEquals(HttpStatus.OK, response.statusCode)

        val result = (response.body as? SetupResponse) ?: error("Response body is null")
        assertEquals(5, result.groups.size, "Should have 5 groups")
        for (group in result.groups) {
            assertEquals(4, group.size, "All groups should have 4 players")
        }
        validatePartition(result.groups, 20)
    }

    @Test
    fun `given 23 players when organizing then returns six tables`() {
        val request = SetupRequest(players = createPlayers(23))
        val response = controller.organize(request)
        assertEquals(HttpStatus.OK, response.statusCode)

        val result = (response.body as? SetupResponse) ?: error("Response body is null")
        // 23 % 4 = 3 -> (23-3)/4 = 5 groups of 4 + 1 group of 3
        assertEquals(6, result.groups.size, "Should have 6 groups")
        val totalSize = result.groups.sumOf { it.size }
        assertEquals(23, totalSize, "Total should be 23")
        validatePartition(result.groups, 23)
    }

    @Test
    fun `given 8 players when organizing then keeps the original order`() {
        val players = createPlayers(8)
        val request = SetupRequest(players = players)
        val response = controller.organize(request)

        val result = (response.body as? SetupResponse) ?: error("Response body is null")
        val flatList = result.groups.flatten()

        // Players should maintain original order
        for (i in 0..7) {
            assertEquals(players[i].name, flatList[i].name, "Player order not preserved at index $i")
        }
    }

    @Test
    fun `given a blank name when organizing then responds 400`() {
        val players = createPlayers(3).toMutableList()
        players[1] = players[1].copy(name = "   ")
        val request = SetupRequest(players = players)

        val response = controller.organize(request)
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `given a repeated name in another case when organizing then responds 400`() {
        val players = createPlayers(3).toMutableList()
        players[1] = players[1].copy(name = " ${players[0].name.uppercase()} ")
        val request = SetupRequest(players = players)

        val response = controller.organize(request)
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("Jogador repetido na lista", response.body)
    }

    @Test
    fun `given new names when organizing then registers them and returns the database ids`() {
        val response = controller.organize(SetupRequest(players = createPlayers(3)))

        val result = (response.body as? SetupResponse) ?: error("Response body is null")
        assertEquals(listOf(1000L, 1001L, 1002L), result.groups.flatten().map { it.id })
    }

    @Test
    fun `given a registered player when organizing with the name in another case then reuses the player`() {
        val ana = Player(id = 7, name = "Ana")
        val repository: PlayerRepository = mock(PlayerRepository::class.java) { invocation ->
            when (invocation.method.name) {
                "findByNameIgnoreCase" -> if (invocation.getArgument<String>(0).equals("ana", ignoreCase = true)) ana else null
                "save" -> invocation.getArgument<Player>(0).apply { id = nextId++ }
                else -> null
            }
        }
        val players = listOf(
            SetupPlayerDTO(id = 1, name = "  ana "),
            SetupPlayerDTO(id = 2, name = "Bruno"),
            SetupPlayerDTO(id = 3, name = "Carla")
        )

        val response = SetupController(SetupService(repository)).organize(SetupRequest(players = players))

        val result = (response.body as? SetupResponse) ?: error("Response body is null")
        assertEquals(SetupPlayerDTO(id = 7, name = "Ana"), result.groups.flatten().first())
    }

    @Test
    fun `given 9 or 11 players when organizing then prefers tables of 4 over tables of 5 and 3`() {
        // n=9 should give [5,4] not [3,3,3] (fewer groups, prioritize 4s)
        val request9 = SetupRequest(players = createPlayers(9))
        val response9 = controller.organize(request9)
        val result9 = (response9.body as? SetupResponse) ?: error("Response body is null")
        assertEquals(2, result9.groups.size, "9 players should give 2 groups, not 3")

        // n=11 should give [4,4,3] not [5,3,3] (maximize 4s)
        val request11 = SetupRequest(players = createPlayers(11))
        val response11 = controller.organize(request11)
        val result11 = (response11.body as? SetupResponse) ?: error("Response body is null")
        assertEquals(3, result11.groups.size)
        val count4 = result11.groups.count { it.size == 4 }
        assertEquals(2, count4, "Should have 2 groups of 4 players, not 1 group of 5 and 2 groups of 3")
    }
}

