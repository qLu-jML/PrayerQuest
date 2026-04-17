package com.prayerquest.app.ui.prayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
import com.prayerquest.app.data.entity.PrayerItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.prayerquest.app.PrayerQuestApplication
import com.prayerquest.app.ads.AdManager
import com.prayerquest.app.domain.model.PrayerMode
import com.prayerquest.app.ui.prayer.components.GradeBar
import com.prayerquest.app.ui.prayer.components.SessionSummary
import com.prayerquest.app.ui.theme.LocalIsPremium
import com.prayerquest.app.ui.prayer.modes.BreathPrayerMode
import com.prayerquest.app.ui.prayer.modes.DailyExamenMode
import com.prayerquest.app.ui.prayer.modes.DailyOfficeMode
import com.prayerquest.app.ui.prayer.modes.FlashPraySwipeMode
import com.prayerquest.app.ui.prayer.modes.GuidedActsMode
import com.prayerquest.app.ui.prayer.modes.IntercessionDrillMode
import com.prayerquest.app.ui.prayer.modes.LectioDivinaMode
import com.prayerquest.app.ui.prayer.modes.PrayerBeadsMode
import com.prayerquest.app.ui.prayer.modes.PrayerJournalMode
import com.prayerquest.app.ui.prayer.modes.VoiceRecordMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerSessionScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Enum name of the [PrayerMode] the user chose from the Mode Picker (DD
     * §3.1.3). When supplied, every session item runs that single mode.
     *
     * Empty string or "MIXED" = mixed session (legacy cyclic behavior — each
     * item rotates through available modes). Left as a string at the nav
     * boundary so we don't have to teach NavType about enums; parsed into a
     * [PrayerMode] inside the ViewModel factory via [PrayerMode.valueOf].
     */
    chosenModeName: String = "",
    /**
     * Optional collection context — when the session was launched from a
     * collection detail screen, this is the collection id (≥ 0). The VM
     * uses it to source prayer items from the collection instead of the
     * user's global Active list. Any negative sentinel (e.g. -1 from the
     * nav layer's "no collection" default) is normalized to null here.
     */
    collectionId: Long? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as PrayerQuestApplication
    val isPremium = LocalIsPremium.current
    val normalizedCollectionId = collectionId?.takeIf { it >= 0 }

    // Called when the user taps "Done" on the session summary. On free
    // accounts this routes through AdManager.showInterstitial which honors
    // the session-frequency + cooldown guards. Premium users skip straight
    // to onBackClick — the gate lives inside showInterstitial but we also
    // branch here so we don't even look up the host Activity when ads are
    // irrelevant.
    val finishSession: () -> Unit = remember(isPremium, onBackClick) {
        {
            val activity = context.findActivity()
            if (activity != null && !isPremium) {
                AdManager.showInterstitial(
                    activity = activity,
                    isPremium = false,
                    onDismissed = onBackClick,
                )
            } else {
                onBackClick()
            }
        }
    }
    // Keyed by the chosen mode AND the collection so switching between
    // collections (or between collection/global) creates a fresh ViewModel
    // instead of reusing the prior session's queue.
    val viewModel: PrayerSessionViewModel = viewModel(
        key = "prayer_session_${chosenModeName.ifEmpty { "MIXED" }}_col_${normalizedCollectionId ?: "none"}",
        factory = PrayerSessionViewModel.Companion.Factory(
            prayerRepository = app.container.prayerRepository,
            gamificationRepository = app.container.gamificationRepository,
            fixedMode = resolveFixedMode(chosenModeName),
            collectionId = normalizedCollectionId,
            collectionRepository = app.container.collectionRepository
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
                        onDone = finishSession,
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
            modifier = Modifier
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
    // Collection-driven session items (id > 0) vs synthetic "no items yet"
    // fallbacks (id = 0). Only surface the "Praying for" banner when there
    // are real user items to list — otherwise the banner would just echo
    // the mode's display name, which is redundant.
    val realItems = state.sessionItems.filter { it.id > 0 }

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
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.small),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        // "Praying for:" context banner — shown when the session has a real
        // list of user-created items (a collection, typically). Keeps the
        // user's actual prayer requests visible while they pray, so a mode
        // like GuidedActs or BreathPrayer feels anchored to their real
        // concerns instead of a generic template.
        //
        // Suppressed for FLASH_PRAY_SWIPE: the swipe card itself displays the
        // topic in huge centered text, so the banner is redundant AND its
        // ~60dp of vertical space pushes the card below the fold.
        if (realItems.isNotEmpty() && state.currentMode != PrayerMode.FLASH_PRAY_SWIPE) {
            PrayingForBanner(items = realItems)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Prayer mode content
        when (state.currentMode) {
            PrayerMode.GUIDED_ACTS -> {
                GuidedActsMode(
                    onModeComplete = { content ->
                        viewModel.onModeComplete(content)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            PrayerMode.VOICE_RECORD -> {
                VoiceRecordMode(
                    onModeComplete = { content ->
                        viewModel.onModeComplete(content)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            PrayerMode.PRAYER_JOURNAL -> {
                PrayerJournalMode(
                    onModeComplete = { content ->
                        viewModel.onModeComplete(content)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            PrayerMode.INTERCESSION_DRILL -> {
                IntercessionDrillMode(
                    onModeComplete = { content ->
                        viewModel.onModeComplete(content)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            PrayerMode.FLASH_PRAY_SWIPE -> {
                // When the session has real user items (typically from a
                // collection), use their titles as the swipe cards so users
                // are flash-praying their OWN list instead of our generic
                // intercession placeholders. Null / empty falls back to the
                // mode's default topics.
                FlashPraySwipeMode(
                    onModeComplete = { content ->
                        viewModel.onModeComplete(content)
                    },
                    modifier = Modifier.weight(1f),
                    topics = realItems.takeIf { it.isNotEmpty() }?.map { it.title }
                )
            }
            PrayerMode.DAILY_EXAMEN -> {
                DailyExamenMode(
                    onModeComplete = { content ->
                        viewModel.onModeComplete(content)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            PrayerMode.LECTIO_DIVINA -> {
                LectioDivinaMode(
                    onModeComplete = { content ->
                        viewModel.onModeComplete(content)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            PrayerMode.BREATH_PRAYER -> {
                BreathPrayerMode(
                    onModeComplete = { content ->
                        viewModel.onModeComplete(content)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            PrayerMode.PRAYER_BEADS -> {
                PrayerBeadsMode(
                    onModeComplete = { content ->
                        viewModel.onModeComplete(content)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            PrayerMode.DAILY_OFFICE -> {
                DailyOfficeMode(
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

/**
 * Maps the nav-string mode argument into an optional [PrayerMode]. Empty /
 * "MIXED" / unknown values fall back to null, which the ViewModel interprets
 * as "cycle through modes" (legacy mixed session).
 */
private fun resolveFixedMode(raw: String): PrayerMode? {
    if (raw.isBlank() || raw.equals("MIXED", ignoreCase = true)) return null
    return runCatching { PrayerMode.valueOf(raw) }.getOrNull()
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

/**
 * Compact "Praying for: Mom's health, Job search, …" context card shown above
 * the mode content whenever the session has real user items backing it — a
 * collection run, typically. Gives every mode (not just the topic-based ones)
 * visibility into what the user is actually praying through, without each
 * mode having to thread a prayer-items param of its own.
 *
 * Titles are joined with "•" so four short items fit on one line on most
 * phones. The container clamps at 2 lines so long collections don't push the
 * mode content off screen — users can always scroll the container above for
 * the full list if the session pulls from a 20-item collection.
 */
@Composable
private fun PrayingForBanner(items: List<PrayerItem>) {
    val joined = items.joinToString(separator = " • ") { it.title }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = if (items.size == 1) "Praying for" else "Praying for ${items.size} items",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
        Text(
            text = joined,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 2
        )
    }
}

/**
 * Walks up the ContextWrapper chain to find the hosting [Activity], which
 * Google Mobile Ads needs to render a full-screen interstitial. Returns
 * null in rare cases (e.g. hosted inside a non-Activity Context such as a
 * preview) — callers fall back to skipping the ad.
 */
private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
