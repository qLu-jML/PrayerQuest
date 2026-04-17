package com.prayerquest.app.ui.formation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prayerquest.app.data.entity.PrayerItem
import com.prayerquest.app.data.repository.GamificationRepository
import com.prayerquest.app.data.repository.PrayerRepository
import com.prayerquest.app.domain.model.PrayerGrade
import com.prayerquest.app.domain.model.PrayerMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FormationViewModel(
    private val prayerRepository: PrayerRepository,
    private val gamificationRepository: GamificationRepository
) : ViewModel() {

    companion object {
        private const val TOTAL_STEPS = 6
        private const val FORMATION_COMPLETION_XP = 25
    }

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _xpEarned = MutableStateFlow(0)
    val xpEarned: StateFlow<Int> = _xpEarned.asStateFlow()

    private val _isComplete = MutableStateFlow(false)
    val isComplete: StateFlow<Boolean> = _isComplete.asStateFlow()

    private val _prayerItem = MutableStateFlow(
        PrayerItem(
            title = "My First Prayer",
            description = "A prayer to begin my journey with PrayerQuest",
            category = "Personal",
            status = PrayerItem.STATUS_ACTIVE
        )
    )
    val prayerItem: StateFlow<PrayerItem> = _prayerItem.asStateFlow()

    fun nextStep() {
        if (_currentStep.value < TOTAL_STEPS - 1) {
            _currentStep.value++
        } else {
            // On final step, award XP
            awardCompletionXP()
        }
    }

    fun previousStep() {
        if (_currentStep.value > 0) {
            _currentStep.value--
        }
    }

    private fun awardCompletionXP() {
        viewModelScope.launch {
            try {
                gamificationRepository.onPrayerSessionCompleted(
                    mode = PrayerMode.GUIDED_ACTS,
                    grade = PrayerGrade.GOOD,
                    durationSeconds = 0,
                    itemsPrayed = 1,
                    isFamousPrayer = false,
                    isGroupPrayer = false
                )
                _xpEarned.value = FORMATION_COMPLETION_XP
                _isComplete.value = true
            } catch (e: Exception) {
                _xpEarned.value = FORMATION_COMPLETION_XP
                _isComplete.value = true
            }
        }
    }

    fun addToCollection(prayerItem: PrayerItem) {
        viewModelScope.launch {
            try {
                prayerRepository.addItem(prayerItem)
            } catch (e: Exception) {
                // Handle silently
            }
        }
    }

    fun addPrayerItem(title: String, description: String) {
        viewModelScope.launch {
            try {
                prayerRepository.addItem(
                    PrayerItem(
                        title = title,
                        description = description,
                        category = "Personal",
                        status = "Active"
                    )
                )
            } catch (e: Exception) {
                // Handle silently
            }
        }
    }

    class Factory(
        private val prayerRepository: PrayerRepository,
        private val gamificationRepository: GamificationRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FormationViewModel(prayerRepository, gamificationRepository) as T
        }
    }
}
