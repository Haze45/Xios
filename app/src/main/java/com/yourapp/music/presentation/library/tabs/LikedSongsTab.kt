package com.yourapp.music.presentation.library.tabs

import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.yourapp.music.R
import com.yourapp.music.core.theme.TextSecondary
import com.yourapp.music.domain.model.Song
import com.yourapp.music.presentation.components.SongListItem
import com.yourapp.music.presentation.player.PlayerViewModel

@OptIn(UnstableApi::class)
@Composable
fun LikedSongsTab(
    songs: List<Song>,
    onSongClick: (Song, List<Song>) -> Unit,
    modifier: Modifier = Modifier,
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    if (songs.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(R.string.no_liked_songs),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        return
    }

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
                        playerViewModel.playSong(
                            song,
                            songs
                        )
                    }
                },
                isCurrentSong = playerState.currentSong?.id == song.id,
                isActivelyPlaying = playerState.currentSong?.id == song.id &&
                        playerState.isPlaying,
                showPlayButton = false
            )
        }
    }
}
