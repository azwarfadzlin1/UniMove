package com.example.unimove.data.model

import com.google.firebase.Timestamp

enum class ChallengeStatus {
    ACTIVE,
    PAUSED,
    COMPLETED
}

data class UserChallengeProgress(
    val challengeId: String = "",
    val accumulatedDistanceKm: Double = 0.0,
    val bestCompletionSeconds: Long? = null,
    val latestCompletionSeconds: Long? = null,
    val totalAttempts: Int = 0,
    val status: ChallengeStatus = ChallengeStatus.ACTIVE,
    val joinedAt: Timestamp? = null,
    val completedAt: Timestamp? = null,
    val lastUpdated: Timestamp? = null
)
