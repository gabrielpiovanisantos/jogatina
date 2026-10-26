package com.gabriel.jogatina.models

import jakarta.persistence.*
import java.time.Instant

/** Período com nome salvo (ex.: uma temporada); [to] nulo é um período em andamento. */
@Entity
@Table(name = "periods")
class SavedPeriod(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false, length = 100)
    var name: String,
    @Column(name = "from_date", nullable = false)
    var from: Instant,
    @Column(name = "to_date")
    var to: Instant? = null
)
