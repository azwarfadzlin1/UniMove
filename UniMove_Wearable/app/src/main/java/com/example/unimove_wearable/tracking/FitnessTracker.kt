package com.example.unimove_wearable.tracking

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.clearUpdateCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseState
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseTypeCapabilities
import androidx.health.services.client.data.ExerciseUpdate
import androidx.health.services.client.endExercise
import androidx.health.services.client.getCapabilities
import androidx.health.services.client.pauseExercise
import androidx.health.services.client.resumeExercise
import androidx.health.services.client.startExercise
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

object FitnessTracker {
    private const val TAG = "WatchSyncDebug"
    private const val LIVE_UPDATE_INTERVAL_MS = 5_000L
    private const val CONNECTION_POLL_INTERVAL_MS = 5_000L

    private val _uiState = MutableStateFlow(TrackingUiState())
    val uiState: StateFlow<TrackingUiState> = _uiState.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val sequenceLock = Any()
    private val lastAckedSequenceBySession = ConcurrentHashMap<Long, Long>()
    private val heartRates = mutableListOf<Int>()

    @Volatile private var appContext: Context? = null
    private var exerciseClient: ExerciseClient? = null
    private var summaryStore: RunSummaryStore? = null
    private var connectionMonitoringStarted = false
    private var sessionId = 0L
    private var nextSequenceNumber = 0L
    private var lastLivePublishEpochMs = 0L
    private var finalSummaryForSession: RunSummary? = null
    private var finalPublishStarted = false

    private val callback = object : ExerciseUpdateCallback {
        override fun onAvailabilityChanged(
            dataType: DataType<*, *>,
            availability: Availability,
        ) {
            if (dataType == DataType.HEART_RATE_BPM) {
                Log.d(TAG, "Heart-rate availability changed: $availability")
            }
        }

        override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
            val exerciseState = update.exerciseStateInfo.state
            val checkpoint = update.activeDurationCheckpoint
            val currentState = _uiState.value
            val latestMetrics = update.latestMetrics

            val currentHeartRate = latestMetrics.getData(DataType.HEART_RATE_BPM)
                .lastOrNull()?.value?.roundToInt() ?: currentState.heartRate
            if (currentHeartRate != null && exerciseState == ExerciseState.ACTIVE) {
                synchronized(heartRates) { heartRates.add(currentHeartRate) }
            }

            val currentDistance = latestMetrics.getData(DataType.DISTANCE_TOTAL)?.total
                ?: currentState.distanceMeters
            val currentSteps = latestMetrics.getData(DataType.STEPS_TOTAL)?.total
                ?: currentState.steps
            val nextPhase = when {
                exerciseState.isEnded -> TrackingPhase.SUMMARY
                exerciseState == ExerciseState.ACTIVE -> TrackingPhase.RUNNING
                exerciseState == ExerciseState.USER_PAUSED ||
                    exerciseState == ExerciseState.AUTO_PAUSED -> TrackingPhase.PAUSED
                currentState.phase == TrackingPhase.STOPPING -> TrackingPhase.STOPPING
                else -> TrackingPhase.STARTING
            }
            val baseDurationMs = checkpoint?.activeDuration?.toMillis()
                ?: currentState.activeDurationBaseMs
            val checkpointEpochMs = checkpoint?.time?.toEpochMilli()
                ?: currentState.activeDurationCheckpointEpochMs
            val elapsedMs = calculateElapsedMs(baseDurationMs, checkpointEpochMs, nextPhase)

            if (exerciseState.isEnded) {
                completeSession(
                    elapsedMs = elapsedMs,
                    distanceMeters = currentDistance,
                    steps = currentSteps,
                    heartRate = currentHeartRate,
                    baseDurationMs = baseDurationMs,
                    checkpointEpochMs = checkpointEpochMs,
                )
                return
            }

            if (finalSummaryForSession != null) return

            _uiState.value = currentState.copy(
                phase = nextPhase,
                distanceMeters = currentDistance,
                steps = currentSteps,
                heartRate = currentHeartRate,
                activeDurationBaseMs = baseDurationMs,
                activeDurationCheckpointEpochMs = checkpointEpochMs,
                statusMessage = null,
                errorMessage = null,
            )
            maybePublishLiveUpdate(
                distanceMeters = currentDistance,
                steps = currentSteps,
                heartRate = currentHeartRate,
                activeDurationMs = elapsedMs,
            )
        }

        override fun onRegistered() {
            Log.d(TAG, "Health Services callback registered")
        }

        override fun onRegistrationFailed(throwable: Throwable) {
            Log.e(TAG, "Health Services callback registration failed", throwable)
            exerciseClient?.let { client ->
                scope.launch { runCatching { client.endExercise() } }
            }
            setFatalError(throwable.message ?: "Unable to register the Health Services callback.")
        }

        override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) = Unit
    }

    @Synchronized
    fun initialize(context: Context) {
        if (appContext != null) return

        val applicationContext = context.applicationContext
        appContext = applicationContext
        exerciseClient = HealthServices.getClient(applicationContext).exerciseClient
        summaryStore = RunSummaryStore(applicationContext)

        val pending = summaryStore.orEmptyPending()
        _uiState.value = _uiState.value.copy(
            summary = summaryStore?.lastSummary(),
            hasPendingSync = pending.isNotEmpty(),
            syncStatus = if (pending.isNotEmpty()) SyncStatus.PENDING else SyncStatus.NONE,
        )
        checkCapabilities()
        startConnectionMonitoring()
    }

    fun startTracking() {
        val client = exerciseClient
        val context = appContext
        val store = summaryStore
        if (client == null || context == null || store == null) {
            setFatalError("Workout tracking is not initialized.")
            return
        }

        if (_uiState.value.phase in ACTIVE_PHASES) {
            Log.d(TAG, "Ignoring duplicate START for active session=$sessionId")
            return
        }

        _uiState.value = _uiState.value.copy(
            phase = TrackingPhase.STARTING,
            statusMessage = "Checking sensors and permissions",
            errorMessage = null,
        )

        scope.launch {
            if (!hasRequiredPermissions(context)) {
                val message = "Motion, heart-rate, and notification access are required. Open UniMove on the watch to grant access."
                Log.e(TAG, message)
                setFatalError(message)
                return@launch
            }

            val runningCapabilities = runCatching {
                client.getCapabilities().getExerciseTypeCapabilities(ExerciseType.RUNNING)
            }.onFailure { Log.e(TAG, "Unable to read Health Services capabilities", it) }
                .getOrElse {
                    setCapabilityError(it.message ?: "Unable to read Health Services capabilities.")
                    return@launch
                }

            if (!supportsRequiredMetrics(runningCapabilities)) {
                val message = "This watch must support heart rate, distance, and steps for running."
                Log.e(TAG, message)
                _uiState.value = _uiState.value.copy(
                    phase = TrackingPhase.ERROR,
                    capabilitiesChecked = true,
                    isSupported = false,
                    statusMessage = null,
                    errorMessage = message,
                )
                return@launch
            }

            val newSessionId = runCatching { store.createSessionId() }
                .onFailure { Log.e(TAG, "Unable to create a durable session id", it) }
                .getOrElse {
                    setFatalError(it.message ?: "Unable to save a new workout on this watch.")
                    return@launch
                }
            synchronized(sequenceLock) {
                sessionId = newSessionId
                nextSequenceNumber = 0L
                finalSummaryForSession = null
                finalPublishStarted = false
            }
            lastLivePublishEpochMs = 0L
            synchronized(heartRates) { heartRates.clear() }

            val pending = store.pendingSummaries().isNotEmpty()
            _uiState.value = TrackingUiState(
                phase = TrackingPhase.STARTING,
                capabilitiesChecked = true,
                isSupported = true,
                isPhoneConnected = _uiState.value.isPhoneConnected,
                hasPendingSync = pending,
                syncStatus = if (pending) SyncStatus.PENDING else SyncStatus.NONE,
                summary = store.lastSummary(),
                statusMessage = "Preparing sensors",
            )

            client.setUpdateCallback(callback)
            runCatching {
                client.startExercise(
                    ExerciseConfig(
                        exerciseType = ExerciseType.RUNNING,
                        dataTypes = REQUIRED_DATA_TYPES,
                        isAutoPauseAndResumeEnabled = false,
                        isGpsEnabled = false,
                    ),
                )
            }.onSuccess {
                Log.i(TAG, "Started Health Services exercise session=$newSessionId")
            }.onFailure {
                Log.e(TAG, "Unable to start Health Services exercise", it)
                clearCallback()
                setFatalError(it.message ?: "Unable to start the workout.")
            }
        }
    }

    fun stopTracking(requestedSessionId: Long? = null) {
        val client = exerciseClient ?: return
        if (requestedSessionId != null && requestedSessionId != sessionId) {
            Log.w(TAG, "Ignoring STOP for session=$requestedSessionId; active session=$sessionId")
            return
        }
        val previousPhase = _uiState.value.phase
        if (previousPhase == TrackingPhase.STOPPING || previousPhase == TrackingPhase.SUMMARY) {
            Log.d(TAG, "Ignoring duplicate STOP for session=$sessionId phase=$previousPhase")
            return
        }
        if (previousPhase !in TRACKABLE_PHASES) {
            Log.d(TAG, "Ignoring STOP without an active exercise")
            return
        }

        _uiState.value = _uiState.value.copy(
            phase = TrackingPhase.STOPPING,
            statusMessage = "Ending workout",
            errorMessage = null,
        )
        scope.launch {
            runCatching { client.endExercise() }
                .onSuccess { Log.i(TAG, "Requested exercise end session=$sessionId") }
                .onFailure {
                    Log.e(TAG, "Unable to end Health Services exercise", it)
                    _uiState.value = _uiState.value.copy(
                        phase = previousPhase,
                        statusMessage = null,
                        errorMessage = it.message ?: "Unable to end the workout. Tap stop to retry.",
                    )
                }
        }
    }

    fun pauseTracking() {
        val client = exerciseClient ?: return
        if (_uiState.value.phase != TrackingPhase.RUNNING) {
            Log.d(TAG, "Ignoring duplicate/invalid PAUSE in phase=${_uiState.value.phase}")
            return
        }
        scope.launch {
            runCatching { client.pauseExercise() }
                .onSuccess { Log.i(TAG, "Requested exercise pause session=$sessionId") }
                .onFailure {
                    Log.e(TAG, "Unable to pause Health Services exercise", it)
                    _uiState.value = _uiState.value.copy(
                        errorMessage = it.message ?: "Unable to pause the workout.",
                    )
                }
        }
    }

    fun resumeTracking() {
        val client = exerciseClient ?: return
        if (_uiState.value.phase != TrackingPhase.PAUSED) {
            Log.d(TAG, "Ignoring duplicate/invalid RESUME in phase=${_uiState.value.phase}")
            return
        }
        scope.launch {
            runCatching { client.resumeExercise() }
                .onSuccess { Log.i(TAG, "Requested exercise resume session=$sessionId") }
                .onFailure {
                    Log.e(TAG, "Unable to resume Health Services exercise", it)
                    _uiState.value = _uiState.value.copy(
                        errorMessage = it.message ?: "Unable to resume the workout.",
                    )
                }
        }
    }

    fun handlePhoneAck(sessionId: Long, sequenceNumber: Long, accepted: Boolean) {
        val store = summaryStore ?: return
        val removedPending = store.acknowledge(sessionId, sequenceNumber)
        val previousAck = lastAckedSequenceBySession[sessionId] ?: Long.MIN_VALUE
        if (sequenceNumber < previousAck) {
            Log.d(TAG, "Ignored stale ACK session=$sessionId seq=$sequenceNumber last=$previousAck")
            return
        }
        lastAckedSequenceBySession[sessionId] = maxOf(previousAck, sequenceNumber)

        val stillPending = store.pendingSummaries().isNotEmpty()
        _uiState.value = _uiState.value.copy(
            lastSyncEpochMs = System.currentTimeMillis(),
            hasPendingSync = stillPending,
            syncStatus = if (stillPending) SyncStatus.PENDING else SyncStatus.SYNCED,
            statusMessage = when {
                stillPending -> "Other completed runs are still waiting to sync"
                accepted -> "Synced to phone"
                else -> "Already received by phone"
            },
        )
        Log.i(
            TAG,
            "Processed phone ACK session=$sessionId seq=$sequenceNumber accepted=$accepted " +
                "clearedPending=$removedPending remainingPending=$stillPending",
        )
    }

    fun handlePhoneCommand(command: String, requestedSessionId: Long?) {
        when (command) {
            DataLayerContracts.COMMAND_START -> startTracking()
            DataLayerContracts.COMMAND_PAUSE -> pauseTracking()
            DataLayerContracts.COMMAND_RESUME -> resumeTracking()
            DataLayerContracts.COMMAND_STOP -> stopTracking(requestedSessionId)
            DataLayerContracts.COMMAND_SYNC_STATUS -> retryPendingSummarySync()
            else -> Log.w(TAG, "Ignoring unknown phone command=$command")
        }
    }

    fun retryPendingSummarySync() {
        val pending = summaryStore.orEmptyPending()
        if (pending.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                hasPendingSync = false,
                syncStatus = if (_uiState.value.lastSyncEpochMs != null) SyncStatus.SYNCED else SyncStatus.NONE,
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            hasPendingSync = true,
            syncStatus = SyncStatus.RETRYING,
            statusMessage = "Retrying sync to phone",
        )
        pending.forEach(::publishPendingSummary)
    }

    fun finishSession() {
        val current = _uiState.value
        _uiState.value = TrackingUiState(
            capabilitiesChecked = current.capabilitiesChecked,
            isSupported = current.isSupported,
            isPhoneConnected = current.isPhoneConnected,
            summary = current.summary,
            hasPendingSync = current.hasPendingSync,
            syncStatus = current.syncStatus,
            lastSyncEpochMs = current.lastSyncEpochMs,
        )
    }

    fun reportPermissionError() {
        setFatalError("Motion, heart-rate, and notification access are required to start a run.")
    }

    private fun completeSession(
        elapsedMs: Long,
        distanceMeters: Double,
        steps: Long,
        heartRate: Int?,
        baseDurationMs: Long,
        checkpointEpochMs: Long?,
    ) {
        if (sessionId == 0L) {
            setFatalError("Health Services ended an exercise without a UniMove session.")
            return
        }

        val summary = synchronized(sequenceLock) {
            finalSummaryForSession ?: RunSummary(
                sessionId = sessionId,
                sequenceNumber = ++nextSequenceNumber,
                durationMs = elapsedMs,
                distanceMeters = distanceMeters,
                steps = steps,
                averageHeartRate = synchronized(heartRates) {
                    if (heartRates.isEmpty()) heartRate else heartRates.average().roundToInt()
                },
                paceMsPerKm = calculatePaceMsPerKm(elapsedMs, distanceMeters),
                timestamp = System.currentTimeMillis(),
            ).also { finalSummaryForSession = it }
        }

        val store = summaryStore
        if (store == null || !store.savePending(summary)) {
            Log.e(TAG, "Unable to persist final summary session=${summary.sessionId}")
            setFatalError("The completed run could not be saved locally. Keep the app open and retry.")
            return
        }

        _uiState.value = _uiState.value.copy(
            phase = TrackingPhase.SUMMARY,
            distanceMeters = distanceMeters,
            steps = steps,
            heartRate = heartRate,
            activeDurationBaseMs = baseDurationMs,
            activeDurationCheckpointEpochMs = checkpointEpochMs,
            lastCompletedSessionId = summary.sessionId,
            hasPendingSync = true,
            syncStatus = SyncStatus.PENDING,
            statusMessage = "Sync pending",
            errorMessage = null,
            summary = summary,
        )
        clearCallback()

        val shouldPublish = synchronized(sequenceLock) {
            if (finalPublishStarted) false else {
                finalPublishStarted = true
                true
            }
        }
        if (shouldPublish) publishPendingSummary(summary)
    }

    private fun maybePublishLiveUpdate(
        distanceMeters: Double,
        steps: Long,
        heartRate: Int?,
        activeDurationMs: Long,
    ) {
        val context = appContext ?: return
        val now = System.currentTimeMillis()
        if (now - lastLivePublishEpochMs < LIVE_UPDATE_INTERVAL_MS) return
        lastLivePublishEpochMs = now
        val sequenceNumber = synchronized(sequenceLock) { ++nextSequenceNumber }

        val request = PutDataMapRequest.create(DataLayerContracts.LIVE_RUN_PATH).apply {
            putPayload(
                sessionId = sessionId,
                timestamp = now,
                sequenceNumber = sequenceNumber,
                elapsedMs = activeDurationMs,
                distanceMeters = distanceMeters,
                steps = steps,
                heartRate = heartRate ?: 0,
                paceMsPerKm = calculatePaceMsPerKm(activeDurationMs, distanceMeters),
                isEnded = false,
            )
        }.asPutDataRequest().setUrgent()

        Wearable.getDataClient(context).putDataItem(request)
            .addOnSuccessListener {
                Log.d(TAG, "Published live update session=$sessionId seq=$sequenceNumber")
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Live update transport failed session=$sessionId seq=$sequenceNumber", error)
                if (!_uiState.value.hasPendingSync) {
                    _uiState.value = _uiState.value.copy(syncStatus = SyncStatus.FAILED)
                }
            }
    }

    private fun publishPendingSummary(summary: RunSummary) {
        val context = appContext ?: return
        val request = PutDataMapRequest.create(
            DataLayerContracts.sessionSummaryPath(summary.sessionId),
        ).apply {
            putPayload(
                sessionId = summary.sessionId,
                timestamp = summary.timestamp,
                sequenceNumber = summary.sequenceNumber,
                elapsedMs = summary.durationMs,
                distanceMeters = summary.distanceMeters,
                steps = summary.steps,
                heartRate = summary.averageHeartRate ?: 0,
                paceMsPerKm = summary.paceMsPerKm,
                isEnded = true,
            )
            // The final sequence is immutable. This transport-only nonce makes
            // an explicit retry observable even when the previous DataItem is
            // still stored under the same session path.
            dataMap.putLong(DataLayerContracts.KEY_DELIVERY_ATTEMPT, System.nanoTime())
        }.asPutDataRequest().setUrgent()

        Wearable.getDataClient(context).putDataItem(request)
            .addOnSuccessListener {
                Log.i(
                    TAG,
                    "Final summary placed in Data Layer; awaiting phone ACK " +
                        "session=${summary.sessionId} seq=${summary.sequenceNumber}",
                )
                val pending = summaryStore.orEmptyPending()
                val summaryStillPending = pending.any {
                    it.sessionId == summary.sessionId && it.sequenceNumber == summary.sequenceNumber
                }
                if (summaryStillPending) {
                    _uiState.value = _uiState.value.copy(
                        hasPendingSync = true,
                        syncStatus = SyncStatus.PENDING,
                        statusMessage = "Waiting for phone acknowledgement",
                    )
                } else {
                    _uiState.value = _uiState.value.copy(hasPendingSync = pending.isNotEmpty())
                }
            }
            .addOnFailureListener { error ->
                Log.e(
                    TAG,
                    "Final summary transport failed session=${summary.sessionId} seq=${summary.sequenceNumber}",
                    error,
                )
                val pending = summaryStore.orEmptyPending()
                val summaryStillPending = pending.any {
                    it.sessionId == summary.sessionId && it.sequenceNumber == summary.sequenceNumber
                }
                if (summaryStillPending) {
                    _uiState.value = _uiState.value.copy(
                        hasPendingSync = true,
                        syncStatus = SyncStatus.FAILED,
                        statusMessage = "Sync failed; will retry",
                    )
                }
            }
    }

    private fun PutDataMapRequest.putPayload(
        sessionId: Long,
        timestamp: Long,
        sequenceNumber: Long,
        elapsedMs: Long,
        distanceMeters: Double,
        steps: Long,
        heartRate: Int,
        paceMsPerKm: Long,
        isEnded: Boolean,
    ) {
        dataMap.putLong(DataLayerContracts.KEY_SESSION_ID, sessionId)
        dataMap.putLong(DataLayerContracts.KEY_TIMESTAMP, timestamp)
        dataMap.putLong(DataLayerContracts.KEY_SEQUENCE_NUMBER, sequenceNumber)
        dataMap.putLong(DataLayerContracts.KEY_ELAPSED_MS, elapsedMs)
        dataMap.putDouble(DataLayerContracts.KEY_DISTANCE_METERS, distanceMeters)
        dataMap.putLong(DataLayerContracts.KEY_STEPS, steps)
        dataMap.putInt(DataLayerContracts.KEY_HEART_RATE, heartRate)
        dataMap.putLong(DataLayerContracts.KEY_PACE_MS_PER_KM, paceMsPerKm)
        dataMap.putBoolean(DataLayerContracts.KEY_IS_ENDED, isEnded)
    }

    private fun startConnectionMonitoring() {
        if (connectionMonitoringStarted) return
        connectionMonitoringStarted = true
        val context = appContext ?: return
        val nodeClient = Wearable.getNodeClient(context)

        scope.launch {
            while (true) {
                runCatching { nodeClient.connectedNodes.await().isNotEmpty() }
                    .onSuccess { isConnected ->
                        val wasConnected = _uiState.value.isPhoneConnected
                        if (wasConnected != isConnected) {
                            _uiState.value = _uiState.value.copy(isPhoneConnected = isConnected)
                            Log.i(TAG, "Phone connection changed: connected=$isConnected")
                            if (!wasConnected && isConnected) retryPendingSummarySync()
                        }
                    }
                    .onFailure { Log.w(TAG, "Unable to query connected phone nodes", it) }
                delay(CONNECTION_POLL_INTERVAL_MS)
            }
        }
    }

    private fun checkCapabilities() {
        val client = exerciseClient ?: return
        scope.launch {
            runCatching { client.getCapabilities().getExerciseTypeCapabilities(ExerciseType.RUNNING) }
                .onSuccess { capabilities ->
                    val supported = supportsRequiredMetrics(capabilities)
                    _uiState.value = _uiState.value.copy(
                        capabilitiesChecked = true,
                        isSupported = supported,
                        errorMessage = if (supported) {
                            _uiState.value.errorMessage
                        } else {
                            "This watch must support heart rate, distance, and steps for running."
                        },
                    )
                }
                .onFailure { setCapabilityError(it.message ?: "Unable to read Health Services capabilities.") }
        }
    }

    private fun setCapabilityError(message: String) {
        Log.e(TAG, message)
        _uiState.value = _uiState.value.copy(
            phase = if (_uiState.value.phase == TrackingPhase.STARTING) TrackingPhase.ERROR else _uiState.value.phase,
            capabilitiesChecked = true,
            isSupported = false,
            statusMessage = null,
            errorMessage = message,
        )
    }

    private fun setFatalError(message: String) {
        _uiState.value = _uiState.value.copy(
            phase = TrackingPhase.ERROR,
            statusMessage = null,
            errorMessage = message,
        )
    }

    private fun supportsRequiredMetrics(capabilities: ExerciseTypeCapabilities): Boolean {
        return REQUIRED_DATA_TYPES.all { it in capabilities.supportedDataTypes }
    }

    private fun hasRequiredPermissions(context: Context): Boolean {
        val permissions = buildList {
            add(Manifest.permission.ACTIVITY_RECOGNITION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (Build.VERSION.SDK_INT >= 36) {
                add(READ_HEART_RATE_PERMISSION)
            } else {
                add(Manifest.permission.BODY_SENSORS)
            }
        }
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun clearCallback() {
        val client = exerciseClient ?: return
        scope.launch {
            runCatching { client.clearUpdateCallback(callback) }
                .onFailure { Log.w(TAG, "Unable to clear Health Services callback", it) }
        }
    }

    private fun calculateElapsedMs(
        baseDurationMs: Long,
        checkpointEpochMs: Long?,
        phase: TrackingPhase,
    ): Long {
        if (checkpointEpochMs == null) return baseDurationMs
        return if (phase == TrackingPhase.RUNNING) {
            baseDurationMs + (Instant.now().toEpochMilli() - checkpointEpochMs).coerceAtLeast(0L)
        } else {
            baseDurationMs
        }
    }

    private fun calculatePaceMsPerKm(elapsedMs: Long, distanceMeters: Double): Long {
        if (elapsedMs <= 0L || distanceMeters < 10.0) return 0L
        return (elapsedMs / (distanceMeters / 1_000.0)).toLong()
    }

    private fun RunSummaryStore?.orEmptyPending(): List<RunSummary> {
        return this?.pendingSummaries().orEmpty()
    }

    private val REQUIRED_DATA_TYPES = setOf(
        DataType.HEART_RATE_BPM,
        DataType.DISTANCE_TOTAL,
        DataType.STEPS_TOTAL,
    )
    private val ACTIVE_PHASES = setOf(
        TrackingPhase.STARTING,
        TrackingPhase.RUNNING,
        TrackingPhase.PAUSED,
        TrackingPhase.STOPPING,
    )
    private val TRACKABLE_PHASES = setOf(
        TrackingPhase.STARTING,
        TrackingPhase.RUNNING,
        TrackingPhase.PAUSED,
    )
    private const val READ_HEART_RATE_PERMISSION = "android.permission.health.READ_HEART_RATE"
}
