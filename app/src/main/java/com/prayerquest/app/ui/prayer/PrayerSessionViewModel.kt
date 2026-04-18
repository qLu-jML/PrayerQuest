package com.prayerquest.app.ui.prayer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prayerquest.app.data.entity.PrayerItem
import com.prayerquest.app.data.entity.PrayerRecord
import com.prayerquest.app.data.repository.CollectionRepository
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
import com.prayerquest.app.R

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
        val showGradeBar: Boolean,
        /**
         * Full list of prayer items in this session (e.g. every item in the
         * collection the user is praying through). Used by the session UI to
         * show a "Praying for: …" context banner and by topic-based modes
         * like FlashPraySwipe to render cards from the user's real items
         * instead of generic placeholders. Empty when the session was
         * launched without a collection (single-item fallback).
         */
        val sessionItems: List<PrayerItem> = emptyList()
    ) : UiState()
    data class Finished(
        val result: SessionGamificationResult
    ) : UiState()
}

class PrayerSessionViewModel(
    private val prayerRepository: PrayerRepository,
    private val gamificationRepository: GamificationRepository,
    /**
     * The mode the user picked in the Mode Picker (DD §3.1.3). When non-null,
     * the session is a single-item run of this one mode — each picked mode is
     * a complete prayer experience on its own (Breath Prayer, Daily Office,
     * Lectio, Beads all have their own internal flow). When null, falls back
     * to the legacy mixed-session behavior: one item per active prayer item
     * with modes assigned cyclically.
     */
    private val fixedMode: PrayerMode? = null,
    /**
     * When set, the session sources its prayer items from this collection
     * instead of the user's global Active list. Null means "global session"
     * — the historical default. Nav layer passes -1 through [Routes.NO_COLLECTION]
     * which is normalized to null at the screen boundary so this field stays
     * semantically meaningful.
     */
    private val collectionId: Long? = null,
    /**
     * Only consulted when [collectionId] is non-null. We accept null here so
     * callers that never pray a collection (e.g. the Home → mode picker
     * path) don't have to thread the repo through — the Factory can inject
     * it uniformly and we just ignore it in the non-collection branch.
     */
    private val collectionRepository: CollectionRepository? = null,
    /**
     * Application context — used only for resolving string resources for the
     * fallback default-session items. Stored as the application context (set
     * by the Factory) so we never leak an Activity from a long-lived VM.
     */
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var sessionItems: List<SessionItem> = emptyList()
    /**
     * Raw PrayerItem list backing [sessionItems]. Cached so the InProgress
     * state can expose it to the UI (for the "Praying for: …" banner and
     * topic-based modes) without re-hitting the repo every frame.
     */
    private var sessionPrayerItems: List<PrayerItem> = emptyList()
    private var currentItemIndex = 0
    private var sessionStartTime = 0L
    private var currentModeCompletionContent = ""
    private var totalXpEarned = 0
    private var sessionDuration = 0L
    private var modesUsed = mutableSetOf<PrayerMode>()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val availableModes = PrayerMode.entries.toList()

    init {
        loadPrayerItems()
    }

    private fun loadPrayerItems() {
        viewModelScope.launch {
            try {
                // Source of items: collection items if the session was
                // launched from a collection detail, else the user's global
                // Active list. The collection path is critical — users were
                // seeing "placeholders" because we used to always hit the
                // global list regardless of entry point.
                val items: List<PrayerItem> = if (collectionId != null && collectionRepository != null) {
                    collectionRepository.getItemsForCollection(collectionId)
                        .takeIf { it.isNotEmpty() }
                        ?: prayerRepository.getActiveItems()
                } else {
                    prayerRepository.getActiveItems()
                }
                sessionPrayerItems = items
                sessionItems = if (fixedMode != null) {
                    // Single-mode session — the chosen mode is a complete
                    // prayer experience. We seed with ONE session item
                    // anchored on the first available prayer item (or a
                    // synthetic one when there are none). The full list is
                    // still exposed via [sessionPrayerItems] so the UI can
                    // show "Praying for: a, b, c…" and topic-based modes
                    // can iterate through every item within the mode.
                    val anchor = items.firstOrNull() ?: syntheticItemFor(fixedMode)
                    listOf(SessionItem(prayerItem = anchor, mode = fixedMode))
                } else if (items.isEmpty()) {
                    generateDefaultSessionItems().also {
                        sessionPrayerItems = it.map { sessionItem -> sessionItem.prayerItem }
                    }
                } else {
                    // Legacy mixed session — cycle modes across active items.
                    items.mapIndexed { index, item ->
                        SessionItem(
                            prayerItem = item,
                            mode = availableModes[index % availableModes.size]
                        )
                    }
                }
                sessionStartTime = System.currentTimeMillis()
                advanceToNextItem()
            } catch (e: Exception) {
                val fallback = if (fixedMode != null) {
                    listOf(SessionItem(prayerItem = syntheticItemFor(fixedMode), mode = fixedMode))
                } else {
                    generateDefaultSessionItems()
                }
                sessionItems = fallback
                sessionPrayerItems = fallback.map { it.prayerItem }
                sessionStartTime = System.currentTimeMillis()
                advanceToNextItem()
            }
        }
    }

    /**
     * When there are no active prayer items to anchor a session on, synthesize
     * one from the mode metadata so the record has something to attach to and
     * the UI has a title to render. id=0 → PrayerRecord will store
     * prayerItemId=null so it doesn't dangle a FK to a nonexistent row.
     */
    private fun syntheticItemFor(mode: PrayerMode): PrayerItem {
        return PrayerItem(id = 0, title = mode.displayName, description = mode.description)
    }

    private fun generateDefaultSessionItems(): List<SessionItem> {
        return listOf(
            SessionItem(
                prayerItem = PrayerItem(id = 0, title = appContext.getString(R.string.prayer_acts_prayer), description = appContext.getString(R.string.prayer_adoration_confession_thanksgiving_supplication)),
                mode = PrayerMode.GUIDED_ACTS
            ),
            SessionItem(
                prayerItem = PrayerItem(id = 0, title = appContext.getString(R.string.prayer_daily_examen), description = appContext.getString(R.string.prayer_ignatian_5_step_prayerful_review_of_the_day)),
                mode = PrayerMode.DAILY_EXAMEN
            ),
            SessionItem(
                prayerItem = PrayerItem(id = 0, title = appContext.getString(R.string.prayer_breath_prayer), description = appContext.getString(R.string.prayer_inhale_and_exhale_a_short_sacred_phrase)),
                mode = PrayerMode.BREATH_PRAYER
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
                showGradeBar = false,
                sessionItems = sessionPrayerItems
            )
        } else {
            finishSession()
        }
    }

    fun onModeComplete(content: String) {
        currentModeCompletionContent = content
        if (fixedMode != null) {
            // Single-mode sessions come from the Mode Picker (DD §3.1.3) and are
            // a complete prayer experience on their own — Flash Pray, Breath
            // Prayer, Lectio, etc. each have their own internal completion
            // flow. A per-item grade row on top of that is an extra prompt the
            // user can easily miss (especially when the mode itself has a
            // vertically-scrolling body, which pushes the bar below the fold).
            // We auto-grade as GOOD (1× multiplier, the neutral middle option)
            // and advance straight to the summary, which is where the "how did
            // this session land" reflection should happen anyway.
            viewModelScope.launch {
                recordPrayerSession(PrayerGrade.GOOD, depth = 3)
                currentItemIndex++
                advanceToNextItem()
            }
        } else {
            // Legacy mixed session — user rotates through multiple items and
            // grades each one so the gamification layer can weight them
            // individually. Keep the per-item grade bar here.
            val currentState = _uiState.value
            if (currentState is UiState.InProgress) {
                _uiState.value = currentState.copy(showGradeBar = true)
            }
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
            modesUsed.add(sessionItem.mode)
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

            prayerRepository.recordSession(record)
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
                // Use the last mode used or default to GuidedActs
                val lastMode = modesUsed.lastOrNull() ?: PrayerMode.GUIDED_ACTS
                // Sprint-18 voice/journal badge wiring. The session is
                // voice-bearing if any item used VOICE_RECORD, and journal-
                // bearing if any item used PRAYER_JOURNAL. These flags drive
                // the VOICE / JOURNAL lifetime counters on UserStats.
                val hadVoice = PrayerMode.VOICE_RECORD in modesUsed
                val hadJournal = PrayerMode.PRAYER_JOURNAL in modesUsed
                val result = gamificationRepository.onPrayerSessionCompleted(
                    mode = lastMode,
                    grade = PrayerGrade.GOOD,
                    durationSeconds = sessionDuration.toInt(),
                    itemsPrayed = sessionItems.size,
                    isFamousPrayer = false,
                    isGroupPrayer = false,
                    hasVoiceTranscript = hadVoice,
                    hasJournalText = hadJournal
                )
                _uiState.value = UiState.Finished(result)
            } catch (e: Exception) {
                val result = SessionGamificationResult(
                    xpEarned = totalXpEarned,
                    totalXp = totalXpEarned,
                    newLevel = 1,
                    leveledUp = false,
                    levelTitle = "Level 1",
                    streakDays = 1,
                    newAchievements = emptyList()
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
            private val gamificationRepository: GamificationRepository,
            private val fixedMode: PrayerMode? = null,
            private val collectionId: Long? = null,
            private val collectionRepository: CollectionRepository? = null,
            /**
             * Context used only to look up string resources for the default-
             * session fallback. Factory normalizes to applicationContext so
             * the VM can't hold on to an Activity.
             */
            private val context: Context
        ) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PrayerSessionViewModel(
                    prayerRepository = prayerRepository,
                    gamificationRepository = gamificationRepository,
                    fixedMode = fixedMode,
                    collectionId = collectionId,
                    collectionRepository = collectionRepository,
                    appContext = context.applicationContext
                ) as T
            }
        }
    }
}
