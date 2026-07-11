package com.example.unimove.wear

enum class WatchSessionStatus {
    ACTIVE,
    COMPLETED,
    STALE
}

data class WatchSessionUiState(
    val session: WatchSessionUpdate,
    val status: WatchSessionStatus,
    val lastUpdatedAtMillis: Long
)
