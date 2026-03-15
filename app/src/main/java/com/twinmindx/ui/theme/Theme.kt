package com.twinmindx.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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

@Composable
fun TwinmindxTheme(content: @Composable () -> Unit) {
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
