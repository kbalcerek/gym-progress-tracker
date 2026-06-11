package com.example.gymprogresstracker.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymprogresstracker.data.Exercise
import com.example.gymprogresstracker.data.GymRepository
import com.example.gymprogresstracker.data.WorkoutSet
import com.example.gymprogresstracker.data.WorkoutSetView
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WorkoutDay(
    val date: LocalDate,
    val exercises: List<WorkoutExerciseGroup>
)

data class WorkoutExerciseGroup(
    val exerciseId: Long,
    val exerciseName: String,
    val sets: List<WorkoutSetView>
)

class WorkoutLogViewModel(private val repository: GymRepository) : ViewModel() {

    val workoutDays: StateFlow<List<WorkoutDay>> = repository.workoutSets
        .map { sets ->
            sets.groupBy { it.date }
                .entries
                .sortedByDescending { it.key }
                .map { (date, dateSets) ->
                    WorkoutDay(
                        date = date,
                        exercises = dateSets.groupBy { it.exerciseId }
                            .entries
                            .map { (exerciseId, exerciseSets) ->
                                WorkoutExerciseGroup(
                                    exerciseId = exerciseId,
                                    exerciseName = exerciseSets.first().exerciseName,
                                    sets = exerciseSets.sortedBy { it.setOrder }
                                )
                            }
                    )
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val exercises: StateFlow<List<Exercise>> = repository.exercises
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addSet(date: LocalDate, exerciseId: Long?, exerciseName: String, weightKg: Double, reps: Int, notes: String) {
        viewModelScope.launch {
            val id = exerciseId ?: repository.getOrCreateExercise(exerciseName)
            val order = repository.getNextSetOrder(date, id)
            repository.insertWorkoutSet(
                WorkoutSet(date = date, exerciseId = id, weightKg = weightKg, reps = reps, notes = notes, setOrder = order)
            )
        }
    }

    fun updateSet(set: WorkoutSet, exerciseId: Long?, exerciseName: String) {
        viewModelScope.launch {
            val id = exerciseId ?: repository.getOrCreateExercise(exerciseName)
            repository.updateWorkoutSet(set.copy(exerciseId = id))
        }
    }

    fun deleteSet(set: WorkoutSet) {
        viewModelScope.launch { repository.deleteWorkoutSet(set) }
    }

    companion object {
        fun factory(repository: GymRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                WorkoutLogViewModel(repository) as T
        }
    }
}
