package com.prayerquest.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per individual prayer event for a group prayer item.
 *
 * Unlike [GroupPrayerItem.prayedByCount] (a lifetime aggregate), this preserves
 * the *when* and *who* of each prayer. That lets the UI show "prayed by X this
 * week" chips, build the activity feed called out in DD §3.6, and support
 * future features like intercessor leaderboards without another migration.
 *
 * Written every time a member prays for a group item — from either the one-tap
 * "I prayed this" button or a full prayer session. Mirrored to Firestore under
 * /groups/{groupId}/prayerItems/{itemId}/activity/{activityId} when the user is
 * signed in and the group is cloud-synced.
 */
@Entity(
    tableName = "group_prayer_activity",
    foreignKeys = [
        ForeignKey(
            entity = PrayerGroup::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = GroupPrayerItem::class,
            parentColumns = ["id"],
            childColumns = ["groupPrayerItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("groupId"),
        Index("groupPrayerItemId"),
        // Composite index accelerates "recent activity for this item" queries,
        // which are the hot path for the weekly-count chip.
        Index(value = ["groupPrayerItemId", "prayedAt"]),
        // Unique index is the deduplication primitive for cloud sync. Both the
        // local writer and the remote puller attach the same client-generated
        // [firestoreId], so a subsequent pull that re-sees the same activity
        // will be silently ignored (OnConflictStrategy.IGNORE on bulk insert).
        // SQLite treats each NULL as distinct, so rows written before a
        // Firestore round-trip (e.g. offline) coexist without conflict.
        Index(value = ["firestoreId"], unique = true)
    ]
)
data class GroupPrayerActivity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val groupPrayerItemId: Long,
    val userId: String,                 // who prayed (auth UID or "local_user")
    val displayName: String = "",       // denormalized for feed display + offline sync
    val prayedAt: Long = System.currentTimeMillis(),
    val firestoreId: String? = null     // activity doc ID when cloud-synced
)
