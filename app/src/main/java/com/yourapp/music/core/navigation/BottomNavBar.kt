package com.yourapp.music.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.yourapp.music.core.theme.AccentGreen
import com.yourapp.music.core.theme.SurfaceDark
import com.yourapp.music.core.theme.TextTertiary

data class BottomNavItem(val screen: Screen, val label: String, val icon: ImageVector)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "Home", Icons.Filled.Home),
    BottomNavItem(Screen.Library, "Library", Icons.Filled.LibraryMusic),
    BottomNavItem(Screen.Queue, "Queue", Icons.Filled.Shuffle),
    BottomNavItem(Screen.Playlists, "Playlists", Icons.Filled.PlaylistPlay),
    BottomNavItem(Screen.Settings, "Settings", Icons.Filled.Settings),
)

@Composable
fun MusicBottomNavBar(
    navController: NavController,
    onQueueClick: () -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(containerColor = SurfaceDark) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.screen.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    // Queue isn't a real navigation destination — it opens the
                    // queue bottom sheet on top of whatever tab is currently
                    // showing, rather than replacing it.
                    if (item.screen == Screen.Queue) {
                        onQueueClick()
                    } else {
                        navController.navigate(item.screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AccentGreen,
                    selectedTextColor = AccentGreen,
                    unselectedIconColor = TextTertiary,
                    unselectedTextColor = TextTertiary,
                    indicatorColor = SurfaceDark
                )
            )
        }
    }
}
