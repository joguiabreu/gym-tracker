package com.gymtracker

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.gymtracker.data.GymRepository
import com.gymtracker.ui.HomeScreen
import com.gymtracker.ui.NewPlanScreen
import com.gymtracker.ui.NewSessionScreen
import com.gymtracker.ui.ProgressionScreen
import com.gymtracker.ui.SessionDetailScreen
import com.gymtracker.ui.WorkoutPlansScreen

private sealed class Screen {
    data object Home : Screen()
    data object NewSession : Screen()
    data class SessionDetail(val sessionId: Long) : Screen()
    data class Progression(val exerciseName: String? = null) : Screen()
    data object Plans : Screen()
    data object NewPlan : Screen()
}

@Composable
fun App() {
    val repository = remember { GymRepository() }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    MaterialTheme {
        when (val screen = currentScreen) {
            is Screen.Home -> HomeScreen(
                repository = repository,
                onSessionClick = { session ->
                    currentScreen = Screen.SessionDetail(session.id)
                },
                onNewSession = { currentScreen = Screen.NewSession },
                onProgressionClick = { currentScreen = Screen.Progression() },
                onPlansClick = { currentScreen = Screen.Plans },
                onStartPlan = { planId ->
                    val session = repository.createSessionFromPlan(planId)
                    if (session != null) {
                        currentScreen = Screen.SessionDetail(session.id)
                    }
                },
                onNewPlan = { currentScreen = Screen.NewPlan }
            )
            is Screen.NewSession -> NewSessionScreen(
                repository = repository,
                onSave = { currentScreen = Screen.Home },
                onCancel = { currentScreen = Screen.Home }
            )
            is Screen.SessionDetail -> SessionDetailScreen(
                sessionId = screen.sessionId,
                repository = repository,
                onBack = { currentScreen = Screen.Home },
                onFinish = { currentScreen = Screen.Home },
                onExerciseProgressionClick = { exerciseName ->
                    currentScreen = Screen.Progression(exerciseName)
                }
            )
            is Screen.Progression -> ProgressionScreen(
                repository = repository,
                initialExercise = screen.exerciseName,
                onBack = { currentScreen = Screen.Home }
            )
            is Screen.Plans -> WorkoutPlansScreen(
                repository = repository,
                onNewPlan = { currentScreen = Screen.NewPlan },
                onStartWorkout = { sessionId ->
                    currentScreen = Screen.SessionDetail(sessionId)
                },
                onBack = { currentScreen = Screen.Home }
            )
            is Screen.NewPlan -> NewPlanScreen(
                repository = repository,
                onSave = { currentScreen = Screen.Plans },
                onCancel = { currentScreen = Screen.Plans }
            )
        }
    }
}
