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


/** Ranking de um jogador com o desempenho em cada facção que ele usou. */
data class PlayerRankingDetailDTO(
    val playerId: Long,
    val playerName: String,
    val totalVictories: Int,
    val totalGames: Int,
    val winPercentage: Double,
    val differentFactions: Int,
    val totalScore: Int,
    val factions: List<PlayerFactionStatsDTO>
)

data class PlayerFactionStatsDTO(
    val faction: String,
    val totalGames: Int,
    val totalVictories: Int,
    val winPercentage: Double,
    val totalScore: Int,
    val averageScore: Double
)
