package com.prayerquest.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prayerquest.app.data.repository.DashboardData
import com.prayerquest.app.data.repository.GamificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Home screen ViewModel — reactive dashboard state.
 * Observes GamificationRepository.observeDashboard() and emits HomeUiState.
 */
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Ready(val dashboard: DashboardData) : HomeUiState
}

class HomeViewModel(
    private val gamificationRepository: GamificationRepository
) : ViewModel() {

    val uiState: Flow<HomeUiState> = gamificationRepository.observeDashboard()
        .map<DashboardData, HomeUiState> { HomeUiState.Ready(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = HomeUiState.Loading
        )
}

class HomeViewModelFactory(
    private val gamificationRepository: GamificationRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(gamificationRepository) as T
    }
}
