package com.prayerquest.app.ui.crisis

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prayerquest.app.domain.content.BreathPrayerLibrary
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource
import com.prayerquest.app.R

/**
 * Restricted Breath Prayer variant for Crisis Prayer Mode (DD §3.10).
 *
 * Differences from the regular [com.prayerquest.app.ui.prayer.modes.BreathPrayerMode]:
 *
 * - **Locked to the Jesus Prayer.** No prayer-variant picker. A person in
 *   acute distress should not be handed a menu; they should be handed a
 *   prayer. The Jesus Prayer is the canonical Orthodox anchor for this
 *   purpose and [BreathPrayerLibrary.default] already returns it.
 * - **Slower cadence: 4 s inhale, 6 s exhale** (vs. the default mode's own
 *   4/6 pacing — we lock it here so future tuning of the default can't
 *   accidentally speed up the crisis flow).
 * - **3-minute default session**, unchanged from the regular default but
 *   made explicit so a future DD change to the regular default doesn't
 *   alter the crisis experience.
 * - **No `onModeComplete` callback into [com.prayerquest.app.ui.prayer.PrayerSessionViewModel].**
 *   The regular flow finalizes via `PrayerSessionViewModel.onModeComplete()`
 *   → `GamificationRepository.onPrayerSessionCompleted()`. That call chain
 *   does not exist here. Leaving this screen fires
 *   [CrisisSessionFinalizer.finalizeBreathSession] — a log-only telemetry
 *   call — and pops the back stack.
 *
 * If a future change starts sharing state with the main session VM, the
 * test in `CrisisModeTest` will fail because it scans this source file for
 * gamification imports. That failure is the feature working as intended.
 */
private const val INHALE_MILLIS = 4_000L
private const val EXHALE_MILLIS = 6_000L
private const val SUGGESTED_SESSION_SECONDS = 180  // 3 minutes per DD

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrisisBreathPrayerScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Locked to the Jesus Prayer. The variable is kept `val` rather than
    // inlined so the phase-flip animations read the same instance across
    // recompositions without re-resolving `default` each frame.
    val prayer = remember { BreathPrayerLibrary.default }

    var inhaling by remember { mutableStateOf(true) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var cycleCount by remember { mutableIntStateOf(0) }

    // Breath pacing. Starts on inhale for a predictable entry.
    LaunchedEffect(Unit) {
        inhaling = true
        while (true) {
            delay(INHALE_MILLIS)
            inhaling = false
            delay(EXHALE_MILLIS)
            inhaling = true
            cycleCount++
        }
    }

    // Session timer. Drives the gentle "three minutes with the Lord" note.
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            elapsedSeconds++
        }
    }

    // Log no-XP telemetry once when the screen is disposed. Captures the
    // final counters without requiring the user to tap a Complete button —
    // leaving mid-session is fine.
    DisposableEffect(Unit) {
        onDispose {
            CrisisSessionFinalizer.finalizeBreathSession(
                elapsedSeconds = elapsedSeconds,
                breathCycles = cycleCount
            )
        }
    }

    val suggestedReached = elapsedSeconds >= SUGGESTED_SESSION_SECONDS

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.prayer_breath_prayer),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.crisis_back_to_crisis_prayer)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = prayer.tradition,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = FontStyle.Italic
            )

            Spacer(modifier = Modifier.height(4.dp))

            BreathingCircle(inhaling = inhaling)

            // Resolve the breath-cue accessibility label at the @Composable
            // level — `semantics { }` is a non-@Composable lambda and can't
            // call stringResource directly.
            val breathCueDescription = if (inhaling) {
                stringResource(R.string.crisis_breathe_in_x, prayer.inhale)
            } else {
                stringResource(R.string.crisis_breathe_out_x, prayer.exhale)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .semantics {
                        contentDescription = breathCueDescription
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (inhaling) stringResource(R.string.common_breathe_in) else stringResource(R.string.common_breathe_out),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (inhaling) prayer.inhale else prayer.exhale,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetaStat(label = stringResource(R.string.common_time), value = formatSeconds(elapsedSeconds))
                MetaStat(label = stringResource(R.string.common_breaths), value = cycleCount.toString())
                MetaStat(
                    label = stringResource(R.string.common_suggested),
                    value = formatSeconds(SUGGESTED_SESSION_SECONDS)
                )
            }

            if (suggestedReached) {
                Text(
                    text = stringResource(R.string.crisis_three_minutes_with_the_lord_you_can_stay_as_long_a),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onNavigateBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = if (suggestedReached) stringResource(R.string.crisis_done_for_now) else stringResource(R.string.common_finish_early),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun BreathingCircle(inhaling: Boolean) {
    val targetScale = if (inhaling) 1f else 0.6f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(
            durationMillis = if (inhaling) INHALE_MILLIS.toInt() else EXHALE_MILLIS.toInt(),
            easing = LinearEasing
        ),
        label = "crisis-breath-scale"
    )

    val transition = rememberInfiniteTransition(label = "crisis-breath-hue")
    val hueProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "crisis-hue"
    )

    val base = MaterialTheme.colorScheme.primary
    val accent = MaterialTheme.colorScheme.tertiary
    val circleColor = lerp(base, accent, hueProgress)

    Box(
        modifier = Modifier
            .size(240.dp)
            .scale(scale)
            .background(
                color = circleColor.copy(alpha = 0.85f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (inhaling) "IN" else "OUT",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MetaStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** M:SS format without a padded leading zero on minutes. */
private fun formatSeconds(total: Int): String {
    val minutes = total / 60
    val seconds = total % 60
    return "%d:%02d".format(minutes, seconds)
}
