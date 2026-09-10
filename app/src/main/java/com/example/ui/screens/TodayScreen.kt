package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.components.SessionDialog
import com.example.ui.viewmodel.MainViewModel
import com.ishara.unileca.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(viewModel: MainViewModel) {
    val semester by viewModel.selectedSemester.collectAsStateWithLifecycle()
    val dateText by viewModel.selectedDate.collectAsStateWithLifecycle()
    val sessions by viewModel.sessionsForSelectedDate.collectAsStateWithLifecycle()
    val entries by viewModel.attendanceForSelectedDate.collectAsStateWithLifecycle()
    val modules by viewModel.modules.collectAsStateWithLifecycle()

    val selectedDate = remember(dateText) { LocalDate.parse(dateText) }
    var showCalendar by remember { mutableStateOf(false) }
    var showExtra by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewModel.changeSelectedDate(selectedDate.minusDays(1).toString()) }) {
                Icon(Icons.Default.ChevronLeft, stringResource(R.string.previous_day))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    selectedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(selectedDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall)
            }
            Row {
                IconButton(onClick = { showCalendar = true }) {
                    Icon(Icons.Default.CalendarMonth, stringResource(R.string.open_calendar))
                }
                IconButton(onClick = { viewModel.changeSelectedDate(selectedDate.plusDays(1).toString()) }) {
                    Icon(Icons.Default.ChevronRight, stringResource(R.string.next_day))
                }
            }
        }

        if (selectedDate != LocalDate.now()) {
            TextButton(onClick = viewModel::jumpToToday, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Icon(Icons.Default.Today, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.jump_today))
            }
        }

        if (semester?.isActive == true && modules.isNotEmpty()) {
            OutlinedButton(onClick = { showExtra = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.AddAlarm, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add_extra_session))
            }
            Spacer(Modifier.height(12.dp))
        }

        val validForSemester = semester?.let { isDateInsideSemester(it, selectedDate) } == true
        if (!validForSemester || sessions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_sessions), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(sessions, key = { it.slot.id }) { item ->
                    val entry = entries.firstOrNull { it.slot.id == item.slot.id }?.entry
                    SessionCard(
                        item = item,
                        entry = entry,
                        selectedDate = selectedDate,
                        editable = semester?.isActive == true,
                        onMark = { held, status -> viewModel.markAttendance(item.slot.id, dateText, held, status) },
                        onClear = { viewModel.clearAttendance(item.slot.id, dateText) }
                    )
                }
            }
        }
    }

    if (showCalendar) {
        val initialMillis = selectedDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showCalendar = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        viewModel.changeSelectedDate(date.toString())
                    }
                    showCalendar = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { showCalendar = false }) { Text(stringResource(R.string.cancel)) } }
        ) { DatePicker(state = pickerState) }
    }

    if (showExtra) {
        SessionDialog(
            modules = modules,
            extraDate = dateText,
            onDismiss = { showExtra = false },
            onConfirm = { moduleId, _, start, end, type ->
                viewModel.addExtraSession(moduleId, dateText, start, end, type)
                showExtra = false
            }
        )
    }
}

@Composable
private fun SessionCard(
    item: TimetableSlotWithModule,
    entry: AttendanceEntry?,
    selectedDate: LocalDate,
    editable: Boolean,
    onMark: (String, String) -> Unit,
    onClear: () -> Unit
) {
    val slot = item.slot
    val started = selectedDate.isBefore(LocalDate.now()) ||
        (selectedDate == LocalDate.now() && runCatching { !LocalTime.now().isBefore(LocalTime.parse(slot.startTime)) }.getOrDefault(false))
    val canMark = editable && started && !selectedDate.isAfter(LocalDate.now())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(item.moduleName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("${slot.sessionType} · ${slot.startTime}–${slot.endTime}", style = MaterialTheme.typography.bodyMedium)
                }
                if (slot.isExtraSession) AssistChip(onClick = {}, label = { Text(stringResource(R.string.extra_badge)) })
            }

            Spacer(Modifier.height(12.dp))
            if (entry != null) {
                val label = if (entry.heldStatus == HeldStatus.NOT_HELD.dbValue) stringResource(R.string.no_class) else entry.attendanceStatus
                Text(label, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                if (editable) TextButton(onClick = onClear) { Text(stringResource(R.string.clear_mark)) }
            }

            if (canMark) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AttendanceButton(stringResource(R.string.present), Modifier.weight(1f)) {
                        onMark(HeldStatus.HELD.dbValue, AttendanceStatus.PRESENT.dbValue)
                    }
                    AttendanceButton(stringResource(R.string.absent), Modifier.weight(1f)) {
                        onMark(HeldStatus.HELD.dbValue, AttendanceStatus.ABSENT.dbValue)
                    }
                    AttendanceButton(stringResource(R.string.medical), Modifier.weight(1f)) {
                        onMark(HeldStatus.HELD.dbValue, AttendanceStatus.MEDICAL.dbValue)
                    }
                    AttendanceButton(stringResource(R.string.no_class), Modifier.weight(1f)) {
                        onMark(HeldStatus.NOT_HELD.dbValue, AttendanceStatus.PRESENT.dbValue)
                    }
                }
            } else if (editable && selectedDate >= LocalDate.now()) {
                Text(stringResource(R.string.future_marking_blocked), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AttendanceButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(horizontal = 2.dp)) {
        Text(text, maxLines = 1, style = MaterialTheme.typography.labelSmall)
    }
}

private fun isDateInsideSemester(semester: Semester, date: LocalDate): Boolean {
    val zone = java.time.ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(semester.createdDate).atZone(zone).toLocalDate()
    val end = semester.endedDate?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
    return !date.isBefore(start) && (end == null || !date.isAfter(end))
}
