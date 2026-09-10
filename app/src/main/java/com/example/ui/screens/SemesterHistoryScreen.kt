package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
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

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.semester_history), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(semesters, key = { it.id }) { semester ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(semester.name, fontWeight = FontWeight.Bold)
                            Text(
                                if (semester.status == SemesterStatus.ACTIVE.dbValue) stringResource(R.string.active) else stringResource(R.string.ended),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        TextButton(onClick = { viewModel.selectSemester(semester.id); onViewSemester() }) {
                            Text(stringResource(R.string.view))
                        }
                        IconButton(onClick = { pendingDelete = semester }) {
                            Icon(Icons.Default.DeleteForever, stringResource(R.string.delete_semester), tint = MaterialTheme.colorScheme.error)
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
