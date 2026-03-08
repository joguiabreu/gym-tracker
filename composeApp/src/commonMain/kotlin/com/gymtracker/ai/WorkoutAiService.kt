package com.gymtracker.ai

import com.gymtracker.data.UserProfile
import com.gymtracker.data.WeeklySplit
import com.gymtracker.data.WeeklySummary
import com.gymtracker.data.MonthlySummary
import com.gymtracker.data.WorkoutSession
import com.gymtracker.shared.GeneratedExercise
import com.gymtracker.shared.GeneratedWorkout

interface WorkoutAiService {

    suspend fun generate(
        profile: UserProfile,
        target: String = "",
        recentSessions: List<WorkoutSession> = emptyList()
    ): Result<GeneratedWorkout>

    suspend fun resuggest(
        profile: UserProfile,
        kept: List<GeneratedExercise>,
        rejected: List<Pair<GeneratedExercise, String>>
    ): Result<GeneratedWorkout>

    suspend fun generateWeeklySplit(
        profile: UserProfile,
        recentSessions: List<WorkoutSession> = emptyList()
    ): Result<WeeklySplit>

    suspend fun generateWeeklySummary(
        sessions: List<WorkoutSession>
    ): Result<WeeklySummary>

    suspend fun generateMonthlySummary(
        weeklySummaries: List<WeeklySummary>,
        month: String
    ): Result<MonthlySummary>
}
