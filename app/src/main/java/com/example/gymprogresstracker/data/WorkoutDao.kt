package com.example.gymprogresstracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("""
        SELECT ws.id, ws.date, ws.exercise_id AS exerciseId,
               e.name AS exerciseName, ws.weight_kg AS weightKg,
               ws.reps, ws.notes, ws.set_order AS setOrder
        FROM workout_sets ws
        JOIN exercises e ON ws.exercise_id = e.id
        ORDER BY ws.date DESC, ws.exercise_id ASC, ws.set_order ASC
    """)
    fun getAllSetsWithExercise(): Flow<List<WorkoutSetView>>

    @Query("""
        SELECT ws.date, MAX(ws.weight_kg) AS maxWeightKg
        FROM workout_sets ws
        WHERE ws.exercise_id = :exerciseId
        GROUP BY ws.date
        ORDER BY ws.date ASC
    """)
    fun getTopWeightByDate(exerciseId: Long): Flow<List<ExerciseChartPoint>>

    @Query("SELECT * FROM workout_sets ORDER BY date ASC, set_order ASC")
    suspend fun getAllRaw(): List<WorkoutSet>

    @Query("SELECT MAX(set_order) FROM workout_sets WHERE date = :date AND exercise_id = :exerciseId")
    suspend fun getMaxSetOrder(date: LocalDate, exerciseId: Long): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(set: WorkoutSet): Long

    @Update
    suspend fun update(set: WorkoutSet)

    @Delete
    suspend fun delete(set: WorkoutSet)

    @Query("DELETE FROM workout_sets")
    suspend fun deleteAll()
}
