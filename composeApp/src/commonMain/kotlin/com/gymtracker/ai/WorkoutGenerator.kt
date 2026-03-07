package com.gymtracker.ai

import com.gymtracker.data.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── AI response schema ──

@Serializable
data class GeneratedWorkout(
    val exercises: List<GeneratedExercise>,
    val reasoning: String = ""
)

@Serializable
data class GeneratedExercise(
    val name: String,
    @SerialName("planned_sets") val plannedSets: Int,
    @SerialName("planned_reps") val plannedReps: Int,
    @SerialName("suggested_weight_kg") val suggestedWeightKg: Double = 0.0,
    val reason: String = ""
)

// ── Prompt builder (used by ClaudeWorkoutService) ──

object PromptBuilder {

    fun buildSystemPrompt(profile: UserProfile): String {
        val equipmentNames = if (profile.equipment.isEmpty()) "None (bodyweight only)"
        else profile.equipment.joinToString(", ") { it.name.lowercase().replace('_', ' ') }

        val availableExercises = ExerciseCatalog.forEquipmentSet(profile.equipment)
            .joinToString("\n") { ex ->
                val muscles = listOf(ex.primaryMuscle.name.lowercase()) +
                    ex.secondaryMuscles.map { it.name.lowercase() }
                "- ${ex.name} (${muscles.joinToString(", ")}) [${ex.category.name.lowercase()}]"
            }

        val injuryNote = if (profile.injuries.isNotBlank())
            "\n\nINJURIES/LIMITATIONS: ${profile.injuries}\nAvoid exercises that aggravate these conditions. Suggest safer alternatives."
        else ""

        return """
You are a personal trainer AI inside a workout tracking app.

USER PROFILE:
- Goal: ${profile.goal}
- Schedule: ${profile.daysPerWeek} days/week
- Equipment: $equipmentNames
- Experience: ${profile.experience.name.lowercase()}$injuryNote

AVAILABLE EXERCISES (pick ONLY from this list):
$availableExercises

RULES:
1. Only use exercise names from the list above — exact spelling matters for tracking.
2. Pick 4-6 exercises per workout unless the user asks otherwise.
3. Suggest sets (2-5), reps (5-20), and weight based on experience level.
4. For beginners, start conservative. For advanced, push harder.
5. Balance the workout — don't overload one muscle group unless requested.
6. If the user gives a reason for rejecting an exercise, respect it in re-suggestions.

RESPONSE FORMAT: Return ONLY valid JSON, no markdown, no explanation outside the JSON.
```json
{
  "exercises": [
    {
      "name": "Exercise Name",
      "planned_sets": 3,
      "planned_reps": 10,
      "suggested_weight_kg": 40.0,
      "reason": "Brief reason for including this exercise"
    }
  ],
  "reasoning": "Brief overall workout reasoning"
}
```
""".trimIndent()
    }

    fun buildGenerateMessage(
        target: String = "",
        recentSessions: List<WorkoutSession> = emptyList()
    ): String {
        val parts = mutableListOf<String>()

        if (target.isNotBlank()) {
            parts.add("Focus: $target")
        } else {
            parts.add("Generate a workout for today. Pick the best muscle groups based on my goal and any recent history.")
        }

        if (recentSessions.isNotEmpty()) {
            parts.add("\nRecent sessions:")
            recentSessions.takeLast(3).forEach { session ->
                val exercises = session.exercises.joinToString(", ") { ex ->
                    val weight = ex.sets.maxOfOrNull { it.weightKg }?.let { " @${it}kg" } ?: ""
                    "${ex.name}$weight"
                }
                parts.add("- ${session.date}: $exercises")
            }
        }

        return parts.joinToString("\n")
    }

    fun buildResuggestMessage(
        kept: List<GeneratedExercise>,
        rejected: List<Pair<GeneratedExercise, String>>
    ): String {
        val parts = mutableListOf("Some exercises were rejected. Re-suggest replacements.\n")

        parts.add("KEEP these exercises (already accepted):")
        kept.forEach { parts.add("- ${it.name}: ${it.plannedSets}x${it.plannedReps}") }

        parts.add("\nREJECTED (need replacements):")
        rejected.forEach { (ex, reason) ->
            parts.add("- ${ex.name} — reason: $reason")
        }

        parts.add("\nReturn a complete workout with the kept exercises in their positions and new exercises replacing the rejected ones.")

        return parts.joinToString("\n")
    }
}
