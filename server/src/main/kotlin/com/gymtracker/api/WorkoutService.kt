package com.gymtracker.api

import com.gymtracker.ai.ClaudeClient
import com.gymtracker.ai.PromptBuilder
import com.gymtracker.shared.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Business logic layer — orchestrates prompt building and Claude API calls.
 *
 * Logging here uses SLF4J (the JVM standard) via Logback. Each method logs:
 * - What was requested (input context for debugging)
 * - How long the Claude API call took (the main latency bottleneck)
 * - What came back (output summary)
 * - Any parse failures (Claude returned bad JSON)
 *
 * In production, these logs feed into tools like Datadog, Grafana, or ELK
 * where you can alert on error rates and latency spikes.
 */
class WorkoutService(private val claude: ClaudeClient) {

    private val log = LoggerFactory.getLogger(WorkoutService::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun generateWorkout(request: GenerateRequest): GeneratedWorkout {
        log.info("generateWorkout: target={}, equipment={}, experience={}, recentSessions={}",
            request.target.ifBlank { "(auto)" },
            request.profile.equipment.size,
            request.profile.experience,
            request.recentSessions.size
        )

        val systemPrompt = PromptBuilder.buildSystemPrompt(request.profile)
        val userMessage = PromptBuilder.buildGenerateMessage(
            target = request.target,
            recentSessions = request.recentSessions,
            weeklySummaries = request.weeklySummaries,
            monthlySummaries = request.monthlySummaries
        )
        log.debug("generateWorkout prompts:\n--- SYSTEM ---\n{}\n--- USER ---\n{}\n--- END ---", systemPrompt, userMessage)

        val start = System.currentTimeMillis()
        val raw = claude.sendMessage(userMessage, systemPrompt, maxTokens = 2048)
        val claudeMs = System.currentTimeMillis() - start
        log.debug("generateWorkout raw response ({} chars):\n{}", raw.length, raw)

        val workout = json.decodeFromString<GeneratedWorkout>(extractJson(raw))
        log.info("generateWorkout: completed in {}ms, exercises={}, names={}",
            claudeMs,
            workout.exercises.size,
            workout.exercises.map { it.name }
        )
        return workout
    }

    suspend fun resuggestWorkout(request: ResuggestRequest): GeneratedWorkout {
        log.info("resuggestWorkout: kept={}, rejected={}, reasons={}",
            request.kept.size,
            request.rejected.size,
            request.rejected.map { it.reason }
        )

        val systemPrompt = PromptBuilder.buildSystemPrompt(request.profile)
        val userMessage = PromptBuilder.buildResuggestMessage(request.kept, request.rejected)
        log.debug("resuggestWorkout prompts:\n--- SYSTEM ---\n{}\n--- USER ---\n{}\n--- END ---", systemPrompt, userMessage)

        val start = System.currentTimeMillis()
        val raw = claude.sendMessage(userMessage, systemPrompt, maxTokens = 2048)
        val claudeMs = System.currentTimeMillis() - start
        log.debug("resuggestWorkout raw response ({} chars):\n{}", raw.length, raw)

        val workout = json.decodeFromString<GeneratedWorkout>(extractJson(raw))
        log.info("resuggestWorkout: completed in {}ms, exercises={}", claudeMs, workout.exercises.size)
        return workout
    }

    suspend fun generateSplit(request: SplitRequest): WeeklySplit {
        log.info("generateSplit: daysPerWeek={}, recentSessions={}",
            request.profile.daysPerWeek,
            request.recentSessions.size
        )

        val (system, user) = PromptBuilder.buildWeeklySplitPrompt(
            request.profile, request.recentSessions
        )
        log.debug("generateSplit prompts:\n--- SYSTEM ---\n{}\n--- USER ---\n{}\n--- END ---", system, user)

        val start = System.currentTimeMillis()
        val raw = claude.sendMessage(user, system, maxTokens = 1024)
        val claudeMs = System.currentTimeMillis() - start
        log.debug("generateSplit raw response ({} chars):\n{}", raw.length, raw)

        val split = json.decodeFromString<WeeklySplit>(extractJson(raw))
        log.info("generateSplit: completed in {}ms, trainingDays={}",
            claudeMs,
            split.days.count { it.focus != "Rest" }
        )
        return split
    }

    suspend fun generateWeeklySummary(request: WeeklySummaryRequest): WeeklySummary {
        log.info("generateWeeklySummary: sessions={}", request.sessions.size)

        val userMessage = PromptBuilder.buildWeeklySummaryPrompt(request.sessions)
        log.debug("generateWeeklySummary prompt ({} chars):\n{}", userMessage.length, userMessage)

        val start = System.currentTimeMillis()
        val raw = claude.sendMessage(userMessage, maxTokens = 512)
        val claudeMs = System.currentTimeMillis() - start
        log.debug("generateWeeklySummary raw response ({} chars):\n{}", raw.length, raw)

        val summary = json.decodeFromString<WeeklySummary>(extractJson(raw))
        log.info("generateWeeklySummary: completed in {}ms, textLength={}", claudeMs, summary.text.length)
        return summary
    }

    suspend fun generateMonthlySummary(request: MonthlySummaryRequest): MonthlySummary {
        log.info("generateMonthlySummary: month={}, weeks={}", request.month, request.weeklySummaries.size)

        val userMessage = PromptBuilder.buildMonthlySummaryPrompt(request.weeklySummaries, request.month)
        log.debug("generateMonthlySummary prompt ({} chars):\n{}", userMessage.length, userMessage)

        val start = System.currentTimeMillis()
        val raw = claude.sendMessage(userMessage, maxTokens = 512)
        val claudeMs = System.currentTimeMillis() - start
        log.debug("generateMonthlySummary raw response ({} chars):\n{}", raw.length, raw)

        val summary = json.decodeFromString<MonthlySummary>(extractJson(raw))
        log.info("generateMonthlySummary: completed in {}ms, textLength={}", claudeMs, summary.text.length)
        return summary
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
