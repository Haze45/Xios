package com.yourapp.music.presentation.library.tabs

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.yourapp.music.domain.model.Song
import com.yourapp.music.presentation.components.SongListItem
import com.yourapp.music.presentation.player.PlayerViewModel

@Composable
fun AllTab(
    songs: List<Song>,
    onSongClick: (Song, List<Song>) -> Unit,
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val playerState by playerViewModel.uiState.collectAsState()

    LazyColumn(modifier = modifier) {
        items(songs, key = { it.id }) { song ->
            SongListItem(
                song = song,
                onClick = { onSongClick(song, songs) },
                onPlayClick = {
                    if (playerState.currentSong?.id == song.id) {
                        playerViewModel.togglePlayPause()
                    } else {
                        playerViewModel.playSong(song, songs)
                    }
                },
                isCurrentSong = playerState.currentSong?.id == song.id,
                isActivelyPlaying = playerState.currentSong?.id == song.id && playerState.isPlaying
            )
        }
    }
}
