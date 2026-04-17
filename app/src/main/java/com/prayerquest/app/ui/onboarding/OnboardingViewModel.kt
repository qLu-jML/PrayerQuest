package com.prayerquest.app.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.prayerquest.app.data.entity.GratitudeEntry
import com.prayerquest.app.data.entity.PrayerCollection
import com.prayerquest.app.data.preferences.DevotionalAuthor
import com.prayerquest.app.data.preferences.LiturgicalCalendar
import com.prayerquest.app.data.preferences.ReminderSlot
import com.prayerquest.app.data.preferences.ReminderSlotConfig
import com.prayerquest.app.data.preferences.UserPreferences
import com.prayerquest.app.data.repository.CollectionRepository
import com.prayerquest.app.data.repository.GratitudeRepository
import com.prayerquest.app.domain.model.Tradition
import com.prayerquest.app.notifications.NotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Holds every answer the user gives during the 8-step onboarding flow so the
 * UI can move forwards and backwards without losing state. Every field has a
 * sensible default — the user can skip any step and we'll just persist the
 * default for that preference.
 */
data class OnboardingAnswers(
    // Step 2 — Name + daily goal
    val displayName: String = "Prayer Warrior",
    val dailyGoalMinutes: Int = 10,

    // Step 3 — Tradition multi-select (defaults to the universal set)
    val traditions: Set<Tradition> = Tradition.DEFAULT,

    // Step 4 — Liturgical calendar
    val liturgicalCalendar: LiturgicalCalendar = LiturgicalCalendar.NONE,

    // Step 5 — Devotional author + per-author times.
    // Spurgeon's "Morning and Evening" naturally has two reading slots per
    // day, so Spurgeon gets a morning time, an evening time, and an enable
    // toggle for each half (user might only want the evening reading, or
    // vice versa). Bonhoeffer is a single evening slot.
    val devotionalAuthor: DevotionalAuthor = DevotionalAuthor.NONE,
    val spurgeonMin: Int = UserPreferences.DEFAULT_SPURGEON_MIN,
    val spurgeonEveningMin: Int = UserPreferences.DEFAULT_SPURGEON_EVENING_MIN,
    val spurgeonMorningEnabled: Boolean = true,
    val spurgeonEveningEnabled: Boolean = true,
    val bonhoefferMin: Int = UserPreferences.DEFAULT_BONHOEFFER_MIN,

    // Step 6 — Reminder window + quiet hours
    val morningEnabled: Boolean = true,
    val morningMin: Int = UserPreferences.DEFAULT_MORNING_MIN,
    val middayEnabled: Boolean = false,
    val middayMin: Int = UserPreferences.DEFAULT_MIDDAY_MIN,
    val eveningEnabled: Boolean = true,
    val eveningMin: Int = UserPreferences.DEFAULT_EVENING_MIN,
    val quietHoursEnabled: Boolean = true,
    val quietStartMin: Int = UserPreferences.DEFAULT_QUIET_START_MIN,
    val quietEndMin: Int = UserPreferences.DEFAULT_QUIET_END_MIN,

    // Step 7 — First prayer collection
    val firstCollectionName: String = "My Prayer List",
    val firstCollectionDescription: String = "Your personal prayer list — add the people and things on your heart",

    // Step 8 — First gratitude entry (optional)
    val firstGratitudeText: String = ""
)

/**
 * ViewModel for the 8-step onboarding flow. Collects answers into a single
 * [OnboardingAnswers] state object, persists everything in one atomic
 * [completeOnboarding] call, and then marks onboarding done.
 *
 * Each step's "save" happens in-memory until the final step; only at the end
 * do we write to DataStore / Room. This keeps things cleanly recoverable if
 * the user backgrounds the app mid-flow (they restart from the top, but no
 * partial state survives to confuse the main app).
 */
class OnboardingViewModel(
    private val collectionRepository: CollectionRepository,
    private val gratitudeRepository: GratitudeRepository,
    private val userPreferences: UserPreferences,
    private val applicationContext: Context
) : ViewModel() {

    private val _answers = MutableStateFlow(OnboardingAnswers())
    val answers: StateFlow<OnboardingAnswers> = _answers.asStateFlow()

    fun update(transform: OnboardingAnswers.() -> OnboardingAnswers) {
        _answers.value = _answers.value.transform()
    }

    fun setDisplayName(name: String) = update { copy(displayName = name) }
    fun setDailyGoal(minutes: Int) = update { copy(dailyGoalMinutes = minutes) }

    fun toggleTradition(tradition: Tradition) {
        val current = _answers.value.traditions
        val next = if (tradition in current) {
            (current - tradition).ifEmpty { Tradition.DEFAULT }
        } else {
            current + tradition
        }
        update { copy(traditions = next) }
    }

    fun setLiturgicalCalendar(choice: LiturgicalCalendar) =
        update { copy(liturgicalCalendar = choice) }

    fun setDevotionalAuthor(author: DevotionalAuthor) =
        update { copy(devotionalAuthor = author) }

    fun setSpurgeonMin(min: Int) = update { copy(spurgeonMin = min) }
    fun setSpurgeonEveningMin(min: Int) = update { copy(spurgeonEveningMin = min) }
    fun setSpurgeonMorningEnabled(enabled: Boolean) =
        update { copy(spurgeonMorningEnabled = enabled) }
    fun setSpurgeonEveningEnabled(enabled: Boolean) =
        update { copy(spurgeonEveningEnabled = enabled) }
    fun setBonhoefferMin(min: Int) = update { copy(bonhoefferMin = min) }

    fun setMorning(enabled: Boolean, min: Int = _answers.value.morningMin) =
        update { copy(morningEnabled = enabled, morningMin = min) }

    fun setMidday(enabled: Boolean, min: Int = _answers.value.middayMin) =
        update { copy(middayEnabled = enabled, middayMin = min) }

    fun setEvening(enabled: Boolean, min: Int = _answers.value.eveningMin) =
        update { copy(eveningEnabled = enabled, eveningMin = min) }

    fun setQuietHoursEnabled(enabled: Boolean) =
        update { copy(quietHoursEnabled = enabled) }

    fun setQuietWindow(startMin: Int, endMin: Int) =
        update { copy(quietStartMin = startMin, quietEndMin = endMin) }

    fun setFirstCollectionName(name: String) =
        update { copy(firstCollectionName = name) }

    fun setFirstCollectionDescription(description: String) =
        update { copy(firstCollectionDescription = description) }

    fun setFirstGratitudeText(text: String) =
        update { copy(firstGratitudeText = text) }

    /**
     * Single final commit of the entire onboarding flow. Writes preferences,
     * seeds the starter prayer collection, saves the first gratitude entry
     * (if provided), triggers a full reschedule so the user's notification
     * choices take effect immediately, and marks onboarding as complete.
     *
     * Catches and prints exceptions at the boundary — we NEVER leave the user
     * stuck on the onboarding screen because of a transient write failure.
     * Worst case: they end up in the main app with a couple of missing
     * preferences, which they can fix in Settings.
     */
    suspend fun completeOnboarding() = withContext(Dispatchers.IO) {
        val a = _answers.value

        // --- Preferences ---------------------------------------------------
        runCatching {
            userPreferences.setDisplayName(a.displayName.ifBlank { "Prayer Warrior" })
            userPreferences.setDailyGoal(a.dailyGoalMinutes)
            userPreferences.setEnabledTraditions(a.traditions)
            userPreferences.setLiturgicalCalendar(a.liturgicalCalendar)
            userPreferences.setDevotionalAuthor(a.devotionalAuthor)
            userPreferences.setDevotionalTime(DevotionalAuthor.SPURGEON, a.spurgeonMin)
            userPreferences.setDevotionalSpurgeonEveningMin(a.spurgeonEveningMin)
            userPreferences.setDevotionalSpurgeonMorningEnabled(a.spurgeonMorningEnabled)
            userPreferences.setDevotionalSpurgeonEveningEnabled(a.spurgeonEveningEnabled)
            userPreferences.setDevotionalTime(DevotionalAuthor.BONHOEFFER, a.bonhoefferMin)

            userPreferences.setReminderSlot(
                ReminderSlotConfig(
                    slot = ReminderSlot.MORNING,
                    enabled = a.morningEnabled,
                    minuteOfDay = a.morningMin,
                    personality = UserPreferences.DEFAULT_MORNING_PERSONALITY
                )
            )
            userPreferences.setReminderSlot(
                ReminderSlotConfig(
                    slot = ReminderSlot.MIDDAY,
                    enabled = a.middayEnabled,
                    minuteOfDay = a.middayMin,
                    personality = UserPreferences.DEFAULT_MIDDAY_PERSONALITY
                )
            )
            userPreferences.setReminderSlot(
                ReminderSlotConfig(
                    slot = ReminderSlot.EVENING,
                    enabled = a.eveningEnabled,
                    minuteOfDay = a.eveningMin,
                    personality = UserPreferences.DEFAULT_EVENING_PERSONALITY
                )
            )
            userPreferences.setQuietHoursEnabled(a.quietHoursEnabled)
            userPreferences.setQuietHoursWindow(a.quietStartMin, a.quietEndMin)
        }.onFailure { it.printStackTrace() }

        // --- Starter prayer collection -------------------------------------
        runCatching {
            val collection = PrayerCollection(
                name = a.firstCollectionName.ifBlank { "My Prayer List" },
                description = a.firstCollectionDescription.ifBlank {
                    "Your personal prayer list — add the people and things on your heart"
                },
                emoji = "\uD83D\uDE4F",  // folded hands
                topicTag = "Personal",
                itemCount = 0
            )
            collectionRepository.create(collection)
        }.onFailure { it.printStackTrace() }

        // --- First gratitude entry (optional — skip if blank) --------------
        if (a.firstGratitudeText.isNotBlank()) {
            runCatching {
                val today = LocalDate.now().toString()  // yyyy-MM-dd
                gratitudeRepository.add(
                    GratitudeEntry(
                        date = today,
                        text = a.firstGratitudeText.trim(),
                        category = GratitudeEntry.CATEGORY_OTHER
                    )
                )
            }.onFailure { it.printStackTrace() }
        }

        // --- Reschedule with the new times we just saved -------------------
        runCatching {
            NotificationScheduler.rescheduleAll(applicationContext)
        }.onFailure { it.printStackTrace() }

        // --- Mark onboarding done — always, even if some writes above failed
        runCatching {
            userPreferences.setOnboardingCompleted(true)
        }.onFailure {
            it.printStackTrace()
            // Last-chance retry: even a single-flag write failure shouldn't
            // trap the user in onboarding forever.
            runCatching { userPreferences.setOnboardingCompleted(true) }
        }
    }

    /**
     * Factory for manual DI.
     */
    class Factory(
        private val collectionRepository: CollectionRepository,
        private val gratitudeRepository: GratitudeRepository,
        private val userPreferences: UserPreferences,
        private val applicationContext: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OnboardingViewModel(
                collectionRepository,
                gratitudeRepository,
                userPreferences,
                applicationContext
            ) as T
        }
    }
}
