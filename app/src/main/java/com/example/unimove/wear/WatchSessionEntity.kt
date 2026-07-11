package com.example.unimove.wear

/**
 * Plain data class representing a single watch session snapshot stored in SQLite.
 * No Room annotations — the schema is managed manually in [WatchDatabase].
 */
data class WatchSessionEntity(
    val sessionId: Long,
    val timestamp: Long,
    val sequenceNumber: Long,
    val elapsedMs: Long,
    val distanceMeters: Double,
    val steps: Long,
    val heartRate: Int,
    val paceMsPerKm: Long,
    val isEnded: Boolean,
    val isSynced: Boolean = false,
)
