package com.prayerquest.app.ui.prayer.modes

import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prayerquest.app.domain.content.DailyOfficeLiturgy
import com.prayerquest.app.domain.content.Hour
import com.prayerquest.app.domain.content.LiturgySection
import com.prayerquest.app.domain.content.OfficeLiturgy
import androidx.compose.ui.res.stringResource
import com.prayerquest.app.R

/**
 * Daily Office — fixed-hour prayer (DD §3.4 item 10). Auto-selects the office
 * that fits the user's current local time (Lauds / Sext / Vespers / Compline)
 * and lets them switch to any of the four at any time via a chip row.
 *
 * Each office walks through six sections — opening versicle, psalm, reading,
 * collect, Lord's Prayer, closing — with a "Next" button that advances one
 * section at a time. The section card surfaces the versicle/response pattern
 * traditional to BCP liturgies: the opening line is the officiant's, the
 * response (when present) is shown as a soft italic under-card.
 *
 * Content is from [DailyOfficeLiturgy] which ships public-domain BCP text
 * (1662 tradition). Sprint 6 adds tradition-specific variants selectable in
 * onboarding (Anglican / Catholic LotH / Orthodox Horologion / Ecumenical).
 */
@Composable
fun DailyOfficeMode(
    onModeComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedHour by remember { mutableStateOf(Hour.forTime()) }
    var sectionIndex by remember { mutableIntStateOf(0) }

    val office = remember(selectedHour) { DailyOfficeLiturgy.forHour(selectedHour) }
    val sections = office.sections
    val section = sections[sectionIndex]
    val isLast = sectionIndex == sections.lastIndex

    PrayerModeScaffold(
        modifier = modifier,
        contentArrangement = Arrangement.spacedBy(16.dp),
        action = {
            // Primary "Next / Complete Office" plus a Back-one-step helper.
            // Both live in the pinned action area so reading a long psalm or
            // collect never hides the way forward. The back helper only
            // appears once there's somewhere to go back to.
            Button(
                onClick = {
                    if (!isLast) {
                        sectionIndex++
                    } else {
                        onModeComplete(formatOfficeTranscript(context, office))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = if (isLast) stringResource(R.string.prayer_modes_complete_office) else stringResource(R.string.common_next),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (sectionIndex > 0) {
                OutlinedButton(
                    onClick = { sectionIndex-- },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.prayer_modes_back_one_step))
                }
            }
        }
    ) {
        // Hour picker — auto-selected, but user can override
        HourChipRow(
            selected = selectedHour,
            onSelect = {
                selectedHour = it
                sectionIndex = 0
            }
        )

        // Office header with emoji + subtitle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = selectedHour.emoji,
                style = MaterialTheme.typography.displaySmall
            )
            Column {
                Text(
                    text = selectedHour.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = selectedHour.subtitle,
                    style = MaterialTheme.typography.labelLarge,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Progress dots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            sections.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            color = if (index <= sectionIndex)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.prayer_modes_x_x_2, sectionIndex + 1, sections.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Section card
        LiturgySectionCard(section)
    }
}

@Composable
private fun HourChipRow(
    selected: Hour,
    onSelect: (Hour) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(Hour.entries.size) { index ->
            val hour = Hour.entries[index]
            val isSelected = hour == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(hour) },
                label = { Text(hour.displayName) },
                leadingIcon = {
                    Text(
                        text = hour.emoji,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
private fun LiturgySectionCard(section: LiturgySection) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = section.body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Start
        )
        section.response?.let { response ->
            Spacer(Modifier.height(4.dp))
            Text(
                text = response,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
            )
        }
    }
}

// Note: takes a Context (not @Composable) because it's invoked from an
// onClick handler when the user finishes the office — onClick lambdas
// are not composable scopes, so stringResource() is unavailable. The
// caller captures LocalContext and threads it in.
private fun formatOfficeTranscript(context: Context, office: OfficeLiturgy): String = buildString {
    appendLine(context.getString(R.string.prayer_modes_daily_office_x, office.hour.displayName))
    appendLine(office.hour.subtitle)
    appendLine()
    office.sections.forEach { section ->
        appendLine("【${section.title}】")
        appendLine(section.body)
        section.response?.let {
            appendLine(context.getString(R.string.prayer_modes_x, it))
        }
        appendLine()
    }
}.trim()
