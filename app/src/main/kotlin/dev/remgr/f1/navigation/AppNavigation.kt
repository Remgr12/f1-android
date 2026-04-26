package dev.remgr.f1.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.remgr.f1.feature.leaderboard.ui.LeaderboardScreen
import dev.remgr.f1.feature.liveracehub.ui.LiveRaceHubScreen
import dev.remgr.f1.feature.pastraces.ui.PastRacesScreen
import dev.remgr.f1.feature.trackmap.ui.TrackMapScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Leaderboard : Screen("leaderboard", "Standings", Icons.Default.EmojiEvents)
    data object PastRaces   : Screen("past_races",  "Races",    Icons.Default.SportsScore)
    data object LiveHub     : Screen("live_hub",    "Live",     Icons.Default.LiveTv)
    data object TrackMap    : Screen("track_map",   "Track",    Icons.Default.Map)
}

private val topLevelScreens = listOf(
    Screen.Leaderboard,
    Screen.PastRaces,
    Screen.LiveHub,
    Screen.TrackMap,
)

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                topLevelScreens.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick  = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        },
                        icon  = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            navController    = navController,
            startDestination = Screen.Leaderboard.route,
        ) {
            composable(Screen.Leaderboard.route) { LeaderboardScreen() }
            composable(Screen.PastRaces.route)   { PastRacesScreen() }
            composable(Screen.LiveHub.route)     { LiveRaceHubScreen() }
            composable(Screen.TrackMap.route)    { TrackMapScreen() }
        }
    }
}
