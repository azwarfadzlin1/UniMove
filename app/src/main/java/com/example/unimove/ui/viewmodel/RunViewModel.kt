package com.example.unimove.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimove.data.model.Challenge
import com.example.unimove.data.model.ChallengeCompletionResult
import com.example.unimove.data.model.ChallengeStandingEntry
import com.example.unimove.data.model.challengeFromDocument
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

data class PendingRunSession(
    val distanceMeters: Float,
    val elapsedSeconds: Int,
    val smoothedPaceSecPerKm: Float,
    val pathSegments: List<List<Map<String, Double>>>
)

private data class ChallengeProgressSnapshot(
    val bestSeconds: Long,
    val improvedPersonalBest: Boolean
)

class RunViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _pendingSession = MutableStateFlow<PendingRunSession?>(null)
    val pendingSession: StateFlow<PendingRunSession?> = _pendingSession.asStateFlow()

    private val _activeChallenge = MutableStateFlow<Challenge?>(null)
    val activeChallenge: StateFlow<Challenge?> = _activeChallenge.asStateFlow()

    private val _pendingChallengeResult = MutableStateFlow<ChallengeCompletionResult?>(null)
    val pendingChallengeResult: StateFlow<ChallengeCompletionResult?> = _pendingChallengeResult.asStateFlow()

    private var activeChallengeListener: ListenerRegistration? = null

    fun setPendingSession(
        distanceMeters: Float,
        elapsedSeconds: Int,
        smoothedPaceSecPerKm: Float,
        pathSegments: List<List<Map<String, Double>>>
    ) {
        _pendingSession.value = PendingRunSession(
            distanceMeters = distanceMeters,
            elapsedSeconds = elapsedSeconds,
            smoothedPaceSecPerKm = smoothedPaceSecPerKm,
            pathSegments = pathSegments
        )
    }

    fun clearPendingSession() {
        _pendingSession.value = null
    }

    fun clearPendingChallengeResult() {
        _pendingChallengeResult.value = null
    }

    fun primeActiveChallenge(challenge: Challenge) {
        _activeChallenge.value = challenge
    }

    fun observeChallenge(challengeId: String?) {
        activeChallengeListener?.remove()
        if (challengeId.isNullOrBlank()) {
            _activeChallenge.value = null
            return
        }

        activeChallengeListener = firestore.collection("challenges")
            .document(challengeId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("RunVM", "Failed to observe challenge", e)
                    return@addSnapshotListener
                }

                _activeChallenge.value = snapshot?.let(::challengeFromDocument)
            }
    }

    fun commitSession(
        runName: String,
        isShared: Boolean,
        userName: String,
        challengeId: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val pending = _pendingSession.value
        if (pending == null) {
            onError("No pending session found")
            return
        }

        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("User not logged in")
            return
        }

        viewModelScope.launch {
            try {
                saveSessionRecord(
                    uid = uid,
                    runName = runName,
                    isShared = isShared,
                    userName = userName,
                    distanceMeters = pending.distanceMeters,
                    elapsedSeconds = pending.elapsedSeconds,
                    smoothedPaceSecPerKm = pending.smoothedPaceSecPerKm,
                    pathSegments = pending.pathSegments,
                    challengeId = challengeId,
                    sessionMode = if (challengeId.isNullOrBlank()) "standard" else "challenge_abandoned"
                )

                val distanceKm = pending.distanceMeters / 1000.0
                updateUserTotals(uid, distanceKm, pending.elapsedSeconds)
                updateWeeklyLeaderboard(uid, distanceKm, userName)
                clearPendingSession()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to save session")
            }
        }
    }

    fun completeChallengeRun(
        challenge: Challenge,
        distanceMeters: Float,
        elapsedSeconds: Int,
        smoothedPaceSecPerKm: Float,
        pathSegments: List<List<Map<String, Double>>>,
        onSuccess: (ChallengeCompletionResult) -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("User not logged in")
            return
        }

        viewModelScope.launch {
            try {
                val userName = resolveCurrentUserName()
                saveSessionRecord(
                    uid = uid,
                    runName = "${challenge.title} Challenge",
                    isShared = false,
                    userName = userName,
                    distanceMeters = distanceMeters,
                    elapsedSeconds = elapsedSeconds,
                    smoothedPaceSecPerKm = smoothedPaceSecPerKm,
                    pathSegments = pathSegments,
                    challengeId = challenge.id,
                    sessionMode = "challenge_completed"
                )

                val distanceKm = distanceMeters / 1000.0
                updateUserTotals(uid, distanceKm, elapsedSeconds)
                updateWeeklyLeaderboard(uid, distanceKm, userName)

                val result = saveChallengeCompletion(
                    challenge = challenge,
                    uid = uid,
                    userName = userName,
                    finishSeconds = elapsedSeconds
                )

                _pendingChallengeResult.value = result
                onSuccess(result)
            } catch (e: Exception) {
                onError(e.message ?: "Failed to complete challenge")
            }
        }
    }

    private suspend fun saveSessionRecord(
        uid: String,
        runName: String,
        isShared: Boolean,
        userName: String,
        distanceMeters: Float,
        elapsedSeconds: Int,
        smoothedPaceSecPerKm: Float,
        pathSegments: List<List<Map<String, Double>>>,
        challengeId: String?,
        sessionMode: String
    ) {
        val distanceKm = distanceMeters / 1000.0
        val avgPace = formatAveragePace(
            distanceKm = distanceKm,
            elapsedSeconds = elapsedSeconds,
            smoothedPaceSecPerKm = smoothedPaceSecPerKm
        )
        val firestoreRoutePoints = pathSegments.map { segment -> mapOf("points" to segment) }

        val sessionData = hashMapOf<String, Any>(
            "runName" to runName,
            "isShared" to isShared,
            "distanceKm" to distanceKm,
            "durationSeconds" to elapsedSeconds,
            "avgPace" to avgPace,
            "timestamp" to Timestamp(Date()),
            "routePoints" to firestoreRoutePoints,
            "sessionMode" to sessionMode
        )
        if (!challengeId.isNullOrBlank()) {
            sessionData["challengeId"] = challengeId
        }

        firestore.collection("users").document(uid)
            .collection("sessions")
            .add(sessionData)
            .await()

        if (isShared) {
            val feedPost = hashMapOf(
                "userId" to uid,
                "username" to userName,
                "runName" to runName,
                "distanceKm" to distanceKm,
                "durationSeconds" to elapsedSeconds,
                "avgPace" to avgPace,
                "timestamp" to Timestamp(Date()),
                "likes" to 0,
                "likedBy" to emptyList<String>(),
                "comments" to 0,
                "routePoints" to firestoreRoutePoints
            )
            if (!challengeId.isNullOrBlank()) {
                feedPost["challengeId"] = challengeId
            }
            firestore.collection("community_feed").add(feedPost).await()
        }
    }

    private suspend fun saveChallengeCompletion(
        challenge: Challenge,
        uid: String,
        userName: String,
        finishSeconds: Int
    ): ChallengeCompletionResult {
        val progressRef = userChallengeProgressRef(uid, challenge.id)
        val participantRef = challengeParticipantRef(challenge.id, uid)
        val now = Timestamp(Date())

        val progressSnapshot = firestore.runTransaction { transaction ->
            val progressDoc = transaction.get(progressRef)
            val participantDoc = transaction.get(participantRef)

            val existingBest = progressDoc.getLong("bestCompletionSeconds")
                ?: participantDoc.getLong("bestCompletionSeconds")
            val newBest = existingBest?.coerceAtMost(finishSeconds.toLong()) ?: finishSeconds.toLong()
            val attemptCount = ((progressDoc.getLong("totalAttempts")
                ?: participantDoc.getLong("attemptCount")
                ?: 0L) + 1L).toInt()
            val improved = existingBest == null || finishSeconds.toLong() < existingBest

            transaction.set(
                progressRef,
                mapOf(
                    "challengeId" to challenge.id,
                    "accumulatedDistanceKm" to challenge.targetDistanceKm,
                    "bestCompletionSeconds" to newBest,
                    "latestCompletionSeconds" to finishSeconds.toLong(),
                    "totalAttempts" to attemptCount,
                    "status" to "COMPLETED",
                    "completedAt" to now,
                    "lastUpdated" to now,
                    "joinedAt" to (progressDoc.getTimestamp("joinedAt") ?: now)
                ),
                SetOptions.merge()
            )

            transaction.set(
                participantRef,
                mapOf(
                    "userId" to uid,
                    "name" to userName,
                    "bestCompletionSeconds" to newBest,
                    "latestCompletionSeconds" to finishSeconds.toLong(),
                    "attemptCount" to attemptCount,
                    "completedAt" to now,
                    "updatedAt" to now
                ),
                SetOptions.merge()
            )

            ChallengeProgressSnapshot(
                bestSeconds = newBest,
                improvedPersonalBest = improved
            )
        }.await()

        val standings = firestore.collection("challenges")
            .document(challenge.id)
            .collection("participants")
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                val bestSeconds = doc.getLong("bestCompletionSeconds") ?: return@mapNotNull null
                ChallengeStandingEntry(
                    userId = doc.getString("userId").orEmpty().ifBlank { doc.id },
                    name = doc.getString("name").orEmpty().ifBlank { "Runner" },
                    bestCompletionSeconds = bestSeconds,
                    latestCompletionSeconds = doc.getLong("latestCompletionSeconds") ?: bestSeconds,
                    attemptCount = (doc.getLong("attemptCount") ?: 0L).toInt(),
                    completedAt = doc.getTimestamp("completedAt")
                )
            }
            .sortedWith(
                compareBy<ChallengeStandingEntry> { it.bestCompletionSeconds }
                    .thenBy { it.completedAt?.seconds ?: Long.MAX_VALUE }
                    .thenBy { it.name.lowercase() }
            )

        val rank = standings.indexOfFirst { it.userId == uid }

        return ChallengeCompletionResult(
            challengeId = challenge.id,
            challengeTitle = challenge.title,
            targetDistanceKm = challenge.targetDistanceKm,
            finishSeconds = finishSeconds,
            personalBestSeconds = progressSnapshot.bestSeconds,
            improvedPersonalBest = progressSnapshot.improvedPersonalBest,
            rank = if (rank >= 0) rank + 1 else 0,
            totalParticipants = standings.size,
            topStandings = standings.take(5)
        )
    }

    fun loadChallengeStandings(challengeId: String, challengeTitle: String, targetDistanceKm: Double) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val standingsSnap = firestore.collection("challenges")
                    .document(challengeId)
                    .collection("participants")
                    .get()
                    .await()

                val standings = standingsSnap.documents
                    .mapNotNull { doc ->
                        val bestSeconds = doc.getLong("bestCompletionSeconds") ?: return@mapNotNull null
                        ChallengeStandingEntry(
                            userId = doc.getString("userId").orEmpty().ifBlank { doc.id },
                            name = doc.getString("name").orEmpty().ifBlank { "Runner" },
                            bestCompletionSeconds = bestSeconds,
                            latestCompletionSeconds = doc.getLong("latestCompletionSeconds") ?: bestSeconds,
                            attemptCount = (doc.getLong("attemptCount") ?: 0L).toInt(),
                            completedAt = doc.getTimestamp("completedAt")
                        )
                    }
                    .sortedWith(
                        compareBy<ChallengeStandingEntry> { it.bestCompletionSeconds }
                            .thenBy { it.completedAt?.seconds ?: Long.MAX_VALUE }
                            .thenBy { it.name.lowercase() }
                    )

                val rank = standings.indexOfFirst { it.userId == uid }
                val myEntry = if (rank >= 0) standings[rank] else null

                val result = ChallengeCompletionResult(
                    challengeId = challengeId,
                    challengeTitle = challengeTitle,
                    targetDistanceKm = targetDistanceKm,
                    finishSeconds = myEntry?.bestCompletionSeconds?.toInt() ?: 0,
                    personalBestSeconds = myEntry?.bestCompletionSeconds,
                    improvedPersonalBest = false,
                    rank = if (rank >= 0) rank + 1 else 0,
                    totalParticipants = standings.size,
                    topStandings = standings // We take all standings for viewing
                )
                _pendingChallengeResult.value = result
            } catch (e: Exception) {
                Log.e("RunVM", "Failed to load standings", e)
            }
        }
    }

    private suspend fun resolveCurrentUserName(): String {
        val uid = auth.currentUser?.uid ?: return "Runner"
        val profileName = firestore.collection("users")
            .document(uid)
            .get()
            .await()
            .getString("name")
            .orEmpty()

        return profileName
            .ifBlank { auth.currentUser?.displayName.orEmpty() }
            .ifBlank { "Runner" }
    }

    private fun challengeParticipantRef(challengeId: String, uid: String): DocumentReference {
        return firestore.collection("challenges")
            .document(challengeId)
            .collection("participants")
            .document(uid)
    }

    private fun userChallengeProgressRef(uid: String, challengeId: String): DocumentReference {
        return firestore.collection("users")
            .document(uid)
            .collection("challenge_progress")
            .document(challengeId)
    }

    private fun formatAveragePace(
        distanceKm: Double,
        elapsedSeconds: Int,
        smoothedPaceSecPerKm: Float
    ): String {
        val paceValue = if (smoothedPaceSecPerKm > 0f) {
            smoothedPaceSecPerKm.toDouble()
        } else {
            if (distanceKm > 0) elapsedSeconds / distanceKm else 0.0
        }

        val mins = (paceValue / 60).toInt()
        val secs = (paceValue % 60).toInt()
        return "%d:%02d /km".format(mins, secs)
    }

    private fun updateUserTotals(uid: String, distanceKm: Double, durationSeconds: Int) {
        val userRef = firestore.collection("users").document(uid)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)

            val currentDist = snapshot.getDouble("totalDistance") ?: 0.0
            val currentTime = snapshot.getLong("totalTime") ?: 0L
            val currentSessions = snapshot.getLong("totalSessions") ?: 0L

            val newTotalDist = currentDist + distanceKm
            val newTotalTime = currentTime + durationSeconds
            val newTotalSessions = currentSessions + 1

            transaction.update(
                userRef,
                mapOf(
                    "totalDistance" to newTotalDist,
                    "totalTime" to newTotalTime,
                    "totalSessions" to newTotalSessions
                )
            )
        }.addOnFailureListener {
            Log.w("RunVM", "Failed to update user totals", it)
        }
    }

    private suspend fun updateWeeklyLeaderboard(uid: String, distanceKm: Double, fallbackName: String) {
        val weekKey = currentWeekKey()
        val leaderboardRef = firestore.collection("weekly_leaderboard")
            .document(weeklyLeaderboardDocumentId(uid, weekKey))
        val userRef = firestore.collection("users").document(uid)
        val now = Timestamp(Date())

        firestore.runTransaction { transaction ->
            val leaderboardSnap = transaction.get(leaderboardRef)
            val userSnap = transaction.get(userRef)

            val currentDistance = leaderboardSnap.getDouble("weeklyDistance") ?: 0.0
            val currentSessions = leaderboardSnap.getLong("weeklySessions") ?: 0L
            val profileName = userSnap.getString("name").orEmpty().ifBlank { fallbackName }

            transaction.set(
                leaderboardRef,
                mapOf(
                    "userId" to uid,
                    "name" to profileName.ifBlank { "Runner" },
                    "weeklyDistance" to (currentDistance + distanceKm),
                    "weeklySessions" to (currentSessions + 1),
                    "weekKey" to weekKey,
                    "updatedAt" to now
                )
            )
        }.await()
    }

    override fun onCleared() {
        super.onCleared()
        activeChallengeListener?.remove()
    }
}
