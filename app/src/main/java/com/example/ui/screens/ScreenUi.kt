package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(27.dp)
                )
            }
        }
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SectionHeading(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(21.dp)
        )
        Spacer(Modifier.size(9.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Surface(
                modifier = Modifier.size(58.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(29.dp)
                    )
                }
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun AttendanceStatusPill(label: String, modifier: Modifier = Modifier) {
    val palette = attendancePalette(label)
    Surface(
        modifier = modifier,
        color = palette.container,
        contentColor = palette.content,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
fun AttendanceActionButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = attendancePalette(label)
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 9.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = palette.container,
            contentColor = palette.content
        )
    ) {
        Text(label, maxLines = 1, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun attendancePalette(label: String): StatusPalette {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.45f
    return when (label.trim().lowercase(Locale.ROOT)) {
        "present" -> StatusPalette(
            content = if (dark) Color(0xFF80E5B1) else Color(0xFF147A4B),
            container = if (dark) Color(0xFF173B2D) else Color(0xFFE1F5E9)
        )
        "absent" -> StatusPalette(
            content = if (dark) Color(0xFFFF9AA1) else Color(0xFFB83243),
            container = if (dark) Color(0xFF47252B) else Color(0xFFFCE7E9)
        )
        "medical" -> StatusPalette(
            content = if (dark) Color(0xFFFFD27D) else Color(0xFF94600A),
            container = if (dark) Color(0xFF45371F) else Color(0xFFFFF2D7)
        )
        "no class", "no-class" -> StatusPalette(
            content = if (dark) Color(0xFFB9C9DB) else Color(0xFF53677E),
            container = if (dark) Color(0xFF2B394A) else Color(0xFFEAF0F6)
        )
        else -> StatusPalette(
            content = MaterialTheme.colorScheme.onSurfaceVariant,
            container = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun SessionTypeIcon(
    type: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    val icon = when (type.trim().lowercase(Locale.ROOT)) {
        "lecture" -> Icons.Default.MenuBook
        "lab", "laboratory" -> Icons.Default.Science
        "tutorial" -> Icons.Default.Groups
        else -> Icons.Default.Schedule
    }
    Icon(imageVector = icon, contentDescription = type, modifier = modifier, tint = tint)
}

private data class StatusPalette(val content: Color, val container: Color)
