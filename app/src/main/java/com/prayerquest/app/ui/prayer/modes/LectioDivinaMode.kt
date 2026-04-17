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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prayerquest.app.domain.content.LectioPassage
import com.prayerquest.app.domain.content.LectioPassageLibrary

/**
 * Lectio Divina — the four-movement monastic reading practice (DD §3.4 item 7):
 *  1. Lectio     — read the passage slowly
 *  2. Meditatio  — meditate: what word stood out?
 *  3. Oratio     — pray back to God
 *  4. Contemplatio — rest in God's presence
 *
 * The user picks a passage from the bundled [LectioPassageLibrary] (ships
 * offline with 12 curated passages in Sprint 3, grows to 50+ in Sprint 6). The
 * passage remains visible at the top of each movement so the user can keep
 * returning to the text as the Holy Spirit draws attention to different
 * phrases across the four readings.
 *
 * Custom passages (user pastes their own) land in Sprint 6 — for Sprint 3 the
 * bundled pack is the source of truth, which keeps the flow honest against
 * the no-network guarantee.
 */
@Composable
fun LectioDivinaMode(
    onModeComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val movements = remember { LectioMovement.all() }
    var currentIndex by remember { mutableIntStateOf(0) }
    var movementNotes by remember { mutableStateOf(List(movements.size) { "" }) }
    var selectedPassage by remember { mutableStateOf(LectioPassageLibrary.default) }
    var passageMenuOpen by remember { mutableStateOf(false) }

    val movement = movements[currentIndex]
    val isLast = currentIndex == movements.lastIndex

    PrayerModeScaffold(
        modifier = modifier,
        contentArrangement = Arrangement.spacedBy(16.dp),
        action = {
            // Pinned "Next Movement / Complete Lectio" button. Lectio is
            // scripture-heavy — the passage card + prompt + 200dp-tall notes
            // field would otherwise push the Next button off-screen on most
            // phones. Keeping it in the scaffold slot means the rhythm of
            // read → respond → advance stays one tap away.
            Button(
                onClick = {
                    if (!isLast) {
                        currentIndex++
                    } else {
                        onModeComplete(
                            formatLectioTranscript(
                                movements = movements,
                                notes = movementNotes,
                                passage = selectedPassage
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = if (isLast) "Complete Lectio" else "Next Movement",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    ) {
        // Passage picker — only relevant before the first movement finishes,
        // but we keep it visible throughout so the user can switch passages
        // mid-session without losing progress flow if the Spirit leads them
        // elsewhere. Notes are passage-agnostic, kept across switches.
        PassagePicker(
            selected = selectedPassage,
            expanded = passageMenuOpen,
            onExpand = { passageMenuOpen = true },
            onDismiss = { passageMenuOpen = false },
            onSelect = {
                selectedPassage = it
                passageMenuOpen = false
            }
        )

        // Scripture card — the text persists across all four movements so the
        // user can keep returning to the passage.
        ScriptureCard(passage = selectedPassage)

        // Four-movement dot indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            movements.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = if (index <= currentIndex)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${currentIndex + 1}/${movements.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = movement.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = movement.subtitle,
                style = MaterialTheme.typography.titleSmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = movement.prompt,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(16.dp)
            )

            // Contemplatio has no text box — it's rest, not output.
            if (movement.collectsResponse) {
                OutlinedTextField(
                    value = movementNotes[currentIndex],
                    onValueChange = { newText ->
                        movementNotes = movementNotes.toMutableList().apply {
                            this[currentIndex] = newText
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 200.dp),
                    placeholder = { Text(movement.placeholder) },
                    shape = MaterialTheme.shapes.medium,
                    trailingIcon = {
                        IconButton(onClick = { /* Voice-to-text — SpeechRecognizer pass */ }) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice to text",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            } else {
                Text(
                    text = "Rest here. No words needed.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

        }
    }
}

@Composable
private fun PassagePicker(
    selected: LectioPassage,
    expanded: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    onSelect: (LectioPassage) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = onExpand,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = selected.reference,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Choose passage"
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss
        ) {
            LectioPassageLibrary.passages.forEach { passage ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = passage.reference,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = passage.category.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = { onSelect(passage) },
                    // Check mark on the currently-selected passage so the user
                    // can spot "where I am" in the list at a glance.
                    trailingIcon = if (passage.id == selected.id) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun ScriptureCard(passage: LectioPassage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = passage.reference,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = passage.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Start
        )
    }
}

private data class LectioMovement(
    val title: String,
    val subtitle: String,
    val prompt: String,
    val placeholder: String,
    val collectsResponse: Boolean
) {
    companion object {
        fun all(): List<LectioMovement> = listOf(
            LectioMovement(
                title = "Lectio",
                subtitle = "Read",
                prompt = "Read the passage slowly, once or twice. Let the words sit on your tongue. Which word or phrase shimmered?",
                placeholder = "The word that caught my attention was…",
                collectsResponse = true
            ),
            LectioMovement(
                title = "Meditatio",
                subtitle = "Meditate",
                prompt = "Turn that word or phrase over in your mind. Why this one? What memory, question, or image does it stir?",
                placeholder = "This word reminds me of… / It asks me…",
                collectsResponse = true
            ),
            LectioMovement(
                title = "Oratio",
                subtitle = "Pray",
                prompt = "Now speak back to God — your response to what he's shown you.",
                placeholder = "Lord, I want to say back to you…",
                collectsResponse = true
            ),
            LectioMovement(
                title = "Contemplatio",
                subtitle = "Rest",
                prompt = "Sit in silence. No more words. Let God simply be with you.",
                placeholder = "",
                collectsResponse = false
            )
        )
    }
}

private fun formatLectioTranscript(
    movements: List<LectioMovement>,
    notes: List<String>,
    passage: LectioPassage
): String = buildString {
    appendLine("Lectio Divina · ${passage.reference}")
    appendLine()
    movements.forEachIndexed { index, movement ->
        if (!movement.collectsResponse) return@forEachIndexed
        val text = notes.getOrNull(index)?.trim().orEmpty()
        if (text.isNotEmpty()) {
            appendLine("【${movement.title} · ${movement.subtitle}】")
            appendLine(text)
            appendLine()
        }
    }
}.trim()
