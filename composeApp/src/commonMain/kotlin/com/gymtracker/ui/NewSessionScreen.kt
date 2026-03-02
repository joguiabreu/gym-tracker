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
import com.gymtracker.data.WorkoutSession
import com.gymtracker.data.estimatedTotalDurationSeconds
import com.gymtracker.data.formatDuration
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
    var showAddExerciseDialog by remember { mutableStateOf(false) }

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
                            repository.addExercise(
                                session.id, exercise.name, exercise.muscleGroup,
                                exercise.plannedSets, exercise.plannedReps,
                                exercise.repDurationSeconds, exercise.restBetweenSetsSeconds
                            )
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
                item {
                    val previewSession = WorkoutSession(id = 0, name = "", date = "", exercises = exercises)
                    val totalSeconds = previewSession.estimatedTotalDurationSeconds()
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Estimated Duration", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    formatDuration(totalSeconds),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
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
                    ExerciseCard(exercise = exercise)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    if (showAddExerciseDialog) {
        AddExerciseDialog(
            onConfirm = { name, muscleGroup, plannedSets, plannedReps, repDuration, restBetween ->
                exercises = exercises + Exercise(
                    id = nextLocalExerciseId++,
                    name = name,
                    muscleGroup = muscleGroup,
                    plannedSets = plannedSets,
                    plannedReps = plannedReps,
                    repDurationSeconds = repDuration,
                    restBetweenSetsSeconds = restBetween
                )
                showAddExerciseDialog = false
            },
            onDismiss = { showAddExerciseDialog = false }
        )
    }

}
