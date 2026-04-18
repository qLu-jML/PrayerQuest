package com.prayerquest.app.data.repository

import com.prayerquest.app.data.dao.*
import com.prayerquest.app.data.entity.*
import com.prayerquest.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * The gamification hot path — adapted from ScriptureQuest's GamificationRepository.
 * Handles XP, streaks, quests, achievements, and coins.
 *
 * Called after every prayer session, gratitude log, and group prayer action.
 */
class GamificationRepository(
    private val userStatsDao: UserStatsDao,
    private val streakDao: StreakDao,
    private val dailyQuestDao: DailyQuestDao,
    private val achievementProgressDao: AchievementProgressDao,
    private val dailyActivityDao: DailyActivityDao,
    private val prayerRecordDao: PrayerRecordDao,
    private val gratitudeEntryDao: GratitudeEntryDao,
    private val prayerGroupDao: PrayerGroupDao,
    /** Backs the TESTIMONY and PARTIAL_ANSWER derived badge categories. */
    private val prayerItemDao: PrayerItemDao,
    /** Backs the NAMES_OF_GOD derived badge category. */
    private val nameOfGodDao: NameOfGodDao,
    /** Backs the FAMOUS_DISTINCT derived badge category. */
    private val famousPrayerDao: FamousPrayerDao
) {

    companion object {
        const val STREAK_DAYS_PER_FREEZE = 7
        const val QUESTS_PER_DAY = 3
        const val QUEST_SET_BONUS_XP = 30
        const val QUEST_SET_BONUS_COINS = 5
        /** Flat XP bonus awarded by [onPrayerMarkedAnswered]. */
        const val ANSWERED_PRAYER_XP = 25

        /** "Early bird" / Morning Watch cutoff — sessions that start strictly BEFORE this hour count. */
        const val EARLY_BIRD_HOUR = 6
        /** "Night owl" / Evening Meditation cutoff — sessions that start on or AFTER this hour count. */
        const val NIGHT_OWL_HOUR = 22

        /**
         * Flat XP bonus awarded when a Gratitude Speed Round finishes under
         * 60 seconds with ≥ 5 entries. Small but visibly separate from the
         * per-entry XP so the "Grateful heart, fast hands!" toast has
         * something concrete to show.
         */
        const val GRATITUDE_SPEED_BONUS_XP = 15
        private val DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE
    }

    private fun today(): String = LocalDate.now().format(DATE_FMT)

    // ═══════════════════════════════════════════════
    // PUBLIC HOT PATH — called after prayer session
    // ═══════════════════════════════════════════════

    /**
     * Main entry point after a prayer session completes.
     * Returns a result object with toast/confetti data for the UI.
     */
    suspend fun onPrayerSessionCompleted(
        mode: PrayerMode,
        grade: PrayerGrade,
        durationSeconds: Int,
        itemsPrayed: Int,
        isFamousPrayer: Boolean,
        isGroupPrayer: Boolean,
        /**
         * True when the session recorded voice transcript content (Voice
         * Record & Reflect, or any mode where the user spoke into the
         * transcriber). Drives VOICE-category badges. Defaults to `false`
         * so existing callers don't need to change.
         */
        hasVoiceTranscript: Boolean = false,
        /**
         * True when the session recorded journal text (either the journal
         * field on the record, or PRAYER_JOURNAL mode). Drives JOURNAL-
         * category badges. `PRAYER_JOURNAL` alone is not enough — we still
         * require the user to actually write — but the call site flips this
         * for any free-text entry too.
         */
        hasJournalText: Boolean = false
    ): SessionGamificationResult {
        val date = today()
        val stats = userStatsDao.get() ?: UserStats()
        val previousXp = stats.totalXp

        // 1. Calculate XP
        val baseXp = mode.baseXp
        val durationBonus = (durationSeconds / 60) * 2  // 2 XP per minute
        val rawXp = (baseXp + durationBonus)
        val xpEarned = (rawXp * grade.xpMultiplier).toInt().coerceAtLeast(1)

        // 2. Award XP + update level
        val newTotalXp = stats.totalXp + xpEarned
        val newLevel = Leveling.levelForXp(newTotalXp)
        userStatsDao.addXpAndSetLevel(xpEarned, newLevel)

        // 3. Update session stats
        val minutes = (durationSeconds + 59) / 60  // round up
        userStatsDao.addSessionStats(minutes)
        // Peak session tracker for the LONGEST_SESSION badge category
        // (Thirty Still at 30 min, Holy Hour at 60 min). Monotone — so
        // a shorter session after a long one does not regress the peak.
        userStatsDao.raiseLongestSession(minutes)
        if (isFamousPrayer) userStatsDao.incrementFamousPrayers()
        if (isGroupPrayer) userStatsDao.incrementGroupPrayers()

        // 3a. Lifetime counters that back the expanded badge roster
        //     (voice / journal / sunday / morning-watch / evening-med).
        //     Journals are flagged either by explicit content or by mode.
        val journalSession = hasJournalText || mode == PrayerMode.PRAYER_JOURNAL
        if (hasVoiceTranscript) userStatsDao.incrementVoiceSessions()
        if (journalSession) userStatsDao.incrementJournalSessions()

        val now = LocalTime.now()
        val nowDate = LocalDate.parse(date, DATE_FMT)
        if (nowDate.dayOfWeek == DayOfWeek.SUNDAY) {
            userStatsDao.incrementSundaySessions()
        }
        // Flip the one-shot early-bird / night-owl flags on the first
        // qualifying session — historically these were declared on
        // UserStats but never set, so the corresponding badges never
        // unlocked. Setting via `update(copy(...))` is safe because both
        // flags are monotonic one-way transitions.
        val needsEarlyBird = !stats.hasEarlyBirdSession && now.hour < EARLY_BIRD_HOUR
        val needsNightOwl = !stats.hasNightOwlSession && now.hour >= NIGHT_OWL_HOUR
        if (needsEarlyBird || needsNightOwl) {
            val refreshed = userStatsDao.get() ?: stats
            userStatsDao.update(
                refreshed.copy(
                    hasEarlyBirdSession = refreshed.hasEarlyBirdSession || needsEarlyBird,
                    hasNightOwlSession = refreshed.hasNightOwlSession || needsNightOwl
                )
            )
        }

        // 4. Streak check-in
        val streakResult = checkInStreak(date, stats)

        // 5. Update daily activity
        val activity = dailyActivityDao.getForDate(date) ?: DailyActivity(date = date)
        dailyActivityDao.upsert(
            activity.copy(
                sessionsCompleted = activity.sessionsCompleted + 1,
                prayerMinutes = activity.prayerMinutes + minutes,
                xpEarned = activity.xpEarned + xpEarned
            )
        )

        // 6. Generate/advance daily quests
        ensureQuestsGenerated(date)
        advanceQuests(date, mode, minutes, itemsPrayed, isFamousPrayer, isGroupPrayer, false)

        // 7. Evaluate achievements
        val newAchievements = evaluateAchievements()

        // 8. Check level-up
        val leveledUp = Leveling.didLevelUp(previousXp, newTotalXp)

        return SessionGamificationResult(
            xpEarned = xpEarned,
            totalXp = newTotalXp,
            newLevel = newLevel,
            leveledUp = leveledUp,
            levelTitle = Leveling.titleForLevel(newLevel),
            streakDays = streakResult.currentStreak,
            newAchievements = newAchievements
        )
    }

    /**
     * Called when gratitude entries are logged.
     *
     * [speedBonus] is `true` when the entries came from the Gratitude Speed
     * Round (§3.6) finishing with ≥ 5 items in under 60 seconds — the bonus
     * adds a flat [GRATITUDE_SPEED_BONUS_XP] on top of the per-entry XP. The
     * screen surfaces the bonus via a toast ("Grateful heart, fast hands!")
     * so the number here is the only place where bonus XP is awarded; the
     * UI just displays it.
     */
    suspend fun onGratitudeLogged(
        count: Int,
        hasPhoto: Boolean,
        speedBonus: Boolean = false
    ): Int {
        val date = today()
        val stats = userStatsDao.get() ?: UserStats()

        // XP: 5 base + 3 per extra + 5 for photo (+15 speed bonus)
        val baseXp = 5 + ((count - 1).coerceAtLeast(0) * 3) + (if (hasPhoto) 5 else 0)
        val bonus = if (speedBonus) GRATITUDE_SPEED_BONUS_XP else 0
        val xpEarned = baseXp + bonus
        val newTotalXp = stats.totalXp + xpEarned
        val newLevel = Leveling.levelForXp(newTotalXp)

        userStatsDao.addXpAndSetLevel(xpEarned, newLevel)
        userStatsDao.addGratitudes(count)
        if (hasPhoto) userStatsDao.incrementGratitudePhotos()

        // Update daily activity
        val activity = dailyActivityDao.getForDate(date) ?: DailyActivity(date = date)
        dailyActivityDao.upsert(
            activity.copy(
                gratitudesLogged = activity.gratitudesLogged + count,
                xpEarned = activity.xpEarned + xpEarned
            )
        )

        // Gratitude also protects streak
        checkInStreak(date, stats)

        // Recompute consecutive-gratitude-days (DD §3.6 "Thankful Heart" /
        // "Gratitude Starter" badges). We look at today + yesterday only —
        // cheap, and if the user logs the same day twice it stays flat.
        updateConsecutiveGratitudeDays(stats, date)

        // Advance quests
        ensureQuestsGenerated(date)
        advanceQuestType(date, QuestType.LOG_GRATITUDE.name, count)

        evaluateAchievements()
        return xpEarned
    }

    /**
     * Refreshes `consecutiveGratitudeDays` on UserStats. Called at the end of
     * each [onGratitudeLogged] so the streak-based gratitude badges (7 / 30
     * consecutive days) see an up-to-date value. We intentionally rely on
     * the DailyActivity table rather than the raw entries — the user logs
     * multiple entries on the same date but that's still one "day" of
     * gratitude.
     */
    private suspend fun updateConsecutiveGratitudeDays(stats: UserStats, today: String) {
        val todayDate = LocalDate.parse(today, DATE_FMT)
        val yesterday = todayDate.minusDays(1).format(DATE_FMT)

        val yesterdayCount = gratitudeEntryDao.getCountForDate(yesterday)
        val newConsecutive = when {
            // First-ever gratitude log today → streak becomes 1.
            stats.consecutiveGratitudeDays == 0 -> 1
            // Logged again today, consecutive day holds.
            yesterdayCount > 0 -> stats.consecutiveGratitudeDays.coerceAtLeast(1)
            // User missed yesterday — reset to 1 (today still counts).
            else -> 1
        }

        if (newConsecutive != stats.consecutiveGratitudeDays) {
            userStatsDao.setConsecutiveGratitudeDays(newConsecutive)
        }

        // Peak tracker for the "Hundred Thankful Days" badge. Has to survive
        // a reset of the live counter — once a user has strung 100 days
        // together the badge is earned for good.
        userStatsDao.raiseLongestGratitudeStreak(newConsecutive)
    }

    /**
     * Called exactly once per "Answered" transition — see
     * [com.prayerquest.app.data.repository.PrayerRepository.markAnswered]
     * and its [PrayerRepository.MarkAnsweredResult.wasNewlyAnswered] guard.
     *
     * Bumps [UserStats.answeredPrayerCount] atomically, awards a flat XP
     * bonus for God's faithfulness (the completion itself is the emotional
     * payoff — see DD §3.5.2 "generous animation is the emotional payoff"),
     * and re-evaluates achievements so the ANSWERED_PRAYER category
     * ("God Answers" → "Faithful Witness" → "Testimony Builder" — the
     * FIRST_ANSWERED / FAITHFUL_10 / FAITHFUL_50 milestones) can unlock
     * on the spot.
     *
     * Returns the list of achievements newly unlocked by this call so the
     * celebration screen can surface them alongside the confetti.
     */
    suspend fun onPrayerMarkedAnswered(): AnsweredGamificationResult {
        val stats = userStatsDao.get() ?: UserStats()
        val xpEarned = ANSWERED_PRAYER_XP
        val newTotalXp = stats.totalXp + xpEarned
        val newLevel = Leveling.levelForXp(newTotalXp)
        val leveledUp = Leveling.didLevelUp(stats.totalXp, newTotalXp)

        userStatsDao.addXpAndSetLevel(xpEarned, newLevel)
        userStatsDao.incrementAnsweredPrayers()

        // Re-read so achievement evaluation sees the incremented
        // answeredPrayerCount.
        val newAchievements = evaluateAchievements()

        return AnsweredGamificationResult(
            xpEarned = xpEarned,
            totalXp = newTotalXp,
            newLevel = newLevel,
            leveledUp = leveledUp,
            newAchievements = newAchievements
        )
    }

    // ═══════════════════════════════════════════════
    // STREAK LOGIC (identical to ScriptureQuest)
    // ═══════════════════════════════════════════════

    private suspend fun checkInStreak(date: String, stats: UserStats): StreakData {
        val streak = streakDao.get() ?: StreakData()
        val today = LocalDate.parse(date, DATE_FMT)

        if (streak.lastActiveDate == date) return streak  // already checked in today

        val lastActive = if (streak.lastActiveDate.isNotEmpty()) {
            LocalDate.parse(streak.lastActiveDate, DATE_FMT)
        } else null

        val gapDays = if (lastActive != null) {
            ChronoUnit.DAYS.between(lastActive, today).toInt()
        } else 0

        var newStreak = streak.currentStreak
        var newFreezes = streak.freezes
        var newHearts = streak.hearts
        var lastFreezeDate = streak.lastFreezeUsedDate

        when {
            gapDays <= 1 -> {
                // Consecutive day or first ever
                newStreak += 1
            }
            gapDays > 1 -> {
                // Missed days — try to consume freezes, then hearts
                val missedDays = gapDays - 1
                var covered = 0
                // Use freezes first
                while (covered < missedDays && newFreezes > 0) {
                    newFreezes--
                    covered++
                    lastFreezeDate = date
                }
                // Then hearts
                while (covered < missedDays && newHearts > 0) {
                    newHearts--
                    covered++
                }
                if (covered >= missedDays) {
                    newStreak += 1  // streak preserved
                } else {
                    // Streak breaks
                    if (streak.currentStreak >= 7) {
                        // Mark comeback for achievement
                        userStatsDao.update(stats.copy(hasComebackAfterGap = true))
                    }
                    newStreak = 1
                }
            }
        }

        // Auto-earn freeze every 7 consecutive streak days
        if (newStreak > 0 && newStreak % STREAK_DAYS_PER_FREEZE == 0) {
            newFreezes++
        }

        val updatedStreak = streak.copy(
            currentStreak = newStreak,
            longestStreak = maxOf(streak.longestStreak, newStreak),
            lastActiveDate = date,
            lastFreezeUsedDate = lastFreezeDate,
            hearts = newHearts,
            freezes = newFreezes
        )
        streakDao.update(updatedStreak)

        return updatedStreak
    }

    // ═══════════════════════════════════════════════
    // DAILY QUESTS
    // ═══════════════════════════════════════════════

    private suspend fun ensureQuestsGenerated(date: String) {
        if (dailyQuestDao.getCountForDate(date) >= QUESTS_PER_DAY) return

        val questTypes = QuestType.generateDailySet()
        val quests = questTypes.mapIndexed { index, type ->
            DailyQuest(
                date = date,
                slotIndex = index,
                questTypeId = type.name,
                target = type.defaultTarget,
                xpReward = type.xpReward,
                coinReward = type.coinReward
            )
        }
        dailyQuestDao.insertAll(quests)

        // Prune old quests (older than 14 days)
        val cutoff = LocalDate.parse(date, DATE_FMT).minusDays(14).format(DATE_FMT)
        dailyQuestDao.pruneOlderThan(cutoff)
    }

    private suspend fun advanceQuests(
        date: String,
        mode: PrayerMode,
        minutes: Int,
        itemsPrayed: Int,
        isFamousPrayer: Boolean,
        isGroupPrayer: Boolean,
        isJournal: Boolean
    ) {
        advanceQuestType(date, QuestType.PRAY_MINUTES.name, minutes)
        advanceQuestType(date, QuestType.COMPLETE_SESSION.name, 1)
        advanceQuestType(date, QuestType.PRAY_FOR_ITEMS.name, itemsPrayed)

        // Mode diversity — check distinct modes used today
        val distinctModes = prayerRecordDao.getDistinctModesForDate(date)
        setQuestProgress(date, QuestType.USE_MODES.name, distinctModes)

        if (isFamousPrayer) advanceQuestType(date, QuestType.FAMOUS_PRAYER.name, 1)
        if (isGroupPrayer) advanceQuestType(date, QuestType.GROUP_PRAYER.name, 1)
        if (isJournal || mode == PrayerMode.PRAYER_JOURNAL) {
            advanceQuestType(date, QuestType.JOURNAL_ENTRY.name, 1)
        }

        // Check for full-set bonus
        val completedCount = dailyQuestDao.getCompletedCountForDate(date)
        if (completedCount >= QUESTS_PER_DAY) {
            val stats = userStatsDao.get() ?: return
            userStatsDao.addXpAndSetLevel(QUEST_SET_BONUS_XP, Leveling.levelForXp(stats.totalXp + QUEST_SET_BONUS_XP))
            userStatsDao.addCoins(QUEST_SET_BONUS_COINS)
        }
    }

    private suspend fun advanceQuestType(date: String, questTypeId: String, amount: Int) {
        val quests = dailyQuestDao.getForDate(date)
        val quest = quests.find { it.questTypeId == questTypeId && !it.completed } ?: return
        val newProgress = quest.progress + amount
        val completed = newProgress >= quest.target
        dailyQuestDao.updateProgress(date, quest.slotIndex, newProgress, completed)
        if (completed && !quest.rewardClaimed) {
            userStatsDao.addXpAndSetLevel(quest.xpReward, Leveling.levelForXp((userStatsDao.get()?.totalXp ?: 0) + quest.xpReward))
            userStatsDao.addCoins(quest.coinReward)
            dailyQuestDao.claimReward(date, quest.slotIndex)
        }
    }

    private suspend fun setQuestProgress(date: String, questTypeId: String, absoluteValue: Int) {
        val quests = dailyQuestDao.getForDate(date)
        val quest = quests.find { it.questTypeId == questTypeId && !it.completed } ?: return
        val completed = absoluteValue >= quest.target
        dailyQuestDao.updateProgress(date, quest.slotIndex, absoluteValue, completed)
        if (completed && !quest.rewardClaimed) {
            userStatsDao.addXpAndSetLevel(quest.xpReward, Leveling.levelForXp((userStatsDao.get()?.totalXp ?: 0) + quest.xpReward))
            userStatsDao.addCoins(quest.coinReward)
            dailyQuestDao.claimReward(date, quest.slotIndex)
        }
    }

    // ═══════════════════════════════════════════════
    // ACHIEVEMENTS
    // ═══════════════════════════════════════════════

    private suspend fun evaluateAchievements(): List<AchievementDef> {
        val stats = userStatsDao.get() ?: return emptyList()
        val streak = streakDao.get() ?: StreakData()
        val newlyUnlocked = mutableListOf<AchievementDef>()

        for (def in Achievements.ALL) {
            val currentProgress = achievementProgressDao.getById(def.id)
            if (currentProgress?.isUnlocked == true) continue  // already earned

            val currentValue = getValueForAchievement(def, stats, streak)

            // Ensure progress row exists
            if (currentProgress == null) {
                achievementProgressDao.insert(AchievementProgress(achievementId = def.id, currentValue = currentValue))
            } else {
                achievementProgressDao.update(currentProgress.copy(currentValue = currentValue))
            }

            if (currentValue >= def.targetValue) {
                // Unlock!
                achievementProgressDao.update(
                    AchievementProgress(
                        achievementId = def.id,
                        currentValue = currentValue,
                        unlockedAt = System.currentTimeMillis(),
                        rewardClaimed = true
                    )
                )
                userStatsDao.addXpAndSetLevel(def.xpReward, Leveling.levelForXp(stats.totalXp + def.xpReward))
                userStatsDao.addCoins(def.coinReward)
                newlyUnlocked.add(def)
            }
        }
        return newlyUnlocked
    }

    private suspend fun getValueForAchievement(
        def: AchievementDef,
        stats: UserStats,
        streak: StreakData
    ): Int = when (def.category) {
        AchievementCategory.STREAK -> streak.currentStreak
        AchievementCategory.PRAYER_MINUTES -> stats.totalPrayerMinutes
        AchievementCategory.ITEMS_PRAYED -> stats.totalItemsPrayed
        AchievementCategory.MODE_DIVERSITY -> stats.totalDistinctModesUsed
        AchievementCategory.FAMOUS_PRAYER -> stats.totalFamousPrayersSaid
        AchievementCategory.ANSWERED_PRAYER -> stats.answeredPrayerCount
        AchievementCategory.GRATITUDE -> when (def.id) {
            // Consecutive-day badges: Gratitude Starter (7 days) and
            // Thankful Heart (30 consecutive days). These read the
            // live `consecutiveGratitudeDays` counter maintained by
            // [updateConsecutiveGratitudeDays] — the old behaviour
            // (compare target to `totalGratitudesLogged`) would have
            // unlocked Gratitude Starter after the user's 7th entry
            // even if all 7 landed on the same day.
            "gratitude_7", "gratitude_30" -> stats.consecutiveGratitudeDays
            "gratitude_photo_50" -> stats.totalGratitudePhotos
            // "Hundred Thankful Days" — peak tracker so the badge survives
            // a reset of the live counter (100 consecutive days ever).
            "gratitude_100_streak" -> stats.longestGratitudeStreak
            // Volume milestones (Abundance Mindset, Faithful Thanksgiver)
            // stay on the raw lifetime-entry counter.
            else -> stats.totalGratitudesLogged
        }
        AchievementCategory.GROUP -> when (def.id) {
            "group_first" -> stats.totalGroupsJoined
            "group_pray_50" -> stats.totalGroupPrayersLogged
            "group_create_5" -> stats.totalGroupsCreated
            else -> 0
        }
        AchievementCategory.LEVELING -> stats.level
        AchievementCategory.XP_MILESTONE -> stats.totalXp
        AchievementCategory.SESSION -> stats.totalSessions
        AchievementCategory.TIME_OF_DAY -> when (def.id) {
            "early_bird" -> if (stats.hasEarlyBirdSession) 1 else 0
            "night_owl" -> if (stats.hasNightOwlSession) 1 else 0
            else -> 0
        }
        AchievementCategory.COMEBACK -> if (stats.hasComebackAfterGap) 1 else 0

        // ── Sprint-18 expansion (2026-04-18) ────────────────────────────
        AchievementCategory.LONGEST_SESSION -> stats.longestSessionMinutes
        AchievementCategory.VOICE -> stats.totalVoiceSessions
        AchievementCategory.JOURNAL -> stats.totalJournalSessions
        AchievementCategory.SABBATH -> stats.totalSundaySessions
        AchievementCategory.TESTIMONY -> prayerItemDao.getAnsweredWithTestimonyCount()
        AchievementCategory.PARTIAL_ANSWER -> prayerItemDao.getPartiallyAnsweredCount()
        AchievementCategory.NAMES_OF_GOD -> nameOfGodDao.getDistinctPrayedCount()
        AchievementCategory.FAMOUS_DISTINCT -> famousPrayerDao.getDistinctPrayedCount()
        AchievementCategory.MODE_MASTERY -> {
            // The AchievementDef.id is `<modeSlug>_N` where modeSlug is the
            // PrayerMode enum `name.lowercase()` (e.g., "guided_acts_25" ↔
            // PrayerMode.GUIDED_ACTS). Strip the trailing `_N` by finding
            // the last underscore and uppercasing the prefix back to the
            // enum name.
            val mode = def.id.substringBeforeLast('_').uppercase()
            prayerRecordDao.getCountByMode(mode)
        }
    }

    // ═══════════════════════════════════════════════
    // DASHBOARD FLOW (reactive combine)
    // ═══════════════════════════════════════════════

    fun observeDashboard(): Flow<DashboardData> {
        val date = today()
        return combine(
            userStatsDao.observe(),
            streakDao.observe(),
            dailyQuestDao.observeForDate(date),
            dailyActivityDao.observeForDate(date),
            achievementProgressDao.observeUnlockedCount()
        ) { stats, streak, quests, activity, achievementCount ->
            DashboardData(
                stats = stats ?: UserStats(),
                streak = streak ?: StreakData(),
                todayQuests = quests,
                todayActivity = activity ?: DailyActivity(date = date),
                unlockedAchievementCount = achievementCount
            )
        }
    }

    /** Observe UserStats for Profile screen. */
    fun observeStats(): Flow<UserStats?> {
        return userStatsDao.observe()
    }

    /** Observe all achievements for Profile screen. */
    fun observeAchievements(): Flow<List<AchievementProgress>> {
        return achievementProgressDao.observeAll()
    }
}

// ═══════════════════════════════════════════════
// RESULT DATA CLASSES
// ═══════════════════════════════════════════════

data class SessionGamificationResult(
    val xpEarned: Int,
    val totalXp: Int,
    val newLevel: Int,
    val leveledUp: Boolean,
    val levelTitle: String,
    val streakDays: Int,
    val newAchievements: List<AchievementDef>
)

/**
 * Result of [GamificationRepository.onPrayerMarkedAnswered] — surfaced to
 * the Big Celebration screen so it can show "+25 XP", any brand-new
 * achievements, and a level-up ribbon on the verse card.
 */
data class AnsweredGamificationResult(
    val xpEarned: Int,
    val totalXp: Int,
    val newLevel: Int,
    val leveledUp: Boolean,
    val newAchievements: List<AchievementDef>
)

data class DashboardData(
    val stats: UserStats,
    val streak: StreakData,
    val todayQuests: List<DailyQuest>,
    val todayActivity: DailyActivity,
    val unlockedAchievementCount: Int
)
