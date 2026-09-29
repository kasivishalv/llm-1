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
    primary = OrangeMain,
    secondary = OrangeLight,
    tertiary = WhiteMain,
    background = BlackMain,
    surface = BlackSurface,
    onPrimary = BlackMain,
    onSecondary = BlackMain,
    onTertiary = BlackMain,
    onBackground = WhiteMain,
    onSurface = WhiteMain,
    primaryContainer = Color(0xFF2B1A08),
    onPrimaryContainer = OrangeLight,
    secondaryContainer = Color(0xFF261D12),
    onSecondaryContainer = OrangeMain,
    surfaceVariant = Color(0xFF222222),
    onSurfaceVariant = Color(0xFFB8B8B8),
    outline = Color(0xFF484848),
    outlineVariant = Color(0xFF323232),
)

private val LightColorScheme = lightColorScheme(
    primary = OrangeMain,
    secondary = OrangeDark,
    tertiary = BlackMain,
    background = WhiteSurface,
    surface = WhiteMain,
    onPrimary = WhiteMain,
    onSecondary = WhiteMain,
    onTertiary = WhiteMain,
    onBackground = BlackMain,
    onSurface = BlackMain,
    primaryContainer = Color(0xFFFFF3E0),
    onPrimaryContainer = OrangeDark,
    secondaryContainer = Color(0xFFFFE0B2),
    onSecondaryContainer = OrangeDark,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFCBD5E1),
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable dynamic colors to enforce Orange/Black/White theme
    content: @Composable () -> Unit
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
