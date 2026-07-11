package com.example.unimove.wear

object DataLayerContracts {
    const val LIVE_RUN_PATH = "/unimove/live_run"
    const val SESSION_SUMMARY_ROOT = "/unimove/watch_session" // Corrected to match Watch app
    const val SESSION_SUMMARY_ALT = "/unimove/run_session"   // Keeping as fallback
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
}
