package com.gymtracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymtracker.data.Equipment
import com.gymtracker.data.ExperienceLevel
import com.gymtracker.data.GymRepository
import com.gymtracker.data.UserProfile

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    repository: GymRepository,
    onComplete: () -> Unit
) {
    val existingProfile = repository.getProfile()

    var goal by remember { mutableStateOf(existingProfile?.goal ?: "") }
    var daysPerWeek by remember { mutableStateOf(existingProfile?.daysPerWeek?.toFloat() ?: 4f) }
    var selectedEquipment by remember { mutableStateOf(existingProfile?.equipment ?: emptySet()) }
    var experience by remember { mutableStateOf(existingProfile?.experience ?: ExperienceLevel.BEGINNER) }
    var injuries by remember { mutableStateOf(existingProfile?.injuries ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingProfile != null) "Edit Profile" else "Set Up Your Profile") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // ── Goal ──
            item {
                Text("What's your goal?", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = goal,
                    onValueChange = { goal = it },
                    placeholder = { Text("e.g. Get bulkier upper body, lose weight, general fitness") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )
            }

            // ── Days per week ──
            item {
                Text("How many days per week?", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Slider(
                        value = daysPerWeek,
                        onValueChange = { daysPerWeek = it },
                        valueRange = 1f..7f,
                        steps = 5,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        "${daysPerWeek.toInt()} days",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // ── Equipment ──
            item {
                Text("What equipment do you have?", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Exercises with no equipment are always available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                val selectableEquipment = Equipment.entries.filter { it != Equipment.NONE }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectableEquipment.forEach { equipment ->
                        val selected = equipment in selectedEquipment
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedEquipment = if (selected) {
                                    selectedEquipment - equipment
                                } else {
                                    selectedEquipment + equipment
                                }
                            },
                            label = { Text(equipmentLabel(equipment)) }
                        )
                    }
                }
            }

            // ── Experience ──
            item {
                Text("Experience level", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExperienceLevel.entries.forEach { level ->
                        FilterChip(
                            selected = experience == level,
                            onClick = { experience = level },
                            label = { Text(experienceLabel(level)) }
                        )
                    }
                }
            }

            // ── Injuries ──
            item {
                Text("Any injuries or limitations?", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = injuries,
                    onValueChange = { injuries = it },
                    placeholder = { Text("e.g. bad left knee, shoulder impingement (leave empty if none)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )
            }

            // ── Save ──
            item {
                Button(
                    onClick = {
                        repository.saveProfile(
                            UserProfile(
                                goal = goal.trim(),
                                daysPerWeek = daysPerWeek.toInt(),
                                equipment = selectedEquipment,
                                experience = experience,
                                injuries = injuries.trim()
                            )
                        )
                        onComplete()
                    },
                    enabled = goal.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(if (existingProfile != null) "Save Changes" else "Get Started")
                }
            }
        }
    }
}

private fun equipmentLabel(equipment: Equipment): String = when (equipment) {
    Equipment.BARBELL -> "Barbell"
    Equipment.DUMBBELL -> "Dumbbell"
    Equipment.CABLE -> "Cable Machine"
    Equipment.MACHINE -> "Machines"
    Equipment.PULL_UP_BAR -> "Pull-up Bar"
    Equipment.DIP_STATION -> "Dip Station"
    Equipment.KETTLEBELL -> "Kettlebell"
    Equipment.BAND -> "Resistance Band"
    Equipment.NONE -> "None"
}

private fun experienceLabel(level: ExperienceLevel): String = when (level) {
    ExperienceLevel.BEGINNER -> "Beginner"
    ExperienceLevel.INTERMEDIATE -> "Intermediate"
    ExperienceLevel.ADVANCED -> "Advanced"
}
