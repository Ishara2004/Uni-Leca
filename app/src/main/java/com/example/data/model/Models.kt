package com.example.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "semesters")
data class Semester(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val status: String, // "Active", "Ended"
    val createdDate: Long = System.currentTimeMillis(),
    val endedDate: Long? = null
) {
    val isActive: Boolean get() = status == "Active"
}

@Entity(
    tableName = "modules",
    foreignKeys = [
        ForeignKey(
            entity = Semester::class,
            parentColumns = ["id"],
            childColumns = ["semesterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["semesterId"])]
)
data class Module(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val semesterId: Int,
    val name: String,
    val attendanceThreshold: Float // e.g. 80.0f
)

@Entity(
    tableName = "timetable_slots",
    foreignKeys = [
        ForeignKey(
            entity = Module::class,
            parentColumns = ["id"],
            childColumns = ["moduleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["moduleId"])]
)
data class TimetableSlot(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val moduleId: Int,
    val dayOfWeek: Int, // 1 = Monday, 7 = Sunday (matches java.time.DayOfWeek)
    val startTime: String, // "HH:MM"
    val endTime: String, // "HH:MM"
    val sessionType: String // "Lecture", "Tutorial", "LAB"
)

@Entity(
    tableName = "attendance_entries",
    foreignKeys = [
        ForeignKey(
            entity = TimetableSlot::class,
            parentColumns = ["id"],
            childColumns = ["timetableSlotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["timetableSlotId"])]
)
data class AttendanceEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timetableSlotId: Int,
    val date: String, // "YYYY-MM-DD"
    val heldStatus: String, // "Held", "Not Held"
    val attendanceStatus: String // "Present", "Absent", "Medical"
)

// UI and business projections
data class TimetableSlotWithModule(
    @Embedded val slot: TimetableSlot,
    val moduleName: String,
    val threshold: Float
)

data class AttendanceDetail(
    @Embedded val entry: AttendanceEntry,
    @Embedded(prefix = "slot_") val slot: TimetableSlot,
    val moduleName: String,
    val moduleId: Int
)

data class ModuleAttendanceStats(
    val module: Module,
    val heldCount: Int,
    val presentCount: Int,
    val medicalCount: Int,
    val absentCount: Int,
    val percentage: Float, // Calculated as (Present + Medical) / Held * 100
    val meetsThreshold: Boolean
)
