package com.example.unimove.data.weather

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class OwmForecastResponse(
    val list: List<OwmForecastItem>,
    val city: OwmCity
)

data class OwmCity(
    val name: String = "",
    val country: String = "",
    val sunrise: Long = 0L,
    val sunset: Long = 0L,
    val timezone: Int = 0
)

data class OwmForecastItem(
    val dt: Long,
    val main: OwmMain,
    val weather: List<OwmWeather>,
    val pop: Float,
    val dt_txt: String
)

data class OwmCurrentWeatherResponse(
    val name: String = "",
    val timezone: Int = 0,
    val main: OwmMain,
    val weather: List<OwmWeather>,
    val sys: OwmSys = OwmSys(),
    val wind: OwmWind = OwmWind()
)

data class OwmSys(
    val sunrise: Long = 0L,
    val sunset: Long = 0L,
    val country: String = ""
)

data class OwmWind(
    val speed: Float = 0f
)

data class OwmMain(
    val temp: Float,
    val feels_like: Float,
    val temp_min: Float,
    val temp_max: Float,
    val humidity: Int
)

data class OwmWeather(
    val main: String,
    val description: String,
    val icon: String
)

interface WeatherApiService {
    @GET("forecast")
    suspend fun getForecastByCity(
        @Query("q") query: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): OwmForecastResponse

    @GET("forecast")
    suspend fun getForecastByLocation(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): OwmForecastResponse

    @GET("weather")
    suspend fun getCurrentWeatherByCity(
        @Query("q") query: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): OwmCurrentWeatherResponse

    @GET("weather")
    suspend fun getCurrentWeatherByLocation(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): OwmCurrentWeatherResponse
}

object WeatherApiClient {
    private const val BASE_URL = "https://api.openweathermap.org/data/2.5/"

    val service: WeatherApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApiService::class.java)
    }
}
