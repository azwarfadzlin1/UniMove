package com.example.unimove.data.meal

import com.example.unimove.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

data class KaloFoodSearchResponse(
    val success: Boolean = false,
    val data: List<KaloFoodItem>? = null,
    val count: Int? = null
)

data class KaloFoodItem(
    val name: String? = null,
    val calories: Double? = null,
    val serving: String? = null
)

interface KaloKalorieApi {
    @GET("api/v1/foods/search")
    suspend fun searchFoods(
        @Query("q") query: String,
        @Header("X-API-Key") apiKey: String
    ): KaloFoodSearchResponse

    @GET("api/v1/halal/search")
    suspend fun searchHalalFoods(
        @Query("q") query: String,
        @Header("X-API-Key") apiKey: String
    ): KaloFoodSearchResponse

    @GET("api/v1/foods")
    suspend fun listFoods(
        @Query("limit") limit: Int = 200,
        @Query("offset") offset: Int = 0,
        @Header("X-API-Key") apiKey: String
    ): KaloFoodSearchResponse

    @GET("api/v1/halal")
    suspend fun listHalalFoods(
        @Query("limit") limit: Int = 200,
        @Query("offset") offset: Int = 0,
        @Header("X-API-Key") apiKey: String
    ): KaloFoodSearchResponse
}

object KaloKalorieApiClient {
    fun serviceOrNull(): KaloKalorieApi? {
        val baseUrl = BuildConfig.KALO_API_BASE_URL.trim()
        if (baseUrl.isBlank()) return null

        val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        return Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KaloKalorieApi::class.java)
    }
}

data class UsdaFoodSearchRequest(
    val query: String,
    val dataType: List<String> = listOf(
        "Survey (FNDDS)",
        "SR Legacy",
        "Foundation",
        "Branded"
    ),
    val pageSize: Int = 40,
    val pageNumber: Int = 1
)

data class UsdaFoodSearchResponse(
    val foods: List<UsdaFoodItem>? = null,
    val totalHits: Int? = null,
    val currentPage: Int? = null,
    val totalPages: Int? = null
)

data class UsdaFoodItem(
    val fdcId: Long? = null,
    val description: String? = null,
    val dataType: String? = null,
    val brandOwner: String? = null,
    val brandName: String? = null,
    val servingSize: Double? = null,
    val servingSizeUnit: String? = null,
    val householdServingFullText: String? = null,
    val score: Double? = null,
    val foodNutrients: List<UsdaFoodNutrient>? = null,
    val foodMeasures: List<UsdaFoodMeasure>? = null
)

data class UsdaFoodNutrient(
    val nutrientId: Int? = null,
    val nutrientName: String? = null,
    val nutrientNumber: String? = null,
    val unitName: String? = null,
    val value: Double? = null
)

data class UsdaFoodMeasure(
    val disseminationText: String? = null,
    val gramWeight: Double? = null,
    val rank: Int? = null,
    val measureUnitAbbreviation: String? = null,
    val measureUnitName: String? = null
)

interface UsdaFoodDataApi {
    @POST("foods/search")
    suspend fun searchFoods(
        @Query("api_key") apiKey: String,
        @Body request: UsdaFoodSearchRequest
    ): UsdaFoodSearchResponse
}

object UsdaFoodDataApiClient {
    fun serviceOrNull(): UsdaFoodDataApi? {
        val baseUrl = BuildConfig.USDA_FDC_API_BASE_URL.trim()
        if (baseUrl.isBlank()) return null

        val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        return Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UsdaFoodDataApi::class.java)
    }
}
