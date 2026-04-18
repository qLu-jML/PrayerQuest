package com.prayerquest.app.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prayerquest.app.PrayerQuestApplication
import com.prayerquest.app.ads.BannerAdView
import com.prayerquest.app.data.entity.DailyQuest
import com.prayerquest.app.data.repository.DashboardData
import com.prayerquest.app.domain.liturgical.LiturgicalDay
import com.prayerquest.app.domain.liturgical.LiturgicalSeason
import com.prayerquest.app.domain.model.Leveling
import com.prayerquest.app.ui.home.HomeUiState
import com.prayerquest.app.ui.home.HomeViewModel
import com.prayerquest.app.ui.home.HomeViewModelFactory
import com.prayerquest.app.ui.theme.FlameRed
import com.prayerquest.app.ui.theme.StainedGlassAmber
import com.prayerquest.app.ui.theme.StainedGlassViolet
import com.prayerquest.app.ui.theme.SuccessGreen
import com.prayerquest.app.ui.theme.WarningGold
import androidx.compose.ui.res.stringResource
import com.prayerquest.app.R

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
    onPrayerGroups: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    // DD §3.10 — low-profile "In distress? Crisis Prayer →" link surfaces
    // the Crisis Prayer Mode without calling attention to itself. Default
    // no-op for previews / tests that don't wire navigation.
    onCrisisPrayer: () -> Unit = {},
    // DD §3.5.4 — tapping the liturgical-day card should open the Library.
    // Library auto-pins the seasonal pack for Advent/Lent/HolyWeek; this
    // callback is a no-op default so previews and tests don't need to wire
    // navigation.
    onLiturgicalTap: () -> Unit = {}
) {
    val app = LocalContext.current.applicationContext as PrayerQuestApplication
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(
            app.container.gamificationRepository,
            app.container.userPreferences
        )
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
                    liturgicalDay = state.liturgicalDay,
                    onStartPrayer = onStartPrayer,
                    onLogGratitude = onLogGratitude,
                    onPrayerGroups = onPrayerGroups,
                    onOpenSettings = onOpenSettings,
                    onCrisisPrayer = onCrisisPrayer,
                    onLiturgicalTap = onLiturgicalTap
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
    liturgicalDay: LiturgicalDay?,
    onStartPrayer: () -> Unit,
    onLogGratitude: () -> Unit,
    onPrayerGroups: () -> Unit,
    onOpenSettings: () -> Unit,
    onCrisisPrayer: () -> Unit,
    onLiturgicalTap: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Greeting row with streak chips + settings cog
        item {
            GreetingRow(
                streakDays = dashboard.streak.currentStreak,
                hearts = dashboard.streak.hearts,
                freezes = dashboard.streak.freezes,
                onOpenSettings = onOpenSettings
            )
        }

        // 1b. Liturgical day card — renders only when the user has opted in
        //     to a calendar (Western/Eastern) in onboarding or settings. When
        //     the preference is NONE, [liturgicalDay] is null and we render
        //     nothing so non-liturgical users see no regression.
        if (liturgicalDay != null) {
            item {
                LiturgicalCard(
                    day = liturgicalDay,
                    onClick = onLiturgicalTap
                )
            }
        }

        // 2. Level card
        item {
            LevelCard(
                level = dashboard.stats.level,
                totalXp = dashboard.stats.totalXp,
                xpProgress = Leveling.progressWithinLevel(dashboard.stats.totalXp)
            )
        }

        // 3. Start Prayer CTA + subtle Crisis Prayer link beneath it.
        //    DD §3.10: the link is intentionally low-profile — findable but
        //    not attention-grabbing. It sits under the primary CTA rather
        //    than in its own card so the Home shelf isn't crowded with
        //    "help" language on a normal good day.
        item {
            StartPrayerCard(
                onStartPrayer = onStartPrayer,
                onCrisisPrayer = onCrisisPrayer
            )
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

        // 7. Bottom banner ad. Auto-hides for premium users via the
        // LocalIsPremium check inside BannerAdView, so callers don't need
        // to wrap this in an isPremium conditional.
        item { BannerAdView(modifier = Modifier.fillMaxWidth()) }

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
    freezes: Int,
    onOpenSettings: () -> Unit
) {
    // Restructured from a single SpaceBetween row into a two-row layout so the
    // settings cog has a predictable right-top home without fighting the streak
    // chips for width. Top row: greeting + cog. Bottom row: three chip stats.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.home_hi_prayer_warrior),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.home_settings),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChipStat(emoji = "🔥", value = streakDays.toString(), tint = FlameRed)
            Spacer(modifier = Modifier.width(8.dp))
            ChipStat(emoji = "🛡️", value = freezes.toString(), tint = WarningGold)
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
                    text = stringResource(R.string.home_level_x, level),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
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
                        text = stringResource(R.string.home_x_x_xp_x_total, xpProgress.first, xpProgress.second, totalXp),
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
private fun StartPrayerCard(
    onStartPrayer: () -> Unit,
    onCrisisPrayer: () -> Unit
) {
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
                text = stringResource(R.string.home_today_s_session),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Button(
                onClick = onStartPrayer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(stringResource(R.string.common_start_prayer))
            }

            // Subtle "In distress? Crisis Prayer →" link per DD §3.10.
            // Deliberately presented as text-only on a clickable surface
            // (not a button) so it reads as a quiet offering, not a
            // shouting help-button. Padding on the clickable Box gives a
            // finger-friendly tap target (~48dp tall with the text inside)
            // so fingers can find it under stress without hunting.
            // Resolve at @Composable level — `semantics { }` is non-@Composable.
            val crisisLinkLabel = stringResource(R.string.home_in_distress_open_crisis_prayer)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onCrisisPrayer)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .semantics {
                        contentDescription = crisisLinkLabel
                    }
            ) {
                Text(
                    text = stringResource(R.string.home_in_distress_crisis_prayer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
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
                    text = stringResource(R.string.home_daily_quests),
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
                    text = stringResource(R.string.home_x_x, quest.progress, quest.target),
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
            label = stringResource(R.string.home_sessions),
            value = sessions.toString(),
            modifier = Modifier.weight(1f)
        )
        TotalTile(
            label = stringResource(R.string.home_minutes),
            value = minutes.toString(),
            modifier = Modifier.weight(1f)
        )
        TotalTile(
            label = stringResource(R.string.home_badges),
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
// SECTION 1b: LITURGICAL DAY CARD
// ═══════════════════════════════════════════════════════

/**
 * Small, subtle card showing the current liturgical day and season.
 *
 * Rendered only when the user has selected a liturgical tradition in
 * onboarding / settings. The visual language is a gentle stained-glass hint —
 * a thin violet leading bar plus a warm amber dot for the feast/season,
 * over a [MaterialTheme.colorScheme.surfaceVariant] tile. Dark-mode safe
 * because the accent colors are constants and the background is themed.
 *
 * The card is clickable; tapping routes to the Library where the matching
 * seasonal pack (if any) is pinned at the top of the Collections shelf.
 */
@Composable
private fun LiturgicalCard(
    day: LiturgicalDay,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thin stained-glass leading bar — violet stripe + amber dot on
            // top. Keeps the accent subtle at ~4dp wide.
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(6.dp)
                    .height(40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(StainedGlassAmber)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(StainedGlassViolet)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = day.dayName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = day.season.displayName(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Human-readable label for a [LiturgicalSeason]. Kept as a file-local
 * extension so it isn't part of the engine's public surface area — the
 * engine deals in enum values; the UI layer picks how to render them.
 */
@Composable
private fun LiturgicalSeason.displayName(): String = when (this) {
    LiturgicalSeason.ADVENT -> stringResource(R.string.home_advent)
    LiturgicalSeason.CHRISTMAS -> stringResource(R.string.home_christmas)
    LiturgicalSeason.EPIPHANY -> stringResource(R.string.home_epiphany)
    LiturgicalSeason.ORDINARY_TIME -> stringResource(R.string.home_ordinary_time)
    LiturgicalSeason.LENT -> stringResource(R.string.home_lent)
    LiturgicalSeason.HOLY_WEEK -> stringResource(R.string.home_holy_week)
    LiturgicalSeason.EASTER -> stringResource(R.string.home_easter)
    LiturgicalSeason.PENTECOST -> stringResource(R.string.home_pentecost)
    LiturgicalSeason.ORDINARY_TIME_2 -> stringResource(R.string.home_ordinary_time)
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
            Text(stringResource(R.string.home_log_gratitude))
        }
        OutlinedButton(
            onClick = onPrayerGroups,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
        ) {
            Text(stringResource(R.string.home_prayer_groups))
        }
    }
}
