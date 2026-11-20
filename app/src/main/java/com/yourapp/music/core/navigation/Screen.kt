package com.yourapp.music.core.navigation

sealed class Screen(val route: String) {
    // Auth
    data object Login : Screen("login")
    data object Register : Screen("register")

    // Bottom nav
    data object Home : Screen("home")
    data object Library : Screen("library")
    data object Queue : Screen("queue")
    data object Playlists : Screen("playlists")
    data object Settings : Screen("settings")

    // Reachable via top icons / nested nav
    data object Search : Screen("search")
    data object Profile : Screen("profile")
    data object Favorites : Screen("favorites")
    data object Player : Screen("player")
    data object PlaylistDetail :
        Screen("playlist_detail/{playlistId}") {
        fun createRoute(playlistId: String) =
            "playlist_detail/$playlistId"
    }

    data object AlbumDetail :
        Screen("album_detail/{albumId}") {
        fun createRoute(albumId: String) =
            "album_detail/" + java.net.URLEncoder.encode(
                albumId,
                "UTF-8"
            )
    }
}
