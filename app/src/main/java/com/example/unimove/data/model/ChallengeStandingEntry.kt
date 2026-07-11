package com.example.unimove.data.model

import com.google.firebase.Timestamp

data class ChallengeStandingEntry(
    val userId: String = "",
    val name: String = "Runner",
    val bestCompletionSeconds: Long = 0L,
    val latestCompletionSeconds: Long = 0L,
    val attemptCount: Int = 0,
    val completedAt: Timestamp? = null
)

data class ChallengeCompletionResult(
    val challengeId: String = "",
    val challengeTitle: String = "",
    val targetDistanceKm: Double = 0.0,
    val finishSeconds: Int = 0,
    val personalBestSeconds: Long? = null,
    val improvedPersonalBest: Boolean = false,
    val rank: Int = 0,
    val totalParticipants: Int = 0,
    val topStandings: List<ChallengeStandingEntry> = emptyList()
)
