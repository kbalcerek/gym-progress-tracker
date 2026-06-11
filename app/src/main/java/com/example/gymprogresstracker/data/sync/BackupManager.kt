package com.example.gymprogresstracker.data.sync

import com.dropbox.core.v2.files.WriteMode
import com.example.gymprogresstracker.data.GymRepository
import java.io.ByteArrayInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupManager(
    private val repository: GymRepository,
    val authManager: DropboxAuthManager
) {
    companion object {
        private const val BACKUP_PATH = "/gym-backup.json"
    }

    suspend fun backupNow(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = authManager.getClient()
                ?: return@withContext Result.failure(Exception("Not linked to Dropbox"))
            val json = repository.exportToJson()
            client.files().uploadBuilder(BACKUP_PATH)
                .withMode(WriteMode.OVERWRITE)
                .uploadAndFinish(ByteArrayInputStream(json.toByteArray(Charsets.UTF_8)))
            authManager.recordBackup()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreNow(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = authManager.getClient()
                ?: return@withContext Result.failure(Exception("Not linked to Dropbox"))
            val download = client.files().download(BACKUP_PATH)
            val json = download.inputStream.bufferedReader().readText()
            repository.importFromJson(json)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
