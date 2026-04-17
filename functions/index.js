const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

/**
 * Safety-net Cloud Function: triggers when a Firebase Auth account is deleted.
 * Cleans up all Firestore data for that user — groups, memberships, prayer items,
 * and the userGroups index.
 */
exports.onUserDeleted = functions.auth.user().onDelete(async (user) => {
  const userId = user.uid;
  console.log(`Cleaning up Firestore data for deleted user: ${userId}`);

  try {
    const userGroupsRef = db.collection("userGroups").doc(userId).collection("groups");
    const userGroupsSnapshot = await userGroupsRef.get();
    const groupIds = userGroupsSnapshot.docs.map((doc) => doc.id);

    console.log(`User ${userId} belongs to ${groupIds.length} groups`);

    for (const groupId of groupIds) {
      const groupRef = db.collection("groups").doc(groupId);

      const membersSnapshot = await groupRef.collection("members").get();
      const admins = membersSnapshot.docs.filter(
        (doc) => doc.data().role === "Admin"
      );
      const isSoleAdmin = admins.length === 1 && admins[0].id === userId;

      if (isSoleAdmin && membersSnapshot.size === 1) {
        console.log(`Deleting orphaned group ${groupId}`);
        await deleteEntireGroup(groupId);
      } else if (isSoleAdmin) {
        const nextAdmin = membersSnapshot.docs
          .filter((doc) => doc.id !== userId)
          .sort((a, b) => {
            const aTime = a.data().joinedAt?.toMillis() || 0;
            const bTime = b.data().joinedAt?.toMillis() || 0;
            return aTime - bTime;
          })[0];

        if (nextAdmin) {
          await groupRef.collection("members").doc(nextAdmin.id).update({ role: "Admin" });
          console.log(`Promoted ${nextAdmin.id} to Admin in group ${groupId}`);
        }
        await removeUserFromGroup(groupId, userId);
      } else {
        await removeUserFromGroup(groupId, userId);
      }

      // Remove prayer items added by this user
      const userItems = await groupRef
        .collection("prayerItems")
        .where("addedBy", "==", userId)
        .get();

      if (userItems.docs.length > 0) {
        const batch = db.batch();
        userItems.docs.forEach((doc) => batch.delete(doc.ref));
        await batch.commit();
      }

      // Remove user from prayedByUsers arrays
      const allItems = await groupRef.collection("prayerItems").get();
      for (const item of allItems.docs) {
        const prayedBy = item.data().prayedByUsers || [];
        if (prayedBy.includes(userId)) {
          await item.ref.update({
            prayedByUsers: admin.firestore.FieldValue.arrayRemove(userId),
            prayedByCount: admin.firestore.FieldValue.increment(-1),
          });
        }
      }
    }

    // Delete the userGroups index
    if (userGroupsSnapshot.docs.length > 0) {
      const batch = db.batch();
      userGroupsSnapshot.docs.forEach((doc) => batch.delete(doc.ref));
      await batch.commit();
    }
    await db.collection("userGroups").doc(userId).delete();

    console.log(`Successfully cleaned up all data for user ${userId}`);
  } catch (error) {
    console.error(`Failed to clean up data for user ${userId}:`, error);
  }
});

async function removeUserFromGroup(groupId, userId) {
  const groupRef = db.collection("groups").doc(groupId);
  await groupRef.collection("members").doc(userId).delete();
  await groupRef.update({
    memberCount: admin.firestore.FieldValue.increment(-1),
  });
  console.log(`Removed user ${userId} from group ${groupId}`);
}

async function deleteEntireGroup(groupId) {
  const groupRef = db.collection("groups").doc(groupId);

  const members = await groupRef.collection("members").get();
  for (const member of members.docs) {
    await db.collection("userGroups").doc(member.id).collection("groups").doc(groupId).delete();
    await member.ref.delete();
  }

  const items = await groupRef.collection("prayerItems").get();
  if (items.docs.length > 0) {
    const batch = db.batch();
    items.docs.forEach((doc) => batch.delete(doc.ref));
    await batch.commit();
  }

  await groupRef.delete();
  console.log(`Deleted entire group ${groupId}`);
}
