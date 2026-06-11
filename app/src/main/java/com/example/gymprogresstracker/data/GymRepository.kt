package com.example.gymprogresstracker.data

import androidx.room.withTransaction
import com.example.gymprogresstracker.data.sync.BackupSnapshot
import com.example.gymprogresstracker.data.sync.BodyweightEntryDto
import com.example.gymprogresstracker.data.sync.ExerciseDto
import com.example.gymprogresstracker.data.sync.WorkoutSetDto
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

private val lenientJson = Json { ignoreUnknownKeys = true }

class GymRepository(private val db: AppDatabase) {
    private val exerciseDao = db.exerciseDao()
    private val workoutDao = db.workoutDao()
    private val bodyweightDao = db.bodyweightDao()

    val exercises: Flow<List<Exercise>> = exerciseDao.getAllExercises()
    val workoutSets: Flow<List<WorkoutSetView>> = workoutDao.getAllSetsWithExercise()
    val bodyweightEntries: Flow<List<BodyweightEntry>> = bodyweightDao.getAll()

    fun getSetsForExercises(exerciseIds: List<Long>): Flow<List<ExerciseSetPoint>> =
        workoutDao.getSetsForExercises(exerciseIds)

    suspend fun insertExercise(name: String): Long =
        exerciseDao.insert(Exercise(name = name.trim()))

    suspend fun getOrCreateExercise(name: String): Long {
        val trimmed = name.trim()
        val id = insertExercise(trimmed)
        return if (id != -1L) id else exerciseDao.findByName(trimmed)!!.id
    }

    suspend fun updateExercise(exercise: Exercise) = exerciseDao.update(exercise)
    suspend fun deleteExercise(exercise: Exercise) = exerciseDao.delete(exercise)

    suspend fun insertWorkoutSet(set: WorkoutSet): Long = workoutDao.insert(set)
    suspend fun updateWorkoutSet(set: WorkoutSet) = workoutDao.update(set)
    suspend fun deleteWorkoutSet(set: WorkoutSet) = workoutDao.delete(set)
    suspend fun getNextSetOrder(date: LocalDate, exerciseId: Long): Int =
        (workoutDao.getMaxSetOrder(date, exerciseId) ?: -1) + 1

    suspend fun insertBodyweightEntry(entry: BodyweightEntry): Long =
        bodyweightDao.insert(entry)
    suspend fun updateBodyweightEntry(entry: BodyweightEntry) = bodyweightDao.update(entry)
    suspend fun deleteBodyweightEntry(entry: BodyweightEntry) = bodyweightDao.delete(entry)

    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val exercises = exerciseDao.getAllList().map { ExerciseDto(it.id, it.name) }
        val sets = workoutDao.getAllRaw().map {
            WorkoutSetDto(it.id, it.date.toEpochDay(), it.exerciseId, it.weightKg, it.reps, it.notes, it.setOrder)
        }
        val bwEntries = bodyweightDao.getAllList().map {
            BodyweightEntryDto(it.id, it.date.toEpochDay(), it.weightKg)
        }
        val snapshot = BackupSnapshot(exercises = exercises, workoutSets = sets, bodyweightEntries = bwEntries)
        Json.encodeToString(snapshot)
    }

    suspend fun importFromJson(json: String) {
        withContext(Dispatchers.IO) {
            val snapshot = lenientJson.decodeFromString<BackupSnapshot>(json)
            db.withTransaction {
                // Delete FK-constrained sets first, then exercises
                workoutDao.deleteAll()
                exerciseDao.deleteAll()
                bodyweightDao.deleteAll()
                snapshot.exercises.forEach {
                    exerciseDao.insertOrReplace(Exercise(id = it.id, name = it.name))
                }
                snapshot.workoutSets.forEach {
                    workoutDao.insert(WorkoutSet(
                        id = it.id,
                        date = LocalDate.ofEpochDay(it.date),
                        exerciseId = it.exerciseId,
                        weightKg = it.weightKg,
                        reps = it.reps,
                        notes = it.notes,
                        setOrder = it.setOrder
                    ))
                }
                snapshot.bodyweightEntries.forEach {
                    bodyweightDao.insert(BodyweightEntry(
                        id = it.id,
                        date = LocalDate.ofEpochDay(it.date),
                        weightKg = it.weightKg
                    ))
                }
            }
        }
    }
}
