package com.gymtracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymtracker.data.GymRepository
import com.gymtracker.data.WorkoutPlan
import com.gymtracker.data.estimatedTotalDurationSeconds
import com.gymtracker.data.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutPlansScreen(
    repository: GymRepository,
    onNewPlan: () -> Unit,
    onStartWorkout: (sessionId: Long) -> Unit,
    onBack: () -> Unit
) {
    var plans by remember { mutableStateOf(repository.getPlans()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout Plans") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewPlan) {
                Icon(Icons.Default.Add, contentDescription = "New plan")
            }
        }
    ) { padding ->
        if (plans.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No plans yet. Tap + to create one!", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(plans) { plan ->
                    PlanCard(
                        plan = plan,
                        onStart = {
                            val session = repository.createSessionFromPlan(plan.id)
                            if (session != null) {
                                onStartWorkout(session.id)
                            }
                        },
                        onDelete = {
                            repository.deletePlan(plan.id)
                            plans = repository.getPlans()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: WorkoutPlan,
    onStart: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(plan.name, style = MaterialTheme.typography.titleMedium)
                    val targeted = plan.exercises
                        .map { it.muscleGroup }.filter { it.isNotBlank() }.distinct()
                    if (targeted.isNotEmpty()) {
                        Text(
                            targeted.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        "${plan.exercises.size} exercise(s) · ~${formatDuration(plan.estimatedTotalDurationSeconds())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                enabled = plan.exercises.isNotEmpty()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Start Workout")
            }
        }
    }
}
