package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Module
import com.example.data.model.SemesterStatus
import com.example.data.model.TimetableSlot
import com.example.data.model.TimetableSlotWithModule
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.ModuleDialog
import com.example.ui.components.SessionDialog
import com.example.ui.viewmodel.MainViewModel
import com.ishara.unileca.R
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TimetableScreen(viewModel: MainViewModel) {
    val modules by viewModel.modules.collectAsStateWithLifecycle()
    val slots by viewModel.slots.collectAsStateWithLifecycle()
    val semester by viewModel.selectedSemester.collectAsStateWithLifecycle()
    val editable = semester?.status == SemesterStatus.ACTIVE.dbValue

    var moduleDialog by remember { mutableStateOf<Module?>(null) }
    var showNewModule by remember { mutableStateOf(false) }
    var slotDialog by remember { mutableStateOf<TimetableSlot?>(null) }
    var showNewSlot by remember { mutableStateOf(false) }
    var deleteModule by remember { mutableStateOf<Module?>(null) }
    var deleteSlot by remember { mutableStateOf<TimetableSlot?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (editable) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showNewModule = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.add_module))
                }
                Button(onClick = { showNewSlot = true }, enabled = modules.isNotEmpty(), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.add_weekly_slot))
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Text(stringResource(R.string.module), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (modules.isNotEmpty()) {
            modules.forEach { module ->
                ListItem(
                    headlineContent = { Text(module.name) },
                    supportingContent = { Text(stringResource(R.string.attendance_threshold, module.attendanceThreshold.toInt())) },
                    trailingContent = {
                        if (editable) Row {
                            IconButton(onClick = { moduleDialog = module }) { Icon(Icons.Default.Edit, stringResource(R.string.edit_module)) }
                            IconButton(onClick = { deleteModule = module }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
                        }
                    }
                )
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 10.dp))
        Text(stringResource(R.string.weekly_timetable), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))

        val recurring = slots.filter { it.slot.isRecurring }
        if (recurring.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_timetable), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val grouped = recurring.groupBy { it.slot.dayOfWeek }
                (1..7).forEach { day ->
                    val daySlots = grouped[day].orEmpty()
                    if (daySlots.isNotEmpty()) {
                        item(key = "day-$day") {
                            Text(
                                DayOfWeek.of(day).getDisplayName(TextStyle.FULL, Locale.getDefault()),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(daySlots, key = { it.slot.id }) { item ->
                            SlotRow(item, editable, onEdit = { slotDialog = it }, onDelete = { deleteSlot = it })
                        }
                    }
                }
            }
        }
    }

    if (showNewModule) {
        ModuleDialog(null, viewModel.settingsManager.defaultThreshold, { showNewModule = false }) { name, threshold ->
            viewModel.addModule(name, threshold); showNewModule = false
        }
    }
    moduleDialog?.let { module ->
        ModuleDialog(module, viewModel.settingsManager.defaultThreshold, { moduleDialog = null }) { name, threshold ->
            viewModel.updateModule(module.copy(name = name, attendanceThreshold = threshold)); moduleDialog = null
        }
    }
    if (showNewSlot) {
        SessionDialog(modules = modules, onDismiss = { showNewSlot = false }) { moduleId, day, start, end, type ->
            viewModel.addTimetableSlot(moduleId, day, start, end, type); showNewSlot = false
        }
    }
    slotDialog?.let { slot ->
        SessionDialog(modules = modules, initial = slot, onDismiss = { slotDialog = null }) { moduleId, day, start, end, type ->
            viewModel.updateTimetableSlot(slot.copy(moduleId = moduleId, dayOfWeek = day, startTime = start, endTime = end, sessionType = type))
            slotDialog = null
        }
    }
    deleteModule?.let { module ->
        ConfirmDialog(
            title = stringResource(R.string.delete),
            message = "Remove ${module.name}? Existing attendance history will be preserved by archiving when necessary.",
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onDismiss = { deleteModule = null },
            onConfirm = { viewModel.deleteModule(module); deleteModule = null }
        )
    }
    deleteSlot?.let { slot ->
        ConfirmDialog(
            title = stringResource(R.string.delete),
            message = "Remove this session? Existing attendance history will be preserved.",
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onDismiss = { deleteSlot = null },
            onConfirm = { viewModel.deleteTimetableSlot(slot); deleteSlot = null }
        )
    }
}

@Composable
private fun SlotRow(
    item: TimetableSlotWithModule,
    editable: Boolean,
    onEdit: (TimetableSlot) -> Unit,
    onDelete: (TimetableSlot) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.moduleName, fontWeight = FontWeight.Bold)
                Text("${item.slot.sessionType} · ${item.slot.startTime}–${item.slot.endTime}", style = MaterialTheme.typography.bodySmall)
            }
            if (editable) {
                IconButton(onClick = { onEdit(item.slot) }) { Icon(Icons.Default.Edit, stringResource(R.string.edit_session)) }
                IconButton(onClick = { onDelete(item.slot) }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
            }
        }
    }
}
