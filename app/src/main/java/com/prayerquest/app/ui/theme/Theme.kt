package com.prayerquest.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// CompositionLocal for the current theme ID
val LocalCurrentThemeId = staticCompositionLocalOf<String> { "prayer_quest" }

/**
 * Create a light color scheme from an AppTheme
 */
private fun createLightColorScheme(theme: AppTheme) = lightColorScheme(
    primary = Color(theme.lightPrimary),
    onPrimary = Color(theme.lightOnPrimary),
    primaryContainer = Color(theme.lightPrimaryContainer),
    onPrimaryContainer = Color(theme.lightOnPrimaryContainer),
    secondary = Color(theme.lightSecondary),
    onSecondary = Color(theme.lightOnSecondary),
    secondaryContainer = Color(theme.lightSecondaryContainer),
    onSecondaryContainer = Color(theme.lightOnPrimaryContainer), // Use on-primary for consistency
    tertiary = Color(theme.lightSecondary), // Use secondary as tertiary for simplicity
    onTertiary = Color(theme.lightOnSecondary),
    tertiaryContainer = Color(theme.lightSecondaryContainer),
    onTertiaryContainer = Color(theme.lightOnPrimaryContainer),
    background = Color(theme.lightBackground),
    onBackground = Color(theme.lightOnBackground),
    surface = Color(theme.lightSurface),
    onSurface = Color(theme.lightOnSurface),
    surfaceVariant = Color(theme.lightSurfaceVariant),
    onSurfaceVariant = Color(theme.lightOnPrimaryContainer),
    outline = Color(theme.lightOnBackground),
    outlineVariant = Color(theme.lightSurfaceVariant),
    error = ErrorRed,
    onError = Color.White
)

/**
 * Create a dark color scheme from an AppTheme
 */
private fun createDarkColorScheme(theme: AppTheme) = darkColorScheme(
    primary = Color(theme.darkPrimary),
    onPrimary = Color(theme.darkOnPrimary),
    primaryContainer = Color(theme.darkPrimaryContainer),
    onPrimaryContainer = Color(theme.darkOnPrimaryContainer),
    secondary = Color(theme.darkSecondary),
    onSecondary = Color(theme.darkOnSecondary),
    secondaryContainer = Color(theme.darkSecondaryContainer),
    onSecondaryContainer = Color(theme.darkOnPrimaryContainer),
    tertiary = Color(theme.darkSecondary), // Use secondary as tertiary for simplicity
    onTertiary = Color(theme.darkOnSecondary),
    tertiaryContainer = Color(theme.darkSecondaryContainer),
    onTertiaryContainer = Color(theme.darkOnPrimaryContainer),
    background = Color(theme.darkBackground),
    onBackground = Color(theme.darkOnBackground),
    surface = Color(theme.darkSurface),
    onSurface = Color(theme.darkOnSurface),
    surfaceVariant = Color(theme.darkSurfaceVariant),
    onSurfaceVariant = Color(theme.darkOnPrimaryContainer),
    outline = Color(theme.darkOnBackground),
    outlineVariant = Color(theme.darkSurfaceVariant),
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun PrayerQuestTheme(
    themeId: String = "prayer_quest",
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,   // Brand palette preserved by default
    content: @Composable () -> Unit
) {
    // Get the theme configuration
    val appTheme = ThemeRepository.getThemeByIdOrDefault(themeId)

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> createDarkColorScheme(appTheme)
        else -> createLightColorScheme(appTheme)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalCurrentThemeId provides themeId,
                content = content
            )
        }
    )
}

/**
 * Get the current theme ID from composition local
 */
@Composable
fun currentThemeId(): String {
    return LocalCurrentThemeId.current
}
