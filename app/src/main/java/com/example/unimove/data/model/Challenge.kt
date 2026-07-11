package com.example.unimove.data.model

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.Timestamp

data class Challenge(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val targetDistanceKm: Double = 0.0,
    val rewardLabel: String = "",
    val badgeText: String = "Race Mode",
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null,
    val isActive: Boolean = true
)

fun challengeFromDocument(doc: DocumentSnapshot): Challenge? {
    val title = doc.getString("title").orEmpty()
    val description = doc.getString("description").orEmpty()
    val targetDistanceKm = doc.getDouble("targetDistanceKm")
        ?: doc.getDouble("targetKm")
        ?: doc.getDouble("distanceKm")
        ?: doc.getLong("targetDistanceKm")?.toDouble()
        ?: doc.getLong("targetKm")?.toDouble()
        ?: doc.getLong("distanceKm")?.toDouble()
        ?: 0.0

    return Challenge(
        id = doc.id,
        title = title,
        description = description,
        targetDistanceKm = targetDistanceKm,
        rewardLabel = doc.getString("rewardLabel").orEmpty(),
        badgeText = doc.getString("badgeText").orEmpty().ifBlank { "Race Mode" },
        startDate = doc.getTimestamp("startDate"),
        endDate = doc.getTimestamp("endDate"),
        isActive = doc.getBoolean("isActive") ?: true
    )
}
