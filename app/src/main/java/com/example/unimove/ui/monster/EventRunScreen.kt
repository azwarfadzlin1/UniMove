package com.example.unimove.ui.monster

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.unimove.R
import com.example.unimove.service.RunTrackingService
import com.google.android.gms.maps.model.LatLng
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.common.Cancelable
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.Style
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineElevationReference
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.scalebar.scalebar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private data class EventMonsterSpawn(
    val id: String,
    val name: String = "Bee Monster",
    val position: LatLng,
    val modelAsset: String = "models/animal-bee.glb",
    val markerColor: Color = Color(0xFF2563EB),
    val visibleRadiusMeters: Float = 90f,
    val encounterRadiusMeters: Float = 40f
)

private const val ROUTE_SOURCE_ID = "monster-event-route-source"
private const val ROUTE_LAYER_ID = "monster-event-route-layer"

private val UKM_MONSTER_SPAWNS = listOf(
    EventMonsterSpawn(
        id = "ukm-pendeta-zaaba-bee",
        name = "Bee Monster",
        position = LatLng(2.919770, 101.773583),
        modelAsset = "models/animal-bee.glb",
        markerColor = Color(0xFFE11D48)
    ),
    EventMonsterSpawn(
        id = "ukm-sel-fuel-bunny",
        name = "Bunny Monster",
        position = LatLng(2.922100, 101.773704),
        modelAsset = "models/animal-bunny.glb",
        markerColor = Color(0xFFF97316)
    ),
    EventMonsterSpawn(
        id = "ukm-kbh-cat",
        name = "Cat Monster",
        position = LatLng(2.927198, 101.777917),
        modelAsset = "models/animal-cat.glb",
        markerColor = Color(0xFFEAB308)
    ),
    EventMonsterSpawn(
        id = "ukm-canselori-caterpillar",
        name = "Caterpillar Monster",
        position = LatLng(2.928833, 101.779097),
        modelAsset = "models/animal-caterpillar.glb",
        markerColor = Color(0xFF22C55E)
    ),
    EventMonsterSpawn(
        id = "ukm-dectar-chick",
        name = "Chick Monster",
        position = LatLng(2.930138, 101.778277),
        modelAsset = "models/animal-chick.glb",
        markerColor = Color(0xFF14B8A6)
    ),
    EventMonsterSpawn(
        id = "ukm-kkm-cow",
        name = "Cow Monster",
        position = LatLng(2.927137, 101.788936),
        modelAsset = "models/animal-cow.glb",
        markerColor = Color(0xFF0EA5E9)
    ),
    EventMonsterSpawn(
        id = "ukm-arena-crab",
        name = "Crab Monster",
        position = LatLng(2.936300, 101.783794),
        modelAsset = "models/animal-crab.glb",
        markerColor = Color(0xFF6366F1)
    ),
    EventMonsterSpawn(
        id = "ukm-pusanika-deer",
        name = "Deer Monster",
        position = LatLng(2.925991, 101.780755),
        modelAsset = "models/animal-deer.glb",
        markerColor = Color(0xFFA855F7)
    )
)

@Composable
fun EventRunScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var boundService by remember { mutableStateOf<RunTrackingService?>(null) }
    var isServiceBound by remember { mutableStateOf(false) }
    val serviceConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                boundService = (service as? RunTrackingService.RunBinder)?.getService()
                isServiceBound = true
            }

            override fun onServiceDisconnected(name: ComponentName?) {
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
        boundService?.isRunning ?: kotlinx.coroutines.flow.MutableStateFlow(false)
    }.collectAsStateWithLifecycle()
    val isPaused by remember(boundService) {
        boundService?.isPaused ?: kotlinx.coroutines.flow.MutableStateFlow(false)
    }.collectAsStateWithLifecycle()
    val distanceMeters by remember(boundService) {
        boundService?.distanceMeters ?: kotlinx.coroutines.flow.MutableStateFlow(0f)
    }.collectAsStateWithLifecycle()
    val elapsedSeconds by remember(boundService) {
        boundService?.elapsedSeconds ?: kotlinx.coroutines.flow.MutableStateFlow(0)
    }.collectAsStateWithLifecycle()
    val paceSecPerKm by remember(boundService) {
        boundService?.displayedPaceSecPerKm ?: kotlinx.coroutines.flow.MutableStateFlow(0f)
    }.collectAsStateWithLifecycle()
    val pathSegments by remember(boundService) {
        boundService?.pathSegments ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())
    }.collectAsStateWithLifecycle()
    val currentLocation by remember(boundService) {
        boundService?.currentLocation ?: kotlinx.coroutines.flow.MutableStateFlow(null)
    }.collectAsStateWithLifecycle()

    val currentLatLng = currentLocation?.let { LatLng(it.latitude, it.longitude) }
    // The Mapbox location plugin uses a separate, unfiltered GPS stream. During a
    // hunt, keep the visible marker, camera and encounter distance on the same
    // filtered coordinate that produced the polyline instead.
    val trackedLatLng = pathSegments.lastOrNull { it.isNotEmpty() }?.lastOrNull()
    val mapUserLatLng = if (isRunning || pathSegments.any { it.isNotEmpty() }) {
        trackedLatLng ?: currentLatLng
    } else {
        currentLatLng
    }
    var permissionGranted by rememberSaveable { mutableStateOf(false) }
    var isFollowingUser by rememberSaveable { mutableStateOf(true) }
    var hasInitiallyCentered by rememberSaveable { mutableStateOf(false) }
    var eventBearing by rememberSaveable { mutableFloatStateOf(0f) }
    var caughtMonsterIds by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var selectedMonsterId by rememberSaveable { mutableStateOf<String?>(null) }
    var monsterEncountered by rememberSaveable { mutableStateOf(false) }
    var showEncounterDialog by rememberSaveable { mutableStateOf(false) }
    var isSavingCatch by remember { mutableStateOf(false) }
    var controlsHeightDp by remember { mutableStateOf(0.dp) }
    var isHudExpanded by rememberSaveable { mutableStateOf(false) }
    var showSaveButton by rememberSaveable { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showEndDialog by rememberSaveable { mutableStateOf(false) }

    val navBarsBottomDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val mapBottomPadding = controlsHeightDp + navBarsBottomDp + 12.dp
    val monsterDistances = remember(mapUserLatLng) {
        val user = mapUserLatLng
        if (user == null) {
            emptyMap()
        } else {
            UKM_MONSTER_SPAWNS.associate { monster ->
                monster.id to distanceBetweenMeters(user, monster.position)
            }
        }
    }
    val nearbyMonsters = remember(monsterDistances, caughtMonsterIds) {
        UKM_MONSTER_SPAWNS.filter { monster ->
            monster.id !in caughtMonsterIds &&
                (monsterDistances[monster.id] ?: Float.MAX_VALUE) <= monster.encounterRadiusMeters
        }
    }
    val selectedMonster = remember(selectedMonsterId, caughtMonsterIds) {
        UKM_MONSTER_SPAWNS.firstOrNull { monster ->
            monster.id == selectedMonsterId && monster.id !in caughtMonsterIds
        }
    }
    val selectedMonsterDistanceMeters = selectedMonster?.let { monsterDistances[it.id] }
    val hasCaughtMonster = caughtMonsterIds.isNotEmpty()
    val monsterCaught = caughtMonsterIds.size >= UKM_MONSTER_SPAWNS.size

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionGranted = result.values.any { it }
        if (permissionGranted) {
            boundService?.refreshLocationUpdates()
        } else {
            errorMessage = "Location permission is required for Monster Event."
        }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    LaunchedEffect(Unit) {
        permissionGranted = hasLocationPermission(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(currentLocation, pathSegments) {
        val location = currentLocation ?: return@LaunchedEffect
        eventBearing = resolveEventBearing(location, pathSegments, eventBearing)
    }

    LaunchedEffect(nearbyMonsters, isRunning, monsterCaught) {
        monsterEncountered = isRunning && !monsterCaught && nearbyMonsters.isNotEmpty()
    }

    fun requestEndEvent() {
        if (isRunning && !isSaving) {
            showEndDialog = true
        } else {
            onBack()
        }
    }

    BackHandler(enabled = isRunning || showSaveButton) {
        requestEndEvent()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        EventMapLayer(
            pathSegments = pathSegments,
            currentLatLng = mapUserLatLng,
            eventBearing = eventBearing,
            monsterSpawns = UKM_MONSTER_SPAWNS,
            monsterDistances = monsterDistances,
            caughtMonsterIds = caughtMonsterIds,
            bottomContentPadding = mapBottomPadding,
            isFollowingUser = isFollowingUser,
            hasInitiallyCentered = hasInitiallyCentered,
            onReCenter = {
                isFollowingUser = true
                hasInitiallyCentered = false
            },
            onCameraCentered = { hasInitiallyCentered = true },
            onUserMovedMap = {
                isFollowingUser = false
            },
            onMonsterClick = { monster ->
                val distanceToMonster = monsterDistances[monster.id]
                if (monster.id !in caughtMonsterIds &&
                    distanceToMonster != null &&
                    distanceToMonster <= monster.encounterRadiusMeters
                ) {
                    selectedMonsterId = monster.id
                    showEncounterDialog = true
                }
            }
        )

        EventTopBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            nearbyMonsterCount = nearbyMonsters.size,
            caughtMonsterCount = caughtMonsterIds.size,
            totalMonsterCount = UKM_MONSTER_SPAWNS.size,
            allMonstersCaught = monsterCaught,
            onBack = ::requestEndEvent
        )

        EventControls(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .onSizeChanged { size ->
                    val newHeight = with(density) { size.height.toDp() }
                    if (controlsHeightDp != newHeight) {
                        controlsHeightDp = newHeight
                    }
                },
            isRunning = isRunning,
            isPaused = isPaused,
            showSaveButton = showSaveButton,
            isSaving = isSaving,
            distanceMeters = distanceMeters,
            elapsedSeconds = elapsedSeconds,
            monsterEncountered = monsterEncountered,
            monsterCaught = monsterCaught,
            isExpanded = isHudExpanded,
            errorMessage = errorMessage,
            onToggleExpanded = { isHudExpanded = !isHudExpanded },
            onStartOrResume = {
                if (!hasLocationPermission(context)) {
                    permissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    )
                    return@EventControls
                }

                errorMessage = null
                showSaveButton = false
                isFollowingUser = true
                hasInitiallyCentered = false

                val action = if (isRunning && isPaused) {
                    RunTrackingService.ACTION_RESUME
                } else {
                    RunTrackingService.ACTION_START
                }
                dispatchTrackingAction(context, action)
            },
            onPause = {
                if (isRunning && !isPaused && !isSaving) {
                    dispatchTrackingAction(context, RunTrackingService.ACTION_PAUSE)
                }
            },
            onEncounterClick = {
                val nearbyMonster = nearbyMonsters.firstOrNull()
                if (nearbyMonster != null && !monsterCaught) {
                    selectedMonsterId = nearbyMonster.id
                    showEncounterDialog = true
                }
            },
            onStop = {
                    if (isRunning && !isSaving) {
                        dispatchTrackingAction(context, RunTrackingService.ACTION_STOP)
                        showSaveButton = (hasCaughtMonster || monsterEncountered) && (distanceMeters > 10f || elapsedSeconds >= 10)
                    }
                },
            onSave = {
                if (isSaving) return@EventControls
                isSaving = true
                errorMessage = null
                scope.launch {
                    runCatching {
                        saveMonsterEventSession(
                            distanceMeters = distanceMeters,
                            elapsedSeconds = elapsedSeconds,
                            paceSecPerKm = paceSecPerKm,
                            pathSegments = pathSegments,
                            monsterSpawn = selectedMonster,
                            monsterEncountered = monsterEncountered,
                            monsterCaught = hasCaughtMonster
                        )
                    }.onSuccess {
                        isSaving = false
                        showSaveButton = false
                        onBack()
                    }.onFailure { error ->
                        isSaving = false
                        errorMessage = error.message ?: "Failed to save Monster Event."
                    }
                }
            }
        )
    }

    if (showEncounterDialog && selectedMonster != null) {
        val spawn = selectedMonster
        MonsterEncounterDialog(
            creatureName = spawn.name,
            modelAsset = spawn.modelAsset,
            distanceMeters = selectedMonsterDistanceMeters,
            isSaving = isSavingCatch,
            catchEnabled = selectedMonsterDistanceMeters?.let { it <= spawn.encounterRadiusMeters } == true,
            onCatch = {
                val caughtSpawn = spawn
                if (isSavingCatch) return@MonsterEncounterDialog
                isSavingCatch = true
                scope.launch {
                    runCatching {
                        saveMonsterEventCatch(
                            monsterSpawn = caughtSpawn,
                            distanceAtCatchMeters = selectedMonsterDistanceMeters
                        )
                    }.onSuccess {
                        caughtMonsterIds = (caughtMonsterIds + caughtSpawn.id).distinct()
                        selectedMonsterId = null
                        monsterEncountered = false
                        showEncounterDialog = false
                        isSavingCatch = false
                    }.onFailure { error ->
                        isSavingCatch = false
                        errorMessage = error.message ?: "Failed to catch monster."
                    }
                }
            },
            onDismiss = {
                selectedMonsterId = null
                showEncounterDialog = false
            }
        )
    }

    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            title = { Text("End Monster Event?") },
            text = {
                Text(
                    if (monsterCaught) {
                        "Every preset creature has been caught. You can save this event session afterward."
                    } else if (hasCaughtMonster) {
                        "Your caught creature is stored. You can save this event session afterward."
                    } else if (monsterEncountered) {
                        "Your current event tracking will stop. You can save it afterward."
                    } else {
                        "Your current event tracking will stop. Find the monster first to save this as an event session."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        dispatchTrackingAction(context, RunTrackingService.ACTION_STOP)
                        showSaveButton = (hasCaughtMonster || monsterEncountered) && (distanceMeters > 10f || elapsedSeconds >= 10)
                        showEndDialog = false
                    }
                ) {
                    Text("End event")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDialog = false }) {
                    Text("Keep going")
                }
            }
        )
    }
}

@Composable
@OptIn(MapboxExperimental::class)
private fun EventMapLayer(
    pathSegments: List<List<LatLng>>,
    currentLatLng: LatLng?,
    eventBearing: Float,
    monsterSpawns: List<EventMonsterSpawn>,
    monsterDistances: Map<String, Float>,
    caughtMonsterIds: List<String>,
    bottomContentPadding: androidx.compose.ui.unit.Dp,
    isFollowingUser: Boolean,
    hasInitiallyCentered: Boolean,
    onReCenter: () -> Unit,
    onCameraCentered: () -> Unit,
    onUserMovedMap: () -> Unit,
    onMonsterClick: (EventMonsterSpawn) -> Unit
) {
    val mapboxToken = stringResource(R.string.mapbox_access_token)
    val hasMapboxToken = remember(mapboxToken) {
        mapboxToken.isNotBlank()
    }
    var monsterScreenPositions by remember { mutableStateOf<Map<String, IntOffset>>(emptyMap()) }
    var userScreenPosition by remember { mutableStateOf<IntOffset?>(null) }
    val latestMonsterSpawns by rememberUpdatedState(monsterSpawns)
    val latestOnUserMovedMap by rememberUpdatedState(onUserMovedMap)
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    var cameraSubscription by remember { mutableStateOf<Cancelable?>(null) }
    val moveListener = remember {
        object : OnMoveListener {
            override fun onMoveBegin(detector: MoveGestureDetector) {
                latestOnUserMovedMap()
            }

            override fun onMove(detector: MoveGestureDetector): Boolean = false

            override fun onMoveEnd(detector: MoveGestureDetector) = Unit
        }
    }

    fun recalculateOverlayScreenPositions() {
        val mapView = mapViewRef.value ?: return
        val nextPositions = latestMonsterSpawns.associate { monster ->
            val point = mapView.mapboxMap.pixelForCoordinate(monster.position.toPoint())
            monster.id to IntOffset(point.x.roundToInt(), point.y.roundToInt())
        }
        if (monsterScreenPositions != nextPositions) {
            monsterScreenPositions = nextPositions
        }
        val nextUserPosition = currentLatLng?.let { coordinate ->
            val point = mapView.mapboxMap.pixelForCoordinate(coordinate.toPoint())
            IntOffset(point.x.roundToInt(), point.y.roundToInt())
        }
        if (userScreenPosition != nextUserPosition) {
            userScreenPosition = nextUserPosition
        }
    }

    LaunchedEffect(monsterSpawns, currentLatLng) {
        recalculateOverlayScreenPositions()
    }

    DisposableEffect(Unit) {
        onDispose {
            mapViewRef.value?.gestures?.removeOnMoveListener(moveListener)
            cameraSubscription?.cancel()
            cameraSubscription = null
            mapViewRef.value = null
        }
    }

    if (!hasMapboxToken) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF182131)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Add MAPBOX_ACCESS_TOKEN in local.properties to load the Monster Event map.",
                modifier = Modifier.padding(24.dp),
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                MapView(viewContext).apply {
                    mapViewRef.value = this
                    mapboxMap.loadStyle(buildMonsterMapStyle(pathSegments))
                    configureMonsterMapOrnaments()
                    gestures.addOnMoveListener(moveListener)
                    cameraSubscription?.cancel()
                    cameraSubscription = mapboxMap.subscribeCameraChanged {
                        recalculateOverlayScreenPositions()
                    }
                }
            },
            update = { mapView ->
                mapViewRef.value = mapView
                mapView.configureMonsterMapOrnaments()

                mapView.mapboxMap.getStyle { style ->
                    style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)
                        ?.featureCollection(buildRouteFeatureCollection(pathSegments))
                }

                if (isFollowingUser && currentLatLng != null) {
                    val cameraBearing = eventBearing
                    val cameraTarget = offsetLatLng(
                        origin = currentLatLng,
                        distanceMeters = 18.0,
                        bearingDegrees = cameraBearing
                    )
                    mapView.mapboxMap.setCamera(
                        cameraOptions {
                            center(cameraTarget.toPoint())
                            zoom(18.8)
                            pitch(65.0)
                            bearing(cameraBearing.toDouble())
                        }
                    )
                    if (!hasInitiallyCentered) {
                        onCameraCentered()
                    }
                }

                recalculateOverlayScreenPositions()
            }
        )

        TrackedUserIndicator(screenPosition = userScreenPosition)

        monsterSpawns.forEach { monster ->
            val isCaught = monster.id in caughtMonsterIds
            val distance = monsterDistances[monster.id]
            PinnedQuestionMonsterIndicator(
                screenPosition = monsterScreenPositions[monster.id],
                isVisible = true,
                isInRange = !isCaught && distance != null && distance <= monster.encounterRadiusMeters,
                isCaught = isCaught,
                markerColor = monster.markerColor,
                onClick = { onMonsterClick(monster) }
            )
        }

        EventCompassControl(
            bearing = eventBearing,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    bottom = bottomContentPadding + if (!isFollowingUser && currentLatLng != null) 78.dp else 16.dp,
                    end = 16.dp
                ),
            onClick = onReCenter
        )

        if (!isFollowingUser && currentLatLng != null) {
            EventMapActionButton(
                icon = Icons.Default.MyLocation,
                contentDescription = "Re-center",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = bottomContentPadding + 16.dp, end = 16.dp),
                onClick = onReCenter
            )
        }
    }
}

@Composable
private fun EventTopBar(
    modifier: Modifier = Modifier,
    nearbyMonsterCount: Int,
    caughtMonsterCount: Int,
    totalMonsterCount: Int,
    allMonstersCaught: Boolean,
    onBack: () -> Unit
) {
    val titleText = when {
        allMonstersCaught -> "All monsters caught"
        nearbyMonsterCount > 0 -> "Monster nearby"
        else -> "Monster Event"
    }
    val statusText = when {
        allMonstersCaught -> "All $totalMonsterCount event creatures are stored"
        nearbyMonsterCount > 0 -> "$nearbyMonsterCount marker in range. Tap a colored marker."
        caughtMonsterCount > 0 -> "$caughtMonsterCount caught, ${totalMonsterCount - caughtMonsterCount} still on the map"
        else -> "$totalMonsterCount colored monster locations shown on the map"
    }
    val badgeColor = when {
        allMonstersCaught -> Color(0xFF15803D)
        nearbyMonsterCount > 0 -> Color(0xFFE87920)
        else -> Color(0xFF2563EB)
    }
    val inkColor = Color(0xFF172033)

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFF9F1DF),
            shape = RoundedCornerShape(22.dp),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF172033), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFFF9F1DF)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titleText,
                        color = inkColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        maxLines = 1
                    )
                    Text(
                        text = statusText,
                        color = inkColor.copy(alpha = 0.70f),
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    color = badgeColor.copy(alpha = 0.18f),
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = "LIVE",
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        color = badgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun EventControls(
    modifier: Modifier = Modifier,
    isRunning: Boolean,
    isPaused: Boolean,
    showSaveButton: Boolean,
    isSaving: Boolean,
    distanceMeters: Float,
    elapsedSeconds: Int,
    monsterEncountered: Boolean,
    monsterCaught: Boolean,
    isExpanded: Boolean,
    errorMessage: String?,
    onToggleExpanded: () -> Unit,
    onStartOrResume: () -> Unit,
    onPause: () -> Unit,
    onEncounterClick: () -> Unit,
    onStop: () -> Unit,
    onSave: () -> Unit
) {
    val actionText = when {
        showSaveButton -> "Save Hunt"
        !isRunning -> "Start Hunt"
        isPaused -> "Resume"
        monsterCaught -> "Caught"
        monsterEncountered -> "Catch Nearby"
        else -> "Hunting"
    }
    val actionColor = when {
        showSaveButton -> Color(0xFF22C55E)
        monsterCaught -> Color(0xFF34D399)
        monsterEncountered -> Color(0xFFFFD166)
        else -> Color(0xFFFF4D4D)
    }
    val actionIcon = when {
        showSaveButton -> Icons.Default.Save
        !isRunning || isPaused -> Icons.Default.PlayArrow
        monsterCaught -> Icons.Default.Save
        monsterEncountered -> Icons.Default.Save
        else -> Icons.Default.Pause
    }

    if (!isExpanded) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded),
                color = Color(0xFFF9F1DF),
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                showSaveButton -> "Hunt ready to save"
                                monsterEncountered && !monsterCaught -> "Monster within range"
                                !isRunning -> "Tap to open hunt controls"
                                isPaused -> "Hunt paused"
                                else -> "Hunt HUD collapsed"
                            },
                            color = Color(0xFF172033),
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${formatEventTime(elapsedSeconds)}  |  ${formatEventDistance(distanceMeters)}",
                            color = Color(0xFF172033).copy(alpha = 0.66f),
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Expand hunt controls",
                        tint = Color(0xFFF9F1DF),
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFF172033), CircleShape)
                            .padding(5.dp)
                    )
                }
            }
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFF9F1DF),
            shape = RoundedCornerShape(26.dp),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EventStat(modifier = Modifier.weight(1f), label = "Time", value = formatEventTime(elapsedSeconds))
                    EventStat(modifier = Modifier.weight(1f), label = "Distance", value = formatEventDistance(distanceMeters))
                    IconButton(
                        onClick = onToggleExpanded,
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF172033), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Collapse hunt controls",
                            tint = Color(0xFFF9F1DF)
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = when {
                        monsterCaught -> Color(0xFFDDFBEA)
                        monsterEncountered -> Color(0xFFFFE5C2)
                        else -> Color(0xFFE8EEFF)
                    },
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (monsterEncountered || monsterCaught) Color(0xFFE87920).copy(alpha = 0.35f) else Color(0xFF2563EB).copy(alpha = 0.24f)
                    )
                ) {
                    Text(
                        text = when {
                            monsterCaught -> "Creature secured. End and save your hunt."
                            monsterEncountered -> "Monster nearby. Open the encounter popup to catch it."
                            else -> "Follow the angled map and close in on the 3D target."
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        color = Color(0xFF172033),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HudMiniButton(
                        icon = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        label = if (!isRunning) "Ready" else if (isPaused) "Resume" else "Pause",
                        enabled = isRunning && !isSaving,
                        onClick = { if (isPaused) onStartOrResume() else onPause() }
                    )
                    HuntOrbButton(
                        text = actionText,
                        icon = actionIcon,
                        color = actionColor,
                        enabled = !isSaving && (!monsterCaught || showSaveButton),
                        isLoading = isSaving,
                        compact = false,
                        onClick = {
                            when {
                                showSaveButton -> onSave()
                                monsterEncountered && !monsterCaught -> onEncounterClick()
                                !isRunning || isPaused -> onStartOrResume()
                                else -> onPause()
                            }
                        }
                    )
                    HudMiniButton(
                        icon = Icons.Default.Close,
                        label = "End",
                        enabled = isRunning && !isSaving,
                        danger = true,
                        onClick = onStop
                    )
                }

                if (showSaveButton) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onSave,
                        enabled = !isSaving,
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Save Event Session")
                        }
                    }
                }

                errorMessage?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun EventStat(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Surface(
        modifier = modifier,
        color = Color(0xFFFFFFFF).copy(alpha = 0.72f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF172033).copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = Color(0xFF5B6475),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                color = Color(0xFF172033),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HuntOrbButton(
    text: String,
    icon: ImageVector,
    color: Color,
    enabled: Boolean,
    isLoading: Boolean,
    compact: Boolean,
    onClick: () -> Unit
) {
    val size = if (compact) 86.dp else 98.dp
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(size)
            .border(5.dp, Color.White.copy(alpha = 0.92f), CircleShape),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            disabledContainerColor = color.copy(alpha = 0.55f)
        ),
        contentPadding = PaddingValues(8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isLoading) {
                CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White, modifier = Modifier.size(26.dp))
            } else {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = if (compact) 11.sp else 12.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun HudMiniButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(width = 76.dp, height = 52.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (danger) Color(0xFFFFE1D8) else Color(0xFFE8EEFF),
            disabledContainerColor = Color(0xFFE5DED1)
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (danger) Color(0xFFB42318) else Color(0xFF172033),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                color = if (danger) Color(0xFFB42318) else Color(0xFF172033),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@OptIn(MapboxExperimental::class)
private fun buildMonsterMapStyle(
    pathSegments: List<List<LatLng>>
) = style(Style.STANDARD) {
    +geoJsonSource(ROUTE_SOURCE_ID) {
        featureCollection(buildRouteFeatureCollection(pathSegments))
    }
    +lineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID) {
        slot("top")
        lineElevationReference(LineElevationReference.GROUND)
        lineElevationGroundScale(1.0)
        lineBorderColor("#FFFFFF")
        lineBorderWidth(2.0)
        lineColor("#38BDF8")
        lineWidth(8.0)
        lineOpacity(0.95)
        lineEmissiveStrength(1.0)
        lineSortKey(1_000.0)
    }
}

private fun buildRouteFeatureCollection(pathSegments: List<List<LatLng>>): FeatureCollection {
    val features = pathSegments.mapNotNull { segment ->
        val points = segment.map { it.toPoint() }
        if (points.size > 1) Feature.fromGeometry(LineString.fromLngLats(points)) else null
    }
    return FeatureCollection.fromFeatures(features)
}

private fun LatLng.toPoint(): Point = Point.fromLngLat(longitude, latitude)

private fun MapView.configureMonsterMapOrnaments() {
    location.updateSettings {
        enabled = false
        pulsingEnabled = false
    }
    scalebar.enabled = false
    compass.enabled = false
}

@Composable
private fun EventMapActionButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(50.dp),
        color = Color(0xE6172033),
        shape = CircleShape,
        shadowElevation = 8.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color(0xFFF9F1DF),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun EventCompassControl(
    bearing: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(50.dp),
        color = Color(0xEEF9F1DF),
        shape = CircleShape,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33172033))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.NearMe,
                contentDescription = "Re-center map compass",
                tint = Color(0xFF172033),
                modifier = Modifier
                    .size(24.dp)
                    .rotate(-bearing)
            )
            Text(
                text = "N",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 5.dp),
                color = Color(0xFFE11D48),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun TrackedUserIndicator(screenPosition: IntOffset?) {
    val position = screenPosition ?: return
    val density = LocalDensity.current
    val markerSize = 38.dp
    val markerSizePx = with(density) { markerSize.roundToPx() }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = position.x - markerSizePx / 2,
                    y = position.y - markerSizePx / 2
                )
            }
            .size(markerSize)
            .background(Color(0x332563EB), CircleShape)
            .padding(8.dp)
            .background(Color.White, CircleShape)
            .padding(3.dp)
            .background(Color(0xFF2F80FF), CircleShape)
    )
}

@Composable
private fun PinnedQuestionMonsterIndicator(
    screenPosition: IntOffset?,
    isVisible: Boolean,
    isInRange: Boolean,
    isCaught: Boolean,
    markerColor: Color,
    onClick: () -> Unit
) {
    val position = screenPosition ?: return
    if (!isVisible) return

    val density = LocalDensity.current
    val markerSize = 72.dp
    val markerSizePx = with(density) { markerSize.roundToPx() }
    val resolvedMarkerColor = when {
        isCaught -> Color(0xFF15803D)
        isInRange -> Color(0xFFE87920)
        else -> markerColor
    }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = position.x - markerSizePx / 2,
                    y = position.y - markerSizePx
                )
            }
            .size(markerSize)
            .clickable(enabled = isInRange && !isCaught, onClick = onClick),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .background(resolvedMarkerColor.copy(alpha = 0.18f), CircleShape)
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = resolvedMarkerColor,
                shape = CircleShape,
                shadowElevation = 7.dp,
                border = androidx.compose.foundation.BorderStroke(3.dp, Color(0xFFF9F1DF))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isCaught) "C" else "?",
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    val fine = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
}

private fun dispatchTrackingAction(context: Context, action: String) {
    val intent = Intent(context, RunTrackingService::class.java).apply {
        this.action = action
    }
    if (action == RunTrackingService.ACTION_START) {
        ContextCompat.startForegroundService(context, intent)
    } else {
        context.startService(intent)
    }
}

private fun resolveEventBearing(
    location: Location,
    pathSegments: List<List<LatLng>>,
    fallback: Float
): Float {
    if (location.hasBearing() && location.speed >= 0.5f) {
        return location.bearing
    }

    val lastSegment = pathSegments.lastOrNull().orEmpty()
    if (lastSegment.size >= 2) {
        return bearingBetween(lastSegment[lastSegment.lastIndex - 1], lastSegment.last())
    }

    return fallback
}

private fun offsetLatLng(
    origin: LatLng,
    distanceMeters: Double,
    bearingDegrees: Float
): LatLng {
    val radiusMeters = 6_371_000.0
    val angularDistance = distanceMeters / radiusMeters
    val bearing = Math.toRadians(bearingDegrees.toDouble())
    val lat1 = Math.toRadians(origin.latitude)
    val lng1 = Math.toRadians(origin.longitude)

    val lat2 = asin(
        sin(lat1) * cos(angularDistance) +
            cos(lat1) * sin(angularDistance) * cos(bearing)
    )
    val lng2 = lng1 + atan2(
        sin(bearing) * sin(angularDistance) * cos(lat1),
        cos(angularDistance) - sin(lat1) * sin(lat2)
    )

    return LatLng(Math.toDegrees(lat2), Math.toDegrees(lng2))
}

private fun bearingBetween(from: LatLng, to: LatLng): Float {
    val lat1 = Math.toRadians(from.latitude)
    val lat2 = Math.toRadians(to.latitude)
    val dLng = Math.toRadians(to.longitude - from.longitude)
    val y = sin(dLng) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
    return ((Math.toDegrees(atan2(y, x)) + 360.0) % 360.0).toFloat()
}

private fun distanceBetweenMeters(from: LatLng, to: LatLng): Float {
    val result = FloatArray(1)
    Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, result)
    return result[0]
}

private suspend fun saveMonsterEventSession(
    distanceMeters: Float,
    elapsedSeconds: Int,
    paceSecPerKm: Float,
    pathSegments: List<List<LatLng>>,
    monsterSpawn: EventMonsterSpawn?,
    monsterEncountered: Boolean,
    monsterCaught: Boolean
) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid ?: error("User not logged in")
    val distanceKm = distanceMeters / 1000.0
    val avgPace = formatAveragePace(distanceKm, elapsedSeconds, paceSecPerKm)
    val routePoints = pathSegments.map { segment ->
        mapOf(
            "points" to segment.map {
                mapOf("lat" to it.latitude, "lng" to it.longitude)
            }
        )
    }
    val now = Timestamp(Date())

    val sessionData = hashMapOf(
        "userId" to uid,
        "userDisplayName" to (auth.currentUser?.displayName ?: ""),
        "runName" to "Monster Event Run",
        "isShared" to false,
        "distanceKm" to distanceKm,
        "durationSeconds" to elapsedSeconds,
        "avgPace" to avgPace,
        "timestamp" to now,
        "routePoints" to routePoints,
        "sessionMode" to "monster_event",
        "eventType" to "monster",
        "monsterEncountered" to monsterEncountered,
        "monsterCaught" to monsterCaught,
        "monsterModelAsset" to (monsterSpawn?.modelAsset ?: "models/animal-bee.glb")
    )

    monsterSpawn?.let {
        sessionData["monsterSpawn"] = mapOf(
            "id" to it.id,
            "name" to it.name,
            "lat" to it.position.latitude,
            "lng" to it.position.longitude,
            "visibleRadiusMeters" to it.visibleRadiusMeters,
            "encounterRadiusMeters" to it.encounterRadiusMeters
        )
    }

    firestore.collection("users").document(uid)
        .collection("sessions")
        .add(sessionData)
        .await()
}

private suspend fun saveMonsterEventCatch(
    monsterSpawn: EventMonsterSpawn,
    distanceAtCatchMeters: Float?
) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val uid = auth.currentUser?.uid ?: error("User not logged in")
    val now = Timestamp(Date())

    val catchData = hashMapOf(
        "userId" to uid,
        "userDisplayName" to (auth.currentUser?.displayName ?: ""),
        "creatureId" to monsterSpawn.id,
        "creatureName" to monsterSpawn.name,
        "modelAsset" to monsterSpawn.modelAsset,
        "caughtAt" to now,
        "isPublic" to true,
        "source" to "monster_event",
        "caughtLocation" to mapOf(
            "lat" to monsterSpawn.position.latitude,
            "lng" to monsterSpawn.position.longitude
        )
    )
    distanceAtCatchMeters?.let {
        catchData["distanceAtCatchMeters"] = it
    }

    val nestedCatch = firestore.collection("users").document(uid)
        .collection("monster_collection")
        .document(monsterSpawn.id)

    runCatching {
        nestedCatch.set(catchData).await()
    }.recoverCatching { error ->
        // Older deployments stored captures in the top-level collection. Keep
        // catching functional while Firestore rules and existing data migrate.
        firestore.collection("monster_event_catches")
            .document("${uid}_${monsterSpawn.id}")
            .set(catchData)
            .await()
    }.getOrThrow()
}

private fun formatAveragePace(distanceKm: Double, elapsedSeconds: Int, paceSecPerKm: Float): String {
    val paceValue = if (paceSecPerKm > 0f) {
        paceSecPerKm.toDouble()
    } else if (distanceKm > 0.0) {
        elapsedSeconds / distanceKm
    } else {
        0.0
    }

    val mins = (paceValue / 60).toInt()
    val secs = (paceValue % 60).toInt()
    return "%d:%02d /km".format(mins, secs)
}

private fun formatEventDistance(distanceMeters: Float): String {
    return "%.2f km".format(distanceMeters / 1000f)
}

private fun formatEventTime(elapsedSeconds: Int): String {
    val hours = elapsedSeconds / 3600
    val mins = (elapsedSeconds % 3600) / 60
    val secs = elapsedSeconds % 60
    return "%02d:%02d:%02d".format(hours, mins, secs)
}
