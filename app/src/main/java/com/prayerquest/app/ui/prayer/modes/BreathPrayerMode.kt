package com.prayerquest.app.ui.prayer.modes

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prayerquest.app.domain.content.BreathPrayer
import com.prayerquest.app.domain.content.BreathPrayerLibrary
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource
import com.prayerquest.app.R

/**
 * Breath Prayer — paced inhale-exhale animation with a short repeatable
 * prayer (DD §3.4 item 8). Default is the Jesus Prayer: "Lord Jesus Christ,
 * Son of God" on the inhale, "have mercy on me, a sinner" on the exhale.
 *
 * The animation is a single pulsing circle that expands for [inhaleMillis]
 * and contracts for [exhaleMillis] — the phrase text swaps under the circle
 * as the phase flips so the user has a visual anchor and a semantic one.
 * After [sessionDurationSeconds] seconds the Complete button becomes
 * emphasized; the user can also tap "Finish early" at any moment (short
 * sessions are just as valid — streak-saving on busy days per DD line 78).
 */
private const val DEFAULT_INHALE_MILLIS = 4000L
private const val DEFAULT_EXHALE_MILLIS = 6000L
private const val DEFAULT_SESSION_SECONDS = 180  // 3 minutes per DD

/**
 * @param topics Optional list of prayer-item titles the session is
 *   iterating through (e.g. a selected pack or collection). When
 *   non-empty we HIDE the breath-prayer variant picker: the user already
 *   committed to "Breath Prayer" when they started this session, and
 *   forcing them to also choose a variant ("Jesus Prayer", "Taizé", etc.)
 *   on top of a pack they picked felt like a dead control in user
 *   testing — the banner above the mode already says "Praying for N
 *   items" and the picker below it looked inert. Hiding it removes the
 *   confusion. For solo breath-prayer runs (topics = null) the picker
 *   stays, because variant choice IS the only interesting control.
 */
@Composable
fun BreathPrayerMode(
    onModeComplete: (String) -> Unit,
    modifier: Modifier = Modifier,
    topics: List<String>? = null
) {
    val hasTopicContext = !topics.isNullOrEmpty()
    var selected by remember { mutableStateOf(BreathPrayerLibrary.default) }
    var pickerOpen by remember { mutableStateOf(false) }
    var inhaling by remember { mutableStateOf(true) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var cycleCount by remember { mutableIntStateOf(0) }

    // Breath pacing. When the user picks a new prayer we reset the phase to
    // inhale so the transition feels fresh rather than mid-exhale.
    LaunchedEffect(selected.id) {
        inhaling = true
        while (true) {
            delay(DEFAULT_INHALE_MILLIS)
            inhaling = false
            delay(DEFAULT_EXHALE_MILLIS)
            inhaling = true
            cycleCount++
        }
    }

    // Session timer — drives the "your suggested time is up" message / button
    // emphasis transition. Runs continuously; user ends when ready.
    LaunchedEffect(selected.id) {
        elapsedSeconds = 0
        cycleCount = 0
        while (true) {
            delay(1000)
            elapsedSeconds++
        }
    }

    val suggestedReached = elapsedSeconds >= DEFAULT_SESSION_SECONDS

    // Resolve summary strings at @Composable level — onClick is plain
    // `() -> Unit`. These recompose with the underlying state anyway, so we
    // just hoist the stringResource calls out of the lambda.
    val breathSummaryTitle = stringResource(R.string.prayer_modes_breath_prayer_x, selected.title)
    val breathSummaryInhale = stringResource(R.string.prayer_modes_inhale_x, selected.inhale)
    val breathSummaryExhale = stringResource(R.string.prayer_modes_exhale_x, selected.exhale)
    val breathSummaryStats = stringResource(
        R.string.prayer_modes_x_x_breath_cycles,
        formatSeconds(elapsedSeconds),
        cycleCount
    )

    PrayerModeScaffold(
        modifier = modifier,
        contentArrangement = Arrangement.spacedBy(16.dp),
        contentHorizontalAlignment = Alignment.CenterHorizontally,
        action = {
            // Pinned Finish button. Breath Prayer is a timer-driven session
            // where the user ends when ready — never push this below the fold.
            Button(
                onClick = {
                    onModeComplete(
                        buildString {
                            appendLine(breathSummaryTitle)
                            appendLine(breathSummaryInhale)
                            appendLine(breathSummaryExhale)
                            appendLine()
                            appendLine(breathSummaryStats)
                        }.trim()
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = if (suggestedReached) stringResource(R.string.prayer_modes_complete) else stringResource(R.string.common_finish_early),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    ) {
        // Prayer picker — compact dropdown at the top, shows tradition label.
        // Hidden when the session is praying through a pack/collection (the
        // banner above the mode already commits the user to "Breath Prayer";
        // a variant picker on top of that read as a dead control). For solo
        // runs the picker is the only interesting control, so it stays.
        if (!hasTopicContext) {
            BreathPrayerPicker(
                selected = selected,
                expanded = pickerOpen,
                onExpand = { pickerOpen = true },
                onDismiss = { pickerOpen = false },
                onSelect = {
                    selected = it
                    pickerOpen = false
                }
            )

            Text(
                text = selected.tradition,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = FontStyle.Italic
            )
        } else {
            // When iterating topics, show the current focus instead — the
            // user is breath-praying _for_ these items, so naming them keeps
            // the screen connected to what they committed to.
            Text(
                text = stringResource(
                    R.string.prayer_modes_breathing_for_topics_jointostring,
                    topics.orEmpty().joinToString(" · ").take(140)
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center
            )
        }

        // Breathing circle
        BreathingCircle(inhaling = inhaling)

        // Phase text — swaps on every breath flip
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (inhaling) stringResource(R.string.common_breathe_in) else stringResource(R.string.common_breathe_out),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (inhaling) selected.inhale else selected.exhale,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }

        // Session meta — elapsed time + cycle count
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MetaStat(
                label = stringResource(R.string.common_time),
                value = formatSeconds(elapsedSeconds)
            )
            MetaStat(
                label = stringResource(R.string.common_breaths),
                value = cycleCount.toString()
            )
            MetaStat(
                label = stringResource(R.string.common_suggested),
                value = formatSeconds(DEFAULT_SESSION_SECONDS)
            )
        }

        // Gentle encouragement once the suggested time has been reached
        if (suggestedReached) {
            Text(
                text = stringResource(R.string.prayer_modes_three_minutes_with_the_lord_well_done),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BreathingCircle(inhaling: Boolean) {
    // Two-stage animation: scale transitions from 0.6 → 1.0 on inhale and back
    // on exhale, matching the pacing constants above. infiniteTransition here
    // keeps the color pulse going independent of the discrete phase flips —
    // gives a subtle extra signal for the visually-attuned user.
    val targetScale = if (inhaling) 1f else 0.6f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(
            durationMillis = if (inhaling) DEFAULT_INHALE_MILLIS.toInt() else DEFAULT_EXHALE_MILLIS.toInt(),
            easing = LinearEasing
        ),
        label = "breath-scale"
    )

    val transition = rememberInfiniteTransition(label = "breath-hue")
    val hueProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hue"
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
private fun BreathPrayerPicker(
    selected: BreathPrayer,
    expanded: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    onSelect: (BreathPrayer) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = onExpand,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = selected.title,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = stringResource(R.string.prayer_modes_choose_breath_prayer)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss
        ) {
            BreathPrayerLibrary.prayers.forEach { prayer ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = prayer.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = prayer.tradition,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = { onSelect(prayer) }
                )
            }
        }
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

/**
 * Format seconds as "M:SS" without a leading zero on minutes — easier to
 * scan than padded MM:SS at short durations (0:45 vs 00:45).
 */
private fun formatSeconds(total: Int): String {
    val minutes = total / 60
    val seconds = total % 60
    return "%d:%02d".format(minutes, seconds)
}
