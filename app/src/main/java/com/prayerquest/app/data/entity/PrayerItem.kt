package com.prayerquest.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-created prayer request / topic.
 * Status transitions: Active → PartiallyAnswered ↔ Active → Answered (archived)
 *
 * [photoUri] — optional **Photo Prayer** (DD §3.9). A user-attached photo that
 * stays with the item for its entire active life (e.g., a photo of the person
 * being prayed for). Distinct from [testimonyPhotoUri], which is only captured
 * at the moment a prayer is answered.
 *
 * **Privacy invariant:** this field is strictly local. It is stored in
 * app-private storage (see `PhotoStorage`) and is NEVER uploaded to Firestore
 * — not even when the item is shared with a Prayer Group. Enforced by
 * [com.prayerquest.app.firebase.FirestoreGroupService.addPrayerItem] accepting
 * only title/description strings, not the full [PrayerItem].
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
    val isUserCreated: Boolean = true,
    /**
     * Photo Prayer URI — absolute file path (filesDir-relative) to the
     * compressed JPEG stored in app-private storage. Null when the user
     * hasn't attached a photo. See DD §3.9.
     */
    val photoUri: String? = null
) {
    companion object {
        const val STATUS_ACTIVE = "Active"
        const val STATUS_PARTIALLY_ANSWERED = "PartiallyAnswered"
        const val STATUS_ANSWERED = "Answered"
    }
}
