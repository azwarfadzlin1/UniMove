package com.example.unimove.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.unimove.ui.theme.*
import com.example.unimove.data.model.CalorieStatus
import com.example.unimove.data.model.buildCalorieProgress
import com.example.unimove.data.weather.OwmForecastItem
import com.example.unimove.ui.viewmodel.AuthViewModel
import com.example.unimove.ui.screens.onboarding.onboardingAvatarOptions
import com.example.unimove.ui.viewmodel.MealLogViewModel
import com.example.unimove.ui.viewmodel.WeatherState
import com.example.unimove.ui.viewmodel.WeatherViewModel
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import com.airbnb.lottie.compose.*
import com.example.unimove.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

// ─── Shared constants ───────────────────────────────────────────────
private val CardRadius = RoundedCornerShape(20.dp)
private val CardElevation = 2.dp
private val CardHeight = 170.dp
private val GridGap = 14.dp
private val HomeCardTitleSize = 14.sp
private val HomeCardValueSize = 20.sp
private val HomeCardSupportingSize = 12.sp

@Composable
private fun AutoFitSingleLineText(
    text: String,
    maxFontSize: TextUnit,
    minFontSize: TextUnit,
    color: Color,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
    letterSpacing: TextUnit = 0.sp,
    textAlign: TextAlign = TextAlign.Start
) {
    var fontSize by remember(text, maxFontSize, minFontSize) { mutableStateOf(maxFontSize) }

    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        letterSpacing = letterSpacing,
        lineHeight = fontSize,
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.hasVisualOverflow && fontSize.value > minFontSize.value) {
                fontSize = (fontSize.value * 0.9f).coerceAtLeast(minFontSize.value).sp
            }
        }
    )
}

private fun formatCaloriesForHome(calories: Int): String {
    val safeCalories = calories.coerceAtLeast(0)
    return when {
        safeCalories >= 100_000 -> "${safeCalories / 1_000}k"
        safeCalories >= 10_000 -> {
            val compact = String.format(Locale.getDefault(), "%.1fk", safeCalories / 1_000.0)
            compact.replace(".0k", "k")
        }
        else -> safeCalories.toString()
    }
}

// Reusable press feedback so cards feel tappable without idle pulsing.
private fun Modifier.cardClickableMicroAnimation(onClick: (() -> Unit)?): Modifier = composed {
    if (onClick == null) return@composed this

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "card_press_scale"
    )

    this
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
        }
        .semantics { role = Role.Button }
        .clickable(
            interactionSource = interactionSource,
            onClick = onClick
        )
}

@Composable
private fun HomeCardAccentLine(accentColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(4.dp)
            .fillMaxHeight()
            .background(
                Brush.verticalGradient(
                    listOf(accentColor, accentColor.copy(alpha = 0.28f))
                )
            )
    )
}

@Composable
fun HomeScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(),
    mealLogViewModel: MealLogViewModel = viewModel()
) {
    Scaffold(
        bottomBar = {
            UniMoveBottomBar(
                navController = navController,
                modifier = Modifier
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        listOf(SurfaceLight, SurfaceLightEnd)
                    )
                )
        ) {
            DashboardHomeContent(navController, authViewModel, mealLogViewModel)
            FloatingHamburgerMenu(navController, this)
        }
    }
}
@Composable
private fun AnimatedWeatherBackdrop(
    modifier: Modifier = Modifier,
    isNight: Boolean
) {
    val transition = rememberInfiniteTransition(label = "weather_backdrop")
    val driftX by transition.animateFloat(
        initialValue = 0f,
        targetValue = 90f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "backdrop_drift_x"
    )
    val driftY by transition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "backdrop_drift_y"
    )

    Canvas(modifier = modifier) {
        drawCircle(
            color = if (isNight) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.16f),
            radius = size.minDimension * 0.28f,
            center = Offset(size.width * 0.82f + driftX, size.height * 0.16f)
        )
        drawCircle(
            color = if (isNight) Color(0xFFA9B8FF).copy(alpha = 0.10f) else Color(0xFFBEE7FF).copy(alpha = 0.14f),
            radius = size.minDimension * 0.22f,
            center = Offset(size.width * 0.18f - driftX * 0.35f, size.height * 0.88f - driftY)
        )
        drawCircle(
            color = if (isNight) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.08f),
            radius = size.minDimension * 0.16f,
            center = Offset(size.width * 0.56f, size.height * 0.36f + driftY)
        )
    }
}

@Composable
private fun WeatherMetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    compactMode: Boolean = false
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.13f))
            .padding(
                horizontal = if (compactMode) 8.dp else 10.dp,
                vertical = if (compactMode) 7.dp else 8.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.78f),
            fontSize = if (compactMode) 10.sp else 11.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = if (compactMode) 12.sp else 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AnimatedWeatherConditionIcon(
    weatherMain: String,
    isNight: Boolean,
    modifier: Modifier = Modifier,
    compactMode: Boolean = false
) {
    val normalized = weatherMain.lowercase(Locale.getDefault())
    val transition = rememberInfiniteTransition(label = "weather_icon_motion")
    val bobOffset by transition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "weather_icon_bob"
    )
    val glowAlpha by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "weather_icon_glow"
    )

    val icon = when {
        normalized.contains("thunderstorm") -> Icons.Default.Bolt
        normalized.contains("rain") || normalized.contains("drizzle") -> Icons.Default.WaterDrop
        normalized.contains("cloud") -> Icons.Default.Cloud
        isNight -> Icons.Default.NightsStay
        else -> Icons.Default.WbSunny
    }
    val iconTint = when {
        normalized.contains("thunderstorm") -> Color(0xFFFFE08A)
        normalized.contains("rain") || normalized.contains("drizzle") -> Color(0xFF9DDCFF)
        isNight -> Color(0xFFC8D6FF)
        else -> Color(0xFFFFE08A)
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = glowAlpha), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier
                .size(if (compactMode) 20.dp else 22.dp)
                .graphicsLayer {
                    translationY = bobOffset
                }
        )
    }
}

private fun isNightNow(sunrise: Long, sunset: Long): Boolean {
    val nowEpoch = Instant.now().epochSecond
    return nowEpoch < sunrise || nowEpoch >= sunset
}

private fun buildLocationLabel(city: String, country: String): String {
    val cityName = city.ifBlank { "Current Location" }
    return if (country.isBlank()) cityName else "$cityName, $country"
}

private suspend fun resolveLocationLabel(context: Context, location: Location): String? {
    if (!Geocoder.isPresent()) return null

    return withContext(Dispatchers.IO) {
        runCatching {
            val addresses = Geocoder(context, Locale.getDefault()).getFromLocation(
                location.latitude,
                location.longitude,
                1
            )
            val address = addresses?.firstOrNull() ?: return@runCatching null

            val primary = address.subLocality ?: address.locality ?: address.subAdminArea ?: address.adminArea
            val countryCode = address.countryCode

            when {
                primary.isNullOrBlank() && countryCode.isNullOrBlank() -> null
                countryCode.isNullOrBlank() -> primary
                primary.isNullOrBlank() -> countryCode
                else -> "$primary, $countryCode"
            }
        }.getOrNull()
    }
}

private enum class WeatherRunDecision {
    Go,
    Caution,
    Indoor
}

private data class WeatherRunInsight(
    val decision: WeatherRunDecision,
    val headline: String,
    val reason: String,
    val action: String
)

private data class WeatherForecastChipData(
    val label: String,
    val tempLabel: String,
    val rainChanceLabel: String
)

private fun buildWeatherRunInsight(
    weatherMain: String,
    weatherDescription: String,
    temp: Float,
    feelsLike: Float,
    humidity: Int,
    windSpeed: Float,
    precipitationChance: Float,
    isNight: Boolean
): WeatherRunInsight {
    val normalizedMain = weatherMain.lowercase(Locale.getDefault())
    val normalizedDescription = weatherDescription.lowercase(Locale.getDefault())
    val stormRisk = normalizedMain.contains("thunderstorm") ||
        normalizedDescription.contains("heavy rain") ||
        precipitationChance >= 0.65f
    val rainRisk = normalizedMain.contains("rain") ||
        normalizedMain.contains("drizzle") ||
        precipitationChance >= 0.35f
    val oppressiveHeat = feelsLike >= 35f || (temp >= 32f && humidity >= 82)
    val hotButRunnable = feelsLike >= 31f || temp >= 31f || humidity >= 75
    val strongWind = windSpeed >= 9f

    return when {
        stormRisk -> WeatherRunInsight(
            decision = WeatherRunDecision.Indoor,
            headline = "Best kept indoors",
            reason = "Rain risk is high and footing will be inconsistent.",
            action = "Swap to treadmill, mobility, or a short strength session."
        )
        oppressiveHeat -> WeatherRunInsight(
            decision = WeatherRunDecision.Indoor,
            headline = "Heat will drain this run",
            reason = "Feels-like temperature is high enough to push pace and heart rate up fast.",
            action = "Move it indoors or wait for a cooler window later."
        )
        rainRisk && strongWind -> WeatherRunInsight(
            decision = WeatherRunDecision.Caution,
            headline = "Short run only",
            reason = "Light rain and wind will make the effort feel heavier than usual.",
            action = "Keep it easy, shorten the route, and avoid slick turns."
        )
        isNight -> WeatherRunInsight(
            decision = WeatherRunDecision.Caution,
            headline = "Good for an easy run",
            reason = "Conditions are manageable, but visibility is your main limiter tonight.",
            action = "Stay on lit routes and keep the session controlled."
        )
        hotButRunnable || strongWind -> WeatherRunInsight(
            decision = WeatherRunDecision.Caution,
            headline = "Run with restraint",
            reason = "The weather is runnable, but not ideal for hard pace or long duration.",
            action = "Treat this as an easy or moderate effort."
        )
        else -> WeatherRunInsight(
            decision = WeatherRunDecision.Go,
            headline = "Good window to run",
            reason = "The current mix of temperature, rain risk, and wind looks friendly.",
            action = "Tempo or easy mileage both make sense right now."
        )
    }
}

private fun buildWeatherForecastChips(
    items: List<OwmForecastItem>,
    timezoneSeconds: Int
): List<WeatherForecastChipData> {
    val formatter = DateTimeFormatter.ofPattern("ha", Locale.getDefault())
    val zoneOffset = ZoneOffset.ofTotalSeconds(timezoneSeconds)

    return items.take(3).map { item ->
        val hourLabel = Instant.ofEpochSecond(item.dt)
            .atOffset(zoneOffset)
            .format(formatter)

        WeatherForecastChipData(
            label = hourLabel,
            tempLabel = "${item.main.temp.toInt()}\u00B0",
            rainChanceLabel = "${(item.pop * 100f).roundToInt()}%"
        )
    }
}

@Composable
private fun WeatherDecisionBadge(
    insight: WeatherRunInsight,
    compactMode: Boolean
) {
    val (label, background, contentColor) = when (insight.decision) {
        WeatherRunDecision.Go -> Triple("Go", Color(0xFF143E2D), Color(0xFF97F7C8))
        WeatherRunDecision.Caution -> Triple("Caution", Color(0xFF4B3410), Color(0xFFFFDB86))
        WeatherRunDecision.Indoor -> Triple("Indoor", Color(0xFF4A1F26), Color(0xFFFFB7B0))
    }

    Surface(
        color = background.copy(alpha = 0.82f),
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = label,
            color = contentColor,
            fontSize = if (compactMode) 10.sp else 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun WeatherForecastChip(
    chip: WeatherForecastChipData,
    modifier: Modifier = Modifier,
    compactMode: Boolean = false
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .padding(horizontal = if (compactMode) 10.dp else 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = chip.label,
            color = Color.White.copy(alpha = 0.76f),
            fontSize = if (compactMode) 10.sp else 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = chip.tempLabel,
            color = Color.White,
            fontSize = if (compactMode) 13.sp else 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = chip.rainChanceLabel,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = if (compactMode) 10.sp else 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun FloatingHamburgerMenu(
    navController: NavController,
    boxWithConstraintsScope: BoxWithConstraintsScope
) {
    var expanded by remember { mutableStateOf(false) }
    val transition = updateTransition(targetState = expanded, label = "MenuTransition")

    val rotation by transition.animateFloat(
        label = "Rotation",
        transitionSpec = { spring(stiffness = Spring.StiffnessLow) }
    ) { state -> if (state) 45f else 0f }

    val radius by transition.animateDp(
        label = "Radius",
        transitionSpec = { spring(stiffness = Spring.StiffnessLow) }
    ) { state -> if (state) 150.dp else 0.dp }

    val scale by transition.animateFloat(
        label = "Scale",
        transitionSpec = { spring(stiffness = Spring.StiffnessLow) }
    ) { state -> if (state) 1f else 0f }

    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var hasInitialPosition by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val fabSize = 56.dp
    val fabSizePx = with(density) { fabSize.toPx() }

    val horizontalMargin = 16.dp
    val verticalMargin = 16.dp

    val maxWidthPx = with(density) { boxWithConstraintsScope.maxWidth.toPx() }
    val maxHeightPx = with(density) { boxWithConstraintsScope.maxHeight.toPx() }
    val horizontalMarginPx = with(density) { horizontalMargin.toPx() }
    val verticalMarginPx = with(density) { verticalMargin.toPx() }

    LaunchedEffect(maxWidthPx, maxHeightPx) {
        if (!hasInitialPosition && maxWidthPx > 0 && maxHeightPx > 0) {
            offsetX = maxWidthPx - fabSizePx - horizontalMarginPx
            offsetY = maxHeightPx - fabSizePx - verticalMarginPx
            hasInitialPosition = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
        ) {
            val items = listOf(
                Triple(Icons.Default.Groups, "Community", "community"),
                Triple(Icons.Default.SmartToy, "FitBot AI", "fitbot"),
                Triple(Icons.Default.SportsEsports, "Game", "achievement")
            )

            if (expanded || transition.currentState) {
                items.forEachIndexed { index, item ->
                    val angleStep = 30f
                    val angle = 180f + (angleStep * (index + 1))
                    val radians = Math.toRadians(angle.toDouble())

                    val itemOffsetX = (radius.value * cos(radians)).dp
                    val itemOffsetY = (radius.value * sin(radians)).dp

                    FloatingActionButton(
                        onClick = {
                            if (!isDragging) {
                                expanded = false
                                navController.navigate(item.third)
                            }
                        },
                        containerColor = CardSurface,
                        contentColor = AccentBlue,
                        shape = CircleShape,
                        modifier = Modifier
                            .offset(x = itemOffsetX, y = itemOffsetY)
                            .scale(scale)
                            .size(68.dp)
                            .shadow(6.dp, CircleShape)
                    ) {
                        Icon(item.first, contentDescription = item.second)
                    }
                }
            }

            FloatingActionButton(
                onClick = { if (!isDragging) expanded = !expanded },
                containerColor = AccentBlue,
                contentColor = Color.White,
                modifier = Modifier
                    .size(fabSize)
                    .shadow(8.dp, CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = { isDragging = false },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val newOffsetX = (offsetX + dragAmount.x).coerceIn(
                                    horizontalMarginPx,
                                    maxWidthPx - fabSizePx - horizontalMarginPx
                                )
                                val newOffsetY = (offsetY + dragAmount.y).coerceIn(
                                    verticalMarginPx,
                                    maxHeightPx - fabSizePx - verticalMarginPx
                                )
                                offsetX = newOffsetX
                                offsetY = newOffsetY
                            }
                        )
                    },
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Menu",
                    modifier = Modifier.rotate(rotation)
                )
            }
        }
    }
}

// --- Helper for Weather ---
private fun hasHomeLocationPermission(ctx: Context): Boolean {
    val fine = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
    return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
}

@SuppressLint("MissingPermission")
@Composable
private fun DashboardHomeContent(
    navController: NavController,
    authViewModel: AuthViewModel,
    mealLogViewModel: MealLogViewModel,
    weatherViewModel: WeatherViewModel = viewModel()
) {
    val context = LocalContext.current
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val coroutineScope = rememberCoroutineScope()
    var currentLocationLabel by remember { mutableStateOf<String?>(null) }

    fun requestWeatherForBestLocation() {
        val currentLocationRequest = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(0)
            .build()

        fun updateWeatherForLocation(location: Location) {
            weatherViewModel.fetchWeatherByLocation(location.latitude, location.longitude)
            coroutineScope.launch {
                currentLocationLabel = resolveLocationLabel(context, location)
            }
        }

        fusedClient.getCurrentLocation(currentLocationRequest, null)
            .addOnSuccessListener { currentLoc ->
                if (currentLoc != null) {
                    updateWeatherForLocation(currentLoc)
                } else {
                    fusedClient.lastLocation
                        .addOnSuccessListener { lastLoc: Location? ->
                            if (lastLoc != null) {
                                updateWeatherForLocation(lastLoc)
                            } else {
                                currentLocationLabel = null
                                weatherViewModel.fetchWeatherByCity("Kajang")
                            }
                        }
                        .addOnFailureListener {
                            currentLocationLabel = null
                            weatherViewModel.fetchWeatherByCity("Kajang")
                        }
                }
            }
            .addOnFailureListener {
                fusedClient.lastLocation
                    .addOnSuccessListener { lastLoc: Location? ->
                        if (lastLoc != null) {
                            updateWeatherForLocation(lastLoc)
                        } else {
                            currentLocationLabel = null
                            weatherViewModel.fetchWeatherByCity("Kajang")
                        }
                    }
                    .addOnFailureListener {
                        currentLocationLabel = null
                        weatherViewModel.fetchWeatherByCity("Kajang")
                    }
            }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.any { it }
        if (granted) {
            requestWeatherForBestLocation()
        } else {
            weatherViewModel.fetchWeatherByCity("Kajang")
        }
    }

    LaunchedEffect(Unit) {
        if (hasHomeLocationPermission(context)) {
            requestWeatherForBestLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val profile = authUiState.profile
    val userName = authUiState.currentUserName ?: "User"
    val calorieHomeState by mealLogViewModel.homeCardState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        mealLogViewModel.observeHomeCard()
    }

    val totals by authViewModel.totals.collectAsStateWithLifecycle()
    var communitySharesToday by remember { mutableIntStateOf(0) }
    val todayStart = remember {
        Timestamp(java.util.Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()))
    }

    DisposableEffect(todayStart) {
        val registration = FirebaseFirestore.getInstance()
            .collection("community_feed")
            .whereGreaterThanOrEqualTo("timestamp", todayStart)
            .addSnapshotListener { snapshot, _ ->
                communitySharesToday = snapshot?.size() ?: 0
            }
        onDispose { registration.remove() }
    }

    val totalKm = totals.totalDistanceKm
    val totalTimeSec = totals.totalTimeSec

    val avgPaceSecPerKm = if (totalKm > 0.01) {
        (totalTimeSec.toDouble() / totalKm).toFloat()
    } else 0f

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(GridGap),
        verticalArrangement = Arrangement.spacedBy(GridGap),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // ── Header ──
        item(span = { GridItemSpan(2) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 12.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Hi, $userName",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = (-0.3).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Let's keep moving today 💪",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.1.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Modern Simple Profile Icon
                IconButton(
                    onClick = { navController.navigate("profile") },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(AccentBlue.copy(alpha = 0.15f))
                        .border(1.dp, AccentBlue.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = AccentBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // ── Weather Card ──
        item(span = { GridItemSpan(2) }) {
            WeatherCard(
                weatherViewModel = weatherViewModel,
                locationLabelOverride = currentLocationLabel
            )
        }

        item(span = { GridItemSpan(2) }) {
            GamificationEventCard(
                onClick = { navController.navigate("event_run") }
            )
        }

        // ── All Time Summary ── (span 2)
        item(span = { GridItemSpan(2) }) {
            AllTimeSummaryCard(
                totalKm = totalKm,
                totalTimeSec = totalTimeSec.toInt(),
                avgPaceSecPerKm = avgPaceSecPerKm
            )
        }

        // ── Grid cards ──
        item {
            ExerciseRecordCard(
                totalKm = totalKm,
                onClick = { navController.navigate("stats") }
            )
        }
        item {
            GoalsGridCard(
                achievedKm = totalKm,
                goalKm = profile?.runGoalKm,
                onClick = { navController.navigate("profile") }
            )
        }

        item {
            CalorieTrackerCard(
                todayCalories = calorieHomeState.todayCalories,
                goalCalories = profile?.dailyCalorieTarget,
                onClick = { navController.navigate("meal_log") },
                onSetTarget = { navController.navigate("meal_log") }
            )
        }
        item {
            WeightManagementCard(
                currentWeightKg = profile?.weight?.toFloat() ?: 0f,
                onClick = { navController.navigate("profile") }
            )
        }

        // ── Bottom Chips ──
        item { LeaderboardChip(navController = navController) }
        item {
            CommunityTeaseChip(
                sharesToday = communitySharesToday,
                onClick = { navController.navigate("community") }
            )
        }

        // ── FitBot AI Card ──
        item(span = { GridItemSpan(2) }) {
            FitBotHomeCard(onClick = { navController.navigate("fitbot") })
        }
    }
}

@Composable
private fun GamificationEventCard(
    onClick: () -> Unit
) {
    val bannerShape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1672f / 941f)
            .shadow(
                elevation = 8.dp,
                shape = bannerShape,
                clip = false,
                spotColor = Color(0xFF4059AD).copy(alpha = 0.22f)
            )
            .clip(bannerShape)
            .cardClickableMicroAnimation(onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.gamification3),
            contentDescription = "Event Live banner",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

// ─── Full Lottie Tile (reusable) ─────────────────────────────────────
@Composable
private fun FullLottieTile(
    lottieRes: Int,
    bg: Color,
    accent: Color,
    onClick: () -> Unit,
    overlay: @Composable BoxWithConstraintsScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(CardHeight)
            .cardClickableMicroAnimation(onClick),
        shape = CardRadius,
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compact = maxWidth < 170.dp
            val animSize = if (compact) 140.dp else 170.dp

            HomeCardAccentLine(
                accentColor = accent,
                modifier = Modifier.align(Alignment.CenterStart)
            )

            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(lottieRes))
            val progress by animateLottieCompositionAsState(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                isPlaying = true,
                speed = 1f
            )

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(animSize)
                )
            }

            overlay()
        }
    }
}

// ─── Static Badge ────────────────────────────────────────────────────
@Composable
private fun StaticBadge(
    modifier: Modifier = Modifier,
    size: Int = 32,
    boxBg: Color,
    contentColor: Color
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(boxBg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size((size - 14).dp)
        )
    }
}

// ─── Leaderboard Chip ────────────────────────────────────────────────
@Composable
fun LeaderboardChip(navController: NavController) {
    FullLottieTile(
        lottieRes = R.raw.trophy,
        bg = AccentBlueLight,
        accent = AccentBlue,
        onClick = { navController.navigate("leaderboard") }
    ) {
        val compact = maxWidth < 170.dp

        // Subtle gradient overlay at the top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .align(Alignment.TopStart)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            AccentBlueLight.copy(alpha = 0.95f),
                            Color.Transparent
                        )
                    )
                )
        )

        Text(
            text = "Leaderboard",
            fontSize = HomeCardTitleSize,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 18.dp, top = 16.dp, end = 16.dp)
        )
    }
}

// ─── Community Tease Chip ────────────────────────────────────────────
@Composable
fun CommunityTeaseChip(
    sharesToday: Int,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(CardHeight)
            .cardClickableMicroAnimation(onClick),
        shape = CardRadius,
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
        ) {
            val compact = maxWidth < 170.dp

            HomeCardAccentLine(
                accentColor = CommunityGreen,
                modifier = Modifier.align(Alignment.CenterStart)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 18.dp, end = 14.dp, top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {

                if (compact) {
                    Column(horizontalAlignment = Alignment.Start) {
                        StaticBadge(
                            boxBg = CommunityGreenSoft,
                            contentColor = CommunityGreenDark
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Community Shares",
                            fontSize = HomeCardTitleSize,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StaticBadge(
                            boxBg = CommunityGreenSoft,
                            contentColor = CommunityGreenDark
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Community Shares",
                            fontSize = HomeCardTitleSize,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column {
                    Text(
                        text = "$sharesToday",
                        fontSize = HomeCardValueSize,
                        fontWeight = FontWeight.Bold,
                        color = CommunityGreenDark,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "shared today",
                        fontSize = HomeCardSupportingSize,
                        color = TextSecondary
                    )
                }

                Spacer(Modifier.height(1.dp))
            }
        }
    }
}

// ─── Sparkline Chart ─────────────────────────────────────────────────
@Composable
fun SparklineChart() {
    val values = listOf(1.0f, 1.4f, 1.1f, 1.5f, 1.8f, 1.6f, 2.0f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val maxV = values.max()
        val minV = values.min()
        val step = w / (values.size - 1)

        val points = values.mapIndexed { i, v ->
            val x = i * step
            val y = h - ((v - minV) / (maxV - minV)) * h
            Offset(x, y)
        }

        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (p in points.drop(1)) lineTo(p.x, p.y)
        }

        val fillPath = Path().apply {
            moveTo(points.first().x, h)
            for (p in points) lineTo(p.x, p.y)
            lineTo(points.last().x, h)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                listOf(ActivityPink.copy(alpha = 0.18f), Color.Transparent)
            ),
            style = Fill
        )

        drawPath(
            path = linePath,
            color = ActivityPink,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        points.forEach { p ->
            drawCircle(Color.White, radius = 6.dp.toPx(), center = p)
            drawCircle(ActivityPink, radius = 3.5.dp.toPx(), center = p)
        }
    }
}

// ─── Bottom Navigation Bar ───────────────────────────────────────────
@Composable
fun UniMoveBottomBar(navController: NavController, modifier: Modifier = Modifier) {
    NavigationBar(
        modifier = modifier.navigationBarsPadding(),
        containerColor = CardSurface,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = true,
            onClick = { navController.navigate("home") },
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("Home", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AccentBlue,
                selectedTextColor = AccentBlue,
                indicatorColor = AccentBlueSoft,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            )
        )

        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("run") },
            icon = { Icon(Icons.AutoMirrored.Filled.DirectionsRun, null) },
            label = { Text("Run", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AccentBlue,
                selectedTextColor = AccentBlue,
                indicatorColor = AccentBlueSoft,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            )
        )

        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("challenge") },
            icon = { Icon(Icons.Default.EmojiEvents, null) },
            label = { Text("Challenge", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AccentBlue,
                selectedTextColor = AccentBlue,
                indicatorColor = AccentBlueSoft,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            )
        )

        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("stats") },
            icon = { Icon(Icons.Default.InsertChart, null) },
            label = { Text("Stats", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AccentBlue,
                selectedTextColor = AccentBlue,
                indicatorColor = AccentBlueSoft,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            )
        )
    }
}

// ─── All Time Summary Card ───────────────────────────────────────────
@Composable
private fun AllTimeSummaryCard(
    totalKm: Double,
    totalTimeSec: Int,
    avgPaceSecPerKm: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(22.dp), clip = false),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(SummaryGradientTop, SummaryGradientBot)
                    )
                )
                .border(1.dp, BorderWhite, RoundedCornerShape(22.dp))
                .padding(18.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Icon badge
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        StatBlue.copy(alpha = 0.18f),
                                        StatBlue.copy(alpha = 0.08f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            tint = StatBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "All Time",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary,
                            letterSpacing = (-0.2).sp
                        )
                        Text(
                            "Your lifetime running summary",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Stats row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardSurface.copy(alpha = 0.85f))
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SummaryCell(
                        title = "Distance",
                        value = String.format(Locale.getDefault(), "%.2f", totalKm),
                        unit = "km",
                        modifier = Modifier.weight(1f)
                    )

                    VerticalDivider()

                    SummaryCell(
                        title = "Avg Pace",
                        value = formatPace(avgPaceSecPerKm).replace(" /km", ""),
                        unit = "/km",
                        modifier = Modifier.weight(1f)
                    )

                    VerticalDivider()

                    SummaryCell(
                        title = "Time",
                        value = formatTime(totalTimeSec),
                        unit = "",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ─── Summary Cell ────────────────────────────────────────────────────
@Composable
private fun SummaryCell(
    title: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.padding(horizontal = 8.dp)) {
        val compact = maxWidth < 92.dp
        val valueSize = if (compact) 17.sp else 21.sp
        val unitSize = if (compact) 11.sp else 12.sp

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                title,
                fontSize = 11.sp,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.3.sp
            )
            Spacer(Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value,
                    fontSize = valueSize,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    letterSpacing = (-0.5).sp
                )
                if (unit.isNotBlank()) {
                    Spacer(Modifier.width(3.dp))
                    Text(
                        unit,
                        fontSize = unitSize,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

// ─── Vertical Divider ────────────────────────────────────────────────
@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(DividerColor)
    )
}

private fun formatPace(paceInSecondsPerKm: Float): String {
    if (paceInSecondsPerKm <= 0f) return "--"
    val mins = (paceInSecondsPerKm / 60).toInt()
    val secs = (paceInSecondsPerKm % 60).toInt()
    return String.format(Locale.getDefault(), "%d:%02d /km", mins, secs)
}

private fun formatTime(elapsedSeconds: Int): String {
    val hours = elapsedSeconds / 3600
    val mins = (elapsedSeconds % 3600) / 60
    val secs = elapsedSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, mins, secs)
}

// ─── Exercise Record Card ────────────────────────────────────────────
@Composable
fun ExerciseRecordCard(
    totalKm: Double,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(CardHeight)
            .cardClickableMicroAnimation(onClick),
        shape = CardRadius,
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            HomeCardAccentLine(
                accentColor = ActivityPink,
                modifier = Modifier.align(Alignment.CenterStart)
            )

            Column(
                modifier = Modifier.padding(
                    start = 18.dp,
                    end = 14.dp,
                    top = 16.dp,
                    bottom = 14.dp
                )
            ) {
                Text(
                    "Activity",
                    fontSize = HomeCardTitleSize,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    String.format(Locale.getDefault(), "%.2f km", totalKm),
                    fontSize = HomeCardValueSize,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = (-0.5).sp
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    SparklineChart()
                }
            }
        }
    }
}

// ─── Goals Grid Card ─────────────────────────────────────────────────
@Composable
fun GoalsGridCard(
    achievedKm: Double,
    goalKm: Double?,
    onClick: () -> Unit
) {
    val validGoal = goalKm?.takeIf { it > 0.0 }
    val remainingKm = validGoal?.let { (it - achievedKm).coerceAtLeast(0.0) }
    val progress = validGoal?.let { (achievedKm / it).coerceIn(0.0, 1.0).toFloat() } ?: 0f
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(CardHeight)
            .cardClickableMicroAnimation(onClick),
        shape = CardRadius,
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            HomeCardAccentLine(
                accentColor = GoalsIndigo,
                modifier = Modifier.align(Alignment.CenterStart)
            )

            Column(
                modifier = Modifier
                    .padding(start = 18.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        GoalsIndigo.copy(alpha = 0.15f),
                                        GoalsIndigo.copy(alpha = 0.06f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Flag,
                            contentDescription = null,
                            tint = GoalsIndigo,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Distance Goal",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = HomeCardTitleSize,
                        color = TextPrimary
                    )
                }

                Column {
                    Text(
                        when {
                            validGoal == null -> "Set goal"
                            remainingKm == 0.0 -> "Achieved"
                            else -> String.format(Locale.getDefault(), "%.1f km", remainingKm)
                        },
                        fontSize = HomeCardValueSize,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        if (validGoal == null) {
                            "Set it in Profile"
                        } else {
                            String.format(Locale.getDefault(), "%.1f of %.1f km", achievedKm, validGoal)
                        },
                        fontSize = HomeCardSupportingSize,
                        color = TextSecondary
                    )
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(7.dp)),
                    color = GoalsIndigo,
                    trackColor = GoalsIndigoSoft
                )
            }
        }
    }
}

// ─── Calorie Tracker Card ────────────────────────────────────────────
@Composable
fun CalorieTrackerCard(
    todayCalories: Int = 0,
    goalCalories: Int? = null,
    onClick: () -> Unit = {},
    onSetTarget: () -> Unit = {}
) {
    val target = goalCalories ?: 2000
    val progressValue = (todayCalories.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    val calorieText = remember(todayCalories) { formatCaloriesForHome(todayCalories) }

    // Colors matching the requested design exactly
    val navyText = Color(0xFF11142D)
    val greyText = Color(0xFF92929D)
    val coralOrange = Color(0xFFFF7043)
    val lightPeach = Color(0xFFFFF2EC)
    val trackColor = Color(0xFFE8EAF6)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(CardHeight)
            .cardClickableMicroAnimation(onClick),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val width = maxWidth
            val isSmall = width < 165.dp
            val isTiny = width < 140.dp

            val outerPadding = when {
                isTiny -> 10.dp
                isSmall -> 12.dp
                else -> 12.dp
            }
            val iconBoxSize = when {
                isTiny -> 28.dp
                isSmall -> 32.dp
                else -> 36.dp
            }
            val iconSize = when {
                isTiny -> 15.dp
                isSmall -> 18.dp
                else -> 20.dp
            }
            val titleSize = when {
                isTiny -> 13.sp
                isSmall -> 15.sp
                else -> 18.sp
            }
            val labelSize = if (isSmall) 11.sp else 12.sp
            val valueMaxSize = when {
                isTiny -> 22.sp
                isSmall -> 26.sp
                width < 190.dp -> 28.sp
                else -> 30.sp
            }
            val valueMinSize = if (isTiny) 16.sp else 20.sp
            val targetSize = when {
                isTiny -> 10.sp
                isSmall -> 11.sp
                else -> 11.sp
            }
            val progressHeight = when {
                isTiny -> 6.dp
                isSmall -> 7.dp
                else -> 7.dp
            }
            val contentGap = if (isSmall) 2.dp else 3.dp
            val targetText = if (isTiny) {
                "$todayCalories / $target kcal"
            } else {
                "$todayCalories / $target kcal target"
            }

            Column(
                modifier = Modifier
                    .padding(outerPadding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Row: Icon + Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(iconBoxSize)
                            .clip(RoundedCornerShape(12.dp))
                            .background(lightPeach),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = coralOrange,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                    Spacer(Modifier.width(if (isSmall) 6.dp else 12.dp))
                    AutoFitSingleLineText(
                        text = "Calories",
                        maxFontSize = titleSize,
                        minFontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = navyText,
                        modifier = Modifier.weight(1f)
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(contentGap)
                ) {
                    Text(
                        text = "intake",
                        fontSize = labelSize,
                        lineHeight = labelSize,
                        color = greyText,
                        fontWeight = FontWeight.Medium
                    )
                    AutoFitSingleLineText(
                        text = calorieText,
                        maxFontSize = valueMaxSize,
                        minFontSize = valueMinSize,
                        fontWeight = FontWeight.ExtraBold,
                        color = navyText,
                        letterSpacing = if (isSmall) (-1).sp else (-1.5).sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = targetText,
                        fontSize = targetSize,
                        lineHeight = targetSize,
                        color = greyText,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Bottom: Progress Bar
                LinearProgressIndicator(
                    progress = { progressValue },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(progressHeight)
                        .clip(CircleShape),
                    color = coralOrange,
                    trackColor = trackColor,
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

// ─── Weight Management Card ──────────────────────────────────────────
@Composable
fun WeightManagementCard(
    currentWeightKg: Float = 0f,
    targetWeightKg: Float = 0f,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(CardHeight)
            .cardClickableMicroAnimation(onClick),
        shape = CardRadius,
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            HomeCardAccentLine(
                accentColor = WeightIndigo,
                modifier = Modifier.align(Alignment.CenterStart)
            )

            Column(
                modifier = Modifier
                    .padding(start = 18.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(WeightIndigoSoft, WeightIndigoSoft.copy(alpha = 0.5f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonitorWeight,
                            contentDescription = null,
                            tint = WeightIndigo,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Weight",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = HomeCardTitleSize,
                        color = TextPrimary
                    )
                }

                Column {
                    Text(
                        if (currentWeightKg > 0f) String.format(Locale.getDefault(), "%.1f kg", currentWeightKg) else "-- kg",
                        fontSize = HomeCardValueSize,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        if (targetWeightKg > 0f) String.format(Locale.getDefault(), "Target %.1f kg", targetWeightKg) else "Set your target weight",
                        fontSize = HomeCardSupportingSize,
                        color = TextSecondary
                    )
                }

                // Track weekly row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceLight)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Track weekly",
                        fontSize = HomeCardSupportingSize,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ─── Weather Card ────────────────────────────────────────────────────
@Composable
private fun WeatherCardContent(
    weatherViewModel: WeatherViewModel,
    locationLabelOverride: String?
) {
    val weatherState by weatherViewModel.uiState.collectAsStateWithLifecycle()
    val fontScale = LocalDensity.current.fontScale
    val compactMode = fontScale > 1.05f || LocalConfiguration.current.screenWidthDp <= 390

    when (val state = weatherState) {
        is WeatherState.Loading -> {
            val infiniteTransition = rememberInfiniteTransition(label = "weather_shimmer")
            val startOffsetX by infiniteTransition.animateFloat(
                initialValue = -1000f,
                targetValue = 1000f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "shimmer_anim"
            )
            val shimmerBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.15f),
                    Color.White.copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.15f)
                ),
                start = Offset(startOffsetX, 0f),
                end = Offset(startOffsetX + 400f, 400f)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF62C7FF), Color(0xFF3768DA))
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = if (compactMode) 14.dp else 16.dp,
                            vertical = if (compactMode) 10.dp else 12.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(if (compactMode) 8.dp else 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            // Location skeleton
                            Box(modifier = Modifier.width(120.dp).height(16.dp).clip(RoundedCornerShape(8.dp)).background(shimmerBrush))
                            Spacer(Modifier.height(8.dp))
                            // Temp skeleton
                            Box(modifier = Modifier.width(80.dp).height(38.dp).clip(RoundedCornerShape(8.dp)).background(shimmerBrush))
                            Spacer(Modifier.height(6.dp))
                            // Description skeleton
                            Box(modifier = Modifier.width(100.dp).height(14.dp).clip(RoundedCornerShape(8.dp)).background(shimmerBrush))
                        }
                        // Weather Icon skeleton
                        Box(modifier = Modifier.size(if (compactMode) 42.dp else 46.dp).clip(CircleShape).background(shimmerBrush))
                    }

                    Spacer(Modifier.height(if (compactMode) 6.dp else 8.dp))

                    // Pills skeleton
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(if (compactMode) 6.dp else 8.dp)
                    ) {
                        repeat(4) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(if (compactMode) 38.dp else 42.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(shimmerBrush)
                            )
                        }
                    }

                    Spacer(Modifier.height(if (compactMode) 6.dp else 8.dp))

                    // Advice Box skeleton
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(shimmerBrush)
                    )
                }
            }
        }

        is WeatherState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1A264D), Color(0xFF334B93))
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (state.message.contains("missing", ignoreCase = true)) {
                            "Weather API key missing."
                        } else {
                            "Weather is unavailable right now."
                        },
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        is WeatherState.Success -> {
            val current = state.current
            val isNight = isNightNow(current.sys.sunrise, current.sys.sunset)
            val weatherMain = current.weather.firstOrNull()?.main.orEmpty()
            val weatherDescription = current.weather.firstOrNull()?.description
                ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                ?: "Current conditions"
            val precipitationChance = state.forecast.list.take(3).maxOfOrNull { it.pop } ?: 0f
            val insight = buildWeatherRunInsight(
                weatherMain = weatherMain,
                weatherDescription = weatherDescription,
                temp = current.main.temp,
                feelsLike = current.main.feels_like,
                humidity = current.main.humidity,
                windSpeed = current.wind.speed,
                precipitationChance = precipitationChance,
                isNight = isNight
            )
            val forecastChips = buildWeatherForecastChips(
                items = state.forecast.list,
                timezoneSeconds = current.timezone
            )
            var detailsExpanded by remember { mutableStateOf(false) }
            val insightAccent = when (insight.decision) {
                WeatherRunDecision.Go -> Color(0xFF97F7C8)
                WeatherRunDecision.Caution -> Color(0xFFFFDB86)
                WeatherRunDecision.Indoor -> Color(0xFFFFB7B0)
            }
            val gradient = if (isNight) {
                Brush.verticalGradient(listOf(Color(0xFF0D1833), Color(0xFF1F2F64)))
            } else {
                Brush.verticalGradient(listOf(Color(0xFF62C7FF), Color(0xFF3768DA)))
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(gradient)
            ) {
                AnimatedWeatherBackdrop(
                    modifier = Modifier.matchParentSize(),
                    isNight = isNight
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .animateContentSize()
                        .padding(
                            horizontal = if (compactMode) 12.dp else 14.dp,
                            vertical = if (compactMode) 8.dp else 10.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(if (compactMode) 6.dp else 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = locationLabelOverride ?: buildLocationLabel(current.name, current.sys.country),
                                    color = Color.White,
                                    fontSize = if (compactMode) 13.sp else 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(Modifier.height(if (compactMode) 4.dp else 6.dp))
                            WeatherDecisionBadge(
                                insight = insight,
                                compactMode = compactMode
                            )
                            Spacer(Modifier.height(if (compactMode) 6.dp else 8.dp))
                            Text(
                                text = insight.headline,
                                color = Color.White,
                                fontSize = if (compactMode) 18.sp else 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${current.main.temp.toInt()}°",
                                color = Color.White,
                                fontSize = if (compactMode) 34.sp else 38.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.8).sp
                            )
                            Text(
                                text = weatherDescription,
                                color = Color.White.copy(alpha = 0.95f),
                                fontSize = if (compactMode) 12.sp else 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        AnimatedWeatherConditionIcon(
                            weatherMain = weatherMain,
                            isNight = isNight,
                            compactMode = compactMode,
                            modifier = Modifier.size(if (compactMode) 42.dp else 46.dp)
                        )
                    }

                    Spacer(Modifier.height(if (compactMode) 4.dp else 6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(if (compactMode) 6.dp else 8.dp)
                    ) {
                        WeatherMetricPill(
                            label = "Feels Like",
                            value = "${current.main.feels_like.toInt()}°",
                            modifier = Modifier.weight(1f),
                            compactMode = compactMode
                        )
                        WeatherMetricPill(
                            label = "Rain Risk",
                            value = "${(precipitationChance * 100f).roundToInt()}%",
                            modifier = Modifier.weight(1f),
                            compactMode = compactMode
                        )
                        WeatherMetricPill(
                            label = "Humidity",
                            value = "${current.main.humidity}%",
                            modifier = Modifier.weight(1f),
                            compactMode = compactMode
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { detailsExpanded = !detailsExpanded }
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (detailsExpanded) "Less weather detail" else "More weather detail",
                            color = Color.White.copy(alpha = 0.82f),
                            fontSize = if (compactMode) 11.sp else 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Surface(
                            color = Color.White.copy(alpha = 0.12f),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = if (detailsExpanded) {
                                    Icons.Default.KeyboardArrowUp
                                } else {
                                    Icons.Default.KeyboardArrowDown
                                },
                                contentDescription = if (detailsExpanded) "Hide details" else "Show details",
                                tint = Color.White,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }

                    if (detailsExpanded) {
                        Text(
                            text = insight.reason,
                            color = Color.White.copy(alpha = 0.86f),
                            fontSize = if (compactMode) 11.sp else 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = insight.action,
                            color = insightAccent,
                            fontSize = if (compactMode) 11.sp else 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (detailsExpanded && forecastChips.isNotEmpty()) {
                        Surface(
                            color = Color.White.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(18.dp),
                            tonalElevation = 0.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Next 3 hours",
                                        color = Color.White,
                                        fontSize = if (compactMode) 11.sp else 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Wait if rain climbs",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = if (compactMode) 10.sp else 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(if (compactMode) 6.dp else 8.dp)
                                ) {
                                    forecastChips.forEach { chip ->
                                        WeatherForecastChip(
                                            chip = chip,
                                            modifier = Modifier.weight(1f),
                                            compactMode = compactMode
                                        )
                                    }
                                }
                            }
                        }
                    }


                }
            }
        }
    }
}

@Composable
fun WeatherCard(
    weatherViewModel: WeatherViewModel,
    locationLabelOverride: String? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 184.dp)
            .shadow(14.dp, RoundedCornerShape(24.dp), spotColor = Color(0xFF243C78).copy(alpha = 0.32f)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        WeatherCardContent(
            weatherViewModel = weatherViewModel,
            locationLabelOverride = locationLabelOverride
        )
    }
}

// ─── FitBot Home Card ──────────────────────────────────────────────
@Composable
fun FitBotHomeCard(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "fitbot_card_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "fitbot_card")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(14.dp, RoundedCornerShape(24.dp), spotColor = Color(0xFF2563EB).copy(alpha = 0.4f))
            .clip(RoundedCornerShape(24.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1E3A8A), // Deep Blue
                            Color(0xFF2563EB), // Vibrant Blue
                            Color(0xFF0EA5E9)  // Bright Cyan
                        )
                    )
                )
        ) {
            val isCompact = maxWidth < 180.dp

            // Moving Shimmer overlay for a "Living" GenUI feel
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { translationX = shimmerOffset }
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (isCompact) 14.dp else 20.dp),
                verticalArrangement = Arrangement.spacedBy(if (isCompact) 12.dp else 16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (isCompact) 10.dp else 14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Glassmorphic Bot Avatar
                        Box(
                            modifier = Modifier
                                .size(if (isCompact) 42.dp else 48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(if (isCompact) 20.dp else 24.dp)
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "FitBot",
                                    color = Color.White,
                                    fontSize = if (isCompact) 15.sp else 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp
                                )
                                // Subtle status dot instead of loud "LIVE" badge
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF38BDF8).copy(alpha = pulseAlpha))
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Powered by Groq",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = if (isCompact) 10.sp else 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Floating CTA
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                            .padding(horizontal = if (isCompact) 10.dp else 12.dp, vertical = if (isCompact) 6.dp else 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Ask",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Generative Input Box Simulation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = if (isCompact) 10.dp else 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isCompact) "Ask anything..." else "Try: \"Create a 5K plan...\"",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = if (isCompact) 11.sp else 12.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

