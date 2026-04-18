package com.prayerquest.app.ui.prayer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import com.prayerquest.app.domain.model.PrayerGrade
import com.prayerquest.app.ui.theme.ErrorRed
import com.prayerquest.app.ui.theme.SuccessGreen
import com.prayerquest.app.ui.theme.WarningGold
import androidx.compose.ui.res.stringResource
import com.prayerquest.app.R

@Composable
fun GradeBar(
    onGradeSelected: (PrayerGrade, Int) -> Unit,
    showDepthSlider: Boolean = true,
    modifier: Modifier = Modifier
) {
    var selectedDepth by remember { mutableStateOf(3) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Depth slider (optional)
        if (showDepthSlider) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.prayer_meditation_depth),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Slider(
                        value = selectedDepth.toFloat(),
                        onValueChange = { selectedDepth = it.toInt() },
                        valueRange = 0f..5f,
                        steps = 4,
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = "$selectedDepth/5",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(40.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grade buttons
        Text(
            text = stringResource(R.string.prayer_how_was_this_session),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            GradeButton(
                label = stringResource(R.string.prayer_again),
                multiplier = "0.5×",
                grade = PrayerGrade.AGAIN,
                color = ErrorRed,
                modifier = Modifier.weight(1f),
                onGradeSelected = { onGradeSelected(it, selectedDepth) }
            )
            GradeButton(
                label = stringResource(R.string.prayer_hard),
                multiplier = "0.75×",
                grade = PrayerGrade.HARD,
                color = WarningGold,
                modifier = Modifier.weight(1f),
                onGradeSelected = { onGradeSelected(it, selectedDepth) }
            )
            GradeButton(
                label = stringResource(R.string.prayer_good),
                multiplier = "1.0×",
                grade = PrayerGrade.GOOD,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                onGradeSelected = { onGradeSelected(it, selectedDepth) }
            )
            GradeButton(
                label = stringResource(R.string.prayer_easy),
                multiplier = "1.25×",
                grade = PrayerGrade.EASY,
                color = SuccessGreen,
                modifier = Modifier.weight(1f),
                onGradeSelected = { onGradeSelected(it, selectedDepth) }
            )
        }
    }
}

/**
 * Grade button with a two-line, single-line-per-line layout. The outer
 * [Button]'s default 24dp horizontal contentPadding is replaced with a
 * tight 4dp; with `softWrap = false` on each [Text] this keeps "0.75×" and
 * "1.25×" from wrapping character-by-character into ugly stacks on narrow
 * phones. Same pattern as ScriptureQuest's donate buttons.
 */
@Composable
private fun GradeButton(
    label: String,
    multiplier: String,
    grade: PrayerGrade,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onGradeSelected: (PrayerGrade) -> Unit
) {
    Button(
        onClick = { onGradeSelected(grade) },
        modifier = modifier.heightIn(min = 64.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = androidx.compose.ui.graphics.Color.White
        ),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = multiplier,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
