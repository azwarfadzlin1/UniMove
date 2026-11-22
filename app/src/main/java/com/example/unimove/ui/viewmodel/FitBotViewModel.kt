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
                val apiKey = BuildConfig.GROQ_API_KEY.trim()
                val maskedKey = if (apiKey.length > 10) "${apiKey.take(7)}...${apiKey.takeLast(4)}" else apiKey
                Log.d(TAG, "Using Groq API Key: $maskedKey (length=${apiKey.length})")
                if (apiKey.isBlank()) {
                    _uiState.update { state ->
                        val msgs = state.messages.toMutableList()
                        val idx = msgs.indexOfLast { it.isStreaming }
                        if (idx >= 0) msgs[idx] = msgs[idx].copy(
                            text = "⚠️ Groq API key is missing. Please check GROQ_API_KEY in local.properties.",
                            isStreaming = false,
                            isError = true
                        )
                        state.copy(messages = msgs, isLoading = false)
                    }
                    saveCurrentConversation()
                    return@launch
                }

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

                // Query available models for this API key to avoid hardcoding dead model names
                val availableModels = runCatching {
                    groqApi.getModels("Bearer $apiKey").data?.map { it.id }.orEmpty()
                }.onFailure { Log.e(TAG, "Failed to query Groq models: ${it.message}") }.getOrNull().orEmpty()
                Log.d(TAG, "Available Groq models for key: $availableModels")

                val preferredModels = listOf(
                    "llama-3.3-70b-versatile",
                    "llama-3.1-8b-instant",
                    "llama-3.2-3b-preview",
                    "llama-3.2-1b-preview",
                    "llama3-70b-8192",
                    "llama3-8b-8192",
                    "mixtral-8x7b-32768",
                    "gemma2-9b-it",
                    "openai/gpt-oss-120b",
                    "openai/gpt-oss-20b",
                    "qwen/qwen3.8-27b",
                    "allam-2-7b"
                )

                val candidateModels = mutableListOf<String>()

                // 1. Add preferred models if available in account
                for (model in preferredModels) {
                    if (availableModels.contains(model)) {
                        candidateModels.add(model)
                    }
                }

                // 2. Add any other available models that look like chat LLMs (filtering out audio/guard/specialized)
                val nonChatKeywords = listOf("whisper", "guard", "orpheus", "safeguard", "embed", "tts", "stt")
                availableModels.filter { id ->
                    nonChatKeywords.none { id.contains(it, ignoreCase = true) }
                }.forEach { model ->
                    if (!candidateModels.contains(model)) {
                        candidateModels.add(model)
                    }
                }

                // 3. Fallbacks in case model discovery was empty or returned nothing
                val defaultFallbacks = listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant", "openai/gpt-oss-120b")
                for (fb in defaultFallbacks) {
                    if (!candidateModels.contains(fb)) {
                        candidateModels.add(fb)
                    }
                }

                Log.d(TAG, "Candidate Groq models in priority order: $candidateModels")

                var responseText: String? = null
                var lastException: Exception? = null

                for (model in candidateModels) {
                    try {
                        Log.d(TAG, "Attempting Groq chat completion with model: $model")
                        val request = GroqChatRequest(model = model, messages = requestMessages)
                        val response = groqApi.createChatCompletion("Bearer $apiKey", request)
                        responseText = response.choices.firstOrNull()?.message?.content ?: "No response. Please try again."
                        Log.d(TAG, "Groq chat completion succeeded with model: $model")
                        break
                    } catch (e: retrofit2.HttpException) {
                        lastException = e
                        val code = e.code()
                        val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull().orEmpty()
                        Log.e(TAG, "Groq HTTP $code for model '$model': $body")
                        if (code == 400 || code == 404 || body.contains("model_terms_required", ignoreCase = true) || body.contains("model_not_found", ignoreCase = true)) {
                            Log.w(TAG, "Model '$model' failed with HTTP $code, trying next candidate model...")
                            continue
                        } else {
                            throw e
                        }
                    } catch (e: Exception) {
                        lastException = e
                        throw e
                    }
                }

                val finalResponseText = responseText ?: throw (lastException ?: Exception("No available Groq models responded."))

                _uiState.update { state ->
                    val msgs = state.messages.toMutableList()
                    val idx = msgs.indexOfLast { it.isStreaming }
                    if (idx >= 0) msgs[idx] = msgs[idx].copy(text = finalResponseText, isStreaming = false)
                    state.copy(messages = msgs, isLoading = false)
                }
                saveCurrentConversation()
            } catch (e: Exception) {
                Log.e(TAG, "sendMessage error: ${e.javaClass.simpleName}: ${e.message}", e)
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

    private fun friendlyError(e: Exception): String = when (e) {
        is retrofit2.HttpException -> {
            val code = e.code()
            val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull().orEmpty()
            Log.e(TAG, "Groq HTTP $code response body: $body")

            val jsonMsg = runCatching {
                com.google.gson.JsonParser.parseString(body)
                    .asJsonObject.getAsJsonObject("error")
                    .get("message").asString
            }.getOrNull()

            when {
                code == 401 || body.contains("invalid_api_key", ignoreCase = true) || body.contains("API key", ignoreCase = true) ->
                    "⚠️ Groq API key is invalid or unauthorized (HTTP 401)."
                code == 404 || body.contains("model_not_found", ignoreCase = true) ->
                    "⚠️ FitBot model not found. Check API key model access (HTTP 404)."
                code == 400 && body.contains("model_terms_required", ignoreCase = true) ->
                    "⚠️ Selected Groq model requires accepting terms in Groq console."
                code == 429 ->
                    "⚠️ Rate limit exceeded. Please try again in a moment (HTTP 429)."
                code == 403 ->
                    "⚠️ Access denied to Groq API (HTTP 403)."
                !jsonMsg.isNullOrBlank() ->
                    "⚠️ Groq API error: $jsonMsg"
                else ->
                    "⚠️ Groq API error (HTTP $code)."
            }
        }
        else -> when {
            e.message?.contains("network", true) == true ||
                    e.message?.contains("Unable to resolve", true) == true -> "⚠️ No internet connection."
            else -> "⚠️ ${e.javaClass.simpleName}: ${e.message?.take(100) ?: "Unknown error"}"
        }
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

        return """
            You are FitBot, an expert AI fitness and wellness assistant in the UniMove app.

            User Profile:
            $profile

            Response Formatting & Presentation Guidelines:
            - Format all responses using clean, structured Markdown for maximum readability.
            - Use bold text (**bold**) for key metrics, core concepts, and emphasis.
            - Use clear headers (e.g. ### Header or **Section Title:**) to organize advice into distinct sections.
            - Use bullet points (`•` or `-`) or numbered lists for steps, workout routines, tips, and key options.
            - Include appropriate emojis (e.g., 🏃‍♂️, 🥗, 💪, 💡, ⏱️, 🎯) to make responses friendly and visually engaging.
            - Keep answers concise, direct, and actionable.
            - Never provide medical diagnoses — advise consulting a physician for health or medical concerns.
        """.trimIndent()
    }

    override fun onCleared() {
        conversationsListener?.remove()
        conversationsListener = null
        super.onCleared()
    }
}
