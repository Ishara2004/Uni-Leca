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
import com.example.ui.settings.SettingsManager

class NotificationReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "uni_leca_reminders"
        const val CHANNEL_NAME = "Attendance Reminders"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val settings = SettingsManager(context)
        if (!settings.notificationsEnabled) return

        val slotId = intent.getIntExtra("slotId", -1)
        if (slotId == -1) return

        val moduleName = intent.getStringExtra("moduleName") ?: "Module"
        val sessionType = intent.getStringExtra("sessionType") ?: "Class"
        val date = intent.getStringExtra("date") ?: ""

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders to mark attendance for finished classes"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("slotId", slotId)
            putExtra("date", date)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            slotId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Mark Attendance")
            .setContentText("Mark attendance for $moduleName ($sessionType)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(slotId, notification)
    }
}
