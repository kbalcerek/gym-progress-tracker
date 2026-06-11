package com.example.gymprogresstracker.data.sync

import kotlinx.serialization.Serializable

@Serializable
data class BackupSnapshot(
    val exportedAt: Long = System.currentTimeMillis(),
    val exercises: List<ExerciseDto>,
    val workoutSets: List<WorkoutSetDto>,
    val bodyweightEntries: List<BodyweightEntryDto>
)

@Serializable
data class ExerciseDto(val id: Long, val name: String)

@Serializable
data class WorkoutSetDto(
    val id: Long,
    val date: Long,
    val exerciseId: Long,
    val weightKg: Double,
    val reps: Int,
    val notes: String,
    val setOrder: Int
)

@Serializable
data class BodyweightEntryDto(
    val id: Long,
    val date: Long,
    val weightKg: Double
)
