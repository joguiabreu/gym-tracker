package com.gymtracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gymtracker.data.GymRepository
import com.gymtracker.data.WorkoutPlan
import com.gymtracker.data.WorkoutSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: GymRepository,
    onSessionClick: (WorkoutSession) -> Unit,
    onNewSession: () -> Unit,
    onProgressionClick: () -> Unit,
    onPlansClick: () -> Unit = {},
    onStartPlan: (Long) -> Unit = {},
    onNewPlan: () -> Unit = {},
    onEditProfile: () -> Unit = {}
) {
    var sessions by remember { mutableStateOf(repository.getSessions()) }
    val plans by remember { mutableStateOf(repository.getPlans()) }

    val activeSession = sessions.find { !it.isFinished }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gym Tracker") },
                actions = {
                    TextButton(onClick = onPlansClick) {
                        Text("Plans")
                    }
                    TextButton(onClick = onProgressionClick) {
                        Text("Progress")
                    }
                    TextButton(onClick = onEditProfile) {
                        Text("Profile")
                    }
                }
            )
        },
        floatingActionButton = {
            if (activeSession == null) {
                FloatingActionButton(onClick = onNewSession) {
                    Icon(Icons.Default.Add, contentDescription = "Add session")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // ── Active Workout ──
            if (activeSession != null) {
                item {
                    Text(
                        "Active Workout",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                item {
                    ActiveWorkoutCard(
                        session = activeSession,
                        onContinue = { onSessionClick(activeSession) },
                        onDelete = {
                            repository.deleteSession(activeSession.id)
                            sessions = repository.getSessions()
                        }
                    )
                }
            }

            // ── Plans header ──
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .padding(top = if (activeSession != null) 8.dp else 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Plans", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onPlansClick) {
                        Text("See All")
                    }
                }
            }

            // ── Plan cards row ──
            item {
                if (plans.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable { onNewPlan() }
                    ) {
                        Box(
                            modifier = Modifier.padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Create your first plan",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(plans) { plan ->
                            QuickStartPlanCard(
                                plan = plan,
                                canStart = activeSession == null,
                                onStart = { onStartPlan(plan.id) }
                            )
                        }
                    }
                }
            }

            // ── Past Workouts ──
            val past = sessions.filter { it.isFinished }
            item {
                Text(
                    "Past Workouts",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        .padding(top = 8.dp)
                )
            }

            if (past.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No finished workouts yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(past) { session ->
                    SessionCard(
                        session = session,
                        onClick = { onSessionClick(session) },
                        onDelete = {
                            repository.deleteSession(session.id)
                            sessions = repository.getSessions()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickStartPlanCard(
    plan: WorkoutPlan,
    canStart: Boolean,
    onStart: () -> Unit
) {
    Card(modifier = Modifier.width(160.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                plan.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            val muscles = plan.exercises
                .map { it.muscleGroup }
                .filter { it.isNotBlank() }
                .distinct()
            if (muscles.isNotEmpty()) {
                Text(
                    muscles.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                "${plan.exercises.size} exercise(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onStart,
                enabled = canStart,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) { Text("Start") }
        }
    }
}

@Composable
private fun ActiveWorkoutCard(
    session: WorkoutSession,
    onContinue: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(session.name, style = MaterialTheme.typography.titleMedium)
                Text(session.date, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                val completedCount = session.exercises.count { it.isCompleted }
                Text(
                    "$completedCount / ${session.exercises.size} exercises done",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error)
            }
            Button(onClick = onContinue) {
                Text("Continue")
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: WorkoutSession,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(session.name, style = MaterialTheme.typography.titleMedium)
                Text(session.date, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                val targeted = session.exercises
                    .map { it.muscleGroup }.filter { it.isNotBlank() }.distinct()
                if (targeted.isNotEmpty()) {
                    Text(
                        targeted.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        "${session.exercises.size} exercise(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
