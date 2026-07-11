package com.example.unimove.wear

import android.content.Context
import android.util.Log
import com.example.unimove.ui.viewmodel.currentWeekKey
import com.example.unimove.ui.viewmodel.weeklyLeaderboardDocumentId
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.Locale

private const val TAG = "WatchSyncDebug"
private const val LIVE_SESSION_STALE_TIMEOUT_MS = 20_000L

/**
 * Singleton repository that:
 * 1. Receives raw [WatchSessionUpdate] objects from [WatchDataListenerService].
 * 2. Persists each update into SQLite ([WatchDatabase]) — survives app crashes.
 * 3. Exposes a [StateFlow] for the UI to observe the live session.
 * 4. Uploads the final session to Firestore when [WatchSessionUpdate.isEnded] == true.
 * 5. Marks the local record as synced and prunes old entries.
 *
 * Singleton via companion object so the WearableListenerService (instantiated
 * by Android OS) and the ViewModel share the same in-memory flow.
 */
class WatchSessionRepository private constructor(context: Context) {

    private val db    = WatchDatabase.getInstance(context)
    private val firestore = FirebaseFirestore.getInstance()
    private val auth  = FirebaseAuth.getInstance()
    private val messageGateway = WatchMessageGateway.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // In-memory state exposed to the UI.
    private val _sessionState = MutableStateFlow<WatchSessionUiState?>(null)
    val sessionState: StateFlow<WatchSessionUiState?> = _sessionState.asStateFlow()

    private var staleSessionJob: Job? = null
    private var pendingRetryJob: Job? = null
    private val latestSequenceBySession = mutableMapOf<Long, Long>()
    private val updateMutex = Mutex()
    private val pendingSyncMutex = Mutex()
    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        if (firebaseAuth.currentUser != null) {
            retryPendingSessions()
        }
    }

    init {
        // Data Layer reception is account-independent. Authentication is only
        // required when a completed local session is uploaded to Firestore.
        auth.addAuthStateListener(authStateListener)

        // On startup, restore the last in-progress session (app-restart recovery)
        scope.launch {
            val latest = db.getLatest()
            if (latest != null) {
                latestSequenceBySession[latest.sessionId] = latest.sequenceNumber
            }
            if (latest != null && !latest.isEnded) {
                restoreActiveSession(
                    update = latest.toUpdate(),
                    lastUpdatedAtMillis = latest.timestamp
                )
            }
        }
    }

    private fun retryPendingSessions() {
        scope.launch {
            db.getPendingSync().forEach { uploadAndMark(it.toUpdate()) }
        }
    }

    private fun schedulePendingRetry() {
        if (pendingRetryJob?.isActive == true) return

        pendingRetryJob = scope.launch {
            delay(PENDING_SYNC_RETRY_MS)
            while (auth.currentUser != null) {
                val pending = db.getPendingSync()
                if (pending.isEmpty()) break

                pending.forEach { uploadAndMark(it.toUpdate()) }
                if (db.getPendingSync().isEmpty()) break
                delay(PENDING_SYNC_RETRY_MS)
            }
        }
    }

    /** Called by [WatchDataListenerService] on every DataItem change. */
    fun onWatchUpdate(update: WatchSessionUpdate, sourceNodeId: String?) {
        Log.d(TAG, "onWatchUpdate called: Session=${update.sessionId}, Seq=${update.sequenceNumber}, Ended=${update.isEnded}")
        scope.launch {
            updateMutex.withLock {
                try {
                    val lastAcceptedSequence = latestSequenceBySession[update.sessionId]
                    if (lastAcceptedSequence != null && update.sequenceNumber <= lastAcceptedSequence) {
                        Log.w(
                            TAG,
                            "Ignoring stale or duplicate update for session ${update.sessionId}: seq=${update.sequenceNumber}, last=$lastAcceptedSequence"
                        )
                        acknowledgeUpdate(
                            sourceNodeId = sourceNodeId,
                            update = update,
                            accepted = false
                        )
                        return@withLock
                    }

                    db.upsert(update.toEntity())   // persist first (crash-safe)
                    Log.d(TAG, "Successfully persisted update locally for session ${update.sessionId}")
                    latestSequenceBySession[update.sessionId] = update.sequenceNumber
                    acknowledgeUpdate(
                        sourceNodeId = sourceNodeId,
                        update = update,
                        accepted = true
                    )
                    if (update.isEnded) {
                        staleSessionJob?.cancel()
                        _sessionState.value = WatchSessionUiState(
                            session = update,
                            status = WatchSessionStatus.COMPLETED,
                            lastUpdatedAtMillis = System.currentTimeMillis()
                        )
                        Log.d(TAG, "Session ${update.sessionId} has ended. Triggering Firestore upload...")
                        uploadAndMark(update)
                    } else {
                        val receivedAtMillis = System.currentTimeMillis()
                        _sessionState.value = WatchSessionUiState(
                            session = update,
                            status = WatchSessionStatus.ACTIVE,
                            lastUpdatedAtMillis = receivedAtMillis
                        )
                        scheduleStaleTimeout(
                            sessionId = update.sessionId,
                            lastUpdatedAtMillis = receivedAtMillis
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in onWatchUpdate for session ${update.sessionId}", e)
                }
            }
        }
    }

    private suspend fun acknowledgeUpdate(
        sourceNodeId: String?,
        update: WatchSessionUpdate,
        accepted: Boolean
    ) {
        if (sourceNodeId.isNullOrBlank()) return

        runCatching {
            messageGateway.sendAck(
                nodeId = sourceNodeId,
                sessionId = update.sessionId,
                sequenceNumber = update.sequenceNumber,
                accepted = accepted
            )
        }.onFailure { error ->
            Log.w(
                TAG,
                "Failed to send ack for session ${update.sessionId}, seq=${update.sequenceNumber}",
                error
            )
        }
    }

    private fun restoreActiveSession(
        update: WatchSessionUpdate,
        lastUpdatedAtMillis: Long
    ) {
        val ageMillis = (System.currentTimeMillis() - lastUpdatedAtMillis).coerceAtLeast(0L)
        if (ageMillis >= LIVE_SESSION_STALE_TIMEOUT_MS) {
            _sessionState.value = WatchSessionUiState(
                session = update,
                status = WatchSessionStatus.STALE,
                lastUpdatedAtMillis = lastUpdatedAtMillis
            )
            return
        }

        _sessionState.value = WatchSessionUiState(
            session = update,
            status = WatchSessionStatus.ACTIVE,
            lastUpdatedAtMillis = lastUpdatedAtMillis
        )
        scheduleStaleTimeout(
            sessionId = update.sessionId,
            lastUpdatedAtMillis = lastUpdatedAtMillis,
            delayMillis = LIVE_SESSION_STALE_TIMEOUT_MS - ageMillis
        )
    }

    private fun scheduleStaleTimeout(
        sessionId: Long,
        lastUpdatedAtMillis: Long,
        delayMillis: Long = LIVE_SESSION_STALE_TIMEOUT_MS
    ) {
        staleSessionJob?.cancel()
        staleSessionJob = scope.launch {
            delay(delayMillis)
            val current = _sessionState.value ?: return@launch
            if (
                current.session.sessionId == sessionId &&
                current.status == WatchSessionStatus.ACTIVE &&
                current.lastUpdatedAtMillis == lastUpdatedAtMillis
            ) {
                _sessionState.value = current.copy(status = WatchSessionStatus.STALE)
            }
        }
    }

    private suspend fun uploadAndMark(update: WatchSessionUpdate) {
        val currentUser = auth.currentUser
        val uid = currentUser?.uid

        if (uid == null) {
            Log.i(TAG, "Session ${update.sessionId} is stored locally until an account signs in")
            return
        }

        pendingSyncMutex.withLock {
            // The account may have changed while this upload waited behind another session.
            if (auth.currentUser?.uid != uid) {
                Log.i(TAG, "Deferring session ${update.sessionId}; active account changed")
                return@withLock
            }

            Log.d(TAG, "Starting atomic sync for session ${update.sessionId} (User: $uid)")

            try {
                val distanceKm = (update.distanceMeters / 1_000.0).coerceAtLeast(0.0)
                val durationSeconds = (update.elapsedMs / 1_000L).coerceAtLeast(0L)
                val weekKey = currentWeekKey()
                val userRef = firestore.collection("users").document(uid)
                val sessionRef = userRef.collection("sessions").document(update.sessionId.toString())
                val leaderboardRef = firestore.collection("weekly_leaderboard")
                    .document(weeklyLeaderboardDocumentId(uid, weekKey))
                val now = Timestamp(Date())
                val payload = mapOf(
                    "userId"          to uid,
                    "runName"         to "Watch Run",
                    "isShared"        to false,
                    "distanceKm"      to distanceKm,
                    "durationSeconds" to durationSeconds,
                    "avgPace"         to formatAvgPace(update.paceMsPerKm, update.elapsedMs, update.distanceMeters),
                    "timestamp"       to Timestamp(Date(update.timestamp)),
                    "routePoints"     to emptyList<Map<String, Double>>(),
                    "sessionMode"     to "watch",
                    "source"          to "watch",
                    "sessionId"       to update.sessionId,
                    "sequenceNumber"  to update.sequenceNumber,
                    "steps"           to update.steps,
                    "heartRate"       to update.heartRate,
                    "paceMsPerKm"     to update.paceMsPerKm,
                    "isEnded"         to update.isEnded,
                    "syncedAt"        to System.currentTimeMillis()
                )

                val created = firestore.runTransaction { transaction ->
                    val sessionSnapshot = transaction.get(sessionRef)
                    if (sessionSnapshot.exists()) {
                        return@runTransaction false
                    }

                    // Firestore transactions require all reads before any write.
                    val userSnapshot = transaction.get(userRef)
                    check(userSnapshot.exists()) { "User profile $uid does not exist" }
                    val leaderboardSnapshot = transaction.get(leaderboardRef)

                    val currentDistance = userSnapshot.getDouble("totalDistance") ?: 0.0
                    val currentTime = userSnapshot.getLong("totalTime") ?: 0L
                    val currentSessions = userSnapshot.getLong("totalSessions") ?: 0L
                    val weeklyDistance = leaderboardSnapshot.getDouble("weeklyDistance") ?: 0.0
                    val weeklySessions = leaderboardSnapshot.getLong("weeklySessions") ?: 0L
                    val profileName = userSnapshot.getString("name").orEmpty().ifBlank {
                        currentUser.displayName.orEmpty()
                    }.ifBlank { "Runner" }

                    transaction.set(sessionRef, payload)
                    transaction.update(
                        userRef,
                        mapOf(
                            "totalDistance" to (currentDistance + distanceKm),
                            "totalTime" to (currentTime + durationSeconds),
                            "totalSessions" to (currentSessions + 1L),
                        )
                    )
                    transaction.set(
                        leaderboardRef,
                        mapOf(
                            "userId" to uid,
                            "name" to profileName,
                            "weeklyDistance" to (weeklyDistance + distanceKm),
                            "weeklySessions" to (weeklySessions + 1L),
                            "weekKey" to weekKey,
                            "updatedAt" to now,
                        )
                    )
                    true
                }.await()

                db.markSynced(update.sessionId)
                db.pruneOldSynced()
                Log.d(
                    TAG,
                    if (created) {
                        "Session ${update.sessionId} synced atomically for $uid"
                    } else {
                        "Session ${update.sessionId} already existed for $uid; marked synced locally"
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload session ${update.sessionId}; it remains pending", e)
                schedulePendingRetry()
            }
        }
    }

    // ── Extension helpers ────────────────────────────────────────────────────

    private fun WatchSessionUpdate.toEntity() = WatchSessionEntity(
        sessionId      = sessionId,
        timestamp      = timestamp,
        sequenceNumber = sequenceNumber,
        elapsedMs      = elapsedMs,
        distanceMeters = distanceMeters,
        steps          = steps,
        heartRate      = heartRate,
        paceMsPerKm    = paceMsPerKm,
        isEnded        = isEnded,
    )

    private fun WatchSessionEntity.toUpdate() = WatchSessionUpdate(
        sessionId      = sessionId,
        timestamp      = timestamp,
        sequenceNumber = sequenceNumber,
        elapsedMs      = elapsedMs,
        distanceMeters = distanceMeters,
        steps          = steps,
        heartRate      = heartRate,
        paceMsPerKm    = paceMsPerKm,
        isEnded        = isEnded,
    )

    /** Observe the latest session row in real-time (for alternative DB-backed UI). */
    fun observeLatestFromDb(): Flow<WatchSessionEntity?> = db.observeLatest()

    private fun formatAvgPace(
        paceMsPerKm: Long,
        elapsedMs: Long,
        distanceMeters: Double,
    ): String {
        val resolvedMsPerKm = when {
            paceMsPerKm > 0L -> paceMsPerKm.toDouble()
            elapsedMs > 0L && distanceMeters >= 10.0 -> elapsedMs / (distanceMeters / 1_000.0)
            else -> 0.0
        }

        if (resolvedMsPerKm <= 0.0) {
            return "--"
        }

        val totalSeconds = (resolvedMsPerKm / 1_000.0).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%d:%02d /km", minutes, seconds)
    }

    companion object {
        private const val PENDING_SYNC_RETRY_MS = 30_000L

        @Volatile private var INSTANCE: WatchSessionRepository? = null

        fun getInstance(context: Context): WatchSessionRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: WatchSessionRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}
