package com.example.data.backup

import android.content.Context
import androidx.room.withTransaction
import com.example.data.db.AppDatabase
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

class BackupRestoreHelper(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(BackupData::class.java)

    suspend fun exportData(outputStream: OutputStream): Boolean = withContext(Dispatchers.IO) {
        try {
            val semesters = db.semesterDao().getAllSemestersDirect()
            val modules = db.moduleDao().getAllModulesDirect()
            val slots = db.timetableSlotDao().getAllSlotsDirect()
            val entries = db.attendanceEntryDao().getAllEntriesDirect()

            val backup = BackupData(
                semesters = semesters,
                modules = modules,
                slots = slots,
                entries = entries
            )

            val json = adapter.toJson(backup)
            outputStream.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importData(inputStream: InputStream): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = inputStream.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            }
            val backup = adapter.fromJson(json) ?: return@withContext false

            // Use transaction to ensure atomic restore
            db.withTransaction {
                // Clear everything
                db.clearAllTables()

                // Insert semesters
                if (backup.semesters.isNotEmpty()) {
                    db.semesterDao().insertSemestersList(backup.semesters)
                }
                // Insert modules
                if (backup.modules.isNotEmpty()) {
                    db.moduleDao().insertModulesList(backup.modules)
                }
                // Insert slots
                if (backup.slots.isNotEmpty()) {
                    db.timetableSlotDao().insertSlotsList(backup.slots)
                }
                // Insert entries
                if (backup.entries.isNotEmpty()) {
                    db.attendanceEntryDao().insertEntriesList(backup.entries)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
