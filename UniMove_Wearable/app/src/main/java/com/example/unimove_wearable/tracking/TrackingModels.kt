package com.example.unimove_wearable.tracking

enum class TrackingPhase {
    IDLE,
    STARTING,
    RUNNING,
    PAUSED,
    STOPPING,
    SUMMARY,
    ERROR,
}

enum class SyncStatus {
    NONE,
    PENDING,
    RETRYING,
    SYNCED,
    FAILED,
}

data class RunSummary(
    val sessionId: Long,
    val sequenceNumber: Long,
    val durationMs: Long,
    val distanceMeters: Double,
    val steps: Long,
    val averageHeartRate: Int?,
    val paceMsPerKm: Long,
    val timestamp: Long,
)

data class TrackingUiState(
    val phase: TrackingPhase = TrackingPhase.IDLE,
    val capabilitiesChecked: Boolean = false,
    val isSupported: Boolean = false,
    val isPhoneConnected: Boolean = false,
    val distanceMeters: Double = 0.0,
    val steps: Long = 0,
    val heartRate: Int? = null,
    val activeDurationBaseMs: Long = 0L,
    val activeDurationCheckpointEpochMs: Long? = null,
    val lastSyncEpochMs: Long? = null,
    val hasPendingSync: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.NONE,
    val lastCompletedSessionId: Long? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val summary: RunSummary? = null,
)
