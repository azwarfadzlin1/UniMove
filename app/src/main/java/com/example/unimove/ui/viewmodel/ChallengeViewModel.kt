package com.example.unimove.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.unimove.data.model.Challenge
import com.example.unimove.data.model.ChallengeStatus
import com.example.unimove.data.model.UserChallengeProgress
import com.example.unimove.data.model.challengeFromDocument
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date

class ChallengeViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid

    private val _activeChallenges = MutableStateFlow<List<Challenge>>(emptyList())
    val activeChallenges: StateFlow<List<Challenge>> = _activeChallenges.asStateFlow()

    private val _userProgress = MutableStateFlow<Map<String, UserChallengeProgress>>(emptyMap())
    val userProgress: StateFlow<Map<String, UserChallengeProgress>> = _userProgress.asStateFlow()

    private var challengesListener: ListenerRegistration? = null
    private var progressListener: ListenerRegistration? = null

    init {
        listenToChallenges()
        listenToUserProgress()
    }

    private fun listenToChallenges() {
        challengesListener = firestore.collection("challenges")
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ChallengeVM", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    _activeChallenges.value = snapshot.documents.mapNotNull { doc ->
                        challengeFromDocument(doc)
                    }.sortedBy { it.endDate?.seconds ?: Long.MAX_VALUE }
                }
            }
    }

    private fun listenToUserProgress() {
        val uid = currentUserId ?: return
        progressListener = firestore.collection("users").document(uid)
            .collection("challenge_progress")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ChallengeVM", "Progress listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    _userProgress.value = snapshot.documents.mapNotNull { doc ->
                        val progress = doc.toObject(UserChallengeProgress::class.java) ?: return@mapNotNull null
                        val challengeId = progress.challengeId.ifBlank { doc.id }
                        challengeId to progress.copy(challengeId = challengeId)
                    }.toMap()
                }
            }
    }

    fun prepareChallenge(
        challenge: Challenge,
        onReady: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = currentUserId
        if (uid == null) {
            onError("User not logged in")
            return
        }

        val now = Timestamp(Date())
        val existingProgress = userProgress.value[challenge.id]
        val progress = UserChallengeProgress(
            challengeId = challenge.id,
            accumulatedDistanceKm = existingProgress?.accumulatedDistanceKm ?: 0.0,
            bestCompletionSeconds = existingProgress?.bestCompletionSeconds,
            latestCompletionSeconds = existingProgress?.latestCompletionSeconds,
            totalAttempts = existingProgress?.totalAttempts ?: 0,
            status = ChallengeStatus.ACTIVE,
            joinedAt = existingProgress?.joinedAt ?: now,
            completedAt = null,
            lastUpdated = now
        )

        firestore.collection("users").document(uid)
            .collection("challenge_progress")
            .document(challenge.id)
            .set(progress, SetOptions.merge())
            .addOnSuccessListener { onReady() }
            .addOnFailureListener { e ->
                Log.e("ChallengeVM", "Error preparing challenge", e)
                onError(e.message ?: "Failed to open challenge")
            }
    }

    fun markChallengePaused(challengeId: String) {
        updateStatus(challengeId, ChallengeStatus.PAUSED)
    }

    fun markChallengeActive(challengeId: String) {
        updateStatus(challengeId, ChallengeStatus.ACTIVE)
    }

    private fun updateStatus(challengeId: String, newStatus: ChallengeStatus) {
        val uid = currentUserId ?: return
        firestore.collection("users").document(uid)
            .collection("challenge_progress")
            .document(challengeId)
            .set(
                mapOf(
                    "challengeId" to challengeId,
                    "status" to newStatus,
                    "lastUpdated" to Timestamp(Date())
                ),
                SetOptions.merge()
            )
            .addOnFailureListener { e ->
                Log.e("ChallengeVM", "Error updating status", e)
            }
    }

    override fun onCleared() {
        super.onCleared()
        challengesListener?.remove()
        progressListener?.remove()
    }
}
