package com.prayerquest.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Bottom navigation destinations — Home, Pray, Library, Profile.
 */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME("home", "Home", Icons.Default.Home),
    PRAY("pray", "Pray", Icons.Default.SelfImprovement),
    LIBRARY("library", "Library", Icons.Default.AutoStories),
    PROFILE("profile", "Profile", Icons.Default.Person)
}

/**
 * Non-bottom-bar routes for detail screens (Phase 2+).
 */
object Routes {
    const val PRAYER_SESSION = "prayer_session"
    const val PRAYER_SESSION_COLLECTION = "prayer_session/{collectionId}"
    const val COLLECTION_DETAIL = "collection_detail/{collectionId}"
    const val CREATE_COLLECTION = "create_collection"
    const val EDIT_COLLECTION = "edit_collection/{collectionId}"
    const val FAMOUS_PRAYER_DETAIL = "famous_prayer/{prayerId}"
    const val ANSWERED_PRAYERS = "answered_prayers"
    const val GRATITUDE_LOG = "gratitude_log"
    const val GRATITUDE_CATALOGUE = "gratitude_catalogue"
    const val GROUP_DETAIL = "group_detail/{groupId}"
    const val CREATE_GROUP = "create_group"
    const val JOIN_GROUP = "join_group"
}
