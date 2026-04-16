package com.prayerquest.app.ui.prayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prayerquest.app.PrayerQuestApplication
import com.prayerquest.app.domain.model.PrayerMode
import com.prayerquest.app.ui.prayer.components.GradeBar
import com.prayerquest.app.ui.prayer.components.SessionSummary
import com.prayerquest.app.ui.prayer.modes.ContemplativeSilenceMode
import com.prayerquest.app.ui.prayer.modes.FlashPraySwipeMode
import com.prayerquest.app.ui.prayer.modes.GratitudeBlastMode
import com.prayerquest.app.ui.prayer.modes.GuidedActsMode
import com.prayerquest.app.ui.prayer.modes.IntercessionDrillMode
import com.prayerquest.app.ui.prayer.modes.PrayerJournalMode
import com.prayerquest.app.ui.prayer.modes.ScriptureSoakMode
import com.prayerquest.app.ui.prayer.modes.VoiceRecordMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerSessionScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as PrayerQuestApplication
    val viewModel: PrayerSessionViewModel = viewModel(
        factory = PrayerSessionViewModel.Companion.Factory(
            prayerRepository = app.container.prayerRepository,
            gamificationRepository = app.container.gamificationRepository
        )
    )
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when (uiState) {
                        is UiState.Loading -> {
                            Text("Loading Prayer Session...")
                        }
                        is UiState.InProgress -> {
                            val inProgressState = uiState as UiState.InProgress
                            Text(
                                text = "Prayer Session · ${inProgressState.currentItemIndex + 1}/${inProgressState.totalItems}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        is UiState.Finished -> {
                            Text("Session Complete!")
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is UiState.Loading -> {
                    LoadingState()
                }
                is UiState.InProgress -> {
                    InProgressState(
                        state = state,
                        viewModel = viewModel
                    )
                }
                is UiState.Finished -> {
                    FinishedState(
                        result = state.result,
                        onPrayAgain = { viewModel.resetSession() },
                        onDone = onBackClick
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.Alignment(Alignment.Center)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Preparing your prayer session...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InProgressState(
    state: UiState.InProgress,
    viewModel: PrayerSessionViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Progress indicator
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Progress",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "${state.currentItemIndex + 1}/${state.totalItems}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            LinearProgressIndicator(
                progress = { (state.currentItemIndex + 1).toFloat() / state.totalItems },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Prayer mode content
        when (state.currentMode) {
            is PrayerMode.GuidedActs -> {
                GuidedActsMode(
                    onModeComplete = { content ->
                        viewModel.onModeComplete(content)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            is PrayerMode.VoiceRecord -> {
                VoiceRecordMode(
                    onModeComplete = { content ->
                        viewModel.onModeComplete(content)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            is PrayerMode.PrayerJournal -> {
                PrayerJournalMode(
                    onModeComplete = { content ->
                        viewModel.onModeComplete(content)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            is PrayerMode.GratitudeBlast -> {
                GratitudeBlastMode(
                    onModeComplete = { content ->
                        viewModel.onModeComplete(content)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            is PrayerMode.IntercessionDrill -> {
                IntercessionDrillMode(
                    onModeComplete = { content ->
                        viewModel.onModeComplete(content)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            is PrayerMode.ScriptureSoak -> {
                ScriptureSoakMode(
                    onModeComplete = { content ->
                        viewModel.onModeComplete(content)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            is PrayerMode.ContemplativeSilence -> {
                ContemplativeSilenceMode(
                    onModeComplete = { content ->
                        viewModel.onModeComplete(content)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            is PrayerMode.FlashPraySwipe -> {
                FlashPraySwipeMode(
                    onModeComplete = { content ->
                        viewModel.onModeComplete(content)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grade bar for the session
        if (state.showGradeBar) {
            GradeBar(
                onGradeSelected = { grade, depth ->
                    viewModel.onGradeSubmitted(grade, depth)
                },
                showDepthSlider = true
            )
        }
    }
}

@Composable
private fun FinishedState(
    result: com.prayerquest.app.data.repository.SessionGamificationResult,
    onPrayAgain: () -> Unit,
    onDone: () -> Unit
) {
    SessionSummary(
        result = result,
        onPrayAgain = onPrayAgain,
        onDone = onDone,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    )
}
