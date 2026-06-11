package com.example.gymprogresstracker.data

import java.time.LocalDate

data class ExerciseSetPoint(
    val exerciseId: Long,
    val date: LocalDate,
    val weightKg: Double,
    val reps: Int
)
