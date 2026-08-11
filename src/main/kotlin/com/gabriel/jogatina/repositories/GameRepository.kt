package com.gabriel.jogatina.repositories

import com.gabriel.jogatina.models.Game
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.rest.core.annotation.RepositoryRestResource

@RepositoryRestResource(collectionResourceRel = "games", path = "games")
interface GameRepository : JpaRepository<Game, Long>
