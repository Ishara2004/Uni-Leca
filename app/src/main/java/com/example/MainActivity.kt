package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.notification.AlarmScheduler
import com.example.ui.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory((application as UniLecaApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            var themeMode by remember { mutableStateOf(viewModel.settingsManager.themeMode) }
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> systemDark
            }

            MyApplicationTheme(darkTheme = darkTheme) {
                MainScreen(
                    viewModel = viewModel,
                    themeMode = themeMode,
                    onThemeModeChange = { selectedMode ->
                        viewModel.settingsManager.themeMode = selectedMode
                        themeMode = selectedMode
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val date = intent?.getStringExtra(AlarmScheduler.EXTRA_DATE).orEmpty()
        if (date.isNotBlank()) viewModel.openDateFromNotification(date)
    }
}
