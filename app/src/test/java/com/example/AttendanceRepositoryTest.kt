package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.AttendanceStatus
import com.example.data.model.HeldStatus
import com.example.data.repository.AttendanceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class AttendanceRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: AttendanceRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AttendanceRepository(db)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `lecture lab and tutorial percentages are calculated separately`() = runTest {
        val semId = repository.createSemester("S1").toInt()
        val moduleId = repository.insertModule(semId, "OS", 80f).toInt()
        val lecture = repository.insertRecurringSlot(moduleId, 1, "09:00", "10:00", "Lecture").toInt()
        val lab = repository.insertRecurringSlot(moduleId, 2, "10:00", "12:00", "LAB").toInt()
        val tutorial = repository.insertRecurringSlot(moduleId, 3, "13:00", "14:00", "Tutorial").toInt()

        repository.markAttendance(lecture, "2026-09-01", HeldStatus.HELD.dbValue, AttendanceStatus.PRESENT.dbValue)
        repository.markAttendance(lecture, "2026-09-08", HeldStatus.HELD.dbValue, AttendanceStatus.ABSENT.dbValue)
        repository.markAttendance(lab, "2026-09-02", HeldStatus.HELD.dbValue, AttendanceStatus.PRESENT.dbValue)
        repository.markAttendance(tutorial, "2026-09-03", HeldStatus.HELD.dbValue, AttendanceStatus.ABSENT.dbValue)

        val stat = repository.getModuleAttendanceStats(semId).first().single()
        assertEquals(50f, stat.bySessionType.first { it.sessionType == "Lecture" }.percentage!!, 0.01f)
        assertEquals(100f, stat.bySessionType.first { it.sessionType == "LAB" }.percentage!!, 0.01f)
        assertEquals(0f, stat.bySessionType.first { it.sessionType == "Tutorial" }.percentage!!, 0.01f)
    }

    @Test
    fun `zero held sessions has no percentage instead of fake 100 percent`() = runTest {
        val semId = repository.createSemester("S1").toInt()
        repository.insertModule(semId, "Math", 80f)
        val stat = repository.getModuleAttendanceStats(semId).first().single()
        assertNull(stat.percentage)
        assertNull(stat.meetsThreshold)
    }

    @Test
    fun `extra session appears only on its specific date`() = runTest {
        val semId = repository.createSemester("S1").toInt()
        val moduleId = repository.insertModule(semId, "OS", 80f).toInt()
        val date = LocalDate.of(2026, 9, 10)
        repository.insertExtraSession(moduleId, date.toString(), "15:00", "17:00", "Lecture")

        assertEquals(1, repository.getSessionsForDate(semId, date.toString()).first().size)
        assertTrue(repository.getSessionsForDate(semId, date.plusDays(1).toString()).first().isEmpty())
    }

    @Test
    fun `removing a slot with attendance archives it instead of deleting history`() = runTest {
        val semId = repository.createSemester("S1").toInt()
        val moduleId = repository.insertModule(semId, "OS", 80f).toInt()
        val slotId = repository.insertRecurringSlot(moduleId, 1, "09:00", "10:00", "Lecture").toInt()
        repository.markAttendance(slotId, "2026-09-01", HeldStatus.HELD.dbValue, AttendanceStatus.PRESENT.dbValue)
        val slot = db.timetableSlotDao().getSlotById(slotId)!!

        repository.deleteSlot(slot)

        assertNotNull(db.timetableSlotDao().getSlotById(slotId))
        assertNotNull(db.timetableSlotDao().getSlotById(slotId)?.archivedAt)
        assertEquals(1, repository.getAttendanceDetailsForSemester(semId).first().size)
        assertTrue(repository.getSlotsWithModuleForSemester(semId).first().isEmpty())
    }
}
