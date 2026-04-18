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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.prayerquest.app.R

/**
 * Daily Examen — the Ignatian 5-step review of the day (DD §3.4 item 6).
 *
 * Steps:
 *  1. Gratitude      — what am I thankful for today?
 *  2. Petition       — ask the Spirit for insight before reviewing
 *  3. Review         — walk through the day with God
 *  4. Repentance     — confess shortcomings
 *  5. Resolution     — how will I meet tomorrow with Christ?
 *
 * Each step is a short prompt with a voice-to-text-capable input and a 60–90s
 * suggested pace (shown as a soft hint — no timer pressure for end-of-day
 * reflection). The final Complete button joins all 5 responses with a
 * separator and hands them to [onModeComplete] so the grade/summary flow can
 * save the full session transcript in [PrayerRecord.journalText].
 *
 * Auto-Gratitude hook (DD line 72): the Gratitude step response is the same
 * text the user would hand to the Gratitude Catalogue; wiring the automatic
 * write into the Gratitude table happens in Sprint 4 when the Gratitude
 * section gets its repository-level side-effect hook.
 */
@Composable
fun DailyExamenMode(
    onModeComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = ExamenStep.all()
    var currentStepIndex by remember { mutableIntStateOf(0) }
    var stepResponses by remember { mutableStateOf(List(steps.size) { "" }) }

    val currentStep = steps[currentStepIndex]
    val isLastStep = currentStepIndex == steps.lastIndex
    val stepResponse = stepResponses[currentStepIndex]

    PrayerModeScaffold(
        modifier = modifier,
        action = {
            Button(
                onClick = {
                    if (!isLastStep) {
                        currentStepIndex++
                    } else {
                        onModeComplete(formatExamenTranscript(steps, stepResponses))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = if (isLastStep) stringResource(R.string.prayer_modes_complete_examen) else stringResource(R.string.prayer_modes_next_step),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    ) {
        // Progress dots — one per examen step
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = if (index <= currentStepIndex)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.prayer_modes_x_x, currentStepIndex + 1, steps.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.prayer_modes_step_x_x, currentStepIndex + 1, currentStep.title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Prompt card — the spiritual question for this step
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
                    text = currentStep.prompt,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = currentStep.guidance,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Pace hint (not a countdown — end-of-day reflection, no pressure)
            Text(
                text = stringResource(R.string.prayer_modes_suggested_x_s_take_your_time, currentStep.suggestedSeconds),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = stepResponse,
                onValueChange = { newText ->
                    stepResponses = stepResponses.toMutableList().apply {
                        this[currentStepIndex] = newText
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 200.dp),
                placeholder = { Text(currentStep.placeholder) },
                shape = MaterialTheme.shapes.medium,
                trailingIcon = {
                    IconButton(onClick = { /* Voice-to-text wired in SpeechRecognizer pass */ }) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = stringResource(R.string.common_voice_to_text),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    }
}

/**
 * The 5 Ignatian steps in order. [suggestedSeconds] is shown as a gentle
 * pacing hint, not enforced — the Examen is end-of-day reflection and should
 * never feel rushed.
 */
private data class ExamenStep(
    val title: String,
    val prompt: String,
    val guidance: String,
    val placeholder: String,
    val suggestedSeconds: Int
) {
    companion object {
        @Composable
        fun all(): List<ExamenStep> = listOf(
            ExamenStep(
                title = stringResource(R.string.common_gratitude),
                prompt = stringResource(R.string.common_what_are_you_thankful_for_today),
                guidance = stringResource(R.string.prayer_modes_start_here_notice_one_or_two_gifts_from_today_big),
                placeholder = stringResource(R.string.prayer_modes_today_i_m_grateful_for),
                suggestedSeconds = 60
            ),
            ExamenStep(
                title = stringResource(R.string.prayer_modes_petition),
                prompt = stringResource(R.string.prayer_modes_ask_the_spirit_for_light_to_see_your_day_honestly),
                guidance = stringResource(R.string.prayer_modes_before_reviewing_invite_god_to_show_you_what_he_se),
                placeholder = stringResource(R.string.prayer_modes_lord_help_me_see_today_the_way_you_see_it),
                suggestedSeconds = 45
            ),
            ExamenStep(
                title = stringResource(R.string.prayer_modes_review),
                prompt = stringResource(R.string.prayer_modes_walk_through_your_day_with_god),
                guidance = stringResource(R.string.prayer_modes_where_did_you_feel_close_to_god_where_did_you_drif),
                placeholder = stringResource(R.string.prayer_modes_this_morning_i_around_midday_tonight_i),
                suggestedSeconds = 90
            ),
            ExamenStep(
                title = stringResource(R.string.prayer_modes_repentance),
                prompt = stringResource(R.string.prayer_modes_where_did_you_miss_the_mark_today),
                guidance = stringResource(R.string.prayer_modes_name_anything_you_want_to_bring_under_christ_s_mer),
                placeholder = stringResource(R.string.prayer_modes_forgive_me_lord_for),
                suggestedSeconds = 60
            ),
            ExamenStep(
                title = stringResource(R.string.prayer_modes_resolution),
                prompt = stringResource(R.string.prayer_modes_how_will_you_meet_tomorrow_with_christ),
                guidance = stringResource(R.string.prayer_modes_a_simple_concrete_resolve_one_thing_you_ll_do_ask),
                placeholder = stringResource(R.string.prayer_modes_tomorrow_with_god_s_help_i_will),
                suggestedSeconds = 45
            )
        )
    }
}

/** Join all 5 responses into a single transcript suitable for [PrayerRecord.journalText]. */
private fun formatExamenTranscript(
    steps: List<ExamenStep>,
    responses: List<String>
): String = buildString {
    steps.forEachIndexed { index, step ->
        val text = responses.getOrNull(index)?.trim().orEmpty()
        if (text.isNotEmpty()) {
            appendLine("【${step.title}】")
            appendLine(text)
            appendLine()
        }
    }
}.trim()
