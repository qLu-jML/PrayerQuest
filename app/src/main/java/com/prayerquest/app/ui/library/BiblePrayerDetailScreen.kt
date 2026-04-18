package com.prayerquest.app.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prayerquest.app.PrayerQuestApplication
import com.prayerquest.app.data.entity.BiblePrayer
import com.prayerquest.app.data.repository.GamificationRepository
import com.prayerquest.app.data.repository.PrayerRepository
import com.prayerquest.app.data.repository.SessionGamificationResult
import com.prayerquest.app.domain.model.PrayerGrade
import com.prayerquest.app.domain.model.PrayerMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Detail view for a single Bible prayer.
 *
 * Layout uses a [Scaffold] with a pinned [bottomBar] so the
 * "I Prayed This Today" CTA is always visible regardless of how long the
 * Scripture text is. Body scrolls beneath it. Tapping the CTA fires the
 * gamification hook (real XP + streak), shows a quick summary dialog, and
 * pops back when the user taps "Done".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiblePrayerDetailScreen(
    prayerId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as PrayerQuestApplication
    val viewModel: BiblePrayerDetailViewModel = viewModel(
        factory = BiblePrayerDetailViewModel.Factory(
            prayerRepository = app.container.prayerRepository,
            gamificationRepository = app.container.gamificationRepository,
            prayerId = prayerId
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val summary by viewModel.summary.collectAsState()
    // Convenience handle to the loaded prayer — null until the DB lookup
    // resolves to a real row. Used by the pinned bottom bar (only visible
    // once we actually have a prayer to log against) and the summary
    // dialog's title. Replaces the old plain-nullable StateFlow whose
    // null-forever case was the source of the infinite-spinner bug when
    // a bad id was navigated to.
    val loadedPrayer = (uiState as? BiblePrayerDetailViewModel.UiState.Loaded)?.prayer

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Bible Prayer") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            val current = loadedPrayer
            if (current != null) {
                Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "You've prayed this",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${current.userPrayedCount} times",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Button(
                            onClick = { viewModel.onPrayedThisToday() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                "I Prayed This Today",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        // Three-branch state machine — see UiState doc on the VM. Prevents
        // the infinite spinner when a lookup resolves to null.
        when (val state = uiState) {
            BiblePrayerDetailViewModel.UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Scaffold
            }
            BiblePrayerDetailViewModel.UiState.NotFound -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Prayer not found",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "We couldn't load this prayer. It may have been removed or updated.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = onNavigateBack) { Text("Go Back") }
                    }
                }
                return@Scaffold
            }
            is BiblePrayerDetailViewModel.UiState.Loaded -> Unit  // fall through
        }
        val current = (uiState as BiblePrayerDetailViewModel.UiState.Loaded).prayer

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = current.title,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    if (current.person.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = current.person,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (current.reference.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = current.reference,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (current.testament.isNotEmpty()) {
                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(
                                        "${current.testament} Testament",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                        if (current.category.isNotEmpty()) {
                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(current.category, style = MaterialTheme.typography.labelSmall)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Label,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }
            }

            if (current.description.isNotBlank()) {
                item { HorizontalDivider() }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "About this prayer",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = current.description,
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }

            item { HorizontalDivider() }

            item {
                // Scripture passage — the verse text itself. If we don't yet
                // have the KJV text for this entry (first MVP pass ships
                // metadata-only for several references), show a graceful
                // placeholder pointing the user to the reference.
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Scripture",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (current.text.isNotBlank()) {
                                current.text
                            } else {
                                "Read ${current.reference} in your Bible. " +
                                    "KJV text for this reference will appear here in the next update."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // Summary-and-pop dialog. Shown after the user taps "I Prayed This
    // Today" and the gamification hook returns. "Done" pops back to
    // wherever the user came from so they don't have to hunt for the
    // back arrow.
    summary?.let { result ->
        PrayedSummaryDialog(
            result = result,
            prayerTitle = loadedPrayer?.title.orEmpty(),
            onDone = {
                viewModel.clearSummary()
                onNavigateBack()
            }
        )
    }
}

/**
 * Small summary dialog shown after a one-tap "I Prayed This Today" on
 * either a Famous or Bible prayer detail. Mirrors the SessionSummary's
 * information density at a glance: XP earned, streak days, level-up
 * callout when relevant.
 */
@Composable
internal fun PrayedSummaryDialog(
    result: SessionGamificationResult,
    prayerTitle: String,
    onDone: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDone,
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = { Text(text = "Amen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (prayerTitle.isNotBlank()) {
                    Text(
                        text = "\"$prayerTitle\" logged.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = "+${result.xpEarned} XP",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                if (result.leveledUp) {
                    Text(
                        text = "Level up! → ${result.levelTitle}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (result.streakDays > 0) {
                    Text(
                        text = "Streak: ${result.streakDays} day${if (result.streakDays == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (result.newAchievements.isNotEmpty()) {
                    Text(
                        text = "New badge: ${result.newAchievements.first().name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDone) { Text("Done") }
        }
    )
}

class BiblePrayerDetailViewModel(
    private val prayerRepository: PrayerRepository,
    private val gamificationRepository: GamificationRepository,
    private val prayerId: String
) : ViewModel() {

    /**
     * Three-state load result — replaces the old `StateFlow<BiblePrayer?>`
     * whose single nullable value couldn't distinguish "still loading" from
     * "lookup finished, nothing found". That ambiguity caused the infinite-
     * spinner bug on library prayers whose ids didn't resolve to a real
     * DB row.
     */
    sealed interface UiState {
        data object Loading : UiState
        data class Loaded(val prayer: BiblePrayer) : UiState
        data object NotFound : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /**
     * Populated after a successful "I Prayed This Today" tap. Null means
     * no summary is showing — the UI consults this to drive the dialog.
     */
    private val _summary = MutableStateFlow<SessionGamificationResult?>(null)
    val summary: StateFlow<SessionGamificationResult?> = _summary.asStateFlow()

    init {
        reload()
    }

    private fun reload() {
        viewModelScope.launch {
            val row = prayerRepository.getBiblePrayer(prayerId)
            _uiState.value = if (row != null) UiState.Loaded(row) else UiState.NotFound
        }
    }

    fun onPrayedThisToday() {
        viewModelScope.launch {
            // Guard: only log if we actually loaded a prayer.
            if (_uiState.value !is UiState.Loaded) return@launch
            prayerRepository.incrementBiblePrayerPrayedCount(prayerId)
            // Fire the standard session-complete hook so XP, streak, and
            // daily quests all register the same as a full-mode session.
            // Bible prayers aren't "famous" per the isFamousPrayer flag
            // (that's for FamousPrayer entries); they're scripture-backed,
            // so we count them as a regular session run via Lectio —
            // closest spiritual analogue for reading-and-praying.
            val result = gamificationRepository.onPrayerSessionCompleted(
                mode = PrayerMode.LECTIO_DIVINA,
                grade = PrayerGrade.GOOD,
                durationSeconds = 60,
                itemsPrayed = 1,
                isFamousPrayer = false,
                isGroupPrayer = false
            )
            reload()
            _summary.value = result
        }
    }

    fun clearSummary() {
        _summary.value = null
    }

    class Factory(
        private val prayerRepository: PrayerRepository,
        private val gamificationRepository: GamificationRepository,
        private val prayerId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BiblePrayerDetailViewModel(
                prayerRepository,
                gamificationRepository,
                prayerId
            ) as T
        }
    }
}
