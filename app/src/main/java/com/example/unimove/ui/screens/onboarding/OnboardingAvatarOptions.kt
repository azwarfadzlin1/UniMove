package com.example.unimove.ui.screens.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class OnboardingAvatarOption(
    val key: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tint: Color,
    val background: Color,
    val description: String
)

val onboardingAvatarOptions = listOf(
    OnboardingAvatarOption(
        key = "endurance",
        title = "Endurance",
        subtitle = "Pace & Distance",
        icon = Icons.Filled.DirectionsRun,
        tint = Color(0xFF8B5CF6), // Violet
        background = Color(0xFF8B5CF6),
        description = "Optimized for long-distance running and stamina."
    ),
    OnboardingAvatarOption(
        key = "sprinter",
        title = "Sprinter",
        subtitle = "High Speed",
        icon = Icons.Filled.Bolt,
        tint = Color(0xFF06B6D4), // Cyan
        background = Color(0xFF06B6D4),
        description = "Explosive energy for short, powerful bursts."
    ),
    OnboardingAvatarOption(
        key = "strength",
        title = "Strength",
        subtitle = "Power & Lifting",
        icon = Icons.Filled.FitnessCenter,
        tint = Color(0xFF10B981), // Emerald
        background = Color(0xFF10B981),
        description = "Focus on muscle building and physical dominance."
    ),
    OnboardingAvatarOption(
        key = "wellness",
        title = "Wellness",
        subtitle = "Mind & Body",
        icon = Icons.Filled.Favorite,
        tint = Color(0xFFEC4899), // Pink
        background = Color(0xFFEC4899),
        description = "Holistic balance between active movement and recovery."
    ),
    OnboardingAvatarOption(
        key = "cardio",
        title = "Cardio",
        subtitle = "Active Burn",
        icon = Icons.Filled.LocalFireDepartment,
        tint = Color(0xFFF59E0B), // Amber
        background = Color(0xFFF59E0B),
        description = "Fast-paced workouts to elevate the heart rate."
    ),
    OnboardingAvatarOption(
        key = "all_rounder",
        title = "All-Rounder",
        subtitle = "General Fitness",
        icon = Icons.Filled.AutoAwesome,
        tint = Color(0xFF6366F1), // Indigo
        background = Color(0xFF6366F1),
        description = "Adaptive style mixing cardio, strength, and endurance."
    )
)
