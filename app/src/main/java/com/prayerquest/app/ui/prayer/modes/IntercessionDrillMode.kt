package com.prayerquest.app.ui.prayer.modes

import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prayerquest.app.ui.components.PrayerPhotoAvatar
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource
import com.prayerquest.app.R

/**
 * Intercession Drill — fast rhythm of praying for a handful of items in
 * succession, 30 seconds per item with a quick note field.
 *
 * ### Why this exists as its own mode
 * The other modes drop the user into a single mode for a single item.
 * Intercession is different: it's a list-oriented cadence — bang through
 * 5-8 people/needs quickly, logging one-line notes as you go. Timer keeps
 * momentum so users don't fall into a 10-minute monologue on item #1.
 *
 * @param topics Optional list of topics to drill through. When the session
 *   is launched from a collection or the user's active list, these are the
 *   real items they're praying for. Null / empty falls back to the generic
 *   placeholders below. Never empty-on-read — the defensive init at the
 *   top of the function normalizes to a non-empty list.
 * @param photoUris Optional parallel list of Photo Prayer (DD §3.9) paths —
 *   indexed the same as [topics]. When present and non-null at a given index,
 *   the current-item card renders a circular avatar of the user's photo next
 *   to the title. Callers pass null for items without a photo; the mode
 *   falls back to a monogram-style avatar in that case.
 */
@Composable
fun IntercessionDrillMode(
    onModeComplete: (String) -> Unit,
    modifier: Modifier = Modifier,
    topics: List<String>? = null,
    photoUris: List<String?>? = null,
) {
    // Normalize caller-supplied topics. Null OR empty falls back to the
    // default list — we never want to render with a zero-length list, which
    // would make `prayerItems[currentItemIndex]` throw
    // IndexOutOfBoundsException on first frame.
    //
    // Resolve the localized default items at @Composable level — `remember {}`
    // is non-@Composable, so we can't call stringResource() inside it.
    val defaultIntercessionItems = listOf(
        stringResource(R.string.prayer_modes_family_members),
        stringResource(R.string.prayer_modes_friends_in_need),
        stringResource(R.string.prayer_modes_work_colleagues),
        stringResource(R.string.prayer_modes_church_community),
        stringResource(R.string.prayer_modes_missionaries),
        stringResource(R.string.prayer_modes_healing_health)
    )
    val prayerItems = remember(topics, defaultIntercessionItems) {
        topics?.filter { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
            ?: defaultIntercessionItems
    }

    var currentItemIndex by remember { mutableIntStateOf(0) }
    var prayedItems by remember { mutableStateOf(setOf<Int>()) }
    // Immutable-map pattern: we REPLACE the map on each write rather than
    // mutate in place. Previous implementation used `mutableMapOf` +
    // `quickNotes[idx] = newText`, which doesn't trigger recomposition
    // (the state value reference never changes) and in rare cases left the
    // TextField reading stale text on re-entry.
    var quickNotes by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var timeRemaining by remember { mutableIntStateOf(30) }

    // Guard against out-of-range reads — `currentItemIndex` should never
    // exceed `prayerItems.size - 1` since we only advance when it's below
    // that, but if upstream state ever gets wedged (e.g. a smaller topics
    // list hot-swapping into a remembered index) we coerce it rather than
    // crash.
    val safeIndex = currentItemIndex.coerceIn(0, prayerItems.size - 1)
    val currentItem = prayerItems[safeIndex]
    val progress = (safeIndex + 1).toFloat() / prayerItems.size
    val isLastItem = safeIndex >= prayerItems.size - 1

    LaunchedEffect(safeIndex) {
        timeRemaining = 30
        while (timeRemaining > 0) {
            delay(1000)
            timeRemaining--
        }
    }

    PrayerModeScaffold(
        modifier = modifier,
        contentArrangement = Arrangement.spacedBy(16.dp),
        action = {
            // Pinned "Prayed & Next / Complete Drill" CTA. Intercession is a
            // fast rhythm with a 30-second ticker per item; if the button is
            // below the fold the user runs out the clock trying to find it.
            Button(
                onClick = {
                    prayedItems = prayedItems + safeIndex
                    if (!isLastItem) {
                        currentItemIndex = safeIndex + 1
                    } else {
                        // Build the transcript from whatever items + notes we
                        // have. We iterate the list directly (no zip with
                        // indices) — simpler and no intermediate
                        // Pair-destructuring lambda.
                        val transcript = buildString {
                            prayerItems.forEachIndexed { idx, item ->
                                val note = quickNotes[idx].orEmpty()
                                if (idx > 0) append("\n")
                                if (note.isNotEmpty()) {
                                    append(item).append(": ").append(note)
                                } else {
                                    append(item)
                                }
                            }
                        }
                        onModeComplete(transcript)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.prayer_modes_mark_as_prayed_2),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = if (!isLastItem) stringResource(R.string.prayer_modes_prayed_next) else stringResource(R.string.prayer_modes_complete_drill),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    ) {
        Text(
            text = stringResource(R.string.prayer_modes_intercession_drill),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // Progress indicator
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.prayer_modes_item_x_x, safeIndex + 1, prayerItems.size),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "${timeRemaining}s",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (timeRemaining < 10)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.small),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        // Current item card
        //
        // Photo Prayer avatar (DD §3.9) sits above the title when the caller
        // supplied a photo for this item. Sized a touch bigger (64dp) than the
        // list-card avatar to give the item a face — makes intercession feel
        // personal instead of a generic template. Items without a photo fall
        // back to a monogram via PrayerPhotoAvatar's null-path branch.
        val currentPhotoUri = photoUris?.getOrNull(safeIndex)
        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrayerPhotoAvatar(
                    photoPath = currentPhotoUri,
                    fallbackLabel = currentItem,
                    size = 64.dp,
                )
                Text(
                    text = stringResource(R.string.prayer_modes_praying_for),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = currentItem,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Quick note field
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.prayer_modes_quick_note_optional),
                style = MaterialTheme.typography.labelMedium
            )
            OutlinedTextField(
                value = quickNotes[safeIndex].orEmpty(),
                onValueChange = { newText ->
                    // Replace the map so Compose observes the change.
                    quickNotes = quickNotes + (safeIndex to newText)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                placeholder = { Text(stringResource(R.string.prayer_modes_e_g_pray_for_guidance)) },
                shape = MaterialTheme.shapes.medium,
                trailingIcon = {
                    IconButton(
                        onClick = { /* Voice-to-text placeholder */ }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = stringResource(R.string.common_voice_to_text),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Completion status
        if (prayedItems.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.prayer_modes_prayed_for_x_x, prayedItems.size, prayerItems.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
