package com.yourapp.music.core.navigation

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yourapp.music.presentation.home.HomeScreen
import com.yourapp.music.presentation.library.LibraryScreen
import com.yourapp.music.presentation.player.MiniPlayer
import com.yourapp.music.presentation.player.PlayerViewModel
import com.yourapp.music.presentation.playlist.PlaylistScreen
import com.yourapp.music.presentation.profile.ProfileScreen
import com.yourapp.music.presentation.queue.QueueBottomSheet
import com.yourapp.music.presentation.settings.SettingsScreen

@OptIn(UnstableApi::class)
@Composable
fun MainShell(
    onNavigateToSearch: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onNavigateToPlaylistDetail: (String) -> Unit,
    onNavigateToAlbumDetail: (String) -> Unit,
    onLogout: () -> Unit
) {
    val bottomNavController = rememberNavController()
    var showQueueSheet by remember { mutableStateOf(false) }

    val playerViewModel: PlayerViewModel = hiltViewModel()
    val playerState by playerViewModel.uiState.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,

        // Handle the top system inset centrally.
        // Bottom navigation remains an overlay.
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top
        )
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {

            // Main screen content.
            // It continues behind the floating bottom controls.
            NavHost(
                navController = bottomNavController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        onSearchClick = onNavigateToSearch,
                        onProfileClick = {
                            bottomNavController.navigate(
                                Screen.Profile.route
                            ) {
                                launchSingleTop = true
                            }
                        },
                        onFavoritesClick = onNavigateToFavorites,
                        onAlbumClick = onNavigateToAlbumDetail,
                        currentlyPlayingSongId = playerState.currentSong?.id,
                        isPlaying = playerState.isPlaying,
                        onSongPlay = { song, queue ->
                            playerViewModel.playSong(
                                song,
                                queue
                            )
                        }
                    )
                }

                composable(Screen.Library.route) {
                    LibraryScreen(
                        onSongClick = { song, queue ->
                            playerViewModel.playSong(
                                song,
                                queue
                            )
                        },
                        onPlaylistClick = { playlist ->
                            onNavigateToPlaylistDetail(
                                playlist.id
                            )
                        }
                    )
                }

                composable(Screen.Playlists.route) {
                    PlaylistScreen(
                        onPlaylistClick = { playlist ->
                            onNavigateToPlaylistDetail(
                                playlist.id
                            )
                        }
                    )
                }

                composable(Screen.Profile.route) {
                    ProfileScreen(
                        onBackClick = {
                            if (!bottomNavController.popBackStack()) {
                                bottomNavController.navigate(
                                    Screen.Home.route
                                ) {
                                    launchSingleTop = true
                                }
                            }
                        },
                        onSettingsClick = {
                            bottomNavController.navigate(
                                Screen.Settings.route
                            ) {
                                launchSingleTop = true
                            }
                        },
                        onPlaylistClick = { playlist ->
                            onNavigateToPlaylistDetail(
                                playlist.id
                            )
                        }
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onLogout = onLogout
                    )
                }
            }

            // Floating bottom controls.
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                MiniPlayer(
                    onExpandClick = onNavigateToPlayer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                MusicBottomNavBar(
                    navController = bottomNavController,
                    onQueueClick = {
                        showQueueSheet = true
                    }
                )
            }
        }
    }

    if (showQueueSheet) {
        QueueBottomSheet(
            onDismiss = {
                showQueueSheet = false
            }
        )
    }
}
