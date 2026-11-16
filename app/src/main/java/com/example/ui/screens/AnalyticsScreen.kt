package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = stringResource(R.string.attendance_analytics),
                subtitle = "Track your attendance and understand where you stand",
                icon = Icons.Default.Assessment
            )
        }

        if (stats.isEmpty()) {
            item {
                EmptyState(
                    title = "No attendance data yet",
                    message = stringResource(R.string.no_analytics),
                    icon = Icons.Default.QueryStats
                )
            }
        } else {
            items(stats, key = { "analytics-${it.module.id}" }) { stat ->
                ModuleAnalyticsCard(stat, viewModel)
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
        true -> if ((pct ?: 0f) >= stat.module.attendanceThreshold + 5f) {
            stringResource(R.string.safe)
        } else {
            stringResource(R.string.at_risk)
        }
        false -> stringResource(R.string.below_threshold)
    }
    val statusColor = when (stat.meetsThreshold) {
        false -> MaterialTheme.colorScheme.error
        true -> MaterialTheme.colorScheme.primary
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(stat.module.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(status, style = MaterialTheme.typography.labelMedium, color = statusColor, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    formatPct(pct),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            if (pct != null) {
                LinearProgressIndicator(
                    progress = { (pct / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Text(
                stringResource(R.string.held_summary, stat.heldCount, stat.presentCount, stat.medicalCount, stat.absentCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SectionHeading("By session type", Icons.Default.Assessment)

            SessionType.entries.forEach { sessionType ->
                val type = stat.bySessionType.firstOrNull {
                    it.sessionType.equals(sessionType.dbValue, ignoreCase = true)
                }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SessionTypeIcon(sessionType.dbValue, Modifier.size(19.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        sessionType.dbValue,
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Medium
                    )
                    Text(formatPct(type?.percentage), fontWeight = FontWeight.Bold)
                }
                type?.let {
                    Text(
                        stringResource(R.string.held_summary, it.heldCount, it.presentCount, it.medicalCount, it.absentCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 27.dp, bottom = 3.dp)
                    )
                }
            }

            if (risk.currentPercentage != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SectionHeading("Attendance outlook", Icons.Default.QueryStats)
                risk.percentageAfterOneMiss?.let {
                    Text(
                        stringResource(R.string.next_miss_prediction, formatPct(it)),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if ((stat.meetsThreshold == true) && risk.missesAllowedBeforeBelowThreshold > 0) {
                    Text(
                        stringResource(R.string.misses_available, risk.missesAllowedBeforeBelowThreshold),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (stat.meetsThreshold == false && risk.consecutiveAttendancesToRecover > 0) {
                    Text(
                        stringResource(R.string.recovery_needed, risk.consecutiveAttendancesToRecover),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun formatPct(value: Float?): String =
    value?.let { String.format(Locale.US, "%.1f%%", it) } ?: "—"
