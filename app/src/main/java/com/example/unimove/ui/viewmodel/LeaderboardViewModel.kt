package com.example.unimove.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimove.data.model.WeeklyLeaderboardEntry
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LeaderboardUiState(
    val isLoading: Boolean = true,
    val weekKey: String = currentWeekKey(),
    val countdownText: String = weeklyResetCountdownText(),
    val entries: List<WeeklyLeaderboardEntry> = emptyList(),
    val currentUserMessage: String? = null,
    val currentUserEntry: WeeklyLeaderboardEntry? = null
) {
    val topThree: List<WeeklyLeaderboardEntry> get() = entries.take(3)
    val remainingEntries: List<WeeklyLeaderboardEntry> get() = entries.drop(3)
    val isEmpty: Boolean get() = !isLoading && entries.isEmpty()
}

class LeaderboardViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    private var leaderboardListener: ListenerRegistration? = null

    init {
        observeCurrentWeekLeaderboard()
        startCountdownTicker()
    }

    private fun observeCurrentWeekLeaderboard() {
        val weekKey = currentWeekKey()
        _uiState.value = _uiState.value.copy(isLoading = true, weekKey = weekKey)

        leaderboardListener?.remove()
        leaderboardListener = firestore.collection("weekly_leaderboard")
            .whereEqualTo("weekKey", weekKey)
            .orderBy("weeklyDistance", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, entries = emptyList())
                    return@addSnapshotListener
                }

                val currentUid = auth.currentUser?.uid
                val entries = snapshot.documents.mapIndexed { index, doc ->
                    WeeklyLeaderboardEntry(
                        id = doc.id,
                        userId = doc.getString("userId").orEmpty(),
                        name = doc.getString("name").orEmpty().ifBlank { "Runner" },
                        weeklyDistance = doc.getDouble("weeklyDistance") ?: 0.0,
                        weeklySessions = doc.getLong("weeklySessions")?.toInt() ?: 0,
                        weekKey = doc.getString("weekKey").orEmpty().ifBlank { weekKey },
                        updatedAt = doc.getTimestamp("updatedAt"),
                        rank = index + 1,
                        isCurrentUser = doc.getString("userId") == currentUid
                    )
                }

                val currentUserEntry = entries.firstOrNull { it.isCurrentUser }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    entries = entries,
                    currentUserEntry = currentUserEntry,
                    currentUserMessage = buildCurrentUserMessage(entries, currentUserEntry)
                )
            }
    }

    private fun startCountdownTicker() {
        viewModelScope.launch {
            while (true) {
                _uiState.value = _uiState.value.copy(countdownText = weeklyResetCountdownText())
                delay(60_000L)
            }
        }
    }

    private fun buildCurrentUserMessage(
        entries: List<WeeklyLeaderboardEntry>,
        currentUserEntry: WeeklyLeaderboardEntry?
    ): String? {
        if (currentUserEntry == null) return null
        if (currentUserEntry.rank <= 1) {
            return "You are leading this week. Keep the pace going."
        }

        val nextEntry = entries.getOrNull(currentUserEntry.rank - 2) ?: return null
        val distanceBehind = (nextEntry.weeklyDistance - currentUserEntry.weeklyDistance)
            .coerceAtLeast(0.0)

        if (distanceBehind <= 0.0) {
            return "You are tied with the next rank. One more run can move you up."
        }

        return "You are ${String.format("%.2f", distanceBehind)} km away from overtaking ${nextEntry.name}"
    }

    override fun onCleared() {
        leaderboardListener?.remove()
        super.onCleared()
    }
}
