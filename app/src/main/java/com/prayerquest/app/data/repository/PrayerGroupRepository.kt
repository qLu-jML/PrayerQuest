package com.prayerquest.app.data.repository

import android.util.Log
import com.prayerquest.app.data.dao.PrayerGroupDao
import com.prayerquest.app.data.dao.PrayerItemDao
import com.prayerquest.app.data.entity.GroupPrayerActivity
import com.prayerquest.app.data.entity.GroupPrayerItem
import com.prayerquest.app.data.entity.GroupPrayerItemCrossRef
import com.prayerquest.app.data.entity.PrayerGroup
import com.prayerquest.app.data.entity.PrayerGroupMember
import com.prayerquest.app.data.entity.PrayerItem
import com.prayerquest.app.firebase.AuthState
import com.prayerquest.app.firebase.FirebaseAuthManager
import com.prayerquest.app.firebase.FirestoreGroup
import com.prayerquest.app.firebase.FirestoreGroupMember
import com.prayerquest.app.firebase.FirestoreGroupPrayerItem
import com.prayerquest.app.firebase.FirestoreGroupService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Prayer Groups repository — orchestrates local Room cache + Firestore cloud sync.
 *
 * Architecture:
 * - Room is the local cache and the UI's reactive binding layer
 * - Firestore is the cloud source of truth for shared group data
 * - Creating/joining a group writes to Firestore first, then caches in Room
 * - Real-time: [startCloudMirror] opens snapshot listeners on every group the
 *   signed-in user belongs to and streams changes into Room. The UI layer
 *   keeps observing Room `Flow<>`s — it never touches Firestore directly.
 * - Offline: Firestore's built-in offline cache answers snapshot listeners
 *   from disk while disconnected and replays queued writes when the network
 *   returns. The app has no manual "refresh" surface.
 */
class PrayerGroupRepository(
    private val groupDao: PrayerGroupDao,
    private val prayerItemDao: PrayerItemDao,
    private val authManager: FirebaseAuthManager? = null,
    private val firestoreService: FirestoreGroupService? = null
) {

    private val isCloudEnabled: Boolean
        get() = authManager != null && firestoreService != null && authManager.isSignedIn

    // --- Groups ---
    fun observeAllGroups(): Flow<List<PrayerGroup>> = groupDao.observeAllGroups()
    fun observeGroup(id: Long): Flow<PrayerGroup?> = groupDao.observeGroupById(id)

    suspend fun getGroup(id: Long): PrayerGroup? = groupDao.getGroupById(id)
    suspend fun getGroupByShareCode(code: String): PrayerGroup? = groupDao.getGroupByShareCode(code)
    suspend fun getGroupCount(): Int = groupDao.getGroupCount()

    /**
     * Create a group — writes to Firestore first (if signed in), then caches locally.
     */
    suspend fun createGroup(name: String, description: String = "", emoji: String = ""): Long {
        val shareCode = generateUniqueShareCode()
        val userId = authManager?.currentUserId ?: "local_user"
        val displayName = authManager?.displayName ?: "Me"

        // Write to Firestore if cloud-enabled
        var firestoreId: String? = null
        if (isCloudEnabled) {
            try {
                firestoreId = firestoreService!!.createGroup(
                    name = name,
                    description = description,
                    emoji = emoji,
                    shareCode = shareCode,
                    userId = userId,
                    displayName = displayName
                )
                // Add to user's group index
                firestoreService.addUserGroupRef(userId, firestoreId, name)
                Log.d(TAG, "Group created in Firestore: $firestoreId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create group in Firestore, saving locally only", e)
            }
        }

        // Cache locally in Room
        val group = PrayerGroup(
            name = name,
            description = description,
            emoji = emoji,
            shareCode = shareCode,
            createdBy = userId,
            firestoreId = firestoreId
        )
        val localId = groupDao.insertGroup(group)

        // Creator is automatically an Admin
        groupDao.insertMember(
            PrayerGroupMember(
                groupId = localId,
                userId = userId,
                role = PrayerGroupMember.ROLE_ADMIN
            )
        )
        return localId
    }

    suspend fun updateGroup(group: PrayerGroup) = groupDao.updateGroup(group)
    suspend fun deleteGroup(group: PrayerGroup) {
        // Delete from Firestore if cloud-enabled
        if (isCloudEnabled && group.firestoreId != null) {
            try {
                val userId = authManager!!.currentUserId!!
                firestoreService!!.leaveGroup(group.firestoreId, userId)
                firestoreService.removeUserGroupRef(userId, group.firestoreId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete group from Firestore", e)
            }
        }
        groupDao.deleteGroup(group)
    }

    // --- Membership ---
    fun observeMembers(groupId: Long): Flow<List<PrayerGroupMember>> = groupDao.observeMembers(groupId)
    suspend fun getMemberCount(groupId: Long): Int = groupDao.getMemberCount(groupId)

    /**
     * Join a group by share code.
     * Looks up the code in Firestore first (if signed in), falls back to local.
     */
    suspend fun joinGroup(shareCode: String): Long? {
        val userId = authManager?.currentUserId ?: "local_user"
        val displayName = authManager?.displayName ?: "Me"

        // Try Firestore first if cloud-enabled
        if (isCloudEnabled) {
            try {
                val firestoreGroup = firestoreService!!.findGroupByShareCode(shareCode)
                if (firestoreGroup != null) {
                    // Join in Firestore
                    val joined = firestoreService.joinGroup(
                        firestoreGroup.firestoreId, userId, displayName
                    )
                    if (joined) {
                        // Add to user's group index
                        firestoreService.addUserGroupRef(
                            userId, firestoreGroup.firestoreId, firestoreGroup.name
                        )

                        // Cache the group locally
                        val localGroup = PrayerGroup(
                            name = firestoreGroup.name,
                            description = firestoreGroup.description,
                            emoji = firestoreGroup.emoji,
                            shareCode = firestoreGroup.shareCode,
                            createdBy = firestoreGroup.createdBy,
                            createdAt = firestoreGroup.createdAt,
                            firestoreId = firestoreGroup.firestoreId
                        )
                        val localId = groupDao.insertGroup(localGroup)
                        groupDao.insertMember(
                            PrayerGroupMember(
                                groupId = localId,
                                userId = userId,
                                role = PrayerGroupMember.ROLE_MEMBER
                            )
                        )

                        // No explicit item seeding: once the user is added to the
                        // members subcollection, the `/userGroups` snapshot
                        // listener (owned by [startCloudMirror]) picks up this
                        // group and its nested item + activity listeners hydrate
                        // the local Room cache automatically. The detail screen
                        // may show an empty list for the sub-second window
                        // before the first snapshot arrives — acceptable for MVP.

                        Log.d(TAG, "Joined group via Firestore: ${firestoreGroup.firestoreId}")
                        return localId
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Firestore join failed, trying local", e)
            }
        }

        // Fallback to local-only join
        val group = groupDao.getGroupByShareCode(shareCode) ?: return null
        val memberId = groupDao.insertMember(
            PrayerGroupMember(groupId = group.id, userId = userId, role = PrayerGroupMember.ROLE_MEMBER)
        )
        return if (memberId > 0) group.id else null
    }

    /**
     * Is the current user the creator of this group? Used by the UI to
     * decide whether to offer the destructive "Delete Group" action
     * alongside "Leave Group". A missing auth manager or a null
     * currentUserId falls back to comparing against the local pseudo-user
     * "local_user" — that's what createGroup stamps on [PrayerGroup.createdBy]
     * for offline-only sessions.
     */
    fun isGroupCreator(group: PrayerGroup): Boolean {
        val uid = authManager?.currentUserId ?: "local_user"
        return group.createdBy == uid
    }

    /**
     * Delete a group as its creator — tears down the group for EVERY
     * member, not just the caller. This is a destructive admin action and
     * is the other option (alongside "Leave") offered to group creators.
     *
     * Steps:
     *  1. If signed-in and the group was synced, delete the full cloud
     *     document + all its subcollections via [FirestoreGroupService.deleteEntireGroup].
     *     That cascade also clears the creator's own `/userGroups` ref.
     *     Other members' `/userGroups` refs are cleaned inside the cloud
     *     cascade so their Groups list stops showing the group on next
     *     snapshot.
     *  2. Clear all local Room rows scoped to this groupId.
     *
     * Safe to call on offline-only groups — the cloud step becomes a no-op
     * and local rows are still wiped.
     */
    suspend fun deleteGroupAsCreator(groupId: Long): Result<Unit> {
        val group = groupDao.getGroupById(groupId)
            ?: return Result.failure(IllegalStateException("Group $groupId not found"))

        if (!isGroupCreator(group)) {
            return Result.failure(
                IllegalStateException("Only the creator can delete this group")
            )
        }

        // 1. Cloud cascade (best-effort — a failure still lets the local
        //    delete proceed so the user isn't stuck looking at a group
        //    they can't actually use).
        if (isCloudEnabled && group.firestoreId != null && firestoreService != null) {
            try {
                firestoreService.deleteEntireGroup(group.firestoreId)
            } catch (e: Exception) {
                Log.e(TAG, "Firestore deleteEntireGroup failed for ${group.firestoreId}", e)
            }
        }

        // 2. Local cascade in invariant-preserving order.
        groupDao.deleteActivityForGroup(groupId)
        groupDao.deleteCrossRefsForGroup(groupId)
        groupDao.deleteGroupPrayerItemsForGroup(groupId)
        groupDao.deleteMembersForGroup(groupId)
        groupDao.deleteGroupById(groupId)

        return Result.success(Unit)
    }

    suspend fun leaveGroup(groupId: Long, userId: String? = null) {
        val uid = userId ?: authManager?.currentUserId ?: "local_user"
        val group = groupDao.getGroupById(groupId)

        // Leave in Firestore if cloud-enabled
        if (isCloudEnabled && group?.firestoreId != null) {
            try {
                firestoreService!!.leaveGroup(group.firestoreId, uid)
                firestoreService.removeUserGroupRef(uid, group.firestoreId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to leave group in Firestore", e)
            }
        }

        groupDao.removeMember(groupId, uid)
    }

    // --- Group Prayer Items ---
    fun observeGroupPrayerItems(groupId: Long): Flow<List<PrayerItem>> =
        groupDao.observeGroupPrayerItems(groupId)

    fun observeGroupPrayerItemDetails(groupId: Long): Flow<List<GroupPrayerItem>> =
        groupDao.observeGroupPrayerItemDetails(groupId)

    /**
     * Add a prayer to a group — writes to Firestore + local cache.
     *
     * The Firestore document ID is captured onto the local row so subsequent
     * prayer events on this item (see [markPrayedForGroupItem]) can mirror
     * to cloud, and so incoming remote activity can be matched back to the
     * right local row by [upsertCloudItem] in the snapshot-listener mirror.
     */
    suspend fun addPrayerToGroup(
        groupId: Long,
        prayerItemId: Long,
        title: String = "",
        description: String = ""
    ): Long {
        val group = groupDao.getGroupById(groupId)

        // Write to Firestore if cloud-enabled
        var firestoreItemId: String? = null
        if (isCloudEnabled && group?.firestoreId != null) {
            try {
                val userId = authManager!!.currentUserId!!
                val displayName = authManager.displayName ?: "Me"
                firestoreItemId = firestoreService!!.addPrayerItem(
                    groupId = group.firestoreId,
                    title = title,
                    description = description,
                    userId = userId,
                    displayName = displayName
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add prayer to Firestore", e)
            }
        }

        // Save locally — firestoreId persists the cloud/local linkage so the
        // sync path can find this row when it pulls the same item back.
        groupDao.insertGroupCrossRef(GroupPrayerItemCrossRef(groupId, prayerItemId))
        return groupDao.insertGroupPrayerItem(
            GroupPrayerItem(
                groupId = groupId,
                prayerItemId = prayerItemId,
                title = title,
                description = description,
                addedBy = authManager?.currentUserId ?: "local_user",
                firestoreId = firestoreItemId
            )
        )
    }

    /**
     * Record a prayer event for a group item.
     *
     * Writes three things:
     *  1. A timestamped row in `group_prayer_activity` — the source of truth for
     *     the "prayed this week" chip and the activity feed.
     *  2. An increment of the lifetime aggregate `prayedByCount` on the item,
     *     for existing "Prayed by N members" UI.
     *  3. A mirror activity doc in Firestore when the user is signed in and the
     *     group/item have Firestore IDs, so other members see the activity on
     *     their next refresh.
     *
     * Firestore failures don't block the local write — the chip updates
     * regardless. A retry happens implicitly on the next hot write; broken
     * mirrors also self-heal when the snapshot listener reconnects and
     * pulls the activity subcollection's current state into Room.
     */
    suspend fun markPrayedForGroupItem(groupPrayerItemId: Long) {
        val userId = authManager?.currentUserId ?: "local_user"
        val displayName = authManager?.displayName ?: "Me"

        val item = groupDao.getGroupPrayerItemById(groupPrayerItemId) ?: return

        // Generate a stable activity ID up front so the local row and the
        // Firestore doc share the same identity. That shared ID is what
        // makes sync idempotent — if this activity is pulled back later,
        // the unique index on firestoreId turns the remote insert into a
        // no-op.
        //
        // For offline or non-cloud users we still generate an ID; it just
        // never makes it to the server. The local unique index is happy
        // with any stable string.
        val activityId = firestoreService?.newActivityId()
            ?: java.util.UUID.randomUUID().toString()

        // 1 + 2: local writes — always happen, offline-first.
        groupDao.insertActivity(
            GroupPrayerActivity(
                groupId = item.groupId,
                groupPrayerItemId = groupPrayerItemId,
                userId = userId,
                displayName = displayName,
                firestoreId = activityId
            )
        )
        groupDao.incrementPrayedByCount(groupPrayerItemId)

        // 3: Firestore mirror when cloud-enabled and the item is synced.
        // recordPrayerActivity runs a transaction that is idempotent on the
        // activityId, so retrying after a dropped network response does not
        // double-count.
        if (isCloudEnabled && item.firestoreId != null) {
            val group = groupDao.getGroupById(item.groupId)
            if (group?.firestoreId != null) {
                try {
                    firestoreService!!.recordPrayerActivity(
                        groupId = group.firestoreId,
                        itemId = item.firestoreId,
                        activityId = activityId,
                        userId = userId,
                        displayName = displayName
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to record prayer activity in Firestore", e)
                }
            }
        }
    }

    // --- Activity Queries ---

    /**
     * Reactive map of groupPrayerItemId → number of prayers in the last week.
     *
     * Note: the window start is captured when this Flow is collected, so
     * the UI will see stable results while the screen is open. Re-collecting
     * on screen re-entry is what advances the window; new activity that
     * arrives via the snapshot-listener mirror updates the count live
     * thanks to Room's invalidation tracker.
     */
    fun observeWeeklyCountsForGroup(groupId: Long): Flow<Map<Long, Int>> {
        val weekAgo = System.currentTimeMillis() - WEEK_MS
        return groupDao.observeActivityCountsSince(groupId, weekAgo)
            .map { rows -> rows.associate { it.itemId to it.count } }
    }

    /**
     * Full recent-activity feed for a group (last week). Drives the optional
     * "who prayed when" view mentioned in DD §3.6.
     */
    fun observeRecentActivity(groupId: Long): Flow<List<GroupPrayerActivity>> {
        val weekAgo = System.currentTimeMillis() - WEEK_MS
        return groupDao.observeRecentActivity(groupId, weekAgo)
    }

    // --- Manual refresh (one-shot pull) ---

    /**
     * Fire a single pull from Firestore into Room, bypassing the snapshot
     * listener cadence. Used by the Groups screen's pull-to-refresh /
     * refresh button as an escape hatch when the user thinks the list is
     * stale (e.g. they joined a group on another device moments ago and
     * don't want to wait for the real-time listener to reconnect).
     *
     * Safe to call at any time. No-ops when the user is signed out or
     * Firestore isn't available.
     */
    suspend fun refreshFromCloud(): Result<Int> {
        if (!isCloudEnabled || firestoreService == null || authManager == null) {
            return Result.success(0)
        }
        val userId = authManager.currentUserId ?: return Result.success(0)
        return try {
            // One-shot take from the cold side of the callbackFlow — we
            // grab the first emission, upsert it, and return. The ongoing
            // snapshot listener on [startCloudMirror] continues to handle
            // live updates after this function returns.
            val pulled = firestoreService.observeUserGroups(userId).first()
            for (cloudGroup in pulled) {
                val localId = upsertCloudGroup(cloudGroup, userId)
                // Also pull items + members for each group so detail screens
                // aren't blank right after a manual refresh.
                val cloudItems = firestoreService
                    .observeGroupPrayerItems(cloudGroup.firestoreId)
                    .first()
                for (item in cloudItems) {
                    upsertCloudItem(item, localId)
                }
            }
            Result.success(pulled.size)
        } catch (e: Exception) {
            Log.e(TAG, "Manual refresh failed", e)
            Result.failure(e)
        }
    }

    // --- Cloud Mirror (Real-Time Snapshot Listeners) ---

    /**
     * Start mirroring Firestore → Room for the lifetime of [scope].
     *
     * Observes [FirebaseAuthManager.authState]: while the user is signed in,
     * opens a snapshot listener on the user's `/userGroups/{uid}/groups`
     * index and, for every group in that index, opens nested listeners on
     * `/groups/{groupId}/prayerItems`, `/groups/{groupId}/members`, and
     * `/groups/{groupId}/prayerItems/{itemId}/activity`. Every remote change
     * is upserted into Room so the UI's existing `Flow<>`s just keep ticking.
     *
     * Listener lifecycle is entirely scope-driven — cancelling [scope]
     * detaches every listener (via the `awaitClose { listener.remove() }`
     * inside each `callbackFlow`). On a sign-out transition, the internal
     * `collectLatest` on `authState` cancels the previous SignedIn block,
     * which cascades to every child listener.
     *
     * Safe to call from multiple ViewModels concurrently — Firestore
     * multiplexes duplicate snapshot listeners onto the same underlying
     * query, so the network cost does not scale with the number of
     * subscribers.
     *
     * Returns the top-level [Job] for tests / callers that want to await
     * cancellation; in normal use the caller just ties this to
     * `viewModelScope` and forgets about it.
     */
    fun startCloudMirror(scope: CoroutineScope): Job = scope.launch {
        if (firestoreService == null || authManager == null) return@launch
        authManager.authState.collectLatest { state ->
            if (state is AuthState.SignedIn) {
                mirrorAllFromCloud(state.user.uid)
            }
            // On SignedOut, collectLatest cancels the previous mirror block
            // and this branch is a no-op — listeners stay down until the
            // next SignedIn emission.
        }
    }

    /**
     * Inner mirror loop — scoped to a single signed-in user. Collects the
     * user's `/userGroups` snapshot feed and, on every emission, upserts the
     * groups and (re)launches per-group listeners inside a nested
     * `coroutineScope`. The nested scope guarantees that when the user's
     * group list changes (a group added, removed, or renamed), every stale
     * per-group listener is cancelled before the new ones attach.
     */
    private suspend fun mirrorAllFromCloud(userId: String) {
        if (firestoreService == null) return
        firestoreService.observeUserGroups(userId).collectLatest { cloudGroups ->
            coroutineScope {
                for (cloudGroup in cloudGroups) {
                    val localId = upsertCloudGroup(cloudGroup, userId)
                    launch { mirrorGroupItems(cloudGroup.firestoreId, localId) }
                    launch { mirrorGroupMembers(cloudGroup.firestoreId, localId) }
                }
                // coroutineScope suspends here until either every listener
                // completes (they don't — snapshot listeners run forever) or
                // the enclosing collectLatest cancels us because the
                // userGroups list changed. Cancellation cascades into each
                // launched listener, detaching it cleanly.
            }
        }
    }

    /**
     * Upsert a group received from the cloud into Room. Matches on
     * `shareCode` because a group's Firestore id becomes its local
     * `firestoreId` but the device's autogenerated `id` is what the UI
     * holds onto. Also ensures the current user has a local membership row
     * for groups that arrive via the listener (e.g. joined on another
     * device) so the Compose `observeMembers` Flow fills in without a
     * separate refresh.
     */
    private suspend fun upsertCloudGroup(cloudGroup: FirestoreGroup, userId: String): Long {
        val existing = groupDao.getGroupByShareCode(cloudGroup.shareCode)
        return if (existing != null) {
            groupDao.updateGroup(
                existing.copy(
                    name = cloudGroup.name,
                    description = cloudGroup.description,
                    emoji = cloudGroup.emoji,
                    firestoreId = cloudGroup.firestoreId
                )
            )
            existing.id
        } else {
            val newGroup = PrayerGroup(
                name = cloudGroup.name,
                description = cloudGroup.description,
                emoji = cloudGroup.emoji,
                shareCode = cloudGroup.shareCode,
                createdBy = cloudGroup.createdBy,
                createdAt = cloudGroup.createdAt,
                firestoreId = cloudGroup.firestoreId
            )
            val id = groupDao.insertGroup(newGroup)
            groupDao.insertMember(
                PrayerGroupMember(
                    groupId = id,
                    userId = userId,
                    role = PrayerGroupMember.ROLE_MEMBER
                )
            )
            id
        }
    }

    /**
     * Listen to a single group's prayer items. On every emission, upserts
     * each item into Room and (re)launches activity listeners for each
     * item. Uses `collectLatest` so when the item list changes (additions,
     * removals, edits) stale activity listeners are torn down.
     */
    private suspend fun mirrorGroupItems(firestoreGroupId: String, localGroupId: Long) {
        if (firestoreService == null) return
        firestoreService.observeGroupPrayerItems(firestoreGroupId).collectLatest { cloudItems ->
            val upserted = cloudItems.map { cloudItem ->
                val localItemId = upsertCloudItem(cloudItem, localGroupId)
                cloudItem.firestoreId to localItemId
            }
            coroutineScope {
                for ((firestoreItemId, localItemId) in upserted) {
                    launch {
                        mirrorItemActivity(
                            firestoreGroupId = firestoreGroupId,
                            firestoreItemId = firestoreItemId,
                            localGroupId = localGroupId,
                            localItemId = localItemId
                        )
                    }
                }
            }
        }
    }

    /**
     * Upsert a remote group prayer item into Room, creating a shadow
     * [PrayerItem] row the first time so the existing join-based Flow
     * (`observeGroupPrayerItems`) surfaces it. Uses `firestoreId` as the
     * merge key — the cloud-side id is the stable anchor.
     */
    private suspend fun upsertCloudItem(
        cloudItem: FirestoreGroupPrayerItem,
        localGroupId: Long
    ): Long {
        val localItems = groupDao.getGroupPrayerItemsForGroup(localGroupId)
        val existing = localItems.firstOrNull { it.firestoreId == cloudItem.firestoreId }
        return if (existing != null) {
            groupDao.updateGroupPrayerItem(
                existing.copy(
                    title = cloudItem.title.ifBlank { existing.title },
                    description = cloudItem.description.ifBlank { existing.description },
                    prayedByCount = cloudItem.prayedByCount
                )
            )
            existing.id
        } else {
            val shadowPrayerItemId = prayerItemDao.insert(
                PrayerItem(
                    title = cloudItem.title,
                    description = cloudItem.description,
                    createdAt = cloudItem.addedAt.takeIf { it > 0 }
                        ?: System.currentTimeMillis(),
                    isUserCreated = false
                )
            )
            groupDao.insertGroupCrossRef(
                GroupPrayerItemCrossRef(localGroupId, shadowPrayerItemId)
            )
            groupDao.insertGroupPrayerItem(
                GroupPrayerItem(
                    groupId = localGroupId,
                    prayerItemId = shadowPrayerItemId,
                    title = cloudItem.title,
                    description = cloudItem.description,
                    addedBy = cloudItem.addedBy,
                    addedAt = cloudItem.addedAt.takeIf { it > 0 }
                        ?: System.currentTimeMillis(),
                    prayedByCount = cloudItem.prayedByCount,
                    firestoreId = cloudItem.firestoreId
                )
            )
        }
    }

    /**
     * Listen to the last week of activity on a single group prayer item and
     * mirror every event into Room. The bulk insert is IGNORE on duplicate
     * `firestoreId`, so events this device originated (already inserted by
     * [markPrayedForGroupItem]) don't double-up when Firestore echoes them
     * back.
     */
    private suspend fun mirrorItemActivity(
        firestoreGroupId: String,
        firestoreItemId: String,
        localGroupId: Long,
        localItemId: Long
    ) {
        if (firestoreService == null) return
        val sinceMs = System.currentTimeMillis() - WEEK_MS
        firestoreService.observeGroupPrayerItemActivity(
            groupId = firestoreGroupId,
            itemId = firestoreItemId,
            sinceMs = sinceMs
        ).collect { activities ->
            if (activities.isNotEmpty()) {
                groupDao.insertRemoteActivities(
                    activities.map { remote ->
                        GroupPrayerActivity(
                            groupId = localGroupId,
                            groupPrayerItemId = localItemId,
                            userId = remote.userId,
                            displayName = remote.displayName,
                            prayedAt = remote.prayedAt,
                            firestoreId = remote.firestoreId
                        )
                    }
                )
            }
        }
    }

    /**
     * Mirror the group's members subcollection into Room. Keeps the
     * `prayer_group_members` table converged with Firestore — inserts new
     * members (IGNORE on duplicate userId within a group), leaves existing
     * ones untouched. Member removal on other devices is a v1.1 concern;
     * for MVP a leftover local row just displays a stale name until the
     * user reinstalls.
     */
    private suspend fun mirrorGroupMembers(firestoreGroupId: String, localGroupId: Long) {
        if (firestoreService == null) return
        firestoreService.observeGroupMembers(firestoreGroupId).collect { cloudMembers ->
            for (cloudMember in cloudMembers) {
                groupDao.insertMember(
                    PrayerGroupMember(
                        groupId = localGroupId,
                        userId = cloudMember.userId,
                        role = cloudMember.role,
                        joinedAt = cloudMember.joinedAt
                    )
                )
            }
        }
    }

    // --- Firestore-aware Members ---

    /**
     * Observe members from Firestore for a cloud-synced group.
     */
    fun observeCloudMembers(groupId: Long): Flow<List<FirestoreGroupMember>>? {
        if (!isCloudEnabled || firestoreService == null) return null

        // We'd need the firestoreId — observe from local and map
        return null // Simplified for MVP, use local members
    }

    // --- Account Deletion ---

    /**
     * Delete all user data from Firestore and local Room, then delete the Firebase Auth account.
     * This is the full account deletion flow triggered from Settings.
     *
     * Order of operations:
     * 1. Delete all Firestore data (groups, memberships, prayer items, user index)
     * 2. Clear all local Room group data
     * 3. Delete Firebase Auth account (must be last — can't undo this)
     */
    suspend fun deleteAccount(): Result<Unit> {
        val userId = authManager?.currentUserId
            ?: return Result.failure(Exception("No user signed in"))

        return try {
            // Step 1: Clean up all Firestore data
            if (firestoreService != null) {
                firestoreService.deleteAllUserData(userId)
            }

            // Step 2: Clear local group data from Room
            groupDao.deleteAllGroups()
            groupDao.deleteAllMembers()
            groupDao.deleteAllGroupPrayerItems()
            groupDao.deleteAllGroupCrossRefs()
            groupDao.deleteAllGroupActivity()

            // Step 3: Delete Firebase Auth account
            val authResult = authManager.deleteAccount()
            if (authResult.isFailure) {
                return Result.failure(authResult.exceptionOrNull()
                    ?: Exception("Failed to delete auth account"))
            }

            Log.d(TAG, "Account fully deleted for user $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Account deletion failed", e)
            Result.failure(e)
        }
    }

    // --- Share Code Generation ---
    private suspend fun generateUniqueShareCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // no I/O/0/1 to avoid confusion
        var attempts = 0
        while (attempts < 10) {
            val code = (1..6).map { chars.random() }.joinToString("")
            val shareCode = "PRAY-$code"

            // Check Firestore for uniqueness if cloud-enabled
            if (isCloudEnabled) {
                try {
                    if (!firestoreService!!.isShareCodeTaken(shareCode)) {
                        return shareCode
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to check share code uniqueness", e)
                    return shareCode // Use it anyway, collision is very unlikely
                }
            } else {
                // Local-only: check local DB
                if (groupDao.getGroupByShareCode(shareCode) == null) {
                    return shareCode
                }
            }
            attempts++
        }
        // Fallback — extremely unlikely to reach here
        val code = (1..8).map { chars.random() }.joinToString("")
        return "PRAY-$code"
    }

    companion object {
        private const val TAG = "PrayerGroupRepo"
        private const val WEEK_MS = 7L * 24 * 60 * 60 * 1000
    }
}
