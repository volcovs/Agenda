package com.personalagenda.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalAgendaTypography = staticCompositionLocalOf { AgendaType }

/** Convenience accessors: AgendaTheme.colors / AgendaTheme.type */
object AgendaTheme {
    val colors: AgendaColors
        @Composable get() = LocalAgendaColors.current
    val type: AgendaTypography
        @Composable get() = LocalAgendaTypography.current
}

@Composable
fun PersonalAgendaTheme(
    option: AppThemeOption = AppThemeOption.SYSTEM,
    content: @Composable () -> Unit,
) {
    val colors = option.colors(systemDark = isSystemInDarkTheme())
    val darkTheme = colors.isDark

    // Populate the Material3 scheme fully from our palette so Material components
    // (time picker, date picker, slider, checkboxes) adopt the editorial look
    // instead of the default purple.
    val onAccent = if (darkTheme) colors.background else androidx.compose.ui.graphics.Color.White
    val base = if (darkTheme) darkColorScheme() else lightColorScheme()
    val materialScheme = base.copy(
        primary = colors.personal,
        onPrimary = onAccent,
        primaryContainer = colors.surfaceElevated,
        onPrimaryContainer = colors.textPrimary,
        secondary = colors.work,
        onSecondary = onAccent,
        tertiary = colors.learning,
        onTertiary = onAccent,
        background = colors.background,
        onBackground = colors.textPrimary,
        surface = colors.surface,
        onSurface = colors.textPrimary,
        surfaceVariant = colors.surfaceElevated,
        onSurfaceVariant = colors.textSecondary,
        surfaceContainer = colors.surface,
        surfaceContainerHigh = colors.surfaceElevated,
        surfaceContainerHighest = colors.surfaceElevated,
        surfaceContainerLow = colors.surface,
        outline = colors.textFaint,
        outlineVariant = colors.divider,
        error = colors.deadline,
        onError = onAccent,
    )

    CompositionLocalProvider(
        LocalAgendaColors provides colors,
        LocalAgendaTypography provides AgendaType,
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = Material3Typography,
            content = content,
        )
    }
}
