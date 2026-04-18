package com.prayerquest.app.data.dao

import androidx.room.*
import com.prayerquest.app.data.entity.GroupPrayerActivity
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

    @Update
    suspend fun updateGroupPrayerItem(item: GroupPrayerItem)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGroupCrossRef(crossRef: GroupPrayerItemCrossRef)

    @Query("UPDATE group_prayer_items SET prayedByCount = prayedByCount + 1 WHERE id = :id")
    suspend fun incrementPrayedByCount(id: Long)

    @Query("UPDATE group_prayer_items SET firestoreId = :firestoreId WHERE id = :id")
    suspend fun setGroupPrayerItemFirestoreId(id: Long, firestoreId: String)

    @Query("SELECT * FROM group_prayer_items WHERE id = :id")
    suspend fun getGroupPrayerItemById(id: Long): GroupPrayerItem?

    /**
     * One-shot list of all group prayer items for a group — used by the cloud
     * sync path to match remote items to local rows by firestoreId without
     * collecting a Flow.
     */
    @Query("SELECT * FROM group_prayer_items WHERE groupId = :groupId")
    suspend fun getGroupPrayerItemsForGroup(groupId: Long): List<GroupPrayerItem>

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

    // --- Group Prayer Activity (per-prayer timestamped log) ---
    /**
     * Insert a single activity row. IGNORE strategy keeps local writes
     * idempotent when the caller pre-generates the firestoreId (any retry or
     * accidental duplicate is a no-op thanks to the unique index on
     * firestoreId).
     *
     * Returns the rowId of the inserted row, or -1 if the insert was ignored.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertActivity(activity: GroupPrayerActivity): Long

    /**
     * Bulk-insert activities pulled from Firestore. Rows whose firestoreId
     * already exists locally are silently skipped — this is how the sync path
     * stays idempotent when pulling the same 7-day window repeatedly.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRemoteActivities(activities: List<GroupPrayerActivity>)

    /**
     * Count of prayer events for a single item since the given timestamp.
     * Used for the "X prayed this week" chip.
     */
    @Query("""
        SELECT COUNT(*) FROM group_prayer_activity
        WHERE groupPrayerItemId = :itemId AND prayedAt >= :sinceMs
    """)
    suspend fun getActivityCountSince(itemId: Long, sinceMs: Long): Int

    /**
     * Reactive map of itemId -> count-of-prayers-since-timestamp for every item
     * in the group. Streams updates whenever a new activity row is inserted.
     */
    @Query("""
        SELECT groupPrayerItemId AS itemId, COUNT(*) AS count
        FROM group_prayer_activity
        WHERE groupId = :groupId AND prayedAt >= :sinceMs
        GROUP BY groupPrayerItemId
    """)
    fun observeActivityCountsSince(
        groupId: Long,
        sinceMs: Long
    ): Flow<List<ItemActivityCount>>

    /**
     * Full recent activity rows for a group (for the activity-feed view).
     */
    @Query("""
        SELECT * FROM group_prayer_activity
        WHERE groupId = :groupId AND prayedAt >= :sinceMs
        ORDER BY prayedAt DESC
    """)
    fun observeRecentActivity(
        groupId: Long,
        sinceMs: Long
    ): Flow<List<GroupPrayerActivity>>

    // --- Bulk Delete (Account Deletion) ---
    @Query("DELETE FROM prayer_groups")
    suspend fun deleteAllGroups()

    @Query("DELETE FROM prayer_group_members")
    suspend fun deleteAllMembers()

    @Query("DELETE FROM group_prayer_items")
    suspend fun deleteAllGroupPrayerItems()

    @Query("DELETE FROM group_prayer_item_cross_ref")
    suspend fun deleteAllGroupCrossRefs()

    @Query("DELETE FROM group_prayer_activity")
    suspend fun deleteAllGroupActivity()

    @Query("DELETE FROM group_prayer_activity WHERE userId = :userId")
    suspend fun deleteActivityForUser(userId: String)

    // --- Per-group Cascade Delete (admin "delete group" flow) ---
    // Used when a group creator tears down a group for everyone. Run in
    // the order: activity → cross refs → items → members → group itself,
    // so foreign-key-ish invariants stay coherent even if we ever add
    // proper FK constraints later. (Currently these tables don't enforce
    // FKs at the SQLite level, but the order is still the right mental
    // model and cheap to maintain.)
    @Query("DELETE FROM group_prayer_activity WHERE groupId = :groupId")
    suspend fun deleteActivityForGroup(groupId: Long)

    @Query("DELETE FROM group_prayer_item_cross_ref WHERE groupId = :groupId")
    suspend fun deleteCrossRefsForGroup(groupId: Long)

    @Query("DELETE FROM group_prayer_items WHERE groupId = :groupId")
    suspend fun deleteGroupPrayerItemsForGroup(groupId: Long)

    @Query("DELETE FROM prayer_group_members WHERE groupId = :groupId")
    suspend fun deleteMembersForGroup(groupId: Long)

    @Query("DELETE FROM prayer_groups WHERE id = :groupId")
    suspend fun deleteGroupById(groupId: Long)
}

/**
 * Projection row for [PrayerGroupDao.observeActivityCountsSince].
 * Mapped to a Map<Long, Int> in the repository layer.
 */
data class ItemActivityCount(
    val itemId: Long,
    val count: Int
)
