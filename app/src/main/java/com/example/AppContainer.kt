package com.example

import android.content.Context
import com.example.data.backup.BackupRestoreHelper
import com.example.data.db.AppDatabase
import com.example.data.repository.AttendanceRepository
import com.example.notification.AlarmScheduler
import com.example.ui.settings.SettingsManager

class AppContainer(context: Context) {
    val database: AppDatabase = AppDatabase.getDatabase(context)
    val repository: AttendanceRepository = AttendanceRepository(database)
    val settings: SettingsManager = SettingsManager(context)
    val alarmScheduler: AlarmScheduler = AlarmScheduler(context)
    val backupRestoreHelper: BackupRestoreHelper = BackupRestoreHelper(database)
}
