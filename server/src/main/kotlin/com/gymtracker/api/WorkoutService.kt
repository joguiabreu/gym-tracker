package com.gymtracker.api

import com.gymtracker.ai.ClaudeClient
import com.gymtracker.ai.PromptBuilder
import com.gymtracker.shared.*
import kotlinx.serialization.json.Json

class WorkoutService(private val claude: ClaudeClient) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun generateWorkout(request: GenerateRequest): GeneratedWorkout {
        val systemPrompt = PromptBuilder.buildSystemPrompt(request.profile)
        val userMessage = PromptBuilder.buildGenerateMessage(
            target = request.target,
            recentSessions = request.recentSessions,
            weeklySummaries = request.weeklySummaries,
            monthlySummaries = request.monthlySummaries
        )
        val raw = claude.sendMessage(userMessage, systemPrompt, maxTokens = 2048)
        return json.decodeFromString<GeneratedWorkout>(extractJson(raw))
    }

    suspend fun resuggestWorkout(request: ResuggestRequest): GeneratedWorkout {
        val systemPrompt = PromptBuilder.buildSystemPrompt(request.profile)
        val userMessage = PromptBuilder.buildResuggestMessage(request.kept, request.rejected)
        val raw = claude.sendMessage(userMessage, systemPrompt, maxTokens = 2048)
        return json.decodeFromString<GeneratedWorkout>(extractJson(raw))
    }

    suspend fun generateSplit(request: SplitRequest): WeeklySplit {
        val (system, user) = PromptBuilder.buildWeeklySplitPrompt(
            request.profile, request.recentSessions
        )
        val raw = claude.sendMessage(user, system, maxTokens = 1024)
        return json.decodeFromString<WeeklySplit>(extractJson(raw))
    }

    suspend fun generateWeeklySummary(request: WeeklySummaryRequest): WeeklySummary {
        val userMessage = PromptBuilder.buildWeeklySummaryPrompt(request.sessions)
        val raw = claude.sendMessage(userMessage, maxTokens = 512)
        return json.decodeFromString<WeeklySummary>(extractJson(raw))
    }

    suspend fun generateMonthlySummary(request: MonthlySummaryRequest): MonthlySummary {
        val userMessage = PromptBuilder.buildMonthlySummaryPrompt(
            request.weeklySummaries, request.month
        )
        val raw = claude.sendMessage(userMessage, maxTokens = 512)
        return json.decodeFromString<MonthlySummary>(extractJson(raw))
    }

    private fun extractJson(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith("```")) {
            val start = trimmed.indexOf('\n') + 1
            val end = trimmed.lastIndexOf("```")
            if (start in 1 until end) {
                return trimmed.substring(start, end).trim()
            }
        }
        return trimmed
    }
}
