package com.example.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "semesters")
data class Semester(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val status: String,
    val createdDate: Long = System.currentTimeMillis(),
    val endedDate: Long? = null
) {
    val isActive: Boolean get() = status == SemesterStatus.ACTIVE.dbValue
}

enum class SemesterStatus(val dbValue: String) {
    ACTIVE("Active"),
    ENDED("Ended")
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
    val attendanceThreshold: Float,
    val archivedAt: Long? = null
) {
    val isArchived: Boolean get() = archivedAt != null
}

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
    indices = [
        Index(value = ["moduleId"]),
        Index(value = ["specificDate"])
    ]
)
data class TimetableSlot(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val moduleId: Int,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val sessionType: String,
    val isRecurring: Boolean = true,
    val specificDate: String? = null,
    val archivedAt: Long? = null
) {
    val isArchived: Boolean get() = archivedAt != null
    val isExtraSession: Boolean get() = !isRecurring
}

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
    indices = [
        Index(value = ["timetableSlotId"]),
        Index(value = ["timetableSlotId", "date"], unique = true)
    ]
)
data class AttendanceEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timetableSlotId: Int,
    val date: String,
    val heldStatus: String,
    val attendanceStatus: String
)

enum class HeldStatus(val dbValue: String) {
    HELD("Held"),
    NOT_HELD("Not Held")
}

enum class AttendanceStatus(val dbValue: String) {
    PRESENT("Present"),
    ABSENT("Absent"),
    MEDICAL("Medical")
}

enum class SessionType(val dbValue: String) {
    LECTURE("Lecture"),
    TUTORIAL("Tutorial"),
    LAB("LAB");

    companion object {
        fun normalize(value: String): String =
            entries.firstOrNull { it.dbValue.equals(value, ignoreCase = true) }?.dbValue
                ?: value.trim().ifBlank { LECTURE.dbValue }
    }
}

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

data class SessionTypeAttendanceStats(
    val sessionType: String,
    val heldCount: Int,
    val presentCount: Int,
    val medicalCount: Int,
    val absentCount: Int,
    val percentage: Float?
)

data class ModuleAttendanceStats(
    val module: Module,
    val heldCount: Int,
    val presentCount: Int,
    val medicalCount: Int,
    val absentCount: Int,
    val percentage: Float?,
    val meetsThreshold: Boolean?,
    val bySessionType: List<SessionTypeAttendanceStats> = emptyList()
) {
    val hasData: Boolean get() = percentage != null
}

data class AttendanceRisk(
    val currentPercentage: Float?,
    val percentageAfterOneMiss: Float?,
    val missesAllowedBeforeBelowThreshold: Int,
    val consecutiveAttendancesToRecover: Int
)
