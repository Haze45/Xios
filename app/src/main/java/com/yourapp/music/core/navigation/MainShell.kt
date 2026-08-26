package com.yourapp.music.core.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yourapp.music.presentation.home.HomeScreen
import com.yourapp.music.presentation.library.LibraryScreen
import com.yourapp.music.presentation.player.MiniPlayer
import com.yourapp.music.presentation.player.PlayerViewModel
import com.yourapp.music.presentation.playlist.PlaylistScreen
import com.yourapp.music.presentation.queue.QueueBottomSheet
import com.yourapp.music.presentation.settings.SettingsScreen

/**
 * Hosts the bottom-nav destinations (Home, Library, Playlists, Settings)
 * inside a Scaffold with the bottom bar always visible. This whole shell is
 * pushed as a single destination from the root NavGraph after login.
 *
 * The docked MiniPlayer lives here too, not inside any individual tab —
 * it renders above the bottom nav bar and stays visible no matter which
 * tab is showing (it draws nothing when no song is playing). Previously
 * it was only rendered inside LibraryScreen, so it silently disappeared
 * on every other tab.
 *
 * The "Queue" bottom-nav item is not a navigation destination — tapping it
 * opens the queue as a bottom sheet over whichever tab is currently showing,
 * so switching tabs to check the queue doesn't lose your place.
 */
@Composable
fun MainShell(
    onNavigateToSearch: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onNavigateToPlaylistDetail: (String) -> Unit,
    onNavigateToAlbumDetail: (String) -> Unit,
    onLogout: () -> Unit
) {
    val bottomNavController = rememberNavController()
    var showQueueSheet by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            Column {
                MiniPlayer(
                    onExpandClick = onNavigateToPlayer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
                MusicBottomNavBar(
                    navController = bottomNavController,
                    onQueueClick = { showQueueSheet = true }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = bottomNavController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Home.route) {
                val playerViewModel: PlayerViewModel = hiltViewModel()
                HomeScreen(
                    onSearchClick = onNavigateToSearch,
                    onProfileClick = onNavigateToProfile,
                    onFavoritesClick = onNavigateToFavorites,
                    onAlbumClick = onNavigateToAlbumDetail,
                    onSongPlay = { song, queue -> playerViewModel.playSong(song, queue); onNavigateToPlayer() }
                )
            }
            composable(Screen.Library.route) {
                val playerViewModel: PlayerViewModel = hiltViewModel()
                LibraryScreen(
                    onSongClick = { song, queue -> playerViewModel.playSong(song, queue); onNavigateToPlayer() },
                    onPlaylistClick = { playlist -> onNavigateToPlaylistDetail(playlist.id) }
                )
            }
            composable(Screen.Playlists.route) {
                PlaylistScreen(
                    onPlaylistClick = { playlist -> onNavigateToPlaylistDetail(playlist.id) }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onLogout = onLogout)
            }
        }
    }

    if (showQueueSheet) {
        QueueBottomSheet(onDismiss = { showQueueSheet = false })
    }
}
