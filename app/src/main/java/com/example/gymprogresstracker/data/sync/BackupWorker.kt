package com.example.gymprogresstracker.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.gymprogresstracker.GymApplication

class BackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as GymApplication
        val result = app.backupManager.backupNow()
        return if (result.isSuccess) Result.success() else Result.retry()
    }
}
