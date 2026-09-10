package com.example.ui.settings

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("uni_leca_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_DEFAULT_THRESHOLD = "default_threshold"
        private const val KEY_REMINDER_DELAY_MINUTES = "reminder_delay_minutes"
        private const val KEY_NOTIFICATION_RATIONALE_SEEN = "notification_rationale_seen"
        private const val KEY_MEDICAL_COUNTS_AS_ATTENDED = "medical_counts_as_attended"
    }

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()

    var defaultThreshold: Float
        get() = prefs.getFloat(KEY_DEFAULT_THRESHOLD, 80.0f)
        set(value) = prefs.edit().putFloat(KEY_DEFAULT_THRESHOLD, value.coerceIn(0f, 100f)).apply()

    var reminderDelayMinutes: Int
        get() = prefs.getInt(KEY_REMINDER_DELAY_MINUTES, 30)
        set(value) = prefs.edit().putInt(KEY_REMINDER_DELAY_MINUTES, value.coerceIn(0, 180)).apply()

    var notificationRationaleSeen: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_RATIONALE_SEEN, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATION_RATIONALE_SEEN, value).apply()

    var medicalCountsAsAttended: Boolean
        get() = prefs.getBoolean(KEY_MEDICAL_COUNTS_AS_ATTENDED, true)
        set(value) = prefs.edit().putBoolean(KEY_MEDICAL_COUNTS_AS_ATTENDED, value).apply()
}
