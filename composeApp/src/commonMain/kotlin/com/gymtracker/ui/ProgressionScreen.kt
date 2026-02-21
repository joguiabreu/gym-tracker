package com.gymtracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymtracker.data.ExerciseProgress
import com.gymtracker.data.GymRepository
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressionScreen(
    repository: GymRepository,
    initialExercise: String? = null,
    onBack: () -> Unit
) {
    val exerciseNames = remember { repository.getAllExerciseNames() }
    var selectedExercise by remember {
        mutableStateOf(initialExercise ?: exerciseNames.firstOrNull() ?: "")
    }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val progression: List<ExerciseProgress> = remember(selectedExercise) {
        if (selectedExercise.isNotBlank()) repository.getExerciseProgression(selectedExercise)
        else emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progression") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Exercise picker
            if (exerciseNames.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No exercises recorded yet.", style = MaterialTheme.typography.bodyLarge)
                }
                return@Scaffold
            }

            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedExercise,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Exercise") },
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    exerciseNames.forEach { name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                selectedExercise = name
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (progression.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("No data yet for $selectedExercise",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                // Chart card
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Max weight per session",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        ProgressionLineChart(
                            data = progression,
                            modifier = Modifier.fillMaxWidth().height(200.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Summary stat
                val maxEver = progression.maxOf { it.maxWeightKg }
                val latest = progression.last().maxWeightKg
                val first = progression.first().maxWeightKg
                val delta = latest - first

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard("Personal Best", "${maxEver} kg", Modifier.weight(1f))
                    StatCard(
                        "Progress",
                        "${if (delta >= 0) "+" else ""}${delta.roundToInt()} kg",
                        Modifier.weight(1f),
                        highlight = delta > 0
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Session log
                Text("Session log", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(progression.reversed()) { point ->
                        ProgressionRow(point)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                color = if (highlight) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ProgressionRow(point: ExerciseProgress) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(point.sessionName, style = MaterialTheme.typography.bodyMedium)
            Text(point.date, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            "${point.maxWeightKg} kg max",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}
