package com.gymtracker.ai

/**
 * Abstraction over the AI backend. WorkoutService talks to this interface,
 * not to a concrete HTTP client. This lets us swap in a mock for testing.
 */
interface AiClient {
    suspend fun sendMessage(
        userMessage: String,
        systemPrompt: String = "",
        maxTokens: Int = 2048
    ): String
}
