package com.gymtracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.gymtracker.ai.GeneratedExercise
import com.gymtracker.ai.GeneratedWorkout
import com.gymtracker.ai.WorkoutAiService
import com.gymtracker.ai.ContextManager
import com.gymtracker.ai.UserTier
import com.gymtracker.data.ExerciseCatalog
import com.gymtracker.data.GymRepository
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private enum class ExerciseStatus { PENDING, ACCEPTED, REJECTED }

private data class ReviewableExercise(
    val exercise: GeneratedExercise,
    val status: ExerciseStatus = ExerciseStatus.PENDING,
    val rejectReason: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateWorkoutScreen(
    repository: GymRepository,
    aiService: WorkoutAiService,
    onStartWorkout: (sessionId: Long) -> Unit,
    onBack: () -> Unit
) {
    val profile = repository.getProfile()
    if (profile == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No profile set up")
        }
        return
    }

    val contextManager = remember { ContextManager(repository) }
    val context = remember { contextManager.buildContext() }

    var target by remember { mutableStateOf("") }
    var workout by remember { mutableStateOf<GeneratedWorkout?>(null) }
    var reviewList by remember { mutableStateOf<List<ReviewableExercise>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var rejectingIndex by remember { mutableStateOf<Int?>(null) }
    var resuggestCount by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    val hasRejected = reviewList.any { it.status == ExerciseStatus.REJECTED }
    val allReviewed = reviewList.isNotEmpty() && reviewList.none { it.status == ExerciseStatus.PENDING }
    val allAccepted = reviewList.isNotEmpty() && reviewList.all { it.status == ExerciseStatus.ACCEPTED }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Generate Workout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // ── Target input (only before generating) ──
            if (workout == null) {
                item {
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it },
                        label = { Text("Focus (optional)") },
                        placeholder = { Text("e.g. chest and triceps, upper body, glutes") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Button(
                        onClick = {
                            isLoading = true
                            error = null
                            scope.launch {
                                aiService.generate(profile, target, context.recentSessions)
                                    .onSuccess { result ->
                                        workout = result
                                        reviewList = result.exercises.map { ReviewableExercise(it) }
                                    }
                                    .onFailure { error = it.message }
                                isLoading = false
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Generate Workout")
                    }
                }
            }

            // ── Error ──
            if (error != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            error!!,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // ── Generated workout ──
            if (workout != null) {
                if (workout!!.reasoning.isNotBlank()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Text(
                                workout!!.reasoning,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                itemsIndexed(reviewList) { index, item ->
                    GeneratedExerciseCard(
                        item = item,
                        onAccept = {
                            reviewList = reviewList.toMutableList().also {
                                it[index] = item.copy(status = ExerciseStatus.ACCEPTED)
                            }
                        },
                        onReject = { rejectingIndex = index },
                        onUndoAccept = {
                            reviewList = reviewList.toMutableList().also {
                                it[index] = item.copy(status = ExerciseStatus.PENDING)
                            }
                        }
                    )
                }

                if (reviewList.any { it.status == ExerciseStatus.PENDING }) {
                    item {
                        OutlinedButton(
                            onClick = {
                                reviewList = reviewList.map {
                                    if (it.status == ExerciseStatus.PENDING)
                                        it.copy(status = ExerciseStatus.ACCEPTED)
                                    else it
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Accept All") }
                    }
                }

                if (allReviewed) {
                    if (hasRejected && resuggestCount < 3) {
                        item {
                            Button(
                                onClick = {
                                    isLoading = true
                                    error = null
                                    scope.launch {
                                        val kept = reviewList
                                            .filter { it.status == ExerciseStatus.ACCEPTED }
                                            .map { it.exercise }
                                        val rejected = reviewList
                                            .filter { it.status == ExerciseStatus.REJECTED }
                                            .map { it.exercise to it.rejectReason }

                                        aiService.resuggest(profile, kept, rejected)
                                            .onSuccess { result ->
                                                workout = result
                                                reviewList = result.exercises.map { ex ->
                                                    val wasKept = kept.any { it.name == ex.name }
                                                    ReviewableExercise(
                                                        exercise = ex,
                                                        status = if (wasKept) ExerciseStatus.ACCEPTED
                                                        else ExerciseStatus.PENDING
                                                    )
                                                }
                                                resuggestCount++
                                            }
                                            .onFailure { error = it.message }
                                        isLoading = false
                                    }
                                },
                                enabled = !isLoading,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text("Re-suggest Rejected (${3 - resuggestCount} left)")
                            }
                        }
                    }

                    if (allAccepted) {
                        item {
                            Button(
                                onClick = {
                                    val sessionId = createSessionFromGenerated(
                                        repository, reviewList.map { it.exercise }
                                    )
                                    onStartWorkout(sessionId)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Start Workout") }
                        }
                    }
                }
            }
        }
    }

    rejectingIndex?.let { index ->
        RejectReasonDialog(
            exerciseName = reviewList[index].exercise.name,
            onConfirm = { reason ->
                reviewList = reviewList.toMutableList().also {
                    it[index] = it[index].copy(
                        status = ExerciseStatus.REJECTED,
                        rejectReason = reason
                    )
                }
                rejectingIndex = null
            },
            onDismiss = { rejectingIndex = null }
        )
    }
}

@Composable
private fun GeneratedExerciseCard(
    item: ReviewableExercise,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onUndoAccept: () -> Unit
) {
    val containerColor = when (item.status) {
        ExerciseStatus.ACCEPTED -> MaterialTheme.colorScheme.primaryContainer
        ExerciseStatus.REJECTED -> MaterialTheme.colorScheme.errorContainer
        ExerciseStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(item.exercise.name, style = MaterialTheme.typography.titleMedium)

            Row(modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    "${item.exercise.plannedSets}x${item.exercise.plannedReps}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (item.exercise.suggestedWeightKg > 0) {
                    Text(
                        " @ ${item.exercise.suggestedWeightKg}kg",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (item.exercise.reason.isNotBlank()) {
                Text(
                    item.exercise.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (item.status == ExerciseStatus.REJECTED) {
                Text(
                    "Rejected: ${item.rejectReason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            when (item.status) {
                ExerciseStatus.PENDING -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onReject,
                            modifier = Modifier.weight(1f)
                        ) { Text("Reject") }
                        Button(
                            onClick = onAccept,
                            modifier = Modifier.weight(1f)
                        ) { Text("Accept") }
                    }
                }
                ExerciseStatus.ACCEPTED -> {
                    TextButton(onClick = onUndoAccept) { Text("Undo") }
                }
                ExerciseStatus.REJECTED -> {}
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RejectReasonDialog(
    exerciseName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val presetReasons = listOf(
        "Machine is taken",
        "Feeling tired for this",
        "Pain/discomfort",
        "Don't like this exercise",
        "Did this recently"
    )
    var selectedReason by remember { mutableStateOf<String?>(null) }
    var customReason by remember { mutableStateOf("") }

    val finalReason = selectedReason ?: customReason

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
                Text("Reject $exerciseName", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Why are you rejecting this?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetReasons.forEach { reason ->
                        FilterChip(
                            selected = selectedReason == reason,
                            onClick = {
                                selectedReason = if (selectedReason == reason) null else reason
                                if (selectedReason != null) customReason = ""
                            },
                            label = { Text(reason) }
                        )
                    }
                }

                OutlinedTextField(
                    value = customReason,
                    onValueChange = {
                        customReason = it
                        if (it.isNotBlank()) selectedReason = null
                    },
                    label = { Text("Or type a reason") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = { onConfirm(finalReason) },
                        enabled = finalReason.isNotBlank()
                    ) { Text("Reject") }
                }
            }
        }
    }
}

private fun createSessionFromGenerated(
    repository: GymRepository,
    exercises: List<GeneratedExercise>
): Long {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val h = now.hour.toString().padStart(2, '0')
    val m = now.minute.toString().padStart(2, '0')

    val session = repository.addSession("${now.date} $h:$m", now.date.toString())
    exercises.forEach { ex ->
        val catalogExercise = ExerciseCatalog.find(ex.name)
        val muscleGroup = catalogExercise?.primaryMuscle?.name?.lowercase()
            ?.replaceFirstChar { it.uppercase() } ?: ""
        repository.addExercise(
            session.id, ex.name, muscleGroup,
            ex.plannedSets, ex.plannedReps
        )
    }
    return session.id
}
