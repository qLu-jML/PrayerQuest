package com.prayerquest.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A prayer request shared within a Prayer Group.
 * Links a PrayerItem to a PrayerGroup with metadata about who added it and prayer counts.
 */
@Entity(
    tableName = "group_prayer_items",
    foreignKeys = [
        ForeignKey(
            entity = PrayerGroup::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PrayerItem::class,
            parentColumns = ["id"],
            childColumns = ["prayerItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("groupId"), Index("prayerItemId")]
)
data class GroupPrayerItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val prayerItemId: Long,
    val addedBy: String = "local_user",
    val addedAt: Long = System.currentTimeMillis(),
    val prayedByCount: Int = 0
)
