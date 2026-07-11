package com.example.unimove.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.unimove.ui.components.AuthScreenBackground
import com.example.unimove.ui.components.authTextFieldColors
import com.example.unimove.ui.theme.*
import com.example.unimove.ui.viewmodel.OnboardingViewModel

private val genderOptions = listOf("Male", "Female")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingHostScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: OnboardingViewModel = viewModel(factory = OnboardingViewModel.Factory(context))
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    val collegeIconRes = resolveCollegeLogoRes(state.college)

    var genderExpanded by remember { mutableStateOf(false) }
    var collegeExpanded by remember { mutableStateOf(false) }

    AuthScreenBackground {
        // Outer box: full screen, so we can pin the button at bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Scrollable content — stops before the bottom button area
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 88.dp) // leave space for bottom button
                    .verticalScroll(scrollState)
                    .padding(top = 32.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // ── Header ──────────────────────────────────────────────
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(AuthPrimary.copy(alpha = 0.12f), CircleShape)
                            .border(1.dp, AuthPrimary.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = AuthPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Text(
                        text = "Build Your Profile",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp
                        )
                    )
                    Text(
                        text = "Tell us a little about yourself to personalize your experience.",
                        color = AuthTextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                }

                // ── Choose Your Style ────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PROFILE PICTURE",
                            color = Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        val selectedOption = onboardingAvatarOptions.find { it.key == state.selectedIconKey }
                        AnimatedContent(
                            targetState = selectedOption?.title ?: "Choose one",
                            label = "selected_label"
                        ) { label ->
                            Text(
                                text = label,
                                color = AuthPrimary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 28.dp)
                    ) {
                        items(onboardingAvatarOptions) { option ->
                            AvatarCard(
                                option = option,
                                isSelected = state.selectedIconKey == option.key,
                                onClick = { viewModel.updateIcon(option.key) }
                            )
                        }
                    }

                    // Description chip below avatar row
                    val selectedOption = onboardingAvatarOptions.find { it.key == state.selectedIconKey }
                    AnimatedVisibility(
                        visible = selectedOption != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        selectedOption?.let {
                            Surface(
                                modifier = Modifier.padding(horizontal = 24.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = it.tint.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, it.tint.copy(alpha = 0.2f))
                            ) {
                                Text(
                                    text = it.description,
                                    color = it.tint.copy(alpha = 0.9f),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // ── Profile Data Form ────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                        // Name
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "YOUR NAME",
                                color = Color.White.copy(alpha = 0.4f),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            OutlinedTextField(
                                value = state.name,
                                onValueChange = { viewModel.updateName(it) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = authTextFieldColors(),
                                singleLine = true,
                                placeholder = {
                                    Text(
                                        "What should we call you?",
                                        color = Color.White.copy(alpha = 0.18f)
                                    )
                                }
                            )
                        }

                        // Age + Gender row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "AGE",
                                    color = Color.White.copy(alpha = 0.4f),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                OutlinedTextField(
                                    value = state.age,
                                    onValueChange = { viewModel.updateAge(it.filter { c -> c.isDigit() }) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = authTextFieldColors(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    placeholder = {
                                        Text("--", color = Color.White.copy(alpha = 0.18f))
                                    }
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1.6f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "GENDER",
                                    color = Color.White.copy(alpha = 0.4f),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                ExposedDropdownMenuBox(
                                    expanded = genderExpanded,
                                    onExpandedChange = { genderExpanded = !genderExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = state.gender,
                                        onValueChange = {},
                                        readOnly = true,
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = authTextFieldColors(),
                                        placeholder = {
                                            Text("Select", color = Color.White.copy(alpha = 0.18f))
                                        },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded)
                                        }
                                    )
                                    ExposedDropdownMenu(
                                        expanded = genderExpanded,
                                        onDismissRequest = { genderExpanded = false },
                                        modifier = Modifier.background(Color(0xFF141414))
                                    ) {
                                        genderOptions.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option, color = Color.White) },
                                                onClick = {
                                                    viewModel.updateGender(option)
                                                    genderExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // College
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = collegeIconRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    "COLLEGE",
                                    color = Color.White.copy(alpha = 0.4f),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }

                            ExposedDropdownMenuBox(
                                expanded = collegeExpanded,
                                onExpandedChange = { collegeExpanded = !collegeExpanded }
                            ) {
                                OutlinedTextField(
                                    value = state.college,
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = authTextFieldColors(),
                                    placeholder = {
                                        Text("Select your college", color = Color.White.copy(alpha = 0.18f))
                                    },
                                    leadingIcon = {
                                        Image(
                                            painter = painterResource(id = collegeIconRes),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = collegeExpanded)
                                    }
                                )
                                ExposedDropdownMenu(
                                    expanded = collegeExpanded,
                                    onDismissRequest = { collegeExpanded = false },
                                    modifier = Modifier.background(Color(0xFF141414))
                                ) {
                                    collegeOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Image(
                                                        painter = painterResource(id = option.logoRes),
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .clip(RoundedCornerShape(4.dp))
                                                    )
                                                    Text(option.name, color = Color.White)
                                                }
                                            },
                                            onClick = {
                                                viewModel.updateCollege(option.name)
                                                collegeExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Weight + Height row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "WEIGHT (KG)",
                                    color = Color.White.copy(alpha = 0.4f),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                OutlinedTextField(
                                    value = state.weight,
                                    onValueChange = {
                                        viewModel.updateWeight(it.filter { c -> c.isDigit() || c == '.' })
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = authTextFieldColors(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    placeholder = {
                                        Text("0.0", color = Color.White.copy(alpha = 0.18f))
                                    }
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "HEIGHT (CM)",
                                    color = Color.White.copy(alpha = 0.4f),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                OutlinedTextField(
                                    value = state.height,
                                    onValueChange = {
                                        viewModel.updateHeight(it.filter { c -> c.isDigit() || c == '.' })
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = authTextFieldColors(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    placeholder = {
                                        Text("000", color = Color.White.copy(alpha = 0.18f))
                                    }
                                )
                            }
                        }
                }

                // Error
                if (state.saveError != null) {
                    Text(
                        text = state.saveError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ── Pinned Bottom CTA ────────────────────────────────────────
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xFF080810).copy(alpha = 0.95f)),
                            startY = 0f,
                            endY = 120f
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.saveAndFinish {
                            navController.navigate("home") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    enabled = state.name.isNotBlank() &&
                        state.college.isNotBlank() &&
                        state.selectedIconKey.isNotBlank() &&
                        !state.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AuthPrimary,
                        disabledContainerColor = AuthPrimary.copy(alpha = 0.25f)
                    )
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "GET STARTED",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AvatarCard(
    option: OnboardingAvatarOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(if (isSelected) 1.06f else 1f, label = "scale")
    val borderAlpha by animateFloatAsState(if (isSelected) 1f else 0.32f, label = "borderAlpha")
    val bgAlpha by animateFloatAsState(if (isSelected) 0.16f else 0.09f, label = "bgAlpha")

    Surface(
        modifier = Modifier
            .size(width = 108.dp, height = 126.dp)
            .scale(scale)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = bgAlpha),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) option.tint else Color.White.copy(alpha = borderAlpha)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(option.tint.copy(alpha = if (isSelected) 0.3f else 0.12f), Color.Transparent)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = option.title,
                    tint = if (isSelected) option.tint else Color.White.copy(alpha = 0.88f),
                    modifier = Modifier.size(38.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = option.title,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
