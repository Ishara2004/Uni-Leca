package com.example.data.backup

import com.example.data.model.AttendanceEntry
import com.example.data.model.Module
import com.example.data.model.Semester
import com.example.data.model.TimetableSlot
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BackupPayload(
    val semesters: List<Semester>,
    val modules: List<Module>,
    val slots: List<TimetableSlot>,
    val entries: List<AttendanceEntry>
)

@JsonClass(generateAdapter = true)
data class BackupEnvelope(
    val formatVersion: Int,
    val appVersion: String,
    val createdAt: Long,
    val payload: BackupPayload,
    val checksumSha256: String
)

data class BackupPreview(
    val formatVersion: Int,
    val createdAt: Long?,
    val semesterCount: Int,
    val moduleCount: Int,
    val attendanceCount: Int,
    val isLegacy: Boolean
)
