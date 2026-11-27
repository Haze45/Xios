package com.yourapp.music.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.yourapp.music.R

data class BottomNavItem(
    val screen: Screen,
    val labelRes: Int,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(
        Screen.Home,
        R.string.nav_home,
        Icons.Filled.Home
    ),
    BottomNavItem(
        Screen.Library,
        R.string.nav_library,
        Icons.Filled.LibraryMusic
    ),
    BottomNavItem(
        Screen.Queue,
        R.string.nav_queue,
        Icons.Filled.Shuffle
    ),
    BottomNavItem(
        Screen.Playlists,
        R.string.nav_playlists,
        Icons.AutoMirrored.Filled.PlaylistPlay
    ),
    BottomNavItem(
        Screen.Settings,
        R.string.nav_settings,
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

    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor = Color(0xFFB3B3B3)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x99080808),
                        Color(0xE6080808),
                        Color(0xFF080808)
                    )
                )
            )
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { item ->

                val selected =
                    currentRoute == item.screen.route

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable {
                            if (item.screen == Screen.Queue) {
                                onQueueClick()
                            } else {
                                val returnedToExistingDestination =
                                    navController.popBackStack(
                                        item.screen.route,
                                        inclusive = false
                                    )

                                if (!returnedToExistingDestination) {
                                    navController.navigate(
                                        item.screen.route
                                    ) {
                                        launchSingleTop =
                                            true
                                    }
                                }
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = stringResource(
                            item.labelRes
                        ),
                        modifier = Modifier.size(29.dp),
                        tint = if (selected) {
                            selectedColor
                        } else {
                            unselectedColor
                        }
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = stringResource(item.labelRes),
                        color = if (selected) {
                            selectedColor
                        } else {
                            unselectedColor
                        },
                        fontSize = 11.sp,
                        fontWeight = if (selected) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Normal
                        },
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }
}
