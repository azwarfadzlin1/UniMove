package com.example.unimove.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.example.unimove.ui.screens.Milestone
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.unimove.ui.monster.BeeModelScene
import com.example.unimove.ui.screens.onboarding.collegeOptions
import com.example.unimove.ui.screens.onboarding.onboardingAvatarOptions
import com.example.unimove.ui.screens.onboarding.resolveCollegeLogoRes
import com.example.unimove.ui.theme.*
import com.example.unimove.ui.viewmodel.AuthViewModel
import com.example.unimove.ui.viewmodel.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private data class CapturedMonster(
    val creatureId: String,
    val creatureName: String,
    val modelAsset: String,
    val caughtAt: Timestamp?
)

@Composable
fun ProfileScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val totals by authViewModel.totals.collectAsStateWithLifecycle()
    val totalDistance = totals.totalDistanceKm
    val profile = uiState.profile
    var showEditDialog by remember { mutableStateOf(false) }
    var isUpdatingProfile by remember { mutableStateOf(false) }
    var profileUpdateError by remember { mutableStateOf<String?>(null) }
    var capturedMonsters by remember { mutableStateOf<List<CapturedMonster>>(emptyList()) }
    var selectedMonsterId by rememberSaveable { mutableStateOf<String?>(null) }
    var collectionError by remember { mutableStateOf<String?>(null) }
    val collegeIconRes = resolveCollegeLogoRes(profile?.college)
    // Firestore ownership rules use request.auth.uid, so always prefer the same
    // Firebase Auth source when selecting the profile collection path.
    val uid = FirebaseAuth.getInstance().currentUser?.uid
        ?: uiState.user?.uid
        ?: profile?.uid?.takeIf { it.isNotBlank() }

    LaunchedEffect(uid) {
        if (uid.isNullOrBlank()) {
            capturedMonsters = emptyList()
            selectedMonsterId = null
            return@LaunchedEffect
        }

        val firestore = FirebaseFirestore.getInstance()
        val nestedResult = runCatching {
            firestore
                .collection("users").document(uid)
                .collection("monster_collection")
                .get()
                .await()
                .documents.mapNotNull(::capturedMonsterFromDocument)
        }
        val legacyResult = runCatching {
            firestore.collection("monster_event_catches")
                .whereEqualTo("userId", uid)
                .get()
                .await()
                .documents.mapNotNull(::capturedMonsterFromDocument)
        }

        val monsters = (nestedResult.getOrDefault(emptyList()) + legacyResult.getOrDefault(emptyList()))
            .groupBy { it.creatureId }
            .mapNotNull { (_, captures) -> captures.maxByOrNull { it.caughtAt?.seconds ?: 0L } }
            .sortedByDescending { it.caughtAt?.seconds ?: 0L }

        if (nestedResult.isSuccess || legacyResult.isSuccess) {
            capturedMonsters = monsters
            selectedMonsterId = selectedMonsterId?.takeIf { id -> monsters.any { it.creatureId == id } }
                ?: monsters.firstOrNull()?.creatureId
            collectionError = null
        } else {
            capturedMonsters = emptyList()
            selectedMonsterId = null
            val error = nestedResult.exceptionOrNull() ?: legacyResult.exceptionOrNull()
            collectionError = error?.message ?: "Failed to load captured monsters"
        }
    }

    // Fallback to "run_icon" if no avatarKey is found
    val selectedAvatarKey = profile?.avatarKey?.ifBlank { "run_icon" } ?: "run_icon"
    val avatarOption = onboardingAvatarOptions.firstOrNull { it.key == selectedAvatarKey }
        ?: onboardingAvatarOptions.first()

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(SurfaceLight, SurfaceLightEnd)
                    )
                )
        ) {
            // Decorative background elements
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .offset(x = (-100).dp, y = (-50).dp)
                    .background(avatarOption.tint.copy(alpha = 0.05f), CircleShape)
            )



            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Custom Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CardSurface.copy(alpha = 0.8f))
                            .border(1.dp, BorderWhite.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }

                    Text(
                        text = "Account Profile",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = (-0.5).sp
                    )

                    IconButton(
                        onClick = {
                            profileUpdateError = null
                            showEditDialog = true
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CardSurface.copy(alpha = 0.8f))
                            .border(1.dp, BorderWhite.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Avatar Section with Enhanced Design
                Box(
                    modifier = Modifier
                        .padding(top = 24.dp, bottom = 20.dp)
                        .size(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Animated-like Outer Ring
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(2.dp, Brush.sweepGradient(listOf(avatarOption.tint.copy(alpha = 0.1f), avatarOption.tint, avatarOption.tint.copy(alpha = 0.1f))), CircleShape)
                            .padding(6.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp)
                            .shadow(20.dp, CircleShape, spotColor = avatarOption.tint)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(MeshGradient1, MeshGradient2)
                                )
                            )
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(avatarOption.background.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = avatarOption.icon,
                            contentDescription = "Avatar",
                            tint = avatarOption.tint,
                            modifier = Modifier.size(70.dp)
                        )
                    }
                }

                Text(
                    text = profile?.name?.ifBlank { "Unimove Runner" } ?: "Unimove Runner",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    letterSpacing = (-0.8).sp
                )

                Surface(
                    color = avatarOption.tint.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = avatarOption.title,
                        fontSize = 13.sp,
                        color = avatarOption.tint,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Modern Detail Cards
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    // College Showcase Logo (Combined with background)
                    if (collegeIconRes != com.example.unimove.R.drawable.ic_college) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = collegeIconRes),
                                contentDescription = "College Logo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    DetailSection(title = "Information") {
                        ProfileRow(
                            icon = Icons.Default.Email,
                            label = "Email Address",
                            value = profile?.email?.ifBlank { "Not connected" } ?: "Not connected"
                        )
                        ProfileRow(
                            icon = Icons.Default.Person,
                            label = "Gender",
                            value = profile?.gender?.ifBlank { "Not specified" } ?: "Not specified"
                        )
                        ProfileRow(
                            icon = Icons.Default.Cake,
                            label = "Age",
                            value = profile?.age?.let { "$it years" } ?: "Not set"
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    DetailSection(title = "Physical Stats") {
                        ProfileRow(
                            icon = Icons.Default.Height,
                            label = "Height",
                            value = profile?.height?.let { "$it cm" } ?: "Not set"
                        )
                        ProfileRow(
                            icon = Icons.Default.MonitorWeight,
                            label = "Weight",
                            value = profile?.weight?.let { "$it kg" } ?: "Not set",
                            isLast = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    RunningGoalSection(
                        achievedKm = totalDistance,
                        goalKm = profile?.runGoalKm,
                        onEdit = { showEditDialog = true }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    ProfileBadgesSection(totalDistance = totalDistance)

                    Spacer(modifier = Modifier.height(24.dp))

                    CapturedMonsterSection(
                        monsters = capturedMonsters,
                        selectedMonsterId = selectedMonsterId,
                        errorMessage = collectionError,
                        onSelectMonster = { selectedMonsterId = it }
                    )

                    if (profileUpdateError != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = profileUpdateError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Redesigned Logout Button
                    OutlinedButton(
                        onClick = {
                            authViewModel.signOut()
                            navController.navigate("landing") {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Red.copy(alpha = 0.8f)
                        ),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Sign Out",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }

    if (showEditDialog && profile != null) {
        EditProfileDialog(
            profile = profile,
            isSaving = isUpdatingProfile,
            onDismiss = {
                if (!isUpdatingProfile) showEditDialog = false
            },
            onSave = { name, gender, college, ageRaw, heightRaw, weightRaw, runGoalRaw ->
                val age = ageRaw.trim().toIntOrNull()
                val height = heightRaw.trim().toDoubleOrNull()
                val weight = weightRaw.trim().toDoubleOrNull()
                val runGoalKm = runGoalRaw.trim().toDoubleOrNull()

                isUpdatingProfile = true
                profileUpdateError = null
                authViewModel.updateProfileDetails(
                    name = name,
                    gender = gender,
                    college = college,
                    age = age,
                    height = height,
                    weight = weight,
                    runGoalKm = runGoalKm,
                    onSuccess = {
                        isUpdatingProfile = false
                        showEditDialog = false
                    },
                    onError = { message ->
                        isUpdatingProfile = false
                        profileUpdateError = message
                    }
                )
            }
        )
    }
}

private fun capturedMonsterFromDocument(
    doc: com.google.firebase.firestore.DocumentSnapshot
): CapturedMonster? {
    val creatureId = doc.getString("creatureId") ?: doc.id.substringAfterLast('_')
    val creatureName = doc.getString("creatureName") ?: return null
    val modelAsset = doc.getString("modelAsset") ?: return null
    return CapturedMonster(
        creatureId = creatureId,
        creatureName = creatureName,
        modelAsset = modelAsset,
        caughtAt = doc.getTimestamp("caughtAt")
    )
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
            letterSpacing = 0.5.sp
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
private fun RunningGoalSection(
    achievedKm: Double,
    goalKm: Double?,
    onEdit: () -> Unit
) {
    val validGoal = goalKm?.takeIf { it > 0.0 }
    val progress = validGoal?.let { (achievedKm / it).coerceIn(0.0, 1.0).toFloat() } ?: 0f

    Column {
        Text(
            text = "Running Goal",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
            letterSpacing = 0.5.sp
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEdit)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (validGoal == null) "Set a distance goal" else "${"%.1f".format(achievedKm)} of ${"%.1f".format(validGoal)} km",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (validGoal == null) "Choose the target shown on Home" else "${(progress * 100).toInt()}% achieved",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit running goal",
                    tint = AccentBlue
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = AccentBlue,
                trackColor = AccentBlue.copy(alpha = 0.12f)
            )
        }
    }
}

@Composable
private fun ProfileRow(
    icon: ImageVector,
    label: String,
    value: String,
    isLast: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentBlue.copy(alpha = 0.78f),
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(18.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    fontSize = 15.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }

        if (!isLast) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                modifier = Modifier.padding(start = 40.dp),
                color = BorderWhite.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )
        }
    }
}

@Composable
private fun ProfileRowPainter(
    iconRes: Int,
    label: String,
    value: String,
    isLast: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AccentBlue.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = AccentBlue.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    fontSize = 15.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }

        if (!isLast) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                modifier = Modifier.padding(start = 52.dp),
                color = BorderWhite.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileDialog(
    profile: UserProfile,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, gender: String, college: String, age: String, height: String, weight: String, runGoalKm: String) -> Unit
) {
    var name by rememberSaveable(profile.uid) { mutableStateOf(profile.name) }
    var gender by rememberSaveable(profile.uid) { mutableStateOf(profile.gender) }
    var college by rememberSaveable(profile.uid) { mutableStateOf(profile.college) }
    var age by rememberSaveable(profile.uid) { mutableStateOf(profile.age?.toString().orEmpty()) }
    var height by rememberSaveable(profile.uid) { mutableStateOf(profile.height?.toString().orEmpty()) }
    var weight by rememberSaveable(profile.uid) { mutableStateOf(profile.weight?.toString().orEmpty()) }
    var runGoalKm by rememberSaveable(profile.uid) { mutableStateOf(profile.runGoalKm?.toString().orEmpty()) }

    var genderExpanded by remember { mutableStateOf(false) }
    var collegeExpanded by remember { mutableStateOf(false) }
    val genderOptions = listOf("Male", "Female")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Profile",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = genderExpanded,
                    onExpandedChange = { genderExpanded = !genderExpanded }
                ) {
                    OutlinedTextField(
                        value = gender,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Gender") },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = genderExpanded,
                        onDismissRequest = { genderExpanded = false }
                    ) {
                        genderOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    gender = option
                                    genderExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = collegeExpanded,
                    onExpandedChange = { collegeExpanded = !collegeExpanded }
                ) {
                    OutlinedTextField(
                        value = college,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("College") },
                        leadingIcon = {
                            Image(
                                painter = painterResource(id = resolveCollegeLogoRes(college)),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = collegeExpanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = collegeExpanded,
                        onDismissRequest = { collegeExpanded = false }
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
                                        Text(option.name)
                                    }
                                },
                                onClick = {
                                    college = option.name
                                    collegeExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = age,
                        onValueChange = { age = it.filter { ch -> ch.isDigit() } },
                        singleLine = true,
                        label = { Text("Age") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = height,
                        onValueChange = { height = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        singleLine = true,
                        label = { Text("Height (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    singleLine = true,
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = runGoalKm,
                    onValueChange = { runGoalKm = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    singleLine = true,
                    label = { Text("Distance goal (km)") },
                    supportingText = { Text("Your all-time running target shown on Home") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("Cancel")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name,
                        gender,
                        college,
                        age,
                        height,
                        weight,
                        runGoalKm
                    )
                },
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save")
                }
            }
        }
    )
}

@Composable
private fun CapturedMonsterSection(
    monsters: List<CapturedMonster>,
    selectedMonsterId: String?,
    errorMessage: String?,
    onSelectMonster: (String) -> Unit
) {
    val selectedMonster = monsters.firstOrNull { it.creatureId == selectedMonsterId }
        ?: monsters.firstOrNull()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Captured Monsters",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp),
            letterSpacing = 0.5.sp
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when {
                errorMessage != null -> {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                }

                selectedMonster == null -> {
                    Text(
                        text = "No captured monsters yet. Join MoveMon Hunt and catch one near a colored marker.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }

                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedMonster.creatureName,
                                color = TextPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp
                            )
                            Text(
                                text = "${monsters.size} captured in your collection",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Surface(
                            color = AccentBlue.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = "3D",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = AccentBlue,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BeeModelScene(
                            modifier = Modifier.fillMaxSize(),
                            modelAsset = selectedMonster.modelAsset,
                            backgroundColor = Color.Transparent,
                            scaleToUnits = 1.75f,
                            cameraZ = 5.0f,
                            centerOriginY = -0.5f,
                            lightIntensity = 150_000.0f,
                            showLoading = false
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(monsters, key = { it.creatureId }) { monster ->
                            CapturedMonsterChip(
                                monster = monster,
                                selected = monster.creatureId == selectedMonster.creatureId,
                                onClick = { onSelectMonster(monster.creatureId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CapturedMonsterChip(
    monster: CapturedMonster,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = if (selected) AccentBlue.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.72f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) AccentBlue.copy(alpha = 0.55f) else BorderWhite.copy(alpha = 0.4f)
        )
    ) {
        Text(
            text = monster.creatureName.removeSuffix(" Monster"),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = if (selected) AccentBlue else TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ProfileBadgesSection(totalDistance: Double) {
    val milestones = listOf(
        Milestone("1 km", "First Steps", 1.0, Icons.Rounded.DirectionsWalk),
        Milestone("5 km", "5K Finisher", 5.0, Icons.Rounded.Timer),
        Milestone("10 km", "Active Runner", 10.0, Icons.Rounded.Bolt),
        Milestone("20 km", "Half Pro", 20.0, Icons.Rounded.MilitaryTech),
        Milestone("50 km", "Distance Master", 50.0, Icons.Rounded.EmojiEvents),
        Milestone("100 km", "Legendary", 100.0, Icons.Rounded.Stars)
    )

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Trophy Cabinet",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp),
            letterSpacing = 0.5.sp
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            items(milestones) { milestone ->
                val isUnlocked = totalDistance >= milestone.distance
                ProfileBadgeCard(milestone = milestone, isUnlocked = isUnlocked)
            }
        }
    }
}

@Composable
private fun ProfileBadgeCard(milestone: Milestone, isUnlocked: Boolean) {
    val primaryColor = AccentBlue // Use theme accent color
    
    Card(
        modifier = Modifier
            .width(100.dp)
            .height(110.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) primaryColor.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = if (isUnlocked) BorderStroke(1.5.dp, primaryColor.copy(alpha = 0.4f)) else null
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = milestone.icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isUnlocked) primaryColor else Color.Gray.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                milestone.title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1
            )
            Text(
                milestone.label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = if (isUnlocked) primaryColor else Color.Gray
            )
        }
    }
}
