package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.data.model.TimetableSlotWithModule
import com.example.ui.settings.SettingsManager
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs = context.getSharedPreferences("uni_leca_alarms", Context.MODE_PRIVATE)
    private val settings = SettingsManager(context)

    fun scheduleAlarmsForSlots(slots: List<TimetableSlotWithModule>) {
        val desiredIds = slots.map { it.slot.id }.toSet()
        val previouslyScheduled = prefs.getStringSet(KEY_SCHEDULED_IDS, emptySet())
            .orEmpty().mapNotNull { it.toIntOrNull() }.toSet()

        (previouslyScheduled - desiredIds).forEach(::cancelAlarm)

        if (!settings.notificationsEnabled) {
            desiredIds.forEach(::cancelAlarm)
            saveScheduledIds(emptySet())
            return
        }

        val successfullyScheduled = mutableSetOf<Int>()
        slots.forEach { item ->
            if (scheduleAlarmForSlot(item)) successfullyScheduled += item.slot.id
        }
        saveScheduledIds(successfullyScheduled)
    }

    fun scheduleAlarmForSlot(item: TimetableSlotWithModule): Boolean {
        val slot = item.slot
        if (slot.archivedAt != null || !settings.notificationsEnabled) {
            cancelAlarm(slot.id)
            return false
        }

        val triggerAt = calculateNextTrigger(item) ?: run {
            cancelAlarm(slot.id)
            return false
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(EXTRA_SLOT_ID, slot.id)
            putExtra(EXTRA_MODULE_NAME, item.moduleName)
            putExtra(EXTRA_SESSION_TYPE, slot.sessionType)
            putExtra(EXTRA_DATE, sessionDateForTrigger(item, triggerAt))
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            slot.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Attendance reminders do not require exact-to-the-minute delivery. Using an
        // inexact idle-safe alarm avoids special exact-alarm access and Play policy friction.
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        rememberScheduled(slot.id)
        return true
    }

    fun cancelAlarm(slotId: Int) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            slotId,
            Intent(context, NotificationReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
        forgetScheduled(slotId)
    }

    fun cancelAllKnownAlarms() {
        prefs.getStringSet(KEY_SCHEDULED_IDS, emptySet()).orEmpty()
            .mapNotNull { it.toIntOrNull() }
            .forEach(::cancelAlarm)
        saveScheduledIds(emptySet())
    }

    private fun calculateNextTrigger(item: TimetableSlotWithModule): Long? {
        val slot = item.slot
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        val endTime = runCatching { LocalTime.parse(slot.endTime) }.getOrNull() ?: return null
        val delay = settings.reminderDelayMinutes.toLong()

        val scheduledDate = if (slot.isRecurring) {
            val today = LocalDate.now(zone)
            val currentDay = today.dayOfWeek.value
            var daysAhead = (slot.dayOfWeek - currentDay + 7) % 7
            var candidate = ZonedDateTime.of(today.plusDays(daysAhead.toLong()), endTime, zone)
                .plusMinutes(delay)
            if (!candidate.isAfter(now)) {
                daysAhead += 7
                candidate = ZonedDateTime.of(today.plusDays(daysAhead.toLong()), endTime, zone)
                    .plusMinutes(delay)
            }
            candidate
        } else {
            val date = slot.specificDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?: return null
            val candidate = ZonedDateTime.of(date, endTime, zone).plusMinutes(delay)
            if (!candidate.isAfter(now)) return null
            candidate
        }

        return scheduledDate.toInstant().toEpochMilli()
    }

    private fun sessionDateForTrigger(item: TimetableSlotWithModule, triggerAt: Long): String {
        item.slot.specificDate?.let { return it }
        val zone = ZoneId.systemDefault()
        val triggerDateTime = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(triggerAt), zone)
        val sessionInstant = triggerDateTime.minusMinutes(settings.reminderDelayMinutes.toLong())
        return sessionInstant.toLocalDate().toString()
    }

    private fun rememberScheduled(slotId: Int) {
        val set = prefs.getStringSet(KEY_SCHEDULED_IDS, emptySet()).orEmpty().toMutableSet()
        set += slotId.toString()
        saveScheduledIds(set.mapNotNull { it.toIntOrNull() }.toSet())
    }

    private fun forgetScheduled(slotId: Int) {
        val set = prefs.getStringSet(KEY_SCHEDULED_IDS, emptySet()).orEmpty().toMutableSet()
        set -= slotId.toString()
        prefs.edit().putStringSet(KEY_SCHEDULED_IDS, set).apply()
    }

    private fun saveScheduledIds(ids: Set<Int>) {
        prefs.edit().putStringSet(KEY_SCHEDULED_IDS, ids.map { it.toString() }.toSet()).apply()
    }

    companion object {
        private const val KEY_SCHEDULED_IDS = "scheduled_alarm_ids"
        const val EXTRA_SLOT_ID = "slotId"
        const val EXTRA_MODULE_NAME = "moduleName"
        const val EXTRA_SESSION_TYPE = "sessionType"
        const val EXTRA_DATE = "date"
    }
}
