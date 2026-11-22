package com.example.unimove.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.unimove.ui.theme.*
import com.example.unimove.ui.viewmodel.AuthViewModel
import com.example.unimove.ui.viewmodel.ChatMessage
import com.example.unimove.ui.viewmodel.ConversationSummary
import com.example.unimove.ui.viewmodel.FitBotViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ─── FitBot Screen ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitBotScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(),
    fitBotViewModel: FitBotViewModel = viewModel()
) {
    val uiState by fitBotViewModel.uiState.collectAsStateWithLifecycle()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val totals by authViewModel.totals.collectAsStateWithLifecycle()
    val profile = authState.profile
    val uid = profile?.uid ?: authState.user?.uid ?: ""

    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Bug 1 fix: key on uid (not profile) so this fires as soon as uid is available,
    // even if profile hasn't loaded yet. The ViewModel guard prevents double-init.
    LaunchedEffect(uid) {
        if (uid.isNotBlank()) {
            fitBotViewModel.initializeChat(
                uid = uid,
                userName = profile?.name?.ifBlank { "there" } ?: "there",
                weightKg = profile?.weight,
                heightCm = profile?.height,
                age = profile?.age,
                gender = profile?.gender,
                totalDistanceKm = totals.totalDistanceKm,
                dailyCalorieTarget = profile?.dailyCalorieTarget
            )
        }
    }

    // Bug 2 fix: always reload conversation list when uid is known.
    // Handles back-navigation where isChatReady is already true so initializeChat skips.
    LaunchedEffect(uid) {
        if (uid.isNotBlank()) {
            fitBotViewModel.loadUserConversations(uid)
        }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            coroutineScope.launch { listState.animateScrollToItem(uiState.messages.size - 1) }
        }
    }

    LaunchedEffect(uiState.historyErrorMessage) {
        val message = uiState.historyErrorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        fitBotViewModel.clearHistoryError()
    }

    // Refresh conversation list every time the drawer is opened
    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.isOpen && uid.isNotBlank()) {
            fitBotViewModel.loadUserConversations(uid)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            FitBotSidebar(
                conversations = uiState.conversations,
                currentConversationId = uiState.currentConversationId,
                isLoading = uiState.isLoadingConversations,
                onNewChat = {
                    fitBotViewModel.startNewChat()
                    coroutineScope.launch { drawerState.close() }
                },
                onSelectConversation = { convId ->
                    fitBotViewModel.loadConversation(uid, convId)
                    coroutineScope.launch { drawerState.close() }
                },
                onDeleteConversation = { convId ->
                    fitBotViewModel.deleteConversation(uid, convId)
                },
                onClose = { coroutineScope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            containerColor = SurfaceLight,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                FitBotTopBar(
                    onBack = { navController.popBackStack() },
                    onMenuClick = { coroutineScope.launch { drawerState.open() } }
                )
            },
            bottomBar = {
                FitBotInputBar(
                    inputText = uiState.inputText,
                    isLoading = uiState.isLoading,
                    onInputChanged = fitBotViewModel::onInputChanged,
                    onSend = fitBotViewModel::sendMessage
                )
            }
        ) { innerPadding ->
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.messages, key = { it.id }) { message ->
                    ChatBubble(message = message)
                }
                if (uiState.messages.size == 1 && !uiState.isLoading) {
                    item {
                        SuggestedPromptsRow(
                            prompts = fitBotViewModel.suggestedPrompts,
                            onSelect = { prompt ->
                                fitBotViewModel.onInputChanged(prompt)
                                fitBotViewModel.sendMessage()
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─── Sidebar ─────────────────────────────────────────────────────────────────

@Composable
private fun FitBotSidebar(
    conversations: List<ConversationSummary>,
    currentConversationId: String?,
    isLoading: Boolean,
    onNewChat: () -> Unit,
    onSelectConversation: (String) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onClose: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerContainerColor = Color(0xFF0F1B3D)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF1A2E5E), Color(0xFF0F1B3D))))
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(AccentBlue.copy(alpha = 0.8f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.SmartToy, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Text("FitBot AI", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            }
        }

        // New Chat Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .clickable(onClick = onNewChat)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text("New Chat", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))

        // Conversations list
        if (isLoading) {
            Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        } else if (conversations.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(top = 40.dp, start = 20.dp, end = 20.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(36.dp))
                    Text("No conversations yet", color = Color.White.copy(alpha = 0.45f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("Start chatting to save history", color = Color.White.copy(alpha = 0.3f), fontSize = 11.sp)
                }
            }
        } else {
            // Group by date
            val today = remember { getDateLabel(System.currentTimeMillis()) }
            val grouped = remember(conversations) {
                conversations.groupBy { getDateLabel(it.updatedAt) }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                grouped.forEach { (dateLabel, convs) ->
                    item {
                        Text(
                            text = dateLabel,
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp)
                        )
                    }
                    items(convs, key = { it.id }) { conv ->
                        ConversationItem(
                            conv = conv,
                            isActive = conv.id == currentConversationId,
                            onSelect = { onSelectConversation(conv.id) },
                            onDelete = { onDeleteConversation(conv.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationItem(
    conv: ConversationSummary,
    isActive: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    var showDelete by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isActive) AccentBlue.copy(alpha = 0.35f)
                else Color.Transparent
            )
            .clickable { onSelect(); showDelete = false }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount < -30f) showDelete = true
                    if (dragAmount > 30f) showDelete = false
                }
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.ChatBubbleOutline,
                contentDescription = null,
                tint = if (isActive) Color.White else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = conv.title,
                color = if (isActive) Color.White else Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (showDelete) {
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFFF6B6B), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

private fun getDateLabel(epochMs: Long): String {
    val cal = Calendar.getInstance()
    val today = cal.get(Calendar.DAY_OF_YEAR)
    val year = cal.get(Calendar.YEAR)
    cal.timeInMillis = epochMs
    val msgDay = cal.get(Calendar.DAY_OF_YEAR)
    val msgYear = cal.get(Calendar.YEAR)
    return when {
        msgYear != year -> SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(epochMs))
        today - msgDay == 0 -> "Today"
        today - msgDay == 1 -> "Yesterday"
        today - msgDay < 7 -> "This Week"
        else -> "Earlier"
    }
}

// ─── Top App Bar ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FitBotTopBar(onBack: () -> Unit, onMenuClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse_alpha"
    )
    Box(
        modifier = Modifier.fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(Color(0xFF1A2E5E), Color(0xFF1A56DB))))
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SmartToy, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("FitBot AI", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF4ADE80).copy(alpha = pulseAlpha)))
                    Text("Your AI assistant · Online", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
            }
            // History / Drawer button
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.History, contentDescription = "Chat history", tint = Color.White)
            }
        }
    }
}

// ─── Chat Bubble & Formatting ───────────────────────────────────────────────

@Composable
private fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!message.isUser) {
            Box(
                modifier = Modifier.size(30.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF1A2E5E), Color(0xFF1A56DB)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SmartToy, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(8.dp))
        }
        Box(
            modifier = Modifier.widthIn(max = if (message.isUser) 280.dp else 320.dp)
                .shadow(if (message.isUser) 4.dp else 2.dp, bubbleShape(message.isUser))
                .clip(bubbleShape(message.isUser))
                .background(if (message.isUser) Brush.linearGradient(listOf(Color(0xFF1A2E5E), Color(0xFF1A56DB))) else Brush.linearGradient(listOf(CardSurface, CardSurface)))
                .border(if (message.isUser) 0.dp else 1.dp, if (message.isUser) Color.Transparent else DividerColor, bubbleShape(message.isUser))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (message.isStreaming && message.text.isEmpty()) {
                TypingIndicator()
            } else {
                FormattedChatMessage(message = message)
            }
        }
        if (message.isUser) Spacer(Modifier.width(8.dp))
    }
}

@Composable
private fun FormattedChatMessage(message: ChatMessage) {
    if (message.isUser || message.isError) {
        Text(
            text = message.text,
            color = when { message.isUser -> Color.White; message.isError -> Color(0xFFEF4444); else -> TextPrimary },
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = if (message.isUser) FontWeight.Medium else FontWeight.Normal
        )
        return
    }

    val blocks = remember(message.text) { parseMarkdownBlocks(message.text) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Header -> {
                    val fontSize = when (block.level) {
                        1 -> 16.sp
                        2 -> 15.5.sp
                        else -> 15.sp
                    }
                    Text(
                        text = parseMarkdownInline(block.text, textColor = TextPrimary, boldColor = Color(0xFF1A56DB)),
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }
                is MarkdownBlock.BulletItem -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(start = 2.dp)
                    ) {
                        Text(
                            text = "•",
                            color = AccentBlue,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = parseMarkdownInline(block.text, textColor = TextPrimary, boldColor = TextPrimary),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                is MarkdownBlock.NumberedItem -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(start = 2.dp)
                    ) {
                        Text(
                            text = "${block.number}.",
                            color = AccentBlue,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.widthIn(min = 18.dp)
                        )
                        Text(
                            text = parseMarkdownInline(block.text, textColor = TextPrimary, boldColor = TextPrimary),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                is MarkdownBlock.Callout -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentBlueSoft.copy(alpha = 0.6f))
                            .border(1.dp, AccentBlue.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = parseMarkdownInline(block.text, textColor = TextPrimary, boldColor = AccentBlue),
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp
                        )
                    }
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = parseMarkdownInline(block.text, textColor = TextPrimary, boldColor = TextPrimary),
                        fontSize = 14.sp,
                        lineHeight = 20.5.sp
                    )
                }
            }
        }
    }
}

private sealed interface MarkdownBlock {
    data class Header(val text: String, val level: Int) : MarkdownBlock
    data class BulletItem(val text: String) : MarkdownBlock
    data class NumberedItem(val number: String, val text: String) : MarkdownBlock
    data class Callout(val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
}

private fun parseMarkdownBlocks(rawText: String): List<MarkdownBlock> {
    if (rawText.isBlank()) return emptyList()

    val lines = rawText.lines()
    val blocks = mutableListOf<MarkdownBlock>()
    val currentParagraphLines = mutableListOf<String>()

    fun flushParagraph() {
        if (currentParagraphLines.isNotEmpty()) {
            val combined = currentParagraphLines.joinToString("\n").trim()
            if (combined.isNotBlank()) {
                blocks.add(MarkdownBlock.Paragraph(combined))
            }
            currentParagraphLines.clear()
        }
    }

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            flushParagraph()
            continue
        }

        when {
            trimmed.startsWith("#") -> {
                flushParagraph()
                val level = trimmed.takeWhile { it == '#' }.length
                val headerText = trimmed.dropWhile { it == '#' || it == ' ' }.trim()
                blocks.add(MarkdownBlock.Header(headerText, level))
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") -> {
                flushParagraph()
                val itemText = trimmed.substring(2).trim()
                blocks.add(MarkdownBlock.BulletItem(itemText))
            }
            trimmed.matches(Regex("^\\d+\\.\\s+.*")) -> {
                flushParagraph()
                val dotIndex = trimmed.indexOf('.')
                val number = trimmed.substring(0, dotIndex).trim()
                val itemText = trimmed.substring(dotIndex + 1).trim()
                blocks.add(MarkdownBlock.NumberedItem(number, itemText))
            }
            trimmed.startsWith("> ") -> {
                flushParagraph()
                val quoteText = trimmed.substring(2).trim()
                blocks.add(MarkdownBlock.Callout(quoteText))
            }
            else -> {
                currentParagraphLines.add(line)
            }
        }
    }
    flushParagraph()
    return blocks
}

private fun parseMarkdownInline(
    text: String,
    textColor: Color,
    boldColor: Color = textColor
): AnnotatedString {
    return buildAnnotatedString {
        val pattern = Regex("(\\*\\*|\\*|`)(.*?)\\1")
        var lastIndex = 0

        for (match in pattern.findAll(text)) {
            val range = match.range
            if (range.first > lastIndex) {
                append(text.substring(lastIndex, range.first))
            }
            val delimiter = match.groupValues[1]
            val content = match.groupValues[2]

            when (delimiter) {
                "**" -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = boldColor)) {
                        append(content)
                    }
                }
                "*" -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(content)
                    }
                }
                "`" -> {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color.Black.copy(alpha = 0.07f),
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append(" $content ")
                    }
                }
            }
            lastIndex = range.last + 1
        }

        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}

private fun bubbleShape(isUser: Boolean) = RoundedCornerShape(
    topStart = 18.dp, topEnd = 18.dp,
    bottomStart = if (isUser) 18.dp else 4.dp,
    bottomEnd = if (isUser) 4.dp else 18.dp
)

// ─── Typing Indicator ────────────────────────────────────────────────────────

@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)) {
        listOf(0, 200, 400).forEach { delay ->
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.7f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(600, delayMillis = delay, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "dot_$delay"
            )
            Box(modifier = Modifier.size(8.dp).scale(scale).clip(CircleShape).background(AccentBlue.copy(alpha = 0.8f)))
        }
    }
}

// ─── Suggested Prompts ───────────────────────────────────────────────────────

@Composable
private fun SuggestedPromptsRow(prompts: List<String>, onSelect: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Try asking...", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 2.dp)) {
            items(prompts) { prompt ->
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(AccentBlueSoft)
                        .border(1.dp, AccentBlue.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onSelect(prompt) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(prompt, color = AccentBlue, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                }
            }
        }
    }
}

// ─── Input Bar ───────────────────────────────────────────────────────────────

@Composable
private fun FitBotInputBar(inputText: String, isLoading: Boolean, onInputChanged: (String) -> Unit, onSend: () -> Unit) {
    val canSend = inputText.isNotBlank() && !isLoading
    val infiniteTransition = rememberInfiniteTransition(label = "send_pulse")
    val sendScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = if (canSend) 1.08f else 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "send_scale"
    )
    Surface(color = CardSurface, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = inputText, onValueChange = onInputChanged,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask FitBot anything...", color = TextMuted, fontSize = 14.sp) },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue, unfocusedBorderColor = DividerColor,
                    cursorColor = AccentBlue, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                ),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                enabled = !isLoading,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
            )
            Box(
                modifier = Modifier.size(46.dp).scale(sendScale)
                    .shadow(if (canSend) 6.dp else 0.dp, CircleShape)
                    .clip(CircleShape)
                    .background(if (canSend) Brush.linearGradient(listOf(Color(0xFF1A2E5E), Color(0xFF1A56DB))) else Brush.linearGradient(listOf(DividerColor, DividerColor)))
                    .clickable(enabled = canSend, onClick = onSend),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AccentBlue, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = if (canSend) Color.White else TextMuted, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
