package com.gabriel.jogatina.controllers

import com.gabriel.jogatina.models.SavedPeriod
import com.gabriel.jogatina.services.SavedPeriodService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.time.Instant

data class PeriodRequest(
    val name: String? = null,
    val from: Instant? = null,
    val to: Instant? = null
)

data class PeriodDTO(
    val id: Long,
    val name: String,
    val from: Instant,
    val to: Instant?
)

private fun SavedPeriod.toDTO() = PeriodDTO(id = id!!, name = name, from = from, to = to)

// "to" é exclusivo, como no filtro dos dashboards: [from, to). Nulo = período em andamento.
@RestController
@RequestMapping("/api/periods")
class PeriodController(private val periodService: SavedPeriodService) {

    @GetMapping
    fun list(): List<PeriodDTO> = periodService.list().map { it.toDTO() }

    @PostMapping
    fun create(@RequestBody request: PeriodRequest): ResponseEntity<PeriodDTO> {
        val period = periodService.create(request.name, request.from, request.to).toDTO()
        return ResponseEntity.created(URI.create("/api/periods/${period.id}")).body(period)
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody request: PeriodRequest): ResponseEntity<PeriodDTO> {
        val period = periodService.update(id, request.name, request.from, request.to)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(period.toDTO())
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> =
        if (periodService.delete(id)) ResponseEntity.noContent().build() else ResponseEntity.notFound().build()

    @ExceptionHandler(IllegalArgumentException::class)
    fun badRequest(e: IllegalArgumentException): ResponseEntity<String> =
        ResponseEntity.badRequest()
            .contentType(MediaType("text", "plain", Charsets.UTF_8))
            .body(e.message ?: "Entrada inválida")
}
