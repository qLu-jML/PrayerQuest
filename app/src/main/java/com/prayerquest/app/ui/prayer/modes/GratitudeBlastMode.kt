package com.prayerquest.app.ui.prayer.modes

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GratitudeBlastMode(
    onModeComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val preLoadedBlessings = listOf(
        "Family", "Health", "Home", "Food", "Friends",
        "Sunrises", "Laughter", "Rest", "Hope", "Grace",
        "Breath", "Nature", "Love", "Strength", "Peace"
    )

    var selectedBlessings by remember { mutableStateOf(setOf<String>()) }
    var customBlessing by remember { mutableStateOf("") }
    var timeRemaining by remember { mutableIntStateOf(300) } // 5 minutes
    var speedBonus by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (timeRemaining > 0) {
            delay(1000)
            timeRemaining--
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Gratitude Blast",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Tap blessings you're grateful for. Aim for 3-10 items!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        // Timer
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
                text = "Time: ${timeRemaining}s",
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = "Selected: ${selectedBlessings.size}",
                style = MaterialTheme.typography.labelMedium
            )
        }

        // Blessing chips
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            preLoadedBlessings.forEach { blessing ->
                InputChip(
                    selected = blessing in selectedBlessings,
                    onClick = {
                        selectedBlessings = if (blessing in selectedBlessings) {
                            selectedBlessings - blessing
                        } else {
                            selectedBlessings + blessing
                        }
                        updateSpeedBonus(selectedBlessings, timeRemaining)
                    },
                    label = { Text(blessing) },
                    shape = MaterialTheme.shapes.medium
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Custom blessing input
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Add Your Own",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = customBlessing,
                    onValueChange = { customBlessing = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    placeholder = { Text("e.g., Music") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
                Button(
                    onClick = {
                        if (customBlessing.isNotBlank()) {
                            selectedBlessings = selectedBlessings + customBlessing
                            customBlessing = ""
                            updateSpeedBonus(selectedBlessings, timeRemaining)
                        }
                    },
                    modifier = Modifier.height(48.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add blessing"
                    )
                }
            }
        }

        // Speed bonus indicator
        if (speedBonus > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚡ +${speedBonus} Speed Bonus XP",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Done button
        Button(
            onClick = {
                onModeComplete(selectedBlessings.joinToString(", "))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = selectedBlessings.size in 3..10,
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = "Done (${selectedBlessings.size}/3-10)",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

private fun updateSpeedBonus(selectedBlessings: Set<String>, timeRemaining: Int) {
    val speedBonus = if (timeRemaining > 240 && selectedBlessings.size >= 5) 25 else 0
    // This would be tracked in state in a real implementation
}
