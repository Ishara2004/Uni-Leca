package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
fun SettingsScreen(
    viewModel: MainViewModel,
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
    onOpenSemesterHistory: () -> Unit,
    onOpenAttendanceHistory: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var notifications by remember { mutableStateOf(viewModel.settingsManager.notificationsEnabled) }
    var threshold by remember { mutableFloatStateOf(viewModel.settingsManager.defaultThreshold) }
    var reminderDelay by remember { mutableIntStateOf(viewModel.settingsManager.reminderDelayMinutes) }
    var medicalCountsAsAttended by remember { mutableStateOf(viewModel.settingsManager.medicalCountsAsAttended) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notifications = granted
        viewModel.setNotificationsEnabled(granted)
        if (!granted) Toast.makeText(context, "Notification permission was not granted", Toast.LENGTH_SHORT).show()
    }

    fun setNotifications(enabled: Boolean) {
        if (!enabled) {
            notifications = false
            viewModel.setNotificationsEnabled(false)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            notifications = true
            viewModel.setNotificationsEnabled(true)
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val ok = context.contentResolver.openOutputStream(uri)?.use { viewModel.exportBackup(it) } == true
            Toast.makeText(context, if (ok) "Backup created" else "Backup failed", Toast.LENGTH_SHORT).show()
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> pendingRestoreUri = uri }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        ScreenHeader(
            title = stringResource(R.string.settings),
            subtitle = "Personalise the app and manage your academic data",
            icon = Icons.Default.Settings
        )

        SettingsCard(title = "Appearance", icon = Icons.Default.Palette) {
            Text(
                "Choose how Uni Leca looks on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = themeMode == "system",
                    onClick = { onThemeModeChange("system") },
                    label = { Text("System") },
                    leadingIcon = { Icon(Icons.Default.SettingsBrightness, null, modifier = Modifier.size(18.dp)) }
                )
                FilterChip(
                    selected = themeMode == "light",
                    onClick = { onThemeModeChange("light") },
                    label = { Text("Light") },
                    leadingIcon = { Icon(Icons.Default.LightMode, null, modifier = Modifier.size(18.dp)) }
                )
                FilterChip(
                    selected = themeMode == "dark",
                    onClick = { onThemeModeChange("dark") },
                    label = { Text("Dark") },
                    leadingIcon = { Icon(Icons.Default.DarkMode, null, modifier = Modifier.size(18.dp)) }
                )
            }
        }

        SettingsCard(title = stringResource(R.string.preferences), icon = Icons.Default.NotificationsActive) {
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
                            viewModel.setReminderDelay(minutes)
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
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.medical_policy), fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = medicalCountsAsAttended,
                    onClick = {
                        medicalCountsAsAttended = true
                        viewModel.setMedicalCountsAsAttended(true)
                    },
                    label = { Text(stringResource(R.string.medical_counts_attended)) }
                )
                FilterChip(
                    selected = !medicalCountsAsAttended,
                    onClick = {
                        medicalCountsAsAttended = false
                        viewModel.setMedicalCountsAsAttended(false)
                    },
                    label = { Text(stringResource(R.string.medical_excluded)) }
                )
            }
        }

        SettingsCard(title = stringResource(R.string.backup_restore), icon = Icons.Default.Backup) {
            Text(
                stringResource(R.string.backup_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Button(onClick = { backupLauncher.launch("UniLeca_Backup_v2.json") }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Backup, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.backup_data))
            }
            OutlinedButton(onClick = { restoreLauncher.launch(arrayOf("application/json", "text/plain")) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Restore, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.restore_data))
            }
        }

        OutlinedButton(onClick = onOpenAttendanceHistory, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.FactCheck, null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.attendance_history))
        }
        OutlinedButton(onClick = onOpenSemesterHistory, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.History, null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.semester_management))
        }

        SettingsCard(title = stringResource(R.string.about), icon = Icons.Default.Info) {
            Text(stringResource(R.string.app_version), fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.privacy_policy), fontWeight = FontWeight.Medium)
            Text(
                stringResource(R.string.privacy_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), content = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            content()
        })
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
