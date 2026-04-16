package com.prayerquest.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Membership record linking a user to a PrayerGroup.
 * Role: Admin (creator) or Member.
 */
@Entity(
    tableName = "prayer_group_members",
    foreignKeys = [
        ForeignKey(
            entity = PrayerGroup::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("groupId")]
)
data class PrayerGroupMember(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val userId: String = "local_user",         // local for MVP
    val role: String = ROLE_MEMBER,            // Admin | Member
    val joinedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val ROLE_ADMIN = "Admin"
        const val ROLE_MEMBER = "Member"
    }
}
