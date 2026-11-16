package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.MenuBook
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

    val recurring = remember(slots) { slots.filter { it.slot.isRecurring } }
    val grouped = remember(recurring) { recurring.groupBy { it.slot.dayOfWeek } }

    // One LazyColumn owns every section, so the module list cannot consume the
    // height needed by a nested timetable list on short devices.
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = stringResource(R.string.timetable),
                subtitle = "Manage your modules and weekly class schedule",
                icon = Icons.Default.CalendarMonth
            )
        }

        if (editable) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Button(
                        onClick = { showNewModule = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.add_module), maxLines = 1)
                    }
                    FilledTonalButton(
                        onClick = { showNewSlot = true },
                        enabled = modules.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.add_weekly_slot), maxLines = 1)
                    }
                }
            }
        }

        item { SectionHeading(stringResource(R.string.module), Icons.Default.MenuBook) }

        if (modules.isEmpty()) {
            item {
                EmptyState(
                    title = "No modules yet",
                    message = "Add your semester modules to start building your timetable.",
                    icon = Icons.Default.MenuBook
                )
            }
        } else {
            items(modules, key = { "module-${it.id}" }) { module ->
                ModuleRow(
                    module = module,
                    editable = editable,
                    onEdit = { moduleDialog = module },
                    onDelete = { deleteModule = module }
                )
            }
        }

        item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
        item { SectionHeading(stringResource(R.string.weekly_timetable), Icons.Default.CalendarMonth) }

        if (recurring.isEmpty()) {
            item {
                EmptyState(
                    title = "No weekly classes",
                    message = stringResource(R.string.no_timetable),
                    icon = Icons.Default.EventBusy
                )
            }
        } else {
            (1..7).forEach { day ->
                val daySlots = grouped[day].orEmpty()
                if (daySlots.isNotEmpty()) {
                    item(key = "day-$day") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                DayOfWeek.of(day).getDisplayName(TextStyle.FULL, Locale.getDefault()),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    daySlots.size.toString(),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    items(daySlots, key = { "slot-${it.slot.id}" }) { item ->
                        SlotRow(
                            item = item,
                            editable = editable,
                            onEdit = { slotDialog = it },
                            onDelete = { deleteSlot = it }
                        )
                    }
                }
            }
        }
    }

    if (showNewModule) {
        ModuleDialog(null, viewModel.settingsManager.defaultThreshold, { showNewModule = false }) { name, threshold ->
            viewModel.addModule(name, threshold)
            showNewModule = false
        }
    }
    moduleDialog?.let { module ->
        ModuleDialog(module, viewModel.settingsManager.defaultThreshold, { moduleDialog = null }) { name, threshold ->
            viewModel.updateModule(module.copy(name = name, attendanceThreshold = threshold))
            moduleDialog = null
        }
    }
    if (showNewSlot) {
        SessionDialog(modules = modules, onDismiss = { showNewSlot = false }) { moduleId, day, start, end, type ->
            viewModel.addTimetableSlot(moduleId, day, start, end, type)
            showNewSlot = false
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
private fun ModuleRow(
    module: Module,
    editable: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(module.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.attendance_threshold, module.attendanceThreshold.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (editable) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, stringResource(R.string.edit_module), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun SlotRow(
    item: TimetableSlotWithModule,
    editable: Boolean,
    onEdit: (TimetableSlot) -> Unit,
    onDelete: (TimetableSlot) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    SessionTypeIcon(
                        item.slot.sessionType,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.moduleName, fontWeight = FontWeight.SemiBold)
                Text(
                    "${item.slot.sessionType}  ·  ${item.slot.startTime}–${item.slot.endTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (editable) {
                IconButton(onClick = { onEdit(item.slot) }) {
                    Icon(Icons.Default.Edit, stringResource(R.string.edit_session), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { onDelete(item.slot) }) {
                    Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
