package com.example.unimove.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimove.data.meal.MealLogRepository
import com.example.unimove.data.model.DailyMealSummary
import com.example.unimove.data.model.FoodSearchResult
import com.example.unimove.data.model.MealEntryDraft
import com.example.unimove.data.model.MealLogEntry
import com.example.unimove.data.model.MealType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeCalorieCardUiState(
    val isLoading: Boolean = true,
    val todayDateKey: String = malaysiaTodayDateKey(),
    val todayCalories: Int = 0,
    val totalItems: Int = 0,
    val errorMessage: String? = null
)

data class MealLogUiState(
    val selectedDateKey: String = malaysiaTodayDateKey(),
    val displayDateLabel: String = formatMealDateLabel(malaysiaTodayDateKey()),
    val calendarStrip: List<DateItem> = getCalendarStrip(malaysiaTodayDateKey()),
    val summary: DailyMealSummary = DailyMealSummary(dateKey = malaysiaTodayDateKey()),
    val entries: List<MealLogEntry> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSearching: Boolean = false,
    val searchResults: List<FoodSearchResult> = emptyList(),
    val errorMessage: String? = null
)

class MealLogViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val repository = MealLogRepository()

    private val _homeCardState = MutableStateFlow(HomeCalorieCardUiState())
    val homeCardState: StateFlow<HomeCalorieCardUiState> = _homeCardState.asStateFlow()

    private val _uiState = MutableStateFlow(MealLogUiState())
    val uiState: StateFlow<MealLogUiState> = _uiState.asStateFlow()

    private var homeSummaryListener: ListenerRegistration? = null
    private var screenSummaryListener: ListenerRegistration? = null
    private var entriesListener: ListenerRegistration? = null
    private var observedMealUid: String? = null

    init {
        val todayDateKey = malaysiaTodayDateKey()
        observeHomeCard(todayDateKey)
        observeDate(todayDateKey)
    }

    fun refreshForAuthenticatedUser() {
        val uid = auth.currentUser?.uid ?: return
        if (
            observedMealUid == uid &&
            homeSummaryListener != null &&
            screenSummaryListener != null &&
            entriesListener != null
        ) {
            return
        }

        observedMealUid = uid
        observeHomeCard(malaysiaTodayDateKey())
        observeDate(_uiState.value.selectedDateKey)
    }

    fun observeHomeCard(dateKey: String = malaysiaTodayDateKey()) {
        val uid = auth.currentUser?.uid ?: run {
            homeSummaryListener?.remove()
            homeSummaryListener = null
            _homeCardState.value = HomeCalorieCardUiState(isLoading = false, todayDateKey = dateKey)
            return
        }
        homeSummaryListener?.remove()
        _homeCardState.value = _homeCardState.value.copy(isLoading = true, todayDateKey = dateKey, errorMessage = null)

        homeSummaryListener = repository.observeDailySummary(
            uid = uid,
            dateKey = dateKey,
            onUpdate = { summary ->
                _homeCardState.value = HomeCalorieCardUiState(
                    isLoading = false,
                    todayDateKey = summary.dateKey.ifBlank { dateKey },
                    todayCalories = summary.totalCalories,
                    totalItems = summary.totalItems
                )
            },
            onError = { message ->
                _homeCardState.value = _homeCardState.value.copy(isLoading = false, errorMessage = message)
            }
        )
    }

    fun observeDate(dateKey: String) {
        val uid = auth.currentUser?.uid ?: run {
            screenSummaryListener?.remove()
            entriesListener?.remove()
            screenSummaryListener = null
            entriesListener = null
            _uiState.update {
                it.copy(
                    selectedDateKey = dateKey,
                    displayDateLabel = formatMealDateLabel(dateKey),
                    calendarStrip = getCalendarStrip(dateKey),
                    summary = DailyMealSummary(dateKey = dateKey),
                    entries = emptyList(),
                    isLoading = false,
                    errorMessage = "Sign in to view meal logs"
                )
            }
            return
        }
        observedMealUid = uid
        screenSummaryListener?.remove()
        entriesListener?.remove()

        _uiState.update {
            it.copy(
                selectedDateKey = dateKey,
                displayDateLabel = formatMealDateLabel(dateKey),
                calendarStrip = getCalendarStrip(dateKey),
                summary = DailyMealSummary(dateKey = dateKey),
                entries = emptyList(),
                isLoading = true,
                errorMessage = null
            )
        }

        screenSummaryListener = repository.observeDailySummary(
            uid = uid,
            dateKey = dateKey,
            onUpdate = { summary ->
                _uiState.update {
                    it.copy(
                        summary = summary.copy(dateKey = summary.dateKey.ifBlank { dateKey }),
                        isLoading = false
                    )
                }
            },
            onError = { message ->
                _uiState.update { it.copy(isLoading = false, errorMessage = message) }
            }
        )

        entriesListener = repository.observeEntries(
            uid = uid,
            dateKey = dateKey,
            onUpdate = { entries ->
                _uiState.update { it.copy(entries = entries, isLoading = false) }
            },
            onError = { message ->
                _uiState.update { it.copy(isLoading = false, errorMessage = message) }
            }
        )
    }

    fun goToPreviousDate() {
        val cal = parseMalaysiaDateKey(_uiState.value.selectedDateKey)
        cal.add(Calendar.DAY_OF_YEAR, -1)
        observeDate(malaysiaDateKeyFor(cal))
    }

    fun goToNextDate() {
        if (!canNavigateToNextMealDate(_uiState.value.selectedDateKey)) return
        val cal = parseMalaysiaDateKey(_uiState.value.selectedDateKey)
        cal.add(Calendar.DAY_OF_YEAR, 1)
        observeDate(malaysiaDateKeyFor(cal))
    }

    fun jumpToToday() {
        val today = malaysiaTodayDateKey()
        observeHomeCard(today)
        observeDate(today)
    }

    fun searchFoods(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, searchResults = emptyList(), errorMessage = null) }
            val results = repository.searchFoods(query)
            _uiState.update { it.copy(isSearching = false, searchResults = results) }
        }
    }

    fun clearSearchResults() {
        _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
    }

    fun saveEntry(
        draft: MealEntryDraft,
        existingEntryId: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("User not logged in")
            return
        }

        val dateKey = _uiState.value.selectedDateKey
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                repository.upsertEntry(uid = uid, dateKey = dateKey, draft = draft, existingEntryId = existingEntryId)
                if (entriesListener == null || screenSummaryListener == null) {
                    observeDate(dateKey)
                }
                if (dateKey == malaysiaTodayDateKey()) {
                    observeHomeCard(dateKey)
                }
                _uiState.update { it.copy(isSaving = false) }
                clearSearchResults()
                onSuccess()
            } catch (e: Exception) {
                val message = e.message ?: "Failed to save meal"
                _uiState.update { it.copy(isSaving = false, errorMessage = message) }
                onError(message)
            }
        }
    }

    fun deleteEntry(entryId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("User not logged in")
            return
        }

        val dateKey = _uiState.value.selectedDateKey
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                repository.deleteEntry(uid = uid, dateKey = dateKey, entryId = entryId)
                if (dateKey == malaysiaTodayDateKey()) {
                    observeHomeCard(dateKey)
                }
                _uiState.update { it.copy(isSaving = false) }
                onSuccess()
            } catch (e: Exception) {
                val message = e.message ?: "Failed to delete meal"
                _uiState.update { it.copy(isSaving = false, errorMessage = message) }
                onError(message)
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
        _homeCardState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        homeSummaryListener?.remove()
        screenSummaryListener?.remove()
        entriesListener?.remove()
        super.onCleared()
    }
}
