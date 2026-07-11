package com.example.unimove_wearable.tracking

object DataLayerContracts {
    const val LIVE_RUN_PATH = "/unimove/live_run"
    const val SESSION_SUMMARY_ROOT = "/unimove/watch_session"
    const val PHONE_ACK_PATH = "/unimove/phone_ack"
    const val PHONE_COMMAND_PATH = "/unimove/phone_command"

    const val KEY_SESSION_ID = "sessionId"
    const val KEY_TIMESTAMP = "timestamp"
    const val KEY_ELAPSED_MS = "elapsedMs"
    const val KEY_DISTANCE_METERS = "distanceMeters"
    const val KEY_STEPS = "steps"
    const val KEY_HEART_RATE = "heartRate"
    const val KEY_PACE_MS_PER_KM = "paceMsPerKm"
    const val KEY_IS_ENDED = "isEnded"
    const val KEY_SEQUENCE_NUMBER = "sequenceNumber"
    const val KEY_ACCEPTED = "accepted"
    const val KEY_COMMAND = "command"
    const val KEY_DELIVERY_ATTEMPT = "deliveryAttempt"

    const val COMMAND_START = "START"
    const val COMMAND_PAUSE = "PAUSE"
    const val COMMAND_RESUME = "RESUME"
    const val COMMAND_STOP = "STOP"
    const val COMMAND_SYNC_STATUS = "SYNC_STATUS"

    fun sessionSummaryPath(sessionId: Long): String = "$SESSION_SUMMARY_ROOT/$sessionId"
}
