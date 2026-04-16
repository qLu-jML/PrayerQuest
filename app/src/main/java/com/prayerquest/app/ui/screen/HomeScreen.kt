package com.prayerquest.app.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prayerquest.app.PrayerQuestApplication
import com.prayerquest.app.data.entity.DailyQuest
import com.prayerquest.app.data.repository.DashboardData
import com.prayerquest.app.domain.model.Leveling
import com.prayerquest.app.ui.home.HomeUiState
import com.prayerquest.app.ui.home.HomeViewModel
import com.prayerquest.app.ui.home.HomeViewModelFactory
import com.prayerquest.app.ui.theme.FlameRed
import com.prayerquest.app.ui.theme.SuccessGreen
import com.prayerquest.app.ui.theme.WarningGold

/**
 * Home Dashboard — Full implementation matching ScriptureQuest's layout.
 *
 * Sections:
 * 1. Greeting row with streak chips
 * 2. Level card with XP progress
 * 3. "Start Prayer" CTA
 * 4. Daily quests (3 slots)
 * 5. Totals strip (Sessions / Minutes / Badges)
 * 6. Quick actions (Log Gratitude / Prayer Groups)
 */
@Composable
fun HomeScreen(
    onStartPrayer: () -> Unit = {},
    onLogGratitude: () -> Unit = {},
    onPrayerGroups: () -> Unit = {}
) {
    val app = LocalContext.current.applicationContext as PrayerQuestApplication
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(app.container.gamificationRepository)
    )
    val uiState by viewModel.uiState.collectAsState(HomeUiState.Loading)

    AnimatedContent(
        targetState = uiState,
        transitionSpec = {
            (slideInVertically { it } + fadeIn()) togetherWith
                    (slideOutVertically { -it } + fadeOut()) using
                    SizeTransform(clip = false)
        }
    ) { state ->
        when (state) {
            HomeUiState.Loading -> LoadingPlaceholder()
            is HomeUiState.Ready -> HomeContent(
                    dashboard = state.dashboard,
                    onStartPrayer = onStartPrayer,
                    onLogGratitude = onLogGratitude,
                    onPrayerGroups = onPrayerGroups
                )
        }
    }
}

@Composable
private fun LoadingPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun HomeContent(
    dashboard: DashboardData,
    onStartPrayer: () -> Unit,
    onLogGratitude: () -> Unit,
    onPrayerGroups: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Greeting row with streak chips
        item {
            GreetingRow(
                streakDays = dashboard.streak.currentStreak,
                hearts = dashboard.stats.hearts,
                freezes = dashboard.stats.freezes
            )
        }

        // 2. Level card
        item {
            LevelCard(
                level = dashboard.stats.level,
                totalXp = dashboard.stats.totalXp,
                xpProgress = Leveling.progressWithinLevel(dashboard.stats.totalXp)
            )
        }

        // 3. Start Prayer CTA
        item {
            StartPrayerCard(onStartPrayer = onStartPrayer)
        }

        // 4. Daily Quests
        item {
            DailyQuestsCard(quests = dashboard.todayQuests)
        }

        // 5. Totals strip
        item {
            TotalsStrip(
                sessions = dashboard.stats.totalSessions,
                minutes = dashboard.stats.totalPrayerMinutes,
                badges = dashboard.unlockedAchievementCount
            )
        }

        // 6. Quick actions
        item {
            QuickActionsRow(
                onLogGratitude = onLogGratitude,
                onPrayerGroups = onPrayerGroups
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ═══════════════════════════════════════════════════════
// SECTION 1: GREETING ROW
// ═══════════════════════════════════════════════════════

@Composable
private fun GreetingRow(
    streakDays: Int,
    hearts: Int,
    freezes: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Hi, Prayer Warrior 👋",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChipStat(emoji = "🔥", value = streakDays.toString(), tint = FlameRed)
            Spacer(modifier = Modifier.width(8.dp))
            ChipStat(emoji = "❄️", value = freezes.toString(), tint = WarningGold)
            Spacer(modifier = Modifier.width(8.dp))
            ChipStat(emoji = "❤️", value = "$hearts/3", tint = MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
private fun ChipStat(
    emoji: String,
    value: String,
    tint: Color
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tint.copy(alpha = 0.15f)),
        color = tint.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                fontSize = 16.sp,
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = tint
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// SECTION 2: LEVEL CARD
// ═══════════════════════════════════════════════════════

@Composable
private fun LevelCard(
    level: Int,
    totalXp: Int,
    xpProgress: Pair<Int, Int>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Level circle
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = level.toString(),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            // Level info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Level $level",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = Leveling.titleForLevel(level),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )

                // XP Progress bar
                Column(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { (xpProgress.first.toFloat() / xpProgress.second).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${xpProgress.first} / ${xpProgress.second} XP · $totalXp total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// SECTION 3: START PRAYER CTA
// ═══════════════════════════════════════════════════════

@Composable
private fun StartPrayerCard(onStartPrayer: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Today's Session",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Button(
                onClick = onStartPrayer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Start Prayer")
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// SECTION 4: DAILY QUESTS
// ═══════════════════════════════════════════════════════

@Composable
private fun DailyQuestsCard(quests: List<DailyQuest>) {
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Quests",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                val completedCount = quests.count { it.completed }
                Text(
                    text = "$completedCount/${quests.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SuccessGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }

            quests.forEachIndexed { index, quest ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
                QuestSlot(quest = quest)
            }
        }
    }
}

@Composable
private fun QuestSlot(quest: DailyQuest) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quest.questTypeId.replace("_", " ").uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${quest.progress} / ${quest.target}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (quest.completed) {
                Surface(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SuccessGreen),
                    color = SuccessGreen
                ) {
                    Text(
                        text = "✓",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }

        // Progress bar
        LinearProgressIndicator(
            progress = { (quest.progress.toFloat() / quest.target).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (quest.completed) SuccessGreen else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )
    }
}

// ═══════════════════════════════════════════════════════
// SECTION 5: TOTALS STRIP
// ═══════════════════════════════════════════════════════

@Composable
private fun TotalsStrip(
    sessions: Int,
    minutes: Int,
    badges: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TotalTile(
            label = "Sessions",
            value = sessions.toString(),
            modifier = Modifier.weight(1f)
        )
        TotalTile(
            label = "Minutes",
            value = minutes.toString(),
            modifier = Modifier.weight(1f)
        )
        TotalTile(
            label = "Badges",
            value = badges.toString(),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TotalTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// SECTION 6: QUICK ACTIONS
// ═══════════════════════════════════════════════════════

@Composable
private fun QuickActionsRow(
    onLogGratitude: () -> Unit,
    onPrayerGroups: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onLogGratitude,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
        ) {
            Text("Log Gratitude")
        }
        OutlinedButton(
            onClick = onPrayerGroups,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
        ) {
            Text("Prayer Groups")
        }
    }
}
