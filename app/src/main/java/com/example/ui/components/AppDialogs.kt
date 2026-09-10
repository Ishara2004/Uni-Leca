package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.data.model.Module
import com.example.data.model.SessionType
import com.example.data.model.Semester
import com.example.data.model.TimetableSlot
import com.ishara.unileca.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun AddSemesterDialog(
    semesters: List<Semester>,
    onDismiss: () -> Unit,
    onConfirm: (String, Int?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var duplicate by remember { mutableStateOf(false) }
    var sourceId by remember(semesters) { mutableStateOf(semesters.firstOrNull()?.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_semester)) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.semester_name)) },
                    placeholder = { Text(stringResource(R.string.semester_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (semesters.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { duplicate = !duplicate },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = duplicate, onCheckedChange = { duplicate = it })
                        Text(stringResource(R.string.duplicate_previous))
                    }
                    if (duplicate) {
                        semesters.take(6).forEach { sem ->
                            Row(
                                Modifier.fillMaxWidth().clickable { sourceId = sem.id }.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = sourceId == sem.id, onClick = { sourceId = sem.id })
                                Text(sem.name)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name.trim(), if (duplicate) sourceId else null) }
            ) { Text(stringResource(R.string.create)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
fun ModuleDialog(
    initial: Module?,
    defaultThreshold: Float,
    onDismiss: () -> Unit,
    onConfirm: (String, Float) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var threshold by remember(initial) { mutableFloatStateOf(initial?.attendanceThreshold ?: defaultThreshold) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.add_module else R.string.edit_module)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.module_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.attendance_threshold, threshold.toInt()))
                Slider(value = threshold, onValueChange = { threshold = it }, valueRange = 50f..100f, steps = 49)
            }
        },
        confirmButton = {
            Button(enabled = name.isNotBlank(), onClick = { onConfirm(name.trim(), threshold) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
fun SessionDialog(
    modules: List<Module>,
    initial: TimetableSlot? = null,
    extraDate: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (moduleId: Int, day: Int, start: String, end: String, type: String) -> Unit
) {
    var moduleId by remember(initial, modules) { mutableIntStateOf(initial?.moduleId ?: modules.firstOrNull()?.id ?: 0) }
    var day by remember(initial, extraDate) {
        mutableIntStateOf(initial?.dayOfWeek ?: extraDate?.let { LocalDate.parse(it).dayOfWeek.value } ?: DayOfWeek.MONDAY.value)
    }
    var start by remember(initial) { mutableStateOf(initial?.startTime ?: "09:00") }
    var end by remember(initial) { mutableStateOf(initial?.endTime ?: "10:00") }
    var type by remember(initial) { mutableStateOf(initial?.sessionType ?: SessionType.LECTURE.dbValue) }
    var dayMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    when {
                        extraDate != null -> R.string.add_extra_session
                        initial != null -> R.string.edit_session
                        else -> R.string.add_weekly_slot
                    }
                )
            )
        },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState())
            ) {
                Text(stringResource(R.string.module), style = MaterialTheme.typography.labelLarge)
                modules.forEach { module ->
                    Row(
                        Modifier.fillMaxWidth().clickable { moduleId = module.id },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = moduleId == module.id, onClick = { moduleId = module.id })
                        Text(module.name)
                    }
                }

                if (extraDate == null) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.day), style = MaterialTheme.typography.labelLarge)
                    Box {
                        OutlinedButton(onClick = { dayMenu = true }) {
                            Text(DayOfWeek.of(day).getDisplayName(TextStyle.FULL, Locale.getDefault()))
                        }
                        DropdownMenu(expanded = dayMenu, onDismissRequest = { dayMenu = false }) {
                            DayOfWeek.entries.forEach { d ->
                                DropdownMenuItem(
                                    text = { Text(d.getDisplayName(TextStyle.FULL, Locale.getDefault())) },
                                    onClick = { day = d.value; dayMenu = false }
                                )
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                    Text(extraDate, style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.session_type), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SessionType.entries.forEach { sessionType ->
                        FilterChip(
                            selected = type == sessionType.dbValue,
                            onClick = { type = sessionType.dbValue },
                            label = { Text(sessionType.dbValue) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = start,
                        onValueChange = { start = it.take(5) },
                        label = { Text(stringResource(R.string.start_time)) },
                        placeholder = { Text("09:00") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = end,
                        onValueChange = { end = it.take(5) },
                        label = { Text(stringResource(R.string.end_time)) },
                        placeholder = { Text("10:00") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = moduleId != 0 && start.isNotBlank() && end.isNotBlank(),
                onClick = { onConfirm(moduleId, day, start, end, type) }
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    destructive: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (destructive) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                else ButtonDefaults.buttonColors()
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
