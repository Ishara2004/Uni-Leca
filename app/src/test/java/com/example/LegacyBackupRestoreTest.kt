package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupRestoreHelper
import com.example.data.db.AppDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LegacyBackupRestoreTest {
    private lateinit var db: AppDatabase
    private lateinit var helper: BackupRestoreHelper

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        helper = BackupRestoreHelper(db)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `v1 plain json imports into v2 without new fields`() = runTest {
        val legacyJson = """
            {
              "semesters":[{"id":1,"name":"Semester 3","status":"Active","createdDate":1784555654720}],
              "modules":[{"id":1,"semesterId":1,"name":"OS","attendanceThreshold":80.0}],
              "slots":[{"id":1,"moduleId":1,"dayOfWeek":1,"startTime":"08:15","endTime":"10:15","sessionType":"Lecture"}],
              "entries":[{"id":1,"timetableSlotId":1,"date":"2026-09-07","heldStatus":"Held","attendanceStatus":"Present"}]
            }
        """.trimIndent()

        val preview = helper.inspectBackup(ByteArrayInputStream(legacyJson.toByteArray()))
        assertNotNull(preview)
        assertTrue(preview!!.isLegacy)
        assertEquals(1, preview.semesterCount)
        assertEquals(1, preview.moduleCount)
        assertEquals(1, preview.attendanceCount)

        assertTrue(helper.importData(ByteArrayInputStream(legacyJson.toByteArray())))

        assertEquals(1, db.semesterDao().getAllSemestersDirect().size)
        assertEquals(1, db.moduleDao().getAllModulesDirect().size)
        assertEquals(1, db.timetableSlotDao().getAllSlotsDirect().size)
        assertEquals(1, db.attendanceEntryDao().getAllEntriesDirect().size)

        val restoredSlot = db.timetableSlotDao().getAllSlotsDirect().single()
        assertTrue(restoredSlot.isRecurring)
        assertNull(restoredSlot.specificDate)
        assertNull(restoredSlot.archivedAt)
    }
}
