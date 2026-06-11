package com.example.gymprogresstracker.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymprogresstracker.data.Exercise
import com.example.gymprogresstracker.data.GymRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExerciseViewModel(private val repository: GymRepository) : ViewModel() {

    val exercises: StateFlow<List<Exercise>> = repository.exercises
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addExercise(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.insertExercise(name) }
    }

    fun updateExercise(exercise: Exercise) {
        viewModelScope.launch { repository.updateExercise(exercise) }
    }

    fun deleteExercise(exercise: Exercise) {
        viewModelScope.launch { repository.deleteExercise(exercise) }
    }

    companion object {
        fun factory(repository: GymRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ExerciseViewModel(repository) as T
        }
    }
}
