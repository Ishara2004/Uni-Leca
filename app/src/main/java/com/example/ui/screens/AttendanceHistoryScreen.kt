package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AttendanceDetail
import com.example.data.model.HeldStatus
import com.example.data.model.Module
import com.example.ui.viewmodel.MainViewModel
import com.ishara.unileca.R

@Composable
fun AttendanceHistoryScreen(viewModel: MainViewModel, onOpenDate: (String) -> Unit) {
    val details by viewModel.attendanceDetails.collectAsStateWithLifecycle()
    val modules by viewModel.modules.collectAsStateWithLifecycle()
    var moduleId by remember { mutableStateOf<Int?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var moduleMenu by remember { mutableStateOf(false) }
    var statusMenu by remember { mutableStateOf(false) }

    val filtered = remember(details, moduleId, status) {
        details.filter { detail ->
            (moduleId == null || detail.moduleId == moduleId) &&
                (status == null || displayStatus(detail) == status)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.attendance_history), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        Text(stringResource(R.string.history_filters), style = MaterialTheme.typography.labelLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) {
                OutlinedButton(onClick = { moduleMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(modules.firstOrNull { it.id == moduleId }?.name ?: stringResource(R.string.all_modules), maxLines = 1)
                }
                DropdownMenu(expanded = moduleMenu, onDismissRequest = { moduleMenu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.all_modules)) }, onClick = { moduleId = null; moduleMenu = false })
                    modules.forEach { module ->
                        DropdownMenuItem(text = { Text(module.name) }, onClick = { moduleId = module.id; moduleMenu = false })
                    }
                }
            }
            Box(Modifier.weight(1f)) {
                OutlinedButton(onClick = { statusMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(status ?: stringResource(R.string.all_statuses), maxLines = 1)
                }
                DropdownMenu(expanded = statusMenu, onDismissRequest = { statusMenu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.all_statuses)) }, onClick = { status = null; statusMenu = false })
                    listOf("Present", "Absent", "Medical", "No Class").forEach { item ->
                        DropdownMenuItem(text = { Text(item) }, onClick = { status = item; statusMenu = false })
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.attendance_history_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.entry.id }) { detail ->
                    HistoryRow(detail = detail, onOpenDate = { onOpenDate(detail.entry.date) })
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(detail: AttendanceDetail, onOpenDate: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(detail.moduleName, fontWeight = FontWeight.Bold)
                Text("${detail.entry.date} · ${detail.slot.sessionType} · ${detail.slot.startTime}–${detail.slot.endTime}", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(displayStatus(detail), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    if (detail.slot.isExtraSession) Text(stringResource(R.string.extra_badge), style = MaterialTheme.typography.labelSmall)
                }
            }
            IconButton(onClick = onOpenDate) {
                Icon(Icons.Default.CalendarMonth, stringResource(R.string.open_date))
            }
        }
    }
}

private fun displayStatus(detail: AttendanceDetail): String =
    if (detail.entry.heldStatus == HeldStatus.NOT_HELD.dbValue) "No Class" else detail.entry.attendanceStatus
