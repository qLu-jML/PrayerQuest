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
import com.prayerquest.app.ui.celebration.AnsweredCelebrationScreen
import com.prayerquest.app.ui.collections.AddItemsToCollectionScreen
import com.prayerquest.app.ui.collections.CollectionDetailScreen
import com.prayerquest.app.ui.collections.CreateCollectionScreen
import com.prayerquest.app.ui.collections.EditPrayerItemScreen
import com.prayerquest.app.ui.crisis.CrisisBreathPrayerScreen
import com.prayerquest.app.ui.crisis.CrisisPrayerScreen
import com.prayerquest.app.ui.crisis.CrisisPsalmsReaderScreen
import com.prayerquest.app.ui.crisis.CrisisResourcesScreen
import com.prayerquest.app.ui.gratitude.GratitudeCatalogueScreen
import com.prayerquest.app.ui.gratitude.GratitudeLogScreen
import com.prayerquest.app.ui.gratitude.GratitudeSpeedRoundScreen
import com.prayerquest.app.ui.groups.AddGroupPrayerScreen
import com.prayerquest.app.ui.groups.CreateGroupScreen
import com.prayerquest.app.ui.groups.GroupDetailScreen
import com.prayerquest.app.ui.groups.JoinGroupScreen
import com.prayerquest.app.ui.groups.PrayerGroupsScreen
import com.prayerquest.app.ui.library.AnsweredPrayerDetailScreen
import com.prayerquest.app.ui.library.BiblePrayerDetailScreen
import com.prayerquest.app.ui.library.FamousPrayerDetailScreen
import com.prayerquest.app.ui.prayer.ModePickerScreen
import com.prayerquest.app.ui.prayer.PrayerListPickerScreen
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
                    },
                    onCrisisPrayer = {
                        // Routes to the Crisis Prayer hub. Not in the
                        // regular prayer-session route tree — Crisis flow
                        // lives in its own set of routes so a distracted
                        // tap on Back never lands on Session summary with
                        // a "You earned XP!" toast.
                        navController.navigate(Routes.CRISIS_PRAYER)
                    },
                    onLiturgicalTap = {
                        // Route to the Library tab. LibraryViewModel already
                        // knows today's liturgical season via UserPreferences +
                        // LiturgicalCalendar.today(), so the seasonal pack is
                        // pinned at the top of the Collections shelf on arrival.
                        navController.navigate(TopLevelDestination.LIBRARY.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(TopLevelDestination.PRAY.route) {
                // Pray tab IS the Mode Picker — no intermediate "modes with a
                // button that opens the real picker" screen. onBackClick is
                // null because this is a bottom-nav root (users exit via the
                // bar, not a back arrow). After picking a mode we route to
                // the list picker so the user can choose which pool of
                // prayers to focus on (general, a collection, or a group).
                ModePickerScreen(
                    onModePicked = { mode ->
                        navController.navigate(Routes.prayerListPicker(mode.name))
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
                    onNavigateToBiblePrayerDetail = { prayerId ->
                        navController.navigate("bible_prayer/$prayerId")
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
                    userPreferences = userPreferences,
                    onNavigateBack = { navController.popBackStack() }
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
                val hasCollectionContext = collectionArg >= 0
                ModePickerScreen(
                    onBackClick = { navController.popBackStack() },
                    onModePicked = { mode ->
                        // Replace the picker in the back stack so Back from a
                        // session returns to wherever the user started (Home,
                        // Collections Detail, etc.), not to the picker they
                        // just cleared.
                        //
                        // Two flows:
                        //   1. Collection already chosen (launched from a
                        //      Collection Detail "Pray this" button) → skip
                        //      the list picker and go straight to the session.
                        //   2. No collection context (Home "Start Prayer") →
                        //      route through the list picker so the user can
                        //      choose general / a collection / a group.
                        if (hasCollectionContext) {
                            navController.navigate(
                                Routes.prayerSession(
                                    mode = mode.name,
                                    collectionId = collectionArg
                                )
                            ) {
                                popUpTo(Routes.PRAYER_MODE_PICKER) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Routes.prayerListPicker(mode.name)) {
                                popUpTo(Routes.PRAYER_MODE_PICKER) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(
                route = Routes.PRAYER_LIST_PICKER,
                arguments = listOf(
                    navArgument("mode") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val modeArg = backStackEntry.arguments?.getString("mode").orEmpty()
                PrayerListPickerScreen(
                    chosenModeName = modeArg,
                    onBackClick = { navController.popBackStack() },
                    onGeneralPrayers = {
                        // Pop the list picker off the stack before launching
                        // the session so Back from Session summary returns the
                        // user to Home / Pray tab, not back into the picker.
                        navController.navigate(Routes.prayerSession(modeArg)) {
                            popUpTo(Routes.PRAYER_LIST_PICKER) { inclusive = true }
                        }
                    },
                    onCollectionPicked = { collectionId ->
                        navController.navigate(
                            Routes.prayerSession(modeArg, collectionId)
                        ) {
                            popUpTo(Routes.PRAYER_LIST_PICKER) { inclusive = true }
                        }
                    },
                    onAddItemsToCollection = { collectionId ->
                        // User tapped an empty collection and chose "Add
                        // Prayers". Pop the list picker so that after they
                        // finish adding items, Back lands them on wherever
                        // they came from (Home / Pray tab) instead of the
                        // stale picker that no longer reflects their edits.
                        navController.navigate(
                            Routes.addItemsToCollection(collectionId)
                        ) {
                            popUpTo(Routes.PRAYER_LIST_PICKER) { inclusive = true }
                        }
                    },
                    onBrowseLibrary = {
                        // "General Prayers" was empty and the user chose
                        // to browse the Library. Jump to the bottom-nav
                        // Library tab using the same popUpTo/saveState
                        // dance bottom-nav clicks use, so the tab's scroll
                        // state is restored and the stack stays clean.
                        navController.navigate(TopLevelDestination.LIBRARY.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onCreateCollection = {
                        // Drop the user straight into the new-collection
                        // sheet. Pop the list picker so Back from the
                        // sheet returns to wherever they started.
                        navController.navigate(Routes.CREATE_COLLECTION) {
                            popUpTo(Routes.PRAYER_LIST_PICKER) { inclusive = true }
                        }
                    },
                    onGroupPicked = { groupId ->
                        // Groups aren't a native session scope yet — route to
                        // the group detail where the user can pick a specific
                        // shared request and pray it. Pop the list picker so
                        // the back stack stays tidy.
                        navController.navigate("group_detail/$groupId") {
                            popUpTo(Routes.PRAYER_LIST_PICKER) { inclusive = true }
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
                    },
                    onEditItem = { itemId ->
                        navController.navigate(Routes.editPrayerItem(itemId))
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
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPaywall = { navController.navigate(Routes.PAYWALL) }
                )
            }

            // Edit Prayer Item — Photo Prayer (DD §3.9) + title/description.
            // Reached from Collection Detail by tapping a prayer card.
            composable(
                route = Routes.EDIT_PRAYER_ITEM,
                arguments = listOf(navArgument("prayerItemId") { type = NavType.LongType })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getLong("prayerItemId") ?: 0L
                EditPrayerItemScreen(
                    prayerItemId = itemId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPaywall = { navController.navigate(Routes.PAYWALL) }
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
            // BIBLE PRAYER DETAIL
            // ═══════════════════════════════════════════

            composable(
                route = Routes.BIBLE_PRAYER_DETAIL,
                arguments = listOf(navArgument("prayerId") { type = NavType.StringType })
            ) { backStackEntry ->
                val prayerId = backStackEntry.arguments?.getString("prayerId") ?: ""
                BiblePrayerDetailScreen(
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
                    onNavigateToSpeedRound = {
                        navController.navigate(Routes.GRATITUDE_SPEED_ROUND)
                    },
                )
            }

            composable(Routes.GRATITUDE_CATALOGUE) {
                GratitudeCatalogueScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Speed Round is a sibling of the Log screen — navigating back
            // pops straight to wherever Speed Round was launched from (the
            // Log screen today, but we also expose it from Home CTAs later).
            // Finishing the round also just pops, and the Log screen's
            // gratitude-count badge refreshes via its Flow subscription, so
            // there's no explicit state-hand-off needed here.
            composable(Routes.GRATITUDE_SPEED_ROUND) {
                GratitudeSpeedRoundScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onCompleted = { _, _, _ -> navController.popBackStack() }
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

            // ═══════════════════════════════════════════
            // BIG CELEBRATION MOMENT (DD §3.5.2)
            //
            // Full-screen modal that plays when a prayer is marked Answered.
            // Callers only navigate here after confirming wasNewlyAnswered,
            // so re-marking an already-answered prayer (or importing one
            // already in the Answered state) never triggers the celebration.
            // onDismiss pops the back stack so the user returns to whichever
            // screen marked the prayer — typically Answered Prayer Detail or
            // a prayer item detail sheet.
            // ═══════════════════════════════════════════

            composable(
                route = Routes.ANSWERED_CELEBRATION,
                arguments = listOf(navArgument("prayerId") { type = NavType.LongType })
            ) { backStackEntry ->
                val prayerId = backStackEntry.arguments?.getLong("prayerId") ?: 0L
                AnsweredCelebrationScreen(
                    prayerId = prayerId,
                    onDismiss = { navController.popBackStack() }
                )
            }

            // ═══════════════════════════════════════════
            // CRISIS PRAYER MODE (DD §3.10)
            //
            // Its own route subtree so none of the screens here share a
            // back-stack entry with PrayerSessionViewModel (the owner of
            // the regular XP-awarding finalization path). If a user hits
            // Back from any crisis sub-screen they land on the Crisis hub,
            // and from there Back lands on Home — never on a session
            // summary, never on a "great job!" celebration.
            // ═══════════════════════════════════════════

            composable(Routes.CRISIS_PRAYER) {
                CrisisPrayerScreen(
                    onOpenPsalms = { navController.navigate(Routes.CRISIS_PSALMS) },
                    onOpenBreath = { navController.navigate(Routes.CRISIS_BREATH) },
                    onOpenResources = { navController.navigate(Routes.CRISIS_RESOURCES) },
                    onReturnHome = {
                        // Pop back to the Home tab without replaying the
                        // Crisis entry fade — the user asked to leave.
                        navController.popBackStack(
                            route = TopLevelDestination.HOME.route,
                            inclusive = false
                        )
                    }
                )
            }

            composable(Routes.CRISIS_PSALMS) {
                CrisisPsalmsReaderScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.CRISIS_BREATH) {
                CrisisBreathPrayerScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.CRISIS_RESOURCES) {
                CrisisResourcesScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Settings is now a top-level destination (bottom bar tab)
        }
    }
}
