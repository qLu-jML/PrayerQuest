package com.prayerquest.app.ui.prayer.modes

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun ContemplativeSilenceMode(
    onModeComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val prompts = listOf(
        "Be still and know that I am God.",
        "Listen to the still small voice within.",
        "Rest in His presence.",
        "Let peace wash over you.",
        "I am here with you.",
        "Trust and be at peace.",
        "Breathe in grace, breathe out worry.",
        "You are loved beyond measure."
    )

    var timeRemaining by remember { mutableIntStateOf(5) } // 5 minutes default
    var isRunning by remember { mutableStateOf(false) }
    var currentPromptIndex by remember { mutableIntStateOf(0) }
    var nextPromptIn by remember { mutableIntStateOf(45) }
    var reflection by remember { mutableStateOf("") }
    var showReflectionField by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (timeRemaining > 0) {
                delay(1000)
                timeRemaining--
                nextPromptIn--

                if (nextPromptIn <= 0) {
                    currentPromptIndex = Random.nextInt(prompts.size)
                    nextPromptIn = Random.nextInt(30, 90)
                }
            }
            showReflectionField = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Contemplative Silence",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Find a quiet place, breathe deeply, and listen for God's voice.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (!isRunning) {
            // Time selector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "How long would you like to meditate?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 3, 5, 10).forEach { minutes ->
                        Button(
                            onClick = {
                                timeRemaining = minutes * 60
                                isRunning = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text("$minutes minute${if (minutes > 1) "s" else ""}")
                        }
                    }
                }
            }
        } else {
            // Timer circle display
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val minutes = timeRemaining / 60
                    val seconds = timeRemaining % 60
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 48.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current prompt
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .background(
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = prompts[currentPromptIndex],
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    isRunning = false
                    showReflectionField = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("End & Reflect", style = MaterialTheme.typography.labelLarge)
            }
        }

        // Reflection field (appears after time is up)
        if (showReflectionField) {
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "What did you experience?",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = reflection,
                    onValueChange = { reflection = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    placeholder = { Text("Share your reflection...") },
                    shape = MaterialTheme.shapes.medium
                )
                Button(
                    onClick = {
                        onModeComplete("Silence meditation: $timeRemaining minutes\n\nReflection:\n$reflection")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = reflection.isNotEmpty(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Submit Reflection", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
