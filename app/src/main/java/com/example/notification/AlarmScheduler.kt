package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.TimetableSlotWithModule
import java.util.Calendar

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarmsForSlots(slots: List<TimetableSlotWithModule>) {
        cancelAllAlarms(slots)
        for (slotWithModule in slots) {
            scheduleAlarmForSlot(slotWithModule)
        }
    }

    fun scheduleAlarmForSlot(slotWithModule: TimetableSlotWithModule) {
        val slot = slotWithModule.slot
        val parts = slot.endTime.split(":")
        if (parts.size < 2) return
        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        val calendar = Calendar.getInstance()
        val now = Calendar.getInstance()

        val calendarDayMap = mapOf(
            1 to Calendar.MONDAY,
            2 to Calendar.TUESDAY,
            3 to Calendar.WEDNESDAY,
            4 to Calendar.THURSDAY,
            5 to Calendar.FRIDAY,
            6 to Calendar.SATURDAY,
            7 to Calendar.SUNDAY
        )
        val targetDay = calendarDayMap[slot.dayOfWeek] ?: Calendar.MONDAY

        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.set(Calendar.DAY_OF_WEEK, targetDay)

        if (calendar.before(now)) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
        }

        // Capture session date before adding 30 mins
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val sessionDate = String.format("%04d-%02d-%02d", year, month, day)

        // Schedule alarm 30 minutes after end time
        calendar.add(Calendar.MINUTE, 30)
        val triggerTime = calendar.timeInMillis

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("slotId", slot.id)
            putExtra("moduleName", slotWithModule.moduleName)
            putExtra("sessionType", slot.sessionType)
            putExtra("date", sessionDate)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            slot.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    fun cancelAllAlarms(slots: List<TimetableSlotWithModule>) {
        for (slotWithModule in slots) {
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                slotWithModule.slot.id,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }
}
