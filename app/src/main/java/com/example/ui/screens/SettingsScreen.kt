package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.components.ConfirmDialog
import com.example.ui.viewmodel.MainViewModel
import com.ishara.unileca.R
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: MainViewModel, onOpenSemesterHistory: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var notifications by remember { mutableStateOf(viewModel.settingsManager.notificationsEnabled) }
    var threshold by remember { mutableFloatStateOf(viewModel.settingsManager.defaultThreshold) }
    var reminderDelay by remember { mutableIntStateOf(viewModel.settingsManager.reminderDelayMinutes) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notifications = granted
        viewModel.settingsManager.notificationsEnabled = granted
        if (!granted) Toast.makeText(context, "Notification permission was not granted", Toast.LENGTH_SHORT).show()
    }

    fun setNotifications(enabled: Boolean) {
        if (!enabled) {
            notifications = false
            viewModel.settingsManager.notificationsEnabled = false
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            notifications = true
            viewModel.settingsManager.notificationsEnabled = true
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val ok = context.contentResolver.openOutputStream(uri)?.use { viewModel.exportBackup(it) } == true
            Toast.makeText(context, if (ok) "Backup created" else "Backup failed", Toast.LENGTH_SHORT).show()
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingRestoreUri = uri
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        SettingsCard(stringResource(R.string.preferences)) {
            SettingSwitch(
                title = stringResource(R.string.class_reminders),
                description = stringResource(R.string.class_reminders_body),
                checked = notifications,
                onCheckedChange = ::setNotifications
            )
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.reminder_delay), fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0, 10, 30, 60).forEach { minutes ->
                    FilterChip(
                        selected = reminderDelay == minutes,
                        onClick = {
                            reminderDelay = minutes
                            viewModel.settingsManager.reminderDelayMinutes = minutes
                        },
                        label = { Text("${minutes}m") }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.default_threshold, threshold.toInt()), fontWeight = FontWeight.Medium)
            Slider(
                value = threshold,
                onValueChange = {
                    threshold = it
                    viewModel.settingsManager.defaultThreshold = it
                },
                valueRange = 50f..100f,
                steps = 49
            )
        }

        Spacer(Modifier.height(12.dp))
        SettingsCard(stringResource(R.string.backup_restore)) {
            Text(stringResource(R.string.backup_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Button(onClick = { backupLauncher.launch("UniLeca_Backup_v2.json") }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Backup, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.backup_data))
            }
            OutlinedButton(onClick = { restoreLauncher.launch(arrayOf("application/json", "text/plain")) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Restore, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.restore_data))
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onOpenSemesterHistory, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.History, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.semester_management))
        }

        Spacer(Modifier.height(12.dp))
        SettingsCard(stringResource(R.string.about)) {
            Text(stringResource(R.string.app_version), fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.privacy_policy), fontWeight = FontWeight.Medium)
            Text(stringResource(R.string.privacy_summary), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    pendingRestoreUri?.let { uri ->
        ConfirmDialog(
            title = stringResource(R.string.confirm_restore),
            message = stringResource(R.string.restore_warning),
            confirmLabel = stringResource(R.string.restore),
            destructive = true,
            onDismiss = { pendingRestoreUri = null },
            onConfirm = {
                pendingRestoreUri = null
                scope.launch {
                    val ok = context.contentResolver.openInputStream(uri)?.use { viewModel.importBackup(it) } == true
                    Toast.makeText(context, if (ok) "Backup restored" else "Restore failed", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), content = {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            content()
        })
    }
}

@Composable
private fun SettingSwitch(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
