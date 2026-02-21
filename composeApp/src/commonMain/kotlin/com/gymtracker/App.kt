package com.gymtracker

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.gymtracker.data.GymRepository
import com.gymtracker.ui.HomeScreen
import com.gymtracker.ui.ProgressionScreen
import com.gymtracker.ui.SessionDetailScreen

private sealed class Screen {
    data object Home : Screen()
    data class SessionDetail(val sessionId: Long) : Screen()
    data class Progression(val exerciseName: String? = null) : Screen()
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
                onProgressionClick = {
                    currentScreen = Screen.Progression()
                }
            )
            is Screen.SessionDetail -> SessionDetailScreen(
                sessionId = screen.sessionId,
                repository = repository,
                onBack = { currentScreen = Screen.Home },
                onExerciseProgressionClick = { exerciseName ->
                    currentScreen = Screen.Progression(exerciseName)
                }
            )
            is Screen.Progression -> ProgressionScreen(
                repository = repository,
                initialExercise = screen.exerciseName,
                onBack = { currentScreen = Screen.Home }
            )
        }
    }
}
