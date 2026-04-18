package com.prayerquest.app.notifications

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.prayerquest.app.PrayerQuestApplication
import com.prayerquest.app.data.preferences.ReminderSlot
import com.prayerquest.app.data.preferences.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Schedules the periodic notification workers. All fire times pull from
 * UserPreferences so that what the user chose in Settings / Onboarding is
 * what ships to the scheduler — no hardcoded 9:00 AM defaults here.
 *
 * Callers:
 *  - [AppContainer.init] runs this once on every launch. It MUST pass its
 *    own [UserPreferences] — during startup `application.container` is still
 *    being assigned, so looking it up would NPE. Post-startup callers can
 *    omit it and we'll fall back to the container.
 *  - [rescheduleAll] is invoked from Settings / Onboarding when the user
 *    changes any time-affecting preference (quiet hours, reminder slots).
 *    We use REPLACE policy on rescheduling so the new time wins immediately.
 */
object NotificationScheduler {

    fun scheduleAllNotifications(context: Context, prefs: UserPreferences? = null) {
        NotificationHelper.createNotificationChannel(context)

        val resolved = prefs ?: (context.applicationContext as PrayerQuestApplication)
            .container.userPreferences

        runBlocking {
            scheduleReminderSlots(context, resolved)
            scheduleStreakAlert(context)            // fixed 20:00 — evening streak-save
            scheduleQuestNotification(context)      // fixed 08:00 — new quest drop
            scheduleGratitudePrompt(context)        // fixed 19:00 — post-dinner prompt
        }
    }

    /**
     * Preferences-aware re-schedule. Called by Settings / Onboarding whenever
     * the user changes a time. Uses REPLACE policy so the new time wins right
     * away; the old periodic work is silently discarded.
     */
    fun rescheduleAll(context: Context, prefs: UserPreferences? = null) {
        val resolved = prefs ?: (context.applicationContext as PrayerQuestApplication)
            .container.userPreferences

        runBlocking {
            scheduleReminderSlots(context, resolved, replace = true)
            // streak / quest / gratitude run on fixed times for MVP —
            // rescheduling them is a no-op but kept for symmetry.
            scheduleStreakAlert(context, replace = true)
            scheduleQuestNotification(context, replace = true)
            scheduleGratitudePrompt(context, replace = true)
        }
    }

    // --- Reminder slots (morning / midday / evening) -------------------

    private suspend fun scheduleReminderSlots(
        context: Context,
        prefs: UserPreferences,
        replace: Boolean = false
    ) {
        val slots = prefs.reminderSlots.first()
        val wm = WorkManager.getInstance(context)
        val policy = if (replace) ExistingPeriodicWorkPolicy.REPLACE
                     else ExistingPeriodicWorkPolicy.KEEP

        slots.forEach { slot ->
            val workName = "daily_prayer_reminder_${slot.slot.name.lowercase()}"
            if (!slot.enabled) {
                // User turned this slot off — cancel any previously scheduled
                // work under its unique name so stale reminders don't survive.
                wm.cancelUniqueWork(workName)
                return@forEach
            }

            val delay = delayUntilMinuteOfDay(slot.minuteOfDay)
            val request = PeriodicWorkRequestBuilder<DailyPrayerReminderWorker>(
                1, TimeUnit.DAYS
            )
                .setInitialDelay(delay, TimeUnit.MINUTES)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofMinutes(15))
                .addTag("reminder_${slot.slot.name.lowercase()}")
                .build()

            wm.enqueueUniquePeriodicWork(workName, policy, request)
        }

        // Cancel orphaned slots that no longer exist in the list. Defensive —
        // shouldn't happen under the current design, but cheap insurance.
        val validNames = ReminderSlot.values().map {
            "daily_prayer_reminder_${it.name.lowercase()}"
        }.toSet()
        val allSlotNames = ReminderSlot.values().map {
            "daily_prayer_reminder_${it.name.lowercase()}"
        }
        allSlotNames.filter { it !in validNames }.forEach { wm.cancelUniqueWork(it) }
    }

    // --- Streak / quest / gratitude (fixed times for MVP) --------------

    private fun scheduleStreakAlert(context: Context, replace: Boolean = false) {
        val request = PeriodicWorkRequestBuilder<StreakAlertWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayUntilMinuteOfDay(20 * 60), TimeUnit.MINUTES)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofMinutes(15))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "streak_alert",
            if (replace) ExistingPeriodicWorkPolicy.REPLACE else ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun scheduleQuestNotification(context: Context, replace: Boolean = false) {
        val request = PeriodicWorkRequestBuilder<QuestNotificationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayUntilMinuteOfDay(8 * 60), TimeUnit.MINUTES)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofMinutes(15))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "quest_notification",
            if (replace) ExistingPeriodicWorkPolicy.REPLACE else ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun scheduleGratitudePrompt(context: Context, replace: Boolean = false) {
        val request = PeriodicWorkRequestBuilder<GratitudePromptWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayUntilMinuteOfDay(19 * 60), TimeUnit.MINUTES)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofMinutes(15))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "gratitude_prompt",
            if (replace) ExistingPeriodicWorkPolicy.REPLACE else ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    // --- Time helpers --------------------------------------------------

    /**
     * Minutes from now until the next occurrence of [minuteOfDay] (0-1439).
     * If the target has already passed today, rolls over to tomorrow.
     */
    private fun delayUntilMinuteOfDay(minuteOfDay: Int): Long {
        val hour = minuteOfDay / 60
        val minute = minuteOfDay % 60

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }
        val delayMillis = target.timeInMillis - now.timeInMillis
        return TimeUnit.MILLISECONDS.toMinutes(delayMillis)
    }
}
