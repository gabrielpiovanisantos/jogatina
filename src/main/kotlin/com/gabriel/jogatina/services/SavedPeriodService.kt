package com.gabriel.jogatina.services

import com.gabriel.jogatina.models.SavedPeriod
import com.gabriel.jogatina.repositories.SavedPeriodRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class SavedPeriodService(private val repository: SavedPeriodRepository) {

    @Transactional(readOnly = true)
    fun list(): List<SavedPeriod> = repository.findAllByOrderByFromDescIdDesc()

    @Transactional
    fun create(name: String?, from: Instant?, to: Instant?): SavedPeriod =
        save(SavedPeriod(name = "", from = Instant.EPOCH), name, from, to)

    /** null se o período não existe. */
    @Transactional
    fun update(id: Long, name: String?, from: Instant?, to: Instant?): SavedPeriod? {
        val period = repository.findById(id).orElse(null) ?: return null
        return save(period, name, from, to)
    }

    /** false se o período não existe. */
    @Transactional
    fun delete(id: Long): Boolean {
        if (!repository.existsById(id)) return false
        repository.deleteById(id)
        return true
    }

    private fun save(period: SavedPeriod, name: String?, from: Instant?, to: Instant?): SavedPeriod {
        val trimmed = name?.trim().orEmpty()
        require(trimmed.isNotEmpty()) { "Nome do período não pode ser vazio" }
        require(trimmed.length <= 100) { "Nome do período pode ter no máximo 100 caracteres" }
        requireNotNull(from) { "'from' é obrigatório" }
        Period(from, to) // valida from < to

        val sameName = repository.findByNameIgnoreCase(trimmed)
        require(sameName == null || sameName.id == period.id) { "Já existe um período com o nome: $trimmed" }

        period.name = trimmed
        period.from = from
        period.to = to
        return try {
            repository.saveAndFlush(period)
        } catch (e: DataIntegrityViolationException) {
            // Outro pedido gravou o mesmo nome entre a checagem e o insert.
            throw IllegalArgumentException("Já existe um período com o nome: $trimmed")
        }
    }
}
