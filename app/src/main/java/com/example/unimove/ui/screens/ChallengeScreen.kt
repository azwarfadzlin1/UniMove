package com.example.unimove.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.unimove.data.model.Challenge
import com.example.unimove.data.model.UserChallengeProgress
import com.example.unimove.ui.viewmodel.ChallengeViewModel
import com.example.unimove.ui.viewmodel.RunViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

private enum class ChallengeLayoutMode {
    Compact,
    Medium,
    Expanded
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeScreen(
    navController: NavController,
    viewModel: ChallengeViewModel = viewModel(),
    runViewModel: RunViewModel
) {
    val challenges by viewModel.activeChallenges.collectAsStateWithLifecycle()
    val userProgress by viewModel.userProgress.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0C1224),
                        Color(0xFF17305C),
                        Color(0xFF2B7FFF)
                    )
                )
            )
    ) {
        val layoutMode = when {
            maxWidth < 360.dp -> ChallengeLayoutMode.Compact
            maxWidth < 420.dp -> ChallengeLayoutMode.Medium
            else -> ChallengeLayoutMode.Expanded
        }
        val horizontalPadding = when (layoutMode) {
            ChallengeLayoutMode.Compact -> 12.dp
            ChallengeLayoutMode.Medium -> 16.dp
            ChallengeLayoutMode.Expanded -> 20.dp
        }
        val contentMaxWidth = when (layoutMode) {
            ChallengeLayoutMode.Compact -> 420.dp
            ChallengeLayoutMode.Medium -> 520.dp
            ChallengeLayoutMode.Expanded -> 640.dp
        }

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Challenges",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            }
        ) { innerPadding ->
            if (challenges.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = horizontalPadding),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyChallengeState(
                        layoutMode = layoutMode,
                        maxContentWidth = contentMaxWidth
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(
                        start = horizontalPadding,
                        end = horizontalPadding,
                        top = 10.dp,
                        bottom = 28.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            ChallengeHeroHeader(
                                challengeCount = challenges.size,
                                layoutMode = layoutMode,
                                maxContentWidth = contentMaxWidth
                            )
                        }
                    }

                    items(challenges) { challenge ->
                        val progress = userProgress[challenge.id]
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            ChallengeArenaCard(
                                challenge = challenge,
                                progress = progress,
                                layoutMode = layoutMode,
                                maxContentWidth = contentMaxWidth,
                                onLaunch = {
                                    runViewModel.primeActiveChallenge(challenge)
                                    runViewModel.clearPendingChallengeResult()
                                    viewModel.prepareChallenge(
                                        challenge = challenge,
                                        onReady = {
                                            val encodedTitle = Uri.encode(challenge.title)
                                            navController.navigate(
                                                "run?challengeId=${challenge.id}&challengeTitle=$encodedTitle&targetDistanceKm=${challenge.targetDistanceKm}"
                                            )
                                        },
                                        onError = { message ->
                                            scope.launch { snackbarHostState.showSnackbar(message) }
                                        }
                                    )
                                },
                                onViewResults = {
                                    runViewModel.loadChallengeStandings(
                                        challenge.id,
                                        challenge.title,
                                        challenge.targetDistanceKm
                                    )
                                    navController.navigate("challenge_result/${challenge.id}?viewOnly=true")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChallengeHeroHeader(
    challengeCount: Int,
    layoutMode: ChallengeLayoutMode,
    maxContentWidth: Dp
) {
    val titleSize = when (layoutMode) {
        ChallengeLayoutMode.Compact -> 24.sp
        ChallengeLayoutMode.Medium -> 26.sp
        ChallengeLayoutMode.Expanded -> 28.sp
    }
    val contentPadding = when (layoutMode) {
        ChallengeLayoutMode.Compact -> 16.dp
        ChallengeLayoutMode.Medium -> 18.dp
        ChallengeLayoutMode.Expanded -> 20.dp
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = maxContentWidth),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.13f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFFFC83D).copy(alpha = 0.2f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color(0xFFFFE28A),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$challengeCount live",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Ready to race",
                color = Color.White,
                fontSize = titleSize,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Pick a challenge, run the distance, climb the board.",
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ChallengeArenaCard(
    challenge: Challenge,
    progress: UserChallengeProgress?,
    layoutMode: ChallengeLayoutMode,
    maxContentWidth: Dp,
    onLaunch: () -> Unit,
    onViewResults: () -> Unit
) {
    val hasResult = progress?.bestCompletionSeconds != null
    val statusText = when {
        hasResult -> "Completed"
        progress?.status?.name == "ACTIVE" -> "In progress"
        else -> "New"
    }
    val buttonText = if (hasResult) "Retry" else "Start"
    val descriptionText = challenge.description.trim().ifBlank { "Distance challenge" }
    val detailText = if (hasResult) {
        "Best ${formatChallengeClock(progress?.bestCompletionSeconds ?: 0L)}"
    } else {
        "Finish distance to rank"
    }
    val cardPadding = when (layoutMode) {
        ChallengeLayoutMode.Compact -> 14.dp
        ChallengeLayoutMode.Medium -> 16.dp
        ChallengeLayoutMode.Expanded -> 18.dp
    }
    val titleSize = when (layoutMode) {
        ChallengeLayoutMode.Compact -> 18.sp
        ChallengeLayoutMode.Medium -> 19.sp
        ChallengeLayoutMode.Expanded -> 20.sp
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = maxContentWidth)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onLaunch),
        shape = RoundedCornerShape(24.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFFFFF), Color(0xFFF4F8FF))
                    )
                )
                .padding(cardPadding)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFEDF4FF)) {
                    Box(
                        modifier = Modifier
                            .size(if (layoutMode == ChallengeLayoutMode.Compact) 50.dp else 54.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF2E7CF6), Color(0xFF50C0FF))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Surface(shape = CircleShape, color = Color(0xFF111827)) {
                        Text(
                            text = challenge.badgeText.ifBlank { "Race" },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = challenge.title,
                        color = Color(0xFF101828),
                        fontWeight = FontWeight.Bold,
                        fontSize = titleSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = descriptionText,
                        color = Color(0xFF667085),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onViewResults,
                    modifier = Modifier
                        .background(Color(0xFFE2E8F0), CircleShape)
                        .size(if (layoutMode == ChallengeLayoutMode.Compact) 38.dp else 42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Leaderboard,
                        contentDescription = "View rankings",
                        tint = Color(0xFF2E7CF6),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            ChallengeStatsBlock(
                layoutMode = layoutMode,
                courseText = "${formatDistanceValue(challenge.targetDistanceKm)} km",
                bestTimeText = progress?.bestCompletionSeconds?.let(::formatChallengeClock) ?: "--:--",
                windowText = challenge.endDate?.let(::formatChallengeEndDate) ?: "Open"
            )

            Spacer(modifier = Modifier.height(14.dp))

            ChallengeActionBar(
                layoutMode = layoutMode,
                statusText = statusText,
                detailText = detailText,
                buttonText = buttonText,
                onLaunch = onLaunch
            )
        }
    }
}

@Composable
private fun ChallengeActionBar(
    layoutMode: ChallengeLayoutMode,
    statusText: String,
    detailText: String,
    buttonText: String,
    onLaunch: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0F172A)
    ) {
        if (layoutMode == ChallengeLayoutMode.Compact) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(
                    text = statusText,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = detailText,
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onLaunch,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7CF6),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(buttonText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = statusText,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = detailText,
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = onLaunch,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7CF6),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(buttonText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChallengeStatsBlock(
    layoutMode: ChallengeLayoutMode,
    courseText: String,
    bestTimeText: String,
    windowText: String
) {
    if (layoutMode == ChallengeLayoutMode.Compact) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ArenaStatPill(
                    modifier = Modifier.weight(1f),
                    label = "Course",
                    value = courseText,
                    icon = Icons.Default.Terrain
                )
                ArenaStatPill(
                    modifier = Modifier.weight(1f),
                    label = "Best",
                    value = bestTimeText,
                    icon = Icons.Default.Timer
                )
            }
            ArenaStatPill(
                modifier = Modifier.fillMaxWidth(),
                label = "Window",
                value = windowText,
                icon = Icons.Default.Bolt
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ArenaStatPill(
                modifier = Modifier.weight(1f),
                label = "Course",
                value = courseText,
                icon = Icons.Default.Terrain
            )
            ArenaStatPill(
                modifier = Modifier.weight(1f),
                label = "Best",
                value = bestTimeText,
                icon = Icons.Default.Timer
            )
            ArenaStatPill(
                modifier = Modifier.weight(1f),
                label = "Window",
                value = windowText,
                icon = Icons.Default.Bolt
            )
        }
    }
}

@Composable
private fun ArenaStatPill(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF8FAFC)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                color = Color(0xFF667085),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF2E7CF6),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = value,
                    color = Color(0xFF111827),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyChallengeState(
    layoutMode: ChallengeLayoutMode,
    maxContentWidth: Dp
) {
    val titleSize = if (layoutMode == ChallengeLayoutMode.Compact) 20.sp else 22.sp

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = maxContentWidth),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.12f)) {
                Box(
                    modifier = Modifier.size(68.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "No live challenge",
                color = Color.White,
                fontSize = titleSize,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "New races will appear here.",
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 14.sp
            )
        }
    }
}

private fun formatChallengeClock(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

private fun formatDistanceValue(distanceKm: Double): String {
    return if (distanceKm % 1.0 == 0.0) {
        distanceKm.toInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", distanceKm)
    }
}

private fun formatChallengeEndDate(timestamp: com.google.firebase.Timestamp): String {
    val formatter = SimpleDateFormat("dd MMM", Locale.getDefault())
    return formatter.format(timestamp.toDate())
}
