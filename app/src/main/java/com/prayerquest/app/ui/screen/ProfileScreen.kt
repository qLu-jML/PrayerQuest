package com.prayerquest.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prayerquest.app.PrayerQuestApplication
import com.prayerquest.app.data.entity.AchievementProgress
import com.prayerquest.app.data.entity.UserStats
import com.prayerquest.app.domain.model.AchievementCategory
import com.prayerquest.app.domain.model.Achievements
import com.prayerquest.app.domain.model.Leveling
import com.prayerquest.app.ui.profile.ProfileUiState
import com.prayerquest.app.ui.profile.ProfileViewModel
import com.prayerquest.app.ui.profile.ProfileViewModelFactory
import com.prayerquest.app.ui.theme.SuccessGreen

/**
 * Profile Screen — Full implementation matching ScriptureQuest's layout.
 *
 * Sections:
 * 1. Profile header card (level, name, title, XP progress)
 * 2. "The Science Behind PrayerQuest" expandable card (10+ entries)
 * 3. Lifetime stats grid
 * 4. Achievement categories with progress bars
 */
@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit = {}
) {
    val app = LocalContext.current.applicationContext as PrayerQuestApplication
    val viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(app.container.gamificationRepository)
    )
    val uiState by viewModel.uiState.collectAsState(ProfileUiState.Loading)

    when (uiState) {
        ProfileUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is ProfileUiState.Ready -> {
            val state = uiState as ProfileUiState.Ready
            ProfileContent(
                stats = state.stats,
                achievements = state.achievements,
                onNavigateToSettings = onNavigateToSettings
            )
        }
    }
}

@Composable
private fun ProfileContent(
    stats: UserStats,
    achievements: List<AchievementProgress>,
    onNavigateToSettings: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Settings button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 1. Profile header
        item {
            ProfileHeaderCard(stats = stats)
        }

        // 2. Science section
        item {
            ScienceCard()
        }

        // 3. Lifetime stats
        item {
            LifetimeStatsCard(stats = stats)
        }

        // 4. Achievements by category
        item {
            AchievementCategoriesCard(achievements = achievements)
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ═══════════════════════════════════════════════════════
// SECTION 1: PROFILE HEADER
// ═══════════════════════════════════════════════════════

@Composable
private fun ProfileHeaderCard(stats: UserStats) {
    var isEditing by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf("Prayer Warrior") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Level circle
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stats.level.toString(),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                // Name and title
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isEditing) {
                        TextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        )
                    } else {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Text(
                        text = "Level ${stats.level} • ${Leveling.titleForLevel(stats.level)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            // XP Progress bar
            val xpProgress = Leveling.progressWithinLevel(stats.totalXp)
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { (xpProgress.first.toFloat() / xpProgress.second).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${xpProgress.first} / ${xpProgress.second} XP · ${stats.totalXp} total",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
            }

            // Edit button
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { isEditing = !isEditing },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEditing) "Done" else "Edit Name")
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// SECTION 2: SCIENCE CARD
// ═══════════════════════════════════════════════════════

@Composable
private fun ScienceCard() {
    var isExpanded by remember { mutableStateOf(false) }

    val scienceEntries = listOf(
        ScienceEntry(
            title = "Guided ACTS",
            citation = "Implementation Intentions (Gollwitzer, 1999)",
            description = "Structuring prayers (Adoration, Confession, Thanksgiving, Supplication) creates mental frameworks that improve prayer consistency and emotional regulation."
        ),
        ScienceEntry(
            title = "Voice Recording",
            citation = "Production Effect (MacLeod et al., 2010)",
            description = "Speaking prayers aloud engages motor cortex and auditory processing, enhancing memory retention and emotional depth compared to silent reading."
        ),
        ScienceEntry(
            title = "Prayer Journal",
            citation = "Expressive Writing (Pennebaker, 1997)",
            description = "Writing about prayers and reflections improves emotional processing and provides a record of answered prayers, strengthening faith narratives."
        ),
        ScienceEntry(
            title = "Gratitude Blast",
            citation = "Gratitude Psychology (Emmons & McCullough, 2003)",
            description = "Regularly recording gratitude increases positive affect, life satisfaction, and resilience. Visual gratitude logs boost dopamine and motivation."
        ),
        ScienceEntry(
            title = "Intercession Drill",
            citation = "Intercessory Prayer Research",
            description = "Praying for others activates empathy networks and reduces self-focused rumination. Structured intercessory prayer targets specific people systematically."
        ),
        ScienceEntry(
            title = "Scripture Soak",
            citation = "Lectio Divina Tradition",
            description = "Meditative scripture reading (read, reflect, respond, rest) engages multiple neural networks and integrates cognitive and contemplative processing."
        ),
        ScienceEntry(
            title = "Contemplative Silence",
            citation = "Centering Prayer (Keating, 1986)",
            description = "Silent meditation reduces mind-wandering and activates the default mode network, fostering spiritual insight and emotional regulation."
        ),
        ScienceEntry(
            title = "Flash-Pray",
            citation = "Habit Loop (Duhigg, 2012)",
            description = "Short, frequent prayers become automatic habits through repeated cue-response cycles, integrating spirituality into daily life naturally."
        ),
        ScienceEntry(
            title = "Streaks",
            citation = "Habit Formation (Lally et al., 2010)",
            description = "66+ consecutive days of prayer builds stable neural pathways. Visual streaks leverage loss aversion to maintain motivation."
        ),
        ScienceEntry(
            title = "XP & Gamification",
            citation = "Self-Determination Theory (Deci & Ryan, 1985)",
            description = "Autonomy, competence, and relatedness drive intrinsic motivation. XP rewards and achievement recognition sustain prayer engagement long-term."
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "The Science Behind PrayerQuest",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = "Toggle science section"
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    Column(modifier = Modifier.padding(16.dp)) {
                        scienceEntries.forEachIndexed { index, entry ->
                            if (index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                            ScienceEntryRow(entry = entry)
                        }
                    }
                }
            }
        }
    }
}

data class ScienceEntry(
    val title: String,
    val citation: String,
    val description: String
)

@Composable
private fun ScienceEntryRow(entry: ScienceEntry) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "🧠",
                fontSize = 20.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = entry.citation,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Text(
            text = entry.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ═══════════════════════════════════════════════════════
// SECTION 3: LIFETIME STATS
// ═══════════════════════════════════════════════════════

@Composable
private fun LifetimeStatsCard(stats: UserStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Lifetime Stats",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 2-column grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatRow(
                    label1 = "Total XP",
                    value1 = stats.totalXp.toString(),
                    label2 = "Sessions",
                    value2 = stats.totalSessions.toString()
                )
                StatRow(
                    label1 = "Minutes Prayed",
                    value1 = stats.totalPrayerMinutes.toString(),
                    label2 = "Answered Prayers",
                    value2 = stats.answeredPrayerCount.toString()
                )
                StatRow(
                    label1 = "Grace Coins",
                    value1 = stats.graceCoins.toString(),
                    label2 = "Gratitudes Logged",
                    value2 = stats.totalGratitudesLogged.toString()
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Badges Earned",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stats.level.toString(),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatRow(
    label1: String,
    value1: String,
    label2: String,
    value2: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label1,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value1,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label2,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value2,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// SECTION 4: ACHIEVEMENTS BY CATEGORY
// ═══════════════════════════════════════════════════════

@Composable
private fun AchievementCategoriesCard(achievements: List<AchievementProgress>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Achievements",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Group achievements by category
            val categoriesWithAchievements = AchievementCategory.entries.mapNotNull { category ->
                val categoryAchievements = Achievements.byCategory(category)
                if (categoryAchievements.isNotEmpty()) {
                    category to categoryAchievements
                } else null
            }

            categoriesWithAchievements.forEachIndexed { index, (category, defs) ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                AchievementCategoryRow(
                    category = category,
                    definitions = defs,
                    achievements = achievements
                )
            }
        }
    }
}

@Composable
private fun AchievementCategoryRow(
    category: AchievementCategory,
    definitions: List<com.prayerquest.app.domain.model.AchievementDef>,
    achievements: List<AchievementProgress>
) {
    val categoryIcon = when (category) {
        AchievementCategory.STREAK -> "🔥"
        AchievementCategory.PRAYER_MINUTES -> "⏱️"
        AchievementCategory.ITEMS_PRAYED -> "🙏"
        AchievementCategory.MODE_DIVERSITY -> "🎯"
        AchievementCategory.FAMOUS_PRAYER -> "📜"
        AchievementCategory.ANSWERED_PRAYER -> "✅"
        AchievementCategory.GRATITUDE -> "💛"
        AchievementCategory.GROUP -> "👥"
        AchievementCategory.LEVELING -> "📈"
        AchievementCategory.XP_MILESTONE -> "💰"
        AchievementCategory.SESSION -> "🎉"
        AchievementCategory.TIME_OF_DAY -> "🌅"
        AchievementCategory.COMEBACK -> "🕊️"
    }

    val categoryName = category.name.replace("_", " ").lowercase()
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(categoryIcon, fontSize = 20.sp)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = definitions.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Progress bar
        val unlockedCount = definitions.count { def ->
            achievements.find { it.achievementId == def.id }?.isUnlocked == true
        }

        LinearProgressIndicator(
            progress = { (unlockedCount.toFloat() / definitions.size).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = SuccessGreen,
            trackColor = SuccessGreen.copy(alpha = 0.2f)
        )

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (unlockedCount == definitions.size) "✓ Unlocked" else "$unlockedCount/${definitions.size}",
            style = MaterialTheme.typography.labelSmall,
            color = if (unlockedCount == definitions.size) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
    }
}
