package com.prayerquest.app.ui.prayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prayerquest.app.data.entity.PrayerItem
import com.prayerquest.app.data.entity.PrayerRecord
import com.prayerquest.app.data.repository.GamificationRepository
import com.prayerquest.app.data.repository.PrayerRepository
import com.prayerquest.app.data.repository.SessionGamificationResult
import com.prayerquest.app.domain.model.PrayerGrade
import com.prayerquest.app.domain.model.PrayerMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Holds a prayer item paired with an auto-assigned mode for the session.
 */
data class SessionItem(
    val prayerItem: PrayerItem,
    val mode: PrayerMode
)

sealed class UiState {
    data object Loading : UiState()
    data class InProgress(
        val currentItemIndex: Int,
        val totalItems: Int,
        val currentMode: PrayerMode,
        val currentPrayerItem: PrayerItem,
        val showGradeBar: Boolean
    ) : UiState()
    data class Finished(
        val result: SessionGamificationResult
    ) : UiState()
}

class PrayerSessionViewModel(
    private val prayerRepository: PrayerRepository,
    private val gamificationRepository: GamificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var sessionItems: List<SessionItem> = emptyList()
    private var currentItemIndex = 0
    private var sessionStartTime = 0L
    private var currentModeCompletionContent = ""
    private var totalXpEarned = 0
    private var sessionDuration = 0L

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val availableModes = PrayerMode.entries.toList()

    init {
        loadPrayerItems()
    }

    private fun loadPrayerItems() {
        viewModelScope.launch {
            try {
                val items = prayerRepository.getPrayerItems()
                if (items.isEmpty()) {
                    sessionItems = generateDefaultSessionItems()
                } else {
                    // Assign modes to items cyclically
                    sessionItems = items.mapIndexed { index, item ->
                        SessionItem(
                            prayerItem = item,
                            mode = availableModes[index % availableModes.size]
                        )
                    }
                }
                sessionStartTime = System.currentTimeMillis()
                advanceToNextItem()
            } catch (e: Exception) {
                sessionItems = generateDefaultSessionItems()
                sessionStartTime = System.currentTimeMillis()
                advanceToNextItem()
            }
        }
    }

    private fun generateDefaultSessionItems(): List<SessionItem> {
        return listOf(
            SessionItem(
                prayerItem = PrayerItem(id = 0, title = "ACTS Prayer", description = "Adoration, Confession, Thanksgiving, Supplication"),
                mode = PrayerMode.GuidedActs
            ),
            SessionItem(
                prayerItem = PrayerItem(id = 0, title = "Gratitude Blast", description = "Rapid-fire gratitude listing"),
                mode = PrayerMode.GratitudeBlast
            ),
            SessionItem(
                prayerItem = PrayerItem(id = 0, title = "Scripture Soak", description = "Meditate on a verse and pray over it"),
                mode = PrayerMode.ScriptureSoak
            )
        )
    }

    private fun advanceToNextItem() {
        if (currentItemIndex < sessionItems.size) {
            val sessionItem = sessionItems[currentItemIndex]
            _uiState.value = UiState.InProgress(
                currentItemIndex = currentItemIndex,
                totalItems = sessionItems.size,
                currentMode = sessionItem.mode,
                currentPrayerItem = sessionItem.prayerItem,
                showGradeBar = false
            )
        } else {
            finishSession()
        }
    }

    fun onModeComplete(content: String) {
        currentModeCompletionContent = content
        val currentState = _uiState.value
        if (currentState is UiState.InProgress) {
            _uiState.value = currentState.copy(showGradeBar = true)
        }
    }

    fun onGradeSubmitted(grade: PrayerGrade, depth: Int) {
        viewModelScope.launch {
            recordPrayerSession(grade, depth)
            currentItemIndex++
            advanceToNextItem()
        }
    }

    private suspend fun recordPrayerSession(grade: PrayerGrade, depth: Int) {
        try {
            val sessionItem = sessionItems[currentItemIndex]
            val sessionDurationMillis = System.currentTimeMillis() - sessionStartTime
            val xpEarned = calculateXpForGrade(grade, sessionItem.mode)

            val record = PrayerRecord(
                prayerItemId = if (sessionItem.prayerItem.id > 0) sessionItem.prayerItem.id else null,
                mode = sessionItem.mode.name,
                durationSeconds = (sessionDurationMillis / 1000).toInt(),
                grade = grade.name,
                depthRating = depth,
                journalText = currentModeCompletionContent.ifEmpty { null },
                xpEarned = xpEarned,
                sessionDate = dateFormat.format(Date())
            )

            prayerRepository.recordPrayerSession(record)
            totalXpEarned += xpEarned
            sessionDuration = sessionDurationMillis / 1000
        } catch (e: Exception) {
            // Continue session even on error
        }
    }

    private fun calculateXpForGrade(grade: PrayerGrade, mode: PrayerMode): Int {
        val baseXp = mode.baseXp
        return (baseXp * grade.xpMultiplier).toInt()
    }

    private fun finishSession() {
        viewModelScope.launch {
            try {
                val result = gamificationRepository.onPrayerSessionCompleted(
                    xpEarned = totalXpEarned,
                    sessionDuration = sessionDuration.toInt(),
                    itemsCompleted = sessionItems.size
                )
                _uiState.value = UiState.Finished(result)
            } catch (e: Exception) {
                val result = SessionGamificationResult(
                    xpEarned = totalXpEarned,
                    streakDays = 1,
                    currentLevel = 1,
                    levelProgressPercent = 25,
                    newlyUnlockedAchievements = emptyList()
                )
                _uiState.value = UiState.Finished(result)
            }
        }
    }

    fun resetSession() {
        currentItemIndex = 0
        totalXpEarned = 0
        sessionDuration = 0L
        sessionStartTime = System.currentTimeMillis()
        loadPrayerItems()
    }

    companion object {
        class Factory(
            private val prayerRepository: PrayerRepository,
            private val gamificationRepository: GamificationRepository
        ) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PrayerSessionViewModel(
                    prayerRepository = prayerRepository,
                    gamificationRepository = gamificationRepository
                ) as T
            }
        }
    }
}
