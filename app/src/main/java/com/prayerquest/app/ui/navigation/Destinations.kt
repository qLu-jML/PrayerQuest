package com.prayerquest.app.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.prayerquest.app.R

/**
 * Bottom navigation destinations — Home, Pray, Library, Groups, Profile.
 *
 * Settings was moved out of the bottom bar — it's now reachable via the
 * cog icon in the Home top bar and a "Settings" button inside the Profile
 * screen. Groups took its slot so Prayer Groups is a top-level tap.
 *
 * Each destination has either an [icon] (Material ImageVector) or an [iconRes]
 * (custom drawable resource) — never both.
 */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector? = null,
    @DrawableRes val iconRes: Int? = null
) {
    HOME("home", "Home", icon = Icons.Default.Home),
    PRAY("pray", "Pray", iconRes = R.drawable.ic_praying_hands),
    LIBRARY("library", "Library", icon = Icons.Default.AutoStories),
    GROUPS("prayer_groups", "Groups", icon = Icons.Default.Groups),
    PROFILE("profile", "Profile", icon = Icons.Default.Person)
}

/**
 * Non-bottom-bar routes for detail screens (Phase 2+).
 */
object Routes {
    /**
     * Sentinel value used for the optional `collectionId` query arg — nav
     * LongType doesn't support nullable, so -1 means "no collection". Bake
     * the constant in one place so producers/consumers stay in sync.
     */
    const val NO_COLLECTION: Long = -1L

    /**
     * Mode picker — "Netflix of Prayer Modes" (DD §3.1.3). Entry point for every
     * prayer session: user picks a mode here, then the session runs that mode.
     *
     * Optional `collectionId` query arg — when launched from a collection
     * detail screen we carry it through so the resulting session prays the
     * items in that collection instead of the user's global Active list. The
     * picker itself doesn't care about the id; it just forwards it when
     * navigating to the session.
     */
    const val PRAYER_MODE_PICKER = "prayer_mode_picker?collectionId={collectionId}"
    fun prayerModePicker(collectionId: Long? = null): String =
        "prayer_mode_picker?collectionId=${collectionId ?: NO_COLLECTION}"

    /**
     * Prayer session accepts the chosen mode as a path arg. The value is a
     * [com.prayerquest.app.domain.model.PrayerMode] enum name (e.g.
     * "GUIDED_ACTS"). An optional "mixed" sentinel (empty string) preserves the
     * older cycle-through-all-modes behavior for callers that still want it
     * (e.g., quest prompts requiring ≥3 modes).
     *
     * Optional `collectionId` query arg — when set (≥ 0) the session loads
     * that collection's prayer items as the session queue instead of the
     * user's global Active list.
     */
    const val PRAYER_SESSION = "prayer_session/{mode}?collectionId={collectionId}"
    const val PRAYER_SESSION_MIXED = "prayer_session/MIXED"
    fun prayerSession(mode: String, collectionId: Long? = null): String =
        "prayer_session/$mode?collectionId=${collectionId ?: NO_COLLECTION}"

    const val COLLECTION_DETAIL = "collection_detail/{collectionId}"
    const val CREATE_COLLECTION = "create_collection"
    const val EDIT_COLLECTION = "edit_collection/{collectionId}"
    const val ADD_ITEMS_TO_COLLECTION = "add_items_to_collection/{collectionId}"
    fun addItemsToCollection(collectionId: Long): String =
        "add_items_to_collection/$collectionId"
    const val FAMOUS_PRAYER_DETAIL = "famous_prayer/{prayerId}"
    const val ANSWERED_PRAYERS = "answered_prayers"
    const val GRATITUDE_LOG = "gratitude_log"
    const val GRATITUDE_CATALOGUE = "gratitude_catalogue"
    const val GROUP_DETAIL = "group_detail/{groupId}"
    const val CREATE_GROUP = "create_group"
    const val JOIN_GROUP = "join_group"
    const val ADD_GROUP_PRAYER = "add_group_prayer/{groupId}"
    fun addGroupPrayer(groupId: Long): String = "add_group_prayer/$groupId"

    /**
     * Premium paywall — shown from Profile, inline gates, or the "tap to
     * remove ads" CTA. Static route (no args) because the offer is the same
     * regardless of where the user came from. If we ever want to A/B test
     * different headlines per entry point, move to a query arg.
     */
    const val PAYWALL = "paywall"
}
