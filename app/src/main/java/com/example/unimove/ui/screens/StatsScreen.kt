package com.example.unimove.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import com.example.unimove.ui.theme.AccentBlueSoft
import com.example.unimove.ui.theme.SummaryGradientTop
import com.example.unimove.ui.theme.SummaryGradientBot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.unimove.ui.theme.AccentBlue
import com.example.unimove.ui.theme.BorderWhite
import com.example.unimove.ui.theme.CardSurface
import com.example.unimove.ui.theme.SurfaceLight
import com.example.unimove.ui.theme.SurfaceLightEnd
import com.example.unimove.ui.theme.TextPrimary
import com.example.unimove.ui.theme.TextSecondary
import com.example.unimove.ui.viewmodel.AnalyticsViewModel
import com.example.unimove.ui.viewmodel.DistanceTrendDay
import com.example.unimove.ui.viewmodel.HeatmapCell
import com.example.unimove.ui.viewmodel.HeatmapLevel
import com.example.unimove.ui.viewmodel.LifetimeAnalyticsSummary
import com.example.unimove.ui.viewmodel.PaceProgressPoint
import com.example.unimove.ui.viewmodel.WatchSessionViewModel
import com.example.unimove.ui.viewmodel.PersonalBests
import com.example.unimove.ui.viewmodel.TimeOfDayStat
import com.example.unimove.wear.WatchSessionUpdate
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.continuous
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    navController: NavController,
    viewModel: AnalyticsViewModel = viewModel(),
    watchViewModel: WatchSessionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val liveSession by watchViewModel.liveSession.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Performance Insights",
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(CircleShape)
                            .background(AccentBlue.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AccentBlue
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh() },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(CircleShape)
                            .background(AccentBlue.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh insights",
                            tint = AccentBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.White.copy(alpha = 0.95f)
                ),
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(SurfaceLight, SurfaceLightEnd)))
                .padding(innerPadding)
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

                uiState.errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.errorMessage.orEmpty(),
                            color = Color.Red.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Live watch session card (shown only for active, fresh watch workouts)
                        if (liveSession != null) {
                            item {
                                LiveWatchSessionCard(session = liveSession!!.session)
                            }
                        }
                        item {
                            LifetimeSummarySection(summary = uiState.summary)
                        }
                        item {
                            PersonalBestsSection(bests = uiState.personalBests)
                        }
                        item {
                            ActivityTimeOfDaySection(timeStats = uiState.activityTimeOfDay)
                        }
                        item {
                            DistanceMomentumSection(days = uiState.distanceTrendLast7Days)
                        }
                        item {
                            PaceProgressionSection(
                                points = uiState.paceProgression,
                                trendSecPerRun = uiState.paceImprovementSecPerRun
                            )
                        }
                        item {
                            EngagementHeatmapSection(
                                monthLabel = uiState.heatmapMonthLabel,
                                cells = uiState.heatmapCells
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LifetimeSummarySection(summary: LifetimeAnalyticsSummary) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.shadow(8.dp, RoundedCornerShape(28.dp), clip = false)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(SummaryGradientTop, SummaryGradientBot)
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Lifetime Summary",
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        fontSize = 20.sp,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Your journey so far",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AccentBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryMetricRow(
                    leftTitle = "Total Runs",
                    leftValue = summary.totalRuns.toString(),
                    leftIcon = Icons.Default.DirectionsRun,
                    rightTitle = "Total Distance",
                    rightValue = String.format(Locale.getDefault(), "%.1f km", summary.totalDistanceKm),
                    rightIcon = Icons.Default.Route
                )
                SummaryMetricRow(
                    leftTitle = "Total Time",
                    leftValue = durationLabel(summary.totalDurationSec),
                    leftIcon = Icons.Default.Timer,
                    rightTitle = "Calories",
                    rightValue = "${summary.totalCaloriesBurned} kcal",
                    rightIcon = Icons.Default.LocalFireDepartment
                )
                SummaryMetricRow(
                    leftTitle = "Avg Pace",
                    leftValue = paceLabel(summary.avgPaceSecPerKm),
                    leftIcon = Icons.Default.Speed,
                    rightTitle = "Longest Run",
                    rightValue = String.format(Locale.getDefault(), "%.1f km", summary.longestRunKm),
                    rightIcon = Icons.Default.TrendingUp
                )
            }

            HorizontalDivider(color = TextPrimary.copy(alpha = 0.05f), thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.CalendarToday, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                    Text(
                        text = "Active run days: ${summary.activeRunDays}",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                androidx.compose.material3.Surface(
                    color = AccentBlue.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    androidx.compose.material3.Text(
                        text = "Day ${summary.daysSinceJoined}",
                        color = AccentBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricRow(
    leftTitle: String,
    leftValue: String,
    leftIcon: androidx.compose.ui.graphics.vector.ImageVector,
    rightTitle: String,
    rightValue: String,
    rightIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryMetricCard(
            modifier = Modifier.weight(1f),
            title = leftTitle,
            value = leftValue,
            icon = leftIcon
        )
        SummaryMetricCard(
            modifier = Modifier.weight(1f),
            title = rightTitle,
            value = rightValue,
            icon = rightIcon
        )
    }
}

@Composable
private fun SummaryMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.7f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentBlueSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = value,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    letterSpacing = (-0.3).sp
                )
            }
        }
    }
}

@Composable
private fun DistanceMomentumSection(days: List<DistanceTrendDay>) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.shadow(8.dp, RoundedCornerShape(28.dp), clip = false)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Distance Momentum",
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Last 7 days performance",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2563EB).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.BarChart, null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                }
            }

            if (days.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .background(AccentBlue.copy(alpha = 0.03f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No runs recorded this week.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                DistanceMomentumChart(days = days)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentBlue.copy(alpha = 0.05f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Week Total", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f km", days.sumOf { it.distanceKm }),
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    VerticalDividerInsight()
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Best Day", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f km", days.maxOfOrNull { it.distanceKm } ?: 0.0),
                            color = Color(0xFF0EA75A),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DistanceMomentumChart(days: List<DistanceTrendDay>) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val labels = remember(days) { days.map { it.label } }
    val xValues = remember(days) { days.indices.toList() }
    val yValues = remember(days) { days.map { it.distanceKm } }

    LaunchedEffect(xValues, yValues) {
        modelProducer.runTransaction {
            columnSeries {
                series(x = xValues, y = yValues)
            }
        }
    }

    val axisLabel = rememberAxisLabelComponent(color = TextSecondary, textSize = 10.sp)
    val chart = rememberCartesianChart(
        rememberColumnCartesianLayer(
            columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                rememberLineComponent(
                    fill = fill(Color(0xFF2563EB)), 
                    thickness = 12.dp
                )
            )
        ),
        startAxis = VerticalAxis.rememberStart(
            label = axisLabel,
            valueFormatter = CartesianValueFormatter { _, value, _ ->
                String.format(Locale.getDefault(), "%.1f", value)
            },
            guideline = rememberAxisGuidelineComponent(fill = fill(Color(0xFFE2E8F0).copy(alpha = 0.5f)))
        ),
        bottomAxis = HorizontalAxis.rememberBottom(
            label = axisLabel,
            valueFormatter = CartesianValueFormatter { _, value, _ ->
                indexLabel(value, labels)
            },
            guideline = null
        )
    )

    CartesianChartHost(
        chart = chart,
        modelProducer = modelProducer,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(top = 8.dp)
    )
}

@Composable
private fun PaceProgressionSection(points: List<PaceProgressPoint>, trendSecPerRun: Double) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.shadow(8.dp, RoundedCornerShape(28.dp), clip = false)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Pace Progression",
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Last 10 training sessions",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF8B5CF6).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.TrendingUp, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(20.dp))
                }
            }

            if (points.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .background(AccentBlue.copy(alpha = 0.03f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Complete 2+ runs to see pace trend.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                PaceProgressionChart(points = points)
                
                androidx.compose.material3.Surface(
                    color = if (trendSecPerRun > 0.5) Color(0xFFE6F4EA) else Color(0xFFF1F3F4),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (trendSecPerRun > 0.5) Icons.Default.AutoAwesome else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (trendSecPerRun > 0.5) Color(0xFF0EA75A) else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        val trendText = when {
                            trendSecPerRun > 0.5 -> "Improving by ${trendSecPerRun.roundToInt()}s per run"
                            trendSecPerRun < -0.5 -> "Pace slowed by ${abs(trendSecPerRun).roundToInt()}s per run"
                            else -> "Pace trend is stable"
                        }
                        Text(
                            text = trendText,
                            color = if (trendSecPerRun > 0.5) Color(0xFF0EA75A) else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaceProgressionChart(points: List<PaceProgressPoint>) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val labels = remember(points) {
        val formatter = DateTimeFormatter.ofPattern("d/M")
        points.map { it.date.format(formatter) }
    }
    val xValues = remember(points) { points.indices.toList() }
    val yValues = remember(points) { points.map { it.paceSecPerKm } }

    LaunchedEffect(xValues, yValues) {
        modelProducer.runTransaction {
            lineSeries {
                series(x = xValues, y = yValues)
            }
        }
    }

    val axisLabel = rememberAxisLabelComponent(color = TextSecondary, textSize = 10.sp)
    val line = LineCartesianLayer.rememberLine(
        fill = LineCartesianLayer.LineFill.single(fill(Color(0xFF2563EB))),
        stroke = LineCartesianLayer.LineStroke.continuous(
            thickness = 3.dp,
            cap = StrokeCap.Round
        ),
        areaFill = LineCartesianLayer.AreaFill.single(fill(Color(0x332563EB)))
    )
    val chart = rememberCartesianChart(
        rememberLineCartesianLayer(
            lineProvider = LineCartesianLayer.LineProvider.series(line)
        ),
        startAxis = VerticalAxis.rememberStart(
            label = axisLabel,
            valueFormatter = CartesianValueFormatter { _, value, _ ->
                paceAxisLabel(value)
            },
            guideline = rememberAxisGuidelineComponent(fill = fill(Color(0xFFE2E8F0)))
        ),
        bottomAxis = HorizontalAxis.rememberBottom(
            label = axisLabel,
            valueFormatter = CartesianValueFormatter { _, value, _ ->
                indexLabel(value, labels)
            }
        )
    )

    CartesianChartHost(
        chart = chart,
        modelProducer = modelProducer,
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
    )
}

@Composable
private fun EngagementHeatmapSection(monthLabel: String, cells: List<HeatmapCell>) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.shadow(6.dp, RoundedCornerShape(28.dp), clip = false)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Consistency Map",
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = monthLabel.ifBlank { "Monthly overview" },
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    imageVector = Icons.Default.GridOn,
                    contentDescription = null,
                    tint = TextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }

            if (cells.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(AccentBlue.copy(alpha = 0.03f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No monthly activity yet.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    cells.chunked(7).forEach { week ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            week.forEach { cell ->
                                val color = when {
                                    cell.date == null -> Color.Transparent
                                    cell.level == HeatmapLevel.STRONG_RUN -> Color(0xFF1D4ED8)
                                    cell.level == HeatmapLevel.LIGHT_RUN -> Color(0xFF93C5FD)
                                    else -> Color(0xFFF3F4F6)
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(color)
                                )
                            }
                        }
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentBlue.copy(alpha = 0.04f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendDot(color = Color(0xFF1D4ED8), label = "5km+")
                    LegendDot(color = Color(0xFF93C5FD), label = "Run")
                    LegendDot(color = Color(0xFFE5E7EB), label = "Rest")
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextSecondary
        )
    }
}

private fun paceLabel(paceSecPerKm: Double): String {
    if (paceSecPerKm <= 0.0) return "-- /km"
    val mins = (paceSecPerKm / 60).toInt()
    val secs = (paceSecPerKm % 60).toInt()
    return String.format(Locale.getDefault(), "%d:%02d /km", mins, secs)
}

private fun paceAxisLabel(paceSecPerKm: Double): String {
    if (paceSecPerKm <= 0.0) return "--"
    val mins = (paceSecPerKm / 60).toInt()
    val secs = (paceSecPerKm % 60).toInt()
    return String.format(Locale.getDefault(), "%d:%02d", mins, secs)
}

private fun durationLabel(totalDurationSec: Long): String {
    if (totalDurationSec <= 0L) return "00:00:00"
    val hours = totalDurationSec / 3600
    val minutes = (totalDurationSec % 3600) / 60
    val seconds = totalDurationSec % 60
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
}

@Composable
private fun VerticalDividerInsight() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(30.dp)
            .background(TextPrimary.copy(alpha = 0.08f))
    )
}

private fun indexLabel(value: Double, labels: List<String>): String {
    val index = value.roundToInt()
    return labels.getOrNull(index) ?: "-"
}

private fun watchPaceLabel(
    paceMsPerKm: Long,
    elapsedMs: Long,
    distanceMeters: Double,
): String {
    val resolvedPaceMsPerKm = when {
        paceMsPerKm > 0L -> paceMsPerKm.toDouble()
        elapsedMs > 0L && distanceMeters >= 10.0 -> elapsedMs / (distanceMeters / 1_000.0)
        else -> 0.0
    }

    if (resolvedPaceMsPerKm <= 0.0) {
        return "-- /km"
    }

    val totalSeconds = (resolvedPaceMsPerKm / 1_000.0).roundToInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d /km", minutes, seconds)
}

// ══════════════════════════════════════════════════════════════════════════
//  Live Watch Session Card
// ══════════════════════════════════════════════════════════════════════════

@Composable
fun LiveWatchSessionCard(session: WatchSessionUpdate) {
    // Pulse animation for the live indicator dot
    val infiniteTransition = rememberInfiniteTransition(label = "watch_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val elapsedSec = (session.elapsedMs / 1000L).toInt()
    val hrs  = elapsedSec / 3600
    val mins = (elapsedSec % 3600) / 60
    val secs = elapsedSec % 60
    val timeLabel = if (hrs > 0) "%d:%02d:%02d".format(hrs, mins, secs)
                    else "%02d:%02d".format(mins, secs)

    val distKm = String.format(Locale.getDefault(), "%.2f km", session.distanceMeters / 1000.0)

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (session.isEnded) Color(0xFFF0F7FF) else Color(0xFF0F1F3D)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Header row ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Watch,
                        contentDescription = null,
                        tint = if (session.isEnded) AccentBlue else Color(0xFF60B0FF),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Watch Workout",
                        fontWeight = FontWeight.ExtraBold,
                        color = if (session.isEnded) TextPrimary else Color.White,
                        fontSize = 16.sp
                    )
                }

                // Live / Ended badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .background(
                            if (session.isEnded) Color(0xFFDEF7EC) else Color(0xFF1A3A6B),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    if (!session.isEnded) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .scale(pulseScale)
                                .background(Color(0xFF4ADE80), CircleShape)
                        )
                    }
                    Text(
                        text = if (session.isEnded) "Completed" else "Live",
                        color = if (session.isEnded) Color(0xFF065F46) else Color(0xFF4ADE80),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(
                color = if (session.isEnded) BorderWhite.copy(alpha = 0.25f)
                        else Color.White.copy(alpha = 0.08f)
            )

            // ── Metrics grid ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                WatchMetric(
                    icon = Icons.Default.DirectionsRun,
                    label = "Distance",
                    value = distKm,
                    isLight = session.isEnded
                )
                WatchMetric(
                    icon = Icons.Default.DirectionsWalk,
                    label = "Steps",
                    value = session.steps.toString(),
                    isLight = session.isEnded
                )
                WatchMetric(
                    icon = Icons.Default.Favorite,
                    label = "Heart Rate",
                    value = if (session.heartRate > 0) "${session.heartRate} bpm" else "--",
                    isLight = session.isEnded,
                    iconTint = Color(0xFFFC4C6B)
                )
                WatchMetricText(
                    label = "Time",
                    value = timeLabel,
                    isLight = session.isEnded
                )
                WatchMetricText(
                    label = "Pace",
                    value = watchPaceLabel(session.paceMsPerKm, session.elapsedMs, session.distanceMeters),
                    isLight = session.isEnded
                )
            }
        }
    }
}

@Composable
private fun WatchMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isLight: Boolean,
    iconTint: Color = if (isLight) AccentBlue else Color(0xFF60B0FF)
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = value,
            color = if (isLight) TextPrimary else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
        Text(
            text = label,
            color = if (isLight) TextSecondary else Color(0xFF90A4C8),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun WatchMetricText(
    label: String,
    value: String,
    isLight: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            color = if (isLight) TextPrimary else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
        Text(
            text = label,
            color = if (isLight) TextSecondary else Color(0xFF90A4C8),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun PersonalBestsSection(bests: PersonalBests) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)), // Deep rich purple
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.shadow(12.dp, RoundedCornerShape(28.dp), clip = false)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1E1B4B), Color(0xFF110E36))
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFBBF24).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text("Personal Bests", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 20.sp, letterSpacing = (-0.5).sp)
                        Text("Your strongest recorded stats", color = Color(0xFF818CF8), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TrophyCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.DirectionsRun,
                    title = "Longest",
                    value = String.format(Locale.getDefault(), "%.1f km", bests.longestRunKm),
                    tint = Color(0xFF34D399) // Green
                )
                TrophyCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Bolt,
                    title = "Pace",
                    value = paceLabel(bests.fastestPaceSecPerKm.toDouble()).replace(" /km", ""),
                    tint = Color(0xFFFBBF24) // Yellow
                )
                TrophyCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.LocalFireDepartment,
                    title = "Calories",
                    value = "${bests.mostCaloriesBurned}",
                    tint = Color(0xFFF87171) // Red
                )
            }
        }
    }
}

@Composable
private fun TrophyCard(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String, tint: Color) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                maxLines = 1,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = title,
                color = Color(0xFFA5B4FC),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ActivityTimeOfDaySection(timeStats: List<TimeOfDayStat>) {
    val maxCount = timeStats.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    val totalSessions = timeStats.sumOf { it.count }
    val colors = listOf(
        Color(0xFFF59E0B),
        Color(0xFF2563EB),
        Color(0xFF7C3AED),
        Color(0xFF0F172A)
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.shadow(4.dp, RoundedCornerShape(24.dp), clip = false)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Activity Distribution",
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = if (totalSessions == 0) "No sessions recorded yet" else "$totalSessions sessions by time of day",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(18.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                timeStats.forEachIndexed { index, stat ->
                    ActivityTimeRow(
                        label = stat.label,
                        count = stat.count,
                        maxCount = maxCount,
                        accent = colors[index % colors.size]
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityTimeRow(
    label: String,
    count: Int,
    maxCount: Int,
    accent: Color
) {
    val targetProgress = if (maxCount <= 0) 0f else (count.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)
    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 220),
        label = "activity_time_progress"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(accent, CircleShape)
                )
                Text(
                    text = label,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "$count ${if (count == 1) "run" else "runs"}",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color(0xFFE5E7EB), CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceAtLeast(if (count > 0) 0.06f else 0f))
                    .height(8.dp)
                    .background(accent, CircleShape)
            )
        }
    }
}
