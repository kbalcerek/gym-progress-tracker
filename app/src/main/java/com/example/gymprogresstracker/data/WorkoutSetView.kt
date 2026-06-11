package com.example.gymprogresstracker.data

import java.time.LocalDate

data class WorkoutSetView(
    val id: Long,
    val date: LocalDate,
    val exerciseId: Long,
    val exerciseName: String,
    val weightKg: Double,
    val reps: Int,
    val notes: String,
    val setOrder: Int
)
