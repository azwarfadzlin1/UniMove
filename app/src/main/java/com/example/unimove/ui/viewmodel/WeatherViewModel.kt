package com.example.unimove.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimove.BuildConfig
import com.example.unimove.data.weather.OwmCurrentWeatherResponse
import com.example.unimove.data.weather.OwmForecastResponse
import com.example.unimove.data.weather.WeatherApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class WeatherState {
    object Loading : WeatherState()
    data class Success(
        val current: OwmCurrentWeatherResponse,
        val forecast: OwmForecastResponse
    ) : WeatherState()
    data class Error(val message: String) : WeatherState()
}

class WeatherViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<WeatherState>(WeatherState.Loading)
    val uiState: StateFlow<WeatherState> = _uiState

    fun fetchWeatherByCity(city: String) {
        viewModelScope.launch {
            _uiState.value = WeatherState.Loading
            try {
                val apiKey = BuildConfig.WEATHER_API_KEY
                if (apiKey.isBlank()) {
                    _uiState.value = WeatherState.Error("API Key is missing.")
                    return@launch
                }

                val current = WeatherApiClient.service.getCurrentWeatherByCity(
                    query = city,
                    apiKey = apiKey
                )
                val forecast = WeatherApiClient.service.getForecastByCity(
                    query = city,
                    apiKey = apiKey
                )

                _uiState.value = WeatherState.Success(current = current, forecast = forecast)
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Failed to fetch weather by city", e)
                _uiState.value = WeatherState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun fetchWeatherByLocation(lat: Double, lon: Double) {
        viewModelScope.launch {
            _uiState.value = WeatherState.Loading
            try {
                val apiKey = BuildConfig.WEATHER_API_KEY
                if (apiKey.isBlank()) {
                    _uiState.value = WeatherState.Error("API Key is missing.")
                    return@launch
                }

                val current = WeatherApiClient.service.getCurrentWeatherByLocation(
                    lat = lat,
                    lon = lon,
                    apiKey = apiKey
                )
                val forecast = WeatherApiClient.service.getForecastByLocation(
                    lat = lat,
                    lon = lon,
                    apiKey = apiKey
                )

                _uiState.value = WeatherState.Success(current = current, forecast = forecast)
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Failed to fetch weather by location", e)
                _uiState.value = WeatherState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
