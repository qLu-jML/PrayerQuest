package com.prayerquest.app.ui.prayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prayerquest.app.PrayerQuestApplication
import com.prayerquest.app.domain.model.PrayerMode
import com.prayerquest.app.domain.model.Tradition
import com.prayerquest.app.domain.model.isVisibleFor
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.prayerquest.app.R

/**
 * The "Netflix of Prayer Modes" (DD §3.1.3). Replaces the old cycle-through-
 * all-modes behavior with an explicit up-front pick: user lands here from
 * "Start Prayer" on Home or the Pray tab, chooses a single mode, and the
 * PrayerSessionScreen runs that mode for the session.
 *
 * Shelves are arranged by [PrayerMode.Category]:
 *  - Quick   (under 3 min)
 *  - Guided  (structured frameworks)
 *  - Expressive (voice / journaling / intercession)
 *  - Traditional (liturgical — Daily Office, Rosary)
 *
 * Tradition gating: modes whose [PrayerMode.nativeTraditions] don't overlap
 * the user's [UserPreferences.enabledTraditions] are collapsed behind an
 * "Explore more traditions" footer. Per-mode manual overrides (disabledModes)
 * hide individual modes regardless of shelf — handled in Settings, not here.
 * This screen's single responsibility is picking.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModePickerScreen(
    onModePicked: (PrayerMode) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Back-button handler. Null means no back arrow is rendered — use this
     * when the picker is acting as a bottom-nav tab root (the Pray tab),
     * where bottom-bar navigation is the user's way out, not a back arrow.
     */
    onBackClick: (() -> Unit)? = null
) {
    val app = LocalContext.current.applicationContext as PrayerQuestApplication
    val prefs = app.container.userPreferences
    val scope = rememberCoroutineScope()

    val enabledTraditions by prefs.enabledTraditions
        .collectAsState(initial = Tradition.DEFAULT)
    val disabledModes by prefs.disabledModes
        .collectAsState(initial = emptySet())

    // Tutorial state. `null` while the flag is loading so the overlay doesn't
    // briefly flash on screens where the user has already seen it.
    val hasSeenTutorialInitial: Boolean? = null
    val hasSeenTutorial by prefs.hasSeenPrayerModeTutorial
        .collectAsState(initial = hasSeenTutorialInitial)
    // Local override — once the user dismisses or finishes the walkthrough we
    // flip this so the overlay disappears immediately, without waiting for the
    // DataStore write + Flow to round-trip.
    var tutorialDismissed by remember { mutableStateOf(false) }
    val showTutorial = hasSeenTutorial == false && !tutorialDismissed

    // Bucket all modes into: visible (respect tradition + override) vs hidden
    // (tradition-opted-out AND user hasn't force-enabled it).
    val visibleModes = remember(enabledTraditions, disabledModes) {
        PrayerMode.entries.filter {
            it.isVisibleFor(enabledTraditions) && it !in disabledModes
        }
    }
    val hiddenTraditionModes = remember(enabledTraditions, disabledModes) {
        PrayerMode.entries.filter {
            !it.isVisibleFor(enabledTraditions) && it !in disabledModes
        }
    }

    // "Explore more traditions" footer expansion state — local to this
    // screen (not persisted; it's a browsing action, not a preference).
    var exploreExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.common_start_prayer)) },
                navigationIcon = {
                    // Only render a back arrow when the caller actually has a
                    // "back" destination. When the picker is the Pray tab root,
                    // onBackClick is null and we leave the slot empty so the
                    // TopAppBar title aligns at the start edge as usual.
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.common_back)
                            )
                        }
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { HeaderBlurb() }

            // Visible shelves — one per category, only if any mode is visible
            PrayerMode.Category.entries.forEach { category ->
                val modesInCategory = visibleModes.filter { it.category == category }
                if (modesInCategory.isNotEmpty()) {
                    item {
                        Shelf(
                            title = shelfTitle(category),
                            modes = modesInCategory,
                            onModePicked = onModePicked
                        )
                    }
                }
            }

            // Footer: collapsed access to opted-out traditional modes.
            if (hiddenTraditionModes.isNotEmpty()) {
                item {
                    ExploreMoreTraditionsFooter(
                        expanded = exploreExpanded,
                        onToggle = { exploreExpanded = !exploreExpanded },
                        hiddenModes = hiddenTraditionModes,
                        onModePicked = onModePicked
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

        // Full-screen first-time walkthrough. Dim the scaffold behind it and
        // intercept all touches so the user can only advance/dismiss via the
        // overlay's own buttons. Dismissal flips both the local toggle (so the
        // overlay disappears right now) AND persists the flag (so we don't
        // show it again on next launch).
        if (showTutorial) {
            PrayerModeTutorialOverlay(
                onDismiss = {
                    tutorialDismissed = true
                    scope.launch { prefs.setHasSeenPrayerModeTutorial(true) }
                }
            )
        }
    }
}

/**
 * Full-screen walkthrough shown the first time the user lands on the Mode
 * Picker. Four steps explain what prayer modes are, how the shelves work,
 * and that switching between modes is always available. Both "Skip" and
 * "Got it" paths persist the seen flag so it never reappears.
 */
@Composable
private fun PrayerModeTutorialOverlay(
    onDismiss: () -> Unit
) {
    val steps = tutorialSteps()
    var stepIndex by remember { mutableIntStateOf(0) }
    val current = steps[stepIndex]
    val isLast = stepIndex == steps.lastIndex

    // Full-screen scrim. Semi-transparent so the picker subtly shows through,
    // reinforcing that the tutorial is about that screen. `clickable` with a
    // no-op intercepts any tap that isn't on our buttons so the scaffold
    // behind can't be interacted with accidentally.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .clickable(enabled = true, onClick = {}),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar — Skip aligned to the right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.common_skip),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // Center content — emoji, title, body
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = current.emoji,
                    fontSize = 72.sp
                )
                Text(
                    text = current.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = current.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }

            // Bottom — page dots + primary action
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.forEachIndexed { index, _ ->
                        val active = index == stepIndex
                        Box(
                            modifier = Modifier
                                .size(if (active) 10.dp else 8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (active) Color.White
                                    else Color.White.copy(alpha = 0.35f)
                                )
                        )
                    }
                }
                Button(
                    onClick = {
                        if (isLast) onDismiss() else stepIndex++
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (isLast) stringResource(R.string.prayer_got_it_let_s_pray) else stringResource(R.string.common_next),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (stepIndex > 0) {
                    TextButton(onClick = { stepIndex-- }) {
                        Text(
                            text = stringResource(R.string.common_back),
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                } else {
                    // Reserve the same vertical space so the layout doesn't
                    // jump when the Back button appears on step 2+.
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

/**
 * One frame of the first-time walkthrough. Kept as a tiny data holder so
 * [tutorialSteps] reads like a prose outline rather than deeply nested
 * Composable calls.
 */
private data class TutorialStep(
    val emoji: String,
    val title: String,
    val body: String
)

@Composable
private fun tutorialSteps(): List<TutorialStep> = listOf(
    TutorialStep(
        emoji = "🙏",
        title = stringResource(R.string.prayer_welcome_to_prayer_modes),
        body = stringResource(R.string.prayer_each_mode_is_a_different_way_to_pray_quick_breath)
    ),
    TutorialStep(
        emoji = "📚",
        title = stringResource(R.string.prayer_shelves_by_style),
        body = stringResource(R.string.prayer_modes_are_grouped_into_quick_guided_expressive_and)
    ),
    TutorialStep(
        emoji = "🔄",
        title = stringResource(R.string.prayer_switch_anytime),
        body = stringResource(R.string.prayer_there_s_no_wrong_choice_start_in_one_mode_today_tr)
    ),
    TutorialStep(
        emoji = "✨",
        title = stringResource(R.string.prayer_you_re_ready),
        body = stringResource(R.string.prayer_tap_any_mode_card_to_begin_you_ll_pick_which_praye)
    )
)

@Composable
private fun HeaderBlurb() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.prayer_how_will_you_pray_today),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.prayer_pick_any_mode_you_can_switch_anytime),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * One horizontal shelf — category title + horizontally-scrolling row of
 * ModeCards. Matches the Netflix visual pattern: title left-aligned, cards
 * spill off the right edge so the user knows there's more to scroll.
 */
@Composable
private fun Shelf(
    title: String,
    modes: List<PrayerMode>,
    onModePicked: (PrayerMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(modes.size) { index ->
                val mode = modes[index]
                ModeCard(
                    mode = mode,
                    onClick = { onModePicked(mode) }
                )
            }
        }
    }
}

/**
 * 220dp-wide fixed card so a full shelf shows ~1.5 cards on a typical phone
 * (visual cue that there's more). Shorter description = fewer line wraps at
 * this width; we truncate via maxLines rather than ellipsize mid-word.
 */
@Composable
private fun ModeCard(
    mode: PrayerMode,
    onClick: () -> Unit
) {
    // Use Card's dedicated onClick overload rather than a .clickable modifier —
    // Material3 Card consumes touches internally to drive its ripple/elevation
    // behavior, and .clickable on the outer modifier can get swallowed before
    // the tap reaches our handler. The onClick param is the canonical wiring.
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(220.dp)
            .heightIn(min = 140.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Mode emoji/icon — simple for now; Sprint 6 ships custom icon set
            Text(
                text = modeEmoji(mode),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = mode.displayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )
            Text(
                text = mode.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3
            )
            Spacer(Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(min = 44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.prayer_x_xp, mode.baseXp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = modeDurationHint(mode),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Footer that shows modes the user's tradition selection would normally hide.
 * Tappable header expands an inline list of the opted-out modes so users can
 * still reach them for occasional exploration without the Mode Picker
 * defaulting to "every shelf for every tradition" (which gets overwhelming).
 */
@Composable
private fun ExploreMoreTraditionsFooter(
    expanded: Boolean,
    onToggle: () -> Unit,
    hiddenModes: List<PrayerMode>,
    onModePicked: (PrayerMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onToggle() }
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.prayer_explore_more_traditions),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.prayer_prayer_practices_from_traditions_you_didn_t_select),
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) stringResource(R.string.prayer_collapse) else stringResource(R.string.prayer_expand),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (expanded) {
            Spacer(Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) {
                items(hiddenModes.size) { index ->
                    val mode = hiddenModes[index]
                    ModeCard(
                        mode = mode,
                        onClick = { onModePicked(mode) }
                    )
                }
            }
        }
    }
}

@Composable
private fun shelfTitle(category: PrayerMode.Category): String = when (category) {
    PrayerMode.Category.QUICK -> stringResource(R.string.prayer_quick_under_3_min)
    PrayerMode.Category.GUIDED -> stringResource(R.string.prayer_guided)
    PrayerMode.Category.EXPRESSIVE -> stringResource(R.string.prayer_expressive)
    PrayerMode.Category.TRADITIONAL -> stringResource(R.string.prayer_traditional)
}

/**
 * Lightweight emoji-per-mode map. Sprint 6 swaps these for a custom icon set
 * matching the Material theming — emoji keeps the shelves visually warm
 * without shipping any extra drawables in Sprint 3.
 */
private fun modeEmoji(mode: PrayerMode): String = when (mode) {
    PrayerMode.FLASH_PRAY_SWIPE -> "⚡"
    PrayerMode.BREATH_PRAYER -> "🌬️"
    PrayerMode.INTERCESSION_DRILL -> "🤝"
    PrayerMode.GUIDED_ACTS -> "🙏"
    PrayerMode.DAILY_EXAMEN -> "🌙"
    PrayerMode.LECTIO_DIVINA -> "📖"
    PrayerMode.VOICE_RECORD -> "🎙️"
    PrayerMode.PRAYER_JOURNAL -> "📔"
    PrayerMode.PRAYER_BEADS -> "📿"
    PrayerMode.DAILY_OFFICE -> "⛪"
}

/**
 * Rough duration hint shown on each mode card. These are intentionally
 * fuzzy — actual session length is user-controlled. Sprint 4 will track
 * median session duration per mode and adjust these based on real usage.
 */
@Composable
private fun modeDurationHint(mode: PrayerMode): String = when (mode) {
    PrayerMode.FLASH_PRAY_SWIPE -> stringResource(R.string.prayer_1_3_min)
    PrayerMode.BREATH_PRAYER -> stringResource(R.string.prayer_3_5_min)
    PrayerMode.INTERCESSION_DRILL -> stringResource(R.string.prayer_3_5_min)
    PrayerMode.GUIDED_ACTS -> stringResource(R.string.prayer_5_10_min)
    PrayerMode.DAILY_EXAMEN -> stringResource(R.string.prayer_5_10_min)
    PrayerMode.LECTIO_DIVINA -> stringResource(R.string.prayer_10_15_min)
    PrayerMode.VOICE_RECORD -> stringResource(R.string.prayer_3_10_min)
    PrayerMode.PRAYER_JOURNAL -> stringResource(R.string.prayer_5_15_min)
    PrayerMode.PRAYER_BEADS -> stringResource(R.string.prayer_5_15_min)
    PrayerMode.DAILY_OFFICE -> stringResource(R.string.prayer_5_10_min)
}
