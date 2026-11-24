package com.yourapp.music.presentation.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourapp.music.core.theme.BackgroundBlack
import com.yourapp.music.domain.model.Playlist
import com.yourapp.music.domain.model.Song
import com.yourapp.music.presentation.components.FilterPill
import com.yourapp.music.presentation.components.ShimmerPlaylistList
import com.yourapp.music.presentation.library.tabs.AllTab
import com.yourapp.music.presentation.library.tabs.DownloadsTab
import com.yourapp.music.presentation.library.tabs.LikedSongsTab
import com.yourapp.music.presentation.library.tabs.PlaylistsTab

@Composable
fun LibraryScreen(
    onSongClick: (Song, List<Song>) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "My Music",
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(onClick = { /* overflow menu */ }) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "More"
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(
                8.dp
            )
        ) {
            items(LibraryTab.entries.toList()) { tab ->
                FilterPill(
                    label = tab.label(),
                    selected = state.selectedTab == tab,
                    onClick = { viewModel.onTabSelected(tab) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f)) {

            if (
                state.isLoading &&
                state.selectedTab == LibraryTab.PLAYLISTS
            ) {
                ShimmerPlaylistList(
                    modifier = Modifier.fillMaxSize(),
                    count = 5
                )
            } else {
                when (state.selectedTab) {
                    LibraryTab.ALL -> AllTab(
                        songs = state.allSongs,
                        onSongClick = onSongClick,
                        modifier = Modifier.fillMaxSize()
                    )

                    LibraryTab.PLAYLISTS -> PlaylistsTab(
                        playlists = state.playlists,
                        onPlaylistClick = onPlaylistClick,
                        modifier = Modifier.fillMaxSize()
                    )

                    LibraryTab.LIKED_SONGS -> LikedSongsTab(
                        songs = state.likedSongs,
                        onSongClick = onSongClick,
                        modifier = Modifier.fillMaxSize()
                    )

                    LibraryTab.DOWNLOADS -> DownloadsTab(
                        songs = state.downloadedSongs,
                        downloadingSongs = state.downloadingSongs,
                        onSongClick = onSongClick,
                        onPauseDownload = viewModel::pauseDownload,
                        onResumeDownload = viewModel::resumeDownload,
                        onCancelDownload = viewModel::cancelDownload,
                        onDeleteDownloadedSong = viewModel::deleteDownloadedSong,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

        }
    }
}

private fun LibraryTab.label(): String = when (this) {
    LibraryTab.ALL -> "All"
    LibraryTab.PLAYLISTS -> "Playlists"
    LibraryTab.LIKED_SONGS -> "Liked Songs"
    LibraryTab.DOWNLOADS -> "Downloads"
}
