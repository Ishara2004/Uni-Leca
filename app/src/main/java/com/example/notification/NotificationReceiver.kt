package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.UniLecaApplication
import com.example.data.model.AttendanceStatus
import com.example.data.model.HeldStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "uni_leca_reminders"
        const val CHANNEL_NAME = "Attendance Reminders"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val container = UniLecaApplication.container(context)
        if (!container.settings.notificationsEnabled) return

        val slotId = intent.getIntExtra(AlarmScheduler.EXTRA_SLOT_ID, -1)
        if (slotId == -1) return
        val moduleName = intent.getStringExtra(AlarmScheduler.EXTRA_MODULE_NAME) ?: "Module"
        val sessionType = intent.getStringExtra(AlarmScheduler.EXTRA_SESSION_TYPE) ?: "Class"
        val date = intent.getStringExtra(AlarmScheduler.EXTRA_DATE).orEmpty()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Reminders to record attendance after scheduled classes"
                }
            )
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(AlarmScheduler.EXTRA_SLOT_ID, slotId)
            putExtra(AlarmScheduler.EXTRA_DATE, date)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            slotId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        fun action(label: String, status: String, held: String, offset: Int): NotificationCompat.Action {
            val actionIntent = Intent(context, AttendanceActionReceiver::class.java).apply {
                putExtra(AlarmScheduler.EXTRA_SLOT_ID, slotId)
                putExtra(AlarmScheduler.EXTRA_DATE, date)
                putExtra(AttendanceActionReceiver.EXTRA_STATUS, status)
                putExtra(AttendanceActionReceiver.EXTRA_HELD, held)
            }
            val pending = PendingIntent.getBroadcast(
                context,
                slotId * 10 + offset,
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            return NotificationCompat.Action.Builder(0, label, pending).build()
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.ishara.unileca.R.drawable.ic_notification)
            .setContentTitle("$moduleName · $sessionType")
            .setContentText("How was your attendance?")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .addAction(action("Present", AttendanceStatus.PRESENT.dbValue, HeldStatus.HELD.dbValue, 1))
            .addAction(action("Absent", AttendanceStatus.ABSENT.dbValue, HeldStatus.HELD.dbValue, 2))
            .addAction(action("Medical", AttendanceStatus.MEDICAL.dbValue, HeldStatus.HELD.dbValue, 3))
            .build()

        notificationManager.notify(slotId, notification)

        // Re-arm only recurring timetable slots. One-off extra sessions intentionally fire once.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val item = container.repository.getSlotWithModule(slotId)
                if (item?.slot?.isRecurring == true && item.slot.archivedAt == null) {
                    container.alarmScheduler.scheduleAlarmForSlot(item)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
