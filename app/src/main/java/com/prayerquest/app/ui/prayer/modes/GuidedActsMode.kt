package com.prayerquest.app.ui.prayer.modes

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

@Composable
fun GuidedActsMode(
    onModeComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val phases = listOf(
        ActsPhase("Adoration", "Express love and praise to God. What attributes of God inspire you most?"),
        ActsPhase("Confession", "Admit struggles and shortcomings. Where do you need grace?"),
        ActsPhase("Thanksgiving", "Give thanks for blessings. What has God provided?"),
        ActsPhase("Supplication", "Ask for help and guidance. What are your needs?")
    )

    var currentPhaseIndex by remember { mutableIntStateOf(0) }
    var phaseTexts by remember { mutableStateOf(List(4) { "" }) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Phase indicator dots
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            phases.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = if (index <= currentPhaseIndex)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Phase content
        val currentPhase = phases[currentPhaseIndex]

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Step ${currentPhaseIndex + 1}: ${currentPhase.title}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = currentPhase.prompt,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(16.dp)
            )

            // Timer (60 seconds per phase)
            ActsPhaseTimer(modifier = Modifier.fillMaxWidth())

            // Text input with voice button
            OutlinedTextField(
                value = phaseTexts[currentPhaseIndex],
                onValueChange = { newText ->
                    phaseTexts = phaseTexts.toMutableList().apply {
                        this[currentPhaseIndex] = newText
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                placeholder = { Text("Write your prayer...") },
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

            // Next button
            Button(
                onClick = {
                    if (currentPhaseIndex < phases.size - 1) {
                        currentPhaseIndex++
                    } else {
                        onModeComplete(phaseTexts.joinToString("\n---\n"))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = if (currentPhaseIndex < phases.size - 1) "Next Step" else "Complete ACTS",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun ActsPhaseTimer(modifier: Modifier = Modifier) {
    var secondsRemaining by remember { mutableIntStateOf(60) }

    LaunchedEffect(Unit) {
        while (secondsRemaining > 0) {
            delay(1000)
            secondsRemaining--
        }
    }

    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${secondsRemaining}s remaining",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

private data class ActsPhase(
    val title: String,
    val prompt: String
)
