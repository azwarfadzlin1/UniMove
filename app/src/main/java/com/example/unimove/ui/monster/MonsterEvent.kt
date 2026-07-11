package com.example.unimove.ui.monster

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.filament.View as FilamentView
import io.github.sceneview.createView
import io.github.sceneview.Scene
import io.github.sceneview.SurfaceType
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberView
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Position
import io.github.sceneview.rememberModelLoader

@Composable
fun MonsterEventScreen(
    onBack: () -> Unit = {},
    onStartEvent: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Monster Event Preview",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Testing 3D model: animal-bee.glb",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(20.dp))

        BeeModelScene(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp),
            backgroundColor = Color(0xFFEFEFEF)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onBack) {
                Text("Back")
            }
            Button(
                onClick = onStartEvent,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7CF6))
            ) {
                Text("Start Event")
            }
        }
    }
}

@Composable
fun BeeModelScene(
    modifier: Modifier = Modifier,
    modelAsset: String = "models/animal-bee.glb",
    backgroundColor: Color = Color.Transparent,
    scaleToUnits: Float = 1.85f,
    cameraZ: Float = 4.8f,
    centerOriginY: Float = -0.5f,
    lightIntensity: Float = 120_000.0f,
    rotationDurationMillis: Int = 12_000,
    showLoading: Boolean = true
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Position(z = cameraZ)
    }
    val mainLightNode = rememberMainLightNode(engine) {
        intensity = lightIntensity
    }
    val view = rememberView(engine) {
        createView(engine).apply {
            blendMode = if (backgroundColor == Color.Transparent) {
                FilamentView.BlendMode.TRANSLUCENT
            } else {
                FilamentView.BlendMode.OPAQUE
            }
        }
    }
    val modelInstance by produceState<io.github.sceneview.model.ModelInstance?>(
        initialValue = null,
        key1 = modelLoader,
        key2 = modelAsset
    ) {
        value = runCatching {
            modelLoader.createModelInstance(modelAsset)
        }.getOrNull()
    }
    val infiniteTransition = rememberInfiniteTransition(label = "monster_rotation")
    val rotationY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = rotationDurationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "monster_rotation_y"
    )
    val isTransparentBackground = backgroundColor == Color.Transparent

    Box(
        modifier = if (isTransparentBackground) {
            modifier
        } else {
            modifier.background(backgroundColor)
        }
    ) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            surfaceType = SurfaceType.TextureSurface,
            engine = engine,
            view = view,
            modelLoader = modelLoader,
            cameraNode = cameraNode,
            mainLightNode = mainLightNode,
            isOpaque = !isTransparentBackground
        ) {
            modelInstance?.let { loadedModel ->
                ModelNode(
                    modelInstance = loadedModel,
                    scaleToUnits = scaleToUnits,
                    autoAnimate = true,
                    centerOrigin = Position(y = centerOriginY),
                    rotation = Rotation(y = rotationY)
                )
            }
        }

        if (showLoading && modelInstance == null) {
            Text(
                text = "Loading 3D model...",
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color(0xAAFFFFFF))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MonsterEncounterDialog(
    creatureName: String,
    modelAsset: String,
    distanceMeters: Float?,
    isSaving: Boolean,
    catchEnabled: Boolean,
    onCatch: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF9F1DF),
        contentColor = Color(0xFF172033),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        tonalElevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = Color(0xFFFFE5C2),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
            ) {
                Text(
                    text = "Unknown monster revealed",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = Color(0xFF172033),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Text(
                text = creatureName,
                color = Color(0xFF172033),
                fontSize = 25.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                text = distanceMeters?.let { "${it.toInt()} m from you" } ?: "Close to your location",
                color = Color(0xFF172033).copy(alpha = 0.68f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentAlignment = Alignment.Center
            ) {
                BeeModelScene(
                    modifier = Modifier.fillMaxSize(),
                    modelAsset = modelAsset,
                    backgroundColor = Color.Transparent,
                    scaleToUnits = 1.95f,
                    cameraZ = 5.0f,
                    centerOriginY = -0.5f,
                    lightIntensity = 150_000.0f
                )
            }

            Text(
                text = "Catch and store this creature in your Monster Event collection.",
                color = Color(0xFF172033).copy(alpha = 0.78f),
                fontSize = 14.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss,
                    enabled = !isSaving
                ) {
                    Text("Not now", color = Color(0xFF172033))
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onCatch,
                    enabled = catchEnabled && !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE87920))
                ) {
                    Text(
                        text = if (isSaving) "Saving..." else "Catch",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
