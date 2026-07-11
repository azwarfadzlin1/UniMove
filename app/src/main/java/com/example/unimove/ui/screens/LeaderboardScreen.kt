package com.example.unimove.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.unimove.data.model.WeeklyLeaderboardEntry
import com.example.unimove.ui.viewmodel.LeaderboardViewModel
import com.example.unimove.ui.viewmodel.formatLeaderboardDistance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    navController: NavController,
    viewModel: LeaderboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4059AD),
                        Color(0xFF6B46C1),
                        Color(0xFF8B5CF6)
                    )
                )
            )
    ) {
        Scaffold(
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Leaderboard",
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Weekly Challenge",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = uiState.countdownText,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )

                    uiState.currentUserMessage?.let { message ->
                        Spacer(Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = message,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                color = Color.White,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    when {
                        uiState.isLoading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        }

                        uiState.isEmpty -> {
                            EmptyLeaderboardHeader()
                        }

                        else -> {
                            PodiumSection(users = uiState.topThree)
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    when {
                        uiState.isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        uiState.isEmpty -> {
                            EmptyLeaderboardBody()
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(24.dp)
                            ) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "All Runners",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1F2937)
                                            )
                                            Text(
                                                text = uiState.weekKey,
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }
                                        Text(
                                            text = "Distance",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                itemsIndexed(uiState.entries) { index, user ->
                                    LeaderboardListItem(user = user)
                                    if (index < uiState.entries.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            color = Color(0xFFF3F4F6)
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
private fun EmptyLeaderboardHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No podium yet",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "No runs recorded this week yet. Be the first to start.",
                color = Color.White.copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun EmptyLeaderboardBody() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No runs recorded this week yet. Be the first to start.",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Once a session is saved, the weekly leaderboard will appear here.",
                fontSize = 13.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PodiumSection(users: List<WeeklyLeaderboardEntry>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        users.getOrNull(1)?.let { user ->
            PodiumCard(
                user = user,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )
        }

        users.getOrNull(0)?.let { user ->
            PodiumCard(
                user = user,
                modifier = Modifier
                    .weight(1.1f)
                    .offset(y = (-20).dp)
                    .zIndex(1f)
            )
        }

        users.getOrNull(2)?.let { user ->
            PodiumCard(
                user = user,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun PodiumCard(
    user: WeeklyLeaderboardEntry,
    modifier: Modifier = Modifier
) {
    val rank = user.rank
    val isFirst = rank == 1
    val medalColor = when (rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> Color.Gray
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                modifier = Modifier
                    .size(if (isFirst) 88.dp else 68.dp)
                    .border(
                        width = if (user.isCurrentUser) 5.dp else 4.dp,
                        color = if (user.isCurrentUser) Color.White else medalColor,
                        shape = CircleShape
                    )
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0E7FF)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.name.take(1).uppercase(),
                    fontSize = if (isFirst) 32.sp else 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4059AD)
                )
            }

            Box(
                modifier = Modifier
                    .offset(x = 4.dp, y = 0.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(medalColor)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$rank",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = user.name,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            fontSize = if (isFirst) 18.sp else 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = formatLeaderboardDistance(user.weeklyDistance),
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.92f),
            fontSize = if (isFirst) 14.sp else 12.sp
        )

        Text(
            text = "${user.weeklySessions} runs",
            color = Color.White.copy(alpha = 0.78f),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun LeaderboardListItem(user: WeeklyLeaderboardEntry) {
    val rowBackground = if (user.isCurrentUser) Color(0xFFF2EEFF) else Color.Transparent
    val rankColor = if (user.isCurrentUser) Color(0xFF6B46C1) else Color.Gray
    val distanceColor = if (user.isCurrentUser) Color(0xFF6B46C1) else Color(0xFF4059AD)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(rowBackground)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${user.rank}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = rankColor,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (user.isCurrentUser) Color(0xFFDCCEFF) else Color(0xFFF3F4F6)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user.name.take(1).uppercase(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (user.isCurrentUser) Color(0xFF6B46C1) else Color(0xFF6B7280)
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = user.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (user.isCurrentUser) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = Color(0xFFE6DCFF),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "YOU",
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6B46C1),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
            Text(
                text = "${user.weeklySessions} runs this week",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        Text(
            text = formatLeaderboardDistance(user.weeklyDistance),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = distanceColor,
            modifier = Modifier.width(76.dp),
            textAlign = TextAlign.End,
            maxLines = 1,
            softWrap = false
        )
    }
}
