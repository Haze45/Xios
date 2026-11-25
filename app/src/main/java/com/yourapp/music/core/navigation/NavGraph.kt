package com.yourapp.music.core.navigation

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yourapp.music.presentation.album.AlbumDetailScreen
import com.yourapp.music.presentation.auth.login.LoginScreen
import com.yourapp.music.presentation.auth.register.RegisterScreen
import com.yourapp.music.presentation.favorites.FavoritesScreen
import com.yourapp.music.presentation.player.PlayerScreen
import com.yourapp.music.presentation.player.PlayerViewModel
import com.yourapp.music.presentation.playlist.PlaylistDetailScreen
import com.yourapp.music.presentation.search.SearchScreen
@OptIn(UnstableApi::class)
@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    val appStartViewModel: AppStartViewModel =
        hiltViewModel()

    val onLogout: () -> Unit = {
        navController.navigate(Screen.Login.route) {
            popUpTo(0) // clear the entire back stack, including "main"
        }
    }

    NavHost(
        navController = navController,
        startDestination = appStartViewModel.startDestination
    ) {

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo(Screen.Login.route) {
                            inclusive = true
                        }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(
                        Screen.Register.route
                    )
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("main") {
                        popUpTo(Screen.Login.route) {
                            inclusive = true
                        }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable("main") {
            MainShell(
                onNavigateToSearch = {
                    navController.navigate(
                        Screen.Search.route
                    )
                },
                onNavigateToFavorites = {
                    navController.navigate(
                        Screen.Favorites.route
                    )
                },
                onNavigateToPlayer = {
                    navController.navigate(
                        Screen.Player.route
                    )
                },
                onNavigateToPlaylistDetail = { id ->
                    navController.navigate(
                        Screen.PlaylistDetail.createRoute(
                            id
                        )
                    )
                },
                onNavigateToAlbumDetail = { albumId ->
                    navController.navigate(
                        Screen.AlbumDetail.createRoute(
                            albumId
                        )
                    )
                },
                onLogout = onLogout
            )
        }

        composable(Screen.Player.route) {
            PlayerScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Search.route) {
            val playerViewModel: PlayerViewModel =
                hiltViewModel()
            SearchScreen(
                onCancelClick = { navController.popBackStack() },
                onSongClick = { song, queue ->
                    playerViewModel.playSong(song, queue)
                    navController.navigate(Screen.Player.route)
                }
            )
        }

        composable(
            route = Screen.PlaylistDetail.route,
            arguments = listOf(navArgument("playlistId") {
                type = NavType.StringType
            })
        ) {
            val playerViewModel: PlayerViewModel =
                hiltViewModel()
            PlaylistDetailScreen(
                onBackClick = { navController.popBackStack() },
                onSongClick = { song, queue ->
                    playerViewModel.playSong(song, queue)
                },
                onPlayAll = { songs ->
                    if (songs.isNotEmpty()) {
                        playerViewModel.playSong(
                            songs.first(),
                            songs
                        )
                        navController.navigate(Screen.Player.route)
                    }
                },
                onExpandPlayer = {
                    navController.navigate(Screen.Player.route)
                }
            )
        }

        composable(
            route = Screen.AlbumDetail.route,
            arguments = listOf(navArgument("albumId") {
                type = NavType.StringType
            })
        ) {
            val playerViewModel: PlayerViewModel =
                hiltViewModel()
            AlbumDetailScreen(
                onBackClick = { navController.popBackStack() },
                onSongClick = { song, queue ->
                    playerViewModel.playSong(song, queue)
                    navController.navigate(Screen.Player.route)
                },
                onPlayAll = { songs ->
                    if (songs.isNotEmpty()) {
                        playerViewModel.playSong(
                            songs.first(),
                            songs
                        )
                        navController.navigate(Screen.Player.route)
                    }
                }
            )
        }

        composable(Screen.Favorites.route) {
            val playerViewModel: PlayerViewModel =
                hiltViewModel()
            FavoritesScreen(
                onBackClick = { navController.popBackStack() },
                onSongClick = { song, queue ->
                    playerViewModel.playSong(song, queue)
                },
                onExpandPlayer = {
                    navController.navigate(Screen.Player.route)
                }
            )
        }

    }
}
