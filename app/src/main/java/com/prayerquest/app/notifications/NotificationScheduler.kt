package com.prayerquest.app.notifications

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    fun scheduleAllNotifications(context: Context) {
        // Create notification channel
        NotificationHelper.createNotificationChannel(context)

        // Schedule Daily Prayer Reminder @ 9:00 AM
        scheduleDailyPrayerReminder(context)

        // Schedule Streak Alert @ 8:00 PM
        scheduleStreakAlert(context)

        // Schedule Quest Notification @ 8:00 AM
        scheduleQuestNotification(context)

        // Schedule Gratitude Prompt @ 7:00 PM
        scheduleGratitudePrompt(context)
    }

    private fun scheduleDailyPrayerReminder(context: Context) {
        val dailyPrayerWorkRequest = PeriodicWorkRequestBuilder<DailyPrayerReminderWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(getDelayUntilTime(9, 0), TimeUnit.MINUTES)
            .setBackoffPolicy(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily_prayer_reminder",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            dailyPrayerWorkRequest
        )
    }

    private fun scheduleStreakAlert(context: Context) {
        val streakAlertWorkRequest = PeriodicWorkRequestBuilder<StreakAlertWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(getDelayUntilTime(20, 0), TimeUnit.MINUTES)
            .setBackoffPolicy(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "streak_alert",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            streakAlertWorkRequest
        )
    }

    private fun scheduleQuestNotification(context: Context) {
        val questNotificationWorkRequest = PeriodicWorkRequestBuilder<QuestNotificationWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(getDelayUntilTime(8, 0), TimeUnit.MINUTES)
            .setBackoffPolicy(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "quest_notification",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            questNotificationWorkRequest
        )
    }

    private fun scheduleGratitudePrompt(context: Context) {
        val gratitudePromptWorkRequest = PeriodicWorkRequestBuilder<GratitudePromptWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(getDelayUntilTime(19, 0), TimeUnit.MINUTES)
            .setBackoffPolicy(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "gratitude_prompt",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            gratitudePromptWorkRequest
        )
    }

    private fun getDelayUntilTime(hour: Int, minute: Int): Long {
        val now = java.util.Calendar.getInstance()
        val target = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
        }

        if (target.before(now)) {
            target.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }

        val delayMillis = target.timeInMillis - now.timeInMillis
        return TimeUnit.MILLISECONDS.toMinutes(delayMillis)
    }
}
