package com.prayerquest.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prayerquest.app.PrayerQuestApplication
import com.prayerquest.app.data.entity.PrayerItem
import com.prayerquest.app.data.repository.PrayerRepository
import com.prayerquest.app.ui.theme.SuccessGreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.res.stringResource
import com.prayerquest.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnsweredPrayerDetailScreen(
    prayerId: Long,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as PrayerQuestApplication
    val viewModel: AnsweredPrayerDetailViewModel = viewModel(
        // VM key includes prayerId so swapping between two answered prayers
        // in the same back stack composes a fresh VM rather than reusing
        // the first one's state.
        key = "answered-$prayerId",
        factory = AnsweredPrayerDetailViewModel.Factory(
            app.container.prayerRepository,
            prayerId
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    // Flatten the sealed state to the legacy `prayer` nullable so the existing
    // body below (which threads `prayer!!` in several places) keeps compiling.
    // Null means "still loading" OR "not found" — the Box below the body
    // disambiguates by reading uiState directly.
    val prayer = (uiState as? AnsweredPrayerDetailViewModel.UiState.Loaded)?.prayer

    if (prayer != null && prayer!!.status == PrayerItem.STATUS_ANSWERED) {
        val dateFormat = SimpleDateFormat(stringResource(R.string.library_mmmm_dd_yyyy), Locale.getDefault())
        val answeredAtMs = prayer!!.answeredAt
        val answeredDate = if (answeredAtMs != null) {
            dateFormat.format(Date(answeredAtMs))
        } else {
            stringResource(R.string.library_recently)
        }

        Column(modifier = modifier.fillMaxSize()) {
            // Top bar
            TopAppBar(
                title = { Text(stringResource(R.string.library_prayer_answered)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Prayer title and answered status
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = prayer!!.title,
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.weight(1f)
                            )
                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(
                                        stringResource(R.string.library_answered),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SuccessGreen
                                    )
                                },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }

                item {
                    // Timeline event
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(40.dp),
                            shape = MaterialTheme.shapes.small,
                            color = SuccessGreen
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✓", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.library_prayer_answered),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = answeredDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                // Original request details
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.library_what_you_prayed_for),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (prayer!!.description.isNotEmpty()) {
                                    Text(
                                        text = prayer!!.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                // Testimony
                if (!prayer!!.testimonyText.isNullOrEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.library_your_testimony),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = prayer!!.testimonyText!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                                )
                            }
                        }
                    }
                }

                // Photo section
                if (!prayer!!.testimonyPhotoUri.isNullOrEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.library_evidence_photo),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        stringResource(R.string.library_photo_x, prayer!!.testimonyPhotoUri),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Voice note section
                if (!prayer!!.testimonyVoiceUri.isNullOrEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.library_voice_note),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.playVoiceNote() },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Text("▶", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Text(
                                        stringResource(R.string.library_tap_to_play_your_testimony),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    } else {
        // Loading or not-found state. Previously this showed an infinite
        // spinner when `prayer` was null because `prayer` came from an
        // `emptyFlow()` that never emitted — so we never transitioned out
        // of the null branch. Now we disambiguate via the sealed uiState:
        //  - Loading: show spinner
        //  - NotFound or Loaded-but-not-ANSWERED: show explicit error
        Box(
            modifier = modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            when (uiState) {
                AnsweredPrayerDetailViewModel.UiState.Loading -> CircularProgressIndicator()
                else -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.library_prayer_not_found),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = stringResource(R.string.library_we_couldn_t_load_this_answered_prayer_it_may_have),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onNavigateBack) { Text(stringResource(R.string.common_go_back)) }
                }
            }
        }
    }
}

class AnsweredPrayerDetailViewModel(
    private val prayerRepository: PrayerRepository,
    private val prayerId: Long
) : ViewModel() {

    /**
     * Three-state load result. Previously the VM exposed an `emptyFlow()`
     * for the prayer, which never emitted — so the screen's "null means
     * loading" check held forever, and every tap into an answered prayer
     * showed an infinite spinner. Now we actually observe the row via
     * the repository and surface a proper NotFound state when the id
     * doesn't resolve.
     */
    sealed interface UiState {
        data object Loading : UiState
        data class Loaded(val prayer: PrayerItem) : UiState
        data object NotFound : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            prayerRepository.observeItem(prayerId).collect { row ->
                _uiState.value = if (row != null) UiState.Loaded(row) else UiState.NotFound
            }
        }
    }

    fun playVoiceNote() {
        viewModelScope.launch {
            // Would implement voice playback here
        }
    }

    class Factory(
        private val prayerRepository: PrayerRepository,
        private val prayerId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AnsweredPrayerDetailViewModel(prayerRepository, prayerId) as T
        }
    }
}
