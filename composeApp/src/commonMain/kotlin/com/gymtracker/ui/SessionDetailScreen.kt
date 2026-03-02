package com.gymtracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.gymtracker.data.EXERCISE_CATALOG
import com.gymtracker.data.Exercise
import com.gymtracker.data.GymRepository
import com.gymtracker.data.MuscleGroup
import com.gymtracker.data.WorkoutSession
import com.gymtracker.data.estimatedDurationSeconds
import com.gymtracker.data.estimatedTotalDurationSeconds
import com.gymtracker.data.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: Long,
    repository: GymRepository,
    onBack: () -> Unit,
    onExerciseProgressionClick: (exerciseName: String) -> Unit = {}
) {
    var session by remember { mutableStateOf(repository.getSession(sessionId)) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var completeExercise by remember { mutableStateOf<Exercise?>(null) }
    var restPromptExercise by remember { mutableStateOf<Exercise?>(null) }

    if (session == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Session not found")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(session!!.name)
                        Text(session!!.date, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
        if (session!!.exercises.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No exercises yet. Tap + to add one!", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                item {
                    DurationEstimateCard(session!!)
                    Spacer(Modifier.height(8.dp))
                }
                val targeted = session!!.exercises
                    .map { it.muscleGroup }.filter { it.isNotBlank() }.distinct()
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
                items(session!!.exercises) { exercise ->
                    ExerciseCard(
                        exercise = exercise,
                        onComplete = { completeExercise = exercise }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    if (showAddExerciseDialog) {
        AddExerciseDialog(
            onConfirm = { name, muscleGroup, plannedSets, plannedReps, repDuration, restBetween ->
                session = repository.addExercise(
                    sessionId, name, muscleGroup, plannedSets, plannedReps,
                    repDuration, restBetween
                )
                showAddExerciseDialog = false
            },
            onDismiss = { showAddExerciseDialog = false }
        )
    }

    completeExercise?.let { exercise ->
        ExerciseCompletionDialog(
            exercise = exercise,
            onConfirm = { repsPerSet ->
                // Record each non-skipped set's actual reps
                repsPerSet.forEachIndexed { index, reps ->
                    if (reps < 0) return@forEachIndexed // skipped
                    val existingSet = exercise.sets.getOrNull(index)
                    if (existingSet != null) {
                        if (existingSet.reps != reps) {
                            session = repository.updateSet(sessionId, exercise.id, existingSet.id, reps, existingSet.weightKg)
                        }
                    } else {
                        session = repository.addSet(sessionId, exercise.id, reps, 0.0)
                    }
                }
                // Mark exercise as completed
                val latestSession = repository.getSession(sessionId)!!
                val latestExercise = latestSession.exercises.find { it.id == exercise.id }!!
                val completed = latestExercise.copy(isCompleted = true)
                session = repository.updateExercise(sessionId, completed)
                completeExercise = null
                restPromptExercise = completed
            },
            onDismiss = { completeExercise = null }
        )
    }

    restPromptExercise?.let { exercise ->
        RestTimePromptDialog(
            onConfirm = { restSeconds ->
                val latestSession = repository.getSession(sessionId)!!
                val latestExercise = latestSession.exercises.find { it.id == exercise.id }!!
                session = repository.updateExercise(
                    sessionId,
                    latestExercise.copy(actualRestAfterSeconds = restSeconds)
                )
                restPromptExercise = null
            },
            onSkip = { restPromptExercise = null }
        )
    }
}

@Composable
private fun DurationEstimateCard(session: WorkoutSession) {
    val totalSeconds = session.estimatedTotalDurationSeconds()
    val completedCount = session.exercises.count { it.isCompleted }
    val totalCount = session.exercises.size

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
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { if (totalCount > 0) completedCount.toFloat() / totalCount else 0f },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "$completedCount / $totalCount exercises completed",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
internal fun ExerciseCard(
    exercise: Exercise,
    onComplete: (() -> Unit)? = null
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                    if (exercise.muscleGroup.isNotBlank()) {
                        Text(
                            exercise.muscleGroup,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (exercise.plannedSets > 0) {
                        val estSeconds = exercise.estimatedDurationSeconds()
                        Text(
                            "~${formatDuration(estSeconds)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (exercise.isCompleted) {
                    Text(
                        "Completed",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (exercise.plannedSets > 0) {
                val done = exercise.sets.size
                val doneColor = if (done >= exercise.plannedSets)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
                Row(modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)) {
                    Text(
                        "Planned ${exercise.plannedSets}×${exercise.plannedReps}  ·  ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "$done/${exercise.plannedSets} sets done",
                        style = MaterialTheme.typography.bodySmall,
                        color = doneColor
                    )
                }
            }
            if (exercise.sets.isNotEmpty()) {
                exercise.sets.forEachIndexed { index, set ->
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        Text(
                            "Set ${index + 1}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(56.dp)
                        )
                        Text(
                            "${set.reps} reps × ${set.weightKg} kg",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            if (onComplete != null && !exercise.isCompleted && exercise.plannedSets > 0) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Mark Complete") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AddExerciseDialog(
    onConfirm: (name: String, muscleGroup: String, plannedSets: Int, plannedReps: Int,
                repDurationSeconds: Int, restBetweenSetsSeconds: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedGroup by remember { mutableStateOf<MuscleGroup?>(null) }
    var selectedExercise by remember { mutableStateOf<String?>(null) }
    var plannedSets by remember { mutableStateOf(3) }
    var plannedReps by remember { mutableStateOf(10) }
    var repDuration by remember { mutableStateOf(3) }
    var restBetween by remember { mutableStateOf(60) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Add Exercise", style = MaterialTheme.typography.titleLarge)

                Text("Area", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    EXERCISE_CATALOG.forEach { group ->
                        FilterChip(
                            selected = selectedGroup == group,
                            onClick = {
                                selectedGroup = group
                                selectedExercise = null
                                plannedSets = 3
                                plannedReps = 10
                                repDuration = 3
                                restBetween = 60
                            },
                            label = { Text(group.name) }
                        )
                    }
                }

                selectedGroup?.let { group ->
                    Text("Exercise", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        group.exercises.forEach { exercise ->
                            FilterChip(
                                selected = selectedExercise == exercise,
                                onClick = { selectedExercise = exercise },
                                label = { Text(exercise) }
                            )
                        }
                    }
                }

                if (selectedExercise != null) {
                    Text("Plan", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Stepper("Sets", plannedSets, { plannedSets = it }, max = 20)
                        Stepper("Reps", plannedReps, { plannedReps = it }, max = 100)
                    }

                    Text("Timing", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Stepper("Sec/Rep", repDuration, { repDuration = it }, min = 1, max = 30)
                        Stepper("Rest (s)", restBetween, { restBetween = it }, min = 0, max = 300, step = 15)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            onConfirm(
                                selectedExercise!!, selectedGroup!!.name,
                                plannedSets, plannedReps, repDuration, restBetween
                            )
                        },
                        enabled = selectedExercise != null
                    ) { Text("Add") }
                }
            }
        }
    }
}

@Composable
private fun ExerciseCompletionDialog(
    exercise: Exercise,
    onConfirm: (repsPerSet: List<Int>) -> Unit,
    onDismiss: () -> Unit
) {
    val setCount = exercise.plannedSets.coerceAtLeast(1)
    // -1 means skipped; positive means actual reps
    val repsState = remember {
        mutableStateListOf<Int>().apply {
            repeat(setCount) { index ->
                add(exercise.sets.getOrNull(index)?.reps ?: exercise.plannedReps)
            }
        }
    }
    // Index of first skipped set, or setCount if none skipped
    val firstSkippedIndex = repsState.indexOfFirst { it < 0 }.let { if (it == -1) setCount else it }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Complete — ${exercise.name}", style = MaterialTheme.typography.titleLarge)
                Text(
                    "How many reps did you do?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                for (index in 0 until setCount) {
                    val isSkipped = repsState[index] < 0
                    val canToggleSkip = if (isSkipped) {
                        // Can only unskip the first skipped set
                        index == firstSkippedIndex
                    } else {
                        // Can skip any set at or after the current first-skipped boundary
                        // (i.e. this set is not before an already-skipped set, or no sets skipped yet)
                        index >= firstSkippedIndex || firstSkippedIndex == setCount
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Set ${index + 1}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(50.dp)
                        )

                        if (isSkipped) {
                            Text(
                                "Skipped",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalIconButton(
                                    onClick = { if (repsState[index] > 1) repsState[index] = repsState[index] - 1 },
                                    enabled = repsState[index] > 1,
                                    modifier = Modifier.size(36.dp)
                                ) { Text("−", style = MaterialTheme.typography.titleMedium) }

                                Text(
                                    repsState[index].toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.widthIn(min = 32.dp)
                                )

                                FilledTonalIconButton(
                                    onClick = { repsState[index] = repsState[index] + 1 },
                                    modifier = Modifier.size(36.dp)
                                ) { Text("+", style = MaterialTheme.typography.titleMedium) }
                            }
                        }

                        if (canToggleSkip) {
                            if (isSkipped) {
                                OutlinedButton(
                                    onClick = { repsState[index] = exercise.plannedReps },
                                    modifier = Modifier.height(36.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) { Text("Undo", style = MaterialTheme.typography.labelMedium) }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        // Skip this set and all after it
                                        for (i in index until setCount) {
                                            repsState[i] = -1
                                        }
                                    },
                                    modifier = Modifier.height(36.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) { Text("Skip", style = MaterialTheme.typography.labelMedium) }
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = { onConfirm(repsState.toList()) }
                    ) { Text("Confirm") }
                }
            }
        }
    }
}

@Composable
private fun RestTimePromptDialog(
    onConfirm: (restSeconds: Int) -> Unit,
    onSkip: () -> Unit
) {
    var restMinutes by remember { mutableStateOf(2) }

    Dialog(onDismissRequest = onSkip) {
        Card(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Rest Before Next Exercise", style = MaterialTheme.typography.titleLarge)
                Text(
                    "How long will you rest?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Stepper("Minutes", restMinutes, { restMinutes = it }, min = 0, max = 15)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onSkip) { Text("Skip") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = { onConfirm(restMinutes * 60) }
                    ) { Text("OK") }
                }
            }
        }
    }
}

@Composable
internal fun Stepper(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    min: Int = 1,
    max: Int,
    step: Int = 1
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalIconButton(
                onClick = { if (value - step >= min) onValueChange(value - step) },
                enabled = value > min
            ) { Text("−", style = MaterialTheme.typography.titleMedium) }

            Text(
                value.toString(),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 40.dp)
            )

            FilledTonalIconButton(
                onClick = { if (value + step <= max) onValueChange(value + step) },
                enabled = value < max
            ) { Text("+", style = MaterialTheme.typography.titleMedium) }
        }
    }
}
