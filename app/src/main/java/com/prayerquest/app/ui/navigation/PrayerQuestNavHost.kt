package com.prayerquest.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.prayerquest.app.ui.collections.AddItemsToCollectionScreen
import com.prayerquest.app.ui.collections.CollectionDetailScreen
import com.prayerquest.app.ui.collections.CreateCollectionScreen
import com.prayerquest.app.ui.gratitude.GratitudeCatalogueScreen
import com.prayerquest.app.ui.gratitude.GratitudeLogScreen
import com.prayerquest.app.ui.groups.AddGroupPrayerScreen
import com.prayerquest.app.ui.groups.CreateGroupScreen
import com.prayerquest.app.ui.groups.GroupDetailScreen
import com.prayerquest.app.ui.groups.JoinGroupScreen
import com.prayerquest.app.ui.groups.PrayerGroupsScreen
import com.prayerquest.app.ui.library.AnsweredPrayerDetailScreen
import com.prayerquest.app.ui.library.FamousPrayerDetailScreen
import com.prayerquest.app.ui.prayer.ModePickerScreen
import com.prayerquest.app.ui.prayer.PrayerSessionScreen
import com.prayerquest.app.ui.premium.PremiumPaywallScreen
import com.prayerquest.app.ui.screen.HomeScreen
import com.prayerquest.app.ui.screen.LibraryScreen
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
                            icon = {
                                if (destination.icon != null) {
                                    Icon(destination.icon, contentDescription = destination.label)
                                } else if (destination.iconRes != null) {
                                    Icon(painterResource(id = destination.iconRes), contentDescription = destination.label)
                                }
                            },
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
                        navController.navigate(Routes.prayerModePicker())
                    },
                    onLogGratitude = {
                        navController.navigate(Routes.GRATITUDE_LOG)
                    },
                    onPrayerGroups = {
                        // Jumps to the Groups bottom-nav tab. Uses the same
                        // popUpTo/saveState dance as the bottom-nav click so
                        // backstack state stays coherent.
                        navController.navigate(TopLevelDestination.GROUPS.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenSettings = {
                        navController.navigate("settings")
                    }
                )
            }

            composable(TopLevelDestination.PRAY.route) {
                // Pray tab IS the Mode Picker — no intermediate "modes with a
                // button that opens the real picker" screen. onBackClick is
                // null because this is a bottom-nav root (users exit via the
                // bar, not a back arrow).
                ModePickerScreen(
                    onModePicked = { mode ->
                        navController.navigate(Routes.prayerSession(mode.name))
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
                    onOpenSettings = {
                        navController.navigate("settings")
                    },
                    onNavigateToPaywall = {
                        navController.navigate(Routes.PAYWALL)
                    }
                )
            }

            composable(TopLevelDestination.GROUPS.route) {
                PrayerGroupsScreen(
                    onNavigateBack = { navController.popBackStack() },
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

            // Settings is now reached via Profile's "Settings" button and the
            // Home cog icon — it's not a bottom-nav tab anymore.
            composable("settings") {
                SettingsScreen(
                    userPreferences = userPreferences
                )
            }

            // ═══════════════════════════════════════════
            // PRAYER MODE PICKER + PRAYER SESSION
            // ═══════════════════════════════════════════

            composable(
                route = Routes.PRAYER_MODE_PICKER,
                arguments = listOf(
                    navArgument("collectionId") {
                        type = NavType.LongType
                        defaultValue = Routes.NO_COLLECTION
                    }
                )
            ) { backStackEntry ->
                val collectionArg = backStackEntry.arguments?.getLong("collectionId")
                    ?: Routes.NO_COLLECTION
                ModePickerScreen(
                    onBackClick = { navController.popBackStack() },
                    onModePicked = { mode ->
                        // Replace the picker in the back stack so Back from a
                        // session returns to wherever the user started (Home,
                        // Collections Detail, etc.), not to the picker they
                        // just cleared. Forward the collection context so the
                        // session prays that collection's items.
                        navController.navigate(
                            Routes.prayerSession(
                                mode = mode.name,
                                collectionId = collectionArg.takeIf { it >= 0 }
                            )
                        ) {
                            popUpTo(Routes.PRAYER_MODE_PICKER) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Routes.PRAYER_SESSION,
                arguments = listOf(
                    navArgument("mode") { type = NavType.StringType },
                    navArgument("collectionId") {
                        type = NavType.LongType
                        defaultValue = Routes.NO_COLLECTION
                    }
                )
            ) { backStackEntry ->
                val modeArg = backStackEntry.arguments?.getString("mode").orEmpty()
                val collectionArg = backStackEntry.arguments?.getLong("collectionId")
                    ?: Routes.NO_COLLECTION
                PrayerSessionScreen(
                    chosenModeName = modeArg,
                    collectionId = collectionArg.takeIf { it >= 0 },
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
                    onNavigateToAddItems = { cid ->
                        navController.navigate(Routes.addItemsToCollection(cid))
                    },
                    onStartPraying = {
                        // Collections currently delegate mode choice to the
                        // Mode Picker. Sprint 6 may bypass the picker and
                        // auto-pick based on collection metadata. Pass
                        // collectionId so the resulting session prays the
                        // items in THIS collection instead of the global
                        // Active list (which was the source of the
                        // "placeholders instead of real prayer items" bug).
                        navController.navigate(Routes.prayerModePicker(collectionId))
                    }
                )
            }

            composable(Routes.CREATE_COLLECTION) {
                CreateCollectionScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onCollectionCreated = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.ADD_ITEMS_TO_COLLECTION,
                arguments = listOf(navArgument("collectionId") { type = NavType.LongType })
            ) { backStackEntry ->
                val cid = backStackEntry.arguments?.getLong("collectionId") ?: 0L
                AddItemsToCollectionScreen(
                    collectionId = cid,
                    onNavigateBack = { navController.popBackStack() }
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
                    onGratitudeSaved = { _ -> navController.popBackStack() },
                    onNavigateToPaywall = { navController.navigate(Routes.PAYWALL) },
                )
            }

            composable(Routes.GRATITUDE_CATALOGUE) {
                GratitudeCatalogueScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ═══════════════════════════════════════════
            // PRAYER GROUPS — detail routes (list screen is top-level above)
            // ═══════════════════════════════════════════

            composable(
                route = Routes.GROUP_DETAIL,
                arguments = listOf(navArgument("groupId") { type = NavType.LongType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getLong("groupId") ?: 0L
                GroupDetailScreen(
                    groupId = groupId,
                    onNavigateBack = { navController.popBackStack() },
                    onAddPrayer = { gid ->
                        navController.navigate(Routes.addGroupPrayer(gid))
                    }
                )
            }

            composable(
                route = Routes.ADD_GROUP_PRAYER,
                arguments = listOf(navArgument("groupId") { type = NavType.LongType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getLong("groupId") ?: 0L
                AddGroupPrayerScreen(
                    groupId = groupId,
                    onNavigateBack = { navController.popBackStack() },
                    onPrayerAdded = { navController.popBackStack() }
                )
            }

            composable(Routes.CREATE_GROUP) {
                CreateGroupScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onGroupCreated = { navController.popBackStack() },
                    onNavigateToPaywall = { navController.navigate(Routes.PAYWALL) },
                )
            }

            composable(Routes.JOIN_GROUP) {
                JoinGroupScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onGroupJoined = { navController.popBackStack() }
                )
            }

            // ═══════════════════════════════════════════
            // PREMIUM PAYWALL
            // ═══════════════════════════════════════════

            // Reached from Profile's upgrade card, inline paywall gates (e.g.
            // add-11th-photo, create-3rd-group), and the banner-ad "remove ads"
            // CTA. Single static route — offer is the same regardless of
            // entry point.
            composable(Routes.PAYWALL) {
                PremiumPaywallScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Settings is now a top-level destination (bottom bar tab)
        }
    }
}
