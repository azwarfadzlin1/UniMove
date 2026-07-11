package com.example.unimove.utils

import com.example.unimove.data.weather.OwmCurrentWeatherResponse
import com.example.unimove.data.weather.OwmForecastResponse
import com.example.unimove.data.weather.OwmWeather
import com.example.unimove.ui.components.WeatherCardState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

object WeatherAnalyzer {

    // Helper to calculate distance based on average 6:30 min/km pace
    private fun calculateSuggestedDistance(minutes: Int): String {
        val distance = minutes / 6.5
        val lowerBound = floor(distance).toInt()
        val upperBound = ceil(distance).toInt()
        // Ensure distance bounds make sense (e.g. 0-1km instead of 0-0km)
        val finalUpper = if (upperBound <= lowerBound) lowerBound + 1 else upperBound
        return "$lowerBound-${finalUpper}km"
    }

    fun analyzeWeatherWindow(
        current: OwmCurrentWeatherResponse,
        forecast: OwmForecastResponse
    ): WeatherCardState {
        // 1. Check current state
        val currentCondition = current.weather.firstOrNull()?.main ?: ""
        if (currentCondition.contains("Rain", ignoreCase = true) || 
            currentCondition.contains("Thunderstorm", ignoreCase = true)) {
            return WeatherCardState.CurrentlyRaining
        }

        // Retain High Heat logic from original states
        if (current.main.temp > 32f || current.main.humidity > 85) {
            return WeatherCardState.HighHeat
        }

        // 2. Scan Future Hours
        val currentTimeMillis = System.currentTimeMillis()
        var rainExpectedTimeMillis: Long? = null

        // Forecast list comes in 3-hour chunks typically for OWM free tier
        for (item in forecast.list) {
            val isRaining = item.weather.any { weatherItem: OwmWeather ->
                weatherItem.main.contains("Rain", ignoreCase = true) || 
                weatherItem.main.contains("Thunderstorm", ignoreCase = true)
            }
            if (isRaining || item.pop > 0.4f) {
                rainExpectedTimeMillis = item.dt * 1000L
                break
            }
        }

        // 3. Evaluate results
        if (rainExpectedTimeMillis != null) {
            val differenceMillis = rainExpectedTimeMillis - currentTimeMillis
            val differenceMinutes = (differenceMillis / (1000 * 60)).toInt()

            // Edge Case: If the forecasted rain block is actually in the past 
            // (can happen with API caching), or too close (< 30 min)
            if (differenceMinutes < 30) {
                return WeatherCardState.CurrentlyRaining
            } else if (differenceMinutes > 12 * 60) {
                // If rain is more than 12 hours away, treat it as clear
                return WeatherCardState.ClearSkiesAhead
            } else {
                // Valid window found!
                val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
                val timeString = formatter.format(Date(rainExpectedTimeMillis))
                val distance = calculateSuggestedDistance(differenceMinutes)

                return WeatherCardState.WindowOfOpportunity(
                    minutesRemaining = differenceMinutes,
                    rainExpectedTime = timeString,
                    suggestedDistance = distance
                )
            }
        } else {
            // No rain detected in the returned forecast data (next 5 days)
            return WeatherCardState.ClearSkiesAhead
        }
    }
}
