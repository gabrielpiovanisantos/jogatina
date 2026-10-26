package com.gabriel.jogatina.repositories

import com.gabriel.jogatina.models.SavedPeriod
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.rest.core.annotation.RepositoryRestResource

// Fora do Spring Data REST: o CRUD passa pelo PeriodController, que valida.
@RepositoryRestResource(exported = false)
interface SavedPeriodRepository : JpaRepository<SavedPeriod, Long> {
    fun findAllByOrderByFromDescIdDesc(): List<SavedPeriod>

    @Query("select p from SavedPeriod p where lower(p.name) = lower(:name)")
    fun findByNameIgnoreCase(name: String): SavedPeriod?
}
