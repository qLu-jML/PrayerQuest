package com.prayerquest.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.prayerquest.app.PrayerQuestApplication
import com.prayerquest.app.data.preferences.UserPreferences
import com.prayerquest.app.ui.collections.CollectionDetailScreen
import com.prayerquest.app.ui.collections.CreateCollectionScreen
import com.prayerquest.app.ui.gratitude.GratitudeCatalogueScreen
import com.prayerquest.app.ui.gratitude.GratitudeLogScreen
import com.prayerquest.app.ui.groups.CreateGroupScreen
import com.prayerquest.app.ui.groups.GroupDetailScreen
import com.prayerquest.app.ui.groups.JoinGroupScreen
import com.prayerquest.app.ui.groups.PrayerGroupsScreen
import com.prayerquest.app.ui.library.AnsweredPrayerDetailScreen
import com.prayerquest.app.ui.library.FamousPrayerDetailScreen
import com.prayerquest.app.ui.prayer.PrayerSessionScreen
import com.prayerquest.app.ui.screen.HomeScreen
import com.prayerquest.app.ui.screen.LibraryScreen
import com.prayerquest.app.ui.screen.PrayScreen
import com.prayerquest.app.ui.screen.ProfileScreen
import com.prayerquest.app.ui.screen.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerQuestNavHost(
    userPreferences: UserPreferences
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Hide bottom bar on detail/modal screens
    val bottomBarRoutes = TopLevelDestination.entries.map { it.route }.toSet()
    val showBottomBar = currentDestination?.route in bottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.HOME.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // ═══════════════════════════════════════════
            // TOP-LEVEL SCREENS (bottom bar visible)
            // ═══════════════════════════════════════════

            composable(TopLevelDestination.HOME.route) {
                HomeScreen(
                    onStartPrayer = {
                        navController.navigate(Routes.PRAYER_SESSION)
                    },
                    onLogGratitude = {
                        navController.navigate(Routes.GRATITUDE_LOG)
                    },
                    onPrayerGroups = {
                        navController.navigate("prayer_groups")
                    }
                )
            }

            composable(TopLevelDestination.PRAY.route) {
                PrayScreen(
                    onStartSession = {
                        navController.navigate(Routes.PRAYER_SESSION)
                    }
                )
            }

            composable(TopLevelDestination.LIBRARY.route) {
                LibraryScreen(
                    onNavigateToCollectionDetail = { collectionId ->
                        navController.navigate("collection_detail/$collectionId")
                    },
                    onNavigateToFamousPrayerDetail = { prayerId ->
                        navController.navigate("famous_prayer/$prayerId")
                    },
                    onNavigateToAnsweredPrayerDetail = { prayerId ->
                        navController.navigate("answered_prayer/$prayerId")
                    },
                    onNavigateToCreateCollection = {
                        navController.navigate(Routes.CREATE_COLLECTION)
                    }
                )
            }

            composable(TopLevelDestination.PROFILE.route) {
                ProfileScreen(
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    }
                )
            }

            // ═══════════════════════════════════════════
            // PRAYER SESSION
            // ═══════════════════════════════════════════

            composable(Routes.PRAYER_SESSION) {
                PrayerSessionScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // ═══════════════════════════════════════════
            // COLLECTION SCREENS
            // ═══════════════════════════════════════════

            composable(
                route = Routes.COLLECTION_DETAIL,
                arguments = listOf(navArgument("collectionId") { type = NavType.LongType })
            ) { backStackEntry ->
                val collectionId = backStackEntry.arguments?.getLong("collectionId") ?: 0L
                CollectionDetailScreen(
                    collectionId = collectionId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAddItems = { _ ->
                        // Could navigate to an add-items screen; for now just pop back
                    },
                    onStartPraying = {
                        navController.navigate(Routes.PRAYER_SESSION)
                    }
                )
            }

            composable(Routes.CREATE_COLLECTION) {
                CreateCollectionScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onCollectionCreated = { navController.popBackStack() }
                )
            }

            // ═══════════════════════════════════════════
            // FAMOUS PRAYER DETAIL
            // ═══════════════════════════════════════════

            composable(
                route = Routes.FAMOUS_PRAYER_DETAIL,
                arguments = listOf(navArgument("prayerId") { type = NavType.StringType })
            ) { backStackEntry ->
                val prayerId = backStackEntry.arguments?.getString("prayerId") ?: ""
                FamousPrayerDetailScreen(
                    prayerId = prayerId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ═══════════════════════════════════════════
            // ANSWERED PRAYER DETAIL
            // ═══════════════════════════════════════════

            composable(
                route = "answered_prayer/{prayerId}",
                arguments = listOf(navArgument("prayerId") { type = NavType.LongType })
            ) { backStackEntry ->
                val prayerId = backStackEntry.arguments?.getLong("prayerId") ?: 0L
                AnsweredPrayerDetailScreen(
                    prayerId = prayerId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ═══════════════════════════════════════════
            // GRATITUDE SCREENS
            // ═══════════════════════════════════════════

            composable(Routes.GRATITUDE_LOG) {
                GratitudeLogScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onGratitudeSaved = { _ -> navController.popBackStack() }
                )
            }

            composable(Routes.GRATITUDE_CATALOGUE) {
                GratitudeCatalogueScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ═══════════════════════════════════════════
            // PRAYER GROUPS
            // ═══════════════════════════════════════════

            composable("prayer_groups") {
                PrayerGroupsScreen(
                    onNavigateToGroupDetail = { groupId ->
                        navController.navigate("group_detail/$groupId")
                    },
                    onNavigateToCreateGroup = {
                        navController.navigate(Routes.CREATE_GROUP)
                    },
                    onNavigateToJoinGroup = {
                        navController.navigate(Routes.JOIN_GROUP)
                    }
                )
            }

            composable(
                route = Routes.GROUP_DETAIL,
                arguments = listOf(navArgument("groupId") { type = NavType.LongType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getLong("groupId") ?: 0L
                GroupDetailScreen(
                    groupId = groupId,
                    onNavigateBack = { navController.popBackStack() },
                    onAddPrayer = { _ -> }
                )
            }

            composable(Routes.CREATE_GROUP) {
                CreateGroupScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onGroupCreated = { navController.popBackStack() }
                )
            }

            composable(Routes.JOIN_GROUP) {
                JoinGroupScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onGroupJoined = { navController.popBackStack() }
                )
            }

            // ═══════════════════════════════════════════
            // SETTINGS
            // ═══════════════════════════════════════════

            composable("settings") {
                SettingsScreen(
                    userPreferences = userPreferences,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
