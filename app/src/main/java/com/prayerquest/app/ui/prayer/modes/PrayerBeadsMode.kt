package com.prayerquest.app.ui.prayer.modes

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prayerquest.app.domain.content.Decade
import com.prayerquest.app.domain.content.PrayerRope
import com.prayerquest.app.domain.content.RosaryContent

/**
 * Prayer Beads — walk through a Rosary, Jesus Prayer rope, Anglican rosary,
 * or Custom rope (DD §3.4 item 9). Visual beads fill as the user taps through;
 * haptic feedback fires on every advance so the rope feels tactile even on a
 * touchscreen.
 *
 * Flow:
 *  - User picks a rope from the bundled [RosaryContent] packs.
 *  - Each decade shows its title + meditation, then a bead strip. Tapping
 *    "Next bead" advances one position and fires HapticFeedbackConstants.
 *  - At the end of a decade the meditation for the next decade appears.
 *  - At the end of the rope, "Complete" hands the session content to
 *    [onModeComplete] with a summary of how far the user walked.
 *
 * Sprint 3 keeps pacing condensed (3 Hail Marys per Catholic decade, 33 knots
 * on the Jesus Prayer rope) so sessions finish within the DD's session
 * envelope. Sprint 6 adds a pacing preference (3/5/10 per decade, 33/50/100
 * knot ropes) and optional mystery-cycle selection (Joyful / Sorrowful /
 * Glorious / Luminous).
 */
@Composable
fun PrayerBeadsMode(
    onModeComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRope by remember { mutableStateOf(RosaryContent.default) }
    var pickerOpen by remember { mutableStateOf(false) }
    var decadeIndex by remember { mutableIntStateOf(0) }
    var beadIndex by remember { mutableIntStateOf(0) }

    val currentDecade = selectedRope.decades[decadeIndex]
    val currentBead = currentDecade.beads[beadIndex]
    val isLastBead = beadIndex == currentDecade.beads.lastIndex &&
        decadeIndex == selectedRope.decades.lastIndex

    val haptics = LocalView.current

    val totalBeads = selectedRope.decades.sumOf { it.beads.size }
    val beadsWalked = selectedRope.decades
        .take(decadeIndex)
        .sumOf { it.beads.size } + beadIndex + 1

    PrayerModeScaffold(
        modifier = modifier,
        contentArrangement = Arrangement.spacedBy(16.dp),
        action = {
            // Pinned "Next Bead / Next Decade / Complete Prayer" button. Beads
            // is a tap-driven mode — keeping the advance always visible means
            // a 33-knot rope reads like a single continuous rhythm instead of
            // "tap, scroll down, tap, scroll down" on every bead.
            Button(
                onClick = {
                    // Haptic tick per DD spec — every bead advance should feel
                    // tactile. KEYBOARD_TAP is the shortest, softest system click;
                    // CONTEXT_CLICK is a tad firmer and reads more "rosary" than
                    // "button tap," so we use it here.
                    haptics.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)

                    if (isLastBead) {
                        onModeComplete(
                            buildString {
                                appendLine("Prayer Beads · ${selectedRope.displayName}")
                                appendLine("${selectedRope.tradition} tradition")
                                appendLine()
                                appendLine("Walked all $totalBeads beads across ${selectedRope.decades.size} decade(s).")
                            }.trim()
                        )
                    } else if (beadIndex < currentDecade.beads.lastIndex) {
                        beadIndex++
                    } else {
                        decadeIndex++
                        beadIndex = 0
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = when {
                        isLastBead -> "Complete Prayer"
                        beadIndex == currentDecade.beads.lastIndex -> "Next Decade"
                        else -> "Next Bead"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    ) {
        RopePicker(
            selected = selectedRope,
            expanded = pickerOpen,
            onExpand = { pickerOpen = true },
            onDismiss = { pickerOpen = false },
            onSelect = {
                selectedRope = it
                decadeIndex = 0
                beadIndex = 0
                pickerOpen = false
            }
        )

        Text(
            text = selectedRope.tradition,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle = FontStyle.Italic
        )

        // Overall rope progress
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Decade ${decadeIndex + 1} of ${selectedRope.decades.size}",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "$beadsWalked / $totalBeads beads",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            LinearProgressIndicator(
                progress = { beadsWalked.toFloat() / totalBeads },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.small),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        // Decade title + meditation
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = currentDecade.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = currentDecade.meditation,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Bead strip — small dots showing position within the current decade.
        // Long decades (e.g., 33-knot Jesus Prayer rope) need horizontal scroll.
        BeadStrip(
            decade = currentDecade,
            activeBead = beadIndex
        )

        // The bead's prayer text — centered card that updates each tap
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = currentBead.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = currentBead.prayerText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }

    }
}

@Composable
private fun BeadStrip(decade: Decade, activeBead: Int) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)
    ) {
        items(decade.beads.size) { index ->
            val active = index == activeBead
            val done = index < activeBead
            Box(
                modifier = Modifier
                    .size(if (active) 18.dp else 14.dp)
                    .background(
                        color = when {
                            active -> MaterialTheme.colorScheme.primary
                            done -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun RopePicker(
    selected: PrayerRope,
    expanded: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    onSelect: (PrayerRope) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = onExpand,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = selected.displayName,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Choose rope"
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss
        ) {
            RosaryContent.allRopes.forEach { rope ->
                DropdownMenuItem(
                    text = {
                        Column(
                            modifier = Modifier.clickable { onSelect(rope) }
                        ) {
                            Text(
                                text = rope.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = rope.tradition,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = { onSelect(rope) }
                )
            }
        }
    }
}
