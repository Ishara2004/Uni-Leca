package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = stringResource(R.string.attendance_history),
                subtitle = "Review past classes and open a date to update your log",
                icon = Icons.Default.History
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    SectionHeading(stringResource(R.string.history_filters), Icons.Default.FilterList)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { moduleMenu = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    modules.firstOrNull { it.id == moduleId }?.name ?: stringResource(R.string.all_modules),
                                    maxLines = 1
                                )
                            }
                            DropdownMenu(expanded = moduleMenu, onDismissRequest = { moduleMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.all_modules)) },
                                    onClick = { moduleId = null; moduleMenu = false }
                                )
                                modules.forEach { module ->
                                    DropdownMenuItem(
                                        text = { Text(module.name) },
                                        onClick = { moduleId = module.id; moduleMenu = false }
                                    )
                                }
                            }
                        }
                        Box(Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { statusMenu = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(status ?: stringResource(R.string.all_statuses), maxLines = 1)
                            }
                            DropdownMenu(expanded = statusMenu, onDismissRequest = { statusMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.all_statuses)) },
                                    onClick = { status = null; statusMenu = false }
                                )
                                listOf("Present", "Absent", "Medical", "No Class").forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text(item) },
                                        onClick = { status = item; statusMenu = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyState(
                    title = "No matching records",
                    message = stringResource(R.string.attendance_history_empty),
                    icon = Icons.Default.History
                )
            }
        } else {
            items(filtered, key = { "history-${it.entry.id}" }) { detail ->
                HistoryRow(detail = detail, onOpenDate = { onOpenDate(detail.entry.date) })
            }
        }
    }
}

@Composable
private fun HistoryRow(detail: AttendanceDetail, onOpenDate: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    SessionTypeIcon(detail.slot.sessionType, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(detail.moduleName, fontWeight = FontWeight.SemiBold)
                Text(
                    "${detail.entry.date}  ·  ${detail.slot.sessionType}  ·  ${detail.slot.startTime}–${detail.slot.endTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    AttendanceStatusPill(displayStatus(detail))
                    if (detail.slot.isExtraSession) {
                        Text(stringResource(R.string.extra_badge), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            IconButton(onClick = onOpenDate) {
                Icon(Icons.Default.CalendarMonth, stringResource(R.string.open_date), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun displayStatus(detail: AttendanceDetail): String =
    if (detail.entry.heldStatus == HeldStatus.NOT_HELD.dbValue) "No Class" else detail.entry.attendanceStatus
