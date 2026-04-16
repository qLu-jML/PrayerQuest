package com.prayerquest.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {

    companion object {
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val SELECTED_THEME_ID = stringPreferencesKey("selected_theme_id")
        private val CUSTOM_THEMES_JSON = stringPreferencesKey("custom_themes_json")
        private val DAILY_GOAL = intPreferencesKey("daily_goal")
        private val GRATITUDE_GOAL = intPreferencesKey("gratitude_goal")
        private val IS_PREMIUM = booleanPreferencesKey("is_premium")
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val DISPLAY_NAME = stringPreferencesKey("display_name")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        val mode = preferences[THEME_MODE] ?: ThemeMode.SYSTEM.name
        ThemeMode.valueOf(mode)
    }

    val selectedThemeId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SELECTED_THEME_ID] ?: "prayer_quest"
    }

    val customThemesJson: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CUSTOM_THEMES_JSON] ?: "[]"
    }

    val dailyGoal: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[DAILY_GOAL] ?: 10
    }

    val gratitudeGoal: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[GRATITUDE_GOAL] ?: 3
    }

    val isPremium: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_PREMIUM] ?: false
    }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETED] ?: false
    }

    val displayName: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[DISPLAY_NAME] ?: "Prayer Warrior"
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode.name
        }
    }

    suspend fun setSelectedThemeId(themeId: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_THEME_ID] = themeId
        }
    }

    suspend fun setCustomThemesJson(json: String) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_THEMES_JSON] = json
        }
    }

    suspend fun setDailyGoal(minutes: Int) {
        val validGoals = listOf(5, 10, 15, 20, 30, 45, 60)
        val goal = if (minutes in validGoals) minutes else 10
        context.dataStore.edit { preferences ->
            preferences[DAILY_GOAL] = goal
        }
    }

    suspend fun setGratitudeGoal(count: Int) {
        val validGoals = listOf(1, 3, 5)
        val goal = if (count in validGoals) count else 3
        context.dataStore.edit { preferences ->
            preferences[GRATITUDE_GOAL] = goal
        }
    }

    suspend fun setIsPremium(premium: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_PREMIUM] = premium
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setDisplayName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[DISPLAY_NAME] = name
        }
    }
}
