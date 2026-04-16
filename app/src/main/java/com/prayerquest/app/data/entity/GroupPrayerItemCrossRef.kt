package com.prayerquest.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Many-to-many cross-ref between PrayerGroup and PrayerItem.
 * Complements GroupPrayerItem for direct relationship queries.
 */
@Entity(
    tableName = "group_prayer_item_cross_ref",
    primaryKeys = ["groupId", "prayerItemId"],
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
    indices = [Index("prayerItemId")]
)
data class GroupPrayerItemCrossRef(
    val groupId: Long,
    val prayerItemId: Long
)
