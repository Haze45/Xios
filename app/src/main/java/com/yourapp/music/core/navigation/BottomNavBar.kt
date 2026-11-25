package com.yourapp.music.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "Home", Icons.Filled.Home),
    BottomNavItem(
        Screen.Library,
        "Library",
        Icons.Filled.LibraryMusic
    ),
    BottomNavItem(
        Screen.Queue,
        "Queue",
        Icons.Filled.Shuffle
    ),
    BottomNavItem(
        Screen.Playlists,
        "Playlists",
        Icons.AutoMirrored.Filled.PlaylistPlay
    ),
    BottomNavItem(
        Screen.Settings,
        "Settings",
        Icons.Filled.Settings
    )
)

@Composable
fun MusicBottomNavBar(
    navController: NavController,
    onQueueClick: () -> Unit = {}
) {
    val navBackStackEntry by
    navController.currentBackStackEntryAsState()

    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.screen.route

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (item.screen == Screen.Queue) {
                        onQueueClick()
                    } else {
                        navController.navigate(item.screen.route) {
                            popUpTo(
                                navController.graph.findStartDestination().id
                            ) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(item.label)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor =
                        MaterialTheme.colorScheme.primary,
                    selectedTextColor =
                        MaterialTheme.colorScheme.primary,
                    unselectedIconColor =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor =
                        MaterialTheme.colorScheme.secondaryContainer
                )
            )
        }
    }
}
