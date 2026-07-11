package com.example.unimove.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.firebase.Timestamp

enum class MealType(val label: String, val icon: ImageVector) {
    BREAKFAST("Breakfast", Icons.Default.BakeryDining),
    LUNCH("Lunch", Icons.Default.LunchDining),
    DINNER("Dinner", Icons.Default.DinnerDining),
    SNACK("Snack", Icons.Default.Icecream);

    companion object {
        fun fromStorage(value: String?): MealType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: entries.firstOrNull { it.label.equals(value, ignoreCase = true) }
                ?: SNACK
        }
    }
}

enum class CalorieStatus {
    NO_TARGET,
    UNDER_TARGET,
    NEAR_TARGET,
    TARGET_REACHED,
    EXCEEDED
}

data class MealLogEntry(
    val id: String = "",
    val foodName: String = "",
    val calories: Int = 0,
    val quantity: Double = 1.0,
    val servingSize: String = "",
    val mealType: MealType = MealType.SNACK,
    val dateKey: String = "",
    val createdAt: Timestamp? = null
)

data class DailyMealSummary(
    val dateKey: String = "",
    val totalCalories: Int = 0,
    val totalItems: Int = 0,
    val updatedAt: Timestamp? = null
)

data class MealEntryDraft(
    val foodName: String,
    val calories: Int,
    val quantity: Double,
    val servingSize: String,
    val mealType: MealType
)

data class FoodSearchResult(
    val name: String,
    val calories: Int,
    val servingSize: String
)

data class CalorieProgress(
    val consumed: Int,
    val target: Int?,
    val progress: Float,
    val status: CalorieStatus,
    val headline: String,
    val supportingText: String
)

fun buildCalorieProgress(consumed: Int, target: Int?): CalorieProgress {
    if (target == null || target <= 0) {
        return CalorieProgress(
            consumed = consumed,
            target = null,
            progress = 0f,
            status = CalorieStatus.NO_TARGET,
            headline = "$consumed kcal today",
            supportingText = "Set your calorie target"
        )
    }

    val progress = (consumed.toFloat() / target.toFloat()).coerceAtLeast(0f)
    val remaining = (target - consumed).coerceAtLeast(0)

    return when {
        consumed > target -> CalorieProgress(
            consumed = consumed,
            target = target,
            progress = progress.coerceAtMost(1.15f),
            status = CalorieStatus.EXCEEDED,
            headline = "$consumed / $target kcal",
            supportingText = "Exceeded by ${consumed - target} kcal"
        )

        consumed == target -> CalorieProgress(
            consumed = consumed,
            target = target,
            progress = 1f,
            status = CalorieStatus.TARGET_REACHED,
            headline = "$consumed / $target kcal",
            supportingText = "Target reached!"
        )

        progress >= 0.8f -> CalorieProgress(
            consumed = consumed,
            target = target,
            progress = progress,
            status = CalorieStatus.NEAR_TARGET,
            headline = "$consumed / $target kcal",
            supportingText = "$remaining kcal remaining"
        )

        else -> CalorieProgress(
            consumed = consumed,
            target = target,
            progress = progress,
            status = CalorieStatus.UNDER_TARGET,
            headline = "$consumed / $target kcal",
            supportingText = "$remaining kcal remaining"
        )
    }
}
