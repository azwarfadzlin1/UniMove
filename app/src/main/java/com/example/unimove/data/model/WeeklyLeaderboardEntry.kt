package com.example.unimove.data.model

import com.google.firebase.Timestamp

data class WeeklyLeaderboardEntry(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val weeklyDistance: Double = 0.0,
    val weeklySessions: Int = 0,
    val weekKey: String = "",
    val updatedAt: Timestamp? = null,
    val rank: Int = 0,
    val isCurrentUser: Boolean = false
)
