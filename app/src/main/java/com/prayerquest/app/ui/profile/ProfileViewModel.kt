package com.prayerquest.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prayerquest.app.data.entity.AchievementProgress
import com.prayerquest.app.data.entity.UserStats
import com.prayerquest.app.data.repository.GamificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Profile screen ViewModel — combines stats and achievements.
 */
sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Ready(
        val stats: UserStats,
        val achievements: List<AchievementProgress>
    ) : ProfileUiState
}

class ProfileViewModel(
    private val gamificationRepository: GamificationRepository
) : ViewModel() {

    val uiState: Flow<ProfileUiState> = combine(
        gamificationRepository.observeStats(),
        gamificationRepository.observeAchievements()
    ) { stats, achievements ->
        ProfileUiState.Ready(
            stats = stats ?: UserStats(),
            achievements = achievements
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = ProfileUiState.Loading
    )
}

class ProfileViewModelFactory(
    private val gamificationRepository: GamificationRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ProfileViewModel(gamificationRepository) as T
    }
}
