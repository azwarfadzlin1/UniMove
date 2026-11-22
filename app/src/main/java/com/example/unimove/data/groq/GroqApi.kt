package com.example.unimove.data.groq

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class GroqMessage(
    val role: String,
    val content: String
)

data class GroqChatRequest(
    val model: String = "llama-3.3-70b-versatile",
    val messages: List<GroqMessage>,
    val temperature: Float = 0.8f,
    val max_tokens: Int = 1024,
    val top_p: Float = 0.95f,
    val stream: Boolean = false
)

data class GroqChoice(
    val index: Int,
    val message: GroqMessage?,
    val finish_reason: String?
)

data class GroqChatResponse(
    val id: String,
    val choices: List<GroqChoice>
)

data class GroqModelItem(
    val id: String
)

data class GroqModelsResponse(
    val data: List<GroqModelItem>?
)

interface GroqApi {
    @retrofit2.http.GET("openai/v1/models")
    suspend fun getModels(
        @Header("Authorization") authHeader: String
    ): GroqModelsResponse

    @POST("openai/v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authHeader: String,
        @Body request: GroqChatRequest
    ): GroqChatResponse
}
