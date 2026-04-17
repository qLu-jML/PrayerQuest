package com.prayerquest.app.ui.prayer.modes

import androidx.compose.animation.animateContentSize
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
import kotlinx.coroutines.delay

@Composable
fun IntercessionDrillMode(
    onModeComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val prayerItems = listOf(
        "Family members",
        "Friends in need",
        "Work colleagues",
        "Church community",
        "Missionaries",
        "Healing & health"
    )

    var currentItemIndex by remember { mutableIntStateOf(0) }
    var prayedItems by remember { mutableStateOf(setOf<Int>()) }
    var quickNotes by remember { mutableStateOf(mutableMapOf<Int, String>()) }
    var timeRemaining by remember { mutableIntStateOf(30) }

    LaunchedEffect(currentItemIndex) {
        timeRemaining = 30
        while (timeRemaining > 0) {
            delay(1000)
            timeRemaining--
        }
    }

    val currentItem = prayerItems[currentItemIndex]
    val progress = (currentItemIndex + 1).toFloat() / prayerItems.size

    PrayerModeScaffold(
        modifier = modifier,
        contentArrangement = Arrangement.spacedBy(16.dp),
        action = {
            // Pinned "Prayed & Next / Complete Drill" CTA. Intercession is a
            // fast rhythm with a 30-second ticker per item; if the button is
            // below the fold the user runs out the clock trying to find it.
            Button(
                onClick = {
                    prayedItems = prayedItems + currentItemIndex
                    if (currentItemIndex < prayerItems.size - 1) {
                        currentItemIndex++
                    } else {
                        onModeComplete(
                            prayerItems.zip(
                                prayerItems.indices.map { idx ->
                                    quickNotes[idx] ?: ""
                                }
                            ).joinToString("\n") { (item, note) ->
                                if (note.isNotEmpty()) "$item: $note" else item
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Mark as prayed",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = if (currentItemIndex < prayerItems.size - 1) "Prayed & Next" else "Complete Drill",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    ) {
        Text(
            text = "Intercession Drill",
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
                    text = "Item ${currentItemIndex + 1}/${prayerItems.size}",
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
                Text(
                    text = "Praying for:",
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
                text = "Quick Note (optional)",
                style = MaterialTheme.typography.labelMedium
            )
            OutlinedTextField(
                value = quickNotes[currentItemIndex] ?: "",
                onValueChange = { newText ->
                    quickNotes[currentItemIndex] = newText
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                placeholder = { Text("e.g., pray for guidance...") },
                shape = MaterialTheme.shapes.medium,
                trailingIcon = {
                    IconButton(
                        onClick = { /* Voice-to-text placeholder */ }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice to text",
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
                    text = "Prayed for ${prayedItems.size}/${prayerItems.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
