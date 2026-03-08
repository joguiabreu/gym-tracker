package com.gymtracker.data

// Re-export from shared module so existing app code doesn't need mass import changes
typealias Equipment = com.gymtracker.shared.Equipment
typealias MuscleGroup = com.gymtracker.shared.MuscleGroup
typealias ExerciseCategory = com.gymtracker.shared.ExerciseCategory
typealias CatalogExercise = com.gymtracker.shared.CatalogExercise

val ExerciseCatalog = com.gymtracker.shared.ExerciseCatalog
