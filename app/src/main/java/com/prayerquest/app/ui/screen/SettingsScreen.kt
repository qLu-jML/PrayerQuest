package com.prayerquest.app.ui.screen

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prayerquest.app.PrayerQuestApplication
import com.prayerquest.app.data.preferences.DevotionalAuthor
import com.prayerquest.app.data.preferences.LiturgicalCalendar
import com.prayerquest.app.data.preferences.ReminderSlot
import com.prayerquest.app.data.preferences.ReminderSlotConfig
import com.prayerquest.app.data.preferences.ThemeMode
import com.prayerquest.app.data.preferences.UserPreferences
import com.prayerquest.app.domain.model.PrayerMode
import com.prayerquest.app.domain.model.Tradition
import com.prayerquest.app.firebase.AuthState
import com.prayerquest.app.ui.settings.SettingsViewModel
import com.prayerquest.app.ui.settings.ThemePickerDialog
import com.prayerquest.app.ui.theme.AppTheme
import com.prayerquest.app.ui.theme.ThemeRepository
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    userPreferences: UserPreferences,
    onNavigateBack: () -> Unit = {},  // Kept for backward compatibility but unused as top-level tab
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.create(
            userPreferences,
            LocalContext.current.applicationContext
        )
    )
) {
    val themeMode by viewModel.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val selectedThemeId by viewModel.selectedThemeId.collectAsState(initial = "prayer_quest")
    val dailyGoal by viewModel.dailyGoal.collectAsState(initial = 10)
    val gratitudeGoal by viewModel.gratitudeGoal.collectAsState(initial = 3)
    val displayName by viewModel.displayName.collectAsState(initial = "Prayer Warrior")
    val reminderSlots by viewModel.reminderSlots.collectAsState(initial = emptyList())
    val quietHoursEnabled by viewModel.quietHoursEnabled.collectAsState(initial = true)
    val quietHoursStartMin by viewModel.quietHoursStartMin.collectAsState(initial = UserPreferences.DEFAULT_QUIET_START_MIN)
    val quietHoursEndMin by viewModel.quietHoursEndMin.collectAsState(initial = UserPreferences.DEFAULT_QUIET_END_MIN)
    val enabledTraditions by viewModel.enabledTraditions.collectAsState(initial = Tradition.DEFAULT)
    val disabledModes by viewModel.disabledModes.collectAsState(initial = emptySet())
    val devotionalAuthor by viewModel.devotionalAuthor.collectAsState(initial = DevotionalAuthor.NONE)
    val devotionalSpurgeonMin by viewModel.devotionalSpurgeonMin.collectAsState(initial = UserPreferences.DEFAULT_SPURGEON_MIN)
    val devotionalSpurgeonEveningMin by viewModel.devotionalSpurgeonEveningMin.collectAsState(initial = UserPreferences.DEFAULT_SPURGEON_EVENING_MIN)
    val devotionalSpurgeonMorningEnabled by viewModel.devotionalSpurgeonMorningEnabled.collectAsState(initial = true)
    val devotionalSpurgeonEveningEnabled by viewModel.devotionalSpurgeonEveningEnabled.collectAsState(initial = true)
    val devotionalBonhoefferMin by viewModel.devotionalBonhoefferMin.collectAsState(initial = UserPreferences.DEFAULT_BONHOEFFER_MIN)
    val liturgicalCalendar by viewModel.liturgicalCalendar.collectAsState(initial = LiturgicalCalendar.NONE)

    val coroutineScope = rememberCoroutineScope()
    val showThemeDialog = remember { mutableStateOf(false) }
    val editingDisplayName = remember { mutableStateOf(displayName) }

    if (showThemeDialog.value) {
        ThemePickerDialog(
            onDismiss = { showThemeDialog.value = false },
            onSaveTheme = { theme ->
                coroutineScope.launch {
                    viewModel.addCustomTheme(theme)
                }
                showThemeDialog.value = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top header
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ═══════════════════════════════════════════════════════════════
            // Theme
            // ═══════════════════════════════════════════════════════════════
            SettingsSectionHeader(title = "Theme")
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Theme Mode",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.values().forEachIndexed { _, mode ->
                    SegmentedButton(
                        selected = themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(mode.name, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Select Theme",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val themes = ThemeRepository.getAllBuiltInThemes()
            val rows = (themes.size + 1) / 2
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .height((rows * 140).dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                userScrollEnabled = false
            ) {
                items(themes) { theme ->
                    ThemePreviewCard(
                        theme = theme,
                        isSelected = theme.id == selectedThemeId,
                        onClick = { viewModel.setSelectedThemeId(theme.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showThemeDialog.value = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Custom Theme")
            }

            SectionDivider()

            // ═══════════════════════════════════════════════════════════════
            // Prayer Goals
            // ═══════════════════════════════════════════════════════════════
            SettingsSectionHeader(title = "Prayer Goals")
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Daily Prayer Goal (minutes)",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            GoalChipRow(
                options = listOf(5, 10, 15, 20, 30, 45, 60),
                selected = dailyGoal,
                onSelect = { viewModel.setDailyGoal(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Daily Gratitude Goal (entries)",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            GoalChipRow(
                options = listOf(1, 3, 5),
                selected = gratitudeGoal,
                onSelect = { viewModel.setGratitudeGoal(it) }
            )

            SectionDivider()

            // ═══════════════════════════════════════════════════════════════
            // Reminders (3 configurable slots)
            // ═══════════════════════════════════════════════════════════════
            SettingsSectionHeader(title = "Reminders")
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Choose when PrayerQuest nudges you to pray. Each slot has its own tone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            reminderSlots.forEach { slot ->
                ReminderSlotRow(
                    config = slot,
                    onChange = { viewModel.setReminderSlot(it) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            SectionDivider()

            // ═══════════════════════════════════════════════════════════════
            // Quiet Hours
            // ═══════════════════════════════════════════════════════════════
            SettingsSectionHeader(title = "Quiet Hours")
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pause notifications during this window. Daily devotional and reminders silently skip.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Enable Quiet Hours",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = quietHoursEnabled,
                    onCheckedChange = { viewModel.setQuietHoursEnabled(it) }
                )
            }

            if (quietHoursEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TimePickerField(
                        label = "Start",
                        minuteOfDay = quietHoursStartMin,
                        onTimePicked = { newStart ->
                            viewModel.setQuietHoursWindow(newStart, quietHoursEndMin)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    TimePickerField(
                        label = "End",
                        minuteOfDay = quietHoursEndMin,
                        onTimePicked = { newEnd ->
                            viewModel.setQuietHoursWindow(quietHoursStartMin, newEnd)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (quietHoursStartMin > quietHoursEndMin) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "This window crosses midnight.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SectionDivider()

            // ═══════════════════════════════════════════════════════════════
            // Traditions
            // ═══════════════════════════════════════════════════════════════
            SettingsSectionHeader(title = "Traditions")
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pick any that speak to you — shapes which prayer modes appear first.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            TraditionChips(
                enabled = enabledTraditions,
                onToggle = { viewModel.toggleTradition(it) }
            )

            SectionDivider()

            // ═══════════════════════════════════════════════════════════════
            // Prayer Modes (per-mode on/off toggles)
            // ═══════════════════════════════════════════════════════════════
            SettingsSectionHeader(title = "Prayer Modes")
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Fine-tune which modes appear in your Mode Picker. Per-mode choices override tradition defaults.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            PrayerMode.values().forEach { mode ->
                ModeToggleRow(
                    mode = mode,
                    enabled = mode !in disabledModes,
                    onToggle = { viewModel.setModeEnabled(mode, it) }
                )
            }

            SectionDivider()

            // ═══════════════════════════════════════════════════════════════
            // Daily Devotional (author + times)
            // ═══════════════════════════════════════════════════════════════
            SettingsSectionHeader(title = "Daily Devotional")
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Receive a short daily reading from a classical author, delivered at the time you choose.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            DevotionalAuthorPicker(
                current = devotionalAuthor,
                onSelect = { viewModel.setDevotionalAuthor(it) }
            )

            if (devotionalAuthor == DevotionalAuthor.SPURGEON ||
                devotionalAuthor == DevotionalAuthor.BOTH
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Spurgeon — Morning & Evening",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Spurgeon's \"Morning and Evening\" has two readings per day. Toggle each on independently.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                ToggleableTimeBlock(
                    label = "Morning reading",
                    enabled = devotionalSpurgeonMorningEnabled,
                    minuteOfDay = devotionalSpurgeonMin,
                    onToggle = { viewModel.setDevotionalSpurgeonMorningEnabled(it) },
                    onTimePicked = {
                        viewModel.setDevotionalTime(DevotionalAuthor.SPURGEON, it)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ToggleableTimeBlock(
                    label = "Evening reflection",
                    enabled = devotionalSpurgeonEveningEnabled,
                    minuteOfDay = devotionalSpurgeonEveningMin,
                    onToggle = { viewModel.setDevotionalSpurgeonEveningEnabled(it) },
                    onTimePicked = {
                        viewModel.setDevotionalSpurgeonEveningMin(it)
                    }
                )
            }

            if (devotionalAuthor == DevotionalAuthor.BONHOEFFER ||
                devotionalAuthor == DevotionalAuthor.BOTH
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                TimePickerField(
                    label = "Bonhoeffer time",
                    minuteOfDay = devotionalBonhoefferMin,
                    onTimePicked = {
                        viewModel.setDevotionalTime(DevotionalAuthor.BONHOEFFER, it)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SectionDivider()

            // ═══════════════════════════════════════════════════════════════
            // Liturgical Calendar
            // ═══════════════════════════════════════════════════════════════
            SettingsSectionHeader(title = "Liturgical Calendar")
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Show the current liturgical day on Home and surface seasonal packs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LiturgicalCalendarPicker(
                current = liturgicalCalendar,
                onSelect = { viewModel.setLiturgicalCalendar(it) }
            )

            SectionDivider()

            // ═══════════════════════════════════════════════════════════════
            // Profile
            // ═══════════════════════════════════════════════════════════════
            SettingsSectionHeader(title = "Profile")
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Display Name",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            TextField(
                value = editingDisplayName.value,
                onValueChange = { editingDisplayName.value = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Prayer Warrior") }
            )

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.setDisplayName(editingDisplayName.value) },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Save Name")
            }

            SectionDivider()

            // ═══════════════════════════════════════════════════════════════
            // About
            // ═══════════════════════════════════════════════════════════════
            SettingsSectionHeader(title = "About")
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "App Version",
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = "1.0.0",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedButton(
                onClick = { /* TODO: Implement rating */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Rate PrayerQuest")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { /* TODO: Implement share */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Share PrayerQuest")
            }

            SectionDivider()

            // Delete Account
            DeleteAccountSection()

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Sprint 4 composable building blocks
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SectionDivider() {
    Spacer(modifier = Modifier.height(28.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(28.dp))
}

@Composable
private fun ReminderSlotRow(
    config: ReminderSlotConfig,
    onChange: (ReminderSlotConfig) -> Unit
) {
    val personalityState = remember(config.slot, config.personality) {
        mutableStateOf(config.personality)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = config.slot.displayName(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = config.enabled,
                onCheckedChange = { newEnabled ->
                    onChange(config.copy(enabled = newEnabled))
                }
            )
        }

        if (config.enabled) {
            Spacer(modifier = Modifier.height(8.dp))
            TimePickerField(
                label = "Time",
                minuteOfDay = config.minuteOfDay,
                onTimePicked = { newMinute ->
                    onChange(config.copy(minuteOfDay = newMinute))
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = personalityState.value,
                onValueChange = { personalityState.value = it },
                label = { Text("Personality / reminder text") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(6.dp))
            TextButton(
                onClick = {
                    onChange(config.copy(personality = personalityState.value))
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Save wording")
            }
        }
    }
}

/**
 * Compact row combining an enable toggle + a time picker. Used for
 * Spurgeon's two daily slots (morning, evening) which can be toggled
 * independently. When disabled, the time picker is still visible but
 * dimmed to hint that the slot won't fire.
 */
@Composable
private fun ToggleableTimeBlock(
    label: String,
    enabled: Boolean,
    minuteOfDay: Int,
    onToggle: (Boolean) -> Unit,
    onTimePicked: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = enabled,
                onCheckedChange = onToggle
            )
        }
        if (enabled) {
            Spacer(modifier = Modifier.height(8.dp))
            TimePickerField(
                label = "Time",
                minuteOfDay = minuteOfDay,
                onTimePicked = onTimePicked,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TimePickerField(
    label: String,
    minuteOfDay: Int,
    onTimePicked: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hour = minuteOfDay / 60
    val minute = minuteOfDay % 60

    OutlinedButton(
        onClick = {
            TimePickerDialog(
                context,
                { _, pickedHour, pickedMinute ->
                    onTimePicked(pickedHour * 60 + pickedMinute)
                },
                hour,
                minute,
                false  // 12h view
            ).show()
        },
        modifier = modifier
    ) {
        Text(
            text = "$label: ${formatTime(minuteOfDay)}",
            fontSize = 14.sp
        )
    }
}

private fun formatTime(minuteOfDay: Int): String {
    val h = minuteOfDay / 60
    val m = minuteOfDay % 60
    val hour12 = when {
        h == 0 -> 12
        h > 12 -> h - 12
        else -> h
    }
    val ampm = if (h < 12) "AM" else "PM"
    return "%d:%02d %s".format(hour12, m, ampm)
}

private fun ReminderSlot.displayName(): String = when (this) {
    ReminderSlot.MORNING -> "Morning"
    ReminderSlot.MIDDAY -> "Midday"
    ReminderSlot.EVENING -> "Evening"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TraditionChips(
    enabled: Set<Tradition>,
    onToggle: (Tradition) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Tradition.values().forEach { tradition ->
            val isSelected = tradition in enabled
            FilterChip(
                selected = isSelected,
                onClick = { onToggle(tradition) },
                label = { Text(tradition.displayName) },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
private fun ModeToggleRow(
    mode: PrayerMode,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mode.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = mode.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle
        )
    }
}

@Composable
private fun DevotionalAuthorPicker(
    current: DevotionalAuthor,
    onSelect: (DevotionalAuthor) -> Unit
) {
    val options = listOf(
        DevotionalAuthor.NONE to "None",
        DevotionalAuthor.SPURGEON to "Spurgeon",
        DevotionalAuthor.BONHOEFFER to "Bonhoeffer",
        DevotionalAuthor.BOTH to "Both"
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (author, label) ->
            SegmentedButton(
                selected = current == author,
                onClick = { onSelect(author) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size)
            ) {
                Text(label, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun LiturgicalCalendarPicker(
    current: LiturgicalCalendar,
    onSelect: (LiturgicalCalendar) -> Unit
) {
    val options = listOf(
        LiturgicalCalendar.NONE to "None",
        LiturgicalCalendar.WESTERN to "Western",
        LiturgicalCalendar.EASTERN to "Eastern"
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = current == value,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size)
            ) {
                Text(label, fontSize = 13.sp)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Existing composables (unchanged below)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun DeleteAccountSection() {
    val context = LocalContext.current
    val app = context.applicationContext as PrayerQuestApplication
    val authManager = app.container.firebaseAuthManager
    val prayerGroupRepository = app.container.prayerGroupRepository

    val authState by authManager.authState.collectAsState()
    val isSignedIn = authState is AuthState.SignedIn

    val coroutineScope = rememberCoroutineScope()
    val showDeleteDialog = remember { mutableStateOf(false) }
    val showConfirmDialog = remember { mutableStateOf(false) }
    val isDeleting = remember { mutableStateOf(false) }
    val deleteResult = remember { mutableStateOf<String?>(null) }

    SettingsSectionHeader(title = "Account")
    Spacer(modifier = Modifier.height(12.dp))

    if (isSignedIn) {
        val user = (authState as AuthState.SignedIn).user
        Text(
            text = "Signed in as ${user.email ?: user.displayName ?: "Unknown"}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Button(
            onClick = { showDeleteDialog.value = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isDeleting.value
        ) {
            if (isDeleting.value) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onError,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Deleting Account...")
            } else {
                Text("Delete My Account")
            }
        }

        if (deleteResult.value != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = deleteResult.value!!,
                style = MaterialTheme.typography.bodySmall,
                color = if (deleteResult.value!!.startsWith("Account"))
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
        }

        Text(
            text = "This will permanently delete your account, remove you from all Prayer Groups, and delete your shared prayer requests from the cloud.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    } else {
        Text(
            text = "Not signed in. Sign in via Prayer Groups to manage your account.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showDeleteDialog.value) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog.value = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete Account?") },
            text = {
                Text(
                    "This will permanently delete your account and remove all your data from Prayer Groups. " +
                    "Your local prayer data (journal, gratitude entries, progress) will NOT be affected — " +
                    "only cloud data is removed.\n\nThis action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog.value = false
                        showConfirmDialog.value = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog.value = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showConfirmDialog.value) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog.value = false },
            title = { Text("Are you absolutely sure?") },
            text = {
                Text(
                    "You are about to permanently delete your PrayerQuest account. " +
                    "You will be removed from all Prayer Groups and your shared prayers will be deleted."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog.value = false
                        isDeleting.value = true
                        deleteResult.value = null
                        coroutineScope.launch {
                            val result = prayerGroupRepository.deleteAccount()
                            isDeleting.value = false
                            deleteResult.value = if (result.isSuccess) {
                                "Account deleted successfully."
                            } else {
                                "Deletion failed: ${result.exceptionOrNull()?.message ?: "Unknown error"}. Please try again or contact jedimasterlenny@gmail.com."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Yes, Delete My Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog.value = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun ThemePreviewCard(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(theme.lightBackground))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = theme.name,
                style = MaterialTheme.typography.labelLarge,
                color = Color(theme.lightOnBackground),
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(theme.lightPrimary))
                )
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(theme.lightSecondary))
                )
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(theme.lightSurface))
                        .border(1.dp, Color(theme.lightOnBackground))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = theme.description,
                style = MaterialTheme.typography.labelSmall,
                color = Color(theme.lightOnBackground),
                maxLines = 2
            )

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.End)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(theme.lightPrimary))
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalChipRow(
    options: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            OutlinedButton(
                onClick = { onSelect(option) },
                modifier = Modifier
                    .then(
                        if (selected == option) {
                            Modifier.background(MaterialTheme.colorScheme.primary)
                        } else {
                            Modifier
                        }
                    )
            ) {
                Text(
                    text = option.toString(),
                    color = if (selected == option) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        }
    }
}
