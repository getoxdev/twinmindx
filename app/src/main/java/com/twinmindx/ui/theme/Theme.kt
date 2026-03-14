package com.twinmindx.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = TextOnPrimary,
    primaryContainer = CardBlueLight,
    onPrimaryContainer = PrimaryBlue,
    secondary = AccentOrange,
    onSecondary = TextOnPrimary,
    secondaryContainer = CardCream,
    onSecondaryContainer = AccentOrange,
    tertiary = AccentCyan,
    onTertiary = TextOnPrimary,
    tertiaryContainer = BackgroundSoft,
    onTertiaryContainer = PrimaryBlue,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundSoft,
    onSurfaceVariant = TextSecondary,
    error = StatusRecording,
    onError = TextOnPrimary,
    errorContainer = StatusRecording.copy(alpha = 0.1f),
    onErrorContainer = StatusRecording,
    outline = BorderLight,
    outlineVariant = BorderBlue,
    scrim = PrimaryBlue.copy(alpha = 0.5f),
    inverseSurface = PrimaryBlue,
    inverseOnSurface = TextOnPrimary,
    inversePrimary = PrimaryBlueLight
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueLight,
    onPrimary = TextOnPrimary,
    primaryContainer = PrimaryBlueDark,
    onPrimaryContainer = PrimaryBlueLight,
    secondary = AccentOrange,
    onSecondary = TextOnPrimary,
    secondaryContainer = AccentOrange.copy(alpha = 0.2f),
    onSecondaryContainer = AccentOrange,
    tertiary = AccentCyan,
    onTertiary = TextOnPrimary,
    tertiaryContainer = PrimaryBlue,
    onTertiaryContainer = AccentCyan,
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFF94A3B8),
    error = StatusRecording,
    onError = TextOnPrimary,
    errorContainer = StatusRecording.copy(alpha = 0.2f),
    onErrorContainer = StatusRecording,
    outline = Color(0xFF475569),
    outlineVariant = PrimaryBlueLight,
    scrim = Color(0xFF000000).copy(alpha = 0.7f),
    inverseSurface = Color(0xFFF1F5F9),
    inverseOnSurface = Color(0xFF0F172A),
    inversePrimary = PrimaryBlue
)

@Composable
fun TwinmindxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled to use our custom blue theme
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
