package com.gabriel.jogatina.repositories

import com.gabriel.jogatina.models.GameResult
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import org.springframework.data.rest.core.annotation.RestResource
import java.time.Instant

@RepositoryRestResource(collectionResourceRel = "gameResults", path = "gameResults")
interface ResultRepository : JpaRepository<GameResult, Long> {
    @RestResource(path = "winners", rel = "winners")
    fun findByWonTrue(): List<GameResult>

    // O cast é necessário: sem ele o Postgres não descobre o tipo de um parâmetro nulo em "? is null".
    /** Resultados das partidas com data em [from, to); um limite nulo fica aberto. */
    @RestResource(exported = false)
    @Query(
        """
        select r from GameResult r
        join fetch r.game g
        join fetch r.player
        where (cast(:from as Instant) is null or g.date >= :from)
          and (cast(:to as Instant) is null or g.date < :to)
        """
    )
    fun findInPeriod(from: Instant?, to: Instant?): List<GameResult>
}

