package com.example.unimove.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.unimove.BuildConfig
import com.example.unimove.data.groq.GroqApi
import com.example.unimove.data.groq.GroqChatRequest
import com.example.unimove.data.groq.GroqMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

private const val TAG = "FitBotViewModel"
private const val CONV_COLLECTION = "fitbot_conversations"

// ─── Data Models ─────────────────────────────────────────────────────────────

data class ChatMessage(
    val id: Long = System.nanoTime(),
    val text: String,
    val isUser: Boolean,
    val isStreaming: Boolean = false,
    val isError: Boolean = false
)

data class ConversationSummary(
    val id: String = "",
    val title: String = "New Chat",
    val updatedAt: Long = 0L,
    val messageCount: Int = 0
)

data class FitBotUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val inputText: String = "",
    val isChatReady: Boolean = false,
    val conversations: List<ConversationSummary> = emptyList(),
    val currentConversationId: String? = null,
    val isLoadingConversations: Boolean = false,
    val historyErrorMessage: String? = null
)

// ─── ViewModel ───────────────────────────────────────────────────────────────

class FitBotViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FitBotUiState())
    val uiState: StateFlow<FitBotUiState> = _uiState.asStateFlow()

    private val firestore = FirebaseFirestore.getInstance()
    private val groqApi: GroqApi by lazy {
        retrofit2.Retrofit.Builder()
            .baseUrl("https://api.groq.com/")
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(GroqApi::class.java)
    }

    // Cached context for session rebuilding
    private var cachedUid: String? = null
    private var cachedSystemPrompt: String = ""
    private var cachedUserName: String = "there"
    private var conversationsListener: ListenerRegistration? = null
    private var conversationsListenerUid: String? = null
    private val saveMutex = Mutex()

    private fun conversationCollection(uid: String) =
        firestore.collection("users").document(uid).collection(CONV_COLLECTION)

    private fun ensureCurrentConversationId(uid: String): String {
        _uiState.value.currentConversationId?.let { return it }

        val newConversationId = conversationCollection(uid).document().id
        _uiState.update { it.copy(currentConversationId = newConversationId) }
        return newConversationId
    }

    // ── Initialize ───────────────────────────────────────────────────────────

    fun initializeChat(
        uid: String,
        userName: String,
        weightKg: Double?,
        heightCm: Double?,
        age: Int?,
        gender: String?,
        totalDistanceKm: Double,
        dailyCalorieTarget: Int?
    ) {
        if (uid.isBlank()) return
        val shouldResetChat = cachedUid != uid || !_uiState.value.isChatReady

        cachedUid = uid
        cachedUserName = if (userName.isBlank()) "there" else userName
        cachedSystemPrompt = buildSystemPrompt(
            userName = cachedUserName, weightKg = weightKg, heightCm = heightCm,
            age = age, gender = gender, totalDistanceKm = totalDistanceKm,
            dailyCalorieTarget = dailyCalorieTarget
        )

        if (shouldResetChat) {
            _uiState.update {
                it.copy(
                    isChatReady = true,
                    messages = listOf(welcomeMessage(cachedUserName)),
                    currentConversationId = null,
                    historyErrorMessage = null
                )
            }
        } else {
            _uiState.update { it.copy(isChatReady = true, historyErrorMessage = null) }
        }

        observeUserConversations(uid)
        Log.d(TAG, "FitBot initialized for uid=$uid")
    }

    private fun welcomeMessage(name: String) = ChatMessage(
        text = "Hey $name! 👋 I'm FitBot, your personal AI assistant.\n\nI can help with workout plans, nutrition, pacing tips, and much more — or just answer any question you have.\n\nWhat can I help you with today? 💪",
        isUser = false
    )

    // ── Conversation List ─────────────────────────────────────────────────────

    fun loadUserConversations(uid: String) {
        if (uid.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingConversations = true) }
            try {
                // No orderBy to avoid requiring a Firestore index — sort in memory instead
                val snap = conversationCollection(uid)
                    .orderBy("updatedAt", Query.Direction.DESCENDING)
                    .limit(50)
                    .get()
                    .await()

                val list = snap.documents.mapNotNull { doc ->
                    val updatedAt = doc.getLong("updatedAt") ?: return@mapNotNull null
                    ConversationSummary(
                        id = doc.id,
                        title = doc.getString("title") ?: "Chat",
                        updatedAt = updatedAt,
                        messageCount = (doc.get("messages") as? List<*>)?.size ?: 0
                    )
                }.sortedByDescending { it.updatedAt }  // sort newest first in memory

                _uiState.update {
                    it.copy(
                        conversations = list,
                        isLoadingConversations = false,
                        historyErrorMessage = null
                    )
                }
                Log.d(TAG, "Loaded ${list.size} conversations for uid=$uid")
            } catch (e: Exception) {
                Log.e(TAG, "loadConversations error: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoadingConversations = false,
                        historyErrorMessage = historyErrorMessage(e, "load")
                    )
                }
            }
        }
    }

    private fun observeUserConversations(uid: String) {
        if (uid.isBlank() || conversationsListenerUid == uid) return

        conversationsListener?.remove()
        conversationsListenerUid = uid
        _uiState.update { it.copy(isLoadingConversations = true) }

        conversationsListener = conversationCollection(uid)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "conversation listener error: ${error.message}", error)
                    _uiState.update {
                        it.copy(
                            isLoadingConversations = false,
                            historyErrorMessage = historyErrorMessage(error, "load")
                        )
                    }
                    return@addSnapshotListener
                }

                val list = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val updatedAt = doc.getLong("updatedAt") ?: return@mapNotNull null
                    ConversationSummary(
                        id = doc.id,
                        title = doc.getString("title") ?: "Chat",
                        updatedAt = updatedAt,
                        messageCount = (doc.get("messages") as? List<*>)?.size ?: 0
                    )
                }
                _uiState.update {
                    it.copy(
                        conversations = list,
                        isLoadingConversations = false,
                        historyErrorMessage = null
                    )
                }
            }
    }

    fun loadConversation(uid: String, conversationId: String) {
        viewModelScope.launch {
            try {
                val doc = conversationCollection(uid).document(conversationId).get().await()
                if (!doc.exists()) return@launch

                @Suppress("UNCHECKED_CAST")
                val raw = doc.get("messages") as? List<Map<String, Any>> ?: emptyList()
                val messages = raw.map { m ->
                    ChatMessage(text = m["text"] as? String ?: "", isUser = m["isUser"] as? Boolean ?: false)
                }
                _uiState.update { it.copy(messages = messages, currentConversationId = conversationId, isChatReady = true) }
            } catch (e: Exception) {
                Log.e(TAG, "loadConversation error: ${e.message}")
            }
        }
    }

    fun startNewChat() {
        _uiState.update { it.copy(
            messages = listOf(welcomeMessage(cachedUserName)),
            inputText = "", isLoading = false, currentConversationId = null
        )}
    }

    fun deleteConversation(uid: String, conversationId: String) {
        viewModelScope.launch {
            try {
                conversationCollection(uid).document(conversationId).delete().await()
                if (_uiState.value.currentConversationId == conversationId) startNewChat()
                loadUserConversations(uid)
            } catch (e: Exception) {
                Log.e(TAG, "deleteConversation error: ${e.message}")
            }
        }
    }

    private fun saveCurrentConversation() {
        val uid = cachedUid?.takeIf { it.isNotBlank() } ?: run {
            Log.e(TAG, "saveCurrentConversation SKIPPED — cachedUid is blank! Was initializeChat called with a valid uid?")
            return
        }
        val messages = _uiState.value.messages.filter { !it.isError && !it.isStreaming && it.text.isNotBlank() }
        if (messages.count { it.isUser } < 1) return // Nothing user said yet

        val firstUserMsg = messages.firstOrNull { it.isUser }?.text ?: "New Chat"
        val title = if (firstUserMsg.length > 55) firstUserMsg.take(52) + "…" else firstUserMsg
        val now = System.currentTimeMillis()
        val existingConvId = _uiState.value.currentConversationId
        val convId = existingConvId ?: ensureCurrentConversationId(uid)
        Log.d(TAG, "Saving conversation for uid=$uid, convId=$convId")

        val data = hashMapOf(
            "title" to title,
            "updatedAt" to now,
            "messages" to messages.map { mapOf("text" to it.text, "isUser" to it.isUser) }
        )

        viewModelScope.launch {
            try {
                saveMutex.withLock {
                    val docRef = conversationCollection(uid).document(convId)
                    if (existingConvId == null) data["createdAt"] = now
                    docRef.set(data, SetOptions.merge()).await()
                }
                _uiState.update { state ->
                    if (state.currentConversationId == convId) {
                        state.copy(currentConversationId = convId, historyErrorMessage = null)
                    } else {
                        state
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "saveConversation error: ${e.message}", e)
                _uiState.update {
                    it.copy(historyErrorMessage = historyErrorMessage(e, "save"))
                }
            }
        }
    }

    // ── Messaging ─────────────────────────────────────────────────────────────

    fun onInputChanged(text: String) = _uiState.update { it.copy(inputText = text) }

    fun clearHistoryError() {
        _uiState.update { it.copy(historyErrorMessage = null) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank() || _uiState.value.isLoading) return

        val placeholder = ChatMessage(text = "", isUser = false, isStreaming = true)
        _uiState.update { it.copy(
            messages = it.messages + ChatMessage(text = text, isUser = true) + placeholder,
            inputText = "", isLoading = true
        )}

        // Persist immediately so the chat survives app close, navigation away, or AI/API failure.
        saveCurrentConversation()

        viewModelScope.launch {
            try {
                // 1. Filter out errors and the placeholder streaming message
                val allMessages = _uiState.value.messages.filter { !it.isError && !it.isStreaming && it.text.isNotBlank() }
                
                // 2. Sliding window — cap at last 10 messages to control input token growth
                val trimmedHistory = if (allMessages.size > 10) allMessages.takeLast(10) else allMessages
                
                // 3. Build request payload for Groq
                val requestMessages = mutableListOf<GroqMessage>()
                requestMessages.add(GroqMessage(role = "system", content = cachedSystemPrompt))
                
                trimmedHistory.forEach { msg ->
                    val role = if (msg.isUser) "user" else "assistant"
                    requestMessages.add(GroqMessage(role = role, content = msg.text))
                }
                
                val request = GroqChatRequest(messages = requestMessages)
                val response = groqApi.createChatCompletion("Bearer ${BuildConfig.GROQ_API_KEY}", request)
                val responseText = response.choices.firstOrNull()?.message?.content ?: "No response. Please try again."

                _uiState.update { state ->
                    val msgs = state.messages.toMutableList()
                    val idx = msgs.indexOfLast { it.isStreaming }
                    if (idx >= 0) msgs[idx] = msgs[idx].copy(text = responseText, isStreaming = false)
                    state.copy(messages = msgs, isLoading = false)
                }
                saveCurrentConversation()
            } catch (e: Exception) {
                Log.e(TAG, "sendMessage error: ${e.javaClass.simpleName}: ${e.message}")
                _uiState.update { state ->
                    val msgs = state.messages.toMutableList()
                    val idx = msgs.indexOfLast { it.isStreaming }
                    if (idx >= 0) msgs[idx] = msgs[idx].copy(text = friendlyError(e), isStreaming = false, isError = true)
                    state.copy(messages = msgs, isLoading = false)
                }
                saveCurrentConversation()
            }
        }
    }

    private fun friendlyError(e: Exception): String = when {
        e.message?.contains("API_KEY_INVALID", true) == true ||
                e.message?.contains("API key not valid", true) == true ||
                e.message?.contains("HTTP 401", true) == true -> "⚠️ API key is invalid."
        e.message?.contains("network", true) == true ||
                e.message?.contains("Unable to resolve", true) == true -> "⚠️ No internet connection."
        e.message?.contains("HTTP 403", true) == true -> "⚠️ API access denied."
        e.message?.contains("HTTP 429", true) == true -> "⚠️ Rate limited. Try again shortly!"
        e.message?.contains("HTTP 404", true) == true -> "⚠️ Model not found. Check API key access."
        else -> "⚠️ ${e.javaClass.simpleName}: ${e.message?.take(100) ?: "Unknown error"}"
    }

    private fun historyErrorMessage(error: Exception, action: String): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("PERMISSION_DENIED", ignoreCase = true) ||
                message.contains("permission", ignoreCase = true) -> {
                "FitBot history is blocked by Firestore rules. Deploy the updated rules, then try New Chat again."
            }
            message.contains("network", ignoreCase = true) ||
                message.contains("unavailable", ignoreCase = true) -> {
                "FitBot history could not $action because the network is unavailable."
            }
            else -> "FitBot history could not $action. Please try again."
        }
    }

    val suggestedPrompts = listOf(
        "Create a beginner 5K training plan for me",
        "What should I eat before a morning run?",
        "How do I improve my running pace?",
        "Give me a 20-minute HIIT workout I can do indoors"
    )

    private fun buildSystemPrompt(
        userName: String, weightKg: Double?, heightCm: Double?,
        age: Int?, gender: String?, totalDistanceKm: Double, dailyCalorieTarget: Int?
    ): String {
        val profile = buildString {
            appendLine("- Name: $userName")
            if (age != null) appendLine("- Age: $age years old")
            if (gender?.isNotBlank() == true) appendLine("- Gender: $gender")
            if (weightKg != null) appendLine("- Weight: ${weightKg}kg")
            if (heightCm != null) appendLine("- Height: ${heightCm}cm")
            if (totalDistanceKm > 0.01) appendLine("- Total distance run: ${String.format("%.1f", totalDistanceKm)}km")
            if (dailyCalorieTarget != null) appendLine("- Daily calorie target: ${dailyCalorieTarget}kcal")
        }.trim().ifBlank { "- Profile not set up" }
        return "You are FitBot, an AI assistant in UniMove (a university fitness app). " +
                "User profile: $profile. " +
                "You are friendly, concise, and knowledgeable. For fitness questions, give personalized, evidence-based coaching using the profile above. " +
                "For any other topic, answer helpfully like a general AI assistant. " +
                "Keep replies under 150 words unless a detailed plan is explicitly requested. " +
                "Never give medical diagnoses — recommend a doctor for health issues."
    }

    override fun onCleared() {
        conversationsListener?.remove()
        conversationsListener = null
        super.onCleared()
    }
}
