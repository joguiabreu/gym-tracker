package com.gymtracker.ai

import com.gymtracker.data.UserProfile
import com.gymtracker.data.WorkoutSession
import kotlinx.serialization.json.Json

class ClaudeWorkoutService(
    private val apiService: ClaudeApiService
) : WorkoutAiService {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun generate(
        profile: UserProfile,
        target: String,
        recentSessions: List<WorkoutSession>
    ): Result<GeneratedWorkout> {
        val systemPrompt = PromptBuilder.buildSystemPrompt(profile)
        val userMessage = PromptBuilder.buildGenerateMessage(target, recentSessions)
        val responseText = apiService.sendMessage(userMessage, systemPrompt)
            .getOrElse { return Result.failure(it) }
        return parseResponse(responseText)
    }

    override suspend fun resuggest(
        profile: UserProfile,
        kept: List<GeneratedExercise>,
        rejected: List<Pair<GeneratedExercise, String>>
    ): Result<GeneratedWorkout> {
        val systemPrompt = PromptBuilder.buildSystemPrompt(profile)
        val userMessage = PromptBuilder.buildResuggestMessage(kept, rejected)
        val responseText = apiService.sendMessage(userMessage, systemPrompt)
            .getOrElse { return Result.failure(it) }
        return parseResponse(responseText)
    }

    private fun parseResponse(text: String): Result<GeneratedWorkout> {
        return try {
            val cleaned = text
                .replace(Regex("```json\\s*"), "")
                .replace(Regex("```\\s*$"), "")
                .trim()
            Result.success(json.decodeFromString<GeneratedWorkout>(cleaned))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to parse workout: ${e.message}"))
        }
    }
}
