package com.gabriel.jogatina.controllers

import com.gabriel.jogatina.repositories.PlayerRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class PlayerDTO(
    val id: Long,
    val name: String
)

@RestController
@RequestMapping("/api/players")
class PlayerController(private val playerRepository: PlayerRepository) {

    @GetMapping
    fun list(): List<PlayerDTO> {
        return playerRepository.findAll()
            .map { PlayerDTO(id = it.id!!, name = it.name) }
            .sortedBy { it.name.lowercase() }
    }
}
