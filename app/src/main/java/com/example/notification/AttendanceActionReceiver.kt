package com.example.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.UniLecaApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AttendanceActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val slotId = intent.getIntExtra(AlarmScheduler.EXTRA_SLOT_ID, -1)
        val date = intent.getStringExtra(AlarmScheduler.EXTRA_DATE).orEmpty()
        val status = intent.getStringExtra(EXTRA_STATUS).orEmpty()
        val held = intent.getStringExtra(EXTRA_HELD).orEmpty()
        if (slotId < 0 || date.isBlank() || status.isBlank() || held.isBlank()) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                UniLecaApplication.container(context).repository.markAttendance(slotId, date, held, status)
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(slotId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_STATUS = "attendanceStatus"
        const val EXTRA_HELD = "heldStatus"
    }
}
