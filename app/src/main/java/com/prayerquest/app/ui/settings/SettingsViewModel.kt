package com.prayerquest.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prayerquest.app.data.preferences.DevotionalAuthor
import com.prayerquest.app.data.preferences.LiturgicalCalendar
import com.prayerquest.app.data.preferences.ReminderSlotConfig
import com.prayerquest.app.data.preferences.ThemeMode
import com.prayerquest.app.data.preferences.UserPreferences
import com.prayerquest.app.domain.model.PrayerMode
import com.prayerquest.app.domain.model.Tradition
import com.prayerquest.app.notifications.NotificationScheduler
import com.prayerquest.app.ui.theme.AppTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Serializable data class for storing custom themes
 */
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
 * SettingsViewModel manages user preferences: themes, goals, reminder slots,
 * quiet hours, tradition + mode toggles, and the daily-devotional author.
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

    // --- Sprint 4 flows (notifications + traditions + devotional) ----------
    val reminderSlots: Flow<List<ReminderSlotConfig>> = userPreferences.reminderSlots
    val quietHoursEnabled: Flow<Boolean> = userPreferences.quietHoursEnabled
    val quietHoursStartMin: Flow<Int> = userPreferences.quietHoursStartMin
    val quietHoursEndMin: Flow<Int> = userPreferences.quietHoursEndMin
    val enabledTraditions: Flow<Set<Tradition>> = userPreferences.enabledTraditions
    val disabledModes: Flow<Set<PrayerMode>> = userPreferences.disabledModes
    val devotionalAuthor: Flow<DevotionalAuthor> = userPreferences.devotionalAuthor
    val devotionalSpurgeonMin: Flow<Int> = userPreferences.devotionalSpurgeonMin
    val devotionalSpurgeonEveningMin: Flow<Int> = userPreferences.devotionalSpurgeonEveningMin
    val devotionalSpurgeonMorningEnabled: Flow<Boolean> =
        userPreferences.devotionalSpurgeonMorningEnabled
    val devotionalSpurgeonEveningEnabled: Flow<Boolean> =
        userPreferences.devotionalSpurgeonEveningEnabled
    val devotionalBonhoefferMin: Flow<Int> = userPreferences.devotionalBonhoefferMin
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

    fun setDevotionalAuthor(author: DevotionalAuthor) {
        viewModelScope.launch {
            userPreferences.setDevotionalAuthor(author)
            NotificationScheduler.rescheduleAll(applicationContext)
        }
    }

    fun setDevotionalTime(author: DevotionalAuthor, minuteOfDay: Int) {
        viewModelScope.launch {
            userPreferences.setDevotionalTime(author, minuteOfDay)
            NotificationScheduler.rescheduleAll(applicationContext)
        }
    }

    /** Spurgeon's EVENING time slot — writes then reschedules. */
    fun setDevotionalSpurgeonEveningMin(minuteOfDay: Int) {
        viewModelScope.launch {
            userPreferences.setDevotionalSpurgeonEveningMin(minuteOfDay)
            NotificationScheduler.rescheduleAll(applicationContext)
        }
    }

    fun setDevotionalSpurgeonMorningEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setDevotionalSpurgeonMorningEnabled(enabled)
            NotificationScheduler.rescheduleAll(applicationContext)
        }
    }

    fun setDevotionalSpurgeonEveningEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setDevotionalSpurgeonEveningEnabled(enabled)
            NotificationScheduler.rescheduleAll(applicationContext)
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
