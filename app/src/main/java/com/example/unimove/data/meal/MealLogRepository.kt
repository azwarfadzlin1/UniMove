package com.example.unimove.data.meal

import com.example.unimove.BuildConfig
import com.example.unimove.data.model.DailyMealSummary
import com.example.unimove.data.model.FoodSearchResult
import com.example.unimove.data.model.MealEntryDraft
import com.example.unimove.data.model.MealLogEntry
import com.example.unimove.data.model.MealType
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class MealLogRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun observeDailySummary(
        uid: String,
        dateKey: String,
        onUpdate: (DailyMealSummary) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return dayDocument(uid, dateKey).addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error.message ?: "Failed to load daily calories")
                return@addSnapshotListener
            }

            if (snapshot == null || !snapshot.exists()) {
                onUpdate(DailyMealSummary(dateKey = dateKey))
                return@addSnapshotListener
            }

            onUpdate(
                DailyMealSummary(
                    dateKey = snapshot.getString("dateKey").orEmpty().ifBlank { dateKey },
                    totalCalories = (snapshot.getLong("totalCalories") ?: 0L).toInt(),
                    totalItems = (snapshot.getLong("totalItems") ?: 0L).toInt(),
                    updatedAt = snapshot.getTimestamp("updatedAt")
                )
            )
        }
    }

    fun observeEntries(
        uid: String,
        dateKey: String,
        onUpdate: (List<MealLogEntry>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return entriesCollection(uid, dateKey)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.message ?: "Failed to load meals")
                    return@addSnapshotListener
                }

                val entries = snapshot?.documents.orEmpty().map { document ->
                    MealLogEntry(
                        id = document.id,
                        foodName = document.getString("foodName").orEmpty(),
                        calories = (document.getLong("calories") ?: 0L).toInt(),
                        quantity = document.getDouble("quantity") ?: 1.0,
                        servingSize = document.getString("servingSize").orEmpty(),
                        mealType = MealType.fromStorage(document.getString("mealType")),
                        dateKey = document.getString("dateKey").orEmpty().ifBlank { dateKey },
                        createdAt = document.getTimestamp("createdAt")
                    )
                }

                onUpdate(entries)
            }
    }

    suspend fun upsertEntry(
        uid: String,
        dateKey: String,
        draft: MealEntryDraft,
        existingEntryId: String? = null
    ) {
        val now = Timestamp(Date())
        val dayRef = dayDocument(uid, dateKey)
        val entryRef = existingEntryId
            ?.takeIf { it.isNotBlank() }
            ?.let { entriesCollection(uid, dateKey).document(it) }
            ?: entriesCollection(uid, dateKey).document()

        firestore.runTransaction { transaction ->
            val daySnapshot = transaction.get(dayRef)
            val entrySnapshot = transaction.get(entryRef)

            val currentTotalCalories = (daySnapshot.getLong("totalCalories") ?: 0L).toInt()
            val currentTotalItems = (daySnapshot.getLong("totalItems") ?: 0L).toInt()
            val previousCalories = (entrySnapshot.getLong("calories") ?: 0L).toInt()
            val existed = entrySnapshot.exists()

            val updatedTotalCalories = (currentTotalCalories - previousCalories + draft.calories).coerceAtLeast(0)
            val updatedTotalItems = (currentTotalItems + if (existed) 0 else 1).coerceAtLeast(0)

            transaction.set(
                dayRef,
                mapOf(
                    "dateKey" to dateKey,
                    "totalCalories" to updatedTotalCalories,
                    "totalItems" to updatedTotalItems,
                    "updatedAt" to now
                ),
                SetOptions.merge()
            )

            transaction.set(
                entryRef,
                mapOf(
                    "foodName" to draft.foodName,
                    "calories" to draft.calories,
                    "quantity" to draft.quantity,
                    "servingSize" to draft.servingSize,
                    "mealType" to draft.mealType.name,
                    "dateKey" to dateKey,
                    "createdAt" to (entrySnapshot.getTimestamp("createdAt") ?: now)
                ),
                SetOptions.merge()
            )
        }.await()
    }

    suspend fun deleteEntry(uid: String, dateKey: String, entryId: String) {
        val dayRef = dayDocument(uid, dateKey)
        val entryRef = entriesCollection(uid, dateKey).document(entryId)
        val now = Timestamp(Date())

        firestore.runTransaction { transaction ->
            val daySnapshot = transaction.get(dayRef)
            val entrySnapshot = transaction.get(entryRef)
            if (!entrySnapshot.exists()) return@runTransaction

            val currentTotalCalories = (daySnapshot.getLong("totalCalories") ?: 0L).toInt()
            val currentTotalItems = (daySnapshot.getLong("totalItems") ?: 0L).toInt()
            val removedCalories = (entrySnapshot.getLong("calories") ?: 0L).toInt()

            transaction.delete(entryRef)
            transaction.set(
                dayRef,
                mapOf(
                    "dateKey" to dateKey,
                    "totalCalories" to (currentTotalCalories - removedCalories).coerceAtLeast(0),
                    "totalItems" to (currentTotalItems - 1).coerceAtLeast(0),
                    "updatedAt" to now
                ),
                SetOptions.merge()
            )
        }.await()
    }

    suspend fun searchFoods(query: String): List<FoodSearchResult> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            return emptyList()
        }

        val usdaResults = withTimeoutOrNull(FOOD_SEARCH_TIMEOUT_MS) {
            searchUsdaFoods(trimmed)
        }.orEmpty()

        if (usdaResults.isNotEmpty()) {
            return usdaResults
        }

        return withTimeoutOrNull(FOOD_SEARCH_TIMEOUT_MS) {
            searchKaloFoods(trimmed)
        }.orEmpty()
    }

    private suspend fun searchKaloFoods(query: String): List<FoodSearchResult> {
        val apiKey = BuildConfig.KALO_API_KEY.trim()
        val service = KaloKalorieApiClient.serviceOrNull()

        if (apiKey.isBlank() || service == null) {
            return emptyList()
        }

        return runCatching {
            val fromFoodSearch = service.searchFoods(query = query, apiKey = apiKey).data.orEmpty()
            val fromHalalSearch = service.searchHalalFoods(query = query, apiKey = apiKey).data.orEmpty()
            val mergedSearch = (fromFoodSearch + fromHalalSearch)
                .toKaloFoodSearchResults()
                .dedupeByNameAndServing()

            if (mergedSearch.isNotEmpty()) {
                return@runCatching mergedSearch
            }

            // Fallback: API search can be strict for some branded items.
            val allFoods = service.listFoods(limit = 250, offset = 0, apiKey = apiKey).data.orEmpty()
            val allHalal = service.listHalalFoods(limit = 250, offset = 0, apiKey = apiKey).data.orEmpty()
            (allFoods + allHalal)
                .toKaloFoodSearchResults()
                .filterByQuery(query)
                .dedupeByNameAndServing()
                .take(MAX_FOOD_SEARCH_RESULTS)
        }.getOrElse { emptyList() }
    }

    private suspend fun searchUsdaFoods(query: String): List<FoodSearchResult> {
        val apiKey = BuildConfig.USDA_FDC_API_KEY.trim()
        val service = UsdaFoodDataApiClient.serviceOrNull()

        if (apiKey.isBlank() || service == null) {
            return emptyList()
        }

        return runCatching {
            service.searchFoods(
                apiKey = apiKey,
                request = UsdaFoodSearchRequest(query = query, pageSize = MAX_FOOD_SEARCH_RESULTS)
            ).foods.orEmpty()
                .sortedWith(
                    compareBy<UsdaFoodItem> { it.dataTypeRank() }
                        .thenByDescending { it.score ?: 0.0 }
                )
                .toUsdaFoodSearchResults()
                .dedupeByNameAndServing()
                .take(MAX_FOOD_SEARCH_RESULTS)
        }.getOrElse { emptyList() }
    }

    private fun dayDocument(uid: String, dateKey: String) =
        firestore.collection("users").document(uid)
            .collection("meal_logs").document(dateKey)

    private fun entriesCollection(uid: String, dateKey: String) =
        dayDocument(uid, dateKey).collection("entries")
}

private const val FOOD_SEARCH_TIMEOUT_MS = 5_000L
private const val MAX_FOOD_SEARCH_RESULTS = 40

private fun List<KaloFoodItem>.toKaloFoodSearchResults(): List<FoodSearchResult> {
    return mapNotNull { item ->
        val name = item.name?.trim().orEmpty()
        val calories = item.calories?.toInt()
        if (name.isBlank() || calories == null || calories <= 0) {
            null
        } else {
            FoodSearchResult(
                name = name,
                calories = calories,
                servingSize = item.serving?.trim().orEmpty().ifBlank { "1 serving" }
            )
        }
    }
}

private fun List<UsdaFoodItem>.toUsdaFoodSearchResults(): List<FoodSearchResult> {
    return mapNotNull { item -> item.toFoodSearchResult() }
}

private fun UsdaFoodItem.toFoodSearchResult(): FoodSearchResult? {
    val name = displayName()
    val caloriesPer100g = caloriesPer100g()
    if (name.isBlank() || caloriesPer100g == null || caloriesPer100g <= 0) {
        return null
    }

    val serving = preferredServing()
    val calories = (caloriesPer100g * serving.grams / 100.0).roundToInt()
    if (calories <= 0) {
        return null
    }

    return FoodSearchResult(
        name = name,
        calories = calories,
        servingSize = serving.label
    )
}

private fun UsdaFoodItem.displayName(): String {
    val descriptionText = description?.trim().orEmpty()
    val brandText = listOfNotNull(
        brandName?.trim()?.takeIf { it.isNotBlank() },
        brandOwner?.trim()?.takeIf { it.isNotBlank() }
    ).distinctBy { it.lowercase(Locale.getDefault()) }
        .firstOrNull()
        .orEmpty()

    return when {
        descriptionText.isBlank() -> brandText
        brandText.isBlank() -> descriptionText
        descriptionText.contains(brandText, ignoreCase = true) -> descriptionText
        else -> "$descriptionText ($brandText)"
    }
}

private fun UsdaFoodItem.caloriesPer100g(): Double? {
    val nutrients = foodNutrients.orEmpty()
    val kcal = nutrients.firstOrNull { nutrient ->
        nutrient.value != null &&
            nutrient.unitName.equals("KCAL", ignoreCase = true) &&
            (nutrient.nutrientId == 1008 || nutrient.nutrientNumber == "208")
    }?.value

    if (kcal != null) return kcal

    val kilojoules = nutrients.firstOrNull { nutrient ->
        nutrient.value != null &&
            nutrient.unitName.equals("kJ", ignoreCase = true) &&
            nutrient.nutrientName.equals("Energy", ignoreCase = true)
    }?.value

    return kilojoules?.div(4.184)
}

private fun UsdaFoodItem.preferredServing(): UsdaServing {
    val servingMeasure = foodMeasures.orEmpty()
        .filter { measure ->
            val text = measure.disseminationText?.trim().orEmpty()
            !text.equals("Quantity not specified", ignoreCase = true) &&
                text.isNotBlank() &&
                (measure.gramWeight ?: 0.0) > 0.0
        }
        .minWithOrNull(
            compareBy<UsdaFoodMeasure> { it.rank ?: Int.MAX_VALUE }
                .thenBy { it.disseminationText.orEmpty() }
        )

    if (servingMeasure != null) {
        val grams = servingMeasure.gramWeight ?: 100.0
        return UsdaServing(
            grams = grams,
            label = "${servingMeasure.disseminationText?.trim().orEmpty()} (${formatAmount(grams)} g)"
        )
    }

    val servingAmount = servingSize
    val servingUnit = servingSizeUnit?.trim().orEmpty()
    val servingUnitIsWeight = servingUnit.equals("g", ignoreCase = true) ||
        servingUnit.equals("gram", ignoreCase = true) ||
        servingUnit.equals("grams", ignoreCase = true)

    if (servingAmount != null && servingAmount > 0.0 && servingUnitIsWeight) {
        val householdText = householdServingFullText?.trim().orEmpty()
        val gramText = "${formatAmount(servingAmount)} g"
        return UsdaServing(
            grams = servingAmount,
            label = if (householdText.isBlank()) gramText else "$householdText ($gramText)"
        )
    }

    return UsdaServing(grams = 100.0, label = "100 g")
}

private fun UsdaFoodItem.dataTypeRank(): Int {
    return when (dataType?.trim()?.lowercase(Locale.getDefault())) {
        "survey (fndds)" -> 0
        "foundation" -> 1
        "sr legacy" -> 2
        "branded" -> 3
        else -> 4
    }
}

private data class UsdaServing(
    val grams: Double,
    val label: String
)

private fun formatAmount(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }
}

private fun List<FoodSearchResult>.filterByQuery(query: String): List<FoodSearchResult> {
    val normalized = query.lowercase(Locale.getDefault())
    return filter { result ->
        result.name.lowercase(Locale.getDefault()).contains(normalized)
    }
}

private fun List<FoodSearchResult>.dedupeByNameAndServing(): List<FoodSearchResult> {
    return distinctBy { "${it.name.lowercase(Locale.getDefault())}|${it.servingSize.lowercase(Locale.getDefault())}" }
        .sortedBy { it.name.lowercase(Locale.getDefault()) }
}
