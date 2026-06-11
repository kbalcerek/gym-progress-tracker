package com.example.gymprogresstracker.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymprogresstracker.BuildConfig
import com.example.gymprogresstracker.R
import com.example.gymprogresstracker.data.sync.BackupManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    backupManager: BackupManager,
    vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(backupManager))
) {
    val context = LocalContext.current
    val isDropboxLinked by vm.isDropboxLinked.collectAsStateWithLifecycle()
    val lastBackupTs by vm.lastBackupTs.collectAsStateWithLifecycle()
    val backupStatus by vm.backupStatus.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showRestoreConfirm by remember { mutableStateOf(false) }

    val successMsg = stringResource(R.string.settings_dropbox_backup_success)
    val errorMsg = stringResource(R.string.settings_dropbox_backup_error)
    val restoreSuccessMsg = stringResource(R.string.settings_dropbox_restore_success)
    val restoreErrorMsg = stringResource(R.string.settings_dropbox_restore_error)

    LaunchedEffect(backupStatus) {
        when (val s = backupStatus) {
            is BackupStatus.Success -> {
                snackbarHostState.showSnackbar(s.message)
                vm.clearStatus()
            }
            is BackupStatus.Error -> {
                snackbarHostState.showSnackbar(s.message)
                vm.clearStatus()
            }
            else -> {}
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.nav_settings)) })
        SnackbarHost(hostState = snackbarHostState)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Language section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_language),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val currentLocale = AppCompatDelegate.getApplicationLocales()
                        .toLanguageTags().let {
                            when {
                                it.startsWith("pl") -> "pl"
                                else -> "en"
                            }
                        }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = currentLocale == "en",
                            onClick = {
                                AppCompatDelegate.setApplicationLocales(
                                    LocaleListCompat.forLanguageTags("en")
                                )
                            }
                        )
                        Text(stringResource(R.string.settings_language_en))
                        Spacer(modifier = Modifier.weight(1f))
                        RadioButton(
                            selected = currentLocale == "pl",
                            onClick = {
                                AppCompatDelegate.setApplicationLocales(
                                    LocaleListCompat.forLanguageTags("pl")
                                )
                            }
                        )
                        Text(stringResource(R.string.settings_language_pl))
                    }
                }
            }

            // Dropbox section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_dropbox),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (BuildConfig.DROPBOX_APP_KEY == "placeholder") {
                        Text(
                            text = stringResource(R.string.settings_dropbox_not_configured),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Text(
                            text = if (isDropboxLinked)
                                stringResource(R.string.settings_dropbox_linked)
                            else
                                stringResource(R.string.settings_dropbox_not_linked),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        lastBackupTs?.let { ts ->
                            val formatted = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(ts))
                            Text(
                                text = stringResource(R.string.settings_dropbox_last_backup, formatted),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } ?: Text(
                            text = stringResource(R.string.settings_dropbox_never_backed_up),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (!isDropboxLinked) {
                            Button(
                                onClick = {
                                    vm.buildDropboxAuthUrl()?.let { url ->
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.settings_dropbox_link))
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { vm.backupNow(successMsg, errorMsg) },
                                    modifier = Modifier.weight(1f),
                                    enabled = backupStatus !is BackupStatus.Loading
                                ) {
                                    if (backupStatus is BackupStatus.Loading) {
                                        CircularProgressIndicator(modifier = Modifier.padding(end = 4.dp))
                                    }
                                    Text(stringResource(R.string.settings_dropbox_backup_now))
                                }
                                OutlinedButton(
                                    onClick = { showRestoreConfirm = true },
                                    modifier = Modifier.weight(1f),
                                    enabled = backupStatus !is BackupStatus.Loading
                                ) {
                                    Text(stringResource(R.string.settings_dropbox_restore))
                                }
                            }
                            TextButton(
                                onClick = { vm.unlinkDropbox() },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text(stringResource(R.string.settings_dropbox_unlink))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text(stringResource(R.string.settings_dropbox_restore)) },
            text = { Text(stringResource(R.string.settings_dropbox_restore_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.restoreNow(restoreSuccessMsg, restoreErrorMsg)
                    showRestoreConfirm = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
