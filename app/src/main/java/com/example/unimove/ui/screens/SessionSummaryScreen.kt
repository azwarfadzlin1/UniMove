package com.example.unimove.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.unimove.ui.viewmodel.AuthViewModel
import com.example.unimove.ui.viewmodel.RunViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.example.unimove.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSummaryScreen(
    navController: NavController,
    viewModel: RunViewModel,
    authViewModel: AuthViewModel,
    challengeId: String? = null
) {
    val pendingSession by viewModel.pendingSession.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val context = LocalContext.current

    var runName by remember { mutableStateOf("Morning Run") }
    var isShared by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Safety check
    if (pendingSession == null) {
        LaunchedEffect(Unit) {
            navController.navigate("home") {
                popUpTo(0)
            }
        }
        return
    }
    
    val session = pendingSession!!
    val mapStyleOptions = remember {
        MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style)
    }

    val bounds = remember(session.pathSegments) {
        var minLat = 90.0
        var maxLat = -90.0
        var minLng = 180.0
        var maxLng = -180.0
        var hasPoints = false

        session.pathSegments.forEach { segment ->
            segment.forEach { point ->
                val lat = point["lat"] ?: 0.0
                val lng = point["lng"] ?: 0.0
                if (lat < minLat) minLat = lat
                if (lat > maxLat) maxLat = lat
                if (lng < minLng) minLng = lng
                if (lng > maxLng) maxLng = lng
                hasPoints = true
            }
        }
        
        if (hasPoints) {
            LatLngBounds(LatLng(minLat, minLng), LatLng(maxLat, maxLng))
        } else null
    }

    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(bounds) {
        if (bounds != null) {
            // Give map a moment to layout before animating
            kotlinx.coroutines.delay(300)
            try {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            } catch (e: Exception) {
                // fall back to rough center if bounds fails
                val centerLat = (bounds.northeast.latitude + bounds.southwest.latitude) / 2
                val centerLng = (bounds.northeast.longitude + bounds.southwest.longitude) / 2
                cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(centerLat, centerLng), 15f)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Session Summary", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("home") { popUpTo(0) } }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        },
        containerColor = Color(0xFFF3F4F8)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Map Snapshot
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = false,
                        mapStyleOptions = mapStyleOptions
                    ),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        scrollGesturesEnabled = false,
                        zoomGesturesEnabled = false,
                        tiltGesturesEnabled = false,
                        rotationGesturesEnabled = false,
                        compassEnabled = false
                    )
                ) {
                    session.pathSegments.forEach { segment ->
                        val latLngList = segment.mapNotNull {
                            val lat = it["lat"]
                            val lng = it["lng"]
                            if (lat != null && lng != null) LatLng(lat, lng) else null
                        }
                        Polyline(
                            points = latLngList,
                            color = Color(0xFF2F80FF),
                            width = 12f
                        )
                    }
                }
            }

            // Stats Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatItem("Distance", String.format("%.2f km", session.distanceMeters / 1000f))
                    StatItem("Time", formatTime(session.elapsedSeconds))
                    
                    val pace = if (session.smoothedPaceSecPerKm > 0f) {
                        val mins = (session.smoothedPaceSecPerKm / 60).toInt()
                        val secs = (session.smoothedPaceSecPerKm % 60).toInt()
                        "%d:%02d /km".format(mins, secs)
                    } else {
                        val distanceKm = session.distanceMeters / 1000f
                        val rawPace = if (distanceKm > 0) session.elapsedSeconds / distanceKm else 0f
                        val mins = (rawPace / 60).toInt()
                        val secs = (rawPace % 60).toInt()
                        "%d:%02d /km".format(mins, secs)
                    }
                    StatItem("Avg Pace", pace)
                }
            }

            // Input Fields
            OutlinedTextField(
                value = runName,
                onValueChange = { runName = it },
                label = { Text("Run Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Share to Community Feed", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Switch(checked = isShared, onCheckedChange = { isShared = it })
            }
            
            if (errorMessage != null) {
                Text(text = errorMessage!!, color = Color.Red, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val userName = authState.currentUserName ?: "Anonymous Runner"
                    isSaving = true
                    viewModel.commitSession(
                        runName = runName,
                        isShared = isShared,
                        userName = userName,
                        challengeId = challengeId,
                        onSuccess = {
                            isSaving = false
                            navController.navigate("home") {
                                popUpTo(0)
                            }
                        },
                        onError = { err ->
                            errorMessage = err
                            isSaving = false
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Save Session", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color.Gray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

private fun formatTime(seconds: Int): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        String.format("%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}
