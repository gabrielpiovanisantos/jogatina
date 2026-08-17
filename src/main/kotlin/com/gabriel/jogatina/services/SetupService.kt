package com.gabriel.jogatina.services

import com.gabriel.jogatina.controllers.SetupPlayerDTO
import org.springframework.stereotype.Service

@Service
class SetupService {

    fun organize(players: List<SetupPlayerDTO>): List<List<SetupPlayerDTO>> {
        val n = players.size
        require(n >= 3) { "É necessário pelo menos 3 jogadores para organizar mesas" }
        require(players.all { it.name.isNotBlank() }) { "Nome de jogador não pode ser vazio" }

        val nonNullIds = players.mapNotNull { it.id }
        require(nonNullIds.size == nonNullIds.distinct().size) { "IDs de jogadores duplicados" }

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


