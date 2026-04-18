package com.prayerquest.app.firebase

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Structural tests for the Firestore group-item payload.
 *
 * Photo Prayers (DD §3.9) are strictly local — the `photoUri` column on
 * [com.prayerquest.app.data.entity.PrayerItem] must never be uploaded to
 * Firestore, not even when the item is shared with a Prayer Group. The
 * primary defense is that [FirestoreGroupService.addPrayerItem] accepts
 * plain strings rather than a full `PrayerItem` entity, but that only holds
 * if the payload builder itself doesn't grow a `photoUri` key.
 *
 * These tests pin the payload shape so any future change that adds the
 * field has to knowingly rewrite them.
 */
class FirestoreGroupServiceTest {

    private val ts = Timestamp(1_700_000_000L, 0)

    @Test
    fun `group prayer item payload never contains photoUri`() {
        val payload = FirestoreGroupService.buildGroupPrayerItemPayload(
            title = "pray for my dad",
            description = "surgery tuesday",
            userId = "uid-123",
            displayName = "Nathan",
            nowTimestamp = ts,
        )

        assertFalse(
            "Payload must never contain photoUri — Photo Prayers are strictly local (DD §3.9)",
            payload.containsKey("photoUri")
        )
        // Also guard the obvious alternate spellings someone might introduce
        // if they don't read this test's rationale.
        assertFalse(payload.containsKey("photo"))
        assertFalse(payload.containsKey("photo_uri"))
        assertFalse(payload.containsKey("imageUri"))
        assertFalse(payload.containsKey("image"))
    }

    @Test
    fun `group prayer item payload exposes only the expected keys`() {
        val payload = FirestoreGroupService.buildGroupPrayerItemPayload(
            title = "Healing for Sarah",
            description = "chemotherapy side effects",
            userId = "uid-abc",
            displayName = "Pastor Joe",
            nowTimestamp = ts,
        )

        val expected = setOf(
            "title",
            "description",
            "addedBy",
            "addedByName",
            "addedAt",
            "prayedByCount",
            "prayedByUsers",
            "status",
        )
        assertEquals(
            "Unexpected payload keys — adding a key here requires revisiting DD §3.9 privacy",
            expected,
            payload.keys,
        )
    }

    @Test
    fun `payload does not leak the user's local photo even when title references one`() {
        // Regression guard: even if a caller pastes a local file path into
        // the title (which would be a UX bug in its own right), the payload
        // still only contains that string as the literal title, not as a
        // photoUri field that a downstream viewer could render.
        val payload = FirestoreGroupService.buildGroupPrayerItemPayload(
            title = "/data/user/0/com.prayerquest.app/files/prayer_photos/abc.jpg",
            description = "",
            userId = "uid-x",
            displayName = "User",
            nowTimestamp = ts,
        )
        assertTrue(payload.containsKey("title"))
        assertFalse(payload.containsKey("photoUri"))
    }
}
