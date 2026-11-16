package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AttendanceEntry
import com.example.data.model.Module
import com.example.data.model.Semester
import com.example.data.model.TimetableSlot

@Database(
    entities = [Semester::class, Module::class, TimetableSlot::class, AttendanceEntry::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun semesterDao(): SemesterDao
    abstract fun moduleDao(): ModuleDao
    abstract fun timetableSlotDao(): TimetableSlotDao
    abstract fun attendanceEntryDao(): AttendanceEntryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE modules ADD COLUMN archivedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE timetable_slots ADD COLUMN isRecurring INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE timetable_slots ADD COLUMN specificDate TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE timetable_slots ADD COLUMN archivedAt INTEGER DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_timetable_slots_specificDate ON timetable_slots(specificDate)")

                // Older builds did not enforce one attendance row per slot/date. Keep the
                // newest row if duplicates exist before adding the unique invariant.
                db.execSQL(
                    """
                    DELETE FROM attendance_entries
                    WHERE id NOT IN (
                        SELECT MAX(id)
                        FROM attendance_entries
                        GROUP BY timetableSlotId, date
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_attendance_entries_timetableSlotId_date " +
                        "ON attendance_entries(timetableSlotId, date)"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "uni_leca_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
