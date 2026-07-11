package com.example.unimove.service

/**
 * Classifies displacement between consecutive good-quality GPS observations.
 * Keeping this independent from Android makes the critical distance rules
 * deterministic and unit-testable.
 */
internal object GpsMovementClassifier {
    fun shouldCountStep(
        distanceMeters: Float,
        elapsedSeconds: Float,
        hasReportedSpeed: Boolean,
        reportedSpeedMps: Float,
        maximumSpeedMps: Float
    ): Boolean {
        if (!distanceMeters.isFinite() || !elapsedSeconds.isFinite()) return false
        if (distanceMeters < MIN_STEP_METERS || elapsedSeconds <= 0f) return false

        val coordinateSpeedMps = distanceMeters / elapsedSeconds
        if (coordinateSpeedMps > maximumSpeedMps) return false

        // A reliable non-zero fused speed permits short, slow walking fixes.
        // Fused location can arrive every 500 ms, where a 3 mph walk moves
        // only about 0.67 m. If the device reports zero (or omits speed),
        // demand stronger coordinate evidence so ordinary stationary drift is
        // not counted.
        val minimumCoordinateSpeed = if (
            hasReportedSpeed && reportedSpeedMps >= REPORTED_MOVING_SPEED_MPS
        ) {
            MIN_SLOW_WALK_SPEED_MPS
        } else {
            MIN_SPEED_WITHOUT_SENSOR_CONFIRMATION_MPS
        }

        return coordinateSpeedMps >= minimumCoordinateSpeed
    }

    private const val MIN_STEP_METERS = 0.3f
    private const val REPORTED_MOVING_SPEED_MPS = 0.45f
    private const val MIN_SLOW_WALK_SPEED_MPS = 0.4f
    private const val MIN_SPEED_WITHOUT_SENSOR_CONFIRMATION_MPS = 0.75f
}

/**
 * Requires consecutive credible steps before declaring movement. Distance from
 * the confirmation steps is held temporarily and returned in full once motion
 * is confirmed, so filtering isolated spikes does not undercount real walking.
 */
internal class GpsMovementAccumulator {
    private var consecutiveSteps = 0
    private var pendingDistanceMeters = 0f
    private var movementConfirmed = false

    fun addStep(
        distanceMeters: Float,
        elapsedSeconds: Float,
        hasReportedSpeed: Boolean,
        reportedSpeedMps: Float,
        maximumSpeedMps: Float
    ): Float {
        val credible = GpsMovementClassifier.shouldCountStep(
            distanceMeters = distanceMeters,
            elapsedSeconds = elapsedSeconds,
            hasReportedSpeed = hasReportedSpeed,
            reportedSpeedMps = reportedSpeedMps,
            maximumSpeedMps = maximumSpeedMps
        )

        if (!credible) {
            reset()
            return 0f
        }

        if (movementConfirmed) return distanceMeters

        consecutiveSteps++
        pendingDistanceMeters += distanceMeters
        if (consecutiveSteps < REQUIRED_CONSECUTIVE_STEPS) return 0f

        movementConfirmed = true
        return pendingDistanceMeters.also { pendingDistanceMeters = 0f }
    }

    fun reset() {
        consecutiveSteps = 0
        pendingDistanceMeters = 0f
        movementConfirmed = false
    }

    private companion object {
        const val REQUIRED_CONSECUTIVE_STEPS = 2
    }
}
