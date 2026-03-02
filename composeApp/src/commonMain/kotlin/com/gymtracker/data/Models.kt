package com.gymtracker.data

data class WorkoutSession(
    val id: Long,
    val name: String,           // e.g. "Thursday training"
    val date: String,           // ISO format: "2026-02-21"
    val exercises: List<Exercise> = emptyList()
)

data class Exercise(
    val id: Long,
    val name: String,           // e.g. "Bicep Curl"
    val muscleGroup: String = "", // e.g. "Arms"
    val plannedSets: Int = 0,
    val plannedReps: Int = 0,
    val sets: List<WorkoutSet> = emptyList()
)

data class WorkoutSet(
    val id: Long,
    val reps: Int,
    val weightKg: Double
)

data class ExerciseProgress(
    val date: String,           // ISO "2026-02-21"
    val sessionName: String,
    val maxWeightKg: Double,
    val totalVolume: Double     // sum of (reps × weight) across all sets
)
