package com.prayerquest.app.ui.crisis

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Crisis Prayer Mode (DD §3.10).
 *
 * Entry is a slow, calming fade-in — the user taps "In distress?" on Home and
 * the screen resolves rather than punching in. Three tiles route to the three
 * sub-experiences (Psalms, Breath Prayer, Resources). A subtle "Return home"
 * link at the bottom gives a low-effort exit.
 *
 * ### Gamification posture
 * This screen is the single explicit **NO-XP path** in the app. Nothing on
 * this screen calls [com.prayerquest.app.data.repository.GamificationRepository],
 * nothing increments streaks, nothing advances quests. Finalization goes
 * through [CrisisSessionFinalizer] — a log-only object with no repository
 * dependencies. The guarantee is enforced by:
 *   1. Design: this file has no `data.repository` import.
 *   2. Wiring: the Crisis breath variant lives in [CrisisBreathPrayerScreen]
 *      and does NOT share a ViewModel with the main prayer session.
 *   3. Test: `CrisisModeTest` asserts no gamification import appears in any
 *      `ui/crisis/*.kt` source file.
 *
 * If any one of those three drifts, the mode has regressed per DD §3.10. Do
 * not patch the test — fix the drift.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CrisisPrayerScreen(
    onOpenPsalms: () -> Unit,
    onOpenBreath: () -> Unit,
    onOpenResources: () -> Unit,
    onReturnHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Calming fade-in. We drive this from a local state rather than an
    // AnimatedVisibility parent so the Back-from-sub-screen re-entry also
    // plays the fade — otherwise re-entering from Psalms would snap on.
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
        CrisisSessionFinalizer.logCrisisEntered()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 900, easing = LinearOutSlowInEasing)
        )
    ) {
        CrisisContent(
            onOpenPsalms = onOpenPsalms,
            onOpenBreath = onOpenBreath,
            onOpenResources = onOpenResources,
            onReturnHome = onReturnHome,
            modifier = modifier
        )
    }
}

@Composable
private fun CrisisContent(
    onOpenPsalms: () -> Unit,
    onOpenBreath: () -> Unit,
    onOpenResources: () -> Unit,
    onReturnHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    // The surface color is pulled from the theme rather than hard-coded so
    // dark mode stays calm too. The gentle tonal elevation gives the screen
    // a "held" feel without introducing stronger colored accents that would
    // read as urgent / alarming.
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── TOP ──────────────────────────────────────
            CrisisHeader()

            Spacer(modifier = Modifier.height(4.dp))

            // ── MIDDLE (three tiles) ─────────────────────
            CrisisTile(
                title = "Psalms that breathe with you",
                subtitle = "Short passages to read slowly. Swipe to advance.",
                emoji = "📖",
                onClick = onOpenPsalms
            )
            CrisisTile(
                title = "Paced breath prayer",
                subtitle = "The Jesus Prayer, at a slower cadence. 3 minutes.",
                emoji = "🕊️",
                onClick = onOpenBreath
            )
            CrisisTile(
                title = "Crisis resources",
                subtitle = "If you need to reach a person, a list of helplines.",
                emoji = "☎️",
                onClick = onOpenResources
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── BOTTOM (subtle return link) ───────────────
            ReturnHomeLink(onReturnHome = onReturnHome)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CrisisHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "You are not alone.",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Text(
            text = "God is with you.",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Take your time. Nothing here is scored.",
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CrisisTile(
    title: String,
    subtitle: String,
    emoji: String,
    onClick: () -> Unit
) {
    // Tap target is deliberately > 48dp tall — the screen is reached when the
    // user is already in distress, so fingers may be imprecise. The min
    // height lives on the Card itself rather than a child so the whole card
    // surface is the hit target.
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 96.dp)
            .semantics { contentDescription = "$title. $subtitle" },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "$emoji  $title",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ReturnHomeLink(onReturnHome: () -> Unit) {
    // Subtle, low-profile — the user should feel the exit is always there
    // without it pulling the eye. Kept at an accessible 48dp tap target via
    // the padding on the clickable Box.
    Box(
        modifier = Modifier
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onReturnHome)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics { contentDescription = "Return to Home" }
    ) {
        Text(
            text = "← Return home",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}
