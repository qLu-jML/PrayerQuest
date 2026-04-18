package com.prayerquest.app.ui.prayer.modes

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.res.stringResource
import com.prayerquest.app.R

@Composable
fun VoiceRecordMode(
    onModeComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var transcription by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableStateOf("00:00") }

    PrayerModeScaffold(
        modifier = modifier,
        contentHorizontalAlignment = Alignment.CenterHorizontally,
        action = {
            // Pinned "Done Recording" button. Voice mode's content is
            // vertically tall (mic button + transcript + playback button) so
            // the Done action was consistently pushed below the fold.
            Button(
                onClick = { onModeComplete(transcription) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = transcription.isNotEmpty(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = stringResource(R.string.prayer_modes_done_recording),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    ) {
        Text(
            text = stringResource(R.string.prayer_modes_voice_prayer),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(R.string.prayer_modes_tap_the_microphone_and_speak_your_prayer_your_word),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Large microphone button
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    color = if (isRecording)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = { isRecording = !isRecording },
                modifier = Modifier.size(100.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = stringResource(R.string.prayer_modes_record_prayer),
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(60.dp)
                )
            }
        }

        Text(
            text = if (isRecording) stringResource(R.string.prayer_modes_recording_x, recordingDuration) else stringResource(R.string.prayer_modes_ready_to_record),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isRecording)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Transcription display and edit
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.prayer_modes_transcription),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (transcription.isNotEmpty()) {
                    IconButton(
                        onClick = { /* Edit transcription */ },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.prayer_modes_edit_transcription),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = transcription,
                onValueChange = { transcription = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                placeholder = { Text(stringResource(R.string.prayer_modes_your_prayer_text_will_appear_here)) },
                enabled = !isRecording,
                shape = MaterialTheme.shapes.small
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Playback placeholder
        if (transcription.isNotEmpty()) {
            Button(
                onClick = { /* Play back recording */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.prayer_modes_play_recording),
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 8.dp)
                )
                Text(stringResource(R.string.prayer_modes_play_back_prayer))
            }
        }

    }
}
