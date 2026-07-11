package com.example.unimove.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.unimove.ui.viewmodel.RunnerStage
import com.example.unimove.ui.viewmodel.getLottieResource
import com.example.unimove.ui.viewmodel.getLottieSpeed
import com.example.unimove.ui.viewmodel.runnerStageFromTotalKm
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementScreen(navController: NavController) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val db = remember { FirebaseFirestore.getInstance() }

    var totalDistance by remember { mutableDoubleStateOf(0.0) }

    DisposableEffect(uid) {
        if (uid == null) {
            onDispose { }
        } else {
            val registration = db.collection("users").document(uid)
                .addSnapshotListener { snap, _ ->
                    if (snap != null && snap.exists()) {
                        totalDistance = snap.getDouble("totalDistance") ?: 0.0
                    }
                }
            onDispose { registration.remove() }
        }
    }

    val stage = remember(totalDistance) { runnerStageFromTotalKm(totalDistance) }
    val palette = remember(stage) { achievementPalette(stage) }
    val milestones = remember {
        listOf(
            Milestone("1 km", "First Steps", 1.0, Icons.AutoMirrored.Rounded.DirectionsWalk),
            Milestone("5 km", "5K Finisher", 5.0, Icons.Rounded.Timer),
            Milestone("10 km", "Active Runner", 10.0, Icons.Rounded.Bolt),
            Milestone("20 km", "Half Pro", 20.0, Icons.Rounded.MilitaryTech),
            Milestone("50 km", "Distance Master", 50.0, Icons.Rounded.EmojiEvents),
            Milestone("100 km", "Legendary", 100.0, Icons.Rounded.Stars)
        )
    }
    val unlockedCount = remember(totalDistance, milestones) {
        milestones.count { totalDistance >= it.distance }
    }
    val nextMilestone = remember(totalDistance, milestones) {
        milestones.firstOrNull { totalDistance < it.distance }
    }
    val previousDistance = remember(totalDistance, milestones) {
        milestones.lastOrNull { totalDistance >= it.distance }?.distance ?: 0.0
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(palette.background, Color.White)
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = { Text("Runner Achievements", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    AchievementHero(
                        stage = stage,
                        totalDistance = totalDistance,
                        unlockedCount = unlockedCount,
                        totalBadges = milestones.size,
                        palette = palette
                    )
                }

                item {
                    LevelStatusCard(
                        stage = stage,
                        totalDistance = totalDistance,
                        nextMilestone = nextMilestone,
                        previousDistance = previousDistance,
                        palette = palette
                    )
                }

                item {
                    Text(
                        text = "Badges",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                item {
                    BadgeGrid(
                        milestones = milestones,
                        totalDistance = totalDistance,
                        nextMilestone = nextMilestone,
                        palette = palette
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementHero(
    stage: RunnerStage,
    totalDistance: Double,
    unlockedCount: Int,
    totalBadges: Int,
    palette: AchievementPalette
) {
    val floatTransition = rememberInfiniteTransition(label = "heroFloat")
    val heroScale by floatTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heroScale"
    )

    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(getLottieResource(stage))
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        speed = getLottieSpeed(stage)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(palette.primary, palette.secondary)
                    )
                )
                .padding(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.10f))
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "Current Level",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stageTitle(stage),
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "${"%.1f".format(totalDistance)} km total",
                            color = Color.White.copy(alpha = 0.88f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    Box(
                        modifier = Modifier
                            .scale(heroScale)
                            .size(108.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        LottieAnimation(
                            composition = composition,
                            progress = { progress },
                            modifier = Modifier.size(92.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeroStatChip(
                        title = "Badges",
                        value = "$unlockedCount / $totalBadges",
                        modifier = Modifier.weight(1f)
                    )
                    HeroStatChip(
                        title = "Tier",
                        value = stageShortLabel(stage),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelStatusCard(
    stage: RunnerStage,
    totalDistance: Double,
    nextMilestone: Milestone?,
    previousDistance: Double,
    palette: AchievementPalette
) {
    val rawProgress = when {
        nextMilestone == null -> 1f
        nextMilestone.distance <= previousDistance -> 1f
        else -> ((totalDistance - previousDistance) / (nextMilestone.distance - previousDistance))
            .coerceIn(0.0, 1.0)
            .toFloat()
    }
    val progress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "levelProgress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
        border = BorderStroke(1.dp, palette.border)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Level Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stageShortLabel(stage),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.primary
                    )
                }

                Surface(
                    color = palette.tint,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = if (nextMilestone == null) "Maxed" else "Next: ${nextMilestone.label}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.primary
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color = palette.primary,
                trackColor = palette.tint
            )

            Text(
                text = if (nextMilestone == null) {
                    "All visible badges unlocked"
                } else {
                    "${"%.1f".format((nextMilestone.distance - totalDistance).coerceAtLeast(0.0))} km left for ${nextMilestone.title}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BadgeGrid(
    milestones: List<Milestone>,
    totalDistance: Double,
    nextMilestone: Milestone?,
    palette: AchievementPalette
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (i in milestones.indices step 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SimpleBadgeCard(
                    milestone = milestones[i],
                    unlocked = totalDistance >= milestones[i].distance,
                    currentTarget = nextMilestone?.distance == milestones[i].distance,
                    palette = palette,
                    modifier = Modifier.weight(1f)
                )

                if (i + 1 < milestones.size) {
                    SimpleBadgeCard(
                        milestone = milestones[i + 1],
                        unlocked = totalDistance >= milestones[i + 1].distance,
                        currentTarget = nextMilestone?.distance == milestones[i + 1].distance,
                        palette = palette,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SimpleBadgeCard(
    milestone: Milestone,
    unlocked: Boolean,
    currentTarget: Boolean,
    palette: AchievementPalette,
    modifier: Modifier = Modifier
) {
    val pulseTransition = rememberInfiniteTransition(label = "badgeTarget")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = if (currentTarget) 1f else 1f,
        targetValue = if (currentTarget) 1.03f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badgeTargetScale"
    )

    val container = when {
        unlocked -> palette.tint
        currentTarget -> palette.primary.copy(alpha = 0.08f)
        else -> Color(0xFFF5F3F0)
    }
    val borderColor = when {
        unlocked -> palette.primary.copy(alpha = 0.22f)
        currentTarget -> palette.primary.copy(alpha = 0.20f)
        else -> Color(0xFFE7E0D8)
    }
    val iconTint = when {
        unlocked -> palette.primary
        currentTarget -> palette.primary
        else -> Color(0xFF9E9A94)
    }

    Card(
        modifier = modifier.scale(pulseScale),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (unlocked || currentTarget) {
                            iconTint.copy(alpha = 0.14f)
                        } else {
                            Color.White.copy(alpha = 0.9f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (unlocked) milestone.icon else Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = iconTint
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = milestone.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = milestone.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = iconTint
                )
            }

            Surface(
                color = Color.White.copy(alpha = 0.72f),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    text = when {
                        unlocked -> "Unlocked"
                        currentTarget -> "In Progress"
                        else -> "Locked"
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = iconTint
                )
            }
        }
    }
}

@Composable
private fun HeroStatChip(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.12f),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.72f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

data class Milestone(
    val label: String,
    val title: String,
    val distance: Double,
    val icon: ImageVector,
    val note: String = ""
)

private data class AchievementPalette(
    val primary: Color,
    val secondary: Color,
    val tint: Color,
    val background: Color,
    val border: Color
)

private fun achievementPalette(stage: RunnerStage): AchievementPalette {
    return when (stage) {
        RunnerStage.KID -> AchievementPalette(
            primary = Color(0xFF1663C7),
            secondary = Color(0xFF52B3F1),
            tint = Color(0xFFEAF5FF),
            background = Color(0xFFF5FAFF),
            border = Color(0xFFD7E8F8)
        )
        RunnerStage.TEEN -> AchievementPalette(
            primary = Color(0xFF137A59),
            secondary = Color(0xFF4BC08B),
            tint = Color(0xFFECFBF3),
            background = Color(0xFFF5FCF7),
            border = Color(0xFFD8EEE0)
        )
        RunnerStage.ADULT -> AchievementPalette(
            primary = Color(0xFF9A5317),
            secondary = Color(0xFFF0A33E),
            tint = Color(0xFFFFF4E7),
            background = Color(0xFFFFFAF4),
            border = Color(0xFFF1E0C8)
        )
    }
}

private fun stageTitle(stage: RunnerStage): String {
    return when (stage) {
        RunnerStage.KID -> "Rookie Runner"
        RunnerStage.TEEN -> "Pro Challenger"
        RunnerStage.ADULT -> "Elite Master"
    }
}

private fun stageShortLabel(stage: RunnerStage): String {
    return when (stage) {
        RunnerStage.KID -> "Rookie"
        RunnerStage.TEEN -> "Pro"
        RunnerStage.ADULT -> "Elite"
    }
}
