package com.prayerquest.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prayerquest.app.PrayerQuestApplication
import kotlin.random.Random
import com.prayerquest.app.R

class QuestNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val userPrefs = (applicationContext as PrayerQuestApplication)
                .container.userPreferences
            if (!QuietHoursGuard.canPostNow(userPrefs)) return Result.success()

            val title = applicationContext.getString(R.string.notifications_new_daily_quests_are_ready)
            val messages = listOf(
                applicationContext.getString(R.string.notifications_complete_all_3_for_bonus_xp),
                applicationContext.getString(R.string.notifications_today_s_quests_await_you_earn_rewards),
                applicationContext.getString(R.string.notifications_start_your_quest_journey_for_extra_rewards),
                applicationContext.getString(R.string.notifications_new_challenges_are_ready_to_complete),
                applicationContext.getString(R.string.notifications_unlock_rewards_by_finishing_today_s_quests)
            )
            val message = messages[Random.nextInt(messages.size)]

            NotificationHelper.post(
                applicationContext,
                NOTIFICATION_ID,
                title,
                message
            )

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1003
    }
}
