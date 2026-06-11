package com.example.gymprogresstracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.gymprogresstracker.data.sync.BackupWorker
import com.example.gymprogresstracker.ui.navigation.GymNavHost
import com.example.gymprogresstracker.ui.theme.GymProgressTrackerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GymProgressTrackerTheme {
                GymNavHost()
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver(application as GymApplication))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val uri = intent.data ?: return
        if (uri.scheme?.startsWith("db-") == true) {
            val code = uri.getQueryParameter("code") ?: return
            lifecycleScope.launch {
                (application as GymApplication).dropboxAuthManager.handleAuthCode(code)
            }
        }
    }
}

private class AppLifecycleObserver(private val app: GymApplication) : DefaultLifecycleObserver {
    override fun onStop(owner: LifecycleOwner) {
        if (!app.isDropboxLinked.value) return
        val request = OneTimeWorkRequestBuilder<BackupWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(app).enqueueUniqueWork(
            "auto_backup",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
