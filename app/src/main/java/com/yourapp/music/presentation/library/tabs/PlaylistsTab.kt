package com.yourapp.music.presentation.library.tabs

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yourapp.music.domain.model.Playlist
import com.yourapp.music.presentation.components.PlaylistRowItem

@Composable
fun PlaylistsTab(
    playlists: List<Playlist>,
    onPlaylistClick: (Playlist) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(playlists, key = { it.id }) { playlist ->
            PlaylistRowItem(
                playlist = playlist,
                onClick = { onPlaylistClick(playlist) }
            )
        }
    }
}
