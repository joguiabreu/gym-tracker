package com.gymtracker.data

/**
 * In-memory repository. Will be replaced with SQLDelight persistence.
 */
class GymRepository {
    private var nextSessionId = 1L
    private var nextExerciseId = 1L
    private var nextSetId = 1L

    private val sessions = mutableListOf<WorkoutSession>()

    fun getSessions(): List<WorkoutSession> = sessions.toList()

    fun getSession(id: Long): WorkoutSession? = sessions.find { it.id == id }

    fun addSession(name: String, date: String): WorkoutSession {
        val session = WorkoutSession(id = nextSessionId++, name = name, date = date)
        sessions.add(session)
        return session
    }

    fun addExercise(sessionId: Long, name: String): WorkoutSession? {
        val index = sessions.indexOfFirst { it.id == sessionId }
        if (index == -1) return null

        val exercise = Exercise(id = nextExerciseId++, name = name)
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

    fun deleteSession(id: Long) {
        sessions.removeAll { it.id == id }
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
