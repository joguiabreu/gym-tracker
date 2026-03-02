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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.gymtracker.data.EXERCISE_CATALOG
import com.gymtracker.data.Exercise
import com.gymtracker.data.GymRepository
import com.gymtracker.data.MuscleGroup
import com.gymtracker.data.WorkoutSession

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
    var addSetForExercise by remember { mutableStateOf<Exercise?>(null) }

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
                        onAddSet = { addSetForExercise = exercise },
                        onShowProgression = { onExerciseProgressionClick(exercise.name) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    if (showAddExerciseDialog) {
        AddExerciseDialog(
            onConfirm = { name, muscleGroup, plannedSets, plannedReps ->
                session = repository.addExercise(sessionId, name, muscleGroup, plannedSets, plannedReps)
                showAddExerciseDialog = false
            },
            onDismiss = { showAddExerciseDialog = false }
        )
    }

    addSetForExercise?.let { exercise ->
        AddSetDialog(
            exerciseName = exercise.name,
            onConfirm = { reps, weightKg ->
                session = repository.addSet(sessionId, exercise.id, reps, weightKg)
                addSetForExercise = null
            },
            onDismiss = { addSetForExercise = null }
        )
    }
}

@Composable
internal fun ExerciseCard(
    exercise: Exercise,
    onAddSet: () -> Unit,
    onShowProgression: () -> Unit = {}
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
                }
                TextButton(onClick = onShowProgression) { Text("Stats") }
                TextButton(onClick = onAddSet) { Text("+ Set") }
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
            if (exercise.sets.isEmpty()) {
                Text("No sets logged yet", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
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
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AddExerciseDialog(
    onConfirm: (name: String, muscleGroup: String, plannedSets: Int, plannedReps: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedGroup by remember { mutableStateOf<MuscleGroup?>(null) }
    var selectedExercise by remember { mutableStateOf<String?>(null) }
    var plannedSets by remember { mutableStateOf(3) }
    var plannedReps by remember { mutableStateOf(10) }

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
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = { onConfirm(selectedExercise!!, selectedGroup!!.name, plannedSets, plannedReps) },
                        enabled = selectedExercise != null
                    ) { Text("Add") }
                }
            }
        }
    }
}

@Composable
internal fun AddSetDialog(
    exerciseName: String,
    onConfirm: (reps: Int, weightKg: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var reps by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val isValid = reps.toIntOrNull() != null && weight.toDoubleOrNull() != null

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Add Set — $exerciseName", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = { Text("Reps") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = { onConfirm(reps.toInt(), weight.toDouble()) },
                        enabled = isValid
                    ) { Text("Add") }
                }
            }
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
private fun Stepper(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    min: Int = 1,
    max: Int
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
                onClick = { if (value > min) onValueChange(value - 1) },
                enabled = value > min
            ) { Text("−", style = MaterialTheme.typography.titleMedium) }

            Text(
                value.toString(),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 40.dp)
            )

            FilledTonalIconButton(
                onClick = { if (value < max) onValueChange(value + 1) },
                enabled = value < max
            ) { Text("+", style = MaterialTheme.typography.titleMedium) }
        }
    }
}
