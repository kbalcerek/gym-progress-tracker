package com.example.gymprogresstracker

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.gymprogresstracker.data.AppDatabase
import com.example.gymprogresstracker.data.GymRepository
import com.example.gymprogresstracker.data.sync.BackupManager
import com.example.gymprogresstracker.data.sync.DropboxAuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private val Application.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gym_settings")

class GymApplication : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy { GymRepository(database) }
    val dropboxAuthManager by lazy { DropboxAuthManager(dataStore) }
    val backupManager by lazy { BackupManager(repository, dropboxAuthManager) }

    private val _isDropboxLinked = MutableStateFlow(false)
    val isDropboxLinked: StateFlow<Boolean> = _isDropboxLinked

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            dropboxAuthManager.isLinked.collect { linked ->
                _isDropboxLinked.value = linked
            }
        }
    }
}
