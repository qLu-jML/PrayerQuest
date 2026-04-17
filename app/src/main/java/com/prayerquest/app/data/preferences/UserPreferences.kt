package com.prayerquest.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.prayerquest.app.domain.model.PrayerMode
import com.prayerquest.app.domain.model.Tradition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

/**
 * Which classical devotional author the user wants delivered each day.
 * Configured in onboarding step 5 and editable in Settings. [NONE] disables the
 * DailyDevotionalWorker entirely.
 */
enum class DevotionalAuthor {
    NONE, SPURGEON, BONHOEFFER, BOTH
}

/**
 * Liturgical calendar preference. If the user selects [NONE], Home hides the
 * liturgical day indicator and seasonal packs do not auto-surface.
 */
enum class LiturgicalCalendar {
    NONE, WESTERN, EASTERN
}

/**
 * Which time-of-day slot a reminder occupies. Used to key per-slot enabled /
 * time / personality preferences and to drive unique WorkManager IDs.
 */
enum class ReminderSlot {
    MORNING, MIDDAY, EVENING
}

/**
 * A reminder slot's current state (enabled + minute-of-day + personality copy).
 * Immutable data class — writes go through [UserPreferences.setReminderSlot].
 */
data class ReminderSlotConfig(
    val slot: ReminderSlot,
    val enabled: Boolean,
    val minuteOfDay: Int,
    val personality: String
)

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

        // Prayer-mode visibility state (Sprint 3). Persisted as CSV of enum
        // names so the store stays human-readable in debug dumps and the
        // schema is trivially evolvable when enums are added later.
        private val ENABLED_TRADITIONS = stringPreferencesKey("enabled_traditions")
        private val DISABLED_MODES = stringPreferencesKey("disabled_modes")

        // Sprint 4 — Notifications + Onboarding.

        // Quiet Hours window, stored as minute-of-day (0..1439) for both
        // endpoints. If start > end the window wraps past midnight (default
        // 22:00 → 07:00 is a wrap). Minute-of-day keeps comparison trivial:
        // in-window = (now in [start, end]) unless wrapped, then negated.
        private val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        private val QUIET_HOURS_START_MIN = intPreferencesKey("quiet_hours_start_min")
        private val QUIET_HOURS_END_MIN = intPreferencesKey("quiet_hours_end_min")

        // Three reminder slots. Each persisted as <enabled>|<minOfDay>|<personality>
        // — pipe-separated because our personality copy legitimately contains
        // commas and colons. Missing slot → falls back to defaults.
        private val REMINDER_MORNING = stringPreferencesKey("reminder_morning")
        private val REMINDER_MIDDAY = stringPreferencesKey("reminder_midday")
        private val REMINDER_EVENING = stringPreferencesKey("reminder_evening")

        // Devotional author selection + per-author delivery time. Spurgeon gets
        // TWO slots — morning AND evening — to match his original
        // "Morning and Evening" book structure (one reading in the morning, one
        // in the evening). Each slot has its own time and its own enable flag
        // so a user can opt in to just one half. Bonhoeffer is a single evening
        // slot.
        //
        // Historical note: the original `devotional_spurgeon_min` key predated
        // the morning/evening split. We preserve its value as the morning slot
        // to keep DataStore upgrades from nuking user-chosen times.
        private val DEVOTIONAL_AUTHOR = stringPreferencesKey("devotional_author")
        private val DEVOTIONAL_SPURGEON_MIN = intPreferencesKey("devotional_spurgeon_min")           // morning (legacy key)
        private val DEVOTIONAL_SPURGEON_EVENING_MIN = intPreferencesKey("devotional_spurgeon_evening_min")
        private val DEVOTIONAL_SPURGEON_MORNING_ENABLED = booleanPreferencesKey("devotional_spurgeon_morning_enabled")
        private val DEVOTIONAL_SPURGEON_EVENING_ENABLED = booleanPreferencesKey("devotional_spurgeon_evening_enabled")
        private val DEVOTIONAL_BONHOEFFER_MIN = intPreferencesKey("devotional_bonhoeffer_min")

        // Liturgical calendar preference (seasonal packs, day indicator).
        private val LITURGICAL_CALENDAR = stringPreferencesKey("liturgical_calendar")

        // ── Defaults ────────────────────────────────────────────────────
        // Quiet Hours default: 10pm → 7am (22:00 → 07:00). This wraps midnight.
        const val DEFAULT_QUIET_START_MIN = 22 * 60
        const val DEFAULT_QUIET_END_MIN = 7 * 60

        const val DEFAULT_MORNING_MIN = 7 * 60 + 30        // 07:30
        const val DEFAULT_MIDDAY_MIN = 12 * 60 + 30        // 12:30
        const val DEFAULT_EVENING_MIN = 20 * 60            // 20:00

        const val DEFAULT_SPURGEON_MIN = 7 * 60            // 07:00 (morning)
        const val DEFAULT_SPURGEON_EVENING_MIN = 20 * 60   // 20:00 (evening — pairs with book structure)
        const val DEFAULT_BONHOEFFER_MIN = 20 * 60         // 20:00

        // Reminder personality defaults. Warm, slightly different tone per
        // slot so the three reminders don't feel like spam. DD §3.2 step 6
        // explicitly calls these out as the tutorial examples.
        const val DEFAULT_MORNING_PERSONALITY = "Morning — start the day with Him"
        const val DEFAULT_MIDDAY_PERSONALITY = "Midday — a quick breath prayer"
        const val DEFAULT_EVENING_PERSONALITY = "Evening — examen and gratitude"
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

    /**
     * Traditions the user has opted in to, driving which Mode Picker shelves
     * appear prominently vs. collapse behind the "Explore more traditions"
     * footer. Defaults to [Tradition.DEFAULT] until onboarding runs (Sprint 4).
     *
     * Stored as CSV to keep the store human-readable. Unknown names in a CSV
     * (e.g., from a future enum rename) are silently dropped — the Flow never
     * throws on deserialize, even after schema churn.
     */
    val enabledTraditions: Flow<Set<Tradition>> = context.dataStore.data.map { preferences ->
        val csv = preferences[ENABLED_TRADITIONS]
        if (csv.isNullOrBlank()) {
            Tradition.DEFAULT
        } else {
            csv.split(",")
                .mapNotNull { raw ->
                    runCatching { Tradition.valueOf(raw.trim()) }.getOrNull()
                }
                .toSet()
                .ifEmpty { Tradition.DEFAULT }
        }
    }

    /**
     * Modes the user has explicitly turned OFF via Settings → Prayer Modes.
     * Per-mode overrides win over tradition-based defaults (either direction):
     * a Protestant can enable the Rosary explicitly; a Catholic can hide
     * Flash-Pray Swipe. See DD §3.1.3.
     */
    val disabledModes: Flow<Set<PrayerMode>> = context.dataStore.data.map { preferences ->
        val csv = preferences[DISABLED_MODES]
        if (csv.isNullOrBlank()) {
            emptySet()
        } else {
            csv.split(",")
                .mapNotNull { raw ->
                    runCatching { PrayerMode.valueOf(raw.trim()) }.getOrNull()
                }
                .toSet()
        }
    }

    // ── Sprint 4 — Notifications / Quiet Hours / Onboarding ───────────────

    val quietHoursEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[QUIET_HOURS_ENABLED] ?: true  // DD §3.2 step 6 — default ON
    }

    val quietHoursStartMin: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[QUIET_HOURS_START_MIN] ?: DEFAULT_QUIET_START_MIN
    }

    val quietHoursEndMin: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[QUIET_HOURS_END_MIN] ?: DEFAULT_QUIET_END_MIN
    }

    /**
     * The three reminder slots as a stable-ordered list (Morning → Midday →
     * Evening). Emits a default slot config when the underlying string is
     * missing or malformed — so the UI never renders an empty reminder row.
     */
    val reminderSlots: Flow<List<ReminderSlotConfig>> =
        context.dataStore.data.map { preferences ->
            listOf(
                parseSlot(
                    raw = preferences[REMINDER_MORNING],
                    slot = ReminderSlot.MORNING,
                    defaultEnabled = true,
                    defaultMinute = DEFAULT_MORNING_MIN,
                    defaultPersonality = DEFAULT_MORNING_PERSONALITY
                ),
                parseSlot(
                    raw = preferences[REMINDER_MIDDAY],
                    slot = ReminderSlot.MIDDAY,
                    defaultEnabled = false,  // opt-in for midday
                    defaultMinute = DEFAULT_MIDDAY_MIN,
                    defaultPersonality = DEFAULT_MIDDAY_PERSONALITY
                ),
                parseSlot(
                    raw = preferences[REMINDER_EVENING],
                    slot = ReminderSlot.EVENING,
                    defaultEnabled = true,
                    defaultMinute = DEFAULT_EVENING_MIN,
                    defaultPersonality = DEFAULT_EVENING_PERSONALITY
                )
            )
        }

    val devotionalAuthor: Flow<DevotionalAuthor> = context.dataStore.data.map { preferences ->
        val raw = preferences[DEVOTIONAL_AUTHOR] ?: DevotionalAuthor.NONE.name
        runCatching { DevotionalAuthor.valueOf(raw) }.getOrDefault(DevotionalAuthor.NONE)
    }

    /**
     * Spurgeon MORNING reminder time. Historically this was simply "Spurgeon
     * time" — we preserve the key so a user who already picked a time before
     * the evening split shipped keeps that choice exactly where it was.
     */
    val devotionalSpurgeonMin: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[DEVOTIONAL_SPURGEON_MIN] ?: DEFAULT_SPURGEON_MIN
    }

    /** Spurgeon EVENING reminder time. Defaults to 20:00. */
    val devotionalSpurgeonEveningMin: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[DEVOTIONAL_SPURGEON_EVENING_MIN] ?: DEFAULT_SPURGEON_EVENING_MIN
    }

    /** Whether Spurgeon's MORNING reminder fires. Defaults ON when author = Spurgeon/Both. */
    val devotionalSpurgeonMorningEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DEVOTIONAL_SPURGEON_MORNING_ENABLED] ?: true
    }

    /** Whether Spurgeon's EVENING reminder fires. Defaults ON when author = Spurgeon/Both. */
    val devotionalSpurgeonEveningEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DEVOTIONAL_SPURGEON_EVENING_ENABLED] ?: true
    }

    val devotionalBonhoefferMin: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[DEVOTIONAL_BONHOEFFER_MIN] ?: DEFAULT_BONHOEFFER_MIN
    }

    val liturgicalCalendar: Flow<LiturgicalCalendar> = context.dataStore.data.map { preferences ->
        val raw = preferences[LITURGICAL_CALENDAR] ?: LiturgicalCalendar.NONE.name
        runCatching { LiturgicalCalendar.valueOf(raw) }.getOrDefault(LiturgicalCalendar.NONE)
    }

    // ── Writers ───────────────────────────────────────────────────────────

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
        val validGoals = listOf(3, 5, 10, 15, 20, 30, 45, 60)
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

    suspend fun setEnabledTraditions(traditions: Set<Tradition>) {
        context.dataStore.edit { preferences ->
            preferences[ENABLED_TRADITIONS] = traditions.joinToString(",") { it.name }
        }
    }

    /**
     * Toggle a single mode on/off. Idempotent: turning an already-disabled mode
     * off is a no-op, same for enabling an already-enabled mode.
     */
    suspend fun setModeEnabled(mode: PrayerMode, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            val currentCsv = preferences[DISABLED_MODES].orEmpty()
            val current = currentCsv.split(",")
                .mapNotNull { raw -> runCatching { PrayerMode.valueOf(raw.trim()) }.getOrNull() }
                .toMutableSet()
            if (enabled) current.remove(mode) else current.add(mode)
            preferences[DISABLED_MODES] = current.joinToString(",") { it.name }
        }
    }

    suspend fun setQuietHoursEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[QUIET_HOURS_ENABLED] = enabled
        }
    }

    suspend fun setQuietHoursWindow(startMin: Int, endMin: Int) {
        val safeStart = startMin.coerceIn(0, 1439)
        val safeEnd = endMin.coerceIn(0, 1439)
        context.dataStore.edit { preferences ->
            preferences[QUIET_HOURS_START_MIN] = safeStart
            preferences[QUIET_HOURS_END_MIN] = safeEnd
        }
    }

    /**
     * Update a single reminder slot. Writing an entire slot object at once
     * rather than individual fields keeps the three sub-keys in a consistent
     * state — the Flow decoder never sees a half-written slot.
     */
    suspend fun setReminderSlot(config: ReminderSlotConfig) {
        val key = when (config.slot) {
            ReminderSlot.MORNING -> REMINDER_MORNING
            ReminderSlot.MIDDAY -> REMINDER_MIDDAY
            ReminderSlot.EVENING -> REMINDER_EVENING
        }
        // Pipe-separated because personality copy contains commas and colons.
        // Escape any embedded pipes in personality — extremely unlikely but
        // cheap to defend against, and keeps the decoder trivially regular.
        val cleanPersonality = config.personality.replace("|", "/")
        context.dataStore.edit { preferences ->
            preferences[key] =
                "${config.enabled}|${config.minuteOfDay.coerceIn(0, 1439)}|$cleanPersonality"
        }
    }

    suspend fun setDevotionalAuthor(author: DevotionalAuthor) {
        context.dataStore.edit { preferences ->
            preferences[DEVOTIONAL_AUTHOR] = author.name
        }
    }

    suspend fun setDevotionalTime(author: DevotionalAuthor, minuteOfDay: Int) {
        val safeMin = minuteOfDay.coerceIn(0, 1439)
        context.dataStore.edit { preferences ->
            when (author) {
                DevotionalAuthor.SPURGEON -> preferences[DEVOTIONAL_SPURGEON_MIN] = safeMin
                DevotionalAuthor.BONHOEFFER -> preferences[DEVOTIONAL_BONHOEFFER_MIN] = safeMin
                DevotionalAuthor.BOTH, DevotionalAuthor.NONE -> {
                    // BOTH/NONE aren't time slots — callers should pass a
                    // specific author. Silently no-op rather than throwing so
                    // a mis-wired UI doesn't crash the user.
                }
            }
        }
    }

    /**
     * Write the Spurgeon evening reminder time. Paired with
     * [setDevotionalTime] (which writes the morning slot) so a user can
     * configure Morning and Evening & Evening independently.
     */
    suspend fun setDevotionalSpurgeonEveningMin(minuteOfDay: Int) {
        val safeMin = minuteOfDay.coerceIn(0, 1439)
        context.dataStore.edit { preferences ->
            preferences[DEVOTIONAL_SPURGEON_EVENING_MIN] = safeMin
        }
    }

    suspend fun setDevotionalSpurgeonMorningEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DEVOTIONAL_SPURGEON_MORNING_ENABLED] = enabled
        }
    }

    suspend fun setDevotionalSpurgeonEveningEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DEVOTIONAL_SPURGEON_EVENING_ENABLED] = enabled
        }
    }

    suspend fun setLiturgicalCalendar(choice: LiturgicalCalendar) {
        context.dataStore.edit { preferences ->
            preferences[LITURGICAL_CALENDAR] = choice.name
        }
    }

    /**
     * Decoder for the pipe-separated reminder-slot string. Tolerates missing
     * fields / malformed ints by falling back to the defaults supplied by the
     * caller — never throws, because a user's notifications should never go
     * silent because of a corrupt DataStore entry.
     */
    private fun parseSlot(
        raw: String?,
        slot: ReminderSlot,
        defaultEnabled: Boolean,
        defaultMinute: Int,
        defaultPersonality: String
    ): ReminderSlotConfig {
        if (raw.isNullOrBlank()) {
            return ReminderSlotConfig(slot, defaultEnabled, defaultMinute, defaultPersonality)
        }
        val parts = raw.split("|", limit = 3)
        val enabled = parts.getOrNull(0)?.toBooleanStrictOrNull() ?: defaultEnabled
        val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 1439) ?: defaultMinute
        val personality = parts.getOrNull(2)?.takeIf { it.isNotBlank() } ?: defaultPersonality
        return ReminderSlotConfig(slot, enabled, minute, personality)
    }
}
