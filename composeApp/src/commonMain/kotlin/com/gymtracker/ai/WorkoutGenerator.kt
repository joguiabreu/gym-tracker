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
        recentSessions: List<WorkoutSession> = emptyList(),
        weeklySplit: WeeklySplit? = null,
        weeklySummaries: List<WeeklySummary> = emptyList(),
        monthlySummaries: List<MonthlySummary> = emptyList()
    ): String {
        val parts = mutableListOf<String>()

        if (target.isNotBlank()) {
            parts.add("Focus: $target")
        } else {
            parts.add("Generate a workout for today. Pick the best muscle groups based on my goal and any recent history.")
        }

        if (weeklySplit != null) {
            parts.add("\nWeekly split plan (week of ${weeklySplit.weekStart}):")
            weeklySplit.days.forEach { day ->
                val status = if (day.completed) " [done]" else ""
                parts.add("  ${day.dayOfWeek}: ${day.focus}$status")
            }
        }

        if (recentSessions.isNotEmpty()) {
            parts.add("\nRecent completed sessions (actuals, not prescriptions):")
            recentSessions.takeLast(3).forEach { session ->
                parts.add("- ${session.date}:")
                session.exercises.forEach { ex ->
                    if (ex.sets.isNotEmpty()) {
                        val setDetails = ex.sets.joinToString(", ") { "${it.reps}r@${it.weightKg}kg" }
                        parts.add("    ${ex.name}: $setDetails")
                    } else {
                        parts.add("    ${ex.name}: (no sets logged)")
                    }
                }
            }
        }

        if (weeklySummaries.isNotEmpty()) {
            parts.add("\nWeekly summaries:")
            weeklySummaries.forEach { parts.add("- Week of ${it.weekStart}: ${it.text}") }
        }

        if (monthlySummaries.isNotEmpty()) {
            parts.add("\nMonthly trends:")
            monthlySummaries.forEach { parts.add("- ${it.month}: ${it.text}") }
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

    fun buildWeeklySplitPrompt(
        profile: UserProfile,
        recentSessions: List<WorkoutSession>
    ): Pair<String, String> {
        val system = """
You are a personal trainer planning a weekly training split.

USER PROFILE:
- Goal: ${profile.goal}
- Schedule: ${profile.daysPerWeek} days/week
- Experience: ${profile.experience.name.lowercase()}
${if (profile.injuries.isNotBlank()) "- Injuries: ${profile.injuries}" else ""}

Return ONLY valid JSON:
{"weekStart":"YYYY-MM-DD","days":[{"dayOfWeek":"Monday","focus":"Chest/Triceps","completed":false},...]}
Include all 7 days. Use "Rest" for non-training days. Spread training days for recovery.
""".trimIndent()

        val user = buildString {
            append("Plan a weekly split for me.")
            if (recentSessions.isNotEmpty()) {
                append("\n\nRecent training:")
                recentSessions.takeLast(3).forEach { session ->
                    val muscles = session.exercises.map { it.muscleGroup }
                        .filter { it.isNotBlank() }.distinct()
                    append("\n- ${session.date}: ${muscles.joinToString(", ")}")
                }
            }
        }

        return system to user
    }

    fun buildWeeklySummaryPrompt(sessions: List<WorkoutSession>): String {
        val parts = mutableListOf("Compress these workout sessions into a ~100 token summary.\n")
        sessions.forEach { session ->
            parts.add("${session.date}:")
            session.exercises.forEach { ex ->
                if (ex.sets.isNotEmpty()) {
                    val best = ex.sets.maxByOrNull { it.weightKg }
                    parts.add("  ${ex.name}: ${ex.sets.size} sets, best ${best?.reps}r@${best?.weightKg}kg")
                } else {
                    parts.add("  ${ex.name}: no sets logged")
                }
            }
        }
        parts.add("\nReturn ONLY JSON: {\"weekStart\":\"YYYY-MM-DD\",\"text\":\"summary here\"}")
        return parts.joinToString("\n")
    }

    fun buildMonthlySummaryPrompt(
        weeklySummaries: List<WeeklySummary>,
        month: String
    ): String {
        val parts = mutableListOf("Compress these weekly summaries into a ~100 token monthly trend report.\n")
        weeklySummaries.forEach { parts.add("Week of ${it.weekStart}: ${it.text}") }
        parts.add("\nReturn ONLY JSON: {\"month\":\"$month\",\"text\":\"monthly trend summary here\"}")
        return parts.joinToString("\n")
    }
}
