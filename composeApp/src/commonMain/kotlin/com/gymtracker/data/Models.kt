package com.gymtracker.data

import kotlinx.serialization.Serializable

data class WorkoutSession(
    val id: Long,
    val name: String,           // e.g. "Thursday training"
    val date: String,           // ISO format: "2026-02-21"
    val exercises: List<Exercise> = emptyList(),
    val isFinished: Boolean = false
)

data class Exercise(
    val id: Long,
    val name: String,           // e.g. "Bicep Curl"
    val muscleGroup: String = "", // e.g. "Arms"
    val plannedSets: Int = 0,
    val plannedReps: Int = 0,
    val sets: List<WorkoutSet> = emptyList(),
    val repDurationSeconds: Int = 3,
    val restBetweenSetsSeconds: Int = 60,
    val isCompleted: Boolean = false,
    val actualRestAfterSeconds: Int? = null
)

data class WorkoutSet(
    val id: Long,
    val reps: Int,
    val weightKg: Double
)

fun Exercise.estimatedDurationSeconds(): Int {
    if (plannedSets <= 0) return 0
    val workTime = plannedSets * plannedReps * repDurationSeconds
    val restTime = (plannedSets - 1) * restBetweenSetsSeconds
    return workTime + restTime
}

fun WorkoutSession.estimatedTotalDurationSeconds(): Int =
    exercises.sumOf { exercise ->
        if (exercise.isCompleted) {
            // Use actual logged data: real set count, real reps, plus actual rest after
            val workTime = exercise.sets.sumOf { it.reps * exercise.repDurationSeconds }
            val restTime = (exercise.sets.size - 1).coerceAtLeast(0) * exercise.restBetweenSetsSeconds
            val restAfter = exercise.actualRestAfterSeconds ?: 0
            workTime + restTime + restAfter
        } else {
            exercise.estimatedDurationSeconds()
        }
    }

fun formatDuration(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}

data class WorkoutPlan(
    val id: Long,
    val name: String,
    val exercises: List<Exercise> = emptyList()
)

fun WorkoutPlan.estimatedTotalDurationSeconds(): Int =
    exercises.sumOf { it.estimatedDurationSeconds() }

enum class ExperienceLevel {
    BEGINNER, INTERMEDIATE, ADVANCED
}

data class UserProfile(
    val goal: String,                       // e.g. "Get bulkier upper body"
    val daysPerWeek: Int,                   // e.g. 4
    val equipment: Set<Equipment>,          // what the user has access to
    val experience: ExperienceLevel,
    val injuries: String = ""               // free text, e.g. "bad left knee"
)

@Serializable
data class WeeklySplit(
    val weekStart: String,                  // ISO date of Monday, e.g. "2026-03-02"
    val days: List<SplitDay>
)

@Serializable
data class SplitDay(
    val dayOfWeek: String,                  // e.g. "Monday"
    val focus: String,                      // e.g. "Chest/Triceps"
    val completed: Boolean = false
)

@Serializable
data class WeeklySummary(
    val weekStart: String,                  // ISO date of Monday
    val text: String                        // AI-compressed summary ~100 tokens
)

@Serializable
data class MonthlySummary(
    val month: String,                      // e.g. "2026-03"
    val text: String                        // AI-compressed trends ~100 tokens
)

data class ExerciseProgress(
    val date: String,           // ISO "2026-02-21"
    val sessionName: String,
    val maxWeightKg: Double,
    val totalVolume: Double     // sum of (reps × weight) across all sets
)
