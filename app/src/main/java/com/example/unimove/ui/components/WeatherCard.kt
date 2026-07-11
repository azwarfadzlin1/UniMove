package com.example.unimove.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed class WeatherCardState(
    val themeColor: Color,
    val icon: ImageVector,
    val headlineKeyword: String
) {
    data class WindowOfOpportunity(
        val minutesRemaining: Int,
        val rainExpectedTime: String,
        val suggestedDistance: String
    ) : WeatherCardState(Color(0xFF4CAF50), Icons.Rounded.Timer, "Clear Window")

    object CurrentlyRaining : WeatherCardState(Color(0xFFF44336), Icons.Rounded.Umbrella, "Stay Indoors")
    object ClearSkiesAhead : WeatherCardState(Color(0xFF4CAF50), Icons.Rounded.WbSunny, "Optimal Now")
    object HighHeat : WeatherCardState(Color(0xFFFF9800), Icons.Rounded.WbSunny, "Heat Alert")
    object Overcast : WeatherCardState(Color(0xFF90CAF9), Icons.Rounded.Cloud, "Cool & Grey")
}

data class WeatherMetrics(
    val temperature: String,
    val feelsLike: String,
    val rainProbability: String,
    val humidity: String,
    val windSpeed: String
)

@Composable
fun WeatherCard(
    weatherState: WeatherCardState,
    metrics: WeatherMetrics,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0D1B2A), Color(0xFF1B263B))
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                // Top Row: Status, Icon & Temp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BeatingWeatherIcon(
                                icon = weatherState.icon,
                                themeColor = weatherState.themeColor,
                                isUrgent = weatherState is WeatherCardState.HighHeat || weatherState is WeatherCardState.CurrentlyRaining
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = weatherState.headlineKeyword.uppercase(),
                                color = weatherState.themeColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = metrics.temperature,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Large Weather Icon with subtle background
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        modifier = Modifier.size(60.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = weatherState.icon,
                                contentDescription = null,
                                tint = weatherState.themeColor,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Compact Metrics Row (Single row to lower height)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactMetricItem(label = "Feels", value = metrics.feelsLike)
                    VerticalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.height(20.dp))
                    CompactMetricItem(label = "Rain %", value = metrics.rainProbability)
                    VerticalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.height(20.dp))
                    CompactMetricItem(label = "Wind", value = metrics.windSpeed)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Insight Section (Insightful Advice)
                InsightBox(weatherState)
            }
        }
    }
}

@Composable
fun CompactMetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun InsightBox(state: WeatherCardState) {
    val (adviceText, icon) = when (state) {
        is WeatherCardState.WindowOfOpportunity -> {
            buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                    append("${state.minutesRemaining} min window! ")
                }
                append("Rain expected at ${state.rainExpectedTime}. Go for a ${state.suggestedDistance} run now.")
            } to Icons.Rounded.Bolt
        }
        is WeatherCardState.CurrentlyRaining -> 
            buildAnnotatedString { append("Stormy conditions. Best to rest or use the indoor treadmill today.") } to Icons.Rounded.Warning
        is WeatherCardState.ClearSkiesAhead -> 
            buildAnnotatedString { append("Perfect conditions. Your usual campus route is clear and safe.") } to Icons.Rounded.CheckCircle
        is WeatherCardState.HighHeat -> 
            buildAnnotatedString { append("High heat! Stick to shaded trails and keep your pace steady.") } to Icons.Rounded.Warning
        is WeatherCardState.Overcast -> 
            buildAnnotatedString { append("Cool and overcast. A great day to push for those extra miles!") } to Icons.Rounded.Speed
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(state.themeColor.copy(alpha = 0.12f))
            .border(1.dp, state.themeColor.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = state.themeColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = adviceText,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun BeatingWeatherIcon(icon: ImageVector, themeColor: Color, isUrgent: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "beating_icon")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isUrgent) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_scale"
    )

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = themeColor,
        modifier = Modifier
            .size(16.dp)
            .scale(scale)
    )
}
