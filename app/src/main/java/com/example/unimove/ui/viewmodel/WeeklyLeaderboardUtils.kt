package com.example.unimove.ui.viewmodel

import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.WeekFields
import java.util.Locale

fun currentWeekKey(zoneId: ZoneId = ZoneId.systemDefault()): String {
    val now = ZonedDateTime.now(zoneId)
    val weekFields = WeekFields.ISO
    val weekBasedYear = now.get(weekFields.weekBasedYear())
    val weekOfYear = now.get(weekFields.weekOfWeekBasedYear())
    return "%04d-W%02d".format(Locale.US, weekBasedYear, weekOfYear)
}

fun weeklyLeaderboardDocumentId(uid: String, weekKey: String = currentWeekKey()): String {
    return "${uid}_${weekKey}"
}

fun weeklyResetCountdownText(zoneId: ZoneId = ZoneId.systemDefault()): String {
    val now = ZonedDateTime.now(zoneId)
    val nextReset = now.toLocalDate()
        .plusDays((8 - now.dayOfWeek.value).toLong())
        .atStartOfDay(zoneId)

    val duration = Duration.between(now, nextReset).coerceAtLeast(Duration.ZERO)
    val days = duration.toDays()
    val hours = duration.minusDays(days).toHours()

    return "Resets in ${days}d ${hours}h"
}

fun formatLeaderboardDistance(distanceKm: Double): String {
    return String.format(Locale.US, "%.2f km", distanceKm)
}
