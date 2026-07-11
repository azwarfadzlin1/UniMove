package com.example.unimove.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.unimove.data.model.ChallengeStandingEntry
import com.example.unimove.ui.viewmodel.RunViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeResultScreen(
    navController: NavController,
    viewModel: RunViewModel,
    challengeId: String,
    viewOnly: Boolean = false
) {
    val result by viewModel.pendingChallengeResult.collectAsStateWithLifecycle()

    LaunchedEffect(result, challengeId) {
        if (!viewOnly && (result == null || result?.challengeId != challengeId)) {
            navController.navigate("home") {
                popUpTo(0)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearPendingChallengeResult()
        }
    }

    val challengeResult = result?.takeIf { it.challengeId == challengeId }
    if (challengeResult == null) {
        if (viewOnly) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF07111F)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF07111F),
                        Color(0xFF17305C),
                        Color(0xFF2563EB)
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .systemBarsPadding(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (!navController.navigateUp()) {
                                    navController.navigate("home") {
                                        popUpTo(0)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Text(
                            text = if (viewOnly) "Challenge standings" else "Challenge result",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                item {
                    ResultHero(
                        title = challengeResult.challengeTitle,
                        rank = challengeResult.rank,
                        totalParticipants = challengeResult.totalParticipants,
                        finishSeconds = challengeResult.finishSeconds,
                        improvedPersonalBest = challengeResult.improvedPersonalBest,
                        viewOnly = viewOnly
                    )
                }

                item {
                    ResultStats(
                        finishSeconds = challengeResult.finishSeconds,
                        personalBestSeconds = challengeResult.personalBestSeconds,
                        targetDistanceKm = challengeResult.targetDistanceKm,
                        viewOnly = viewOnly
                    )
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = Color.White
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Current standings",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111827)
                            )
                            Text(
                                text = "Fastest completion time wins the challenge.",
                                fontSize = 13.sp,
                                color = Color(0xFF6B7280)
                            )

                            if (challengeResult.topStandings.isEmpty()) {
                                Text(
                                    text = "You are the first finisher. The board will update as more runners complete the course.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF111827)
                                )
                            } else {
                                challengeResult.topStandings.forEachIndexed { index, entry ->
                                    StandingRow(
                                        position = index + 1,
                                        entry = entry,
                                        highlight = index + 1 == challengeResult.rank
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF111827)
                            ),
                            onClick = {
                                navController.navigate("challenge") {
                                    popUpTo("home") { inclusive = false }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timeline,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Back to Challenges", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF14315C),
                                contentColor = Color.White
                            ),
                            onClick = {
                                navController.navigate("home") {
                                    popUpTo(0)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Return Home", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultHero(
    title: String,
    rank: Int,
    totalParticipants: Int,
    finishSeconds: Int,
    improvedPersonalBest: Boolean,
    viewOnly: Boolean
) {
    Surface(
        shape = RoundedCornerShape(30.dp),
        color = Color.White.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFFFC83D).copy(alpha = 0.18f)
            ) {
                Box(
                    modifier = Modifier.size(84.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (viewOnly) Icons.Default.Leaderboard else Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFFFE28A),
                        modifier = Modifier.size(42.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = if (viewOnly) "Challenge Standings" else "Challenge cleared",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            
            if (!viewOnly || finishSeconds > 0) {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = formatChallengeResultTime(finishSeconds.toLong()),
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black
                )
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (rank > 0 && totalParticipants > 0) {
                    "You are currently #$rank of $totalParticipants finishers."
                } else if (viewOnly && totalParticipants > 0) {
                    "You haven't completed this challenge yet."
                } else if (totalParticipants == 0) {
                    "No finishers yet. Be the first!"
                } else {
                    "You are on the board."
                },
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            if (improvedPersonalBest && !viewOnly) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0F172A)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4ADE80),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "New personal best",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultStats(
    finishSeconds: Int,
    personalBestSeconds: Long?,
    targetDistanceKm: Double,
    viewOnly: Boolean
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ResultStat(
                label = if (viewOnly && finishSeconds == 0) "Your Best" else "Finish Time",
                value = if (viewOnly && finishSeconds == 0) (personalBestSeconds?.let(::formatChallengeResultTime) ?: "--:--") else formatChallengeResultTime(finishSeconds.toLong())
            )
            if (!viewOnly || finishSeconds > 0) {
                ResultStat(
                    label = "Best Time",
                    value = personalBestSeconds?.let(::formatChallengeResultTime) ?: "--:--"
                )
            }
            ResultStat(
                label = "Course",
                value = String.format(Locale.getDefault(), "%.1f km", targetDistanceKm)
            )
        }
    }
}

@Composable
private fun ResultStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF6B7280)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827)
        )
    }
}

@Composable
private fun StandingRow(
    position: Int,
    entry: ChallengeStandingEntry,
    highlight: Boolean
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (highlight) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = if (position == 1) Color(0xFFFFEDD5) else Color.White
                ) {
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = position.toString(),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                    }
                }
                Spacer(modifier = Modifier.size(10.dp))
                Column {
                    Text(
                        text = entry.name,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                    Text(
                        text = "${entry.attemptCount} attempt${if (entry.attemptCount == 1) "" else "s"}",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }

            Text(
                text = formatChallengeResultTime(entry.bestCompletionSeconds),
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )
        }
    }
}

private fun formatChallengeResultTime(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
