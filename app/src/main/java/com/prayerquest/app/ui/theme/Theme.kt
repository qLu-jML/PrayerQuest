package com.prayerquest.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

// CompositionLocal for the current theme ID — read by a few screens that want
// to brand themselves (e.g. streak flame tinting).
val LocalCurrentThemeId = staticCompositionLocalOf<String> { "prayer_quest" }

// CompositionLocal for premium status — ad views read this to hide ads for
// premium users without needing a ViewModel collection.
val LocalIsPremium = staticCompositionLocalOf<Boolean> { false }

@Composable
fun PrayerQuestTheme(
    themeId: String = "prayer_quest",
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,   // Brand palette preserved by default
    content: @Composable () -> Unit
) {
    val appTheme = ThemeRepository.getThemeByIdOrDefault(themeId)

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> appTheme.toMaterialColorScheme(isDark = darkTheme)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = {
            CompositionLocalProvider(
                LocalCurrentThemeId provides themeId,
                content = content
            )
        }
    )
}

@Composable
fun currentThemeId(): String = LocalCurrentThemeId.current
