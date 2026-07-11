package com.example.unimove.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.unimove.data.model.CalorieStatus
import com.example.unimove.data.model.FoodSearchResult
import com.example.unimove.data.model.MealEntryDraft
import com.example.unimove.data.model.MealLogEntry
import com.example.unimove.data.model.MealType
import com.example.unimove.data.model.buildCalorieProgress
import com.example.unimove.ui.theme.AccentBlue
import com.example.unimove.ui.theme.AccentBlueSoft
import com.example.unimove.ui.theme.CalorieAmber
import com.example.unimove.ui.theme.CalorieAmberBg
import com.example.unimove.ui.theme.CalorieAmberSoft
import com.example.unimove.ui.theme.CardSurface
import com.example.unimove.ui.theme.DividerColor
import com.example.unimove.ui.theme.SurfaceLight
import com.example.unimove.ui.theme.SurfaceLightEnd
import com.example.unimove.ui.theme.TextPrimary
import com.example.unimove.ui.theme.TextSecondary
import com.example.unimove.ui.viewmodel.AuthViewModel
import com.example.unimove.ui.viewmodel.DateItem
import com.example.unimove.ui.viewmodel.MealLogViewModel
import com.example.unimove.ui.viewmodel.canNavigateToNextMealDate
import com.example.unimove.ui.viewmodel.malaysiaTodayDateKey
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun MealLogScreen(
    navController: NavController,
    mealLogViewModel: MealLogViewModel,
    authViewModel: AuthViewModel
) {
    val uiState by mealLogViewModel.uiState.collectAsStateWithLifecycle()
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val dailyTarget = authUiState.profile?.dailyCalorieTarget
    val groupedEntries = remember(uiState.entries) {
        MealType.entries.associateWith { type -> uiState.entries.filter { it.mealType == type } }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showTargetDialog by rememberSaveable { mutableStateOf(false) }
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<MealLogEntry?>(null) }
    var deleteEntry by remember { mutableStateOf<MealLogEntry?>(null) }
    val userUid = authUiState.user?.uid

    LaunchedEffect(dailyTarget) {
        if (dailyTarget == null || dailyTarget <= 0) {
            showTargetDialog = true
        }
    }

    LaunchedEffect(userUid) {
        if (userUid != null) {
            mealLogViewModel.refreshForAuthenticatedUser()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            mealLogViewModel.dismissError()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = AccentBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add meal")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(SurfaceLight, SurfaceLightEnd)))
                .padding(innerPadding)
        ) {
            MealLogHeader(
                onBack = { navController.popBackStack() },
                onEditTarget = { showTargetDialog = true }
            )

            DateNavigationRow(
                displayDateLabel = uiState.displayDateLabel,
                selectedDateKey = uiState.selectedDateKey,
                canGoNext = canNavigateToNextMealDate(uiState.selectedDateKey),
                onPrevious = { mealLogViewModel.goToPreviousDate() },
                onNext = { mealLogViewModel.goToNextDate() },
                onToday = { mealLogViewModel.jumpToToday() }
            )

            CalendarStrip(
                items = uiState.calendarStrip,
                selectedDateKey = uiState.selectedDateKey,
                onDateSelected = { dateKey ->
                    if (dateKey <= malaysiaTodayDateKey()) {
                        mealLogViewModel.observeDate(dateKey)
                    }
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = AccentBlue,
                        strokeWidth = 3.dp
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            DailySummaryCard(
                                totalCalories = uiState.summary.totalCalories,
                                totalItems = uiState.summary.totalItems,
                                dailyTarget = dailyTarget
                            )
                        }

                        if (dailyTarget == null || dailyTarget <= 0) {
                            item {
                                MissingTargetCard(onSetTarget = { showTargetDialog = true })
                            }
                        }

                        item {
                            Text(
                                text = "Daily Meal Log",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                            )
                        }

                        if (uiState.entries.isEmpty()) {
                            item { EmptyMealState(onAddClick = { showAddSheet = true }) }
                        } else {
                            MealType.entries.forEach { mealType ->
                                val entries = groupedEntries[mealType].orEmpty()
                                if (entries.isNotEmpty()) {
                                    item {
                                        MealSection(
                                            mealType = mealType,
                                            entries = entries,
                                            onEdit = { entry -> editingEntry = entry },
                                            onDelete = { entry -> deleteEntry = entry }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTargetDialog) {
        SetCalorieTargetDialog(
            currentTarget = dailyTarget,
            onDismiss = { showTargetDialog = false },
            onSave = { target ->
                authViewModel.updateDailyCalorieTarget(
                    targetCalories = target,
                    onSuccess = {
                        showTargetDialog = false
                        scope.launch { snackbarHostState.showSnackbar("Calorie target updated") }
                    },
                    onError = { message ->
                        scope.launch { snackbarHostState.showSnackbar(message) }
                    }
                )
            }
        )
    }

    if (showAddSheet || editingEntry != null) {
        AddFoodSheet(
            initialEntry = editingEntry,
            isSaving = uiState.isSaving,
            isSearching = uiState.isSearching,
            searchResults = uiState.searchResults,
            onDismiss = {
                showAddSheet = false
                editingEntry = null
                mealLogViewModel.clearSearchResults()
            },
            onSearch = { query -> mealLogViewModel.searchFoods(query) },
            onSave = { draft, existingEntryId ->
                mealLogViewModel.saveEntry(
                    draft = draft,
                    existingEntryId = existingEntryId,
                    onSuccess = {
                        showAddSheet = false
                        editingEntry = null
                        scope.launch { snackbarHostState.showSnackbar("Meal saved") }
                    },
                    onError = { message ->
                        scope.launch { snackbarHostState.showSnackbar(message) }
                    }
                )
            }
        )
    }

    deleteEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleteEntry = null },
            title = { Text("Delete meal entry") },
            text = { Text("Remove ${entry.foodName} from this day?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        mealLogViewModel.deleteEntry(
                            entryId = entry.id,
                            onSuccess = {
                                deleteEntry = null
                                scope.launch { snackbarHostState.showSnackbar("Meal deleted") }
                            },
                            onError = { message ->
                                scope.launch { snackbarHostState.showSnackbar(message) }
                            }
                        )
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD14343))
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteEntry = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = CardSurface
        )
    }
}

@Composable
private fun DateNavigationRow(
    displayDateLabel: String,
    selectedDateKey: String,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPrevious,
            modifier = Modifier
                .size(40.dp)
                .background(CardSurface, CircleShape)
        ) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous day", tint = TextPrimary)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedContent(targetState = displayDateLabel, label = "meal_date_label") { label ->
                Text(
                    text = label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
            }
            Text(
                text = selectedDateKey,
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            TextButton(onClick = onToday) {
                Text("Today", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        IconButton(
            onClick = onNext,
            enabled = canGoNext,
            modifier = Modifier
                .size(40.dp)
                .background(CardSurface, CircleShape)
        ) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Next day",
                tint = if (canGoNext) TextPrimary else TextSecondary.copy(alpha = 0.35f)
            )
        }
    }
}

@Composable
private fun MealLogHeader(
    onBack: () -> Unit,
    onEditTarget: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp)
                .background(CardSurface, CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary, modifier = Modifier.size(20.dp))
        }

        Text(
            text = "Meal Tracker",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        IconButton(
            onClick = onEditTarget,
            modifier = Modifier
                .size(40.dp)
                .background(CardSurface, CircleShape)
        ) {
            Icon(Icons.Default.Tune, contentDescription = "Settings", tint = TextPrimary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun CalendarStrip(
    items: List<DateItem>,
    selectedDateKey: String,
    onDateSelected: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val todayDateKey = malaysiaTodayDateKey()
    
    LaunchedEffect(items, selectedDateKey) {
        val index = items.indexOfFirst { it.dateKey == selectedDateKey }
        if (index >= 0) {
            listState.animateScrollToItem((index - 2).coerceAtLeast(0))
        }
    }

    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items) { item ->
            val isSelected = item.dateKey == selectedDateKey
            val isFuture = item.dateKey > todayDateKey
            val backgroundColor by animateColorAsState(if (isSelected) AccentBlue else CardSurface, label = "")
            val contentColor by animateColorAsState(if (isSelected) Color.White else TextPrimary, label = "")
            val subColor by animateColorAsState(if (isSelected) Color.White.copy(alpha = 0.8f) else TextSecondary, label = "")
            val scale by animateFloatAsState(if (isSelected) 1.06f else 1f, label = "date_scale")

            Column(
                modifier = Modifier
                    .width(54.dp)
                    .scale(scale)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isFuture) CardSurface.copy(alpha = 0.55f) else backgroundColor)
                    .clickable(enabled = !isFuture) { onDateSelected(item.dateKey) }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.dayOfWeek,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isFuture) TextSecondary.copy(alpha = 0.45f) else subColor
                )
                Text(
                    text = item.dayOfMonth,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isFuture) TextSecondary.copy(alpha = 0.45f) else contentColor
                )
                if (item.isToday) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(if (isSelected) Color.White else AccentBlue, CircleShape)
                    )
                } else {
                    Spacer(modifier = Modifier.size(4.dp))
                }
            }
        }
    }
}

@Composable
private fun DailySummaryCard(
    totalCalories: Int,
    totalItems: Int,
    dailyTarget: Int?
) {
    val progressData = buildCalorieProgress(totalCalories, dailyTarget)
    val statusColor = progressColor(progressData.status)
    val animatedProgress by animateFloatAsState(
        targetValue = progressData.progress.coerceIn(0f, 1f),
        label = "progress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(CalorieAmberBg.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalDining,
                            contentDescription = null,
                            tint = CalorieAmber,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Daily Progress",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "$totalItems items recorded",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                StatusChip(status = progressData.status)
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = progressData.headline,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    if (dailyTarget != null) {
                        Text(
                            text = "${(animatedProgress * 100).toInt()}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
                
                Text(
                    text = progressData.supportingText,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color = statusColor,
                trackColor = statusColor.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
private fun MealSection(
    mealType: MealType,
    entries: List<MealLogEntry>,
    onEdit: (MealLogEntry) -> Unit,
    onDelete: (MealLogEntry) -> Unit
) {
    val sectionCalories = entries.sumOf { it.calories }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = mealType.icon,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = mealType.label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Text(
                text = "$sectionCalories kcal",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                entries.forEachIndexed { index, entry ->
                    MealEntryRow(
                        entry = entry,
                        onEdit = { onEdit(entry) },
                        onDelete = { onDelete(entry) }
                    )
                    if (index != entries.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = DividerColor.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MealEntryRow(
    entry: MealLogEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.foodName,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${trimQuantity(entry.quantity)} × ${entry.servingSize}",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "${entry.calories} kcal",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(end = 4.dp)
            )
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun StatusChip(status: CalorieStatus) {
    val label = when (status) {
        CalorieStatus.NO_TARGET -> "No Target"
        CalorieStatus.UNDER_TARGET -> "Healthy"
        CalorieStatus.NEAR_TARGET -> "Warning"
        CalorieStatus.TARGET_REACHED -> "Perfect"
        CalorieStatus.EXCEEDED -> "Limit Exceeded"
    }
    val color = progressColor(status)

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = CircleShape,
        border = BorderStroke(width = 1.dp, color = color.copy(alpha = 0.2f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

private fun progressColor(status: CalorieStatus): Color {
    return when (status) {
        CalorieStatus.NO_TARGET -> TextSecondary
        CalorieStatus.UNDER_TARGET -> AccentBlue
        CalorieStatus.NEAR_TARGET -> CalorieAmber
        CalorieStatus.TARGET_REACHED -> Color(0xFF4CAF50)
        CalorieStatus.EXCEEDED -> Color(0xFFD32F2F)
    }
}

@Composable
private fun MissingTargetCard(onSetTarget: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AccentBlueSoft.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = AccentBlue)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Goal not set",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Set a daily limit to track your balance.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
            IconButton(
                onClick = onSetTarget,
                modifier = Modifier.background(AccentBlue, CircleShape)
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun EmptyMealState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(CalorieAmberSoft.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.LocalDining,
                contentDescription = null,
                tint = CalorieAmber,
                modifier = Modifier.size(32.dp)
            )
        }
        Text(
            text = "Hungry?",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "You haven't logged any meals yet.",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onAddClick,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Your First Meal")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddFoodSheet(
    initialEntry: MealLogEntry?,
    isSaving: Boolean,
    isSearching: Boolean,
    searchResults: List<FoodSearchResult>,
    onDismiss: () -> Unit,
    onSearch: (String) -> Unit,
    onSave: (MealEntryDraft, String?) -> Unit
) {
    var foodName by rememberSaveable(initialEntry?.id) { mutableStateOf(initialEntry?.foodName.orEmpty()) }
    var caloriesText by rememberSaveable(initialEntry?.id) {
        mutableStateOf(initialEntry?.caloriesPerServingText().orEmpty())
    }
    var quantityText by rememberSaveable(initialEntry?.id) {
        mutableStateOf(initialEntry?.quantity?.toString().orEmpty().ifBlank { "1" })
    }
    var servingSize by rememberSaveable(initialEntry?.id) {
        mutableStateOf(initialEntry?.servingSize.orEmpty().ifBlank { "serving" })
    }
    var selectedMealType by rememberSaveable(initialEntry?.id) { mutableStateOf(initialEntry?.mealType ?: MealType.BREAKFAST) }
    var validationMessage by remember { mutableStateOf<String?>(null) }
    val sheetScrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(DividerColor, CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(sheetScrollState)
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialEntry == null) "Log Meal" else "Edit Meal",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = foodName,
                    onValueChange = {
                        foodName = it
                        validationMessage = null
                    },
                    label = { Text("Food name") },
                    placeholder = { Text("Search rice, chicken, apple") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (foodName.isNotBlank() && !isSearching) {
                                onSearch(foodName.trim())
                            }
                        }
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = AccentBlue,
                        unfocusedIndicatorColor = DividerColor
                    ),
                    trailingIcon = {
                        if (foodName.isNotBlank()) {
                            IconButton(
                                onClick = { onSearch(foodName.trim()) },
                                enabled = !isSearching,
                                modifier = Modifier.size(52.dp)
                            ) {
                                if (isSearching) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                                else Icon(Icons.Default.Search, contentDescription = "Search food", tint = AccentBlue, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                )

                AnimatedVisibility(visible = searchResults.isNotEmpty()) {
                    Column(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Select a food to fill calories per serving",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = searchResults,
                                key = { "${it.name}|${it.servingSize}|${it.calories}" }
                            ) { result ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                    foodName = result.name
                                    caloriesText = result.calories.toString()
                                    servingSize = result.servingSize
                                    validationMessage = null
                                },
                                color = AccentBlueSoft.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.1f))
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                                    Text(
                                        result.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text("${result.calories} kcal • ${result.servingSize}", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = caloriesText,
                    onValueChange = {
                        caloriesText = it.filter { ch -> ch.isDigit() }
                        validationMessage = null
                    },
                    label = { Text("Calories per serving") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text("kcal") }
                )
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = {
                        quantityText = it.filter { ch -> ch.isDigit() || ch == '.' }
                        validationMessage = null
                    },
                    label = { Text("Quantity") },
                    modifier = Modifier.weight(0.7f),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            OutlinedTextField(
                value = servingSize,
                onValueChange = {
                    servingSize = it
                    validationMessage = null
                },
                label = { Text("Serving size") },
                placeholder = { Text("bowl, 100 g, cup") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Meal Category",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MealType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedMealType == type,
                            onClick = { selectedMealType = type },
                            label = { Text(type.label) },
                            leadingIcon = { Icon(type.icon, null, modifier = Modifier.size(16.dp)) },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentBlueSoft,
                                selectedLabelColor = AccentBlue,
                                selectedLeadingIconColor = AccentBlue
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = DividerColor,
                                selectedBorderColor = AccentBlue,
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.dp,
                                enabled = true,
                                selected = selectedMealType == type
                            )
                        )
                    }
                }
            }

            if (validationMessage != null) {
                Text(
                    text = validationMessage.orEmpty(),
                    color = Color(0xFFD14343),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Button(
                onClick = {
                    val caloriesPerServing = caloriesText.toIntOrNull()
                    val quantity = quantityText.toDoubleOrNull()
                    val validation = when {
                        foodName.isBlank() -> "Enter food name"
                        caloriesPerServing == null || caloriesPerServing <= 0 -> "Invalid calories"
                        quantity == null || quantity <= 0.0 -> "Invalid quantity"
                        else -> null
                    }

                    if (validation != null) {
                        validationMessage = validation
                        return@Button
                    }

                    onSave(
                        MealEntryDraft(
                            foodName = foodName.trim(),
                            calories = ((caloriesPerServing ?: 0) * (quantity ?: 1.0)).roundToInt(),
                            quantity = quantity ?: 1.0,
                            servingSize = servingSize.trim(),
                            mealType = selectedMealType
                        ),
                        initialEntry?.id
                    )
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                else Text("Save Meal", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SetCalorieTargetDialog(
    currentTarget: Int?,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var targetInput by rememberSaveable(currentTarget) {
        mutableStateOf(currentTarget?.toString().orEmpty())
    }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Daily Calorie Goal", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("UniMove uses this goal to help you stay on track with your fitness progress.", fontSize = 14.sp, color = TextSecondary)
                OutlinedTextField(
                    value = targetInput,
                    onValueChange = {
                        targetInput = it.filter { ch -> ch.isDigit() }
                        errorText = null
                    },
                    label = { Text("Target kcal") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                if (errorText != null) {
                    Text(text = errorText.orEmpty(), color = Color(0xFFD14343), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = targetInput.toIntOrNull()
                    if (target == null || target !in 500..8000) {
                        errorText = "Please enter a value between 500 and 8000"
                    } else {
                        onSave(target)
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) { Text("Set Goal") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = CardSurface
    )
}

private fun trimQuantity(quantity: Double): String {
    return if (quantity % 1.0 == 0.0) {
        quantity.toInt().toString()
    } else {
        String.format("%.1f", quantity)
    }
}

private fun MealLogEntry.caloriesPerServingText(): String {
    val safeQuantity = quantity.takeIf { it > 0.0 } ?: 1.0
    return (calories / safeQuantity).roundToInt().coerceAtLeast(0).toString()
}
