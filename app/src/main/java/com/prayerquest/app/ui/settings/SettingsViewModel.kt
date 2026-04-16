package com.prayerquest.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prayerquest.app.data.preferences.ThemeMode
import com.prayerquest.app.data.preferences.UserPreferences
import com.prayerquest.app.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Serializable data class for storing custom themes
 */
@Serializable
data class CustomThemeData(
    val id: String,
    val name: String,
    val description: String,
    val lightPrimary: String,       // Store as hex string
    val lightOnPrimary: String,
    val lightPrimaryContainer: String,
    val lightOnPrimaryContainer: String,
    val lightSecondary: String,
    val lightOnSecondary: String,
    val lightSecondaryContainer: String,
    val lightBackground: String,
    val lightSurface: String,
    val lightOnBackground: String,
    val lightOnSurface: String,
    val lightSurfaceVariant: String,
    val darkPrimary: String,
    val darkOnPrimary: String,
    val darkPrimaryContainer: String,
    val darkOnPrimaryContainer: String,
    val darkSecondary: String,
    val darkOnSecondary: String,
    val darkSecondaryContainer: String,
    val darkBackground: String,
    val darkSurface: String,
    val darkOnBackground: String,
    val darkOnSurface: String,
    val darkSurfaceVariant: String
) {
    fun toAppTheme(): AppTheme {
        return AppTheme(
            id = id,
            name = name,
            description = description,
            isBuiltIn = false,
            lightPrimary = lightPrimary.toLong(16),
            lightOnPrimary = lightOnPrimary.toLong(16),
            lightPrimaryContainer = lightPrimaryContainer.toLong(16),
            lightOnPrimaryContainer = lightOnPrimaryContainer.toLong(16),
            lightSecondary = lightSecondary.toLong(16),
            lightOnSecondary = lightOnSecondary.toLong(16),
            lightSecondaryContainer = lightSecondaryContainer.toLong(16),
            lightBackground = lightBackground.toLong(16),
            lightSurface = lightSurface.toLong(16),
            lightOnBackground = lightOnBackground.toLong(16),
            lightOnSurface = lightOnSurface.toLong(16),
            lightSurfaceVariant = lightSurfaceVariant.toLong(16),
            darkPrimary = darkPrimary.toLong(16),
            darkOnPrimary = darkOnPrimary.toLong(16),
            darkPrimaryContainer = darkPrimaryContainer.toLong(16),
            darkOnPrimaryContainer = darkOnPrimaryContainer.toLong(16),
            darkSecondary = darkSecondary.toLong(16),
            darkOnSecondary = darkOnSecondary.toLong(16),
            darkSecondaryContainer = darkSecondaryContainer.toLong(16),
            darkBackground = darkBackground.toLong(16),
            darkSurface = darkSurface.toLong(16),
            darkOnBackground = darkOnBackground.toLong(16),
            darkOnSurface = darkOnSurface.toLong(16),
            darkSurfaceVariant = darkSurfaceVariant.toLong(16)
        )
    }

    companion object {
        fun fromAppTheme(theme: AppTheme): CustomThemeData {
            return CustomThemeData(
                id = theme.id,
                name = theme.name,
                description = theme.description,
                lightPrimary = "%08X".format(theme.lightPrimary),
                lightOnPrimary = "%08X".format(theme.lightOnPrimary),
                lightPrimaryContainer = "%08X".format(theme.lightPrimaryContainer),
                lightOnPrimaryContainer = "%08X".format(theme.lightOnPrimaryContainer),
                lightSecondary = "%08X".format(theme.lightSecondary),
                lightOnSecondary = "%08X".format(theme.lightOnSecondary),
                lightSecondaryContainer = "%08X".format(theme.lightSecondaryContainer),
                lightBackground = "%08X".format(theme.lightBackground),
                lightSurface = "%08X".format(theme.lightSurface),
                lightOnBackground = "%08X".format(theme.lightOnBackground),
                lightOnSurface = "%08X".format(theme.lightOnSurface),
                lightSurfaceVariant = "%08X".format(theme.lightSurfaceVariant),
                darkPrimary = "%08X".format(theme.darkPrimary),
                darkOnPrimary = "%08X".format(theme.darkOnPrimary),
                darkPrimaryContainer = "%08X".format(theme.darkPrimaryContainer),
                darkOnPrimaryContainer = "%08X".format(theme.darkOnPrimaryContainer),
                darkSecondary = "%08X".format(theme.darkSecondary),
                darkOnSecondary = "%08X".format(theme.darkOnSecondary),
                darkSecondaryContainer = "%08X".format(theme.darkSecondaryContainer),
                darkBackground = "%08X".format(theme.darkBackground),
                darkSurface = "%08X".format(theme.darkSurface),
                darkOnBackground = "%08X".format(theme.darkOnBackground),
                darkOnSurface = "%08X".format(theme.darkOnSurface),
                darkSurfaceVariant = "%08X".format(theme.darkSurfaceVariant)
            )
        }
    }
}

/**
 * SettingsViewModel manages user preferences for settings, themes, and goals
 */
class SettingsViewModel(private val userPreferences: UserPreferences) : ViewModel() {

    // Expose user preferences as flows
    val themeMode: Flow<ThemeMode> = userPreferences.themeMode
    val selectedThemeId: Flow<String> = userPreferences.selectedThemeId
    val dailyGoal: Flow<Int> = userPreferences.dailyGoal
    val gratitudeGoal: Flow<Int> = userPreferences.gratitudeGoal
    val displayName: Flow<String> = userPreferences.displayName
    val customThemesJson: Flow<String> = userPreferences.customThemesJson

    /**
     * Set the theme mode (System, Light, or Dark)
     */
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            userPreferences.setThemeMode(mode)
        }
    }

    /**
     * Set the selected theme ID
     */
    fun setSelectedThemeId(themeId: String) {
        viewModelScope.launch {
            userPreferences.setSelectedThemeId(themeId)
        }
    }

    /**
     * Set the daily prayer goal in minutes
     */
    fun setDailyGoal(minutes: Int) {
        viewModelScope.launch {
            userPreferences.setDailyGoal(minutes)
        }
    }

    /**
     * Set the daily gratitude goal in entries
     */
    fun setGratitudeGoal(count: Int) {
        viewModelScope.launch {
            userPreferences.setGratitudeGoal(count)
        }
    }

    /**
     * Set the user's display name
     */
    fun setDisplayName(name: String) {
        viewModelScope.launch {
            userPreferences.setDisplayName(name)
        }
    }

    /**
     * Add a custom theme to the user's collection
     * Reads current custom themes, adds the new one, and saves back
     */
    fun addCustomTheme(theme: AppTheme) {
        viewModelScope.launch {
            try {
                val customThemeData = CustomThemeData.fromAppTheme(theme)
                userPreferences.customThemesJson.collect { json ->
                    val themes = try {
                        Json.decodeFromString<List<CustomThemeData>>(
                            if (json.isBlank() || json == "[]") "[]" else json
                        )
                    } catch (e: Exception) {
                        emptyList()
                    }

                    val updatedThemes = themes.filter { it.id != customThemeData.id } + customThemeData
                    val newJson = Json.encodeToString(updatedThemes)
                    userPreferences.setCustomThemesJson(newJson)
                }
            } catch (e: Exception) {
                // Handle JSON serialization error gracefully
            }
        }
    }

    /**
     * Remove a custom theme by ID
     */
    fun removeCustomTheme(themeId: String) {
        viewModelScope.launch {
            try {
                userPreferences.customThemesJson.collect { json ->
                    val themes = try {
                        Json.decodeFromString<List<CustomThemeData>>(
                            if (json.isBlank() || json == "[]") "[]" else json
                        )
                    } catch (e: Exception) {
                        emptyList()
                    }

                    val updatedThemes = themes.filter { it.id != themeId }
                    val newJson = Json.encodeToString(updatedThemes)
                    userPreferences.setCustomThemesJson(newJson)
                }
            } catch (e: Exception) {
                // Handle error gracefully
            }
        }
    }

    /**
     * Factory for creating SettingsViewModel instances
     */
    companion object {
        fun create(userPreferences: UserPreferences): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                        return SettingsViewModel(userPreferences) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}
