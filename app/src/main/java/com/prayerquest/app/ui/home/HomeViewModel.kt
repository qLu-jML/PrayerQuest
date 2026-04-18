package com.prayerquest.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prayerquest.app.data.preferences.LiturgicalCalendar as CalendarChoice
import com.prayerquest.app.data.preferences.UserPreferences
import com.prayerquest.app.data.repository.DashboardData
import com.prayerquest.app.data.repository.GamificationRepository
import com.prayerquest.app.domain.liturgical.LiturgicalCalendar
import com.prayerquest.app.domain.liturgical.LiturgicalDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/**
 * Home screen ViewModel — reactive dashboard state.
 *
 * Observes [GamificationRepository.observeDashboard] for streak/level/quest
 * state AND [UserPreferences.liturgicalCalendar] for the Liturgical Day
 * indicator. Both Flows are combined into a single atomic [HomeUiState.Ready]
 * so the UI never renders with half-loaded data.
 *
 * The liturgical day is computed lazily from `LocalDate.now()` inside the
 * combine block; the engine is a pure object so this is cheap and never
 * allocates. If the user has selected [CalendarChoice.NONE] the resulting
 * [HomeUiState.Ready.liturgicalDay] is null — HomeScreen treats that as
 * "hide the indicator".
 */
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Ready(
        val dashboard: DashboardData,
        /** Null when the user has the liturgical calendar disabled (NONE). */
        val liturgicalDay: LiturgicalDay? = null
    ) : HomeUiState
}

class HomeViewModel(
    private val gamificationRepository: GamificationRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val uiState: Flow<HomeUiState> = combine(
        gamificationRepository.observeDashboard(),
        userPreferences.liturgicalCalendar
    ) { dashboard, calendarChoice ->
        val day = if (calendarChoice == CalendarChoice.NONE) {
            null
        } else {
            LiturgicalCalendar.today(
                date = LocalDate.now(),
                calendar = calendarChoice
            )
        }
        HomeUiState.Ready(dashboard = dashboard, liturgicalDay = day) as HomeUiState
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = HomeUiState.Loading
    )
}

class HomeViewModelFactory(
    private val gamificationRepository: GamificationRepository,
    private val userPreferences: UserPreferences
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(gamificationRepository, userPreferences) as T
    }
}
