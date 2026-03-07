package com.gymtracker.ai

import com.gymtracker.data.*

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
}
