package com.prayerquest.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single completed prayer session record.
 * Links to either a PrayerItem, FamousPrayer, or GroupPrayerItem (one of).
 */
@Entity(tableName = "prayer_records")
data class PrayerRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val prayerItemId: Long? = null,
    val famousPrayerId: String? = null,
    val groupPrayerItemId: Long? = null,
    val mode: String = "",                     // PrayerMode enum name
    val durationSeconds: Int = 0,
    val grade: String = "",                    // Again | Hard | Good | Easy
    val depthRating: Int = 0,                  // 0-5 optional slider
    val voiceTranscript: String? = null,
    val journalText: String? = null,
    val xpEarned: Int = 0,
    val sessionDate: String = ""               // yyyy-MM-dd
)
