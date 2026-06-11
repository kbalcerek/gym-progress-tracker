package com.example.gymprogresstracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gymprogresstracker.data.sync.BackupManager
import com.example.gymprogresstracker.data.sync.DropboxAuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class BackupStatus {
    object Idle : BackupStatus()
    object Loading : BackupStatus()
    data class Success(val message: String) : BackupStatus()
    data class Error(val message: String) : BackupStatus()
}

class SettingsViewModel(private val backupManager: BackupManager) : ViewModel() {

    private val authManager: DropboxAuthManager = backupManager.authManager

    val isDropboxLinked: StateFlow<Boolean> = authManager.isLinked
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val lastBackupTs: StateFlow<Long?> = authManager.lastBackupTs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _backupStatus = MutableStateFlow<BackupStatus>(BackupStatus.Idle)
    val backupStatus: StateFlow<BackupStatus> = _backupStatus.asStateFlow()

    fun buildDropboxAuthUrl(): String? = authManager.buildAuthUrl()

    fun unlinkDropbox() {
        viewModelScope.launch { authManager.unlink() }
    }

    fun backupNow(onSuccess: String, onError: String) {
        viewModelScope.launch {
            _backupStatus.value = BackupStatus.Loading
            val result = backupManager.backupNow()
            _backupStatus.value = if (result.isSuccess) BackupStatus.Success(onSuccess)
            else BackupStatus.Error(result.exceptionOrNull()?.message ?: onError)
        }
    }

    fun restoreNow(onSuccess: String, onError: String) {
        viewModelScope.launch {
            _backupStatus.value = BackupStatus.Loading
            val result = backupManager.restoreNow()
            _backupStatus.value = if (result.isSuccess) BackupStatus.Success(onSuccess)
            else BackupStatus.Error(result.exceptionOrNull()?.message ?: onError)
        }
    }

    fun clearStatus() {
        _backupStatus.value = BackupStatus.Idle
    }

    companion object {
        fun factory(backupManager: BackupManager) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(backupManager) as T
        }
    }
}
