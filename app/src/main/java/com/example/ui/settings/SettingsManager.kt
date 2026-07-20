package com.example.ui.settings

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("uni_leca_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_DEFAULT_THRESHOLD = "default_threshold"
    }

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()

    var defaultThreshold: Float
        get() = prefs.getFloat(KEY_DEFAULT_THRESHOLD, 80.0f)
        set(value) = prefs.edit().putFloat(KEY_DEFAULT_THRESHOLD, value).apply()
}
