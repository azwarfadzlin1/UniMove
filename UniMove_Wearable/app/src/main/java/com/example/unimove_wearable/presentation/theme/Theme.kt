package com.example.unimove_wearable.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

@Composable
fun UniMove_WearableTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = Colors(
            primary = Color(0xFFF3F4F6),
            primaryVariant = Color(0xFFB8C0CC),
            secondary = Color(0xFF79E6B3),
            secondaryVariant = Color(0xFF4DB383),
            background = Color.Black,
            surface = Color(0xFF111417),
            error = Color(0xFFFF6B6B),
            onPrimary = Color.Black,
            onSecondary = Color.Black,
            onBackground = Color(0xFFF3F4F6),
            onSurface = Color(0xFFF3F4F6),
            onSurfaceVariant = Color(0xFF8B95A1),
            onError = Color.Black
        ),
        content = content
    )
}
