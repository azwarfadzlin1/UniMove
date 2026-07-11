package com.example.unimove.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.location.LocationCompat
import com.example.unimove.MainActivity
import com.example.unimove.R
import com.example.unimove.wear.WatchCommandType
import com.example.unimove.wear.WatchMessageGateway
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

data class TimedPoint(val location: Location, val timeMs: Long)

/**
 * Foreground service that keeps GPS tracking alive while the screen is off or
 * the user navigates away from the RunScreen.
 */
class RunTrackingService : Service() {

    // ── Binder ──────────────────────────────────────────────────────────────
    inner class RunBinder : Binder() {
        fun getService(): RunTrackingService = this@RunTrackingService
    }

    private val binder = RunBinder()
    override fun onBind(intent: Intent?): IBinder = binder

    override fun onUnbind(intent: Intent?): Boolean {
        if (!_isRunning.value) {
            stopSelf()
        }
        return super.onUnbind(intent)
    }

    // ── Service Lifecycle Scope ─────────────────────────────────────────────
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var timerJob: Job? = null
    private var paceStopJob: Job? = null
    private val watchMessageGateway by lazy { WatchMessageGateway.getInstance(applicationContext) }

    // ── State flows (observed by the bound RunScreen) ────────────────────────
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _distanceMeters = MutableStateFlow(0f)
    val distanceMeters: StateFlow<Float> = _distanceMeters.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _pathSegments = MutableStateFlow<List<List<LatLng>>>(emptyList())
    val pathSegments: StateFlow<List<List<LatLng>>> = _pathSegments.asStateFlow()

    private val _displayedPaceSecPerKm = MutableStateFlow(0f)
    val displayedPaceSecPerKm: StateFlow<Float> = _displayedPaceSecPerKm.asStateFlow()

    // ── Internal Tracking State ─────────────────────────────────────────────
    private var lastValidLocation: Location? = null
    private var lastAcceptedElapsedNs: Long? = null
    private var lastPolylineLocation: Location? = null
    private var sessionStartMs: Long = 0L
    private val movementAccumulator = GpsMovementAccumulator()
    private var acceptedDistanceMeters = 0f

    // Pace internal state
    private val pacePoints = mutableListOf<TimedPoint>()
    private var lastMoveWallMs: Long = 0L
    private var lastPaceUpdateWallMs: Long = 0L
    private var distAtLastPaceUpdateM: Float = 0f

    // ── Location client ──────────────────────────────────────────────────────
    private lateinit var fusedClient: FusedLocationProviderClient
    private var locationUpdatesActive = false

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY, 1000L
    )
        .setMinUpdateIntervalMillis(500L)
        .setMinUpdateDistanceMeters(0f)
        .setWaitForAccurateLocation(false)
        .build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            Log.d(TAG, "onLocationResult: ${result.locations.size} locations received")
            for (location in result.locations) {
                Log.d(TAG, "Location: lat=${location.latitude}, lng=${location.longitude}, acc=${location.accuracy}")
                updateDisplayLocation(location)
                if (!_isRunning.value || _isPaused.value) continue

                processLocation(location)
            }
        }
    }

    private fun processLocation(location: Location) {
        val nowWall = System.currentTimeMillis()
        Log.d(TAG, "processLocation: dist=${_distanceMeters.value}, time=${_elapsedSeconds.value}")

        // Stats point?
        val isValidForStats = isValidLocation(
            location = location,
            lastLocation = lastValidLocation,
            lastElapsedNs = lastAcceptedElapsedNs,
            sessionStartMs = sessionStartMs
        )
        
        if (!isValidForStats) {
            // Advance the observation anchor for every good-quality fix, even
            // while stationary. This prevents jitter from accumulating into a
            // fake step and lets a later walking fix be measured over its real
            // interval instead of against an arbitrarily old point.
            val inWarmUp = (System.currentTimeMillis() - sessionStartMs) < WARMUP_MS
            val anchorMaxAccuracy = if (inWarmUp) {
                MAX_WARMUP_STATS_ACCURACY_M
            } else {
                MAX_STATS_ACCURACY_M
            }
            val ageMs = System.currentTimeMillis() - location.time
            if (
                location.hasAccuracy() &&
                location.accuracy <= anchorMaxAccuracy &&
                ageMs in 0..STALE_FIX_MS
            ) {
                lastValidLocation = location
                lastAcceptedElapsedNs = location.elapsedRealtimeNanos
            }
            return
        }

        // The route must use the same accepted fixes as distance. Drawing raw
        // fixes here makes the path and Event Run marker jump even when the
        // distance filter correctly identifies GPS jitter.
        if (shouldAddPolylinePoint(location, lastPolylineLocation, sessionStartMs)) {
            val p = LatLng(location.latitude, location.longitude)
            val currentList = _pathSegments.value.toMutableList()
            if (currentList.isEmpty()) currentList.add(mutableListOf())

            val lastSegment = currentList.last().toMutableList()
            lastSegment.add(p)
            currentList[currentList.lastIndex] = lastSegment
            _pathSegments.value = currentList
            lastPolylineLocation = location
        }

        val prev = lastValidLocation
        val movementM = acceptedDistanceMeters
        
        lastMoveWallMs = nowWall

        // Distance
        if (prev != null) {
            _distanceMeters.value += movementM
            updateNotification()
        }

        lastValidLocation = location
        lastAcceptedElapsedNs = location.elapsedRealtimeNanos

        // Pace window
        pacePoints.add(TimedPoint(location, nowWall))

        while (pacePoints.isNotEmpty() && nowWall - pacePoints.first().timeMs > PACE_MAX_WINDOW_AGE_MS) {
            pacePoints.removeAt(0)
        }

        updatePace(nowWall)
    }

    private fun updatePace(nowWall: Long) {
        val dist = _distanceMeters.value
        val canShow = dist >= PACE_LOCK_DISTANCE_M
        val gainedSinceLast = dist - distAtLastPaceUpdateM

        if (canShow && gainedSinceLast >= PACE_UPDATE_STEP_M) {
            val paceRaw = computeRollingWindowPaceSecPerKm(pacePoints)
            val hasMinWindow = pacePoints.size >= 2 && (dist >= PACE_MIN_WINDOW_M)

            if (hasMinWindow && paceRaw > 0f) {
                val currentPace = _displayedPaceSecPerKm.value
                val alpha = if (currentPace > 0f && paceRaw < currentPace) 0.28f else 0.10f
                val base = if (currentPace > 0f) currentPace else paceRaw
                val smoothed = ema(base, paceRaw, alpha).coerceIn(0f, PACE_MAX_SEC_PER_KM)

                _displayedPaceSecPerKm.value = roundPaceToNearest5Sec(smoothed)
                distAtLastPaceUpdateM = dist
                lastPaceUpdateWallMs = nowWall
                updateNotification()
            }
        }
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        
        // Start persistent location monitoring for auto-centering
        requestBackgroundLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun requestBackgroundLocationUpdates() {
        if (hasLocationPermission()) {
            if (locationUpdatesActive) {
                Log.d(TAG, "requestBackgroundLocationUpdates: already active")
                return
            }
            Log.d(TAG, "requestBackgroundLocationUpdates: permissions granted, requesting...")
            locationUpdatesActive = true
            fusedClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                .addOnFailureListener { error ->
                    locationUpdatesActive = false
                    Log.e(TAG, "Failed to register location updates", error)
                }
            if (_currentLocation.value == null) {
                seedCurrentLocation(anchorForActiveRun = false, preferFreshFix = false)
            }
        } else {
            Log.d(TAG, "requestBackgroundLocationUpdates: permissions NOT granted")
        }
    }

    @SuppressLint("MissingPermission")
    private fun seedCurrentLocation(anchorForActiveRun: Boolean, preferFreshFix: Boolean) {
        if (!hasLocationPermission()) return

        val onLocation: (Location?) -> Unit = { location ->
            if (location == null) {
                Log.d(TAG, "seedCurrentLocation: no location available")
            } else {
                Log.d(
                    TAG,
                    "seedCurrentLocation: lat=${location.latitude}, lng=${location.longitude}, acc=${location.accuracy}"
                )
                updateDisplayLocation(location)
                if (anchorForActiveRun) {
                    anchorSessionStart(location)
                }
            }
        }

        if (preferFreshFix) {
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        onLocation(location)
                    } else {
                        fusedClient.lastLocation.addOnSuccessListener(onLocation)
                    }
                }
                .addOnFailureListener {
                    Log.w(TAG, "seedCurrentLocation: getCurrentLocation failed", it)
                    fusedClient.lastLocation.addOnSuccessListener(onLocation)
                }
        } else {
            fusedClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    onLocation(location)
                } else {
                    fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener(onLocation)
                }
            }
        }
    }

    private fun anchorSessionStart(location: Location) {
        if (!_isRunning.value || _isPaused.value) return

        val ageMs = System.currentTimeMillis() - location.time
        val okAcc = location.hasAccuracy() && location.accuracy <= START_ANCHOR_MAX_ACC_M
        val okAge = ageMs in 0..START_ANCHOR_MAX_AGE_MS
        if (!okAcc || !okAge) {
            Log.d(TAG, "anchorSessionStart: skip anchor age=$ageMs acc=${location.accuracy}")
            return
        }

        lastValidLocation = location
        lastAcceptedElapsedNs = location.elapsedRealtimeNanos
        lastPolylineLocation = location
        pacePoints.clear()
        pacePoints.add(TimedPoint(location, System.currentTimeMillis()))
        _pathSegments.value = listOf(listOf(LatLng(location.latitude, location.longitude)))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val watchCommand = when (intent?.action) {
            ACTION_START -> WatchCommandType.START.also { handleStart() }
            ACTION_PAUSE -> WatchCommandType.PAUSE.also { handlePause() }
            ACTION_RESUME -> WatchCommandType.RESUME.also { handleResume() }
            ACTION_STOP -> WatchCommandType.STOP.also { handleStop() }
            else -> null
        }

        if (watchCommand != null) {
            // Launch from the service's Main dispatcher so rapid START/PAUSE/
            // RESUME/STOP intents enter the gateway in the same order received.
            serviceScope.launch {
                try {
                    watchMessageGateway.sendCommandToConnectedNodes(watchCommand)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    // Watch support is optional and must never terminate a
                    // phone-only run if Play Services or Data Layer fails.
                    Log.w(TAG, "Watch sync failed for ${watchCommand.name}; continuing phone-only", error)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        fusedClient.removeLocationUpdates(locationCallback)
        locationUpdatesActive = false
        timerJob?.cancel()
        paceStopJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    // ── Control API ──────────────────────────────────────────────────────────
    fun start() = handleStart()
    fun refreshLocationUpdates() = requestBackgroundLocationUpdates()

    private fun handleStart() {
        Log.d(TAG, "handleStart: isRunning=${_isRunning.value}")
        if (_isRunning.value) return
        resetSessionData()
        _isRunning.value = true
        _isPaused.value = false
        sessionStartMs = System.currentTimeMillis()
        lastMoveWallMs = sessionStartMs
        
        requestBackgroundLocationUpdates()
        seedCurrentLocation(anchorForActiveRun = true, preferFreshFix = true)
        
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startTimer()
        startPaceStopMonitor()
    }

    private fun handlePause() {
        if (!_isRunning.value || _isPaused.value) return
        _isPaused.value = true
        timerJob?.cancel()
        paceStopJob?.cancel()
        updateNotification()
    }

    private fun handleResume() {
        if (!_isRunning.value || !_isPaused.value) return
        _isPaused.value = false
        
        // Reset anchors for resume to avoid jumps
        lastValidLocation = null
        lastAcceptedElapsedNs = null
        lastPolylineLocation = null
        pacePoints.clear()
        movementAccumulator.reset()
        acceptedDistanceMeters = 0f
        lastPaceUpdateWallMs = 0L
        distAtLastPaceUpdateM = _distanceMeters.value
        lastMoveWallMs = System.currentTimeMillis()
        
        // Add new segment
        val currentList = _pathSegments.value.toMutableList()
        currentList.add(mutableListOf())
        _pathSegments.value = currentList

        startTimer()
        startPaceStopMonitor()
        updateNotification()
    }

    private fun handleStop() {
        _isRunning.value = false
        _isPaused.value = false
        timerJob?.cancel()
        paceStopJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun resetSessionData() {
        _currentLocation.value = null
        _distanceMeters.value = 0f
        _elapsedSeconds.value = 0
        _pathSegments.value = listOf(mutableListOf())
        _displayedPaceSecPerKm.value = 0f
        acceptedDistanceMeters = 0f
        movementAccumulator.reset()
        
        lastValidLocation = null
        lastAcceptedElapsedNs = null
        lastPolylineLocation = null
        pacePoints.clear()
        lastMoveWallMs = 0L
        lastPaceUpdateWallMs = 0L
        distAtLastPaceUpdateM = 0f
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (true) {
                delay(1000)
                _elapsedSeconds.value++
                updateNotification()
            }
        }
    }

    private fun startPaceStopMonitor() {
        paceStopJob?.cancel()
        paceStopJob = serviceScope.launch {
            while (true) {
                delay(1000)
                if (_displayedPaceSecPerKm.value <= 0f) continue

                val nowWall = System.currentTimeMillis()
                val stopped = (nowWall - lastMoveWallMs) > 1500L
                if (!stopped) continue

                if (lastPaceUpdateWallMs == 0L) {
                    lastPaceUpdateWallMs = nowWall
                    continue
                }

                val since = nowWall - lastPaceUpdateWallMs
                if (since >= STOP_STEP_MS) {
                    val bumped = (_displayedPaceSecPerKm.value + STOP_STEP_ADD_SEC).coerceAtMost(PACE_MAX_SEC_PER_KM)
                    _displayedPaceSecPerKm.value = roundPaceToNearest5Sec(bumped)
                    lastPaceUpdateWallMs = nowWall
                    updateNotification()
                }
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    // ── Notification ─────────────────────────────────────────────────────────
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Run Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows your live run stats while tracking is active"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val distText = String.format(Locale.getDefault(), "%.2f km", _distanceMeters.value / 1000f)
        val timeText = formatTime(_elapsedSeconds.value)
        val paceText = formatPace(_displayedPaceSecPerKm.value)

        val statusLine = when {
            _isPaused.value -> "Paused  •  $distText"
            else            -> "$distText  •  $timeText  •  $paceText"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_run)
            .setContentTitle(if (_isPaused.value) "Run Paused" else "Run in Progress")
            .setContentText(statusLine)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(tapIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    fun updateNotification() {
        if (!_isRunning.value) return
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification())
    }

    // ── Helpers & Filtering (moved from RunScreen) ──────────────────────────
    private fun formatTime(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }

    private fun formatPace(paceSecPerKm: Float): String {
        if (paceSecPerKm <= 0f) return "--/km"
        val m = (paceSecPerKm / 60).toInt()
        val s = (paceSecPerKm % 60).toInt()
        return "%d:%02d/km".format(m, s)
    }

    private fun isTooOldForSession(locationTime: Long, sessionStartMs: Long): Boolean {
        if (sessionStartMs <= 0L) return true
        val diff = sessionStartMs - locationTime
        return diff > 2_000L && diff > STALE_FIX_MS
    }

    private fun shouldAddPolylinePoint(location: Location, last: Location?, sessionStartMs: Long): Boolean {
        if (sessionStartMs <= 0L) return false
        if (!location.hasAccuracy() || location.accuracy > MAX_STATS_ACCURACY_M) return false
        val nowWall = System.currentTimeMillis()
        val justStarted = (nowWall - sessionStartMs) < START_GRACE_MS
        val ageMs = nowWall - location.time
        val staleLimit = if (justStarted) START_STALE_LIMIT_MS else STALE_FIX_MS
        if (ageMs > staleLimit) return false
        if (isTooOldForSession(location.time, sessionStartMs)) return false
        if (last == null) return true
        return last.distanceTo(location) >= MIN_ROUTE_STEP_M
    }

    /**
     * Publishes a stable location for map markers and camera following.
     * Raw fused fixes still enter the tracking filter, but low-quality,
     * stationary and implausibly fast changes never reach the UI.
     */
    private fun updateDisplayLocation(location: Location) {
        if (!location.hasAccuracy() || location.accuracy > MAX_DISPLAY_ACCURACY_M) {
            Log.d(TAG, "displayLocation: REJECT - poor accuracy (${location.accuracy})")
            return
        }

        val ageMs = System.currentTimeMillis() - location.time
        if (ageMs !in 0..MAX_DISPLAY_FIX_AGE_MS) {
            Log.d(TAG, "displayLocation: REJECT - stale ($ageMs ms)")
            return
        }

        val previous = _currentLocation.value
        if (previous == null) {
            _currentLocation.value = Location(location)
            return
        }

        val movementM = previous.distanceTo(location)
        val dtS = (location.elapsedRealtimeNanos - previous.elapsedRealtimeNanos) / 1_000_000_000f
        if (dtS <= 0f) return

        val coordinateSpeedMps = movementM / dtS
        if (!LocationCompat.isMock(location) && coordinateSpeedMps > MAX_DISPLAY_SPEED_MPS) {
            Log.d(TAG, "displayLocation: REJECT - jump ($coordinateSpeedMps m/s)")
            return
        }
        if (coordinateSpeedMps < MIN_DISPLAY_COORDINATE_SPEED_MPS) {
            Log.d(TAG, "displayLocation: HOLD - stationary coordinate speed ($coordinateSpeedMps m/s)")
            return
        }

        if (location.hasSpeed() && location.speed < MIN_REPORTED_SPEED_MPS) {
            val substantiallyBetter =
                location.accuracy <= previous.accuracy * DISPLAY_ACCURACY_IMPROVEMENT_RATIO
            if (substantiallyBetter) {
                _currentLocation.value = Location(location)
            } else {
                Log.d(TAG, "displayLocation: HOLD - device stationary")
            }
            return
        }

        if (movementM < MIN_DISPLAY_STEP_M) return

        // Low-pass only genuine movement so the marker and following camera
        // progress smoothly instead of snapping to each individual fix.
        _currentLocation.value = Location(location).apply {
            latitude = previous.latitude + (location.latitude - previous.latitude) * DISPLAY_SMOOTHING_ALPHA
            longitude = previous.longitude + (location.longitude - previous.longitude) * DISPLAY_SMOOTHING_ALPHA
        }
    }

    private fun isValidLocation(location: Location, lastLocation: Location?, lastElapsedNs: Long?, sessionStartMs: Long): Boolean {
        acceptedDistanceMeters = 0f
        if (!location.hasAccuracy()) {
            Log.d(TAG, "isValidLocation: REJECT - no accuracy")
            return false
        }
        val nowWall = System.currentTimeMillis()
        val justStarted = (sessionStartMs > 0L) && (nowWall - sessionStartMs) < START_GRACE_MS
        val ageMs = nowWall - location.time
        val staleLimit = if (justStarted) START_STALE_LIMIT_MS else STALE_FIX_MS
        if (ageMs > staleLimit) {
            Log.d(TAG, "isValidLocation: REJECT - stale (ageMs=$ageMs, limit=$staleLimit)")
            return false
        }
        if (isTooOldForSession(location.time, sessionStartMs)) {
            Log.d(TAG, "isValidLocation: REJECT - too old for session")
            return false
        }
        val inWarmUp = (System.currentTimeMillis() - sessionStartMs) < WARMUP_MS
        val maxAcc = if (inWarmUp) MAX_WARMUP_STATS_ACCURACY_M else MAX_STATS_ACCURACY_M
        if (location.accuracy > maxAcc) {
            Log.d(TAG, "isValidLocation: REJECT - poor accuracy (${location.accuracy} > $maxAcc)")
            return false
        }
        if (lastLocation == null || lastElapsedNs == null) {
            Log.d(TAG, "isValidLocation: ACCEPT - first point")
            return true
        }
        val dtS = (location.elapsedRealtimeNanos - lastElapsedNs) / 1_000_000_000f
        if (dtS <= 0f) {
            Log.d(TAG, "isValidLocation: REJECT - non-positive dt")
            return false
        }
        val movementM = lastLocation.distanceTo(location)
        val speedMps = movementM / dtS
        val maxSpeed = if (LocationCompat.isMock(location)) TELEPORT_SPEED_MPS_MOCK else TELEPORT_SPEED_MPS
        if (speedMps > maxSpeed) {
            movementAccumulator.reset()
            Log.d(TAG, "isValidLocation: REJECT - teleport ($speedMps m/s)")
            return false
        }
        acceptedDistanceMeters = movementAccumulator.addStep(
            distanceMeters = movementM,
            elapsedSeconds = dtS,
            hasReportedSpeed = location.hasSpeed(),
            reportedSpeedMps = location.speed,
            maximumSpeedMps = maxSpeed
        )
        val accepted = acceptedDistanceMeters > 0f
        if (!accepted) {
            Log.d(
                TAG,
                "isValidLocation: REJECT - no credible movement " +
                    "(distance=$movementM, coordinateSpeed=$speedMps, reportedSpeed=${location.speed})"
            )
        } else {
            Log.d(
                TAG,
                "isValidLocation: ACCEPT - rawMovement=$movementM, counted=$acceptedDistanceMeters, " +
                    "coordinateSpeed=$speedMps, reportedSpeed=${location.speed}"
            )
        }
        return accepted
    }

    private fun computeRollingWindowPaceSecPerKm(points: List<TimedPoint>): Float {
        if (points.size < 2) return 0f
        var distM = 0f
        var startIndex = points.lastIndex
        for (i in points.lastIndex downTo 1) {
            val step = points[i - 1].location.distanceTo(points[i].location)
            distM += step
            startIndex = i - 1
            if (distM >= PACE_WINDOW_M) break
        }
        if (distM <= 0f) return 0f
        val dtMs = points.last().timeMs - points[startIndex].timeMs
        if (dtMs <= 0L) return 0f
        val paceSecPerKm = (dtMs / 1000f) / (distM / 1000f)
        return paceSecPerKm.coerceAtMost(PACE_MAX_SEC_PER_KM)
    }

    private fun ema(old: Float, new: Float, alpha: Float): Float {
        if (old <= 0f) return new
        return old + alpha * (new - old)
    }

    private fun roundPaceToNearest5Sec(paceSecPerKm: Float): Float {
        if (paceSecPerKm <= 0f) return 0f
        return (paceSecPerKm / 5f).roundToInt() * 5f
    }

    companion object {
        private const val TAG = "RunTrackingService"
        const val CHANNEL_ID      = "run_tracking_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START        = "ACTION_RUN_START"
        const val ACTION_PAUSE        = "ACTION_RUN_PAUSE"
        const val ACTION_RESUME       = "ACTION_RUN_RESUME"
        const val ACTION_STOP         = "ACTION_RUN_STOP"

        // Tracking / filtering constants
        private const val WARMUP_MS = 25_000L
        private const val STALE_FIX_MS = 10_000L
        private const val TELEPORT_SPEED_MPS = 8f
        private const val TELEPORT_SPEED_MPS_MOCK = 50f
        private const val START_GRACE_MS = 7_000L
        private const val START_STALE_LIMIT_MS = 30_000L
        private const val MAX_WARMUP_STATS_ACCURACY_M = 50f
        private const val MAX_STATS_ACCURACY_M = 50f
        private const val MIN_ROUTE_STEP_M = 0.8f
        private const val MAX_DISPLAY_ACCURACY_M = 45f
        private const val MAX_DISPLAY_FIX_AGE_MS = 10_000L
        private const val MAX_DISPLAY_SPEED_MPS = 10f
        private const val MIN_DISPLAY_COORDINATE_SPEED_MPS = 0.55f
        private const val MIN_DISPLAY_STEP_M = 2f
        private const val DISPLAY_ACCURACY_IMPROVEMENT_RATIO = 0.65f
        private const val DISPLAY_SMOOTHING_ALPHA = 0.65
        // Below ~0.75 m/s on this device, stationary GPS drift can accumulate
        // 5-7 metres between sparse fused updates. Normal walking is typically
        // above this boundary; slower changes remain anchored until a credible
        // movement fix arrives.
        private const val MIN_REPORTED_SPEED_MPS = 0.45f
        private const val PACE_LOCK_DISTANCE_M = 25f
        private const val PACE_UPDATE_STEP_M = 15f
        private const val PACE_WINDOW_M = 60f
        private const val PACE_MIN_WINDOW_M = 30f
        private const val PACE_MAX_WINDOW_AGE_MS = 20_000L
        private const val STOP_STEP_MS = 5_000L
        private const val STOP_STEP_ADD_SEC = 25f
        private const val PACE_MAX_SEC_PER_KM = 30 * 60f
        private const val START_ANCHOR_MAX_AGE_MS = 3_000L
        private const val START_ANCHOR_MAX_ACC_M = 35f
    }
}
