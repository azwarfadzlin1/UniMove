package com.example.unimove.ui.screens

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.unimove.R
import com.example.unimove.data.model.Challenge
import com.example.unimove.service.RunTrackingService
import com.example.unimove.ui.viewmodel.RunViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunScreen(
    navController: NavController,
    viewModel: RunViewModel = viewModel(),
    challengeId: String? = null,
    challengeTitle: String? = null,
    targetDistanceKm: Double? = null
) {
    val context = LocalContext.current
    val activeChallenge by viewModel.activeChallenge.collectAsStateWithLifecycle()
    val hasLocationPermissionCheck = remember(context) { { hasLocationPermission(context) } }
    
    // ── Foreground service binding ──────────────────────────────────────────
    var boundService by remember { mutableStateOf<RunTrackingService?>(null) }
    var isServiceBound by remember { mutableStateOf(false) }
    val serviceConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                Log.d("RunScreen", "Service connected")
                boundService = (service as? RunTrackingService.RunBinder)?.getService()
                isServiceBound = true
            }
            override fun onServiceDisconnected(name: ComponentName?) { 
                Log.d("RunScreen", "Service disconnected")
                boundService = null 
                isServiceBound = false
            }
        }
    }

    DisposableEffect(Unit) {
        val intent = Intent(context, RunTrackingService::class.java)
        isServiceBound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        onDispose {
            if (isServiceBound) {
                context.unbindService(serviceConnection)
                isServiceBound = false
            }
        }
    }

    val isRunning by remember(boundService) {
        boundService?.isRunning ?: MutableStateFlow(false)
    }.collectAsStateWithLifecycle()
    
    val isPaused by remember(boundService) {
        boundService?.isPaused ?: MutableStateFlow(false)
    }.collectAsStateWithLifecycle()

    val distanceMeters by remember(boundService) {
        boundService?.distanceMeters ?: MutableStateFlow(0f)
    }.collectAsStateWithLifecycle()
    
    val elapsedSeconds by remember(boundService) {
        boundService?.elapsedSeconds ?: MutableStateFlow(0)
    }.collectAsStateWithLifecycle()
    
    val displayedPaceSecPerKm by remember(boundService) {
        boundService?.displayedPaceSecPerKm ?: MutableStateFlow(0f)
    }.collectAsStateWithLifecycle()
    
    val pathSegments by remember(boundService) {
        boundService?.pathSegments ?: MutableStateFlow(emptyList())
    }.collectAsStateWithLifecycle()
    
    val currentLocation by remember(boundService) {
        boundService?.currentLocation ?: MutableStateFlow(null)
    }.collectAsStateWithLifecycle()
    
    LaunchedEffect(currentLocation) {
        val loc = currentLocation
        if (loc != null) {
            Log.d("RunScreen", "UI received location: ${loc.latitude}, ${loc.longitude}")
        }
    }

    // UI state
    val snackbarHostState = remember { SnackbarHostState() }
    val density = LocalDensity.current
    val isChallengeMode = !challengeId.isNullOrBlank()
    
    // Auto-centering state
    var isMapFollowingUser by remember { mutableStateOf(true) }
    var hasInitiallyCentered by remember { mutableStateOf(false) }

    // Map/path
    val cameraPositionState = rememberCameraPositionState()
    val currentLatLng = currentLocation?.let { LatLng(it.latitude, it.longitude) }
    val currentAccuracyM = currentLocation?.accuracy

    // Controls panel measurement
    var controlsHeightDp by remember { mutableStateOf(0.dp) }
    val navBarsBottomDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val mapBottomPadding = remember(controlsHeightDp, navBarsBottomDp) {
        controlsHeightDp + navBarsBottomDp + 12.dp
    }

    val resolvedChallenge = activeChallenge?.takeIf { it.targetDistanceKm > 0.0 || it.title.isNotBlank() }
        ?: if (isChallengeMode) {
            Challenge(
                id = challengeId.orEmpty(),
                title = challengeTitle.orEmpty().ifBlank { "Challenge Run" },
                targetDistanceKm = targetDistanceKm ?: 0.0
            )
        } else {
            null
        }
    val challengeTargetMeters = (((resolvedChallenge?.targetDistanceKm ?: 0.0)) * 1000.0).toFloat()

    // Permission & errors
    var permissionGranted by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionGranted = result.values.any { it }
        if (!permissionGranted) {
            errorMessage = "Location permission is required to start tracking."
        } else {
            boundService?.refreshLocationUpdates()
        }
    }

    // Save state
    var showSaveButton by rememberSaveable { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isCompletingChallenge by rememberSaveable { mutableStateOf(false) }
    var showCancelRunDialog by rememberSaveable { mutableStateOf(false) }

    // Request POST_NOTIFICATIONS
    val notifPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        permissionGranted = hasLocationPermissionCheck()
        MapsInitializer.initialize(context, MapsInitializer.Renderer.LATEST) {}
    }

    // Auto-centering and following logic
    LaunchedEffect(currentLatLng) {
        val pos = currentLatLng ?: return@LaunchedEffect
        Log.d("RunScreen", "Location updated in UI: ${pos.latitude}, ${pos.longitude}")
        
        // Initial center
        if (!hasInitiallyCentered) {
            Log.d("RunScreen", "Initial centering on $pos")
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(pos, 18.5f))
            hasInitiallyCentered = true
        }

        // Auto-follow if enabled
        if (isMapFollowingUser) {
            val zoom = cameraPositionState.position.zoom.coerceAtLeast(17f)
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(pos, zoom))
        }
    }

    // Detect manual map movement to disable auto-follow
    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.isMoving && cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE) {
            isMapFollowingUser = false
        }
    }

    LaunchedEffect(challengeId) {
        viewModel.observeChallenge(challengeId)
    }

    val hasCancelableRun = (isRunning || showSaveButton) && !isSaving && !isCompletingChallenge
    val requestBackNavigation = remember(
        navController,
        context,
        hasCancelableRun,
        isSaving,
        isCompletingChallenge
    ) {
        {
            when {
                isSaving || isCompletingChallenge -> Unit
                hasCancelableRun -> showCancelRunDialog = true
                else -> {
                    navController.navigateUp()
                    Unit
                }
            }
        }
    }

    BackHandler(enabled = hasCancelableRun || isSaving || isCompletingChallenge) {
        requestBackNavigation()
    }

    // Challenge Completion Logic
    LaunchedEffect(isChallengeMode, activeChallenge?.id, challengeTargetMeters, distanceMeters, isRunning, isPaused, isCompletingChallenge) {
        val challenge = resolvedChallenge ?: return@LaunchedEffect
        if (!isChallengeMode || challengeTargetMeters <= 0f || !isRunning || isPaused || isCompletingChallenge) {
            return@LaunchedEffect
        }
        if (distanceMeters < challengeTargetMeters) return@LaunchedEffect

        isCompletingChallenge = true
        isSaving = true
        showSaveButton = false
        
        dispatchRunServiceAction(context, RunTrackingService.ACTION_STOP)

        viewModel.completeChallengeRun(
            challenge = challenge,
            distanceMeters = distanceMeters,
            elapsedSeconds = elapsedSeconds,
            smoothedPaceSecPerKm = displayedPaceSecPerKm,
            pathSegments = pathSegments.map { segment -> segment.map { mapOf("lat" to it.latitude, "lng" to it.longitude) } },
            onSuccess = {
                isSaving = false
                navController.navigate("challenge_result/${challenge.id}") {
                    popUpTo("challenge") { inclusive = false }
                }
            },
            onError = { message ->
                isSaving = false
                isCompletingChallenge = false
                errorMessage = message
                showSaveButton = distanceMeters > 20f
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = Color.Unspecified,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            MapLayer(
                cameraPositionState = cameraPositionState,
                pathSegments = pathSegments, 
                currentLatLng = currentLatLng,
                currentAccuracyM = currentAccuracyM,
                bottomContentPadding = mapBottomPadding,
                isFollowingUser = isMapFollowingUser,
                onReCenter = { isMapFollowingUser = true }
            )

            FloatingBackButton(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding(),
                onBack = requestBackNavigation
            )

            if (isChallengeMode) {
                if (resolvedChallenge != null) {
                    ChallengeFlightHud(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 8.dp, start = 16.dp, end = 16.dp),
                        challenge = resolvedChallenge,
                        distanceMeters = distanceMeters,
                        elapsedSeconds = elapsedSeconds,
                        isRunning = isRunning && !isPaused
                    )
                } else {
                    ChallengeActiveBadge(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(top = 8.dp)
                    )
                }
            }

            ControlsLayer(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .imePadding()
                    .onSizeChanged { size ->
                        controlsHeightDp = with(density) { size.height.toDp() }
                    },
                isRunning = isRunning,
                isPaused = isPaused,
                showSaveButton = showSaveButton,
                isSaving = isSaving,
                isChallengeMode = isChallengeMode,
                distanceMeters = distanceMeters,
                elapsedSeconds = elapsedSeconds,
                paceInSecondsPerKm = displayedPaceSecPerKm,
                challengeTargetMeters = challengeTargetMeters,
                onStartOrResume = {
                    if (isChallengeMode && resolvedChallenge == null) {
                        errorMessage = "Challenge details are still loading."
                        return@ControlsLayer
                    }

                    if (!hasLocationPermissionCheck()) {
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                        return@ControlsLayer
                    }

                    showSaveButton = false
                    errorMessage = null
                    isMapFollowingUser = true
                    hasInitiallyCentered = false

                    if (isRunning && isPaused) {
                        dispatchRunServiceAction(context, RunTrackingService.ACTION_RESUME)
                    } else {
                        dispatchRunServiceAction(context, RunTrackingService.ACTION_START)
                    }
                },
                onPause = {
                    if (!isRunning || isSaving || isCompletingChallenge) return@ControlsLayer
                    dispatchRunServiceAction(context, RunTrackingService.ACTION_PAUSE)
                },
                onStop = {
                    if (isCompletingChallenge) return@ControlsLayer
                    dispatchRunServiceAction(context, RunTrackingService.ACTION_STOP)
                    showSaveButton = distanceMeters > 20f
                },
                onSave = {
                    isSaving = true
                    viewModel.setPendingSession(
                        distanceMeters = distanceMeters,
                        elapsedSeconds = elapsedSeconds,
                        smoothedPaceSecPerKm = displayedPaceSecPerKm,
                        pathSegments = pathSegments.map { segment -> segment.map { mapOf("lat" to it.latitude, "lng" to it.longitude) } }
                    )
                    isSaving = false
                    showSaveButton = false
                    navController.navigate("session_summary") {
                        popUpTo("home") { inclusive = false }
                    }
                },
                errorMessage = errorMessage
            )

            if (isCompletingChallenge) {
                ChallengeCompletionOverlay(
                    modifier = Modifier.fillMaxSize(),
                    challengeTitle = resolvedChallenge?.title.orEmpty()
                )
            }
        }
    }

    if (showCancelRunDialog) {
        AlertDialog(
            onDismissRequest = { showCancelRunDialog = false },
            title = { Text("Cancel run?") },
            text = { Text("Your current run progress will be discarded.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelRunDialog = false
                        showSaveButton = false
                        errorMessage = null
                        dispatchRunServiceAction(context, RunTrackingService.ACTION_STOP)
                        navController.navigateUp()
                    }
                ) {
                    Text("Cancel Run")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelRunDialog = false }) {
                    Text("Keep Running")
                }
            }
        )
    }
}

@Composable
private fun MapLayer(
    cameraPositionState: CameraPositionState,
    pathSegments: List<List<LatLng>>,
    currentLatLng: LatLng?,
    currentAccuracyM: Float?,
    bottomContentPadding: Dp,
    isFollowingUser: Boolean,
    onReCenter: () -> Unit
) {
    val context = LocalContext.current
    val mapStyleOptions = remember {
        MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style)
    }
    val stravaBlue = Color(0xFF2F80FF)

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapStyleOptions = mapStyleOptions
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false
            ),
            contentPadding = PaddingValues(bottom = bottomContentPadding)
        ) {
            pathSegments.forEach { seg ->
                if (seg.size > 1) {
                    Polyline(
                        points = seg,
                        color = stravaBlue,
                        width = 14f
                    )
                }
            }

            currentLatLng?.let { pos ->
                val acc = (currentAccuracyM ?: 0f)
                val ringRadius = max(18f, acc)

                Circle(
                    center = pos,
                    radius = ringRadius.toDouble(),
                    fillColor = stravaBlue.copy(alpha = 0.18f),
                    strokeColor = stravaBlue.copy(alpha = 0.55f),
                    strokeWidth = 4f
                )

                Circle(
                    center = pos,
                    radius = 7.0,
                    fillColor = stravaBlue,
                    strokeColor = Color.White,
                    strokeWidth = 8f
                )
            }
        }

        if (!isFollowingUser && currentLatLng != null) {
            Button(
                onClick = onReCenter,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = bottomContentPadding + 16.dp, end = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NearMe,
                    contentDescription = "Re-center",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ControlsLayer(
    modifier: Modifier = Modifier,
    isRunning: Boolean,
    isPaused: Boolean,
    showSaveButton: Boolean,
    isSaving: Boolean,
    isChallengeMode: Boolean,
    distanceMeters: Float,
    elapsedSeconds: Int,
    paceInSecondsPerKm: Float,
    challengeTargetMeters: Float,
    onStartOrResume: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onSave: () -> Unit,
    errorMessage: String?
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        color = Color(0xFF0F1115),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 12.dp
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StatsSection(
                distanceMeters = distanceMeters,
                elapsedSeconds = elapsedSeconds,
                paceInSecondsPerKm = paceInSecondsPerKm,
                isChallengeMode = isChallengeMode,
                challengeTargetMeters = challengeTargetMeters
            )

            ControlButtons(
                isRunning = isRunning,
                isPaused = isPaused,
                isSaving = isSaving,
                showSaveButton = showSaveButton,
                isChallengeMode = isChallengeMode,
                onStartOrResume = onStartOrResume,
                onPause = onPause,
                onStop = onStop,
                onSave = onSave
            )

            if (isChallengeMode) {
                Text(
                    text = "Challenge mode finishes automatically once you reach the full course distance.",
                    color = Color(0xFF9AA3AF),
                    fontSize = 12.sp
                )
            }

            errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun StatsSection(
    distanceMeters: Float,
    elapsedSeconds: Int,
    paceInSecondsPerKm: Float,
    isChallengeMode: Boolean,
    challengeTargetMeters: Float
) {
    val remainingMeters = if (isChallengeMode && challengeTargetMeters > 0f) {
        (challengeTargetMeters - distanceMeters).coerceAtLeast(0f)
    } else {
        0f
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RunStat(
            modifier = Modifier.weight(1f),
            label = if (isChallengeMode) "Race Time" else "Time",
            value = formatTime(elapsedSeconds)
        )
        RunStat(
            modifier = Modifier.weight(1f),
            label = if (isChallengeMode) "Km Left" else "Distance",
            value = if (isChallengeMode) formatDistance(remainingMeters) else formatDistance(distanceMeters)
        )
        RunStat(modifier = Modifier.weight(1f), label = "Pace", value = formatPace(paceInSecondsPerKm))
    }
}

@Composable
private fun ControlButtons(
    isRunning: Boolean,
    isPaused: Boolean,
    isSaving: Boolean,
    showSaveButton: Boolean,
    isChallengeMode: Boolean,
    onStartOrResume: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onSave: () -> Unit
) {
    androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        if (!isRunning) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onStartOrResume,
                enabled = !isSaving,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isChallengeMode) Color(0xFF2E7CF6) else Color(0xFF2A2F3A)
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(if (isChallengeMode) "Start Race" else "Start", maxLines = 1)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val leftLabel = if (isPaused) "Resume" else "Pause"
                val leftIcon = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { if (isPaused) onStartOrResume() else onPause() },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A3F4A))
                ) {
                    Icon(leftIcon, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(leftLabel, maxLines = 1, softWrap = false)
                }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onStop,
                    enabled = !isSaving,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFC4C02))
                ) {
                    Text(
                        if (isChallengeMode) "End Attempt" else "Stop",
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        if (showSaveButton) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onSave,
                enabled = !isSaving,
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text(
                        if (isChallengeMode) "Save Run Only" else "Save Session",
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ChallengeFlightHud(
    modifier: Modifier = Modifier,
    challenge: Challenge,
    distanceMeters: Float,
    elapsedSeconds: Int,
    isRunning: Boolean
) {
    val targetMeters = (challenge.targetDistanceKm * 1000.0).toFloat().coerceAtLeast(1f)
    val progress = (distanceMeters / targetMeters).coerceIn(0f, 1f)
    val remainingMeters = (targetMeters - distanceMeters).coerceAtLeast(0f)
    val transition = rememberInfiniteTransition(label = "challenge_flight")
    val bobbing by transition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "challenge_flight_bob"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xCC081120)
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = challenge.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isRunning) "Race clock is live" else "Ready at the line",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E3A8A)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatTime(elapsedSeconds),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            ) {
                val density = LocalDensity.current
                val maxTravelPx = with(density) { maxWidth.toPx() - 28.dp.toPx() }

                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth()
                        .height(10.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White.copy(alpha = 0.12f)
                ) {}

                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth(progress.coerceAtLeast(0.02f))
                        .height(10.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFF38BDF8)
                ) {}

                Icon(
                    imageVector = Icons.Default.NearMe,
                    contentDescription = null,
                    tint = Color(0xFFFFC83D),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset {
                            IntOffset(
                                x = (progress * maxTravelPx).roundToInt(),
                                y = bobbing.roundToInt()
                            )
                        }
                        .size(26.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatDistance(remainingMeters)} left",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${formatDistance(distanceMeters)} / ${formatDistance(targetMeters)}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun ChallengeCompletionOverlay(
    modifier: Modifier = Modifier,
    challengeTitle: String
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF0F172A)
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFFC83D),
                    modifier = Modifier.size(44.dp)
                )
                Text(
                    text = "Finish line reached",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = challengeTitle.ifBlank { "Saving challenge result..." },
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 14.sp
                )
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

@Composable
private fun RunStat(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    BoxWithConstraints(modifier = modifier) {
        val compact = maxWidth < 110.dp
        val valueSize = if (compact) 18.sp else 24.sp

        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                label,
                fontSize = 12.sp,
                color = Color(0xFF9AA3AF),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                value,
                fontSize = valueSize,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun FloatingBackButton(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    Surface(
        modifier = modifier
            .statusBarsPadding()
            .padding(start = 12.dp, top = 10.dp),
        color = Color.Black.copy(alpha = 0.55f),
        shape = CircleShape,
        shadowElevation = 10.dp
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
    }
}

private fun hasLocationPermission(ctx: Context): Boolean {
    val fine = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
    return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
}

private fun dispatchRunServiceAction(context: Context, action: String) {
    val intent = Intent(context, RunTrackingService::class.java).apply {
        this.action = action
    }
    if (action == RunTrackingService.ACTION_START) {
        ContextCompat.startForegroundService(context, intent)
    } else {
        context.startService(intent)
    }
}

private fun formatDistance(distanceMeters: Float): String {
    val km = distanceMeters / 1000f
    return "%.2f km".format(km)
}

private fun formatPace(paceInSecondsPerKm: Float): String {
    if (paceInSecondsPerKm <= 0f) return "--"
    val mins = (paceInSecondsPerKm / 60).toInt()
    val secs = (paceInSecondsPerKm % 60).toInt()
    return "%d:%02d /km".format(mins, secs)
}

private fun formatTime(elapsedSeconds: Int): String {
    val hours = elapsedSeconds / 3600
    val mins = (elapsedSeconds % 3600) / 60
    val secs = elapsedSeconds % 60
    return "%02d:%02d:%02d".format(hours, mins, secs)
}

@Composable
fun ChallengeActiveBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Challenge Run",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
