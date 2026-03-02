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

enum class ChartMode { MaxWeight, Volume }

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
    var chartMode by remember { mutableStateOf(ChartMode.MaxWeight) }

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
                // Mode toggle
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = chartMode == ChartMode.MaxWeight,
                        onClick = { chartMode = ChartMode.MaxWeight },
                        label = { Text("Max weight") }
                    )
                    FilterChip(
                        selected = chartMode == ChartMode.Volume,
                        onClick = { chartMode = ChartMode.Volume },
                        label = { Text("Volume") }
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Chart card
                val chartValues = if (chartMode == ChartMode.MaxWeight)
                    progression.map { it.maxWeightKg }
                else
                    progression.map { it.totalVolume }
                val yLabel = if (chartMode == ChartMode.MaxWeight) "kg" else "vol"

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            if (chartMode == ChartMode.MaxWeight) "Max weight per session"
                            else "Volume per session",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        ProgressionLineChart(
                            data = progression,
                            values = chartValues,
                            yLabel = yLabel,
                            modifier = Modifier.fillMaxWidth().height(200.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Summary stats
                if (chartMode == ChartMode.MaxWeight) {
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
                } else {
                    val maxVol = progression.maxOf { it.totalVolume }
                    val latestVol = progression.last().totalVolume
                    val firstVol = progression.first().totalVolume
                    val deltaVol = latestVol - firstVol
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard("Best Session Vol", "${maxVol.roundToInt()} vol", Modifier.weight(1f))
                        StatCard(
                            "Vol Progress",
                            "${if (deltaVol >= 0) "+" else ""}${deltaVol.roundToInt()} vol",
                            Modifier.weight(1f),
                            highlight = deltaVol > 0
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Session log
                Text("Session log", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(progression.reversed()) { point ->
                        ProgressionRow(point, showVolume = chartMode == ChartMode.Volume)
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
private fun ProgressionRow(point: ExerciseProgress, showVolume: Boolean = false) {
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
            if (showVolume) "${point.totalVolume.roundToInt()} vol"
            else "${point.maxWeightKg} kg max",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}
