package com.example.unimove.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimove.wear.WatchSessionRepository
import com.example.unimove.wear.WatchSessionStatus
import com.example.unimove.wear.WatchSessionUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel that exposes live watch session data to the Insights (StatsScreen) UI.
 * Uses [AndroidViewModel] to safely access [Application] context for the singleton repository.
 */
class WatchSessionViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = WatchSessionRepository.getInstance(app)

    /**
     * Latest phone-side session state from the watch channel.
     */
    val sessionState: StateFlow<WatchSessionUiState?> = repository.sessionState

    /**
     * Active and fresh watch session only.
     * Completed or stale sessions are intentionally filtered out of the live UI.
     */
    val liveSession: StateFlow<WatchSessionUiState?> = repository.sessionState
        .map { state -> state?.takeIf { it.status == WatchSessionStatus.ACTIVE } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )
}
