package com.gymtracker.ai

import com.gymtracker.shared.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Returns realistic fake responses instead of calling Claude.
 *
 * This reads the prompt to decide what JSON shape to return (workout, split,
 * weekly summary, or monthly summary). The responses use real exercise names
 * from the catalog so the rest of the pipeline (JSON parsing, logging, API
 * contract) is tested identically to production.
 *
 * Toggle with: MOCK_MODE=true (no API key required)
 */
class MockAiClient : AiClient {

    private val log = LoggerFactory.getLogger(MockAiClient::class.java)
    private val json = Json { encodeDefaults = true; prettyPrint = false }

    override suspend fun sendMessage(
        userMessage: String,
        systemPrompt: String,
        maxTokens: Int
    ): String {
        log.info("MockAiClient: generating fake response (systemLen={}, userLen={})",
            systemPrompt.length, userMessage.length
        )

        val response = when {
            userMessage.contains("were rejected", ignoreCase = true) -> mockResuggest(userMessage)
            userMessage.contains("weekly split", ignoreCase = true)
                || userMessage.contains("Plan a weekly split", ignoreCase = true) -> mockSplit(userMessage)
            userMessage.contains("weekly summaries into", ignoreCase = true)
                || userMessage.contains("monthly trend", ignoreCase = true) -> mockMonthlySummary(userMessage)
            userMessage.contains("Compress these workout sessions", ignoreCase = true)
                || userMessage.contains("week_start", ignoreCase = true) -> mockWeeklySummary()
            else -> mockWorkout(systemPrompt, userMessage)
        }

        log.debug("MockAiClient: returning {} chars", response.length)
        return response
    }

    private fun mockWorkout(systemPrompt: String, userMessage: String): String {
        // Pick exercises based on target muscle from the user message
        val target = extractTarget(userMessage)
        val exercises = pickExercises(target, systemPrompt)

        val workout = GeneratedWorkout(
            exercises = exercises,
            reasoning = "[MOCK] Generated ${exercises.size} exercises for ${target.ifBlank { "full body" }}. " +
                "This is a mock response — in production, Claude would tailor this to your history and goals."
        )
        return json.encodeToString(workout)
    }

    private fun mockResuggest(userMessage: String): String {
        // Parse kept exercises from the prompt and generate replacements for rejected ones
        val keptNames = Regex("""- (.+?): \d+x\d+""")
            .findAll(userMessage.substringAfter("KEEP", ""))
            .map { it.groupValues[1] }
            .toList()

        val rejectedCount = Regex("""- (.+?) — reason:""")
            .findAll(userMessage.substringAfter("REJECTED", ""))
            .count()

        val kept = keptNames.map { name ->
            GeneratedExercise(
                name = name,
                plannedSets = 3,
                plannedReps = 10,
                suggestedWeightKg = 40.0,
                reason = "Kept from previous suggestion"
            )
        }

        // Pick replacement exercises that aren't in the kept list
        val replacements = ExerciseCatalog.exercises
            .filter { it.name !in keptNames }
            .shuffled()
            .take(rejectedCount.coerceAtLeast(1))
            .map { ex ->
                GeneratedExercise(
                    name = ex.name,
                    plannedSets = 3,
                    plannedReps = 10,
                    suggestedWeightKg = 30.0,
                    reason = "[MOCK] Replacement for rejected exercise"
                )
            }

        val workout = GeneratedWorkout(
            exercises = kept + replacements,
            reasoning = "[MOCK] Kept ${kept.size} exercises, replaced $rejectedCount."
        )
        return json.encodeToString(workout)
    }

    private fun mockSplit(userMessage: String): String {
        val split = WeeklySplit(
            weekStart = "2026-03-09",
            days = listOf(
                SplitDay("Monday", "Chest/Triceps"),
                SplitDay("Tuesday", "Back/Biceps"),
                SplitDay("Wednesday", "Rest"),
                SplitDay("Thursday", "Legs/Glutes"),
                SplitDay("Friday", "Shoulders/Arms"),
                SplitDay("Saturday", "Rest"),
                SplitDay("Sunday", "Rest")
            )
        )
        return json.encodeToString(split)
    }

    private fun mockWeeklySummary(): String {
        val summary = WeeklySummary(
            weekStart = "2026-03-03",
            text = "[MOCK] 4 sessions completed. Bench press progressed 60→65kg. " +
                "Squat volume increased. Skipped one leg day. Consistent upper body work."
        )
        return json.encodeToString(summary)
    }

    private fun mockMonthlySummary(userMessage: String): String {
        val month = Regex(""""month":"(\d{4}-\d{2})"""").find(userMessage)?.groupValues?.get(1)
            ?: "2026-03"
        val summary = MonthlySummary(
            month = month,
            text = "[MOCK] Focused on hypertrophy. Bench 1RM estimated at 80kg (up from 70kg). " +
                "Consistent 4x/week schedule. Prefers push/pull/legs split."
        )
        return json.encodeToString(summary)
    }

    private fun extractTarget(userMessage: String): String {
        val focusMatch = Regex("""Focus:\s*(.+)""", RegexOption.IGNORE_CASE).find(userMessage)
        return focusMatch?.groupValues?.get(1)?.trim()?.lowercase() ?: ""
    }

    private fun pickExercises(target: String, systemPrompt: String): List<GeneratedExercise> {
        // Filter catalog by target muscle group if specified
        val muscleGroup = MuscleGroup.entries.find { target.contains(it.name, ignoreCase = true) }

        val pool = if (muscleGroup != null) {
            ExerciseCatalog.exercises.filter {
                it.primaryMuscle == muscleGroup || muscleGroup in it.secondaryMuscles
            }
        } else {
            // No specific target — pick a balanced mix
            ExerciseCatalog.exercises
        }

        val selected = pool.shuffled().take(5)

        return selected.mapIndexed { i, ex ->
            val isCompound = ex.category == ExerciseCategory.COMPOUND
            GeneratedExercise(
                name = ex.name,
                plannedSets = if (isCompound) 4 else 3,
                plannedReps = if (isCompound) 8 else 12,
                suggestedWeightKg = listOf(20.0, 30.0, 40.0, 50.0, 60.0)[i % 5],
                reason = "[MOCK] ${ex.primaryMuscle.name.lowercase()} ${ex.category.name.lowercase()}"
            )
        }
    }
}
