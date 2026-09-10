package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.MainViewModel
import com.ishara.unileca.R
import kotlinx.coroutines.launch

@Composable
fun ReportsScreen(viewModel: MainViewModel) {
    val semesters by viewModel.allSemesters.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedSemesterId.collectAsStateWithLifecycle()
    val selected = semesters.firstOrNull { it.id == selectedId }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var menu by remember { mutableStateOf(false) }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { viewModel.exportPDF(it) }
            }.onSuccess { Toast.makeText(context, "PDF exported", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, it.message ?: "Export failed", Toast.LENGTH_LONG).show() }
        }
    }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { viewModel.exportCSV(it) }
            }.onSuccess { Toast.makeText(context, "CSV exported", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, it.message ?: "Export failed", Toast.LENGTH_LONG).show() }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.export_reports), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.select_semester), fontWeight = FontWeight.Medium)
        Box {
            OutlinedButton(onClick = { menu = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selected?.name ?: stringResource(R.string.no_semester))
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, null)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                semesters.forEach { sem ->
                    DropdownMenuItem(text = { Text(sem.name) }, onClick = { viewModel.selectSemester(sem.id); menu = false })
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Button(
            enabled = selected != null,
            onClick = { pdfLauncher.launch("UniLeca_${safeName(selected?.name)}.pdf") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.PictureAsPdf, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.export_pdf))
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            enabled = selected != null,
            onClick = { csvLauncher.launch("UniLeca_${safeName(selected?.name)}.csv") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.TableView, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.export_csv))
        }
    }
}

private fun safeName(value: String?): String = value.orEmpty().ifBlank { "Attendance" }
    .replace(Regex("[^A-Za-z0-9_-]+"), "_").take(50)
