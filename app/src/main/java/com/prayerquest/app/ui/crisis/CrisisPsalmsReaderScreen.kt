package com.prayerquest.app.ui.crisis

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.sp
import com.prayerquest.app.domain.content.CrisisPsalmsLibrary
import kotlinx.coroutines.delay

/**
 * Paged Psalms reader for Crisis Prayer Mode. One Psalm per card, swipe to
 * advance.
 *
 * ### Auto-advance
 * A slow auto-advance timer is available but **OFF by default** (DD §3.10).
 * The rationale: a person in acute distress may want the next Psalm to
 * surface on its own, but they should never be rushed off the current one.
 * When on, the interval is intentionally long (45 s) — longer than the
 * fastest-reading estimate in [CrisisPsalmsLibrary] so a quick reader
 * finishes before the page advances, and a slow reader still has room to
 * stop it.
 *
 * ### No XP
 * Uses [CrisisSessionFinalizer] for leaving-the-screen telemetry. No
 * gamification path fires — see the note in [CrisisPrayerScreen].
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CrisisPsalmsReaderScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val psalms = CrisisPsalmsLibrary.psalms
    val pagerState = rememberPagerState(pageCount = { psalms.size })

    var autoAdvance by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    // Highest index the user has reached — used for telemetry on leave so we
    // log "psalms read" (distinct cards viewed), not "current page".
    val maxPageSeen by remember(pagerState) {
        derivedStateOf { maxOf(pagerState.currentPage, 0) }
    }

    // Session timer for telemetry.
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            elapsedSeconds++
        }
    }

    // Opt-in auto-advance. Slow cadence (45 s) so a reader who's sitting with
    // the text never gets yanked off it. Pauses automatically when the user
    // toggles it off.
    LaunchedEffect(autoAdvance) {
        if (!autoAdvance) return@LaunchedEffect
        while (autoAdvance) {
            delay(45_000L)
            if (!autoAdvance) break
            val next = (pagerState.currentPage + 1).coerceAtMost(psalms.size - 1)
            if (next != pagerState.currentPage) {
                pagerState.animateScrollToPage(next)
            } else {
                // Hit the end — don't loop; the user explicitly picked
                // auto-advance, not auto-restart.
                autoAdvance = false
            }
        }
    }

    // On leave, log a single telemetry line — no XP path, no repository.
    DisposableEffect(Unit) {
        onDispose {
            CrisisSessionFinalizer.finalizePsalmsSession(
                elapsedSeconds = elapsedSeconds,
                psalmsRead = maxPageSeen + 1
            )
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Psalms",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Crisis Prayer"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page dots — sits above the pager so the user sees how many cards
            // there are without having to swipe to the end to find out.
            PageDots(
                total = psalms.size,
                current = pagerState.currentPage,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                PsalmCard(
                    reference = psalms[page].reference,
                    framing = psalms[page].framing,
                    text = psalms[page].text,
                    estimatedReadSeconds = psalms[page].estimatedReadSeconds
                )
            }

            AutoAdvanceToggle(
                enabled = autoAdvance,
                onToggle = { autoAdvance = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PsalmCard(
    reference: String,
    framing: String,
    text: String,
    estimatedReadSeconds: Int
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = framing,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = reference,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "About ${formatSeconds(estimatedReadSeconds)} to read slowly",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PageDots(
    total: Int,
    current: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.semantics {
            contentDescription = "Page ${current + 1} of $total"
        },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until total) {
            val active = i == current
            val color = if (active) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
            }
            Box(
                modifier = Modifier
                    .size(if (active) 10.dp else 6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun AutoAdvanceToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onToggle(!enabled) }
            .semantics {
                contentDescription = if (enabled) {
                    "Auto-advance is on. Tap to turn off."
                } else {
                    "Auto-advance is off. Tap to turn on."
                }
            },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Auto-advance",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Slowly turn the page every 45 seconds",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle
            )
        }
    }
}

// ── local helpers ─────────────────────────────────────────────

private fun formatSeconds(total: Int): String {
    val minutes = total / 60
    val seconds = total % 60
    return if (minutes == 0) "${seconds}s" else "%d:%02d".format(minutes, seconds)
}
