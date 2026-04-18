package com.prayerquest.app.ui.prayer.modes

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun FlashPraySwipeMode(
    onModeComplete: (String) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Optional topic list — when the user is praying through a collection we
     * want the swipe cards to show their *actual* prayer items, not our
     * generic placeholders. Null or empty falls back to the default
     * intercession topics below. Titles only (no descriptions) so the card's
     * big centered text stays readable at a glance.
     */
    topics: List<String>? = null
) {
    val defaultTopics = listOf(
        "Global peace",
        "Healing from pain",
        "Financial wisdom",
        "Loving relationships",
        "Spiritual growth",
        "Community unity",
        "Strength & courage",
        "Forgiveness",
        "Joy & laughter",
        "Purpose & calling"
    )
    val prayerTopics = topics?.takeIf { it.isNotEmpty() } ?: defaultTopics

    var currentIndex by remember { mutableIntStateOf(0) }
    var swipedCount by remember { mutableIntStateOf(0) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    // Guard against out-of-bounds instead of `?: return` — the latter would
    // render the whole composable as empty when the user swipes past the last
    // card, leaving only the back arrow visible and hiding the completion
    // button below. Now we fall through to the "all swiped" completion block.
    val allSwiped = currentIndex >= prayerTopics.size
    val currentTopic = prayerTopics.getOrNull(currentIndex) ?: ""

    PrayerModeScaffold(
        modifier = modifier,
        // Tighter arrangement than the default 20dp — FlashPraySwipe has a
        // 200dp card that has to stay above-the-fold on standard phones, so
        // every dp of vertical chrome between the progress bar and the card
        // matters. The 12dp rhythm still reads as spacious.
        contentArrangement = Arrangement.spacedBy(12.dp),
        action = {
            // Pinned bottom action. Two modes:
            //  - allSwiped: "Complete Session" — the user finished every card.
            //  - otherwise: "Finish Session" — early-exit with whatever they've
            //    prayed so far. Always visible so the user is never trapped
            //    mid-deck wondering how to bail.
            //
            // Gated by `swipedCount > 0`: previously the user could tap Finish
            // Session immediately without ever interacting with a card and
            // still earn XP. The back arrow in the TopAppBar still lets them
            // fully bail out of the session without any swipes. A helper line
            // above the button explains the gate so users aren't puzzled by
            // a disabled CTA.
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (swipedCount == 0) {
                    Text(
                        text = "Swipe a card right to pray — at least one is required to finish.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    )
                }
                Button(
                    onClick = {
                        onModeComplete(
                            if (allSwiped) {
                                "Flash Prayed $swipedCount/$currentIndex topics"
                            } else {
                                "Flash Prayed $swipedCount/${currentIndex + 1} topics"
                            }
                        )
                    },
                    enabled = swipedCount > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = if (allSwiped) "Complete Session" else "Finish Session",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    ) {
        // Headline removed — TopAppBar already shows "Prayer Session · N/N",
        // duplicating it here just pushed the swipe card below the fold.
        Text(
            text = "Swipe right to pray → | Swipe left to skip ←",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // Card counter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Card ${currentIndex + 1}/${prayerTopics.size}",
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = "Prayed: $swipedCount",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Swipeable prayer card — only rendered while there's a card left to
        // swipe. Once the user has swiped through every topic we hide the
        // card entirely and let the "all done" completion block below take
        // the full attention of the screen.
        if (!allSwiped) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .offset { IntOffset(dragOffset.roundToInt(), 0) }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    when {
                                        dragOffset > 100 -> {
                                            // Swiped right - marked as prayed
                                            swipedCount++
                                            dragOffset = 0f
                                            currentIndex++
                                        }
                                        dragOffset < -100 -> {
                                            // Swiped left - skipped
                                            dragOffset = 0f
                                            currentIndex++
                                        }
                                        else -> {
                                            // Snap back
                                            dragOffset = 0f
                                        }
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    dragOffset += dragAmount
                                }
                            )
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Pray for:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = currentTopic,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        if (dragOffset > 50) {
                            Text(
                                text = "✓ I Prayed This",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else if (dragOffset < -50) {
                            Text(
                                text = "← Skip",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Bottom Pray/Skip hint row removed — the instruction line above
            // ("Swipe right to pray → | Swipe left to skip ←") already says
            // this, and the card itself shows live "✓ I Prayed This" / "← Skip"
            // feedback as the user drags. Keeping the hint row would push the
            // card below the fold on standard-height phones.
        } else {
            // Completion celebration block — shown once every card has been
            // swiped. The actual "Complete Session" CTA lives in the pinned
            // action slot; this block is just the congratulatory message.
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "You've gone through all ${prayerTopics.size} topics!",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
