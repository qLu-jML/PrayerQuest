package com.prayerquest.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prayerquest.app.data.preferences.LiturgicalCalendar
import com.prayerquest.app.data.preferences.ReminderSlotConfig
import com.prayerquest.app.data.preferences.ThemeMode
import com.prayerquest.app.data.preferences.UserPreferences
import com.prayerquest.app.domain.model.PrayerMode
import com.prayerquest.app.domain.model.Tradition
import com.prayerquest.app.notifications.NotificationScheduler
import androidx.compose.ui.graphics.Color
import com.prayerquest.app.ui.theme.AppTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Serializable data class for storing custom themes. Matches the 10-slot
 * AppColorScheme-style schema so custom themes can ride the same Material
 * conversion as built-ins. Colors serialize as 8-char ARGB hex strings.
 */
data class CustomThemeData(
    val id: String,
    val name: String,
    val description: String,
    val primary: String,
    val primaryLight: String,
    val primaryDark: String,
    val secondary: String,
    val backgroundPaper: String,
    val backgroundDefault: String,
    val accent: String,
    val success: String,
    val warning: String,
    val info: String
) {
    fun toAppTheme(): AppTheme = AppTheme(
        id = id,
        name = name,
        description = description,
        isBuiltIn = false,
        primary = Color(primary.toLong(16)),
        primaryLight = Color(primaryLight.toLong(16)),
        primaryDark = Color(primaryDark.toLong(16)),
        secondary = Color(secondary.toLong(16)),
        backgroundPaper = Color(backgroundPaper.toLong(16)),
        backgroundDefault = Color(backgroundDefault.toLong(16)),
        accent = Color(accent.toLong(16)),
        success = Color(success.toLong(16)),
        warning = Color(warning.toLong(16)),
        info = Color(info.toLong(16))
    )

    companion object {
        private fun Color.toHex(): String {
            val argb = (alpha * 255).toInt().coerceIn(0, 255) shl 24 or
                ((red * 255).toInt().coerceIn(0, 255) shl 16) or
                ((green * 255).toInt().coerceIn(0, 255) shl 8) or
                (blue * 255).toInt().coerceIn(0, 255)
            return "%08X".format(argb.toLong() and 0xFFFFFFFFL)
        }

        fun fromAppTheme(theme: AppTheme): CustomThemeData = CustomThemeData(
            id = theme.id,
            name = theme.name,
            description = theme.description,
            primary = theme.primary.toHex(),
            primaryLight = theme.primaryLight.toHex(),
            primaryDark = theme.primaryDark.toHex(),
            secondary = theme.secondary.toHex(),
            backgroundPaper = theme.backgroundPaper.toHex(),
            backgroundDefault = theme.backgroundDefault.toHex(),
            accent = theme.accent.toHex(),
            success = theme.success.toHex(),
            warning = theme.warning.toHex(),
            info = theme.info.toHex()
        )
    }
}

/**
 * SettingsViewModel manages user preferences: themes, goals, reminder slots,
 * quiet hours, and tradition + mode toggles.
 *
 * Holds on to an [applicationContext] (the safe application-scoped Context
 * — never an Activity) so time-affecting setters can immediately call
 * [NotificationScheduler.rescheduleAll] after writing the new preference.
 * This is what makes a newly chosen reminder time "just work" without
 * requiring a relaunch.
 */
class SettingsViewModel(
    private val userPreferences: UserPreferences,
    private val applicationContext: Context
) : ViewModel() {

    // --- Existing flows (theme / goals / name) -----------------------------
    val themeMode: Flow<ThemeMode> = userPreferences.themeMode
    val selectedThemeId: Flow<String> = userPreferences.selectedThemeId
    val dailyGoal: Flow<Int> = userPreferences.dailyGoal
    val gratitudeGoal: Flow<Int> = userPreferences.gratitudeGoal
    val displayName: Flow<String> = userPreferences.displayName
    val customThemesJson: Flow<String> = userPreferences.customThemesJson

    // --- Sprint 4 flows (notifications + traditions) -----------------------
    val reminderSlots: Flow<List<ReminderSlotConfig>> = userPreferences.reminderSlots
    val quietHoursEnabled: Flow<Boolean> = userPreferences.quietHoursEnabled
    val quietHoursStartMin: Flow<Int> = userPreferences.quietHoursStartMin
    val quietHoursEndMin: Flow<Int> = userPreferences.quietHoursEndMin
    val enabledTraditions: Flow<Set<Tradition>> = userPreferences.enabledTraditions
    val disabledModes: Flow<Set<PrayerMode>> = userPreferences.disabledModes
    val liturgicalCalendar: Flow<LiturgicalCalendar> = userPreferences.liturgicalCalendar

    // --- Existing setters --------------------------------------------------

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { userPreferences.setThemeMode(mode) }
    }

    fun setSelectedThemeId(themeId: String) {
        viewModelScope.launch { userPreferences.setSelectedThemeId(themeId) }
    }

    fun setDailyGoal(minutes: Int) {
        viewModelScope.launch { userPreferences.setDailyGoal(minutes) }
    }

    fun setGratitudeGoal(count: Int) {
        viewModelScope.launch { userPreferences.setGratitudeGoal(count) }
    }

    fun setDisplayName(name: String) {
        viewModelScope.launch { userPreferences.setDisplayName(name) }
    }

    // --- Sprint 4 setters --------------------------------------------------

    /**
     * Swap a single reminder slot. Writes first, then reschedules — so the
     * new time/enabled state lands in WorkManager before the user can close
     * the settings screen. Reschedule uses REPLACE policy internally so the
     * old work is immediately retired.
     */
    fun setReminderSlot(config: ReminderSlotConfig) {
        viewModelScope.launch {
            userPreferences.setReminderSlot(config)
            NotificationScheduler.rescheduleAll(applicationContext)
        }
    }

    fun setQuietHoursEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setQuietHoursEnabled(enabled)
            // Quiet hours don't change fire times, but the Guard reads the
            // latest flag at fire time — no reschedule strictly needed.
            // Reschedule here anyway for consistency/clarity.
            NotificationScheduler.rescheduleAll(applicationContext)
        }
    }

    fun setQuietHoursWindow(startMin: Int, endMin: Int) {
        viewModelScope.launch {
            userPreferences.setQuietHoursWindow(startMin, endMin)
        }
    }

    fun toggleTradition(tradition: Tradition) {
        viewModelScope.launch {
            val current = userPreferences.enabledTraditions.first()
            val next = if (tradition in current) {
                // Refuse to leave the user with an empty set — fall back to
                // DEFAULT so the Mode Picker still has shelves.
                (current - tradition).ifEmpty { Tradition.DEFAULT }
            } else {
                current + tradition
            }
            userPreferences.setEnabledTraditions(next)
        }
    }

    fun setModeEnabled(mode: PrayerMode, enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setModeEnabled(mode, enabled)
        }
    }

    fun setLiturgicalCalendar(choice: LiturgicalCalendar) {
        viewModelScope.launch {
            userPreferences.setLiturgicalCalendar(choice)
        }
    }

    // --- Custom themes (unchanged) -----------------------------------------

    private val gson = Gson()
    private val themeListType = object : TypeToken<List<CustomThemeData>>() {}.type

    fun addCustomTheme(theme: AppTheme) {
        viewModelScope.launch {
            try {
                val customThemeData = CustomThemeData.fromAppTheme(theme)
                userPreferences.customThemesJson.collect { json ->
                    val themes: List<CustomThemeData> = try {
                        gson.fromJson(
                            if (json.isBlank() || json == "[]") "[]" else json,
                            themeListType
                        ) ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }

                    val updatedThemes = themes.filter { it.id != customThemeData.id } + customThemeData
                    val newJson = gson.toJson(updatedThemes)
                    userPreferences.setCustomThemesJson(newJson)
                }
            } catch (e: Exception) {
                // Handle JSON serialization error gracefully
            }
        }
    }

    fun removeCustomTheme(themeId: String) {
        viewModelScope.launch {
            try {
                userPreferences.customThemesJson.collect { json ->
                    val themes: List<CustomThemeData> = try {
                        gson.fromJson(
                            if (json.isBlank() || json == "[]") "[]" else json,
                            themeListType
                        ) ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }

                    val updatedThemes = themes.filter { it.id != themeId }
                    val newJson = gson.toJson(updatedThemes)
                    userPreferences.setCustomThemesJson(newJson)
                }
            } catch (e: Exception) {
                // Handle error gracefully
            }
        }
    }

    /**
     * Factory for creating SettingsViewModel instances.
     * Now requires the application context for notification rescheduling.
     */
    companion object {
        fun create(
            userPreferences: UserPreferences,
            applicationContext: Context
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                        return SettingsViewModel(
                            userPreferences,
                            applicationContext.applicationContext
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}
