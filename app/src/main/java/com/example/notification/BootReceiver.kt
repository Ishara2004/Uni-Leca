package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.repository.AttendanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val repository = AttendanceRepository(context)
            val alarmScheduler = AlarmScheduler(context)

            CoroutineScope(Dispatchers.IO).launch {
                val activeSemester = repository.getActiveSemesterSync()
                if (activeSemester != null) {
                    val slots = repository.getSlotsWithModuleForSemester(activeSemester.id).first()
                    alarmScheduler.scheduleAlarmsForSlots(slots)
                }
            }
        }
    }
}
