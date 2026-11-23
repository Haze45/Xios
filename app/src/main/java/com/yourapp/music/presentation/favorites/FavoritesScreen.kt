package com.yourapp.music.presentation.favorites

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.yourapp.music.core.theme.AccentGreen
import com.yourapp.music.core.theme.BackgroundBlack
import com.yourapp.music.core.theme.TextSecondary
import com.yourapp.music.domain.model.Song
import com.yourapp.music.presentation.components.SongListItem
import com.yourapp.music.presentation.player.MiniPlayer
import com.yourapp.music.presentation.player.PlayerViewModel

@OptIn(UnstableApi::class)
@Composable
fun FavoritesScreen(
    onBackClick: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onExpandPlayer: () -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val playerState by playerViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack)
            .systemBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Text(
                    "Liked Songs",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(Modifier.height(12.dp))

            if (state.songs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No liked songs yet — tap the heart on any song to save it here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn {
                    items(
                        state.songs,
                        key = { it.id }) { song ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SongListItem(
                                song = song,
                                onClick = {
                                    onSongClick(
                                        song,
                                        state.songs
                                    )
                                },
                                onPlayClick = {
                                    if (playerState.currentSong?.id == song.id) {
                                        playerViewModel.togglePlayPause()
                                    } else {
                                        playerViewModel.playSong(
                                            song,
                                            state.songs
                                        )
                                    }
                                },
                                modifier = Modifier.weight(
                                    1f
                                ),
                                isCurrentSong = playerState.currentSong?.id == song.id,
                                isActivelyPlaying = playerState.currentSong?.id == song.id &&
                                        playerState.isPlaying,
                                showPlayButton = false
                            )

                            IconButton(onClick = {
                                viewModel.removeFavorite(
                                    song.id
                                )
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = "Unlike ${song.title}",
                                    tint = AccentGreen
                                )
                            }
                        }
                    }
                }
            }
        }

        MiniPlayer(
            onExpandClick = onExpandPlayer,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}
