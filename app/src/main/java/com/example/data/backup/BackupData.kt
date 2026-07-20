package com.example.data.backup

import com.example.data.model.*
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BackupData(
    val semesters: List<Semester>,
    val modules: List<Module>,
    val slots: List<TimetableSlot>,
    val entries: List<AttendanceEntry>
)
