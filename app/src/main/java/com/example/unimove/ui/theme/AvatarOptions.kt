package com.example.unimove.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class AvatarOption(
    val key: String,
    val icon: ImageVector,
    val tint: Color
)

val avatarOptions = listOf(
    AvatarOption("default", Icons.Default.Person, Color.Gray)
)

// Removed fallback colors to prevent compilation conflict
