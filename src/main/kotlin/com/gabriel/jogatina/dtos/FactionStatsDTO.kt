package com.gabriel.jogatina.dtos

data class FactionStatsDTO(
    val faction: String,
    val totalGames: Int,
    val totalVictories: Int,
    val winPercentage: Double,
    val totalScore: Int,
    val averageScore: Double,
    val distinctPlayers: Int
)

data class FactionPlayerStatsDTO(
    val playerId: Long,
    val playerName: String,
    val totalGames: Int,
    val totalVictories: Int,
    val winPercentage: Double,
    val totalScore: Int,
    val averageScore: Double
)

data class FactionDetailDTO(
    val faction: String,
    val totalGames: Int,
    val totalVictories: Int,
    val winPercentage: Double,
    val totalScore: Int,
    val averageScore: Double,
    val distinctPlayers: Int,
    val players: List<FactionPlayerStatsDTO>
)
