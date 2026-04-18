package com.prayerquest.app.ui.prayer.modes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.res.stringResource
import com.prayerquest.app.R

@Composable
fun PrayerJournalMode(
    onModeComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var journalEntry by remember { mutableStateOf("") }
    var showRecap by remember { mutableStateOf(false) }

    val dateFormatter = SimpleDateFormat(stringResource(R.string.prayer_modes_eeee_mmmm_d_yyyy), Locale.getDefault())
    val currentDate = dateFormatter.format(Date())

    PrayerModeScaffold(
        modifier = modifier,
        action = {
            // Pinned bottom action area. The button that lives here depends on
            // the two-phase flow:
            //   • Writing phase  → "Submit Prayer" (advances to recap)
            //   • Recap phase    → "Edit" + "Confirm" side-by-side
            // Keeping both phases' actions in the scaffold slot guarantees the
            // primary call-to-action is always visible without scrolling —
            // which was the specific bug on long journal entries where the
            // user had to scroll past a 200dp text field to find Submit.
            if (!showRecap) {
                Button(
                    onClick = { showRecap = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = journalEntry.isNotEmpty(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = stringResource(R.string.prayer_modes_submit_prayer),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showRecap = false },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(stringResource(R.string.common_edit))
                    }
                    Button(
                        onClick = { onModeComplete(journalEntry) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(stringResource(R.string.common_confirm))
                    }
                }
            }
        },
        contentArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.prayer_modes_prayer_journal),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // Date header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentDate,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        // Prompt text
        Text(
            text = stringResource(R.string.prayer_modes_write_freely_about_what_s_on_your_heart_today_this),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        // Text input with voice button
        OutlinedTextField(
            value = journalEntry,
            onValueChange = { journalEntry = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            placeholder = { Text(stringResource(R.string.prayer_modes_write_your_prayer)) },
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

        // Recap section — shown after user taps Submit. Lives in the
        // scrollable body so long recaps can scroll while the Edit/Confirm
        // actions stay pinned below.
        if (showRecap) {
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.prayer_modes_your_prayer_recap),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = formatPrayerRecap(journalEntry),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

private fun formatPrayerRecap(text: String): String {
    val lines = text.split("\n").filter { it.isNotBlank() }
    return if (lines.isEmpty()) {
        text
    } else {
        lines.take(3).joinToString("\n") +
        if (lines.size > 3) "\n..." else ""
    }
}
