package com.gymtracker.ai

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ClaudeRequest(
    val model: String,
    val messages: List<ClaudeMessage>,
    val system: String = "",
    @SerialName("max_tokens") val maxTokens: Int = 2048
)

@Serializable
data class ClaudeMessage(
    val role: String,
    val content: String
)

@Serializable
data class ClaudeResponse(
    val content: List<ContentBlock> = emptyList(),
    val usage: Usage? = null
)

@Serializable
data class ContentBlock(
    val type: String,
    val text: String = ""
)

@Serializable
data class Usage(
    @SerialName("input_tokens") val inputTokens: Int = 0,
    @SerialName("output_tokens") val outputTokens: Int = 0
)

class ClaudeClient(
    private val apiKey: String,
    private val model: String = "claude-sonnet-4-20250514"
) {
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
    }

    suspend fun sendMessage(
        userMessage: String,
        systemPrompt: String = "",
        maxTokens: Int = 2048
    ): String {
        val response = httpClient.post("https://api.anthropic.com/v1/messages") {
            contentType(ContentType.Application.Json)
            header("x-api-key", apiKey)
            header("anthropic-version", "2023-06-01")
            setBody(
                ClaudeRequest(
                    model = model,
                    messages = listOf(ClaudeMessage(role = "user", content = userMessage)),
                    system = systemPrompt,
                    maxTokens = maxTokens
                )
            )
        }
        val body = response.body<ClaudeResponse>()
        return body.content.firstOrNull { it.type == "text" }?.text
            ?: throw Exception("No text in Claude response")
    }
}
