package com.example.data.repository

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AttendanceRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    val semesterDao = db.semesterDao()
    val moduleDao = db.moduleDao()
    val slotDao = db.timetableSlotDao()
    val attendanceDao = db.attendanceEntryDao()

    // Semesters
    val allSemesters: Flow<List<Semester>> = semesterDao.getAllSemesters()
    val activeSemester: Flow<Semester?> = semesterDao.getActiveSemesterFlow()

    suspend fun getActiveSemesterSync(): Semester? = semesterDao.getActiveSemester()

    suspend fun getSemesterById(id: Int): Semester? = semesterDao.getSemesterById(id)

    suspend fun insertSemester(name: String): Long {
        val semester = Semester(name = name, status = "Active")
        return semesterDao.insertSemester(semester)
    }

    suspend fun updateSemester(semester: Semester) {
        semesterDao.updateSemester(semester)
    }

    suspend fun deleteSemester(semester: Semester) {
        semesterDao.deleteSemester(semester)
    }

    suspend fun endSemester(id: Int) {
        val sem = semesterDao.getSemesterById(id)
        if (sem != null) {
            semesterDao.updateSemester(sem.copy(status = "Ended", endedDate = System.currentTimeMillis()))
        }
    }

    // Modules
    fun getModulesForSemester(semesterId: Int): Flow<List<Module>> =
        moduleDao.getModulesForSemester(semesterId)

    suspend fun insertModule(semesterId: Int, name: String, threshold: Float): Long {
        val module = Module(semesterId = semesterId, name = name, attendanceThreshold = threshold)
        return moduleDao.insertModule(module)
    }

    suspend fun deleteModule(module: Module) {
        moduleDao.deleteModule(module)
    }

    // Slots
    fun getSlotsWithModuleForSemester(semesterId: Int): Flow<List<TimetableSlotWithModule>> =
        slotDao.getSlotsWithModuleForSemester(semesterId)

    suspend fun insertSlot(moduleId: Int, dayOfWeek: Int, startTime: String, endTime: String, sessionType: String): Long {
        val slot = TimetableSlot(
            moduleId = moduleId,
            dayOfWeek = dayOfWeek,
            startTime = startTime,
            endTime = endTime,
            sessionType = sessionType
        )
        return slotDao.insertSlot(slot)
    }

    suspend fun deleteSlot(slot: TimetableSlot) {
        slotDao.deleteSlot(slot)
    }

    // Attendance
    fun getAttendanceDetailsForSemester(semesterId: Int): Flow<List<AttendanceDetail>> =
        attendanceDao.getAttendanceDetailsForSemester(semesterId)

    fun getAttendanceDetailsForDate(semesterId: Int, date: String): Flow<List<AttendanceDetail>> =
        attendanceDao.getAttendanceDetailsForDate(semesterId, date)

    suspend fun markAttendance(slotId: Int, date: String, heldStatus: String, attendanceStatus: String) {
        val existing = attendanceDao.getEntryForSlotAndDate(slotId, date)
        if (existing != null) {
            attendanceDao.updateAttendanceEntry(
                existing.copy(heldStatus = heldStatus, attendanceStatus = attendanceStatus)
            )
        } else {
            attendanceDao.insertAttendanceEntry(
                AttendanceEntry(
                    timetableSlotId = slotId,
                    date = date,
                    heldStatus = heldStatus,
                    attendanceStatus = attendanceStatus
                )
            )
        }
    }

    // Stats
    fun getModuleAttendanceStats(semesterId: Int): Flow<List<ModuleAttendanceStats>> {
        val modulesFlow = moduleDao.getModulesForSemester(semesterId)
        val entriesFlow = attendanceDao.getAttendanceDetailsForSemester(semesterId)

        return combine(modulesFlow, entriesFlow) { modules, entries ->
            val entriesByModule = entries.groupBy { it.moduleId }

            modules.map { module ->
                val moduleEntries = entriesByModule[module.id] ?: emptyList()
                val heldEntries = moduleEntries.filter { it.entry.heldStatus == "Held" }
                val heldCount = heldEntries.size
                val presentCount = heldEntries.count { it.entry.attendanceStatus == "Present" }
                val medicalCount = heldEntries.count { it.entry.attendanceStatus == "Medical" }
                val absentCount = heldEntries.count { it.entry.attendanceStatus == "Absent" }

                val percentage = if (heldCount > 0) {
                    ((presentCount + medicalCount).toFloat() / heldCount) * 100f
                } else {
                    100f // Default to 100% when no classes have been held
                }

                ModuleAttendanceStats(
                    module = module,
                    heldCount = heldCount,
                    presentCount = presentCount,
                    medicalCount = medicalCount,
                    absentCount = absentCount,
                    percentage = percentage,
                    meetsThreshold = percentage >= module.attendanceThreshold
                )
            }
        }
    }

    // Duplicate Semester
    suspend fun duplicateSemester(fromSemesterId: Int, toSemesterId: Int) = withContext(Dispatchers.IO) {
        val modules = moduleDao.getModulesForSemesterSync(fromSemesterId)
        val slots = slotDao.getSlotsWithModuleForSemesterSync(fromSemesterId)

        for (module in modules) {
            val newModuleId = moduleDao.insertModule(
                Module(
                    semesterId = toSemesterId,
                    name = module.name,
                    attendanceThreshold = module.attendanceThreshold
                )
            ).toInt()

            val moduleSlots = slots.filter { it.slot.moduleId == module.id }
            for (slotWithModule in moduleSlots) {
                val oldSlot = slotWithModule.slot
                slotDao.insertSlot(
                    TimetableSlot(
                        moduleId = newModuleId,
                        dayOfWeek = oldSlot.dayOfWeek,
                        startTime = oldSlot.startTime,
                        endTime = oldSlot.endTime,
                        sessionType = oldSlot.sessionType
                    )
                )
            }
        }
    }

    suspend fun getCurrentOrNextSession(): TimetableSlotWithModule? = withContext(Dispatchers.IO) {
        val active = getActiveSemesterSync() ?: return@withContext null
        val slots = slotDao.getSlotsWithModuleForSemesterSync(active.id)
        if (slots.isEmpty()) return@withContext null

        val now = java.util.Calendar.getInstance()
        val dayOfWeek = when (now.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.MONDAY -> 1
            java.util.Calendar.TUESDAY -> 2
            java.util.Calendar.WEDNESDAY -> 3
            java.util.Calendar.THURSDAY -> 4
            java.util.Calendar.FRIDAY -> 5
            java.util.Calendar.SATURDAY -> 6
            java.util.Calendar.SUNDAY -> 7
            else -> 1
        }

        val todaySlots = slots.filter { it.slot.dayOfWeek == dayOfWeek }
        if (todaySlots.isEmpty()) return@withContext null

        val currentHour = now.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(java.util.Calendar.MINUTE)
        val nowMinutes = currentHour * 60 + currentMinute

        val sortedSlots = todaySlots.sortedBy {
            val parts = it.slot.startTime.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            h * 60 + m
        }

        for (slotWithMod in sortedSlots) {
            val endParts = slotWithMod.slot.endTime.split(":")
            val endH = endParts.getOrNull(0)?.toIntOrNull() ?: 0
            val endM = endParts.getOrNull(1)?.toIntOrNull() ?: 0
            val endMinutes = endH * 60 + endM
            if (endMinutes > nowMinutes) {
                return@withContext slotWithMod
            }
        }

        return@withContext sortedSlots.firstOrNull()
    }
}
