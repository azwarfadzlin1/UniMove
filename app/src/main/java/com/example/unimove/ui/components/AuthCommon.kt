package com.example.unimove.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unimove.ui.theme.*

@Composable
fun OrDivider() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
        Text(
            text = "OR",
            color = AuthTextSecondary.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
    }
}

@Composable
fun authTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White.copy(alpha = 0.9f),
    focusedBorderColor = AuthPrimary.copy(alpha = 0.6f),
    unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
    cursorColor = AuthPrimary,
    focusedLabelColor = AuthPrimary.copy(alpha = 0.8f),
    unfocusedLabelColor = AuthTextSecondary.copy(alpha = 0.7f),
    focusedContainerColor = AuthInputFocused,
    unfocusedContainerColor = AuthInputBackground,
    errorBorderColor = AuthTertiary.copy(alpha = 0.5f),
    errorLabelColor = AuthTertiary,
    errorContainerColor = AuthInputBackground
)

@Composable
fun SocialButton(
    text: String,
    @DrawableRes iconResId: Int,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(60.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        color = Color.White.copy(alpha = 0.03f)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                modifier = Modifier.align(Alignment.CenterStart).size(24.dp),
                tint = Color.Unspecified
            )
            Text(
                text = text,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AuthScreenBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Deep Mesh Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(MeshGradient1.copy(alpha = 0.15f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(x = 0f, y = 0f),
                        radius = 2000f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(MeshGradient2.copy(alpha = 0.12f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(x = 1000f, y = 1000f),
                        radius = 1500f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(MeshGradient4.copy(alpha = 0.1f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(x = 0f, y = 2000f),
                        radius = 1800f
                    )
                )
        )

        content()
    }
}
