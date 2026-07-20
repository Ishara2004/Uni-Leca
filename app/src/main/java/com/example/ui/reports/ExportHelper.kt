package com.example.ui.reports

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.data.model.AttendanceDetail
import com.example.data.model.ModuleAttendanceStats
import java.io.OutputStream

object ExportHelper {

    fun exportToCSV(
        details: List<AttendanceDetail>,
        moduleStats: List<ModuleAttendanceStats>,
        outputStream: OutputStream
    ) {
        val writer = outputStream.bufferedWriter()
        
        // Header
        writer.write("MODULE ATTENDANCE SUMMARY\n")
        writer.write("Module,Attendance Threshold,Classes Held,Present,Medical,Absent,Attendance %,Status\n")
        for (stat in moduleStats) {
            val status = if (stat.percentage >= stat.module.attendanceThreshold) "MEETS THRESHOLD" else "BELOW THRESHOLD"
            writer.write("\"${stat.module.name}\",${stat.module.attendanceThreshold}%,${stat.heldCount},${stat.presentCount},${stat.medicalCount},${stat.absentCount},${String.format("%.1f", stat.percentage)}%,$status\n")
        }
        writer.write("\n\n")

        // Detailed log
        writer.write("DETAILED ATTENDANCE LOG\n")
        writer.write("Date,Module,Session Type,Held Status,Attendance Status\n")
        for (detail in details) {
            val entry = detail.entry
            val slot = detail.slot
            val statusText = if (entry.heldStatus == "Held") entry.attendanceStatus else "Not Held"
            writer.write("${entry.date},\"${detail.moduleName}\",\"${slot.sessionType}\",${entry.heldStatus},${statusText}\n")
        }
        writer.flush()
    }

    fun exportToPDF(
        semesterName: String,
        details: List<AttendanceDetail>,
        moduleStats: List<ModuleAttendanceStats>,
        outputStream: OutputStream
    ) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            isAntiAlias = true
        }
        
        val titlePaint = Paint().apply {
            color = Color.parseColor("#0288D1") // Primary Blue
            textSize = 18f
            isFakeBoldText = true
            isAntiAlias = true
        }
        
        val subtitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 12f
            isAntiAlias = true
        }
        
        val headerPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f
            isFakeBoldText = true
            isAntiAlias = true
        }

        // PAGE 1: Summary Sheet
        val pageInfo1 = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page1 = pdfDocument.startPage(pageInfo1)
        var canvas = page1.canvas

        // Header section
        canvas.drawText("Uni Leca Attendance Report", 40f, 50f, titlePaint)
        canvas.drawText("Semester: $semesterName", 40f, 70f, subtitlePaint)
        
        // Divider
        paint.color = Color.parseColor("#B0BEC5")
        canvas.drawLine(40f, 85f, 555f, 85f, paint)

        canvas.drawText("MODULE ATTENDANCE SUMMARY", 40f, 110f, headerPaint)

        var y = 135f
        canvas.drawText("Module", 40f, y, headerPaint)
        canvas.drawText("Threshold", 220f, y, headerPaint)
        canvas.drawText("Held", 300f, y, headerPaint)
        canvas.drawText("Attended", 360f, y, headerPaint)
        canvas.drawText("Percentage", 440f, y, headerPaint)

        canvas.drawLine(40f, y + 5, 555f, y + 5, paint)
        y += 22f

        for (stat in moduleStats) {
            canvas.drawText(stat.module.name, 40f, y, textPaint)
            canvas.drawText("${stat.module.attendanceThreshold}%", 220f, y, textPaint)
            canvas.drawText("${stat.heldCount}", 300f, y, textPaint)
            canvas.drawText("${stat.presentCount + stat.medicalCount}", 360f, y, textPaint)

            val meets = stat.percentage >= stat.module.attendanceThreshold
            val pctColor = if (meets) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
            val pctPaint = Paint(textPaint).apply {
                color = pctColor
                isFakeBoldText = true
            }
            canvas.drawText("${String.format("%.1f", stat.percentage)}%", 440f, y, pctPaint)
            y += 20f
        }

        pdfDocument.finishPage(page1)

        // PAGES 2+: Detailed Logs
        val pageSize = 32
        val chunks = details.chunked(pageSize)
        
        var pageNumber = 2
        for (chunk in chunks) {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas

            canvas.drawText("ATTENDANCE LOG DETAIL (Page ${pageNumber - 1})", 40f, 50f, headerPaint)
            canvas.drawLine(40f, 60f, 555f, 60f, paint)

            var logY = 85f
            canvas.drawText("Date", 40f, logY, headerPaint)
            canvas.drawText("Module", 130f, logY, headerPaint)
            canvas.drawText("Type", 280f, logY, headerPaint)
            canvas.drawText("Held", 380f, logY, headerPaint)
            canvas.drawText("Status", 440f, logY, headerPaint)

            canvas.drawLine(40f, logY + 5, 555f, logY + 5, paint)
            logY += 22f

            for (detail in chunk) {
                val entry = detail.entry
                val slot = detail.slot
                val statusText = if (entry.heldStatus == "Held") entry.attendanceStatus else "Not Held"

                canvas.drawText(entry.date, 40f, logY, textPaint)
                canvas.drawText(detail.moduleName, 130f, logY, textPaint)
                canvas.drawText(slot.sessionType, 280f, logY, textPaint)
                canvas.drawText(entry.heldStatus, 380f, logY, textPaint)

                val statusColor = when (statusText) {
                    "Present" -> Color.parseColor("#2E7D32")
                    "Medical" -> Color.parseColor("#1565C0")
                    "Absent" -> Color.parseColor("#C62828")
                    else -> Color.GRAY
                }
                val statusPaint = Paint(textPaint).apply {
                    color = statusColor
                    isFakeBoldText = statusText != "Not Held"
                }
                canvas.drawText(statusText, 440f, logY, statusPaint)
                logY += 18f
            }

            pdfDocument.finishPage(page)
            pageNumber++
        }

        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
    }
}
