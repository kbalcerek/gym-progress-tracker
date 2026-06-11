package com.example.gymprogresstracker.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymprogresstracker.data.BodyweightEntry
import com.example.gymprogresstracker.data.Exercise
import com.example.gymprogresstracker.data.ExerciseChartPoint
import com.example.gymprogresstracker.data.GymRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

class ProgressViewModel(private val repository: GymRepository) : ViewModel() {

    val exercises: StateFlow<List<Exercise>> = repository.exercises
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val bodyweightEntries: StateFlow<List<BodyweightEntry>> = repository.bodyweightEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedExerciseId = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val exerciseChartPoints: StateFlow<List<ExerciseChartPoint>> = selectedExerciseId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getTopWeightByDate(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectExercise(exerciseId: Long?) {
        selectedExerciseId.value = exerciseId
    }

    companion object {
        fun factory(repository: GymRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ProgressViewModel(repository) as T
        }
    }
}
