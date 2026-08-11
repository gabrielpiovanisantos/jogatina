package com.gabriel.jogatina.models

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "gameResults")
class Game(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false)
    var date: Instant,
    @OneToMany(mappedBy = "game", cascade = [CascadeType.ALL], orphanRemoval = true)
    var gameResults: MutableList<GameResult> = mutableListOf()
) {
    @PrePersist
    @PreUpdate
    fun syncRelationship() {
        gameResults.forEach { it.game = this }
    }
}
