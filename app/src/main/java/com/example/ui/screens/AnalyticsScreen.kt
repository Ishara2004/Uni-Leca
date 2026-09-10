package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ModuleAttendanceStats
import com.example.data.model.SessionType
import com.example.ui.viewmodel.MainViewModel
import com.ishara.unileca.R
import java.util.Locale

@Composable
fun AnalyticsScreen(viewModel: MainViewModel) {
    val stats by viewModel.moduleStats.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.attendance_analytics), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        if (stats.isEmpty()) {
            Text(stringResource(R.string.no_analytics), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(stats, key = { it.module.id }) { stat -> ModuleAnalyticsCard(stat, viewModel) }
            }
        }
    }
}

@Composable
private fun ModuleAnalyticsCard(stat: ModuleAttendanceStats, viewModel: MainViewModel) {
    val pct = stat.percentage
    val risk = viewModel.riskFor(stat)
    val status = when (stat.meetsThreshold) {
        null -> stringResource(R.string.no_data)
        true -> if ((pct ?: 0f) >= stat.module.attendanceThreshold + 5f) stringResource(R.string.safe) else stringResource(R.string.at_risk)
        false -> stringResource(R.string.below_threshold)
    }

    Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(stat.module.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(status, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Text(formatPct(pct), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            if (pct != null) {
                LinearProgressIndicator(
                    progress = { (pct / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text(
                stringResource(R.string.held_summary, stat.heldCount, stat.presentCount, stat.medicalCount, stat.absentCount),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))

            SessionType.entries.forEach { sessionType ->
                val type = stat.bySessionType.firstOrNull { it.sessionType.equals(sessionType.dbValue, ignoreCase = true) }
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.type_attendance, sessionType.dbValue), fontWeight = FontWeight.Medium)
                    Text(formatPct(type?.percentage), fontWeight = FontWeight.Bold)
                }
                type?.let {
                    Text(
                        stringResource(R.string.held_summary, it.heldCount, it.presentCount, it.medicalCount, it.absentCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (risk.currentPercentage != null) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(10.dp))
                risk.percentageAfterOneMiss?.let {
                    Text(stringResource(R.string.next_miss_prediction, formatPct(it)), style = MaterialTheme.typography.bodyMedium)
                }
                if ((stat.meetsThreshold == true) && risk.missesAllowedBeforeBelowThreshold > 0) {
                    Text(stringResource(R.string.misses_available, risk.missesAllowedBeforeBelowThreshold), style = MaterialTheme.typography.bodySmall)
                }
                if (stat.meetsThreshold == false && risk.consecutiveAttendancesToRecover > 0) {
                    Text(stringResource(R.string.recovery_needed, risk.consecutiveAttendancesToRecover), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun formatPct(value: Float?): String = value?.let { String.format(Locale.US, "%.1f%%", it) } ?: "—"
