package com.invictus.smarttelegramfilter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColors = darkColorScheme(
    primary               = Color(0xFF4FC3F7),
    onPrimary             = Color(0xFF003347),
    primaryContainer      = Color(0xFF004C68),
    onPrimaryContainer    = Color(0xFFB8E7FF),
    secondary             = Color(0xFF82AAFF),
    secondaryContainer    = Color(0xFF1A3A6B),
    onSecondaryContainer  = Color(0xFFD6E3FF),
    background            = Color(0xFF0D1117),
    surface               = Color(0xFF161B22),
    surfaceVariant        = Color(0xFF21262D),
    onBackground          = Color(0xFFE6EDF3),
    onSurface             = Color(0xFFE6EDF3),
    onSurfaceVariant      = Color(0xFF8B949E),
    outline               = Color(0xFF30363D),
    error                 = Color(0xFFF85149),
)

private val LightColors = lightColorScheme(
    primary               = Color(0xFF006494),
    onPrimary             = Color.White,
    primaryContainer      = Color(0xFFCDE7FF),
    onPrimaryContainer    = Color(0xFF001E30),
    secondary             = Color(0xFF4361EE),
    secondaryContainer    = Color(0xFFDCE1FF),
    onSecondaryContainer  = Color(0xFF001165),
    background            = Color(0xFFF6F8FA),
    surface               = Color.White,
    surfaceVariant        = Color(0xFFF0F6FC),
    onBackground          = Color(0xFF1C2526),
    onSurface             = Color(0xFF1C2526),
    onSurfaceVariant      = Color(0xFF57606A),
    outline               = Color(0xFFD0D7DE),
    error                 = Color(0xFFCF222E),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small      = RoundedCornerShape(8.dp),
    medium     = RoundedCornerShape(12.dp),
    large      = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun SmartFilterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography  = Typography(),
        shapes      = AppShapes,
        content     = content,
    )
}
