package com.gymtracker.ai

import com.gymtracker.data.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days

class MockWorkoutService : WorkoutAiService {

    override suspend fun generate(
        profile: UserProfile,
        target: String,
        recentSessions: List<WorkoutSession>
    ): Result<GeneratedWorkout> {
        val available = ExerciseCatalog.forEquipmentSet(profile.equipment)
        val picked = available.shuffled().take(5)
        val isBeginner = profile.experience == ExperienceLevel.BEGINNER

        val exercises = picked.map { ex ->
            GeneratedExercise(
                name = ex.name,
                plannedSets = if (isBeginner) 3 else 4,
                plannedReps = if (ex.category == ExerciseCategory.COMPOUND) 8 else 12,
                suggestedWeightKg = when {
                    ex.equipment.contains(Equipment.NONE) -> 0.0
                    isBeginner -> 20.0
                    else -> 40.0
                },
                reason = "Good for ${ex.primaryMuscle.name.lowercase()}"
            )
        }

        return Result.success(
            GeneratedWorkout(
                exercises = exercises,
                reasoning = "Mock workout — ${picked.size} exercises selected based on your equipment and experience."
            )
        )
    }

    override suspend fun resuggest(
        profile: UserProfile,
        kept: List<GeneratedExercise>,
        rejected: List<Pair<GeneratedExercise, String>>
    ): Result<GeneratedWorkout> {
        val rejectedNames = rejected.map { it.first.name }.toSet()
        val keptNames = kept.map { it.name }.toSet()
        val available = ExerciseCatalog.forEquipmentSet(profile.equipment)
            .filter { it.name !in rejectedNames && it.name !in keptNames }

        val replacements = available.shuffled().take(rejected.size)
        val isBeginner = profile.experience == ExperienceLevel.BEGINNER

        val newExercises = replacements.map { ex ->
            GeneratedExercise(
                name = ex.name,
                plannedSets = if (isBeginner) 3 else 4,
                plannedReps = if (ex.category == ExerciseCategory.COMPOUND) 8 else 12,
                suggestedWeightKg = when {
                    ex.equipment.contains(Equipment.NONE) -> 0.0
                    isBeginner -> 20.0
                    else -> 40.0
                },
                reason = "Replacement for rejected exercise"
            )
        }

        return Result.success(
            GeneratedWorkout(
                exercises = kept + newExercises,
                reasoning = "Mock re-suggestion — replaced ${rejected.size} exercise(s)."
            )
        )
    }

    override suspend fun generateWeeklySplit(
        profile: UserProfile,
        recentSessions: List<WorkoutSession>
    ): Result<WeeklySplit> {
        val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val splits = when {
            profile.daysPerWeek <= 2 -> listOf("Full Body", "Full Body")
            profile.daysPerWeek == 3 -> listOf("Push (Chest/Shoulders/Triceps)", "Pull (Back/Biceps)", "Legs/Core")
            profile.daysPerWeek == 4 -> listOf("Chest/Triceps", "Back/Biceps", "Legs/Glutes", "Shoulders/Arms")
            profile.daysPerWeek == 5 -> listOf("Chest", "Back", "Legs", "Shoulders/Arms", "Full Body")
            else -> listOf("Chest", "Back", "Quads/Glutes", "Shoulders", "Arms", "Hamstrings/Core")
        }

        val days = dayNames.take(7).mapIndexed { index, day ->
            if (index < splits.size) SplitDay(dayOfWeek = day, focus = splits[index])
            else SplitDay(dayOfWeek = day, focus = "Rest")
        }

        val now = Clock.System.now()
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val dayOfWeek = today.dayOfWeek.ordinal
        val monday = (now - dayOfWeek.days).toLocalDateTime(TimeZone.currentSystemDefault()).date

        return Result.success(WeeklySplit(weekStart = monday.toString(), days = days))
    }

    override suspend fun generateWeeklySummary(
        sessions: List<WorkoutSession>
    ): Result<WeeklySummary> {
        val exerciseNames = sessions.flatMap { it.exercises }.map { it.name }.distinct()
        val muscleGroups = sessions.flatMap { it.exercises }
            .map { it.muscleGroup }.filter { it.isNotBlank() }.distinct()

        val now = Clock.System.now()
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val dayOfWeek = today.dayOfWeek.ordinal
        val monday = (now - dayOfWeek.days).toLocalDateTime(TimeZone.currentSystemDefault()).date

        val text = buildString {
            append("${sessions.size} sessions completed. ")
            if (muscleGroups.isNotEmpty()) {
                append("Trained: ${muscleGroups.joinToString(", ")}. ")
            }
            append("Exercises: ${exerciseNames.take(5).joinToString(", ")}.")
        }

        return Result.success(WeeklySummary(weekStart = monday.toString(), text = text))
    }

    override suspend fun generateMonthlySummary(
        weeklySummaries: List<WeeklySummary>,
        month: String
    ): Result<MonthlySummary> {
        val text = "Month $month: ${weeklySummaries.size} weeks of training logged. Mock monthly trend summary."
        return Result.success(MonthlySummary(month = month, text = text))
    }
}
