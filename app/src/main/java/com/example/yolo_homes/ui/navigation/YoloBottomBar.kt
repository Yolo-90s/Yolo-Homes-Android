package com.example.yolo_homes.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.yolo_homes.data.model.UserSession

/**
 * The persistent bottom navigation — Home / Maintenance / Water / Reports /
 * Profile, always visible with text labels. Replaces icon-only top-bar
 * actions and swipeable tabs with no visible affordance (see plan notes).
 * [TopLevelDestination] already had everything this needs (route, label,
 * icon) — it was just never wired to an actual `NavigationBar` until now.
 *
 * Tabs are filtered by role: a Guest only sees Home + Profile (everything
 * else is flat-scoped or building-wide data they have no access to); a
 * Member additionally sees Maintenance + Water (their own flat's data) but
 * not Reports (building-wide, Admin/Developer only).
 */
@Composable
fun YoloBottomBar(navController: NavHostController, session: UserSession) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val visibleDestinations = TopLevelDestination.entries.filter { it.isVisibleFor(session) }

    NavigationBar {
        visibleDestinations.forEach { destination ->
            val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(destination.route) {
                        // Pop up to the start destination to avoid a huge stack
                        // of tab visits, keep at most one copy of each tab, and
                        // restore its scroll/entered state when returning to it.
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) }
            )
        }
    }
}
