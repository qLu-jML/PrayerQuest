package com.prayerquest.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Many-to-many join between PrayerCollection and PrayerItem.
 */
@Entity(
    tableName = "prayer_collection_cross_ref",
    primaryKeys = ["collectionId", "prayerItemId"],
    foreignKeys = [
        ForeignKey(
            entity = PrayerCollection::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
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
data class PrayerCollectionCrossRef(
    val collectionId: Long,
    val prayerItemId: Long
)
