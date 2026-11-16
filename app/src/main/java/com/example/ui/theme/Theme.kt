package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = LightBlueContainer,
    onPrimaryContainer = Color(0xFF07365E),
    secondary = BrandCyan,
    onSecondary = Color(0xFF003640),
    secondaryContainer = Color(0xFFD6F7FC),
    onSecondaryContainer = Color(0xFF063B44),
    tertiary = BrandAmber,
    onTertiary = Color(0xFF3F2A00),
    background = LightBackground,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextMuted,
    outline = Color(0xFFC9D9E9),
    outlineVariant = Color(0xFFDDE8F3),
    error = Color(0xFFC83D50),
    errorContainer = Color(0xFFFFDADB),
    onErrorContainer = Color(0xFF680018)
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color(0xFF003353),
    primaryContainer = Color(0xFF164C77),
    onPrimaryContainer = Color(0xFFD7ECFF),
    secondary = DarkSecondary,
    onSecondary = Color(0xFF00363F),
    secondaryContainer = Color(0xFF164B55),
    onSecondaryContainer = Color(0xFFD0F7FC),
    tertiary = DarkTertiary,
    onTertiary = Color(0xFF402B00),
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextMuted,
    outline = Color(0xFF536B83),
    outlineVariant = Color(0xFF344A61),
    error = Color(0xFFFF9AA5),
    errorContainer = Color(0xFF5C2630),
    onErrorContainer = Color(0xFFFFDADD)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
