package com.gabriel.jogatina.dtos

data class PlayerRankingDTO(
    val playerId: Long,
    val playerName: String,
    val totalVictories: Int,
    val totalGames: Int,
    val winPercentage: Double,
    val differentFactions: Int,
    val totalScore: Int
)

