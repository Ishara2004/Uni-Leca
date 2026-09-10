package com.example.data.db

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SemesterDao {
    @Query("SELECT * FROM semesters ORDER BY createdDate DESC")
    fun getAllSemesters(): Flow<List<Semester>>

    @Query("SELECT * FROM semesters ORDER BY createdDate DESC")
    suspend fun getAllSemestersDirect(): List<Semester>

    @Query("SELECT * FROM semesters WHERE status = 'Active' ORDER BY createdDate DESC LIMIT 1")
    fun getActiveSemesterFlow(): Flow<Semester?>

    @Query("SELECT * FROM semesters WHERE status = 'Active' ORDER BY createdDate DESC LIMIT 1")
    suspend fun getActiveSemester(): Semester?

    @Query("SELECT * FROM semesters WHERE id = :id")
    suspend fun getSemesterById(id: Int): Semester?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSemester(semester: Semester): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSemestersList(semesters: List<Semester>)

    @Update suspend fun updateSemester(semester: Semester)
    @Delete suspend fun deleteSemester(semester: Semester)

    @Query("UPDATE semesters SET status = 'Ended', endedDate = :endedAt WHERE status = 'Active'")
    suspend fun endAllActiveSemesters(endedAt: Long)
}

@Dao
interface ModuleDao {
    @Query("SELECT * FROM modules WHERE semesterId = :semesterId AND archivedAt IS NULL ORDER BY name ASC")
    fun getModulesForSemester(semesterId: Int): Flow<List<Module>>

    @Query("SELECT * FROM modules WHERE semesterId = :semesterId ORDER BY name ASC")
    fun getAllModulesForSemesterFlow(semesterId: Int): Flow<List<Module>>

    @Query("SELECT * FROM modules WHERE semesterId = :semesterId AND archivedAt IS NULL ORDER BY name ASC")
    suspend fun getModulesForSemesterSync(semesterId: Int): List<Module>

    @Query("SELECT * FROM modules WHERE semesterId = :semesterId ORDER BY name ASC")
    suspend fun getAllModulesForSemesterSync(semesterId: Int): List<Module>

    @Query("SELECT * FROM modules")
    suspend fun getAllModulesDirect(): List<Module>

    @Query("SELECT * FROM modules WHERE id = :id")
    suspend fun getModuleById(id: Int): Module?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertModule(module: Module): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModulesList(modules: List<Module>)

    @Update suspend fun updateModule(module: Module)
    @Delete suspend fun deleteModule(module: Module)

    @Query("UPDATE modules SET archivedAt = :archivedAt WHERE id = :moduleId")
    suspend fun archiveModule(moduleId: Int, archivedAt: Long)
}

@Dao
interface TimetableSlotDao {
    @Query("""
        SELECT s.*, m.name AS moduleName, m.attendanceThreshold AS threshold
        FROM timetable_slots s
        INNER JOIN modules m ON s.moduleId = m.id
        WHERE m.semesterId = :semesterId
          AND s.archivedAt IS NULL
          AND m.archivedAt IS NULL
        ORDER BY s.dayOfWeek ASC, s.startTime ASC
    """)
    fun getSlotsWithModuleForSemester(semesterId: Int): Flow<List<TimetableSlotWithModule>>

    @Query("""
        SELECT s.*, m.name AS moduleName, m.attendanceThreshold AS threshold
        FROM timetable_slots s
        INNER JOIN modules m ON s.moduleId = m.id
        WHERE m.semesterId = :semesterId
          AND s.archivedAt IS NULL
          AND m.archivedAt IS NULL
    """)
    suspend fun getSlotsWithModuleForSemesterSync(semesterId: Int): List<TimetableSlotWithModule>

    @Query("""
        SELECT s.*, m.name AS moduleName, m.attendanceThreshold AS threshold
        FROM timetable_slots s
        INNER JOIN modules m ON s.moduleId = m.id
        WHERE s.id = :id
        LIMIT 1
    """)
    suspend fun getSlotWithModuleById(id: Int): TimetableSlotWithModule?

    @Query("SELECT * FROM timetable_slots")
    suspend fun getAllSlotsDirect(): List<TimetableSlot>

    @Query("SELECT * FROM timetable_slots WHERE id = :id")
    suspend fun getSlotById(id: Int): TimetableSlot?

    @Query("SELECT * FROM timetable_slots WHERE moduleId = :moduleId AND archivedAt IS NULL")
    suspend fun getSlotsForModule(moduleId: Int): List<TimetableSlot>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSlot(slot: TimetableSlot): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlotsList(slots: List<TimetableSlot>)

    @Update suspend fun updateSlot(slot: TimetableSlot)
    @Delete suspend fun deleteSlot(slot: TimetableSlot)

    @Query("UPDATE timetable_slots SET archivedAt = :archivedAt WHERE id = :slotId")
    suspend fun archiveSlot(slotId: Int, archivedAt: Long)

    @Query("UPDATE timetable_slots SET archivedAt = :archivedAt WHERE moduleId = :moduleId AND archivedAt IS NULL")
    suspend fun archiveSlotsForModule(moduleId: Int, archivedAt: Long)
}

@Dao
interface AttendanceEntryDao {
    @Query("""
        SELECT e.*,
               s.id AS slot_id,
               s.moduleId AS slot_moduleId,
               s.dayOfWeek AS slot_dayOfWeek,
               s.startTime AS slot_startTime,
               s.endTime AS slot_endTime,
               s.sessionType AS slot_sessionType,
               s.isRecurring AS slot_isRecurring,
               s.specificDate AS slot_specificDate,
               s.archivedAt AS slot_archivedAt,
               m.name AS moduleName,
               m.id AS moduleId
        FROM attendance_entries e
        INNER JOIN timetable_slots s ON e.timetableSlotId = s.id
        INNER JOIN modules m ON s.moduleId = m.id
        WHERE m.semesterId = :semesterId
        ORDER BY e.date DESC, s.startTime DESC
    """)
    fun getAttendanceDetailsForSemester(semesterId: Int): Flow<List<AttendanceDetail>>

    @Query("""
        SELECT e.*,
               s.id AS slot_id,
               s.moduleId AS slot_moduleId,
               s.dayOfWeek AS slot_dayOfWeek,
               s.startTime AS slot_startTime,
               s.endTime AS slot_endTime,
               s.sessionType AS slot_sessionType,
               s.isRecurring AS slot_isRecurring,
               s.specificDate AS slot_specificDate,
               s.archivedAt AS slot_archivedAt,
               m.name AS moduleName,
               m.id AS moduleId
        FROM attendance_entries e
        INNER JOIN timetable_slots s ON e.timetableSlotId = s.id
        INNER JOIN modules m ON s.moduleId = m.id
        WHERE m.semesterId = :semesterId AND e.date = :date
        ORDER BY s.startTime ASC
    """)
    fun getAttendanceDetailsForDate(semesterId: Int, date: String): Flow<List<AttendanceDetail>>

    @Query("SELECT * FROM attendance_entries")
    suspend fun getAllEntriesDirect(): List<AttendanceEntry>

    @Query("SELECT * FROM attendance_entries WHERE timetableSlotId = :slotId AND date = :date LIMIT 1")
    suspend fun getEntryForSlotAndDate(slotId: Int, date: String): AttendanceEntry?

    @Upsert
    suspend fun upsertAttendanceEntry(entry: AttendanceEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntriesList(entries: List<AttendanceEntry>)

    @Delete suspend fun deleteAttendanceEntry(entry: AttendanceEntry)

    @Query("DELETE FROM attendance_entries WHERE timetableSlotId = :slotId AND date = :date")
    suspend fun deleteEntryForSlotAndDate(slotId: Int, date: String)

    @Query("SELECT COUNT(*) FROM attendance_entries WHERE timetableSlotId = :slotId")
    suspend fun countEntriesForSlot(slotId: Int): Int

    @Query("SELECT COUNT(*) FROM attendance_entries WHERE timetableSlotId IN (SELECT id FROM timetable_slots WHERE moduleId = :moduleId)")
    suspend fun countEntriesForModule(moduleId: Int): Int

    @Query("DELETE FROM attendance_entries WHERE timetableSlotId IN (SELECT id FROM timetable_slots WHERE moduleId IN (SELECT id FROM modules WHERE semesterId = :semesterId))")
    suspend fun clearAllAttendanceForSemester(semesterId: Int)
}
