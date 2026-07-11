package com.example.unimove.wear

/**
 * Mirrors the DataMap payload sent by the Wear OS watch app via the Data Layer API.
 * Keys must match DataLayerContracts on the watch side.
 */
data class WatchSessionUpdate(
    val sessionId: Long,
    val timestamp: Long,
    val sequenceNumber: Long,
    val elapsedMs: Long,
    val distanceMeters: Double,
    val steps: Long,
    val heartRate: Int,
    val paceMsPerKm: Long,
    val isEnded: Boolean,
)
