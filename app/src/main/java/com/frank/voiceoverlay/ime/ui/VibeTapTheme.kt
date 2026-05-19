package com.frank.voiceoverlay.ime.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val VibeTapColorScheme = darkColorScheme(
    primary = Color(0xFF63E6BE),
    onPrimary = Color(0xFF06261C),
    secondary = Color(0xFF89B4FF),
    onSecondary = Color(0xFF0B1B34),
    tertiary = Color(0xFFF3B8FF),
    onTertiary = Color(0xFF331142),
    background = Color(0xFF050816),
    onBackground = Color(0xFFE9ECF8),
    surface = Color(0xFF11182A),
    onSurface = Color(0xFFF4F7FF),
    surfaceVariant = Color(0xFF1A2339),
    onSurfaceVariant = Color(0xFFBAC6E3),
    error = Color(0xFFFF8F9B),
    onError = Color(0xFF3A0710),
)

private val VibeTapShapes = Shapes(
    extraSmall = RoundedCornerShape(14.dp),
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun VibeTapTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VibeTapColorScheme,
        shapes = VibeTapShapes,
        typography = Typography(),
        content = content,
    )
}
