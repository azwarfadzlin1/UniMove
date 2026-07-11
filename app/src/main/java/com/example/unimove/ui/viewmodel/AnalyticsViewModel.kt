package com.example.unimove.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

private val malaysiaZoneId: ZoneId = ZoneId.of("Asia/Kuala_Lumpur")
private val dayLabelFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE")

enum class HeatmapLevel {
    NONE,
    LIGHT_RUN,
    STRONG_RUN
}

data class LifetimeAnalyticsSummary(
    val totalRuns: Int = 0,
    val totalDistanceKm: Double = 0.0,
    val totalDurationSec: Long = 0L,
    val totalCaloriesBurned: Int = 0,
    val avgPaceSecPerKm: Double = 0.0,
    val longestRunKm: Double = 0.0,
    val activeRunDays: Int = 0,
    val daysSinceJoined: Long = 0
)

data class DistanceTrendDay(
    val date: LocalDate,
    val label: String,
    val distanceKm: Double,
    val burnedCalories: Int,
    val sessions: Int
)

data class PaceProgressPoint(
    val date: LocalDate,
    val paceSecPerKm: Int
)

data class HeatmapCell(
    val date: LocalDate? = null,
    val level: HeatmapLevel = HeatmapLevel.NONE
)

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val summary: LifetimeAnalyticsSummary = LifetimeAnalyticsSummary(),
    val distanceTrendLast7Days: List<DistanceTrendDay> = emptyList(),
    val paceProgression: List<PaceProgressPoint> = emptyList(),
    val paceImprovementSecPerRun: Double = 0.0,
    val heatmapMonthLabel: String = "",
    val heatmapCells: List<HeatmapCell> = emptyList(),
    val personalBests: PersonalBests = PersonalBests(),
    val activityTimeOfDay: List<TimeOfDayStat> = emptyList()
)

data class PersonalBests(
    val longestRunKm: Double = 0.0,
    val fastestPaceSecPerKm: Int = 0,
    val mostCaloriesBurned: Int = 0
)

data class TimeOfDayStat(
    val label: String,
    val count: Int
)

private data class SessionRecord(
    val timestamp: Instant,
    val date: LocalDate,
    val distanceKm: Double,
    val durationSec: Int,
    val paceSecPerKm: Int,
    val caloriesBurned: Int
)

class AnalyticsViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _uiState.value = AnalyticsUiState(
                isLoading = false,
                errorMessage = "User not logged in"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                _uiState.value = loadAnalytics(uid = uid)
            } catch (e: Exception) {
                _uiState.value = AnalyticsUiState(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load analytics"
                )
            }
        }
    }

    private suspend fun loadAnalytics(uid: String): AnalyticsUiState {
        val userDoc = firestore.collection("users").document(uid).get().await()
        val weightKg = (userDoc.getDouble("weight") ?: 65.0).coerceAtLeast(35.0)
        val createdAt = userDoc.getTimestamp("createdAt")

        val sessionsSnapshot = firestore.collection("users").document(uid)
            .collection("sessions")
            .get()
            .await()

        val sessionRecords = sessionsSnapshot.documents.mapNotNull { doc ->
            val timestamp = doc.getTimestamp("timestamp")?.toDate()?.toInstant() ?: return@mapNotNull null
            val distanceKm = (doc.getDouble("distanceKm") ?: 0.0).coerceAtLeast(0.0)
            val durationSec = (doc.getLong("durationSeconds") ?: 0L).toInt().coerceAtLeast(0)
            val paceSec = parsePaceSec(doc.getString("avgPace"), distanceKm, durationSec)

            SessionRecord(
                timestamp = timestamp,
                date = timestamp.atZone(malaysiaZoneId).toLocalDate(),
                distanceKm = distanceKm,
                durationSec = durationSec,
                paceSecPerKm = paceSec,
                caloriesBurned = estimateRunningCaloriesBurned(
                    weightKg = weightKg,
                    distanceKm = distanceKm,
                    durationSec = durationSec
                )
            )
        }

        val today = LocalDate.now(malaysiaZoneId)
        val createdDate = createdAt?.toDate()?.toInstant()?.atZone(malaysiaZoneId)?.toLocalDate()
        val daysSinceJoined = if (createdDate != null) {
            ChronoUnit.DAYS.between(createdDate, today).coerceAtLeast(0) + 1
        } else {
            0L
        }

        val totalDistanceKm = sessionRecords.sumOf { it.distanceKm }
        val totalDurationSec = sessionRecords.sumOf { it.durationSec.toLong() }
        val totalCaloriesBurned = sessionRecords.sumOf { it.caloriesBurned }
        val longestRunKm = sessionRecords.maxOfOrNull { it.distanceKm } ?: 0.0
        val avgPaceSecPerKm = if (totalDistanceKm > 0.0) {
            totalDurationSec / totalDistanceKm
        } else {
            0.0
        }

        val summary = LifetimeAnalyticsSummary(
            totalRuns = sessionRecords.size,
            totalDistanceKm = totalDistanceKm,
            totalDurationSec = totalDurationSec,
            totalCaloriesBurned = totalCaloriesBurned,
            avgPaceSecPerKm = avgPaceSecPerKm,
            longestRunKm = longestRunKm,
            activeRunDays = sessionRecords.map { it.date }.distinct().size,
            daysSinceJoined = daysSinceJoined
        )

        val personalBests = PersonalBests(
            longestRunKm = longestRunKm,
            fastestPaceSecPerKm = sessionRecords.filter { it.paceSecPerKm > 0 }.minOfOrNull { it.paceSecPerKm } ?: 0,
            mostCaloriesBurned = sessionRecords.maxOfOrNull { it.caloriesBurned } ?: 0
        )

        val morningCount = sessionRecords.count { it.timestamp.atZone(malaysiaZoneId).hour in 5..11 }
        val afternoonCount = sessionRecords.count { it.timestamp.atZone(malaysiaZoneId).hour in 12..16 }
        val eveningCount = sessionRecords.count { it.timestamp.atZone(malaysiaZoneId).hour in 17..20 }
        val nightCount = sessionRecords.count { it.timestamp.atZone(malaysiaZoneId).hour !in 5..20 }

        val activityTimeOfDay = listOf(
            TimeOfDayStat("Morning", morningCount),
            TimeOfDayStat("Afternoon", afternoonCount),
            TimeOfDayStat("Evening", eveningCount),
            TimeOfDayStat("Night", nightCount)
        )

        val last7Dates = (6 downTo 0).map { offset -> today.minusDays(offset.toLong()) }
        val distanceByDate = sessionRecords.groupBy { it.date }.mapValues { entry ->
            entry.value.sumOf { it.distanceKm }
        }
        val burnedByDate = sessionRecords.groupBy { it.date }.mapValues { entry ->
            entry.value.sumOf { it.caloriesBurned }
        }
        val sessionsByDate = sessionRecords.groupBy { it.date }.mapValues { entry ->
            entry.value.size
        }
        val distanceTrendDays = last7Dates.map { date ->
            val distanceKm = distanceByDate[date] ?: 0.0
            val burned = burnedByDate[date] ?: 0
            DistanceTrendDay(
                date = date,
                label = date.format(dayLabelFormatter),
                distanceKm = distanceKm,
                burnedCalories = burned,
                sessions = sessionsByDate[date] ?: 0
            )
        }

        val paceProgression = sessionRecords
            .sortedByDescending { it.timestamp }
            .take(10)
            .reversed()
            .map {
                PaceProgressPoint(
                    date = it.date,
                    paceSecPerKm = it.paceSecPerKm
                )
            }

        val paceImprovementSecPerRun = if (paceProgression.size >= 2) {
            val oldest = paceProgression.first().paceSecPerKm
            val latest = paceProgression.last().paceSecPerKm
            (oldest - latest).toDouble() / (paceProgression.size - 1).toDouble()
        } else {
            0.0
        }

        val monthStart = today.withDayOfMonth(1)
        val monthEnd = today.withDayOfMonth(today.lengthOfMonth())
        val monthLabel = monthStart.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        val runDistanceByDateThisMonth = sessionRecords
            .asSequence()
            .filter { !it.date.isBefore(monthStart) && !it.date.isAfter(monthEnd) }
            .groupBy { it.date }
            .mapValues { (_, sessions) -> sessions.sumOf { it.distanceKm } }

        val firstDayOffset = monthStart.dayOfWeek.value - 1 // Monday=0
        val heatmapCells = mutableListOf<HeatmapCell>()
        repeat(firstDayOffset) { heatmapCells += HeatmapCell() }
        for (day in 1..monthStart.lengthOfMonth()) {
            val date = monthStart.withDayOfMonth(day)
            val distanceKm = runDistanceByDateThisMonth[date] ?: 0.0

            val level = when {
                distanceKm >= 5.0 -> HeatmapLevel.STRONG_RUN
                distanceKm > 0.0 -> HeatmapLevel.LIGHT_RUN
                else -> HeatmapLevel.NONE
            }
            heatmapCells += HeatmapCell(date = date, level = level)
        }
        while (heatmapCells.size % 7 != 0) {
            heatmapCells += HeatmapCell()
        }

        return AnalyticsUiState(
            isLoading = false,
            summary = summary,
            distanceTrendLast7Days = distanceTrendDays,
            paceProgression = paceProgression,
            paceImprovementSecPerRun = paceImprovementSecPerRun,
            heatmapMonthLabel = monthLabel,
            heatmapCells = heatmapCells,
            personalBests = personalBests,
            activityTimeOfDay = activityTimeOfDay
        )
    }

    private fun parsePaceSec(avgPace: String?, distanceKm: Double, durationSec: Int): Int {
        val parsed = avgPace
            ?.substringBefore(" ")
            ?.split(":")
            ?.takeIf { it.size == 2 }
            ?.let { parts ->
                val mins = parts[0].toIntOrNull() ?: return@let null
                val secs = parts[1].toIntOrNull() ?: return@let null
                mins * 60 + secs
            }
        if (parsed != null && parsed > 0) return parsed

        if (distanceKm > 0.0 && durationSec > 0) {
            return (durationSec / distanceKm).roundToInt()
        }
        return 0
    }

    private fun estimateRunningCaloriesBurned(weightKg: Double, distanceKm: Double, durationSec: Int): Int {
        if (distanceKm <= 0.0 || durationSec <= 0) return 0
        val hours = durationSec / 3600.0
        if (hours <= 0.0) return 0

        val speedKmH = distanceKm / hours
        val met = when {
            speedKmH < 8.0 -> 8.3
            speedKmH < 9.7 -> 9.8
            speedKmH < 11.3 -> 11.0
            speedKmH < 12.1 -> 11.8
            speedKmH < 12.9 -> 12.8
            else -> 14.5
        }
        return (met * weightKg * hours).roundToInt().coerceAtLeast(0)
    }
}
