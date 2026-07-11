package com.example.unimove.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material.icons.outlined.ModeComment
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.unimove.ui.theme.*
import com.example.unimove.ui.viewmodel.CommunityFeedViewModel
import com.example.unimove.ui.viewmodel.FeedPost
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class FeedUiState {
    Loading,
    Empty,
    Content
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityFeedScreen(
    navController: NavController,
    viewModel: CommunityFeedViewModel = viewModel()
) {
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val uiState = when {
        isLoading -> FeedUiState.Loading
        posts.isEmpty() -> FeedUiState.Empty
        else -> FeedUiState.Content
    }

    val screenWidth = LocalConfiguration.current.screenWidthDp
    val isSmallScreen = screenWidth < 380

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent,
        topBar = {
            Column(
                modifier = Modifier
                    .background(Color.White)
                    .statusBarsPadding()
            ) {
                TopAppBar(
                    title = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Community",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp,
                                    fontSize = if (isSmallScreen) 24.sp else 28.sp
                                ),
                                color = TextPrimary
                            )
                            Text(
                                text = "Campus runs, real momentum",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = if (isSmallScreen) 12.sp else 14.sp
                                ),
                                color = TextSecondary
                            )
                        }
                    },
                    navigationIcon = {
                        Surface(
                            onClick = { navController.navigateUp() },
                            modifier = Modifier.padding(start = 12.dp),
                            shape = CircleShape,
                            color = Color.White,
                            border = BorderStroke(1.dp, DividerColor),
                            shadowElevation = 2.dp
                        ) {
                            Box(
                                modifier = Modifier.size(if (isSmallScreen) 38.dp else 44.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = CommunityGreen,
                                    modifier = Modifier.size(if (isSmallScreen) 18.dp else 22.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        navigationIconContentColor = TextPrimary,
                        titleContentColor = TextPrimary
                    )
                )

            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF9FBFF))
                .padding(innerPadding)
        ) {
            FeedBackgroundDecor()

            Crossfade(
                targetState = uiState,
                animationSpec = tween(durationMillis = 280),
                label = "feed_state"
            ) { state ->
                when (state) {
                    FeedUiState.Loading -> FeedLoadingState()
                    FeedUiState.Empty -> EmptyFeedState()
                    FeedUiState.Content -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 20.dp,
                                end = 20.dp,
                                top = 12.dp,
                                bottom = 28.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            itemsIndexed(
                                items = posts,
                                key = { _, post -> post.id }
                            ) { index, post ->
                                AnimatedFeedPostCard(
                                    index = index,
                                    post = post,
                                    onLikeClick = { viewModel.toggleLike(post.id, post.isLiked) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedBackgroundDecor() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.TopEnd)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CommunityGreen.copy(alpha = 0.14f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = 64.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            StatBlue.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun FeedLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            tonalElevation = 2.dp,
            shadowElevation = 10.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp,
                    color = CommunityGreen
                )
                Column {
                    Text(
                        text = "Loading the latest runs",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Pulling in the community feed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyFeedState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 1.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(CommunityGreen.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                        contentDescription = null,
                        tint = CommunityGreen
                    )
                }
                Text(
                    text = "No runs shared yet",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = "Be the first student to post a session and start the momentum.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun AnimatedFeedPostCard(
    index: Int,
    post: FeedPost,
    onLikeClick: () -> Unit
) {
    val isSmallScreen = LocalConfiguration.current.screenWidthDp < 380
    var visible by rememberSaveable(post.id) { mutableStateOf(false) }

    LaunchedEffect(post.id) {
        if (!visible) {
            delay((index * 55L).coerceAtMost(220L))
            visible = true
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(450, easing = FastOutSlowInEasing)) +
            slideInVertically(
                animationSpec = tween(450, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 5 }
            ),
        exit = fadeOut(animationSpec = tween(200)) +
            slideOutVertically(targetOffsetY = { it / 8 }),
        label = "feed_post_visibility"
    ) {
        PostCard(post = post, onLikeClick = onLikeClick, isSmallScreen = isSmallScreen)
    }
}

@Composable
private fun PostCard(
    post: FeedPost,
    onLikeClick: () -> Unit,
    isSmallScreen: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(30.dp),
                spotColor = Color.Black.copy(alpha = 0.12f)
            ),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isSmallScreen) 14.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FeedPostHeader(post = post, isSmallScreen = isSmallScreen)

            if (post.runName.isNotEmpty()) {
                Text(
                    text = post.runName,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = if (isSmallScreen) 18.sp else 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.4).sp
                    ),
                    color = TextPrimary
                )
            }

            if (post.pathSegments.isNotEmpty()) {
                ActivityRouteCard(post = post, isSmallScreen = isSmallScreen)
            }

            if (post.distanceKm > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (isSmallScreen) 6.dp else 12.dp)
                ) {
                    FeedStatItem(
                        modifier = Modifier.weight(1f),
                        label = "Distance",
                        value = String.format(Locale.getDefault(), "%.2f", post.distanceKm),
                        unit = "km",
                        icon = Icons.Default.Straighten,
                        isSmallScreen = isSmallScreen
                    )
                    FeedStatItem(
                        modifier = Modifier.weight(1f),
                        label = "Duration",
                        value = formatFeedTime(post.durationSeconds),
                        unit = "min",
                        icon = Icons.Default.Schedule,
                        isSmallScreen = isSmallScreen
                    )
                    FeedStatItem(
                        modifier = Modifier.weight(1f),
                        label = "Pace",
                        value = post.avgPace.replace("/km", "").trim(),
                        unit = "/km",
                        icon = Icons.Default.Speed,
                        isSmallScreen = isSmallScreen
                    )
                }
            }

            if (post.content.isNotEmpty()) {
                Text(
                    text = post.content,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = if (isSmallScreen) 20.sp else 24.sp,
                        fontSize = if (isSmallScreen) 14.sp else 16.sp,
                        color = TextPrimary.copy(alpha = 0.85f)
                    )
                )
            }

            HorizontalDivider(color = DividerColor.copy(alpha = 0.5f))

            FeedActionRow(
                post = post,
                onLikeClick = onLikeClick
            )
        }
    }
}

@Composable
private fun FeedPostHeader(post: FeedPost, isSmallScreen: Boolean = false) {
    val username = post.username.ifBlank { "Runner" }
    val avatarLabel = remember(username) {
        username.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(if (isSmallScreen) 44.dp else 54.dp)
                .clip(CircleShape)
                .background(CommunityGreenSoft),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = avatarLabel,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = CommunityGreenDark,
                    fontSize = if (isSmallScreen) 18.sp else 22.sp
                )
            )
        }

        Spacer(modifier = Modifier.width(if (isSmallScreen) 10.dp else 14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = username,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (isSmallScreen) 15.sp else 17.sp
                ),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = post.timeStr,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = if (isSmallScreen) 11.sp else 12.sp
                ),
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = CommunityGreen.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, CommunityGreen.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = if (isSmallScreen) 8.dp else 10.dp,
                    vertical = 6.dp
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                    contentDescription = null,
                    modifier = Modifier.size(if (isSmallScreen) 12.dp else 14.dp),
                    tint = CommunityGreen
                )
                Text(
                    text = "Activity",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = CommunityGreen,
                        fontSize = if (isSmallScreen) 10.sp else 12.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun ActivityRouteCard(post: FeedPost, isSmallScreen: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isSmallScreen) 180.dp else 230.dp)
            .clip(RoundedCornerShape(24.dp))
    ) {
        MapSnapshotComposable(pathSegments = post.pathSegments)

        Surface(
            modifier = Modifier
                .padding(if (isSmallScreen) 10.dp else 14.dp)
                .align(Alignment.TopStart),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    modifier = Modifier.size(if (isSmallScreen) 14.dp else 16.dp),
                    tint = StatBlue
                )
                Text(
                    text = "Route preview",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isSmallScreen) 10.sp else 12.sp
                    ),
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun FeedActionRow(
    post: FeedPost,
    onLikeClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionButton(
            icon = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            label = post.likes.toString(),
            color = if (post.isLiked) ActivityPink else TextSecondary,
            onClick = onLikeClick
        )
        ActionButton(
            icon = Icons.Outlined.ModeComment,
            label = "Comment",
            onClick = {}
        )
        ActionButton(
            icon = Icons.Default.Share,
            label = "Share",
            onClick = {}
        )
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    color: Color = TextSecondary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

@Composable
private fun FeedStatItem(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    unit: String,
    icon: ImageVector,
    isSmallScreen: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, DividerColor.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(if (isSmallScreen) 10.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(if (isSmallScreen) 6.dp else 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isSmallScreen) 4.dp else 6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(if (isSmallScreen) 12.dp else 16.dp),
                    tint = CommunityGreen
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = if (isSmallScreen) 9.sp else 11.sp
                    ),
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = if (isSmallScreen) 16.sp else 24.sp,
                        letterSpacing = if (isSmallScreen) (-0.2).sp else (-0.5).sp
                    ),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = if (isSmallScreen) 9.sp else 11.sp
                    ),
                    color = TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun MapSnapshotComposable(pathSegments: List<List<Map<String, Double>>>) {
    val bounds = remember(pathSegments) {
        var minLat = 90.0
        var maxLat = -90.0
        var minLng = 180.0
        var maxLng = -180.0
        var hasPoints = false

        pathSegments.forEach { segment ->
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
        } else {
            null
        }
    }

    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(bounds) {
        if (bounds != null) {
            delay(200)
            try {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 50))
            } catch (_: Exception) {
                val centerLat = (bounds.northeast.latitude + bounds.southwest.latitude) / 2
                val centerLng = (bounds.northeast.longitude + bounds.southwest.longitude) / 2
                cameraPositionState.position =
                    CameraPosition.fromLatLngZoom(LatLng(centerLat, centerLng), 14f)
            }
        }
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = false),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            scrollGesturesEnabled = false,
            zoomGesturesEnabled = false,
            tiltGesturesEnabled = false,
            rotationGesturesEnabled = false,
            compassEnabled = false
        ),
        googleMapOptionsFactory = { GoogleMapOptions().liteMode(true) }
    ) {
        pathSegments.forEach { segment ->
            val latLngList = segment.mapNotNull {
                val lat = it["lat"]
                val lng = it["lng"]
                if (lat != null && lng != null) LatLng(lat, lng) else null
            }
            Polyline(
                points = latLngList,
                color = StatBlue,
                width = 10f
            )
        }
    }
}

private fun formatFeedTime(seconds: Int): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }
}
