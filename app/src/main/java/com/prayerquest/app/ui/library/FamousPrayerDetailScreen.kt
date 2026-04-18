package com.prayerquest.app.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import com.prayerquest.app.data.entity.FamousPrayer
import com.prayerquest.app.data.repository.GamificationRepository
import com.prayerquest.app.data.repository.PrayerRepository
import com.prayerquest.app.data.repository.SessionGamificationResult
import com.prayerquest.app.domain.model.PrayerGrade
import com.prayerquest.app.domain.model.PrayerMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.prayerquest.app.R

/**
 * Detail view for a single famous prayer (Lord's Prayer, Serenity, etc).
 *
 * Mirrors [BiblePrayerDetailScreen]: [Scaffold] with a pinned [bottomBar]
 * so the "I Prayed This Today" CTA is always visible regardless of how
 * long the prayer text is. Tapping the CTA fires the gamification hook
 * (real XP + streak; flagged as isFamousPrayer = true so famous-prayer
 * quests and badges progress), shows a summary dialog, and pops back when
 * the user taps "Done".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamousPrayerDetailScreen(
    prayerId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as PrayerQuestApplication
    val viewModel: FamousPrayerDetailViewModel = viewModel(
        factory = FamousPrayerDetailViewModel.Factory(
            prayerRepository = app.container.prayerRepository,
            gamificationRepository = app.container.gamificationRepository,
            prayerId = prayerId
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val summary by viewModel.summary.collectAsState()
    var isFavorite by remember { mutableStateOf(false) }
    // Convenience handle to the loaded prayer — null until the DB lookup
    // resolves to a real row. Used by the pinned bottom bar (only visible
    // once we actually have a prayer to log against) and the summary
    // dialog's title. Replaces the old plain-nullable StateFlow whose
    // null-forever case was the source of the infinite-spinner bug when
    // a bad id was navigated to.
    val loadedPrayer = (uiState as? FamousPrayerDetailViewModel.UiState.Loaded)?.prayer

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.library_prayer)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { isFavorite = !isFavorite },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = stringResource(R.string.library_favorite),
                            tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                                text = stringResource(R.string.library_you_ve_prayed_this),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.library_x_times, current.userPrayedCount),
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
                                stringResource(R.string.library_i_prayed_this_today),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        // Three-branch state machine:
        //  - Loading: first DB lookup hasn't resolved yet → spinner
        //  - NotFound: lookup resolved to null (bad id, dropped row, stale
        //    deep link) → explicit error screen with a Back button so the
        //    user isn't stuck staring at a spinner forever. This was the
        //    infinite-wheel bug users reported on certain library prayers.
        //  - Loaded: render the prayer as before
        when (val state = uiState) {
            FamousPrayerDetailViewModel.UiState.Loading -> {
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
            FamousPrayerDetailViewModel.UiState.NotFound -> {
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
                            text = stringResource(R.string.library_prayer_not_found),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = stringResource(R.string.library_we_couldn_t_load_this_prayer_it_may_have_been_remo),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = onNavigateBack) { Text(stringResource(R.string.common_go_back)) }
                    }
                }
                return@Scaffold
            }
            is FamousPrayerDetailViewModel.UiState.Loaded -> Unit  // fall through
        }
        val current = (uiState as FamousPrayerDetailViewModel.UiState.Loaded).prayer

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            item {
                // Title / author / date / category — plain Column with
                // explicit spacedBy so nothing can overlap the title, and
                // the category chip drops down to its own row below the
                // date line instead of getting layered onto the header.
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = current.title,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    if (current.author.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.library_by_x_2, current.author),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (current.dateComposed.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.library_composed_x, current.dateComposed),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = FontStyle.Italic
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
                                    contentDescription = stringResource(R.string.common_tag),
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .height(28.dp)
                        )
                    }
                }
            }

            item { HorizontalDivider() }

            item {
                // Prayer text
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = current.text,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    )
                }
            }

            if (current.source.isNotBlank()) {
                item {
                    Text(
                        text = stringResource(R.string.library_source_x, current.source),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // Summary-and-pop dialog. Shared implementation lives in
    // BiblePrayerDetailScreen (same package, internal visibility).
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

class FamousPrayerDetailViewModel(
    private val prayerRepository: PrayerRepository,
    private val gamificationRepository: GamificationRepository,
    private val prayerId: String
) : ViewModel() {

    /**
     * Three-state load result — replaces the old `StateFlow<FamousPrayer?>`
     * whose single nullable value couldn't distinguish "still loading" from
     * "lookup finished, nothing found". That ambiguity caused the infinite-
     * spinner bug on library prayers whose ids didn't resolve to a real
     * DB row.
     */
    sealed interface UiState {
        data object Loading : UiState
        data class Loaded(val prayer: FamousPrayer) : UiState
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
            val row = prayerRepository.getFamousPrayer(prayerId)
            _uiState.value = if (row != null) UiState.Loaded(row) else UiState.NotFound
        }
    }

    fun onPrayedThisToday() {
        viewModelScope.launch {
            // Guard: only log if we actually loaded a prayer. Prevents a
            // tap during the NotFound error screen (if the bottom bar ever
            // re-appeared there) from running the increment against a
            // non-existent id.
            if (_uiState.value !is UiState.Loaded) return@launch
            prayerRepository.incrementFamousPrayerPrayedCount(prayerId)
            // Fire the standard session-complete hook so XP, streak, and
            // daily quests all register. Famous-prayer flagged so quests
            // like "pray 5 famous prayers" advance and badges like
            // Lord's-Prayer-50× / 100× Warrior progress through the
            // usage-tracking path (see GamificationRepository).
            val result = gamificationRepository.onPrayerSessionCompleted(
                mode = PrayerMode.LECTIO_DIVINA,
                grade = PrayerGrade.GOOD,
                durationSeconds = 60,
                itemsPrayed = 1,
                isFamousPrayer = true,
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
            return FamousPrayerDetailViewModel(
                prayerRepository,
                gamificationRepository,
                prayerId
            ) as T
        }
    }
}
