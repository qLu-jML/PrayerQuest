package com.prayerquest.app.ui.prayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import com.prayerquest.app.data.entity.PrayerItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.prayerquest.app.PrayerQuestApplication
import com.prayerquest.app.ads.AdManager
import com.prayerquest.app.domain.model.PrayerMode
import com.prayerquest.app.ui.components.PrayerPhotoAvatar
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
import androidx.compose.ui.res.stringResource
import com.prayerquest.app.R

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
            collectionRepository = app.container.collectionRepository,
            context = context
        )
    )
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when (uiState) {
                        is UiState.Loading -> {
                            Text(stringResource(R.string.prayer_loading_prayer_session))
                        }
                        is UiState.InProgress -> {
                            val inProgressState = uiState as UiState.InProgress
                            Text(
                                text = stringResource(R.string.prayer_prayer_session_x_x, inProgressState.currentItemIndex + 1, inProgressState.totalItems),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        is UiState.Finished -> {
                            Text(stringResource(R.string.prayer_session_complete))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
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
            text = stringResource(R.string.prayer_preparing_your_prayer_session),
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
    val context = LocalContext.current
    val app = context.applicationContext as PrayerQuestApplication
    val prefs = app.container.userPreferences
    val scope = rememberCoroutineScope()

    // Collection-driven session items (id > 0) vs synthetic "no items yet"
    // fallbacks (id = 0). Only surface the "Praying for" banner when there
    // are real user items to list — otherwise the banner would just echo
    // the mode's display name, which is redundant.
    val realItems = state.sessionItems.filter { it.id > 0 }

    // --- Per-mode first-run intro overlay (DD §3.1.3) ---
    // Tracks which modes the user has already seen. Hold `null` while the
    // initial value is still loading from DataStore so the overlay doesn't
    // briefly flash in / out on screens where the user has already dismissed
    // it. Also track per-session dismissals so re-entering the same mode
    // during one session doesn't re-open the sheet even before the Flow
    // roundtrips the persisted write back.
    val seenModes by prefs.seenModeTutorials.collectAsState(initial = null)
    var dismissedThisSession by remember { mutableStateOf<Set<PrayerMode>>(emptySet()) }
    val showModeIntro = seenModes != null &&
            state.currentMode !in seenModes!! &&
            state.currentMode !in dismissedThisSession

    // --- Collapsible session header strip ---
    // The "Praying for" banner is genuinely useful when starting a session,
    // but once the user is actively praying it eats vertical real estate a
    // long-form mode (Journal, Voice, Guided ACTS) would rather use for its
    // own content. A chevron toggle on the progress row lets the user
    // collapse the banner (and hide the detail list) without ever leaving
    // the session. State is session-scoped — not persisted — because the
    // meaningful default when starting any new session is "show me what I
    // committed to". If the user re-enters a session later, they see the
    // full strip again by design.
    var stripExpanded by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Progress row — tappable on the right-hand "collapse" chevron when
        // there's a banner to hide (i.e. we have real items AND the mode is
        // banner-eligible). For banner-less sessions (FlashPray or synthetic
        // items) the chevron disappears; collapsing nothing wastes a tap.
        val hasBanner = realItems.isNotEmpty() &&
                state.currentMode != PrayerMode.FLASH_PRAY_SWIPE
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
                    text = stringResource(R.string.prayer_progress),
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.prayer_x_x, state.currentItemIndex + 1, state.totalItems),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (hasBanner) {
                        IconButton(
                            onClick = { stripExpanded = !stripExpanded },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (stripExpanded) {
                                    Icons.Default.ExpandLess
                                } else {
                                    Icons.Default.ExpandMore
                                },
                                contentDescription = if (stripExpanded) {
                                    stringResource(R.string.prayer_collapse_session_header)
                                } else {
                                    stringResource(R.string.prayer_expand_session_header)
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
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
        //
        // Also suppressed when the user has manually collapsed the strip via
        // the chevron — the progress row above remains, so they still see
        // where they are in the session, just without the full item list.
        AnimatedVisibility(visible = hasBanner && stripExpanded) {
            Column {
                PrayingForBanner(items = realItems)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Prayer mode content
        when (state.currentMode) {
            PrayerMode.GUIDED_ACTS -> {
                // Topics thread into the Supplication phase prompt so the
                // fourth step explicitly names the items the user committed
                // to pray for, instead of the generic "What are your needs?"
                GuidedActsMode(
                    onModeComplete = { content ->
                        viewModel.onModeComplete(content)
                    },
                    modifier = Modifier.weight(1f),
                    topics = realItems.takeIf { it.isNotEmpty() }?.map { it.title }
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
                // Same pattern as FLASH_PRAY_SWIPE — when the session is
                // anchored on real user items (a collection, typically),
                // drill through THOSE instead of generic "Family members /
                // Missionaries / ..." placeholders. Null / empty falls
                // back to the mode's default list.
                //
                // Photo URIs are passed as a parallel list so each item's
                // Photo Prayer (DD §3.9) anchors the drill card. Falls back
                // to a monogram via PrayerPhotoAvatar when the item has none.
                IntercessionDrillMode(
                    onModeComplete = { content ->
                        viewModel.onModeComplete(content)
                    },
                    modifier = Modifier.weight(1f),
                    topics = realItems.takeIf { it.isNotEmpty() }?.map { it.title },
                    photoUris = realItems.takeIf { it.isNotEmpty() }?.map { it.photoUri },
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
                    topics = realItems.takeIf { it.isNotEmpty() }?.map { it.title },
                    photoUris = realItems.takeIf { it.isNotEmpty() }?.map { it.photoUri },
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
                // Pass topics through so the mode hides its (otherwise inert)
                // variant picker when the user is praying through a pack —
                // and so the header can name the items they committed to.
                BreathPrayerMode(
                    onModeComplete = { content ->
                        viewModel.onModeComplete(content)
                    },
                    modifier = Modifier.weight(1f),
                    topics = realItems.takeIf { it.isNotEmpty() }?.map { it.title }
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
        } // Column

        // Per-mode first-run intro overlay. Rendered LAST so it sits above
        // the session column — the overlay blocks input to everything
        // behind it until the user taps Got it, which feels right given
        // this is the first time they're seeing the mode. Dismissal marks
        // the mode as seen in DataStore AND adds it to the session-scoped
        // set so it doesn't flash back in before the Flow round-trips.
        if (showModeIntro) {
            ModeIntroOverlay(
                mode = state.currentMode,
                onDismiss = {
                    val m = state.currentMode
                    dismissedThisSession = dismissedThisSession + m
                    scope.launch { prefs.markModeTutorialSeen(m) }
                }
            )
        }
    } // Box
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
    // Photo Prayer (DD §3.9) gets a dedicated hero slot when there is exactly
    // one item with a photo — the header of every mode becomes a face/anchor
    // for the single concern the user is praying through. With multiple items
    // we show a small row of avatars instead so the banner's vertical footprint
    // stays bounded. Zero photos → text-only banner (the original look).
    val singleItemHero = items.size == 1 && !items.first().photoUri.isNullOrBlank()
    val photoItems = items.filter { !it.photoUri.isNullOrBlank() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (singleItemHero) {
            val solo = items.first()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrayerPhotoAvatar(
                    photoPath = solo.photoUri,
                    fallbackLabel = solo.title,
                    size = 56.dp,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.prayer_praying_for),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = solo.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 2
                    )
                }
            }
        } else {
            Text(
                text = if (items.size == 1) stringResource(R.string.prayer_praying_for) else stringResource(R.string.prayer_praying_for_x_items, items.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            // Row of mini avatars when any item has a photo. Capped at 5 so
            // a 20-item collection doesn't blow past the one-line budget.
            if (photoItems.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    photoItems.take(5).forEach { item ->
                        PrayerPhotoAvatar(
                            photoPath = item.photoUri,
                            fallbackLabel = item.title,
                            size = 28.dp,
                        )
                    }
                    if (photoItems.size > 5) {
                        Text(
                            text = stringResource(R.string.prayer_x, photoItems.size - 5),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.padding(start = 4.dp)
                                .align(Alignment.CenterVertically)
                        )
                    }
                }
            }
            Text(
                text = joined,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 2
            )
        }
    }
}

/**
 * Per-mode first-run introduction. Shown the first time the user engages
 * each prayer mode in a session (DD §3.1.3 — "each mode feels different;
 * users need a short primer to try new ones"). Full-screen scrim over the
 * session so the user can't interact with the mode until they've read the
 * intro or tapped "Got it". Persisted dismissal lives in
 * `UserPreferences.markModeTutorialSeen`; the ModePicker's one-shot
 * tutorial is separate.
 */
@Composable
private fun ModeIntroOverlay(
    mode: PrayerMode,
    onDismiss: () -> Unit
) {
    val intro = modeIntroContent(mode)
    val scrollState = rememberScrollState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            // Intercept any background taps so the mode UI behind can't
            // respond — the only dismissal paths are the two buttons below.
            .clickable(enabled = true, onClick = {}),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .widthIn(max = 420.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = intro.emoji,
                    fontSize = 56.sp
                )
                Text(
                    text = mode.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = mode.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = intro.howItWorks,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.common_skip))
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(stringResource(R.string.prayer_got_it))
                    }
                }
            }
        }
    }
}

/** One-shot content bundle for [ModeIntroOverlay]. */
private data class ModeIntro(
    val emoji: String,
    val howItWorks: String
)

/**
 * Mode → intro copy. Kept local to this file so adding a new mode keeps
 * the overlay content near the dispatch logic. Copy is intentionally
 * concrete ("tap a card", "speak your prayer") so the first-run user
 * knows EXACTLY what to do — the mode's `description` is already shown
 * on the sheet above this block for the higher-level "what is this".
 */
@Composable
private fun modeIntroContent(mode: PrayerMode): ModeIntro = when (mode) {
    PrayerMode.GUIDED_ACTS -> ModeIntro(
        emoji = "\uD83D\uDD4A\uFE0F", // dove
        howItWorks = stringResource(R.string.prayer_you_ll_walk_through_four_steps_adoration_confessio)
    )
    PrayerMode.VOICE_RECORD -> ModeIntro(
        emoji = "\uD83C\uDFA4",
        howItWorks = stringResource(R.string.prayer_tap_the_microphone_and_pray_out_loud_we_ll_transcr)
    )
    PrayerMode.PRAYER_JOURNAL -> ModeIntro(
        emoji = "\uD83D\uDCD6",
        howItWorks = stringResource(R.string.prayer_write_freely_about_what_s_on_your_heart_use_the_mi)
    )
    PrayerMode.INTERCESSION_DRILL -> ModeIntro(
        emoji = "\uD83D\uDE4F",
        howItWorks = stringResource(R.string.prayer_you_ll_see_one_name_or_request_at_a_time_for_30_se)
    )
    PrayerMode.FLASH_PRAY_SWIPE -> ModeIntro(
        emoji = "\u26A1",
        howItWorks = stringResource(R.string.prayer_swipe_each_card_after_you_ve_prayed_for_it_right_f)
    )
    PrayerMode.DAILY_EXAMEN -> ModeIntro(
        emoji = "\uD83D\uDD4E",
        howItWorks = stringResource(R.string.prayer_ignatius_five_step_review_of_the_day_gratitude_awa)
    )
    PrayerMode.LECTIO_DIVINA -> ModeIntro(
        emoji = "\uD83D\uDCDC",
        howItWorks = stringResource(R.string.prayer_four_movements_over_a_short_passage_of_scripture_r)
    )
    PrayerMode.BREATH_PRAYER -> ModeIntro(
        emoji = "\uD83C\uDF43",
        howItWorks = stringResource(R.string.prayer_breathe_in_on_the_first_phrase_out_on_the_second_p)
    )
    PrayerMode.PRAYER_BEADS -> ModeIntro(
        emoji = "\uD83D\uDCFF",
        howItWorks = stringResource(R.string.prayer_tap_each_bead_as_you_pray_through_it_pick_rosary_j)
    )
    PrayerMode.DAILY_OFFICE -> ModeIntro(
        emoji = "\u26EA",
        howItWorks = stringResource(R.string.prayer_fixed_hour_prayer_morning_midday_evening_or_compli)
    )
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
