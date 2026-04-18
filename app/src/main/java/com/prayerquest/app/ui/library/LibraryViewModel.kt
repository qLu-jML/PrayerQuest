package com.prayerquest.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prayerquest.app.data.preferences.LiturgicalCalendar as CalendarChoice
import com.prayerquest.app.data.preferences.UserPreferences
import com.prayerquest.app.data.prayer.SuggestedPrayerPack
import com.prayerquest.app.data.prayer.SuggestedPrayerPackLoader
import com.prayerquest.app.data.repository.CollectionRepository
import com.prayerquest.app.data.repository.GratitudeRepository
import com.prayerquest.app.data.repository.PrayerRepository
import com.prayerquest.app.domain.liturgical.LiturgicalCalendar
import com.prayerquest.app.domain.liturgical.LiturgicalSeason
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class LibraryViewModel(
    collectionRepository: CollectionRepository,
    private val prayerRepository: PrayerRepository,
    gratitudeRepository: GratitudeRepository,
    userPreferences: UserPreferences,
    suggestedPackLoader: SuggestedPrayerPackLoader
) : ViewModel() {

    // Collections tab data
    val collections = collectionRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Famous Prayers tab data
    val famousPrayers = prayerRepository.observeAllFamousPrayers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Bible Prayers tab data — imported from assets/prayers/bible_prayers.json
    // on first launch via BiblePrayerImporter (AppContainer wires this in).
    val biblePrayers = prayerRepository.observeAllBiblePrayers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Answered Prayers tab data
    val answeredPrayers = prayerRepository.observeAnsweredItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Gratitude tab data
    val gratitudeEntries = gratitudeRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * The seasonal pack to pin at the top of the Collections shelf, if any.
     *
     * DD §3.5.4 requires the Library to auto-surface Advent / Lent / Holy
     * Week packs during those seasons when the user has the Liturgical
     * Calendar enabled (Western or Eastern). Returns null in all other
     * situations:
     *  - User has calendar set to NONE → no pin, no regression.
     *  - Current season is Christmas / Easter / Ordinary Time → nothing to
     *    pin (only Advent, Lent, Holy Week map to a pack).
     *  - Matching pack isn't present in assets (e.g., stripped from a slim
     *    build) → nothing to pin, no crash.
     *
     * The pack list is loaded ONCE from assets (not reactively) because the
     * asset bundle cannot change at runtime. The preference Flow is the only
     * source of reactivity — swapping tradition instantly flips the pin.
     */
    val todaySeasonalPack = userPreferences.liturgicalCalendar
        .map { calendarChoice ->
            if (calendarChoice == CalendarChoice.NONE) return@map null
            val day = LiturgicalCalendar.today(
                date = LocalDate.now(),
                calendar = calendarChoice
            )
            val packId = day.season.seasonalPackId() ?: return@map null
            // Loading the pack list is cheap (7 small JSON files) and only
            // happens once the user has opted in to the calendar — so this
            // work is gated behind a real intent.
            suggestedPackLoader.loadAll().firstOrNull { it.id == packId }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    class Factory(
        private val collectionRepository: CollectionRepository,
        private val prayerRepository: PrayerRepository,
        private val gratitudeRepository: GratitudeRepository,
        private val userPreferences: UserPreferences,
        private val suggestedPackLoader: SuggestedPrayerPackLoader
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LibraryViewModel(
                collectionRepository,
                prayerRepository,
                gratitudeRepository,
                userPreferences,
                suggestedPackLoader
            ) as T
        }
    }
}

/**
 * Map a liturgical season to the asset pack id that covers it.
 *
 * Only the three seasons the DD calls out as auto-surfaceable return a
 * non-null id. All other seasons return null, signalling "no pin".
 * The string ids match the `id` field inside each JSON under
 * `app/src/main/assets/packs/`.
 */
private fun LiturgicalSeason.seasonalPackId(): String? = when (this) {
    LiturgicalSeason.ADVENT -> "advent"
    LiturgicalSeason.LENT -> "lent"
    LiturgicalSeason.HOLY_WEEK -> "holy_week"
    else -> null
}
