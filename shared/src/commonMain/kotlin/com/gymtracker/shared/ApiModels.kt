package com.gymtracker.shared

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Enums ──

@Serializable
enum class Equipment {
    BARBELL, DUMBBELL, CABLE, MACHINE, NONE, PULL_UP_BAR, DIP_STATION, KETTLEBELL, BAND
}

@Serializable
enum class ExperienceLevel {
    BEGINNER, INTERMEDIATE, ADVANCED
}

// ── User Profile ──

@Serializable
data class UserProfile(
    val goal: String,
    @SerialName("days_per_week") val daysPerWeek: Int,
    val equipment: Set<Equipment>,
    val experience: ExperienceLevel,
    val injuries: String = ""
)

// ── Session data (sent by app as context) ──

@Serializable
data class SessionData(
    val date: String,
    val exercises: List<ExerciseData>
)

@Serializable
data class ExerciseData(
    val name: String,
    @SerialName("muscle_group") val muscleGroup: String = "",
    val sets: List<SetData> = emptyList()
)

@Serializable
data class SetData(
    val reps: Int,
    @SerialName("weight_kg") val weightKg: Double
)

// ── AI Response types ──

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

// ── Weekly Split ──

@Serializable
data class WeeklySplit(
    @SerialName("week_start") val weekStart: String,
    val days: List<SplitDay>
)

@Serializable
data class SplitDay(
    @SerialName("day_of_week") val dayOfWeek: String,
    val focus: String,
    val completed: Boolean = false
)

// ── Summaries ──

@Serializable
data class WeeklySummary(
    @SerialName("week_start") val weekStart: String,
    val text: String
)

@Serializable
data class MonthlySummary(
    val month: String,
    val text: String
)

// ── API Request types ──

@Serializable
data class GenerateRequest(
    val profile: UserProfile,
    val target: String = "",
    @SerialName("recent_sessions") val recentSessions: List<SessionData> = emptyList(),
    @SerialName("weekly_summaries") val weeklySummaries: List<WeeklySummary> = emptyList(),
    @SerialName("monthly_summaries") val monthlySummaries: List<MonthlySummary> = emptyList()
)

@Serializable
data class ResuggestRequest(
    val profile: UserProfile,
    val kept: List<GeneratedExercise>,
    val rejected: List<RejectedExercise>
)

@Serializable
data class RejectedExercise(
    val name: String,
    @SerialName("planned_sets") val plannedSets: Int,
    @SerialName("planned_reps") val plannedReps: Int,
    val reason: String
)

@Serializable
data class SplitRequest(
    val profile: UserProfile,
    @SerialName("recent_sessions") val recentSessions: List<SessionData> = emptyList()
)

@Serializable
data class WeeklySummaryRequest(
    val sessions: List<SessionData>
)

@Serializable
data class MonthlySummaryRequest(
    @SerialName("weekly_summaries") val weeklySummaries: List<WeeklySummary>,
    val month: String
)

@Serializable
data class ErrorResponse(
    val error: String
)
