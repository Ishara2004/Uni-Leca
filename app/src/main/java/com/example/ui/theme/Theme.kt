package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DarkMinimalPrimary,
    secondary = DarkMinimalSecondary,
    tertiary = DarkMinimalTertiary,
    background = DarkMinimalBackground,
    surface = DarkMinimalSurface,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = DarkMinimalOnSurface,
    onSurface = DarkMinimalOnSurface,
    onSurfaceVariant = Color(0xFF94A3B8),
    primaryContainer = Color(0xFF00325B),
    onPrimaryContainer = Color(0xFFD1E4FF)
)

private val LightColorScheme = lightColorScheme(
    primary = MinimalPrimary,
    secondary = MinimalSecondary,
    tertiary = MinimalTertiary,
    background = MinimalBackground,
    surface = MinimalSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = MinimalTextPrimary,
    onSurface = MinimalTextPrimary,
    onSurfaceVariant = MinimalTextSecondary,
    primaryContainer = MinimalTertiary,
    onPrimaryContainer = MinimalOnPrimaryContainer,
    surfaceVariant = Color(0xFFF1F5F9) // slate-100
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set dynamic color default to false to force our beautiful branding
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
