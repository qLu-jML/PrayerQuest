package com.prayerquest.app.data.dao

import androidx.room.*
import com.prayerquest.app.data.entity.GroupPrayerItem
import com.prayerquest.app.data.entity.GroupPrayerItemCrossRef
import com.prayerquest.app.data.entity.PrayerGroup
import com.prayerquest.app.data.entity.PrayerGroupMember
import com.prayerquest.app.data.entity.PrayerItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerGroupDao {

    // --- Group CRUD ---
    @Insert
    suspend fun insertGroup(group: PrayerGroup): Long

    @Update
    suspend fun updateGroup(group: PrayerGroup)

    @Delete
    suspend fun deleteGroup(group: PrayerGroup)

    @Query("SELECT * FROM prayer_groups WHERE id = :id")
    suspend fun getGroupById(id: Long): PrayerGroup?

    @Query("SELECT * FROM prayer_groups WHERE id = :id")
    fun observeGroupById(id: Long): Flow<PrayerGroup?>

    @Query("SELECT * FROM prayer_groups ORDER BY createdAt DESC")
    fun observeAllGroups(): Flow<List<PrayerGroup>>

    @Query("SELECT * FROM prayer_groups WHERE shareCode = :code")
    suspend fun getGroupByShareCode(code: String): PrayerGroup?

    @Query("SELECT COUNT(*) FROM prayer_groups")
    suspend fun getGroupCount(): Int

    // --- Membership ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMember(member: PrayerGroupMember): Long

    @Delete
    suspend fun deleteMember(member: PrayerGroupMember)

    @Query("SELECT * FROM prayer_group_members WHERE groupId = :groupId")
    fun observeMembers(groupId: Long): Flow<List<PrayerGroupMember>>

    @Query("SELECT COUNT(*) FROM prayer_group_members WHERE groupId = :groupId")
    suspend fun getMemberCount(groupId: Long): Int

    @Query("DELETE FROM prayer_group_members WHERE groupId = :groupId AND userId = :userId")
    suspend fun removeMember(groupId: Long, userId: String)

    // --- Group Prayer Items ---
    @Insert
    suspend fun insertGroupPrayerItem(item: GroupPrayerItem): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGroupCrossRef(crossRef: GroupPrayerItemCrossRef)

    @Query("UPDATE group_prayer_items SET prayedByCount = prayedByCount + 1 WHERE id = :id")
    suspend fun incrementPrayedByCount(id: Long)

    @Query("""
        SELECT pi.* FROM prayer_items pi
        INNER JOIN group_prayer_items gpi ON pi.id = gpi.prayerItemId
        WHERE gpi.groupId = :groupId
        ORDER BY gpi.addedAt DESC
    """)
    fun observeGroupPrayerItems(groupId: Long): Flow<List<PrayerItem>>

    @Query("SELECT * FROM group_prayer_items WHERE groupId = :groupId ORDER BY addedAt DESC")
    fun observeGroupPrayerItemDetails(groupId: Long): Flow<List<GroupPrayerItem>>

    @Query("SELECT COUNT(*) FROM group_prayer_items WHERE groupId = :groupId")
    suspend fun getGroupPrayerItemCount(groupId: Long): Int
}
