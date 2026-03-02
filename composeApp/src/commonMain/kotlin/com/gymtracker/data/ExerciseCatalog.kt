package com.gymtracker.data

data class MuscleGroup(val name: String, val exercises: List<String>)

val EXERCISE_CATALOG = listOf(
    MuscleGroup("Chest",     listOf("Bench Press", "Push Up", "Chest Fly")),
    MuscleGroup("Back",      listOf("Pull Up", "Bent Over Row", "Lat Pulldown")),
    MuscleGroup("Arms",      listOf("Bicep Curl", "Tricep Dip", "Hammer Curl")),
    MuscleGroup("Legs",      listOf("Squat", "Deadlift", "Leg Press")),
    MuscleGroup("Shoulders", listOf("Overhead Press", "Lateral Raise", "Front Raise")),
    MuscleGroup("Core",      listOf("Plank", "Crunch", "Russian Twist"))
)
