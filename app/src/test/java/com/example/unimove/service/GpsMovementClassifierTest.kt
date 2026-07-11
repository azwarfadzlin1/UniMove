package com.example.unimove.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GpsMovementClassifierTest {
    @Test
    fun stationaryDriftIsRejected() {
        assertFalse(classify(distanceMeters = 6f, seconds = 10f, speed = 0f))
    }

    @Test
    fun thirteenMinuteWalkingPaceIsCounted() {
        // 1 km / 13 min = 1.282 m/s; roughly 6.41 m per five-second fix.
        assertTrue(classify(distanceMeters = 6.41f, seconds = 5f, speed = 1.28f))
    }

    @Test
    fun threeMphHalfSecondWalkingFixIsCounted() {
        // 3 mph = 1.34 m/s; at the service's 500 ms minimum update interval
        // that is only about 0.67 m per fix.
        assertTrue(classify(distanceMeters = 0.67f, seconds = 0.5f, speed = 1.34f))
    }

    @Test
    fun twoMphHalfSecondWalkingFixIsCounted() {
        // 2 mph = 0.89 m/s; still valid walking even though each fix is small.
        assertTrue(classify(distanceMeters = 0.45f, seconds = 0.5f, speed = 0.89f))
    }

    @Test
    fun walkingStillCountsWhenFusedSpeedIsMissing() {
        assertTrue(
            GpsMovementClassifier.shouldCountStep(
                distanceMeters = 6.41f,
                elapsedSeconds = 5f,
                hasReportedSpeed = false,
                reportedSpeedMps = 0f,
                maximumSpeedMps = 8f
            )
        )
    }

    @Test
    fun slowWalkingStillCountsWhenFusedSpeedIsMissing() {
        assertTrue(
            GpsMovementClassifier.shouldCountStep(
                distanceMeters = 4.47f,
                elapsedSeconds = 5f,
                hasReportedSpeed = false,
                reportedSpeedMps = 0f,
                maximumSpeedMps = 8f
            )
        )
    }

    @Test
    fun sparseWalkingFixIsCounted() {
        assertTrue(classify(distanceMeters = 12.82f, seconds = 10f, speed = 1.28f))
    }

    @Test
    fun teleportIsRejected() {
        assertFalse(classify(distanceMeters = 100f, seconds = 5f, speed = 0f))
    }

    @Test
    fun isolatedStationarySpeedSpikesAddNoDistance() {
        val accumulator = GpsMovementAccumulator()
        var total = 0f
        total += accumulator.addStep(4.54f, 0.79f, true, 2.60f, 8f)
        total += accumulator.addStep(0.69f, 9.33f, true, 0.15f, 8f)
        total += accumulator.addStep(2.19f, 0.50f, true, 1.16f, 8f)
        total += accumulator.addStep(0.43f, 9.62f, true, 0.07f, 8f)
        total += accumulator.addStep(2.02f, 0.83f, true, 1.19f, 8f)
        assertTrue(total == 0f)
    }

    @Test
    fun confirmedThirteenMinutePaceRecoversFullKilometre() {
        val accumulator = GpsMovementAccumulator()
        var total = 0f
        repeat(156) {
            total += accumulator.addStep(6.41f, 5f, true, 1.28f, 8f)
        }
        assertTrue(total in 999f..1001f)
    }

    @Test
    fun confirmedThreeMphHalfSecondFixesRecoverDistance() {
        val accumulator = GpsMovementAccumulator()
        var total = 0f
        repeat(150) {
            total += accumulator.addStep(0.67f, 0.5f, true, 1.34f, 8f)
        }
        assertTrue(total in 100f..101f)
    }

    private fun classify(distanceMeters: Float, seconds: Float, speed: Float) =
        GpsMovementClassifier.shouldCountStep(
            distanceMeters = distanceMeters,
            elapsedSeconds = seconds,
            hasReportedSpeed = true,
            reportedSpeedMps = speed,
            maximumSpeedMps = 8f
        )
}
