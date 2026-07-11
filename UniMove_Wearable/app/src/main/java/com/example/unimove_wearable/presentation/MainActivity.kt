package com.example.unimove_wearable.presentation

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.ambient.AmbientLifecycleObserver
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.example.unimove_wearable.presentation.theme.UniMove_WearableTheme
import com.example.unimove_wearable.tracking.FitnessTracker
import com.example.unimove_wearable.tracking.RunSummary
import com.example.unimove_wearable.tracking.SyncStatus
import com.example.unimove_wearable.tracking.TrackingForegroundService
import com.example.unimove_wearable.tracking.TrackingPhase
import com.example.unimove_wearable.tracking.TrackingUiState
import kotlinx.coroutines.delay
import java.util.Locale

private val RunMint = Color(0xFF7AF0B0)
private val RunMintDim = Color(0xFF153426)
private val RunCyan = Color(0xFF66D9EF)
private val RunAmber = Color(0xFFFFD166)
private val RunRed = Color(0xFFFF5F57)
private val RunInk = Color(0xFFF8FAFC)
private val RunMuted = Color(0xFF9AA6B2)
private val RunSurface = Color(0xFF10161D)
private val RunSurfaceSoft = Color(0xFF17212B)
private val RunBorder = Color(0xFF263241)

private data class WatchUiSpec(
    val horizontalPadding: Dp,
    val topSpacer: Dp,
    val bottomSpacer: Dp,
    val listSpacing: Dp,
    val panelHorizontalPadding: Dp,
    val panelVerticalPadding: Dp,
    val panelRadius: Dp,
    val tileGap: Dp,
    val tileHorizontalPadding: Dp,
    val tileVerticalPadding: Dp,
    val tileRadius: Dp,
    val tileMinHeight: Dp,
    val pillHorizontalPadding: Dp,
    val pillVerticalPadding: Dp,
    val ambientHorizontalPadding: Dp,
    val smallIcon: Dp,
    val titleIcon: Dp,
    val titleText: TextUnit,
    val subtitleText: TextUnit,
    val bodyText: TextUnit,
    val bodyLineHeight: TextUnit,
    val elapsedText: TextUnit,
    val elapsedLineHeight: TextUnit,
    val heroText: TextUnit,
    val heroTextCompact: TextUnit,
    val heroLineHeight: TextUnit,
    val metricText: TextUnit,
    val metricTextCompact: TextUnit,
    val metricUnitText: TextUnit,
    val metricLabelText: TextUnit,
    val sectionText: TextUnit,
    val primaryButtonHeight: Dp,
    val primaryButtonText: TextUnit,
    val controlButton: Dp,
    val controlIcon: Dp,
    val controlGap: Dp,
) {
    companion object {
        val FortyMm = WatchUiSpec(
            horizontalPadding = 12.dp,
            topSpacer = 16.dp,
            bottomSpacer = 10.dp,
            listSpacing = 6.dp,
            panelHorizontalPadding = 10.dp,
            panelVerticalPadding = 8.dp,
            panelRadius = 14.dp,
            tileGap = 6.dp,
            tileHorizontalPadding = 5.dp,
            tileVerticalPadding = 6.dp,
            tileRadius = 12.dp,
            tileMinHeight = 52.dp,
            pillHorizontalPadding = 9.dp,
            pillVerticalPadding = 4.dp,
            ambientHorizontalPadding = 22.dp,
            smallIcon = 12.dp,
            titleIcon = 22.dp,
            titleText = 23.sp,
            subtitleText = 10.sp,
            bodyText = 10.sp,
            bodyLineHeight = 13.sp,
            elapsedText = 29.sp,
            elapsedLineHeight = 31.sp,
            heroText = 32.sp,
            heroTextCompact = 28.sp,
            heroLineHeight = 34.sp,
            metricText = 13.sp,
            metricTextCompact = 11.sp,
            metricUnitText = 8.sp,
            metricLabelText = 8.sp,
            sectionText = 13.sp,
            primaryButtonHeight = 42.dp,
            primaryButtonText = 13.sp,
            controlButton = 48.dp,
            controlIcon = 23.dp,
            controlGap = 12.dp,
        )

        private val Roomy = FortyMm.copy(
            horizontalPadding = 14.dp,
            topSpacer = 20.dp,
            bottomSpacer = 16.dp,
            listSpacing = 8.dp,
            panelHorizontalPadding = 12.dp,
            panelVerticalPadding = 10.dp,
            panelRadius = 18.dp,
            tileGap = 8.dp,
            tileHorizontalPadding = 8.dp,
            tileVerticalPadding = 8.dp,
            tileRadius = 16.dp,
            tileMinHeight = 58.dp,
            ambientHorizontalPadding = 26.dp,
            smallIcon = 13.dp,
            titleIcon = 24.dp,
            titleText = 24.sp,
            subtitleText = 11.sp,
            bodyText = 11.sp,
            bodyLineHeight = 14.sp,
            elapsedText = 31.sp,
            elapsedLineHeight = 33.sp,
            heroText = 35.sp,
            heroTextCompact = 31.sp,
            heroLineHeight = 38.sp,
            metricText = 15.sp,
            metricTextCompact = 13.sp,
            metricUnitText = 9.sp,
            metricLabelText = 9.sp,
            sectionText = 14.sp,
            primaryButtonHeight = 44.dp,
            primaryButtonText = 14.sp,
            controlButton = 52.dp,
            controlIcon = 25.dp,
            controlGap = 14.dp,
        )

        fun forViewport(width: Dp, height: Dp): WatchUiSpec {
            return if (minOf(width, height) <= 210.dp) FortyMm else Roomy
        }
    }
}

private val LocalWatchUiSpec = staticCompositionLocalOf { WatchUiSpec.FortyMm }

class MainActivity : ComponentActivity() {
    private var hasAllPermissionsState by mutableStateOf(false)
    private var isAmbientState by mutableStateOf(false)
    private lateinit var ambientObserver: AmbientLifecycleObserver

    private val ambientCallback = object : AmbientLifecycleObserver.AmbientLifecycleCallback {
        override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
            isAmbientState = true
        }

        override fun onExitAmbient() {
            isAmbientState = false
        }

        override fun onUpdateAmbient() = Unit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)
        ambientObserver = AmbientLifecycleObserver(this, ambientCallback)
        lifecycle.addObserver(ambientObserver)
        FitnessTracker.initialize(this)
        hasAllPermissionsState = hasRequiredPermissions()

        setContent {
            WearApp(
                hasAllPermissions = hasAllPermissionsState,
                isAmbient = isAmbientState,
                onRunTapped = {
                    if (hasAllPermissionsState) {
                        startTrackingService()
                    } else {
                        permissionLauncher.launch(requiredPermissions().toTypedArray())
                    }
                },
                onStopTracking = { stopTrackingService() },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        hasAllPermissionsState = hasRequiredPermissions()
    }

    override fun onDestroy() {
        if (::ambientObserver.isInitialized) {
            lifecycle.removeObserver(ambientObserver)
        }
        super.onDestroy()
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            hasAllPermissionsState = hasRequiredPermissions()
            if (hasAllPermissionsState) {
                startTrackingService()
            } else {
                FitnessTracker.reportPermissionError()
            }
        }

    private fun requiredPermissions(): List<String> {
        val permissions = mutableListOf(Manifest.permission.ACTIVITY_RECOGNITION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        if (Build.VERSION.SDK_INT >= 36) {
            permissions += "android.permission.health.READ_HEART_RATE"
        } else {
            permissions += Manifest.permission.BODY_SENSORS
        }
        return permissions
    }

    private fun hasRequiredPermissions(): Boolean {
        return requiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(this, permission) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startTrackingService() {
        val intent = Intent(this, TrackingForegroundService::class.java).apply {
            action = TrackingForegroundService.ACTION_START
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopTrackingService() {
        val intent = Intent(this, TrackingForegroundService::class.java).apply {
            action = TrackingForegroundService.ACTION_STOP
        }
        startService(intent)
    }
}

@Composable
private fun WearApp(
    hasAllPermissions: Boolean,
    isAmbient: Boolean,
    onRunTapped: () -> Unit,
    onStopTracking: () -> Unit,
) {
    val uiState by FitnessTracker.uiState.collectAsState()
    val elapsedMs = rememberLiveElapsed(uiState)

    UniMove_WearableTheme {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            val spec = remember(maxWidth, maxHeight) { WatchUiSpec.forViewport(maxWidth, maxHeight) }
            CompositionLocalProvider(LocalWatchUiSpec provides spec) {
                Scaffold(
                    timeText = {
                        if (uiState.phase != TrackingPhase.SUMMARY) {
                            TimeText()
                        }
                    },
                ) {
                    if (isAmbient && uiState.phase.isActiveSession()) {
                        AmbientRunScreen(uiState = uiState, elapsedMs = elapsedMs)
                    } else {
                        when (uiState.phase) {
                            TrackingPhase.RUNNING,
                            TrackingPhase.STARTING,
                            TrackingPhase.STOPPING,
                            TrackingPhase.PAUSED,
                            -> LiveRunScreen(
                                uiState = uiState,
                                elapsedMs = elapsedMs,
                                onStopTracking = onStopTracking,
                            )

                            TrackingPhase.SUMMARY -> SummaryScreen(
                                uiState = uiState,
                                onFinish = { FitnessTracker.finishSession() },
                            )

                            else -> RunStartScreen(
                                uiState = uiState,
                                hasAllPermissions = hasAllPermissions,
                                onRunTapped = onRunTapped,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RunStartScreen(
    uiState: TrackingUiState,
    hasAllPermissions: Boolean,
    onRunTapped: () -> Unit,
) {
    val spec = LocalWatchUiSpec.current
    WatchList {
        item { Spacer(modifier = Modifier.height(spec.topSpacer)) }
        item {
            PhoneStatusPill(isConnected = uiState.isPhoneConnected)
        }
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                    contentDescription = null,
                    tint = RunMint,
                    modifier = Modifier.size(spec.titleIcon),
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "UniMove",
                    color = RunInk,
                    fontSize = spec.titleText,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
                Text(
                    text = "Watch run tracker",
                    color = RunMuted,
                    fontSize = spec.subtitleText,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        uiState.summary?.let { summary ->
            item {
                LastRunPanel(summary = summary)
            }
        }
        item {
            StatusPanel(
                text = startStatusText(uiState, hasAllPermissions),
                color = if (uiState.errorMessage != null || !uiState.isSupported) RunRed else RunMuted,
            )
        }
        if (uiState.hasPendingSync || uiState.lastSyncEpochMs != null) {
            item {
                SyncPanel(uiState = uiState)
            }
        }
        item {
            PrimaryActionButton(
                text = when {
                    !uiState.capabilitiesChecked -> "Checking sensors"
                    hasAllPermissions -> "Start run"
                    else -> "Grant access"
                },
                enabled = uiState.capabilitiesChecked && uiState.isSupported,
                onClick = onRunTapped,
            )
        }
        item { Spacer(modifier = Modifier.height(spec.bottomSpacer)) }
    }
}

@Composable
private fun LiveRunScreen(
    uiState: TrackingUiState,
    elapsedMs: Long,
    onStopTracking: () -> Unit,
) {
    val spec = LocalWatchUiSpec.current
    WatchList {
        item { Spacer(modifier = Modifier.height(spec.topSpacer)) }
        item {
            SessionPill(uiState = uiState)
        }
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatDuration(elapsedMs),
                    color = RunInk,
                    fontSize = spec.elapsedText,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = spec.elapsedLineHeight,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
                Text(
                    text = "elapsed",
                    color = RunMuted,
                    fontSize = spec.metricLabelText,
                    maxLines = 1,
                )
            }
        }
        item {
            HeroMetricPanel(
                value = formatDistance(uiState.distanceMeters),
                label = "distance",
                color = RunInk,
            )
        }
        item {
            MetricGrid(
                first = MetricData(
                    label = "heart",
                    value = uiState.heartRate?.toString() ?: "--",
                    unit = "bpm",
                    color = RunRed,
                    icon = Icons.Default.Favorite,
                ),
                second = MetricData(
                    label = "pace",
                    value = formatPaceValue(elapsedMs, uiState.distanceMeters),
                    unit = "/km",
                    color = RunCyan,
                    icon = null,
                ),
            )
        }
        item {
            MetricGrid(
                first = MetricData(
                    label = "steps",
                    value = uiState.steps.toString(),
                    unit = "",
                    color = RunInk,
                    icon = null,
                ),
                second = MetricData(
                    label = "sync",
                    value = compactSyncText(uiState),
                    unit = "",
                    color = if (uiState.hasPendingSync) RunAmber else RunMint,
                    icon = Icons.Default.Smartphone,
                ),
            )
        }
        item {
            PhoneStatusPill(isConnected = uiState.isPhoneConnected)
        }
        uiState.errorMessage?.let { error ->
            item {
                StatusPanel(text = error, color = RunRed)
            }
        }
        item {
            ControlRow(
                phase = uiState.phase,
                onStopTracking = onStopTracking,
            )
        }
        item { Spacer(modifier = Modifier.height(spec.bottomSpacer)) }
    }
}

@Composable
private fun AmbientRunScreen(
    uiState: TrackingUiState,
    elapsedMs: Long,
) {
    val spec = LocalWatchUiSpec.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = spec.ambientHorizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = if (uiState.phase == TrackingPhase.PAUSED) "Paused" else "Run active",
                color = if (uiState.phase == TrackingPhase.PAUSED) RunAmber else RunMint,
                fontSize = spec.sectionText,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                text = formatDistance(uiState.distanceMeters),
                color = RunInk,
                fontSize = spec.elapsedText,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
            Text(
                text = formatDuration(elapsedMs),
                color = RunMuted,
                fontSize = spec.metricText,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                text = "Wake for controls",
                color = RunMuted,
                fontSize = spec.metricLabelText,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SummaryScreen(
    uiState: TrackingUiState,
    onFinish: () -> Unit,
) {
    val summary = uiState.summary
    val spec = LocalWatchUiSpec.current
    WatchList {
        item { Spacer(modifier = Modifier.height(spec.topSpacer)) }
        item {
            Text(
                text = "Run complete",
                color = RunMint,
                fontSize = spec.sectionText,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
        if (summary != null) {
            item {
                HeroMetricPanel(
                    value = formatDistance(summary.distanceMeters),
                    label = "total distance",
                    color = RunInk,
                )
            }
            item {
                MetricGrid(
                    first = MetricData(
                        label = "time",
                        value = formatDuration(summary.durationMs),
                        unit = "",
                        color = RunInk,
                        icon = null,
                    ),
                    second = MetricData(
                        label = "steps",
                        value = summary.steps.toString(),
                        unit = "",
                        color = RunInk,
                        icon = null,
                    ),
                )
            }
            item {
                MetricGrid(
                    first = MetricData(
                        label = "avg hr",
                        value = summary.averageHeartRate?.toString() ?: "--",
                        unit = "bpm",
                        color = RunRed,
                        icon = Icons.Default.Favorite,
                    ),
                    second = MetricData(
                        label = "pace",
                        value = formatPaceValue(summary.durationMs, summary.distanceMeters),
                        unit = "/km",
                        color = RunCyan,
                        icon = null,
                    ),
                )
            }
            item {
                SyncPanel(uiState = uiState)
            }
        } else {
            item {
                StatusPanel(
                    text = "No session data found.",
                    color = RunMuted,
                )
            }
        }
        item {
            PrimaryActionButton(
                text = "Finish",
                enabled = true,
                onClick = onFinish,
            )
        }
        item { Spacer(modifier = Modifier.height(spec.bottomSpacer)) }
    }
}

@Composable
private fun WatchList(
    content: ScalingLazyListScope.() -> Unit,
) {
    val spec = LocalWatchUiSpec.current
    val listState = rememberScalingLazyListState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = spec.horizontalPadding),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spec.listSpacing),
            content = content,
        )
    }
}

@Composable
private fun PhoneStatusPill(isConnected: Boolean) {
    val spec = LocalWatchUiSpec.current
    val color = if (isConnected) RunMint else RunMuted
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .background(
                color = if (isConnected) RunMintDim else RunSurface,
                shape = CircleShape,
            )
            .border(
                border = BorderStroke(1.dp, if (isConnected) RunMint.copy(alpha = 0.45f) else RunBorder),
                shape = CircleShape,
            )
            .padding(horizontal = spec.pillHorizontalPadding, vertical = spec.pillVerticalPadding),
    ) {
        Icon(
            imageVector = Icons.Default.Smartphone,
            contentDescription = if (isConnected) "Phone connected" else "Phone unavailable",
            tint = color,
            modifier = Modifier.size(spec.smallIcon),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (isConnected) "Phone connected" else "Phone unavailable",
            color = color,
            fontSize = spec.bodyText,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LastRunPanel(summary: RunSummary) {
    val spec = LocalWatchUiSpec.current
    RoundedPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = "Last run",
                    color = RunMuted,
                    fontSize = spec.metricLabelText,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
                Text(
                    text = formatDuration(summary.durationMs),
                    color = RunInk,
                    fontSize = spec.metricText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
            Text(
                text = formatDistance(summary.distanceMeters),
                color = RunMint,
                fontSize = spec.sectionText,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

@Composable
private fun StatusPanel(
    text: String,
    color: Color,
) {
    val spec = LocalWatchUiSpec.current
    Text(
        text = text,
        color = color,
        fontSize = spec.bodyText,
        lineHeight = spec.bodyLineHeight,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
    )
}

@Composable
private fun SyncPanel(uiState: TrackingUiState) {
    val spec = LocalWatchUiSpec.current
    val color = when {
        uiState.syncStatus == SyncStatus.FAILED -> RunRed
        uiState.syncStatus == SyncStatus.PENDING || uiState.syncStatus == SyncStatus.RETRYING -> RunAmber
        uiState.syncStatus == SyncStatus.SYNCED -> RunMint
        else -> RunMuted
    }
    RoundedPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Smartphone,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(spec.smallIcon),
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = syncText(uiState),
                color = color,
                fontSize = spec.bodyText,
                fontWeight = FontWeight.SemiBold,
                lineHeight = spec.bodyLineHeight,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}

@Composable
private fun SessionPill(uiState: TrackingUiState) {
    val spec = LocalWatchUiSpec.current
    val color = phaseColor(uiState.phase)
    val label = uiState.statusMessage ?: phaseLabel(uiState.phase)
    Text(
        text = label,
        color = color,
        fontSize = spec.bodyText,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), CircleShape)
            .border(BorderStroke(1.dp, color.copy(alpha = 0.45f)), CircleShape)
            .padding(horizontal = spec.pillHorizontalPadding, vertical = spec.pillVerticalPadding),
    )
}

@Composable
private fun HeroMetricPanel(
    value: String,
    label: String,
    color: Color,
) {
    val spec = LocalWatchUiSpec.current
    val valueSize = if (value.length > 7) spec.heroTextCompact else spec.heroText
    RoundedPanel(
        background = RunSurfaceSoft,
        border = RunMint.copy(alpha = 0.35f),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                color = color,
                fontSize = valueSize,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = spec.heroLineHeight,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
            Text(
                text = label,
                color = RunMuted,
                fontSize = spec.metricLabelText,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private data class MetricData(
    val label: String,
    val value: String,
    val unit: String,
    val color: Color,
    val icon: ImageVector?,
)

@Composable
private fun MetricGrid(
    first: MetricData,
    second: MetricData,
) {
    val spec = LocalWatchUiSpec.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spec.tileGap),
    ) {
        MiniMetric(
            data = first,
            modifier = Modifier.weight(1f),
        )
        MiniMetric(
            data = second,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MiniMetric(
    data: MetricData,
    modifier: Modifier = Modifier,
) {
    val spec = LocalWatchUiSpec.current
    val valueSize = if (data.value.length > 5) spec.metricTextCompact else spec.metricText
    Column(
        modifier = modifier
            .heightIn(min = spec.tileMinHeight)
            .background(RunSurface, RoundedCornerShape(spec.tileRadius))
            .border(BorderStroke(1.dp, RunBorder), RoundedCornerShape(spec.tileRadius))
            .padding(horizontal = spec.tileHorizontalPadding, vertical = spec.tileVerticalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            data.icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = data.color,
                    modifier = Modifier.size(spec.smallIcon),
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = data.value,
                color = data.color,
                fontSize = valueSize,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        if (data.unit.isNotBlank()) {
            Text(
                text = data.unit,
                color = data.color.copy(alpha = 0.78f),
                fontSize = spec.metricUnitText,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
        Text(
            text = data.label,
            color = RunMuted,
            fontSize = spec.metricLabelText,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ControlRow(
    phase: TrackingPhase,
    onStopTracking: () -> Unit,
) {
    val spec = LocalWatchUiSpec.current
    val isPaused = phase == TrackingPhase.PAUSED
    val isStopping = phase == TrackingPhase.STOPPING
    Row(
        horizontalArrangement = Arrangement.spacedBy(spec.controlGap),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp),
    ) {
        Button(
            onClick = {
                if (isPaused) {
                    FitnessTracker.resumeTracking()
                } else {
                    FitnessTracker.pauseTracking()
                }
            },
            enabled = phase == TrackingPhase.RUNNING || phase == TrackingPhase.PAUSED,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (isPaused) RunMint else RunAmber,
                contentColor = Color.Black,
            ),
            modifier = Modifier.size(spec.controlButton),
        ) {
            Icon(
                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = if (isPaused) "Resume run" else "Pause run",
                tint = Color.Black,
                modifier = Modifier.size(spec.controlIcon),
            )
        }

        Button(
            onClick = onStopTracking,
            enabled = !isStopping,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = RunRed,
                contentColor = Color.White,
            ),
            modifier = Modifier.size(spec.controlButton),
        ) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = "Stop run",
                tint = Color.White,
                modifier = Modifier.size(spec.controlIcon),
            )
        }
    }
}

@Composable
private fun PrimaryActionButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val spec = LocalWatchUiSpec.current
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .height(spec.primaryButtonHeight),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = RunMint,
            contentColor = Color.Black,
        ),
        onClick = onClick,
    ) {
        Text(
            text = text,
            color = Color.Black,
            fontSize = spec.primaryButtonText,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RoundedPanel(
    background: Color = RunSurface,
    border: Color = RunBorder,
    content: @Composable () -> Unit,
) {
    val spec = LocalWatchUiSpec.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(spec.panelRadius))
            .border(BorderStroke(1.dp, border), RoundedCornerShape(spec.panelRadius))
            .padding(horizontal = spec.panelHorizontalPadding, vertical = spec.panelVerticalPadding),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun rememberLiveElapsed(uiState: TrackingUiState): Long {
    var nowEpochMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(uiState.phase, uiState.activeDurationCheckpointEpochMs, uiState.activeDurationBaseMs) {
        while (uiState.phase == TrackingPhase.RUNNING) {
            nowEpochMs = System.currentTimeMillis()
            delay(1_000)
        }
    }

    val checkpointEpochMs = uiState.activeDurationCheckpointEpochMs ?: return uiState.activeDurationBaseMs
    return if (uiState.phase == TrackingPhase.RUNNING) {
        uiState.activeDurationBaseMs + (nowEpochMs - checkpointEpochMs).coerceAtLeast(0L)
    } else {
        uiState.activeDurationBaseMs
    }
}

private fun TrackingPhase.isActiveSession(): Boolean {
    return this == TrackingPhase.RUNNING ||
        this == TrackingPhase.STARTING ||
        this == TrackingPhase.STOPPING ||
        this == TrackingPhase.PAUSED
}

private fun phaseLabel(phase: TrackingPhase): String {
    return when (phase) {
        TrackingPhase.STARTING -> "Preparing sensors"
        TrackingPhase.RUNNING -> "Run active"
        TrackingPhase.PAUSED -> "Paused"
        TrackingPhase.STOPPING -> "Ending run"
        TrackingPhase.ERROR -> "Needs attention"
        else -> "Ready"
    }
}

private fun phaseColor(phase: TrackingPhase): Color {
    return when (phase) {
        TrackingPhase.PAUSED -> RunAmber
        TrackingPhase.STOPPING,
        TrackingPhase.ERROR,
        -> RunRed
        TrackingPhase.STARTING -> RunCyan
        else -> RunMint
    }
}

private fun startStatusText(
    uiState: TrackingUiState,
    hasAllPermissions: Boolean,
): String {
    return when {
        uiState.errorMessage != null -> uiState.errorMessage
        !uiState.capabilitiesChecked -> "Checking Health Services running capabilities."
        !uiState.isSupported -> "This watch does not support distance and heart-rate run tracking."
        !hasAllPermissions -> "Grant motion, heart-rate, and notification access to start."
        else -> "Ready to start. Live stats will sync to your phone."
    }
}

private fun syncText(uiState: TrackingUiState): String {
    return when (uiState.syncStatus) {
        SyncStatus.FAILED -> "Sync failed; retrying when phone returns"
        SyncStatus.RETRYING -> "Sync failed/retrying"
        SyncStatus.PENDING -> if (uiState.isPhoneConnected) "Sync pending" else "Sync pending; phone unavailable"
        SyncStatus.SYNCED -> "Synced to phone"
        SyncStatus.NONE -> if (uiState.isPhoneConnected) "Phone connected" else "Phone unavailable"
    }
}

private fun compactSyncText(uiState: TrackingUiState): String {
    return when (uiState.syncStatus) {
        SyncStatus.FAILED -> "failed"
        SyncStatus.RETRYING -> "retrying"
        SyncStatus.PENDING -> "pending"
        SyncStatus.SYNCED -> "synced"
        SyncStatus.NONE -> if (uiState.isPhoneConnected) "ready" else "offline"
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1_000
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

private fun formatDistance(distanceMeters: Double): String {
    return String.format(Locale.getDefault(), "%.2f km", distanceMeters / 1_000.0)
}

private fun formatPaceValue(
    elapsedMs: Long,
    distanceMeters: Double,
): String {
    if (elapsedMs <= 0L || distanceMeters < 10.0) {
        return "--:--"
    }

    val totalSeconds = elapsedMs / 1_000.0
    val secondsPerKm = totalSeconds / (distanceMeters / 1_000.0)
    val minutes = (secondsPerKm / 60).toInt()
    val seconds = (secondsPerKm % 60).toInt()
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}
