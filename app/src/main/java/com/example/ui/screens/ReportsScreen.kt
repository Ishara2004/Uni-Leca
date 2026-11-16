package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.School
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

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader(
            title = stringResource(R.string.export_reports),
            subtitle = "Create a shareable record of your semester attendance",
            icon = Icons.Default.Assessment
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeading(stringResource(R.string.select_semester), Icons.Default.School)
                Box {
                    OutlinedButton(
                        onClick = { menu = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selected?.name ?: stringResource(R.string.no_semester))
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        semesters.forEach { sem ->
                            DropdownMenuItem(
                                text = { Text(sem.name) },
                                onClick = { viewModel.selectSemester(sem.id); menu = false }
                            )
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeading("Export format", Icons.Default.Assessment)
                Text(
                    "Choose a format to save or share your attendance report.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    enabled = selected != null,
                    onClick = { pdfLauncher.launch("UniLeca_${safeName(selected?.name)}.pdf") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PictureAsPdf, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.export_pdf))
                }
                FilledTonalButton(
                    enabled = selected != null,
                    onClick = { csvLauncher.launch("UniLeca_${safeName(selected?.name)}.csv") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.TableView, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.export_csv))
                }
            }
        }
    }
}

private fun safeName(value: String?): String = value.orEmpty().ifBlank { "Attendance" }
    .replace(Regex("[^A-Za-z0-9_-]+"), "_").take(50)
