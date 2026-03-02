package com.gymtracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymtracker.data.Exercise
import com.gymtracker.data.GymRepository
import com.gymtracker.data.WorkoutSet
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSessionScreen(
    repository: GymRepository,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    var exercises by remember { mutableStateOf(listOf<Exercise>()) }
    var nextLocalExerciseId by remember { mutableStateOf(1L) }
    var nextLocalSetId by remember { mutableStateOf(1L) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var addSetForExercise by remember { mutableStateOf<Exercise?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Session") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                        val h = now.hour.toString().padStart(2, '0')
                        val m = now.minute.toString().padStart(2, '0')
                        val name = "${now.date} $h:$m"
                        val date = now.date.toString()

                        val session = repository.addSession(name, date)
                        exercises.forEach { exercise ->
                            repository.addExercise(session.id, exercise.name)
                            val savedExercise = repository.getSession(session.id)!!.exercises.last()
                            exercise.sets.forEach { set ->
                                repository.addSet(session.id, savedExercise.id, set.reps, set.weightKg)
                            }
                        }
                        onSave()
                    }) {
                        Text("Save")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddExerciseDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add exercise")
            }
        }
    ) { padding ->
        if (exercises.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No exercises yet. Tap + to add one!", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
            ) {
                val targeted = exercises.map { it.muscleGroup }.filter { it.isNotBlank() }.distinct()
                if (targeted.isNotEmpty()) {
                    item {
                        Text(
                            "Targeted: ${targeted.joinToString(" · ")}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
                items(exercises) { exercise ->
                    ExerciseCard(
                        exercise = exercise,
                        onAddSet = { addSetForExercise = exercise }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    if (showAddExerciseDialog) {
        AddExerciseDialog(
            onConfirm = { name, muscleGroup, plannedSets, plannedReps ->
                exercises = exercises + Exercise(
                    id = nextLocalExerciseId++,
                    name = name,
                    muscleGroup = muscleGroup,
                    plannedSets = plannedSets,
                    plannedReps = plannedReps
                )
                showAddExerciseDialog = false
            },
            onDismiss = { showAddExerciseDialog = false }
        )
    }

    addSetForExercise?.let { exercise ->
        AddSetDialog(
            exerciseName = exercise.name,
            onConfirm = { reps, weightKg ->
                val newSet = WorkoutSet(id = nextLocalSetId++, reps = reps, weightKg = weightKg)
                exercises = exercises.map { ex ->
                    if (ex.id == exercise.id) ex.copy(sets = ex.sets + newSet) else ex
                }
                addSetForExercise = null
            },
            onDismiss = { addSetForExercise = null }
        )
    }
}
