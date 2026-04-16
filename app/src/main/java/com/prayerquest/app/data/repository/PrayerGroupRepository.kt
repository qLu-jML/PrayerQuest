package com.prayerquest.app.data.repository

import com.prayerquest.app.data.dao.PrayerGroupDao
import com.prayerquest.app.data.entity.GroupPrayerItem
import com.prayerquest.app.data.entity.GroupPrayerItemCrossRef
import com.prayerquest.app.data.entity.PrayerGroup
import com.prayerquest.app.data.entity.PrayerGroupMember
import com.prayerquest.app.data.entity.PrayerItem
import kotlinx.coroutines.flow.Flow

/**
 * Prayer Groups CRUD, invite code generation/validation, membership, and group prayer items.
 * MVP: fully offline with manual refresh. Real-time sync is POST-MVP.
 */
class PrayerGroupRepository(
    private val groupDao: PrayerGroupDao
) {

    // --- Groups ---
    fun observeAllGroups(): Flow<List<PrayerGroup>> = groupDao.observeAllGroups()
    fun observeGroup(id: Long): Flow<PrayerGroup?> = groupDao.observeGroupById(id)

    suspend fun getGroup(id: Long): PrayerGroup? = groupDao.getGroupById(id)
    suspend fun getGroupByShareCode(code: String): PrayerGroup? = groupDao.getGroupByShareCode(code)
    suspend fun getGroupCount(): Int = groupDao.getGroupCount()

    suspend fun createGroup(name: String, description: String = "", emoji: String = ""): Long {
        val shareCode = generateShareCode()
        val group = PrayerGroup(
            name = name,
            description = description,
            emoji = emoji,
            shareCode = shareCode
        )
        val groupId = groupDao.insertGroup(group)
        // Creator is automatically an Admin
        groupDao.insertMember(
            PrayerGroupMember(
                groupId = groupId,
                role = PrayerGroupMember.ROLE_ADMIN
            )
        )
        return groupId
    }

    suspend fun updateGroup(group: PrayerGroup) = groupDao.updateGroup(group)
    suspend fun deleteGroup(group: PrayerGroup) = groupDao.deleteGroup(group)

    // --- Membership ---
    fun observeMembers(groupId: Long): Flow<List<PrayerGroupMember>> = groupDao.observeMembers(groupId)
    suspend fun getMemberCount(groupId: Long): Int = groupDao.getMemberCount(groupId)

    suspend fun joinGroup(shareCode: String): Long? {
        val group = groupDao.getGroupByShareCode(shareCode) ?: return null
        return groupDao.insertMember(
            PrayerGroupMember(groupId = group.id, role = PrayerGroupMember.ROLE_MEMBER)
        )
    }

    suspend fun leaveGroup(groupId: Long, userId: String = "local_user") {
        groupDao.removeMember(groupId, userId)
    }

    // --- Group Prayer Items ---
    fun observeGroupPrayerItems(groupId: Long): Flow<List<PrayerItem>> =
        groupDao.observeGroupPrayerItems(groupId)

    fun observeGroupPrayerItemDetails(groupId: Long): Flow<List<GroupPrayerItem>> =
        groupDao.observeGroupPrayerItemDetails(groupId)

    suspend fun addPrayerToGroup(groupId: Long, prayerItemId: Long): Long {
        groupDao.insertGroupCrossRef(GroupPrayerItemCrossRef(groupId, prayerItemId))
        return groupDao.insertGroupPrayerItem(
            GroupPrayerItem(groupId = groupId, prayerItemId = prayerItemId)
        )
    }

    suspend fun markPrayedForGroupItem(groupPrayerItemId: Long) {
        groupDao.incrementPrayedByCount(groupPrayerItemId)
    }

    // --- Share Code Generation ---
    private fun generateShareCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // no I/O/0/1 to avoid confusion
        val code = (1..6).map { chars.random() }.joinToString("")
        return "PRAY-$code"
    }
}
