package com.prayerquest.app.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prayerquest.app.PrayerQuestApplication
import com.prayerquest.app.R
import com.prayerquest.app.data.entity.NameOfGod
import com.prayerquest.app.data.repository.GamificationRepository
import com.prayerquest.app.data.repository.LibraryRepository
import com.prayerquest.app.data.repository.SessionGamificationResult
import com.prayerquest.app.domain.model.PrayerGrade
import com.prayerquest.app.domain.model.PrayerMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Detail view for a single Name of God (DD §3.12).
 *
 * Structural mirror of [FamousPrayerDetailScreen] and [BiblePrayerDetailScreen]
 * — a [Scaffold] with a pinned [bottomBar] carrying the "Pray this name"
 * CTA so the call-to-action stays visible regardless of how long the
 * meaning / Scripture blurb runs.
 *
 * Mode choice for the CTA is [PrayerMode.BREATH_PRAYER], not
 * LECTIO_DIVINA: DD §3.12 positions Names of God as a contemplative
 * surface (slow, repeated, name-as-anchor), which is exactly what Breath
 * Prayer's cadence is for. [isFamousPrayer] is intentionally `false` —
 * Names of God have their own badge track ("Names of God" family in the
 * achievement catalogue), separate from the Lord's-Prayer / famous-prayer
 * milestone path.
 *
 * Three-state UiState (Loading / Loaded / NotFound) matches the fix that
 * killed the infinite-spinner bug on FamousPrayerDetailScreen — a bad id
 * or a dropped row surfaces an explicit error screen with a Back button
 * instead of leaving the user staring at a spinner.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NameOfGodDetailScreen(
    nameId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as PrayerQuestApplication
    val viewModel: NameOfGodDetailViewModel = viewModel(
        factory = NameOfGodDetailViewModel.Factory(
            libraryRepository = app.container.libraryRepository,
            gamificationRepository = app.container.gamificationRepository,
            nameId = nameId
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val loadedName = (uiState as? NameOfGodDetailViewModel.UiState.Loaded)?.name

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.library_name_of_god)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                }
            )
        },
        bottomBar = {
            val current = loadedName
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
                                text = stringResource(
                                    R.string.library_x_times,
                                    current.userPrayedCount
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Button(
                            onClick = { viewModel.onPrayedThisName() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                stringResource(R.string.library_pray_this_name),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        when (val state = uiState) {
            NameOfGodDetailViewModel.UiState.Loading -> {
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
            NameOfGodDetailViewModel.UiState.NotFound -> {
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
                            text = stringResource(R.string.library_name_not_found),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = stringResource(R.string.library_we_couldn_t_load_this_name),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = onNavigateBack) {
                            Text(stringResource(R.string.common_go_back))
                        }
                    }
                }
                return@Scaffold
            }
            is NameOfGodDetailViewModel.UiState.Loaded -> Unit
        }
        val current = (uiState as NameOfGodDetailViewModel.UiState.Loaded).name

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = current.name,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    if (current.hebrewOrGreek.isNotBlank() &&
                        current.hebrewOrGreek != current.name
                    ) {
                        Text(
                            text = current.hebrewOrGreek,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = FontStyle.Italic
                        )
                    }
                    if (current.scriptureReference.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = current.scriptureReference,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item { HorizontalDivider() }

            // Meaning block — the core contemplative content.
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.library_meaning),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = current.meaning,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                        )
                    }
                }
            }

            // Scripture text — hidden when the importer didn't carry it
            // (older JSON entries, optional field).
            if (current.description.isNotBlank()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.library_scripture),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = current.description,
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                            )
                        }
                    }
                }
            }

            // Gentle contemplative prompt — nudges users into Breath
            // Prayer framing before they tap the CTA. Keeps the page
            // pastoral without a heavy instruction wall.
            item {
                Text(
                    text = stringResource(R.string.library_name_of_god_breath_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // Reuse the shared summary dialog from BiblePrayerDetailScreen (same
    // package, internal visibility). Keeps the "logged +N XP" UX identical
    // across every library-level "I prayed this" surface.
    summary?.let { result ->
        PrayedSummaryDialog(
            result = result,
            prayerTitle = loadedName?.name.orEmpty(),
            onDone = {
                viewModel.clearSummary()
                onNavigateBack()
            }
        )
    }
}

class NameOfGodDetailViewModel(
    private val libraryRepository: LibraryRepository,
    private val gamificationRepository: GamificationRepository,
    private val nameId: String
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data class Loaded(val name: NameOfGod) : UiState
        data object NotFound : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _summary = MutableStateFlow<SessionGamificationResult?>(null)
    val summary: StateFlow<SessionGamificationResult?> = _summary.asStateFlow()

    init {
        reload()
    }

    private fun reload() {
        viewModelScope.launch {
            // The DAO's observeAll() is overkill for a one-shot detail
            // lookup; the existing getById() on the DAO is the direct
            // path. The repository doesn't expose it today because no
            // caller needed a suspend getter — the detail screen is the
            // first. We reach through the DAO via a tiny repository
            // passthrough rather than extend the public API surface now.
            val row = libraryRepository.getNameOfGodById(nameId)
            _uiState.value = if (row != null) UiState.Loaded(row) else UiState.NotFound
        }
    }

    /**
     * "Pray this name" — logs a contemplative Breath Prayer session and
     * increments the name's prayed count. See the class-level KDoc for
     * why BREATH_PRAYER + isFamousPrayer=false is the right shape.
     */
    fun onPrayedThisName() {
        viewModelScope.launch {
            if (_uiState.value !is UiState.Loaded) return@launch
            libraryRepository.incrementNameOfGodPrayedCount(nameId)
            val result = gamificationRepository.onPrayerSessionCompleted(
                mode = PrayerMode.BREATH_PRAYER,
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
        private val libraryRepository: LibraryRepository,
        private val gamificationRepository: GamificationRepository,
        private val nameId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NameOfGodDetailViewModel(
                libraryRepository,
                gamificationRepository,
                nameId
            ) as T
        }
    }
}
