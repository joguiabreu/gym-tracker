package com.gymtracker.ai

import com.gymtracker.data.UserProfile
import com.gymtracker.data.WorkoutSession

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
}
