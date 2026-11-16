package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Semester
import com.example.data.model.SemesterStatus
import com.example.ui.components.ConfirmDialog
import com.example.ui.viewmodel.MainViewModel
import com.ishara.unileca.R

@Composable
fun SemesterHistoryScreen(viewModel: MainViewModel, onViewSemester: () -> Unit) {
    val semesters by viewModel.allSemesters.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Semester?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = stringResource(R.string.semester_history),
                subtitle = "Switch between current and completed semesters",
                icon = Icons.Default.History
            )
        }

        if (semesters.isEmpty()) {
            item {
                EmptyState(
                    title = "No semesters saved",
                    message = "Your semester history will appear here.",
                    icon = Icons.Default.School
                )
            }
        } else {
            items(semesters, key = { "semester-${it.id}" }) { semester ->
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
                                Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(semester.name, fontWeight = FontWeight.SemiBold)
                            val active = semester.status == SemesterStatus.ACTIVE.dbValue
                            Surface(
                                color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    if (active) stringResource(R.string.active) else stringResource(R.string.ended),
                                    color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                        TextButton(onClick = { viewModel.selectSemester(semester.id); onViewSemester() }) {
                            Text(stringResource(R.string.view))
                        }
                        IconButton(onClick = { pendingDelete = semester }) {
                            Icon(
                                Icons.Default.DeleteForever,
                                stringResource(R.string.delete_semester),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { semester ->
        ConfirmDialog(
            title = stringResource(R.string.delete_semester),
            message = stringResource(R.string.delete_semester_confirm, semester.name),
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onDismiss = { pendingDelete = null },
            onConfirm = { viewModel.deleteSemester(semester.id); pendingDelete = null }
        )
    }
}
