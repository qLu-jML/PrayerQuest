package com.prayerquest.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prayerquest.app.data.preferences.ThemeMode
import com.prayerquest.app.data.preferences.UserPreferences
import com.prayerquest.app.ui.settings.SettingsViewModel
import com.prayerquest.app.ui.settings.ThemePickerDialog
import com.prayerquest.app.ui.theme.AppTheme
import com.prayerquest.app.ui.theme.ThemeRepository
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    userPreferences: UserPreferences,
    onNavigateBack: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel { SettingsViewModel(userPreferences) }
) {
    val themeMode by viewModel.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val selectedThemeId by viewModel.selectedThemeId.collectAsState(initial = "prayer_quest")
    val dailyGoal by viewModel.dailyGoal.collectAsState(initial = 10)
    val gratitudeGoal by viewModel.gratitudeGoal.collectAsState(initial = 3)
    val displayName by viewModel.displayName.collectAsState(initial = "Prayer Warrior")

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
        // Top bar with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
        // Theme Section
        SettingsSectionHeader(title = "Theme")
        Spacer(modifier = Modifier.height(12.dp))

        // Theme Mode Picker
        Text(
            text = "Theme Mode",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ThemeMode.values().forEachIndexed { _, mode ->
                SegmentedButton(
                    selected = themeMode == mode,
                    onClick = {
                        coroutineScope.launch {
                            viewModel.setThemeMode(mode)
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(mode.name, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Theme Picker Grid
        Text(
            text = "Select Theme",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Theme grid — use fixed height to avoid nested-scrollable crash
        val themes = ThemeRepository.getAllBuiltInThemes()
        val rows = (themes.size + 1) / 2 // 2 columns
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
                    onClick = {
                        coroutineScope.launch {
                            viewModel.setSelectedThemeId(theme.id)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Create Custom Theme Button
        Button(
            onClick = { showThemeDialog.value = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Custom Theme")
        }

        Spacer(modifier = Modifier.height(28.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(28.dp))

        // Prayer Goals Section
        SettingsSectionHeader(title = "Prayer Goals")
        Spacer(modifier = Modifier.height(12.dp))

        // Daily Prayer Goal
        Text(
            text = "Daily Prayer Goal (minutes)",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        GoalChipRow(
            options = listOf(5, 10, 15, 20, 30, 45, 60),
            selected = dailyGoal,
            onSelect = { goal ->
                coroutineScope.launch {
                    viewModel.setDailyGoal(goal)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Daily Gratitude Goal
        Text(
            text = "Daily Gratitude Goal (entries)",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        GoalChipRow(
            options = listOf(1, 3, 5),
            selected = gratitudeGoal,
            onSelect = { goal ->
                coroutineScope.launch {
                    viewModel.setGratitudeGoal(goal)
                }
            }
        )

        Spacer(modifier = Modifier.height(28.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(28.dp))

        // Display Name Section
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
            onClick = {
                coroutineScope.launch {
                    viewModel.setDisplayName(editingDisplayName.value)
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Save Name")
        }

        Spacer(modifier = Modifier.height(28.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(28.dp))

        // About Section
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

        Spacer(modifier = Modifier.height(32.dp))
        } // inner scrollable column
    } // outer column
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
            // Theme name
            Text(
                text = theme.name,
                style = MaterialTheme.typography.labelLarge,
                color = Color(theme.lightOnBackground),
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Color swatches
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Primary color
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(theme.lightPrimary))
                )

                // Secondary color
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(theme.lightSecondary))
                )

                // Background preview (if different from card)
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(theme.lightSurface))
                        .border(1.dp, Color(theme.lightOnBackground))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = theme.description,
                style = MaterialTheme.typography.labelSmall,
                color = Color(theme.lightOnBackground),
                maxLines = 2
            )

            // Check mark if selected
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
