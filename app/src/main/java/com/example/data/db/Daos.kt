package com.example.data.db

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SemesterDao {
    @Query("SELECT * FROM semesters ORDER BY createdDate DESC")
    fun getAllSemesters(): Flow<List<Semester>>

    @Query("SELECT * FROM semesters")
    suspend fun getAllSemestersDirect(): List<Semester>

    @Query("SELECT * FROM semesters WHERE status = 'Active' LIMIT 1")
    fun getActiveSemesterFlow(): Flow<Semester?>

    @Query("SELECT * FROM semesters WHERE status = 'Active' LIMIT 1")
    suspend fun getActiveSemester(): Semester?

    @Query("SELECT * FROM semesters WHERE id = :id")
    suspend fun getSemesterById(id: Int): Semester?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSemester(semester: Semester): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSemestersList(semesters: List<Semester>)

    @Update
    suspend fun updateSemester(semester: Semester)

    @Delete
    suspend fun deleteSemester(semester: Semester)
}

@Dao
interface ModuleDao {
    @Query("SELECT * FROM modules WHERE semesterId = :semesterId ORDER BY name ASC")
    fun getModulesForSemester(semesterId: Int): Flow<List<Module>>

    @Query("SELECT * FROM modules WHERE semesterId = :semesterId ORDER BY name ASC")
    suspend fun getModulesForSemesterSync(semesterId: Int): List<Module>

    @Query("SELECT * FROM modules")
    suspend fun getAllModulesDirect(): List<Module>

    @Query("SELECT * FROM modules WHERE id = :id")
    suspend fun getModuleById(id: Int): Module?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModule(module: Module): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModulesList(modules: List<Module>)

    @Update
    suspend fun updateModule(module: Module)

    @Delete
    suspend fun deleteModule(module: Module)
}

@Dao
interface TimetableSlotDao {
    @Query("""
        SELECT s.*, m.name as moduleName, m.attendanceThreshold as threshold
        FROM timetable_slots s
        INNER JOIN modules m ON s.moduleId = m.id
        WHERE m.semesterId = :semesterId
        ORDER BY s.dayOfWeek ASC, s.startTime ASC
    """)
    fun getSlotsWithModuleForSemester(semesterId: Int): Flow<List<TimetableSlotWithModule>>

    @Query("""
        SELECT s.*, m.name as moduleName, m.attendanceThreshold as threshold
        FROM timetable_slots s
        INNER JOIN modules m ON s.moduleId = m.id
        WHERE m.semesterId = :semesterId
    """)
    suspend fun getSlotsWithModuleForSemesterSync(semesterId: Int): List<TimetableSlotWithModule>

    @Query("SELECT * FROM timetable_slots")
    suspend fun getAllSlotsDirect(): List<TimetableSlot>

    @Query("SELECT * FROM timetable_slots WHERE id = :id")
    suspend fun getSlotById(id: Int): TimetableSlot?

    @Query("SELECT * FROM timetable_slots WHERE moduleId = :moduleId")
    suspend fun getSlotsForModule(moduleId: Int): List<TimetableSlot>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlot(slot: TimetableSlot): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlotsList(slots: List<TimetableSlot>)

    @Update
    suspend fun updateSlot(slot: TimetableSlot)

    @Delete
    suspend fun deleteSlot(slot: TimetableSlot)
}

@Dao
interface AttendanceEntryDao {
    @Query("""
        SELECT e.*, 
               s.id as slot_id, s.moduleId as slot_moduleId, s.dayOfWeek as slot_dayOfWeek, s.startTime as slot_startTime, s.endTime as slot_endTime, s.sessionType as slot_sessionType,
               m.name as moduleName, m.id as moduleId
        FROM attendance_entries e
        INNER JOIN timetable_slots s ON e.timetableSlotId = s.id
        INNER JOIN modules m ON s.moduleId = m.id
        WHERE m.semesterId = :semesterId
        ORDER BY e.date DESC, s.startTime DESC
    """)
    fun getAttendanceDetailsForSemester(semesterId: Int): Flow<List<AttendanceDetail>>

    @Query("""
        SELECT e.*, 
               s.id as slot_id, s.moduleId as slot_moduleId, s.dayOfWeek as slot_dayOfWeek, s.startTime as slot_startTime, s.endTime as slot_endTime, s.sessionType as slot_sessionType,
               m.name as moduleName, m.id as moduleId
        FROM attendance_entries e
        INNER JOIN timetable_slots s ON e.timetableSlotId = s.id
        INNER JOIN modules m ON s.moduleId = m.id
        WHERE m.semesterId = :semesterId AND e.date = :date
    """)
    fun getAttendanceDetailsForDate(semesterId: Int, date: String): Flow<List<AttendanceDetail>>

    @Query("SELECT * FROM attendance_entries")
    suspend fun getAllEntriesDirect(): List<AttendanceEntry>

    @Query("""
        SELECT * FROM attendance_entries 
        WHERE timetableSlotId = :slotId AND date = :date 
        LIMIT 1
    """)
    suspend fun getEntryForSlotAndDate(slotId: Int, date: String): AttendanceEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceEntry(entry: AttendanceEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntriesList(entries: List<AttendanceEntry>)

    @Update
    suspend fun updateAttendanceEntry(entry: AttendanceEntry)

    @Delete
    suspend fun deleteAttendanceEntry(entry: AttendanceEntry)

    @Query("DELETE FROM attendance_entries WHERE timetableSlotId IN (SELECT id FROM timetable_slots WHERE moduleId IN (SELECT id FROM modules WHERE semesterId = :semesterId))")
    suspend fun clearAllAttendanceForSemester(semesterId: Int)
}
