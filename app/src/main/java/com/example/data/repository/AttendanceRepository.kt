package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.db.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

class AttendanceRepository(private val db: AppDatabase) {
    val semesterDao = db.semesterDao()
    val moduleDao = db.moduleDao()
    val slotDao = db.timetableSlotDao()
    val attendanceDao = db.attendanceEntryDao()

    val allSemesters: Flow<List<Semester>> = semesterDao.getAllSemesters()
    val activeSemester: Flow<Semester?> = semesterDao.getActiveSemesterFlow()

    suspend fun getActiveSemesterSync(): Semester? = semesterDao.getActiveSemester()
    suspend fun getSemesterById(id: Int): Semester? = semesterDao.getSemesterById(id)

    suspend fun createSemester(name: String, duplicateFromId: Int? = null): Long = db.withTransaction {
        val now = System.currentTimeMillis()
        semesterDao.endAllActiveSemesters(now)
        val newId = semesterDao.insertSemester(Semester(name = name.trim(), status = SemesterStatus.ACTIVE.dbValue)).toInt()
        if (duplicateFromId != null) duplicateSemesterInternal(duplicateFromId, newId)
        newId.toLong()
    }

    suspend fun updateSemester(semester: Semester) = semesterDao.updateSemester(semester)
    suspend fun deleteSemester(semesterId: Int) = db.withTransaction {
        semesterDao.getSemesterById(semesterId)?.let { semesterDao.deleteSemester(it) }
    }

    suspend fun endSemester(id: Int) {
        semesterDao.getSemesterById(id)?.let {
            semesterDao.updateSemester(it.copy(status = SemesterStatus.ENDED.dbValue, endedDate = System.currentTimeMillis()))
        }
    }

    fun getModulesForSemester(semesterId: Int): Flow<List<Module>> = moduleDao.getModulesForSemester(semesterId)

    suspend fun insertModule(semesterId: Int, name: String, threshold: Float): Long =
        moduleDao.insertModule(Module(semesterId = semesterId, name = name.trim(), attendanceThreshold = threshold.coerceIn(0f, 100f)))

    suspend fun updateModule(module: Module) = moduleDao.updateModule(module)

    suspend fun deleteModule(module: Module) = db.withTransaction {
        val historyCount = attendanceDao.countEntriesForModule(module.id)
        if (historyCount == 0) moduleDao.deleteModule(module)
        else {
            val now = System.currentTimeMillis()
            moduleDao.archiveModule(module.id, now)
            slotDao.archiveSlotsForModule(module.id, now)
        }
    }

    fun getSlotsWithModuleForSemester(semesterId: Int): Flow<List<TimetableSlotWithModule>> =
        slotDao.getSlotsWithModuleForSemester(semesterId)

    fun getSessionsForDate(semesterId: Int, date: String): Flow<List<TimetableSlotWithModule>> =
        slotDao.getSlotsWithModuleForSemester(semesterId).map { slots ->
            val localDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return@map emptyList()
            val day = localDate.dayOfWeek.value
            slots.filter { item ->
                val slot = item.slot
                (slot.isRecurring && slot.dayOfWeek == day) || (!slot.isRecurring && slot.specificDate == date)
            }.sortedBy { toMinutes(it.slot.startTime) }
        }

    suspend fun insertRecurringSlot(moduleId: Int, dayOfWeek: Int, startTime: String, endTime: String, sessionType: String): Long {
        requireValidTimeRange(startTime, endTime)
        return slotDao.insertSlot(
            TimetableSlot(
                moduleId = moduleId,
                dayOfWeek = dayOfWeek.coerceIn(1, 7),
                startTime = normalizeTime(startTime),
                endTime = normalizeTime(endTime),
                sessionType = SessionType.normalize(sessionType),
                isRecurring = true,
                specificDate = null
            )
        )
    }

    suspend fun insertExtraSession(moduleId: Int, date: String, startTime: String, endTime: String, sessionType: String): Long {
        val parsedDate = LocalDate.parse(date)
        requireValidTimeRange(startTime, endTime)
        return slotDao.insertSlot(
            TimetableSlot(
                moduleId = moduleId,
                dayOfWeek = parsedDate.dayOfWeek.value,
                startTime = normalizeTime(startTime),
                endTime = normalizeTime(endTime),
                sessionType = SessionType.normalize(sessionType),
                isRecurring = false,
                specificDate = parsedDate.toString()
            )
        )
    }

    suspend fun updateSlot(slot: TimetableSlot) {
        requireValidTimeRange(slot.startTime, slot.endTime)
        slotDao.updateSlot(slot.copy(startTime = normalizeTime(slot.startTime), endTime = normalizeTime(slot.endTime), sessionType = SessionType.normalize(slot.sessionType)))
    }

    suspend fun deleteSlot(slot: TimetableSlot) {
        val historyCount = attendanceDao.countEntriesForSlot(slot.id)
        if (historyCount == 0) slotDao.deleteSlot(slot) else slotDao.archiveSlot(slot.id, System.currentTimeMillis())
    }

    suspend fun getSlotWithModule(slotId: Int): TimetableSlotWithModule? = slotDao.getSlotWithModuleById(slotId)
    fun getAttendanceDetailsForSemester(semesterId: Int): Flow<List<AttendanceDetail>> = attendanceDao.getAttendanceDetailsForSemester(semesterId)
    fun getAttendanceDetailsForDate(semesterId: Int, date: String): Flow<List<AttendanceDetail>> = attendanceDao.getAttendanceDetailsForDate(semesterId, date)

    suspend fun markAttendance(slotId: Int, date: String, heldStatus: String, attendanceStatus: String) {
        val slot = slotDao.getSlotById(slotId) ?: return
        if (slot.archivedAt != null) return
        if (!slot.isRecurring && slot.specificDate != date) return
        val existing = attendanceDao.getEntryForSlotAndDate(slotId, date)
        attendanceDao.upsertAttendanceEntry(
            AttendanceEntry(
                id = existing?.id ?: 0,
                timetableSlotId = slotId,
                date = date,
                heldStatus = heldStatus,
                attendanceStatus = attendanceStatus
            )
        )
    }

    suspend fun clearAttendance(slotId: Int, date: String) = attendanceDao.deleteEntryForSlotAndDate(slotId, date)

    fun getModuleAttendanceStats(semesterId: Int, medicalCountsAsAttended: Boolean = true): Flow<List<ModuleAttendanceStats>> {
        val modulesFlow = moduleDao.getAllModulesForSemesterFlow(semesterId)
        val entriesFlow = attendanceDao.getAttendanceDetailsForSemester(semesterId)
        return combine(modulesFlow, entriesFlow) { modules, entries ->
            val entriesByModule = entries.groupBy { it.moduleId }
            modules.mapNotNull { module ->
                val moduleEntries = entriesByModule[module.id].orEmpty()
                if (module.isArchived && moduleEntries.isEmpty()) return@mapNotNull null
                buildModuleStats(module, moduleEntries, medicalCountsAsAttended)
            }
        }
    }

    fun calculateRisk(stat: ModuleAttendanceStats, medicalCountsAsAttended: Boolean = true): AttendanceRisk {
        val held = if (medicalCountsAsAttended) stat.heldCount else (stat.heldCount - stat.medicalCount).coerceAtLeast(0)
        val attended = stat.presentCount + if (medicalCountsAsAttended) stat.medicalCount else 0
        val threshold = stat.module.attendanceThreshold
        if (held == 0) return AttendanceRisk(null, null, 0, 0)

        val afterMiss = attended.toFloat() / (held + 1) * 100f
        var allowedMisses = 0
        while (allowedMisses < 10_000) {
            val pct = attended.toFloat() / (held + allowedMisses + 1) * 100f
            if (pct < threshold) break
            allowedMisses++
        }

        var recover = 0
        if ((attended.toFloat() / held * 100f) < threshold) {
            while (recover < 10_000) {
                recover++
                val pct = (attended + recover).toFloat() / (held + recover) * 100f
                if (pct >= threshold) break
            }
        }
        return AttendanceRisk(stat.percentage, afterMiss, allowedMisses, recover)
    }

    suspend fun duplicateSemester(fromSemesterId: Int, toSemesterId: Int) = db.withTransaction { duplicateSemesterInternal(fromSemesterId, toSemesterId) }

    private suspend fun duplicateSemesterInternal(fromSemesterId: Int, toSemesterId: Int) {
        val modules = moduleDao.getModulesForSemesterSync(fromSemesterId)
        val slots = slotDao.getSlotsWithModuleForSemesterSync(fromSemesterId).filter { it.slot.isRecurring }
        for (module in modules) {
            val newModuleId = moduleDao.insertModule(Module(semesterId = toSemesterId, name = module.name, attendanceThreshold = module.attendanceThreshold)).toInt()
            slots.filter { it.slot.moduleId == module.id }.forEach { item ->
                val old = item.slot
                slotDao.insertSlot(TimetableSlot(moduleId = newModuleId, dayOfWeek = old.dayOfWeek, startTime = old.startTime, endTime = old.endTime, sessionType = old.sessionType, isRecurring = true))
            }
        }
    }

    suspend fun getCurrentOrNextSession(): TimetableSlotWithModule? = withContext(Dispatchers.IO) {
        val active = getActiveSemesterSync() ?: return@withContext null
        val today = LocalDate.now()
        val now = LocalTime.now()
        slotDao.getSlotsWithModuleForSemesterSync(active.id)
            .filter { item ->
                val s = item.slot
                (s.isRecurring && s.dayOfWeek == today.dayOfWeek.value) || (!s.isRecurring && s.specificDate == today.toString())
            }
            .sortedBy { toMinutes(it.slot.startTime) }
            .firstOrNull { item -> runCatching { LocalTime.parse(item.slot.endTime) }.getOrNull()?.isAfter(now) == true }
    }

    private fun buildModuleStats(module: Module, details: List<AttendanceDetail>, medicalCountsAsAttended: Boolean): ModuleAttendanceStats {
        val heldEntries = details.filter { it.entry.heldStatus == HeldStatus.HELD.dbValue }
        val present = heldEntries.count { it.entry.attendanceStatus == AttendanceStatus.PRESENT.dbValue }
        val medical = heldEntries.count { it.entry.attendanceStatus == AttendanceStatus.MEDICAL.dbValue }
        val absent = heldEntries.count { it.entry.attendanceStatus == AttendanceStatus.ABSENT.dbValue }
        val percentage = percentageFor(present, medical, absent, medicalCountsAsAttended)

        val typeStats = heldEntries.groupBy { SessionType.normalize(it.slot.sessionType) }.map { (type, typeEntries) ->
            val typePresent = typeEntries.count { it.entry.attendanceStatus == AttendanceStatus.PRESENT.dbValue }
            val typeMedical = typeEntries.count { it.entry.attendanceStatus == AttendanceStatus.MEDICAL.dbValue }
            val typeAbsent = typeEntries.count { it.entry.attendanceStatus == AttendanceStatus.ABSENT.dbValue }
            SessionTypeAttendanceStats(
                sessionType = type,
                heldCount = typeEntries.size,
                presentCount = typePresent,
                medicalCount = typeMedical,
                absentCount = typeAbsent,
                percentage = percentageFor(typePresent, typeMedical, typeAbsent, medicalCountsAsAttended)
            )
        }.sortedBy { it.sessionType }

        return ModuleAttendanceStats(
            module = module,
            heldCount = heldEntries.size,
            presentCount = present,
            medicalCount = medical,
            absentCount = absent,
            percentage = percentage,
            meetsThreshold = percentage?.let { it >= module.attendanceThreshold },
            bySessionType = typeStats
        )
    }

    private fun percentageFor(present: Int, medical: Int, absent: Int, medicalCountsAsAttended: Boolean): Float? {
        val denominator = present + absent + if (medicalCountsAsAttended) medical else 0
        if (denominator == 0) return null
        val attended = present + if (medicalCountsAsAttended) medical else 0
        return attended.toFloat() / denominator * 100f
    }

    private fun requireValidTimeRange(start: String, end: String) {
        val startTime = LocalTime.parse(normalizeTime(start))
        val endTime = LocalTime.parse(normalizeTime(end))
        require(endTime.isAfter(startTime)) { "End time must be after start time" }
    }

    private fun normalizeTime(value: String): String {
        val parts = value.trim().split(":")
        require(parts.size == 2) { "Time must use HH:mm" }
        val h = parts[0].toInt()
        val m = parts[1].toInt()
        require(h in 0..23 && m in 0..59) { "Invalid time" }
        return "%02d:%02d".format(h, m)
    }

    private fun toMinutes(value: String): Int = runCatching {
        val parts = value.split(":")
        parts[0].toInt() * 60 + parts[1].toInt()
    }.getOrDefault(0)
}
