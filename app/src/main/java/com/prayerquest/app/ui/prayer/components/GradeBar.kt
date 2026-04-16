package com.prayerquest.app.ui.prayer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.prayerquest.app.domain.model.PrayerGrade
import com.prayerquest.app.ui.theme.ErrorRed
import com.prayerquest.app.ui.theme.SuccessGreen
import com.prayerquest.app.ui.theme.WarningGold

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
                    text = "Meditation Depth",
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
            text = "How was this session?",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GradeButton(
                label = "Again\n(0.5x)",
                grade = PrayerGrade.AGAIN,
                color = ErrorRed,
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp),
                onGradeSelected = { onGradeSelected(it, selectedDepth) }
            )
            GradeButton(
                label = "Hard\n(0.75x)",
                grade = PrayerGrade.HARD,
                color = WarningGold,
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp),
                onGradeSelected = { onGradeSelected(it, selectedDepth) }
            )
            GradeButton(
                label = "Good\n(1.0x)",
                grade = PrayerGrade.GOOD,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp),
                onGradeSelected = { onGradeSelected(it, selectedDepth) }
            )
            GradeButton(
                label = "Easy\n(1.25x)",
                grade = PrayerGrade.EASY,
                color = SuccessGreen,
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp),
                onGradeSelected = { onGradeSelected(it, selectedDepth) }
            )
        }
    }
}

@Composable
private fun GradeButton(
    label: String,
    grade: PrayerGrade,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onGradeSelected: (PrayerGrade) -> Unit
) {
    Button(
        onClick = { onGradeSelected(grade) },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = androidx.compose.ui.graphics.Color.White
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
