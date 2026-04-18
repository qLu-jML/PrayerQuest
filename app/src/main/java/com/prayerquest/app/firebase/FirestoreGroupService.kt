package com.prayerquest.app.firebase

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firestore service for Prayer Groups cloud sync.
 *
 * Firestore data model:
 *   /groups/{groupId}
 *     - name, description, emoji, shareCode, createdBy, createdAt
 *   /groups/{groupId}/members/{userId}
 *     - displayName, role, joinedAt
 *   /groups/{groupId}/prayerItems/{itemId}
 *     - title, description, addedBy, addedByName, addedAt, prayedByCount, prayedByUsers, status
 *   /groups/{groupId}/prayerItems/{itemId}/activity/{activityId}
 *     - userId, userName, prayedAt
 *
 * Offline behavior:
 *   Firestore's Android SDK enables local disk persistence by default — queries
 *   served from the persistent cache when offline, and writes are queued and
 *   replayed automatically on reconnect. We rely on that default rather than
 *   calling `FirebaseFirestore.setFirestoreSettings(...)` to avoid init-ordering
 *   hazards (settings must be applied before any other instance access).
 *
 *   Our snapshot listeners (observeUserGroups, observeGroupPrayerItems, etc.)
 *   fire immediately from cache on attach, then again from the server once the
 *   connection resolves — giving the UI an offline-first feel for free.
 */
class FirestoreGroupService {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // ─── Group CRUD ──────────────────────────────────────────────────────

    /**
     * Create a new group in Firestore. Returns the Firestore document ID.
     */
    suspend fun createGroup(
        name: String,
        description: String,
        emoji: String,
        shareCode: String,
        userId: String,
        displayName: String
    ): String {
        val groupData = hashMapOf(
            "name" to name,
            "description" to description,
            "emoji" to emoji,
            "shareCode" to shareCode,
            "createdBy" to userId,
            "createdAt" to Timestamp.now(),
            "memberCount" to 1
        )

        val docRef = db.collection(GROUPS_COLLECTION).add(groupData).await()
        val groupId = docRef.id

        // Add creator as Admin member
        val memberData = hashMapOf(
            "displayName" to displayName,
            "role" to "Admin",
            "joinedAt" to Timestamp.now()
        )
        db.collection(GROUPS_COLLECTION)
            .document(groupId)
            .collection(MEMBERS_SUBCOLLECTION)
            .document(userId)
            .set(memberData)
            .await()

        Log.d(TAG, "Created group $groupId with code $shareCode")
        return groupId
    }

    /**
     * Find a group by its share code. Returns null if not found.
     */
    suspend fun findGroupByShareCode(shareCode: String): FirestoreGroup? {
        val snapshot = db.collection(GROUPS_COLLECTION)
            .whereEqualTo("shareCode", shareCode)
            .limit(1)
            .get()
            .await()

        return if (snapshot.documents.isNotEmpty()) {
            val doc = snapshot.documents[0]
            FirestoreGroup(
                firestoreId = doc.id,
                name = doc.getString("name") ?: "",
                description = doc.getString("description") ?: "",
                emoji = doc.getString("emoji") ?: "",
                shareCode = doc.getString("shareCode") ?: "",
                createdBy = doc.getString("createdBy") ?: "",
                createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                memberCount = doc.getLong("memberCount")?.toInt() ?: 0
            )
        } else null
    }

    /**
     * Join a group — adds the user to the members subcollection.
     */
    suspend fun joinGroup(groupId: String, userId: String, displayName: String): Boolean {
        return try {
            val memberData = hashMapOf(
                "displayName" to displayName,
                "role" to "Member",
                "joinedAt" to Timestamp.now()
            )

            db.collection(GROUPS_COLLECTION)
                .document(groupId)
                .collection(MEMBERS_SUBCOLLECTION)
                .document(userId)
                .set(memberData)
                .await()

            // Increment member count
            db.collection(GROUPS_COLLECTION)
                .document(groupId)
                .update("memberCount", FieldValue.increment(1))
                .await()

            Log.d(TAG, "User $userId joined group $groupId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to join group", e)
            false
        }
    }

    /**
     * Leave a group — removes the user from the members subcollection.
     */
    suspend fun leaveGroup(groupId: String, userId: String) {
        try {
            db.collection(GROUPS_COLLECTION)
                .document(groupId)
                .collection(MEMBERS_SUBCOLLECTION)
                .document(userId)
                .delete()
                .await()

            db.collection(GROUPS_COLLECTION)
                .document(groupId)
                .update("memberCount", FieldValue.increment(-1))
                .await()

            Log.d(TAG, "User $userId left group $groupId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to leave group", e)
        }
    }

    // ─── Group Prayer Items ──────────────────────────────────────────────

    /**
     * Add a prayer item to a group.
     */
    suspend fun addPrayerItem(
        groupId: String,
        title: String,
        description: String,
        userId: String,
        displayName: String
    ): String {
        val itemData = hashMapOf(
            "title" to title,
            "description" to description,
            "addedBy" to userId,
            "addedByName" to displayName,
            "addedAt" to Timestamp.now(),
            "prayedByCount" to 0,
            "prayedByUsers" to listOf<String>(),
            "status" to "Active"
        )

        val docRef = db.collection(GROUPS_COLLECTION)
            .document(groupId)
            .collection(PRAYER_ITEMS_SUBCOLLECTION)
            .add(itemData)
            .await()

        Log.d(TAG, "Added prayer item ${docRef.id} to group $groupId")
        return docRef.id
    }

    /**
     * Mark that a user prayed for a group prayer item.
     *
     * Legacy aggregate-only write. Prefer [recordPrayerActivity], which also
     * writes a timestamped entry to the activity subcollection so the UI can
     * show "X prayed this week" and drive the activity feed.
     */
    suspend fun markPrayed(groupId: String, itemId: String, userId: String) {
        val itemRef = db.collection(GROUPS_COLLECTION)
            .document(groupId)
            .collection(PRAYER_ITEMS_SUBCOLLECTION)
            .document(itemId)

        itemRef.update(
            "prayedByCount", FieldValue.increment(1),
            "prayedByUsers", FieldValue.arrayUnion(userId)
        ).await()
    }

    /**
     * Generate a fresh activity document ID without a network round-trip.
     * Firestore's client SDK allocates collision-safe IDs locally, so we can
     * stamp the same ID onto the local Room row *and* the remote Firestore
     * doc. That shared ID is the backbone of idempotent sync — if this
     * device's activity is later pulled back during refresh, the unique
     * index on [GroupPrayerActivity.firestoreId] skips the duplicate insert.
     */
    fun newActivityId(): String =
        db.collection(GROUPS_COLLECTION).document().id

    /**
     * Record a single prayer event for a group prayer item.
     *
     * Uses a caller-supplied [activityId] (see [newActivityId]) and writes via
     * `.set(...)` — not `.add(...)` — so retries from a flaky network do not
     * produce duplicate activity docs. The caller is expected to have already
     * written the local row with the same ID; this method makes the cloud
     * side converge.
     *
     * Also keeps the parent item's aggregate counters fresh (so existing
     * "Prayed by N members" UI and `lastPrayedAt` queries still work without
     * a migration). `FieldValue.increment` combined with idempotent activity
     * writes means a retry cannot inflate the count past the true number of
     * distinct activity docs — the activity write is a no-op on retry, which
     * means the whole batch is a no-op on retry.
     */
    suspend fun recordPrayerActivity(
        groupId: String,
        itemId: String,
        activityId: String,
        userId: String,
        displayName: String
    ) {
        val itemRef = db.collection(GROUPS_COLLECTION)
            .document(groupId)
            .collection(PRAYER_ITEMS_SUBCOLLECTION)
            .document(itemId)
        val activityRef = itemRef.collection(ACTIVITY_SUBCOLLECTION).document(activityId)

        // Transaction gives us atomic "check-then-write" so a retry after a
        // dropped response cannot inflate the aggregate counter. If the
        // activity doc already exists, the transaction is a no-op; otherwise
        // both the activity write and the aggregate update commit together.
        db.runTransaction { transaction ->
            val existing = transaction.get(activityRef)
            if (existing.exists()) {
                return@runTransaction
            }
            transaction.set(
                activityRef,
                hashMapOf(
                    "userId" to userId,
                    "displayName" to displayName,
                    "prayedAt" to Timestamp.now()
                )
            )
            // arrayUnion keeps prayedByUsers de-duplicated; increment is safe
            // because the activity write and the counter write are now in a
            // single atomic transaction.
            transaction.update(
                itemRef,
                mapOf(
                    "prayedByCount" to FieldValue.increment(1),
                    "prayedByUsers" to FieldValue.arrayUnion(userId),
                    "lastPrayedAt" to Timestamp.now()
                )
            )
        }.await()

        Log.d(TAG, "Recorded activity $activityId for item $itemId by $userId")
    }

    /**
     * Observe recent prayer activity for a single item as a Flow of snapshots.
     *
     * Emits the full window of activity on first attach (so callers don't need
     * a separate "catch up" call) and then re-emits whenever another member's
     * prayer event lands in Firestore. Backed by Firestore's local cache, so
     * it also emits offline — the cache returns the last-seen window while
     * queued writes replay in the background.
     *
     * The caller is responsible for lifecycle — cancelling the collecting
     * coroutine detaches the underlying snapshot listener.
     */
    fun observeGroupPrayerItemActivity(
        groupId: String,
        itemId: String,
        sinceMs: Long
    ): Flow<List<FirestoreGroupPrayerActivity>> = callbackFlow {
        val listener = db.collection(GROUPS_COLLECTION)
            .document(groupId)
            .collection(PRAYER_ITEMS_SUBCOLLECTION)
            .document(itemId)
            .collection(ACTIVITY_SUBCOLLECTION)
            .whereGreaterThanOrEqualTo("prayedAt", Timestamp(sinceMs / 1000, 0))
            .orderBy("prayedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing item activity", error)
                    return@addSnapshotListener
                }
                val events = snapshot?.documents?.map { doc ->
                    FirestoreGroupPrayerActivity(
                        firestoreId = doc.id,
                        userId = doc.getString("userId") ?: "",
                        displayName = doc.getString("displayName") ?: "",
                        prayedAt = doc.getTimestamp("prayedAt")?.toDate()?.time ?: 0L
                    )
                } ?: emptyList()
                trySend(events)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Update prayer item status (Active → Answered, etc.)
     */
    suspend fun updatePrayerItemStatus(groupId: String, itemId: String, status: String) {
        db.collection(GROUPS_COLLECTION)
            .document(groupId)
            .collection(PRAYER_ITEMS_SUBCOLLECTION)
            .document(itemId)
            .update("status", status)
            .await()
    }

    // ─── Observers (Snapshot Listeners as Flows) ─────────────────────────

    /**
     * Observe all groups the user belongs to.
     * Uses a collection group query on the members subcollection.
     */
    fun observeUserGroups(userId: String): Flow<List<FirestoreGroup>> = callbackFlow {
        // Listen to groups where user is a member by querying the user's
        // membership mirror documents under /userGroups/{uid}/groups/{groupId}.
        // (A collectionGroup query on "members" with FieldPath.documentId()
        // would require a full document path, not just the UID, so we keep
        // a per-user membership collection instead.)
        val userGroupsRef = db.collection(USER_GROUPS_COLLECTION)
            .document(userId)
            .collection("groups")

        val listener = userGroupsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error observing user groups", error)
                return@addSnapshotListener
            }

            val groupIds = snapshot?.documents?.mapNotNull { it.id } ?: emptyList()
            // Fetch full group data for each ID
            if (groupIds.isEmpty()) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            // Fetch groups by IDs (Firestore 'in' query supports up to 30)
            db.collection(GROUPS_COLLECTION)
                .whereIn("__name__", groupIds.take(30).map {
                    db.collection(GROUPS_COLLECTION).document(it)
                })
                .get()
                .addOnSuccessListener { groupSnapshot ->
                    val groups = groupSnapshot.documents.mapNotNull { doc ->
                        FirestoreGroup(
                            firestoreId = doc.id,
                            name = doc.getString("name") ?: "",
                            description = doc.getString("description") ?: "",
                            emoji = doc.getString("emoji") ?: "",
                            shareCode = doc.getString("shareCode") ?: "",
                            createdBy = doc.getString("createdBy") ?: "",
                            createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                            memberCount = doc.getLong("memberCount")?.toInt() ?: 0
                        )
                    }
                    trySend(groups)
                }
        }

        awaitClose { listener.remove() }
    }

    /**
     * Observe prayer items for a specific group.
     */
    fun observeGroupPrayerItems(groupId: String): Flow<List<FirestoreGroupPrayerItem>> = callbackFlow {
        val listener = db.collection(GROUPS_COLLECTION)
            .document(groupId)
            .collection(PRAYER_ITEMS_SUBCOLLECTION)
            .orderBy("addedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing group prayer items", error)
                    return@addSnapshotListener
                }

                val items = snapshot?.documents?.mapNotNull { doc ->
                    FirestoreGroupPrayerItem(
                        firestoreId = doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        addedBy = doc.getString("addedBy") ?: "",
                        addedByName = doc.getString("addedByName") ?: "",
                        addedAt = doc.getTimestamp("addedAt")?.toDate()?.time ?: 0L,
                        prayedByCount = doc.getLong("prayedByCount")?.toInt() ?: 0,
                        prayedByUsers = (doc.get("prayedByUsers") as? List<*>)
                            ?.filterIsInstance<String>() ?: emptyList(),
                        status = doc.getString("status") ?: "Active"
                    )
                } ?: emptyList()

                trySend(items)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Observe members of a group.
     */
    fun observeGroupMembers(groupId: String): Flow<List<FirestoreGroupMember>> = callbackFlow {
        val listener = db.collection(GROUPS_COLLECTION)
            .document(groupId)
            .collection(MEMBERS_SUBCOLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing group members", error)
                    return@addSnapshotListener
                }

                val members = snapshot?.documents?.mapNotNull { doc ->
                    FirestoreGroupMember(
                        userId = doc.id,
                        displayName = doc.getString("displayName") ?: "",
                        role = doc.getString("role") ?: "Member",
                        joinedAt = doc.getTimestamp("joinedAt")?.toDate()?.time ?: 0L
                    )
                } ?: emptyList()

                trySend(members)
            }

        awaitClose { listener.remove() }
    }

    // ─── User Group Index ────────────────────────────────────────────────

    /**
     * Add a group reference to the user's group index.
     * This enables efficient "get all my groups" queries.
     */
    suspend fun addUserGroupRef(userId: String, groupId: String, groupName: String) {
        val refData = hashMapOf(
            "groupName" to groupName,
            "joinedAt" to Timestamp.now()
        )
        db.collection(USER_GROUPS_COLLECTION)
            .document(userId)
            .collection("groups")
            .document(groupId)
            .set(refData)
            .await()
    }

    /**
     * Remove a group reference from the user's group index.
     */
    suspend fun removeUserGroupRef(userId: String, groupId: String) {
        db.collection(USER_GROUPS_COLLECTION)
            .document(userId)
            .collection("groups")
            .document(groupId)
            .delete()
            .await()
    }

    // ─── Account Deletion ────────────────────────────────────────────────

    /**
     * Delete ALL Firestore data associated with a user.
     * Called during account deletion — removes memberships, prayer items added by user,
     * user group index, and groups where user is the sole admin.
     */
    suspend fun deleteAllUserData(userId: String) {
        try {
            // 1. Get all groups the user belongs to
            val userGroupRefs = db.collection(USER_GROUPS_COLLECTION)
                .document(userId)
                .collection("groups")
                .get()
                .await()

            val groupIds = userGroupRefs.documents.map { it.id }

            for (groupId in groupIds) {
                val groupRef = db.collection(GROUPS_COLLECTION).document(groupId)

                // Check if user is the sole admin
                val members = groupRef.collection(MEMBERS_SUBCOLLECTION).get().await()
                val admins = members.documents.filter { it.getString("role") == "Admin" }
                val isSoleAdmin = admins.size == 1 && admins[0].id == userId

                if (isSoleAdmin && members.size() == 1) {
                    // User is the only member — delete the entire group
                    deleteEntireGroup(groupId)
                } else if (isSoleAdmin) {
                    // Promote the longest-tenured member to Admin before leaving
                    val nextAdmin = members.documents
                        .filter { it.id != userId }
                        .minByOrNull { it.getTimestamp("joinedAt")?.toDate()?.time ?: Long.MAX_VALUE }
                    if (nextAdmin != null) {
                        groupRef.collection(MEMBERS_SUBCOLLECTION)
                            .document(nextAdmin.id)
                            .update("role", "Admin")
                            .await()
                    }
                    // Remove user from group
                    leaveGroup(groupId, userId)
                } else {
                    // Regular member — just leave
                    leaveGroup(groupId, userId)
                }

                // Remove prayer items added by this user in the group
                val userItems = groupRef.collection(PRAYER_ITEMS_SUBCOLLECTION)
                    .whereEqualTo("addedBy", userId)
                    .get()
                    .await()
                for (item in userItems.documents) {
                    item.reference.delete().await()
                }

                // Remove user from prayedByUsers arrays in remaining items
                val allItems = groupRef.collection(PRAYER_ITEMS_SUBCOLLECTION).get().await()
                for (item in allItems.documents) {
                    val prayedBy = (item.get("prayedByUsers") as? List<*>)
                        ?.filterIsInstance<String>() ?: emptyList()
                    if (userId in prayedBy) {
                        item.reference.update(
                            "prayedByUsers", FieldValue.arrayRemove(userId),
                            "prayedByCount", FieldValue.increment(-1)
                        ).await()
                    }
                }
            }

            // 2. Delete the user's group index document and subcollection
            for (doc in userGroupRefs.documents) {
                doc.reference.delete().await()
            }
            // Delete the parent userGroups document
            db.collection(USER_GROUPS_COLLECTION).document(userId).delete().await()

            Log.d(TAG, "Deleted all Firestore data for user $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete all user data from Firestore", e)
            throw e
        }
    }

    /**
     * Delete an entire group and all its subcollections.
     *
     * Exposed for use by [PrayerGroupRepository.deleteGroupAsCreator] so
     * group admins can tear down a group for everyone, not just leave it
     * themselves. Callers must gate this on the current user actually
     * being the creator — Firestore security rules do the same check
     * server-side, but the UI should fail fast rather than round-trip.
     */
    suspend fun deleteEntireGroup(groupId: String) {
        val groupRef = db.collection(GROUPS_COLLECTION).document(groupId)

        // Delete members subcollection
        val members = groupRef.collection(MEMBERS_SUBCOLLECTION).get().await()
        for (member in members.documents) {
            // Also clean up each member's userGroups index
            db.collection(USER_GROUPS_COLLECTION)
                .document(member.id)
                .collection("groups")
                .document(groupId)
                .delete()
                .await()
            member.reference.delete().await()
        }

        // Delete prayer items subcollection
        val items = groupRef.collection(PRAYER_ITEMS_SUBCOLLECTION).get().await()
        for (item in items.documents) {
            item.reference.delete().await()
        }

        // Delete the group document itself
        groupRef.delete().await()
        Log.d(TAG, "Deleted entire group $groupId")
    }

    /**
     * Check if a share code is already in use.
     */
    suspend fun isShareCodeTaken(shareCode: String): Boolean {
        val snapshot = db.collection(GROUPS_COLLECTION)
            .whereEqualTo("shareCode", shareCode)
            .limit(1)
            .get()
            .await()
        return snapshot.documents.isNotEmpty()
    }

    companion object {
        private const val TAG = "FirestoreGroupService"
        private const val GROUPS_COLLECTION = "groups"
        private const val MEMBERS_SUBCOLLECTION = "members"
        private const val PRAYER_ITEMS_SUBCOLLECTION = "prayerItems"
        private const val ACTIVITY_SUBCOLLECTION = "activity"
        private const val USER_GROUPS_COLLECTION = "userGroups"
    }
}

// ─── Data Transfer Objects ───────────────────────────────────────────────

data class FirestoreGroup(
    val firestoreId: String,
    val name: String,
    val description: String = "",
    val emoji: String = "",
    val shareCode: String,
    val createdBy: String,
    val createdAt: Long,
    val memberCount: Int = 0
)

data class FirestoreGroupPrayerItem(
    val firestoreId: String,
    val title: String,
    val description: String = "",
    val addedBy: String,
    val addedByName: String = "",
    val addedAt: Long,
    val prayedByCount: Int = 0,
    val prayedByUsers: List<String> = emptyList(),
    val status: String = "Active"
)

data class FirestoreGroupMember(
    val userId: String,
    val displayName: String,
    val role: String,
    val joinedAt: Long
)

data class FirestoreGroupPrayerActivity(
    val firestoreId: String,
    val userId: String,
    val displayName: String,
    val prayedAt: Long
)
