package com.friendcall.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FriendCallColors = darkColorScheme(
    primary = Color(0xFF9A8CFF),
    onPrimary = Color(0xFF1B1245),
    primaryContainer = Color(0xFF3A3380),
    onPrimaryContainer = Color(0xFFE6E0FF),
    secondary = Color(0xFF61D9C5),
    onSecondary = Color(0xFF00382F),
    background = Color(0xFF101322),
    onBackground = Color(0xFFE4E6F2),
    surface = Color(0xFF1A2033),
    onSurface = Color(0xFFE4E6F2),
    surfaceVariant = Color(0xFF252C45),
    onSurfaceVariant = Color(0xFFB8BCD0),
    surfaceContainerLowest = Color(0xFF0C0F1B),
    surfaceContainerLow = Color(0xFF151A2B),
    surfaceContainer = Color(0xFF1A2033),
    surfaceContainerHigh = Color(0xFF212840),
    surfaceContainerHighest = Color(0xFF252C45),
    outline = Color(0xFF6E7391),
    outlineVariant = Color(0xFF353B57),
)

@Composable
fun FriendCallTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = FriendCallColors, content = content)
}
