package com.wakemyway.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WakeMyWayColors = darkColorScheme(
    primary = Color(0xFFE4A17F),
    onPrimary = Color(0xFF101115),
    background = Color(0xFF101115),
    onBackground = Color(0xFFF7F3EC),
    surface = Color(0xFF242129),
    onSurface = Color(0xFFF1E8DC),
    secondary = Color(0xFF8EA18B),
)

@Composable
fun WakeMyWayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WakeMyWayColors,
        content = content,
    )
}
