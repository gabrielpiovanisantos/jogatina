package com.gabriel.jogatina.controllers

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import com.gabriel.jogatina.services.SetupService

data class SetupPlayerDTO(
    val id: Long? = null,
    val name: String
)

data class SetupRequest(
    val players: List<SetupPlayerDTO>
)

data class SetupResponse(
    val groups: List<List<SetupPlayerDTO>>
)

@RestController
@RequestMapping("/api/setup")
class SetupController(private val setupService: SetupService) {

    @PostMapping("/organize")
    fun organize(@RequestBody request: SetupRequest): ResponseEntity<Any> {
        val players = request.players
        return try {
            val groups = setupService.organize(players)
            ResponseEntity.ok(SetupResponse(groups = groups))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(e.message ?: "Entrada inválida")
        } catch (e: Exception) {
            ResponseEntity.status(500).body("Erro ao organizar mesas: ${e.message}")
        }
    }
}

