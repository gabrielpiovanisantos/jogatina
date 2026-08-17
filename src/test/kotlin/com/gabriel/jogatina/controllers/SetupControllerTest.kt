package com.gabriel.jogatina.controllers

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.http.HttpStatus
import com.gabriel.jogatina.services.SetupService

class SetupControllerTest {

    private val controller = SetupController(SetupService())

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
    fun testLessThan3PlayersReturnsError() {
        val request1 = SetupRequest(players = createPlayers(1))
        val response1 = controller.organize(request1)
        assertEquals(HttpStatus.BAD_REQUEST, response1.statusCode)

        val request2 = SetupRequest(players = createPlayers(2))
        val response2 = controller.organize(request2)
        assertEquals(HttpStatus.BAD_REQUEST, response2.statusCode)
    }

    @Test
    fun testExactly3Players() {
        val request = SetupRequest(players = createPlayers(3))
        val response = controller.organize(request)
        assertEquals(HttpStatus.OK, response.statusCode)

        val result = (response.body as? SetupResponse) ?: error("Response body is null")
        assertEquals(1, result.groups.size, "Should have 1 group")
        assertEquals(3, result.groups[0].size, "Group should have 3 players")
        validatePartition(result.groups, 3)
    }

    @Test
    fun testExactly4Players() {
        val request = SetupRequest(players = createPlayers(4))
        val response = controller.organize(request)
        assertEquals(HttpStatus.OK, response.statusCode)

        val result = (response.body as? SetupResponse) ?: error("Response body is null")
        assertEquals(1, result.groups.size, "Should have 1 group")
        assertEquals(4, result.groups[0].size, "Group should have 4 players")
        validatePartition(result.groups, 4)
    }

    @Test
    fun testExactly5Players() {
        val request = SetupRequest(players = createPlayers(5))
        val response = controller.organize(request)
        assertEquals(HttpStatus.OK, response.statusCode)

        val result = (response.body as? SetupResponse) ?: error("Response body is null")
        assertEquals(1, result.groups.size, "Should have 1 group")
        assertEquals(5, result.groups[0].size, "Group should have 5 players")
        validatePartition(result.groups, 5)
    }

    @Test
    fun testExactly6Players() {
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
    fun testExactly7Players() {
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
    fun testExactly8Players() {
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
    fun testExactly9Players() {
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
    fun testExactly10Players() {
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
    fun testExactly11Players() {
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
    fun testExactly12Players() {
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
    fun testExactly13Players() {
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
    fun testExactly14Players() {
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
    fun testExactly15Players() {
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
    fun testExactly20Players() {
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
    fun testExactly23Players() {
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
    fun testPlayerOrderPreserved() {
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
    fun testBlankPlayerNameReturnsError() {
        val players = createPlayers(3).toMutableList()
        players[1] = players[1].copy(name = "   ")
        val request = SetupRequest(players = players)

        val response = controller.organize(request)
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun testDuplicatePlayerIdsReturnsError() {
        val players = createPlayers(3).toMutableList()
        players[1] = players[1].copy(id = players[0].id)
        val request = SetupRequest(players = players)

        val response = controller.organize(request)
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun testPreferenceFor4sOver5sAnd3s() {
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

