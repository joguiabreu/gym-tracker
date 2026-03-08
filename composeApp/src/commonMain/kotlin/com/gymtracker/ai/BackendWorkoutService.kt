package com.gymtracker.ai

import com.gymtracker.data.WorkoutSession
import com.gymtracker.shared.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class BackendWorkoutService(
    private val baseUrl: String = "http://localhost:8080"
) : WorkoutAiService {

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
    }

    override suspend fun generate(
        profile: UserProfile,
        target: String,
        recentSessions: List<WorkoutSession>
    ): Result<GeneratedWorkout> = runCatching {
        client.post("$baseUrl/workout/generate") {
            contentType(ContentType.Application.Json)
            setBody(GenerateRequest(
                profile = profile,
                target = target,
                recentSessions = recentSessions.map { it.toSessionData() }
            ))
        }.body<GeneratedWorkout>()
    }

    override suspend fun resuggest(
        profile: UserProfile,
        kept: List<GeneratedExercise>,
        rejected: List<Pair<GeneratedExercise, String>>
    ): Result<GeneratedWorkout> = runCatching {
        client.post("$baseUrl/workout/resuggest") {
            contentType(ContentType.Application.Json)
            setBody(ResuggestRequest(
                profile = profile,
                kept = kept,
                rejected = rejected.map { (ex, reason) ->
                    RejectedExercise(
                        name = ex.name,
                        plannedSets = ex.plannedSets,
                        plannedReps = ex.plannedReps,
                        reason = reason
                    )
                }
            ))
        }.body<GeneratedWorkout>()
    }

    override suspend fun generateWeeklySplit(
        profile: UserProfile,
        recentSessions: List<WorkoutSession>
    ): Result<WeeklySplit> = runCatching {
        client.post("$baseUrl/split/generate") {
            contentType(ContentType.Application.Json)
            setBody(SplitRequest(
                profile = profile,
                recentSessions = recentSessions.map { it.toSessionData() }
            ))
        }.body<WeeklySplit>()
    }

    override suspend fun generateWeeklySummary(
        sessions: List<WorkoutSession>
    ): Result<WeeklySummary> = runCatching {
        client.post("$baseUrl/summary/weekly") {
            contentType(ContentType.Application.Json)
            setBody(WeeklySummaryRequest(
                sessions = sessions.map { it.toSessionData() }
            ))
        }.body<WeeklySummary>()
    }

    override suspend fun generateMonthlySummary(
        weeklySummaries: List<WeeklySummary>,
        month: String
    ): Result<MonthlySummary> = runCatching {
        client.post("$baseUrl/summary/monthly") {
            contentType(ContentType.Application.Json)
            setBody(MonthlySummaryRequest(
                weeklySummaries = weeklySummaries,
                month = month
            ))
        }.body<MonthlySummary>()
    }
}

private fun WorkoutSession.toSessionData(): SessionData = SessionData(
    date = date,
    exercises = exercises.map { ex ->
        ExerciseData(
            name = ex.name,
            muscleGroup = ex.muscleGroup,
            sets = ex.sets.map { set ->
                SetData(reps = set.reps, weightKg = set.weightKg)
            }
        )
    }
)
