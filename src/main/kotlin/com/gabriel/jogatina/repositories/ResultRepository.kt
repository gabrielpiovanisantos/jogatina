package com.gabriel.jogatina.repositories

import com.gabriel.jogatina.models.GameResult
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import org.springframework.data.rest.core.annotation.RestResource

@RepositoryRestResource(collectionResourceRel = "gameResults", path = "gameResults")
interface ResultRepository : JpaRepository<GameResult, Long> {
    @RestResource(path = "winners", rel = "winners")
    fun findByWonTrue(): List<GameResult>
}

