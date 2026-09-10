package com.example.ui.reports

import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.data.model.AttendanceDetail
import com.example.data.model.ModuleAttendanceStats
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object ExportHelper {
    fun exportToCSV(
        semesterName: String,
        details: List<AttendanceDetail>,
        moduleStats: List<ModuleAttendanceStats>,
        outputStream: OutputStream
    ) {
        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.appendLine("UNI LECA ATTENDANCE REPORT")
            writer.appendLine("Semester,${csv(semesterName)}")
            writer.appendLine("Generated,${csv(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))}")
            writer.appendLine()
            writer.appendLine("MODULE SUMMARY")
            writer.appendLine("Module,Session Type,Threshold,Classes Held,Present,Medical,Absent,Attendance %,Status")

            moduleStats.forEach { stat ->
                val overallStatus = when (stat.meetsThreshold) {
                    true -> "MEETS THRESHOLD"
                    false -> "BELOW THRESHOLD"
                    null -> "NO DATA"
                }
                writer.appendLine(
                    listOf(
                        csv(stat.module.name),
                        csv("Overall"),
                        csv("${stat.module.attendanceThreshold}%"),
                        stat.heldCount.toString(),
                        stat.presentCount.toString(),
                        stat.medicalCount.toString(),
                        stat.absentCount.toString(),
                        csv(formatPct(stat.percentage)),
                        csv(overallStatus)
                    ).joinToString(",")
                )
                stat.bySessionType.forEach { type ->
                    writer.appendLine(
                        listOf(
                            csv(stat.module.name),
                            csv(type.sessionType),
                            csv("${stat.module.attendanceThreshold}%"),
                            type.heldCount.toString(),
                            type.presentCount.toString(),
                            type.medicalCount.toString(),
                            type.absentCount.toString(),
                            csv(formatPct(type.percentage)),
                            csv(type.percentage?.let { if (it >= stat.module.attendanceThreshold) "MEETS THRESHOLD" else "BELOW THRESHOLD" } ?: "NO DATA")
                        ).joinToString(",")
                    )
                }
            }

            writer.appendLine()
            writer.appendLine("DETAILED ATTENDANCE LOG")
            writer.appendLine("Date,Module,Session Type,Session Kind,Start,End,Held Status,Attendance Status")
            details.forEach { detail ->
                val entry = detail.entry
                val slot = detail.slot
                val statusText = if (entry.heldStatus == "Held") entry.attendanceStatus else "Not Held"
                writer.appendLine(
                    listOf(
                        csv(entry.date), csv(detail.moduleName), csv(slot.sessionType),
                        csv(if (slot.isExtraSession) "Extra" else "Weekly"),
                        csv(slot.startTime), csv(slot.endTime), csv(entry.heldStatus), csv(statusText)
                    ).joinToString(",")
                )
            }
        }
    }

    fun exportToPDF(
        semesterName: String,
        details: List<AttendanceDetail>,
        moduleStats: List<ModuleAttendanceStats>,
        outputStream: OutputStream
    ) {
        val pdf = PdfDocument()
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(2, 136, 209); textSize = 18f; isFakeBoldText = true }
        val heading = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 11f; isFakeBoldText = true }
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 9f }
        val muted = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.GRAY; textSize = 8f }
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; strokeWidth = 1f }

        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var y = 0f

        fun startPage(section: String) {
            page?.let { pdf.finishPage(it) }
            pageNumber++
            page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
            val canvas = page!!.canvas
            canvas.drawText("Uni Leca Attendance Report", 36f, 42f, title)
            canvas.drawText("Semester: ${truncate(semesterName, 62)}", 36f, 61f, heading)
            canvas.drawText("Generated: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}", 36f, 76f, muted)
            canvas.drawLine(36f, 86f, 559f, 86f, line)
            canvas.drawText(section, 36f, 105f, heading)
            y = 124f
        }

        fun ensureSpace(height: Float, section: String) {
            if (page == null || y + height > 808f) startPage(section)
        }

        startPage("MODULE ATTENDANCE SUMMARY")
        moduleStats.forEach { stat ->
            val rows = 2 + stat.bySessionType.size
            ensureSpace(rows * 16f + 14f, "MODULE ATTENDANCE SUMMARY (continued)")
            val canvas = page!!.canvas
            canvas.drawText(truncate(stat.module.name, 42), 36f, y, heading)
            canvas.drawText("Overall ${formatPct(stat.percentage)} · threshold ${stat.module.attendanceThreshold.toInt()}%", 260f, y, text)
            y += 15f
            stat.bySessionType.forEach { type ->
                canvas.drawText("${truncate(type.sessionType, 16)}: ${formatPct(type.percentage)}", 52f, y, text)
                canvas.drawText("Held ${type.heldCount}  P ${type.presentCount}  M ${type.medicalCount}  A ${type.absentCount}", 210f, y, text)
                y += 14f
            }
            if (stat.bySessionType.isEmpty()) {
                canvas.drawText("No recorded attendance", 52f, y, muted)
                y += 14f
            }
            canvas.drawLine(36f, y, 559f, y, line)
            y += 12f
        }

        startPage("DETAILED ATTENDANCE LOG")
        details.forEach { detail ->
            ensureSpace(18f, "DETAILED ATTENDANCE LOG (continued)")
            val canvas = page!!.canvas
            val slot = detail.slot
            val status = if (detail.entry.heldStatus == "Held") detail.entry.attendanceStatus else "Not Held"
            canvas.drawText(detail.entry.date, 36f, y, text)
            canvas.drawText(truncate(detail.moduleName, 24), 105f, y, text)
            canvas.drawText(truncate(slot.sessionType, 12), 255f, y, text)
            canvas.drawText("${slot.startTime}-${slot.endTime}", 330f, y, text)
            canvas.drawText(if (slot.isExtraSession) "Extra" else "Weekly", 405f, y, muted)
            canvas.drawText(status, 465f, y, text)
            y += 16f
        }

        page?.let { pdf.finishPage(it) }
        pdf.writeTo(outputStream)
        pdf.close()
    }

    private fun formatPct(value: Float?): String =
        value?.let { String.format(Locale.US, "%.1f%%", it) } ?: "N/A"

    private fun truncate(value: String, max: Int): String =
        if (value.length <= max) value else value.take(max - 1) + "…"

    private fun csv(raw: String): String {
        var value = raw
        if (value.firstOrNull() in setOf('=', '+', '-', '@')) value = "'$value"
        return "\"${value.replace("\"", "\"\"")}\""
    }
}
