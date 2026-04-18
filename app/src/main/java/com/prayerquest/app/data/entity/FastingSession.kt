package com.prayerquest.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-declared fasting session, paired with prayer intentions and
 * (optionally) linked prayer items. Medical disclaimer is shown before a
 * fast is started — see `assets/legal/fasting_disclaimer.md` (Sprint 6).
 *
 * Status lifecycle: ACTIVE → COMPLETED | BROKEN | CANCELLED.
 * Linked prayer item IDs are stored as a comma-delimited string so the
 * schema stays flat (a real join table is reserved for v1.1 if needed).
 */
@Entity(tableName = "fasting_session")
data class FastingSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDate: String,                     // yyyy-MM-dd
    val endDate: String? = null,               // yyyy-MM-dd, null while active
    val plannedDurationHours: Int,
    val type: String,                          // e.g., "WATER", "PARTIAL", "SOCIAL_MEDIA", "SUNRISE_TO_SUNSET"
    val intention: String,                     // what the user is fasting and praying about
    val linkedPrayerItemIds: String = "",      // CSV of PrayerItem.id values
    val journalEntries: String = "",           // pipe-delimited "yyyy-MM-dd HH:mm|text" lines
    val status: String = "ACTIVE",             // ACTIVE | COMPLETED | BROKEN | CANCELLED
    val createdAt: Long = System.currentTimeMillis()
)
