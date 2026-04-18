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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.res.stringResource
import com.prayerquest.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userPreferences: UserPreferences,
    onNavigateBack: () -> Unit = {},
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
    val displayName by viewModel.displayName.collectAsState(initial = stringResource(R.string.common_prayer_warrior))
    val reminderSlots by viewModel.reminderSlots.collectAsState(initial = emptyList())
    val quietHoursEnabled by viewModel.quietHoursEnabled.collectAsState(initial = true)
    val quietHoursStartMin by viewModel.quietHoursStartMin.collectAsState(initial = UserPreferences.DEFAULT_QUIET_START_MIN)
    val quietHoursEndMin by viewModel.quietHoursEndMin.collectAsState(initial = UserPreferences.DEFAULT_QUIET_END_MIN)
    val enabledTraditions by viewModel.enabledTraditions.collectAsState(initial = Tradition.DEFAULT)
    val disabledModes by viewModel.disabledModes.collectAsState(initial = emptySet())
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
        // Top bar with back button — returns the user to the screen they
        // came from (Profile or Home cog). Settings is no longer a bottom-
        // nav tab, so the back arrow is the primary way out.
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.home_settings),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.common_back)
                    )
                }
            }
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
            SettingsSectionHeader(title = stringResource(R.string.settings_theme))
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.settings_theme_mode),
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
                text = stringResource(R.string.settings_select_theme),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val themes = ThemeRepository.getAllBuiltInThemes()
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                themes.forEach { theme ->
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
                Text(stringResource(R.string.settings_create_custom_theme))
            }

            SectionDivider()

            // ═══════════════════════════════════════════════════════════════
            // Prayer Goals
            // ═══════════════════════════════════════════════════════════════
            SettingsSectionHeader(title = stringResource(R.string.settings_prayer_goals))
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.settings_daily_prayer_goal_minutes),
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
                text = stringResource(R.string.settings_daily_gratitude_goal_entries),
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
            SettingsSectionHeader(title = stringResource(R.string.settings_reminders))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.settings_choose_when_prayerquest_nudges_you_to_pray_each_sl),
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
            SettingsSectionHeader(title = stringResource(R.string.settings_quiet_hours))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.settings_pause_notifications_during_this_window_reminders_s),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_enable_quiet_hours),
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
                        label = stringResource(R.string.common_start),
                        minuteOfDay = quietHoursStartMin,
                        onTimePicked = { newStart ->
                            viewModel.setQuietHoursWindow(newStart, quietHoursEndMin)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    TimePickerField(
                        label = stringResource(R.string.common_end),
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
                        text = stringResource(R.string.settings_this_window_crosses_midnight),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SectionDivider()

            // ═══════════════════════════════════════════════════════════════
            // Traditions
            // ═══════════════════════════════════════════════════════════════
            SettingsSectionHeader(title = stringResource(R.string.settings_traditions))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.settings_pick_any_that_speak_to_you_shapes_which_prayer_mod),
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
            SettingsSectionHeader(title = stringResource(R.string.settings_prayer_modes))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.settings_fine_tune_which_modes_appear_in_your_mode_picker_p),
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
            // Liturgical Calendar
            // ═══════════════════════════════════════════════════════════════
            SettingsSectionHeader(title = stringResource(R.string.settings_liturgical_calendar))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.settings_show_the_current_liturgical_day_on_home_and_surfac),
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
            SettingsSectionHeader(title = stringResource(R.string.settings_profile))
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.settings_display_name),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            TextField(
                value = editingDisplayName.value,
                onValueChange = { editingDisplayName.value = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.common_prayer_warrior)) }
            )

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.setDisplayName(editingDisplayName.value) },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.settings_save_name))
            }

            SectionDivider()

            // ═══════════════════════════════════════════════════════════════
            // About
            // ═══════════════════════════════════════════════════════════════
            SettingsSectionHeader(title = stringResource(R.string.settings_about))
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.settings_app_version),
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
                Text(stringResource(R.string.settings_rate_prayerquest))
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { /* TODO: Implement share */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_share_prayerquest))
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
                label = stringResource(R.string.common_time),
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
                label = { Text(stringResource(R.string.settings_personality_reminder_text)) },
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
                Text(stringResource(R.string.settings_save_wording))
            }
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
            text = stringResource(R.string.common_x_x_2, label, formatTime(minuteOfDay)),
            fontSize = 14.sp
        )
    }
}

@Composable
private fun formatTime(minuteOfDay: Int): String {
    val h = minuteOfDay / 60
    val m = minuteOfDay % 60
    val hour12 = when {
        h == 0 -> 12
        h > 12 -> h - 12
        else -> h
    }
    val ampm = if (h < 12) "AM" else "PM"
    return stringResource(R.string.common_d_02d_s).format(hour12, m, ampm)
}

@Composable
private fun ReminderSlot.displayName(): String = when (this) {
    ReminderSlot.MORNING -> stringResource(R.string.onboarding_morning)
    ReminderSlot.MIDDAY -> stringResource(R.string.onboarding_midday)
    ReminderSlot.EVENING -> stringResource(R.string.onboarding_evening)
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
private fun LiturgicalCalendarPicker(
    current: LiturgicalCalendar,
    onSelect: (LiturgicalCalendar) -> Unit
) {
    val options = listOf(
        LiturgicalCalendar.NONE to stringResource(R.string.common_none),
        LiturgicalCalendar.WESTERN to stringResource(R.string.common_western),
        LiturgicalCalendar.EASTERN to stringResource(R.string.common_eastern)
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

    SettingsSectionHeader(title = stringResource(R.string.settings_account))
    Spacer(modifier = Modifier.height(12.dp))

    if (isSignedIn) {
        val user = (authState as AuthState.SignedIn).user
        val userLabel = user.email ?: user.displayName ?: stringResource(R.string.common_unknown)
        Text(
            text = stringResource(R.string.settings_signed_in_as_user, userLabel),
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
                Text(stringResource(R.string.settings_deleting_account))
            } else {
                Text(stringResource(R.string.settings_delete_my_account))
            }
        }

        if (deleteResult.value != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = deleteResult.value!!,
                style = MaterialTheme.typography.bodySmall,
                color = if (deleteResult.value!!.startsWith(stringResource(R.string.settings_account)))
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
        }

        Text(
            text = stringResource(R.string.settings_this_will_permanently_delete_your_account_remove_y),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    } else {
        Text(
            text = stringResource(R.string.settings_not_signed_in_sign_in_via_prayer_groups_to_manage),
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
            title = { Text(stringResource(R.string.settings_delete_account)) },
            text = {
                Text(
                    stringResource(R.string.settings_this_will_permanently_delete_your_account_and_remo) +
                    stringResource(R.string.settings_your_local_prayer_data_journal_gratitude_entries_p) +
                    stringResource(R.string.settings_only_cloud_data_is_removed_n_nthis_action_cannot_b)
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
                    Text(stringResource(R.string.common_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog.value = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (showConfirmDialog.value) {
        val accountDeletedMsg = stringResource(R.string.settings_account_deleted_success)
        val unknownErrorMsg = stringResource(R.string.common_unknown_error)
        val deletionFailedTemplate = stringResource(R.string.settings_deletion_failed_template)
        AlertDialog(
            onDismissRequest = { showConfirmDialog.value = false },
            title = { Text(stringResource(R.string.settings_are_you_absolutely_sure)) },
            text = {
                Text(
                    stringResource(R.string.settings_you_are_about_to_permanently_delete_your_prayerque) +
                    stringResource(R.string.settings_you_will_be_removed_from_all_prayer_groups_and_you)
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
                                accountDeletedMsg
                            } else {
                                val errMsg = result.exceptionOrNull()?.message ?: unknownErrorMsg
                                String.format(deletionFailedTemplate, errMsg)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.settings_yes_delete_my_account))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog.value = false }) {
                    Text(stringResource(R.string.common_cancel))
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

/**
 * Theme row, single-column, ported one-to-one from ScriptureQuest's
 * ThemeCard so PrayerQuest's palette picker renders with the same density
 * and richness: RadioButton + name/description on top, a 10-swatch palette
 * strip below. Each swatch surfaces one semantic slot from the theme —
 * primary, primaryLight, primaryDark, secondary, backgroundPaper,
 * backgroundDefault, accent, success, warning, info.
 */
@Composable
private fun ThemePreviewCard(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = 2.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = MaterialTheme.shapes.large
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = onClick
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = theme.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = theme.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Color palette row — aligns under the name column (40.dp start
            // inset skips the RadioButton width so the swatches lead flush
            // with the theme name).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    theme.primary,
                    theme.primaryLight,
                    theme.primaryDark,
                    theme.secondary,
                    theme.backgroundPaper,
                    theme.backgroundDefault,
                    theme.accent,
                    theme.success,
                    theme.warning,
                    theme.info
                ).forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(color, shape = MaterialTheme.shapes.small)
                            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
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
