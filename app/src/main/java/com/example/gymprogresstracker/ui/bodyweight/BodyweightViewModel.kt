package com.example.gymprogresstracker.ui.bodyweight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymprogresstracker.data.BodyweightEntry
import com.example.gymprogresstracker.data.GymRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BodyweightViewModel(private val repository: GymRepository) : ViewModel() {

    val entries: StateFlow<List<BodyweightEntry>> = repository.bodyweightEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addEntry(date: LocalDate, weightKg: Double) {
        viewModelScope.launch {
            repository.insertBodyweightEntry(BodyweightEntry(date = date, weightKg = weightKg))
        }
    }

    fun updateEntry(entry: BodyweightEntry) {
        viewModelScope.launch { repository.updateBodyweightEntry(entry) }
    }

    fun deleteEntry(entry: BodyweightEntry) {
        viewModelScope.launch { repository.deleteBodyweightEntry(entry) }
    }

    companion object {
        fun factory(repository: GymRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                BodyweightViewModel(repository) as T
        }
    }
}
