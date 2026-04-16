package com.prayerquest.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.prayerquest.app.data.preferences.ThemeMode
import com.prayerquest.app.ui.navigation.PrayerQuestNavHost
import com.prayerquest.app.ui.theme.PrayerQuestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as PrayerQuestApplication
        val userPreferences = app.container.userPreferences

        setContent {
            val themeMode by userPreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val selectedThemeId by userPreferences.selectedThemeId.collectAsState(initial = "prayer_quest")

            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            PrayerQuestTheme(
                themeId = selectedThemeId,
                darkTheme = darkTheme
            ) {
                PrayerQuestNavHost(
                    userPreferences = userPreferences
                )
            }
        }
    }
}
