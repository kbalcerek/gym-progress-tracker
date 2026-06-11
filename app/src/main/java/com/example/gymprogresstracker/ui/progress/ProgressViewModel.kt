package com.example.gymprogresstracker.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymprogresstracker.data.BodyweightEntry
import com.example.gymprogresstracker.data.Exercise
import com.example.gymprogresstracker.data.ExerciseSetPoint
import com.example.gymprogresstracker.data.GymRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class ExerciseSeriesData(
    val exerciseId: Long,
    val exerciseName: String,
    val colorIndex: Int,
    val points: List<ExerciseSetPoint>
)

class ProgressViewModel(private val repository: GymRepository) : ViewModel() {

    val exercises: StateFlow<List<Exercise>> = repository.exercises
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val bodyweightEntries: StateFlow<List<BodyweightEntry>> = repository.bodyweightEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedExerciseIds = MutableStateFlow<Set<Long>>(emptySet())

    fun toggleExercise(exerciseId: Long) {
        selectedExerciseIds.value =
            if (exerciseId in selectedExerciseIds.value) selectedExerciseIds.value - exerciseId
            else selectedExerciseIds.value + exerciseId
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val exerciseSeries: StateFlow<List<ExerciseSeriesData>> = selectedExerciseIds
        .flatMapLatest { ids ->
            if (ids.isEmpty()) flowOf(emptyList())
            else repository.getSetsForExercises(ids.sorted())
        }
        .combine(exercises) { sets, exs ->
            sets.groupBy { it.exerciseId }
                .mapNotNull { (id, pts) ->
                    val idx = exs.indexOfFirst { it.id == id }
                    if (idx < 0) null
                    else ExerciseSeriesData(id, exs[idx].name, idx, pts)
                }
                .sortedBy { it.colorIndex }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        fun factory(repository: GymRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ProgressViewModel(repository) as T
        }
    }
}
