package com.prayerquest.app.ui.screen

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.prayerquest.app.ui.premium.DonateCard
import com.prayerquest.app.ui.profile.ProfileUiState
import com.prayerquest.app.ui.profile.ProfileViewModel
import com.prayerquest.app.ui.profile.ProfileViewModelFactory
import com.prayerquest.app.ui.theme.LocalIsPremium
import com.prayerquest.app.ui.theme.SuccessGreen

/**
 * Profile Screen — Full implementation matching ScriptureQuest's layout.
 *
 * Sections:
 * 1. Profile header card (level, name, title, XP progress)
 * 2. Lifetime stats grid
 * 3. Achievement categories with progress bars
 */
@Composable
fun ProfileScreen(
    onOpenSettings: () -> Unit = {},
    onNavigateToPaywall: () -> Unit = {}
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
                onOpenSettings = onOpenSettings,
                onNavigateToPaywall = onNavigateToPaywall
            )
        }
    }
}

@Composable
private fun ProfileContent(
    stats: UserStats,
    achievements: List<AchievementProgress>,
    onOpenSettings: () -> Unit,
    onNavigateToPaywall: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Profile header
        item {
            ProfileHeaderCard(stats = stats)
        }

        // 1b. Settings shortcut — previously a top-level bottom-nav tab, now
        //     reachable from here and from the Home cog. Kept right under the
        //     header so users who expect "Settings lives on Profile" can find it.
        item {
            SettingsShortcutCard(onOpenSettings = onOpenSettings)
        }

        // 1c. Premium upgrade / badge card.
        //     Free users: warm CTA that routes to the paywall.
        //     Premium users: subtle "thank you" badge that still shows (so the
        //     card space doesn't jarringly disappear between sessions, and it
        //     doubles as a receipt/affirmation of their membership).
        item {
            PremiumStatusCard(
                onNavigateToPaywall = onNavigateToPaywall,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        // 2. Support the mission (Buy Me a Coffee)
        //    Placed directly below the header so it's visible without
        //    scrolling — same position ScriptureQuest uses for its donate card.
        item {
            DonateCard(modifier = Modifier.padding(horizontal = 8.dp))
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

/**
 * Settings entry point on the Profile tab. Matches the visual weight of the
 * other cards on the screen (padded, rounded, full-width) so it doesn't look
 * like an afterthought.
 */
@Composable
private fun SettingsShortcutCard(onOpenSettings: () -> Unit) {
    OutlinedButton(
        onClick = onOpenSettings,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .height(56.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Settings,
            contentDescription = null,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

/**
 * Premium status card. Shows two very different states based on
 * [LocalIsPremium]:
 *
 *  • Free user — warm gradient-free primary-container card with a
 *    WorkspacePremium icon, a short benefit list, and an "Upgrade" CTA that
 *    routes to the paywall. Deliberately understated (not flashy gold/glitter)
 *    so it doesn't feel pushy alongside the DonateCard that sits right below.
 *
 *  • Premium user — tertiaryContainer-toned badge with a check icon that
 *    simply says "Premium supporter — thank you." No CTA, no "manage
 *    subscription" button here (that lives in Settings); this card is purely
 *    affirmation so the user sees that their membership is recognized every
 *    time they land on Profile.
 */
@Composable
private fun PremiumStatusCard(
    onNavigateToPaywall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPremium = LocalIsPremium.current
    if (isPremium) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Premium Supporter",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "Thank you for sustaining PrayerQuest.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.WorkspacePremium,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PrayerQuest Premium",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Ad-free · Unlimited gratitude photos · Larger prayer groups",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onNavigateToPaywall,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Upgrade to Premium",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
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
                        text = "Level ${stats.level}",
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
// SECTION 2: LIFETIME STATS
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
