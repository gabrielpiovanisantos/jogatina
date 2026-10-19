package com.gabriel.jogatina.services

import com.gabriel.jogatina.controllers.SetupPlayerDTO
import com.gabriel.jogatina.models.Player
import com.gabriel.jogatina.repositories.PlayerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SetupService(private val playerRepository: PlayerRepository) {

    @Transactional
    fun organize(requested: List<SetupPlayerDTO>): List<List<SetupPlayerDTO>> {
        val n = requested.size
        require(n >= 3) { "É necessário pelo menos 3 jogadores para organizar mesas" }
        require(requested.all { it.name.isNotBlank() }) { "Nome de jogador não pode ser vazio" }

        val names = requested.map { it.name.trim() }
        require(names.size == names.distinctBy { it.lowercase() }.size) { "Jogador repetido na lista" }

        // Players are identified by name: the ids sent by the frontend are temporary,
        // so each name is looked up and registered on first use to get its real id.
        val players = names.map { name ->
            val player = playerRepository.findByNameIgnoreCase(name) ?: playerRepository.save(Player(name = name))
            SetupPlayerDTO(id = player.id, name = player.name)
        }

        // First compute the sizes of each group according to the strategy
        val sizes = mutableListOf<Int>()
        when (n % 4) {
            0 -> {
                val count4 = n / 4
                repeat(count4) { sizes.add(4) }
            }
            1 -> {
                val count4 = (n - 5) / 4
                repeat(count4) { sizes.add(4) }
                sizes.add(5)
            }
            2 -> {
                if (n == 6) {
                    sizes.add(3); sizes.add(3)
                } else {
                    val count4 = (n - 10) / 4
                    repeat(count4) { sizes.add(4) }
                    sizes.add(5); sizes.add(5)
                }
            }
            3 -> {
                val count4 = (n - 3) / 4
                repeat(count4) { sizes.add(4) }
                sizes.add(3)
            }
        }

        // Build groups preserving order
        val groups = mutableListOf<List<SetupPlayerDTO>>()
        var start = 0
        for (s in sizes) {
            groups.add(players.subList(start, start + s))
            start += s
        }

        return groups.sortedByDescending { it.size }
    }
}


