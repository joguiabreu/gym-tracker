package com.gymtracker.ai

import com.gymtracker.data.*
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
        val userMessage = PromptBuilder.buildGenerateMessage(target, recentSessions = recentSessions)
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

    override suspend fun generateWeeklySplit(
        profile: UserProfile,
        recentSessions: List<WorkoutSession>
    ): Result<WeeklySplit> {
        val prompt = PromptBuilder.buildWeeklySplitPrompt(profile, recentSessions)
        val responseText = apiService.sendMessage(prompt.second, prompt.first)
            .getOrElse { return Result.failure(it) }
        return try {
            val cleaned = cleanJson(responseText)
            Result.success(json.decodeFromString<WeeklySplit>(cleaned))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to parse weekly split: ${e.message}"))
        }
    }

    override suspend fun generateWeeklySummary(
        sessions: List<WorkoutSession>
    ): Result<WeeklySummary> {
        val prompt = PromptBuilder.buildWeeklySummaryPrompt(sessions)
        val responseText = apiService.sendMessage(
            prompt, "You are a fitness data analyst. Compress workout data into concise summaries.",
            maxTokens = 256
        ).getOrElse { return Result.failure(it) }
        return try {
            val cleaned = cleanJson(responseText)
            Result.success(json.decodeFromString<WeeklySummary>(cleaned))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to parse weekly summary: ${e.message}"))
        }
    }

    override suspend fun generateMonthlySummary(
        weeklySummaries: List<WeeklySummary>,
        month: String
    ): Result<MonthlySummary> {
        val prompt = PromptBuilder.buildMonthlySummaryPrompt(weeklySummaries, month)
        val responseText = apiService.sendMessage(
            prompt, "You are a fitness data analyst. Compress weekly summaries into monthly trend reports.",
            maxTokens = 256
        ).getOrElse { return Result.failure(it) }
        return try {
            val cleaned = cleanJson(responseText)
            Result.success(json.decodeFromString<MonthlySummary>(cleaned))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to parse monthly summary: ${e.message}"))
        }
    }

    private fun parseResponse(text: String): Result<GeneratedWorkout> {
        return try {
            Result.success(json.decodeFromString<GeneratedWorkout>(cleanJson(text)))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to parse workout: ${e.message}"))
        }
    }

    private fun cleanJson(text: String): String = text
        .replace(Regex("```json\\s*"), "")
        .replace(Regex("```\\s*$"), "")
        .trim()
}
