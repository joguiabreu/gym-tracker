package com.gymtracker.ai

import com.gymtracker.data.UserProfile
import com.gymtracker.data.WeeklySplit
import com.gymtracker.data.WeeklySummary
import com.gymtracker.data.MonthlySummary
import com.gymtracker.data.WorkoutSession
import com.gymtracker.shared.GeneratedExercise
import com.gymtracker.shared.GeneratedWorkout
import com.gymtracker.util.Logger
import kotlinx.datetime.Clock

/**
 * Decorator that wraps any WorkoutAiService with observability.
 *
 * Why a decorator instead of adding logs inside each service?
 * - Single place to maintain logging logic — not scattered across Mock/Claude/Backend
 * - Any new implementation automatically gets logging
 * - Can be removed or swapped without touching business logic
 * - This is the same pattern production systems use for metrics, tracing, retries, etc.
 *
 * What we track:
 * - Every call: what was requested (input context)
 * - Every result: what came back (output summary)
 * - Every failure: what went wrong (error details)
 * - Timing: how long each call took (critical for spotting slow API calls)
 */
class LoggingWorkoutService(
    private val delegate: WorkoutAiService,
    private val serviceName: String = delegate::class.simpleName ?: "Unknown"
) : WorkoutAiService {

    private val tag = "AI"

    override suspend fun generate(
        profile: UserProfile,
        target: String,
        recentSessions: List<WorkoutSession>
    ): Result<GeneratedWorkout> {
        Logger.info(tag, "generate requested",
            "service" to serviceName,
            "target" to target.ifBlank { "(auto)" },
            "equipment" to profile.equipment.size,
            "experience" to profile.experience,
            "recentSessions" to recentSessions.size
        )
        Logger.debug(tag, "generate profile detail",
            "goal" to profile.goal,
            "daysPerWeek" to profile.daysPerWeek,
            "equipment" to profile.equipment.map { it.name }
        )
        if (recentSessions.isNotEmpty()) {
            Logger.debug(tag, "generate session context",
                "sessions" to recentSessions.map { s ->
                    "${s.name}(${s.exercises.size} exercises, ${s.exercises.sumOf { it.sets.size }} sets)"
                }
            )
        }

        return timed("generate") {
            delegate.generate(profile, target, recentSessions)
        }.also { result ->
            result.onSuccess { workout ->
                Logger.info(tag, "generate succeeded",
                    "exercises" to workout.exercises.size,
                    "names" to workout.exercises.map { it.name }
                )
                Logger.debug(tag, "generate result detail",
                    "exercises" to workout.exercises.map { e ->
                        "${e.name}: ${e.plannedSets}x${e.plannedReps} @${e.suggestedWeightKg}kg"
                    }
                )
            }
        }
    }

    override suspend fun resuggest(
        profile: UserProfile,
        kept: List<GeneratedExercise>,
        rejected: List<Pair<GeneratedExercise, String>>
    ): Result<GeneratedWorkout> {
        Logger.info(tag, "resuggest requested",
            "service" to serviceName,
            "kept" to kept.size,
            "rejected" to rejected.size,
            "reasons" to rejected.map { it.second }
        )
        Logger.debug(tag, "resuggest detail",
            "keptExercises" to kept.map { it.name },
            "rejectedExercises" to rejected.map { "${it.first.name} (${it.second})" }
        )

        return timed("resuggest") {
            delegate.resuggest(profile, kept, rejected)
        }.also { result ->
            result.onSuccess { workout ->
                Logger.info(tag, "resuggest succeeded",
                    "exercises" to workout.exercises.size,
                    "newNames" to workout.exercises.map { it.name }
                )
            }
        }
    }

    override suspend fun generateWeeklySplit(
        profile: UserProfile,
        recentSessions: List<WorkoutSession>
    ): Result<WeeklySplit> {
        Logger.info(tag, "weekly split requested",
            "service" to serviceName,
            "daysPerWeek" to profile.daysPerWeek
        )

        return timed("generateWeeklySplit") {
            delegate.generateWeeklySplit(profile, recentSessions)
        }.also { result ->
            result.onSuccess { split ->
                Logger.info(tag, "weekly split generated",
                    "trainingDays" to split.days.count { it.focus != "Rest" },
                    "restDays" to split.days.count { it.focus == "Rest" }
                )
            }
        }
    }

    override suspend fun generateWeeklySummary(
        sessions: List<WorkoutSession>
    ): Result<WeeklySummary> {
        Logger.info(tag, "weekly summary requested",
            "service" to serviceName,
            "sessions" to sessions.size
        )

        return timed("generateWeeklySummary") {
            delegate.generateWeeklySummary(sessions)
        }.also { result ->
            result.onSuccess {
                Logger.info(tag, "weekly summary generated",
                    "weekStart" to it.weekStart,
                    "textLength" to it.text.length
                )
            }
        }
    }

    override suspend fun generateMonthlySummary(
        weeklySummaries: List<WeeklySummary>,
        month: String
    ): Result<MonthlySummary> {
        Logger.info(tag, "monthly summary requested",
            "service" to serviceName,
            "month" to month,
            "weeksCount" to weeklySummaries.size
        )

        return timed("generateMonthlySummary") {
            delegate.generateMonthlySummary(weeklySummaries, month)
        }.also { result ->
            result.onSuccess {
                Logger.info(tag, "monthly summary generated",
                    "month" to it.month,
                    "textLength" to it.text.length
                )
            }
        }
    }

    /**
     * Measures execution time and logs failures.
     *
     * Timing is the most useful metric in observability — it tells you:
     * - Is the mock fast? (should be <1ms)
     * - Is Claude API slow? (typically 1-3 seconds)
     * - Is the backend healthy? (network + Claude combined)
     * - Is something timing out?
     */
    private suspend fun <T> timed(operation: String, block: suspend () -> Result<T>): Result<T> {
        val start = Clock.System.now()
        val result = block()
        val elapsed = Clock.System.now() - start

        result.onFailure { error ->
            Logger.error(tag, "$operation failed",
                error = error,
                "service" to serviceName,
                "durationMs" to elapsed.inWholeMilliseconds
            )
        }

        Logger.debug(tag, "$operation completed",
            "durationMs" to elapsed.inWholeMilliseconds
        )

        return result
    }
}
