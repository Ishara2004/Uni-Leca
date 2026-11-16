package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.UniLecaApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val container = UniLecaApplication.container(context)
                val active = container.repository.getActiveSemesterSync()
                if (active == null) {
                    container.alarmScheduler.cancelAllKnownAlarms()
                } else {
                    val slots = container.repository.getSlotsWithModuleForSemester(active.id).first()
                    container.alarmScheduler.scheduleAlarmsForSlots(slots)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
