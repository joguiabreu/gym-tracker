package com.gymtracker.shared

import kotlinx.serialization.Serializable

@Serializable
enum class MuscleGroup {
    CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, QUADS, HAMSTRINGS, GLUTES, CALVES, CORE
}

@Serializable
enum class ExerciseCategory {
    COMPOUND, ISOLATION
}

@Serializable
data class CatalogExercise(
    val name: String,
    val primaryMuscle: MuscleGroup,
    val secondaryMuscles: List<MuscleGroup> = emptyList(),
    val equipment: List<Equipment>,
    val category: ExerciseCategory
)

object ExerciseCatalog {
    val exercises = listOf(
        // ── Chest ──
        CatalogExercise("Barbell Bench Press", MuscleGroup.CHEST, listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS), listOf(Equipment.BARBELL), ExerciseCategory.COMPOUND),
        CatalogExercise("Dumbbell Bench Press", MuscleGroup.CHEST, listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS), listOf(Equipment.DUMBBELL), ExerciseCategory.COMPOUND),
        CatalogExercise("Incline Barbell Bench Press", MuscleGroup.CHEST, listOf(MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS), listOf(Equipment.BARBELL), ExerciseCategory.COMPOUND),
        CatalogExercise("Incline Dumbbell Bench Press", MuscleGroup.CHEST, listOf(MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS), listOf(Equipment.DUMBBELL), ExerciseCategory.COMPOUND),
        CatalogExercise("Dumbbell Chest Fly", MuscleGroup.CHEST, emptyList(), listOf(Equipment.DUMBBELL), ExerciseCategory.ISOLATION),
        CatalogExercise("Cable Chest Fly", MuscleGroup.CHEST, emptyList(), listOf(Equipment.CABLE), ExerciseCategory.ISOLATION),
        CatalogExercise("Push Up", MuscleGroup.CHEST, listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS), listOf(Equipment.NONE), ExerciseCategory.COMPOUND),
        CatalogExercise("Chest Dip", MuscleGroup.CHEST, listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS), listOf(Equipment.DIP_STATION), ExerciseCategory.COMPOUND),
        CatalogExercise("Machine Chest Press", MuscleGroup.CHEST, listOf(MuscleGroup.TRICEPS), listOf(Equipment.MACHINE), ExerciseCategory.COMPOUND),
        CatalogExercise("Pec Deck", MuscleGroup.CHEST, emptyList(), listOf(Equipment.MACHINE), ExerciseCategory.ISOLATION),

        // ── Back ──
        CatalogExercise("Barbell Row", MuscleGroup.BACK, listOf(MuscleGroup.BICEPS), listOf(Equipment.BARBELL), ExerciseCategory.COMPOUND),
        CatalogExercise("Dumbbell Row", MuscleGroup.BACK, listOf(MuscleGroup.BICEPS), listOf(Equipment.DUMBBELL), ExerciseCategory.COMPOUND),
        CatalogExercise("Pull Up", MuscleGroup.BACK, listOf(MuscleGroup.BICEPS), listOf(Equipment.PULL_UP_BAR), ExerciseCategory.COMPOUND),
        CatalogExercise("Chin Up", MuscleGroup.BACK, listOf(MuscleGroup.BICEPS), listOf(Equipment.PULL_UP_BAR), ExerciseCategory.COMPOUND),
        CatalogExercise("Lat Pulldown", MuscleGroup.BACK, listOf(MuscleGroup.BICEPS), listOf(Equipment.CABLE), ExerciseCategory.COMPOUND),
        CatalogExercise("Seated Cable Row", MuscleGroup.BACK, listOf(MuscleGroup.BICEPS), listOf(Equipment.CABLE), ExerciseCategory.COMPOUND),
        CatalogExercise("T-Bar Row", MuscleGroup.BACK, listOf(MuscleGroup.BICEPS), listOf(Equipment.BARBELL), ExerciseCategory.COMPOUND),
        CatalogExercise("Face Pull", MuscleGroup.BACK, listOf(MuscleGroup.SHOULDERS), listOf(Equipment.CABLE), ExerciseCategory.ISOLATION),
        CatalogExercise("Straight Arm Pulldown", MuscleGroup.BACK, emptyList(), listOf(Equipment.CABLE), ExerciseCategory.ISOLATION),
        CatalogExercise("Machine Row", MuscleGroup.BACK, listOf(MuscleGroup.BICEPS), listOf(Equipment.MACHINE), ExerciseCategory.COMPOUND),

        // ── Shoulders ──
        CatalogExercise("Overhead Press", MuscleGroup.SHOULDERS, listOf(MuscleGroup.TRICEPS), listOf(Equipment.BARBELL), ExerciseCategory.COMPOUND),
        CatalogExercise("Dumbbell Shoulder Press", MuscleGroup.SHOULDERS, listOf(MuscleGroup.TRICEPS), listOf(Equipment.DUMBBELL), ExerciseCategory.COMPOUND),
        CatalogExercise("Arnold Press", MuscleGroup.SHOULDERS, listOf(MuscleGroup.TRICEPS), listOf(Equipment.DUMBBELL), ExerciseCategory.COMPOUND),
        CatalogExercise("Lateral Raise", MuscleGroup.SHOULDERS, emptyList(), listOf(Equipment.DUMBBELL), ExerciseCategory.ISOLATION),
        CatalogExercise("Cable Lateral Raise", MuscleGroup.SHOULDERS, emptyList(), listOf(Equipment.CABLE), ExerciseCategory.ISOLATION),
        CatalogExercise("Front Raise", MuscleGroup.SHOULDERS, emptyList(), listOf(Equipment.DUMBBELL), ExerciseCategory.ISOLATION),
        CatalogExercise("Reverse Fly", MuscleGroup.SHOULDERS, listOf(MuscleGroup.BACK), listOf(Equipment.DUMBBELL), ExerciseCategory.ISOLATION),
        CatalogExercise("Machine Shoulder Press", MuscleGroup.SHOULDERS, listOf(MuscleGroup.TRICEPS), listOf(Equipment.MACHINE), ExerciseCategory.COMPOUND),

        // ── Biceps ──
        CatalogExercise("Barbell Curl", MuscleGroup.BICEPS, emptyList(), listOf(Equipment.BARBELL), ExerciseCategory.ISOLATION),
        CatalogExercise("Dumbbell Curl", MuscleGroup.BICEPS, emptyList(), listOf(Equipment.DUMBBELL), ExerciseCategory.ISOLATION),
        CatalogExercise("Hammer Curl", MuscleGroup.BICEPS, emptyList(), listOf(Equipment.DUMBBELL), ExerciseCategory.ISOLATION),
        CatalogExercise("Incline Dumbbell Curl", MuscleGroup.BICEPS, emptyList(), listOf(Equipment.DUMBBELL), ExerciseCategory.ISOLATION),
        CatalogExercise("Cable Curl", MuscleGroup.BICEPS, emptyList(), listOf(Equipment.CABLE), ExerciseCategory.ISOLATION),
        CatalogExercise("Preacher Curl", MuscleGroup.BICEPS, emptyList(), listOf(Equipment.BARBELL), ExerciseCategory.ISOLATION),
        CatalogExercise("Concentration Curl", MuscleGroup.BICEPS, emptyList(), listOf(Equipment.DUMBBELL), ExerciseCategory.ISOLATION),

        // ── Triceps ──
        CatalogExercise("Tricep Pushdown", MuscleGroup.TRICEPS, emptyList(), listOf(Equipment.CABLE), ExerciseCategory.ISOLATION),
        CatalogExercise("Overhead Tricep Extension", MuscleGroup.TRICEPS, emptyList(), listOf(Equipment.DUMBBELL), ExerciseCategory.ISOLATION),
        CatalogExercise("Cable Overhead Tricep Extension", MuscleGroup.TRICEPS, emptyList(), listOf(Equipment.CABLE), ExerciseCategory.ISOLATION),
        CatalogExercise("Skull Crusher", MuscleGroup.TRICEPS, emptyList(), listOf(Equipment.BARBELL), ExerciseCategory.ISOLATION),
        CatalogExercise("Tricep Dip", MuscleGroup.TRICEPS, listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS), listOf(Equipment.DIP_STATION), ExerciseCategory.COMPOUND),
        CatalogExercise("Close Grip Bench Press", MuscleGroup.TRICEPS, listOf(MuscleGroup.CHEST), listOf(Equipment.BARBELL), ExerciseCategory.COMPOUND),
        CatalogExercise("Tricep Kickback", MuscleGroup.TRICEPS, emptyList(), listOf(Equipment.DUMBBELL), ExerciseCategory.ISOLATION),

        // ── Quads ──
        CatalogExercise("Barbell Back Squat", MuscleGroup.QUADS, listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS), listOf(Equipment.BARBELL), ExerciseCategory.COMPOUND),
        CatalogExercise("Front Squat", MuscleGroup.QUADS, listOf(MuscleGroup.GLUTES, MuscleGroup.CORE), listOf(Equipment.BARBELL), ExerciseCategory.COMPOUND),
        CatalogExercise("Goblet Squat", MuscleGroup.QUADS, listOf(MuscleGroup.GLUTES), listOf(Equipment.DUMBBELL, Equipment.KETTLEBELL), ExerciseCategory.COMPOUND),
        CatalogExercise("Leg Press", MuscleGroup.QUADS, listOf(MuscleGroup.GLUTES), listOf(Equipment.MACHINE), ExerciseCategory.COMPOUND),
        CatalogExercise("Leg Extension", MuscleGroup.QUADS, emptyList(), listOf(Equipment.MACHINE), ExerciseCategory.ISOLATION),
        CatalogExercise("Bulgarian Split Squat", MuscleGroup.QUADS, listOf(MuscleGroup.GLUTES), listOf(Equipment.DUMBBELL, Equipment.NONE), ExerciseCategory.COMPOUND),
        CatalogExercise("Walking Lunge", MuscleGroup.QUADS, listOf(MuscleGroup.GLUTES), listOf(Equipment.DUMBBELL, Equipment.NONE), ExerciseCategory.COMPOUND),
        CatalogExercise("Hack Squat", MuscleGroup.QUADS, listOf(MuscleGroup.GLUTES), listOf(Equipment.MACHINE), ExerciseCategory.COMPOUND),

        // ── Hamstrings ──
        CatalogExercise("Romanian Deadlift", MuscleGroup.HAMSTRINGS, listOf(MuscleGroup.GLUTES, MuscleGroup.BACK), listOf(Equipment.BARBELL), ExerciseCategory.COMPOUND),
        CatalogExercise("Dumbbell Romanian Deadlift", MuscleGroup.HAMSTRINGS, listOf(MuscleGroup.GLUTES), listOf(Equipment.DUMBBELL), ExerciseCategory.COMPOUND),
        CatalogExercise("Lying Leg Curl", MuscleGroup.HAMSTRINGS, emptyList(), listOf(Equipment.MACHINE), ExerciseCategory.ISOLATION),
        CatalogExercise("Seated Leg Curl", MuscleGroup.HAMSTRINGS, emptyList(), listOf(Equipment.MACHINE), ExerciseCategory.ISOLATION),
        CatalogExercise("Good Morning", MuscleGroup.HAMSTRINGS, listOf(MuscleGroup.BACK, MuscleGroup.GLUTES), listOf(Equipment.BARBELL), ExerciseCategory.COMPOUND),
        CatalogExercise("Nordic Hamstring Curl", MuscleGroup.HAMSTRINGS, emptyList(), listOf(Equipment.NONE), ExerciseCategory.ISOLATION),

        // ── Glutes ──
        CatalogExercise("Hip Thrust", MuscleGroup.GLUTES, listOf(MuscleGroup.HAMSTRINGS), listOf(Equipment.BARBELL), ExerciseCategory.COMPOUND),
        CatalogExercise("Glute Bridge", MuscleGroup.GLUTES, listOf(MuscleGroup.HAMSTRINGS), listOf(Equipment.NONE, Equipment.BARBELL), ExerciseCategory.COMPOUND),
        CatalogExercise("Cable Pull Through", MuscleGroup.GLUTES, listOf(MuscleGroup.HAMSTRINGS), listOf(Equipment.CABLE), ExerciseCategory.COMPOUND),
        CatalogExercise("Cable Kickback", MuscleGroup.GLUTES, emptyList(), listOf(Equipment.CABLE), ExerciseCategory.ISOLATION),
        CatalogExercise("Sumo Deadlift", MuscleGroup.GLUTES, listOf(MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.BACK), listOf(Equipment.BARBELL), ExerciseCategory.COMPOUND),
        CatalogExercise("Kettlebell Swing", MuscleGroup.GLUTES, listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.CORE), listOf(Equipment.KETTLEBELL), ExerciseCategory.COMPOUND),

        // ── Calves ──
        CatalogExercise("Standing Calf Raise", MuscleGroup.CALVES, emptyList(), listOf(Equipment.MACHINE, Equipment.NONE), ExerciseCategory.ISOLATION),
        CatalogExercise("Seated Calf Raise", MuscleGroup.CALVES, emptyList(), listOf(Equipment.MACHINE), ExerciseCategory.ISOLATION),

        // ── Core ──
        CatalogExercise("Plank", MuscleGroup.CORE, emptyList(), listOf(Equipment.NONE), ExerciseCategory.ISOLATION),
        CatalogExercise("Hanging Leg Raise", MuscleGroup.CORE, emptyList(), listOf(Equipment.PULL_UP_BAR), ExerciseCategory.ISOLATION),
        CatalogExercise("Cable Crunch", MuscleGroup.CORE, emptyList(), listOf(Equipment.CABLE), ExerciseCategory.ISOLATION),
        CatalogExercise("Ab Wheel Rollout", MuscleGroup.CORE, emptyList(), listOf(Equipment.NONE), ExerciseCategory.ISOLATION),
        CatalogExercise("Russian Twist", MuscleGroup.CORE, emptyList(), listOf(Equipment.NONE, Equipment.DUMBBELL), ExerciseCategory.ISOLATION),
        CatalogExercise("Dead Bug", MuscleGroup.CORE, emptyList(), listOf(Equipment.NONE), ExerciseCategory.ISOLATION),

        // ── Full body / compounds ──
        CatalogExercise("Conventional Deadlift", MuscleGroup.BACK, listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.QUADS), listOf(Equipment.BARBELL), ExerciseCategory.COMPOUND),
        CatalogExercise("Clean and Press", MuscleGroup.SHOULDERS, listOf(MuscleGroup.QUADS, MuscleGroup.GLUTES, MuscleGroup.BACK), listOf(Equipment.BARBELL), ExerciseCategory.COMPOUND),
        CatalogExercise("Thruster", MuscleGroup.QUADS, listOf(MuscleGroup.SHOULDERS, MuscleGroup.GLUTES), listOf(Equipment.BARBELL, Equipment.DUMBBELL), ExerciseCategory.COMPOUND),
        CatalogExercise("Burpee", MuscleGroup.CORE, listOf(MuscleGroup.CHEST, MuscleGroup.QUADS), listOf(Equipment.NONE), ExerciseCategory.COMPOUND),
    )

    fun byMuscle(muscle: MuscleGroup): List<CatalogExercise> =
        exercises.filter { it.primaryMuscle == muscle }

    fun byEquipment(equipment: Equipment): List<CatalogExercise> =
        exercises.filter { equipment in it.equipment }

    fun forEquipmentSet(available: Set<Equipment>): List<CatalogExercise> =
        exercises.filter { exercise ->
            exercise.equipment.any { it == Equipment.NONE || it in available }
        }

    fun names(): List<String> = exercises.map { it.name }

    fun find(name: String): CatalogExercise? =
        exercises.find { it.name.equals(name, ignoreCase = true) }
}
