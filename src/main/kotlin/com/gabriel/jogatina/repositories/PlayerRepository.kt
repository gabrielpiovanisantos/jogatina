package com.gabriel.jogatina.repositories

import com.gabriel.jogatina.models.Player
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.rest.core.annotation.RepositoryRestResource

@RepositoryRestResource(collectionResourceRel = "players", path = "players")
interface PlayerRepository : JpaRepository<Player, Long> {
    fun findByNameIgnoreCase(name: String): Player?
}