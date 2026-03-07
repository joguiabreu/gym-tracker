package com.gymtracker.data

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * In-memory repository. Will be replaced with SQLDelight persistence.
 */
class GymRepository {
    private var nextSessionId = 1L
    private var nextExerciseId = 1L
    private var nextSetId = 1L
    private var nextPlanId = 1L

    private val sessions = mutableListOf<WorkoutSession>()
    private val plans = mutableListOf<WorkoutPlan>()
    private var userProfile: UserProfile? = null
    private var currentSplit: WeeklySplit? = null
    private val weeklySummaries = mutableListOf<WeeklySummary>()
    private val monthlySummaries = mutableListOf<MonthlySummary>()

    // ── User Profile ──

    fun getProfile(): UserProfile? = userProfile

    fun hasProfile(): Boolean = userProfile != null

    fun saveProfile(profile: UserProfile) {
        userProfile = profile
    }

    // ── Weekly Split ──

    fun getCurrentSplit(): WeeklySplit? = currentSplit

    fun saveSplit(split: WeeklySplit) {
        currentSplit = split
    }

    fun markSplitDayCompleted(dayOfWeek: String) {
        val split = currentSplit ?: return
        currentSplit = split.copy(
            days = split.days.map {
                if (it.dayOfWeek == dayOfWeek) it.copy(completed = true) else it
            }
        )
    }

    // ── Summaries ──

    fun getWeeklySummaries(): List<WeeklySummary> = weeklySummaries.toList()

    fun addWeeklySummary(summary: WeeklySummary) {
        weeklySummaries.removeAll { it.weekStart == summary.weekStart }
        weeklySummaries.add(summary)
    }

    fun getMonthlySummaries(): List<MonthlySummary> = monthlySummaries.toList()

    fun addMonthlySummary(summary: MonthlySummary) {
        monthlySummaries.removeAll { it.month == summary.month }
        monthlySummaries.add(summary)
    }

    fun getFinishedSessions(): List<WorkoutSession> =
        sessions.filter { it.isFinished }

    // ── Sessions ──

    fun getSessions(): List<WorkoutSession> = sessions.toList()

    fun getSession(id: Long): WorkoutSession? = sessions.find { it.id == id }

    fun addSession(name: String, date: String): WorkoutSession {
        val session = WorkoutSession(id = nextSessionId++, name = name, date = date)
        sessions.add(session)
        return session
    }

    fun addExercise(
        sessionId: Long,
        name: String,
        muscleGroup: String = "",
        plannedSets: Int = 0,
        plannedReps: Int = 0,
        repDurationSeconds: Int = 3,
        restBetweenSetsSeconds: Int = 60
    ): WorkoutSession? {
        val index = sessions.indexOfFirst { it.id == sessionId }
        if (index == -1) return null

        val exercise = Exercise(
            id = nextExerciseId++,
            name = name,
            muscleGroup = muscleGroup,
            plannedSets = plannedSets,
            plannedReps = plannedReps,
            repDurationSeconds = repDurationSeconds,
            restBetweenSetsSeconds = restBetweenSetsSeconds
        )
        val updated = sessions[index].copy(exercises = sessions[index].exercises + exercise)
        sessions[index] = updated
        return updated
    }

    fun addSet(sessionId: Long, exerciseId: Long, reps: Int, weightKg: Double): WorkoutSession? {
        val index = sessions.indexOfFirst { it.id == sessionId }
        if (index == -1) return null

        val session = sessions[index]
        val set = WorkoutSet(id = nextSetId++, reps = reps, weightKg = weightKg)
        val updatedExercises = session.exercises.map { exercise ->
            if (exercise.id == exerciseId) exercise.copy(sets = exercise.sets + set)
            else exercise
        }
        val updated = session.copy(exercises = updatedExercises)
        sessions[index] = updated
        return updated
    }

    fun updateExercise(sessionId: Long, updatedExercise: Exercise): WorkoutSession? {
        val index = sessions.indexOfFirst { it.id == sessionId }
        if (index == -1) return null

        val session = sessions[index]
        val updatedExercises = session.exercises.map { exercise ->
            if (exercise.id == updatedExercise.id) updatedExercise else exercise
        }
        val updated = session.copy(exercises = updatedExercises)
        sessions[index] = updated
        return updated
    }

    fun updateSet(sessionId: Long, exerciseId: Long, setId: Long, reps: Int, weightKg: Double): WorkoutSession? {
        val index = sessions.indexOfFirst { it.id == sessionId }
        if (index == -1) return null

        val session = sessions[index]
        val updatedExercises = session.exercises.map { exercise ->
            if (exercise.id == exerciseId) {
                exercise.copy(sets = exercise.sets.map { set ->
                    if (set.id == setId) set.copy(reps = reps, weightKg = weightKg) else set
                })
            } else exercise
        }
        val updated = session.copy(exercises = updatedExercises)
        sessions[index] = updated
        return updated
    }

    fun finishSession(id: Long): WorkoutSession? {
        val index = sessions.indexOfFirst { it.id == id }
        if (index == -1) return null
        val updated = sessions[index].copy(isFinished = true)
        sessions[index] = updated
        return updated
    }

    fun deleteSession(id: Long) {
        sessions.removeAll { it.id == id }
    }

    // ── Workout Plans ──

    fun getPlans(): List<WorkoutPlan> = plans.toList()

    fun getPlan(id: Long): WorkoutPlan? = plans.find { it.id == id }

    fun addPlan(name: String): WorkoutPlan {
        val plan = WorkoutPlan(id = nextPlanId++, name = name)
        plans.add(plan)
        return plan
    }

    fun addExerciseToPlan(
        planId: Long,
        name: String,
        muscleGroup: String = "",
        plannedSets: Int = 0,
        plannedReps: Int = 0,
        repDurationSeconds: Int = 3,
        restBetweenSetsSeconds: Int = 60
    ): WorkoutPlan? {
        val index = plans.indexOfFirst { it.id == planId }
        if (index == -1) return null

        val exercise = Exercise(
            id = nextExerciseId++,
            name = name,
            muscleGroup = muscleGroup,
            plannedSets = plannedSets,
            plannedReps = plannedReps,
            repDurationSeconds = repDurationSeconds,
            restBetweenSetsSeconds = restBetweenSetsSeconds
        )
        val updated = plans[index].copy(exercises = plans[index].exercises + exercise)
        plans[index] = updated
        return updated
    }

    fun deletePlan(id: Long) {
        plans.removeAll { it.id == id }
    }

    fun createSessionFromPlan(planId: Long): WorkoutSession? {
        val plan = getPlan(planId) ?: return null
        val now = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
        val h = now.hour.toString().padStart(2, '0')
        val m = now.minute.toString().padStart(2, '0')
        val name = "${now.date} $h:$m"
        val date = now.date.toString()

        val session = addSession(name, date)
        plan.exercises.forEach { exercise ->
            addExercise(
                session.id, exercise.name, exercise.muscleGroup,
                exercise.plannedSets, exercise.plannedReps,
                exercise.repDurationSeconds, exercise.restBetweenSetsSeconds
            )
        }
        return getSession(session.id)
    }

    /** All distinct exercise names across all sessions, sorted alphabetically. */
    fun getAllExerciseNames(): List<String> =
        sessions.flatMap { it.exercises }
            .map { it.name }
            .distinct()
            .sorted()

    /**
     * Returns one data point per session where the exercise appears,
     * sorted by date ascending. Each point holds the max weight lifted
     * and total volume (reps × weight) in that session.
     */
    fun getExerciseProgression(name: String): List<ExerciseProgress> =
        sessions
            .mapNotNull { session ->
                val exercise = session.exercises
                    .find { it.name.equals(name, ignoreCase = true) }
                    ?.takeIf { it.sets.isNotEmpty() }
                    ?: return@mapNotNull null

                ExerciseProgress(
                    date = session.date,
                    sessionName = session.name,
                    maxWeightKg = exercise.sets.maxOf { it.weightKg },
                    totalVolume = exercise.sets.sumOf { it.reps * it.weightKg }
                )
            }
            .sortedBy { it.date }
}
