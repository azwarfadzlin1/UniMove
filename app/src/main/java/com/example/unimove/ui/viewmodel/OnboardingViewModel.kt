package com.example.unimove.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.unimove.data.OnboardingPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class OnboardingState(
    val name: String = "",
    val weight: String = "",
    val height: String = "",
    val gender: String = "",
    val college: String = "",
    val age: String = "",
    val selectedIconKey: String = "",
    val isSaving: Boolean = false,
    val saveError: String? = null
)

class OnboardingViewModel(private val context: Context) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val prefs = OnboardingPreferences(context)

    init {
        preloadExistingProfile()
    }

    fun updateName(name: String) { _state.value = _state.value.copy(name = name) }
    fun updateWeight(weight: String) { _state.value = _state.value.copy(weight = weight) }
    fun updateHeight(height: String) { _state.value = _state.value.copy(height = height) }
    fun updateGender(gender: String) { _state.value = _state.value.copy(gender = gender) }
    fun updateCollege(college: String) { _state.value = _state.value.copy(college = college) }
    fun updateAge(age: String) { _state.value = _state.value.copy(age = age) }
    fun updateIcon(iconKey: String) { _state.value = _state.value.copy(selectedIconKey = iconKey) }

    fun saveAndFinish(onComplete: () -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _state.value = _state.value.copy(saveError = "Session expired. Access denied.")
            return
        }

        val s = _state.value
        _state.value = s.copy(isSaving = true, saveError = null)

        viewModelScope.launch {
            try {
                val updates = mutableMapOf<String, Any>(
                    "name" to s.name.trim(),
                    "weight" to (s.weight.toDoubleOrNull() ?: 0.0),
                    "height" to (s.height.toDoubleOrNull() ?: 0.0),
                    "gender" to s.gender,
                    "college" to s.college.trim(),
                    "profileCompleted" to true,
                    "onboardingCompleted" to true,
                    "updatedAt" to System.currentTimeMillis()
                )

                val ageInt = s.age.toIntOrNull()
                if (ageInt != null) updates["age"] = ageInt

                if (s.selectedIconKey.isNotBlank()) {
                    updates["avatarKey"] = s.selectedIconKey
                }

                // Cloud Sync
                firestore.collection("users").document(uid)
                    .set(updates, SetOptions.merge())
                    .await()

                // Local Cache
                prefs.saveBasicProfile(s.name.trim(), s.selectedIconKey)
                prefs.setOnboardingCompleted(true)

                _state.value = _state.value.copy(isSaving = false)
                onComplete()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    saveError = "Sync Failed: ${e.localizedMessage}"
                )
            }
        }
    }

    private fun preloadExistingProfile() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val doc = firestore.collection("users").document(uid).get().await()
                if (!doc.exists()) return@launch

                _state.value = _state.value.copy(
                    name = doc.getString("name") ?: "",
                    weight = doc.get("weight")?.toString() ?: "",
                    height = doc.get("height")?.toString() ?: "",
                    gender = doc.getString("gender") ?: "",
                    college = doc.getString("college") ?: "",
                    age = doc.get("age")?.toString() ?: "",
                    selectedIconKey = doc.getString("avatarKey") ?: ""
                )
            } catch (_: Exception) {}
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OnboardingViewModel(context) as T
        }
    }
}
