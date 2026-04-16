package com.prayerquest.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-created prayer request / topic.
 * Status transitions: Active → PartiallyAnswered ↔ Active → Answered (archived)
 */
@Entity(tableName = "prayer_items")
data class PrayerItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = "",
    val status: String = STATUS_ACTIVE,          // Active | PartiallyAnswered | Answered
    val createdAt: Long = System.currentTimeMillis(),
    val answeredAt: Long? = null,
    val testimonyText: String? = null,
    val testimonyPhotoUri: String? = null,
    val testimonyVoiceUri: String? = null,
    val isUserCreated: Boolean = true
) {
    companion object {
        const val STATUS_ACTIVE = "Active"
        const val STATUS_PARTIALLY_ANSWERED = "PartiallyAnswered"
        const val STATUS_ANSWERED = "Answered"
    }
}
