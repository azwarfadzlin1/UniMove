package com.example.unimove.ui.viewmodel

import com.example.unimove.R

enum class RunnerStage {
    KID, TEEN, ADULT
}

fun runnerStageFromTotalKm(totalKm: Double): RunnerStage {
    return when {
        totalKm >= 20.0 -> RunnerStage.ADULT     // adult ONLY at 20km+
        totalKm >= 5.0  -> RunnerStage.TEEN      // teen for 5km up to 19.99km
        else            -> RunnerStage.KID
    }
}

fun getLottieResource(stage: RunnerStage): Int {
    return when (stage) {
        RunnerStage.KID -> R.raw.run_kid
        RunnerStage.TEEN -> R.raw.run_teen
        RunnerStage.ADULT -> R.raw.run_adult
    }
}

fun getLottieSpeed(stage: RunnerStage): Float {
    return when (stage) {
        RunnerStage.KID -> 1.0f
        RunnerStage.TEEN -> 1.2f
        RunnerStage.ADULT -> 1.35f
    }
}

