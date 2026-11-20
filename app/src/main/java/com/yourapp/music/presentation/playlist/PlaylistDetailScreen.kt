package com.yourapp.music.presentation.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.yourapp.music.core.theme.AccentGreen
import com.yourapp.music.core.theme.BackgroundBlack
import com.yourapp.music.core.theme.TextSecondary
import com.yourapp.music.domain.model.Song
import com.yourapp.music.presentation.components.SongListItem
import com.yourapp.music.presentation.player.PlayerViewModel

@Composable
fun PlaylistDetailScreen(
    onBackClick: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val playerState by playerViewModel.uiState.collectAsState()
    val playlist = state.playlist

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack)
            .systemBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Playlist", style = MaterialTheme.typography.titleMedium)
        }

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (playlist == null) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text(
                    state.errorMessage ?: "Playlist not found",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = playlist.coverUrl,
                    contentDescription = playlist.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(playlist.title, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "By ${playlist.ownerName} • ${playlist.songCount} Songs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    playlist.description?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { onPlayAll(state.songs) },
                enabled = state.songs.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = BackgroundBlack),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Play All")
            }

            Spacer(Modifier.height(16.dp))

            if (state.songs.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Text("No songs in this playlist yet.", color = TextSecondary)
                }
            } else {
                LazyColumn {
                    items(state.songs, key = { it.id }) { song ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SongListItem(
                                song = song,
                                onClick = { onSongClick(song, state.songs) },
                                onPlayClick = {
                                    if (playerState.currentSong?.id == song.id) {
                                        playerViewModel.togglePlayPause()
                                    } else {
                                        playerViewModel.playSong(song, state.songs)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                isCurrentSong = playerState.currentSong?.id == song.id,
                                isActivelyPlaying = playerState.currentSong?.id == song.id && playerState.isPlaying
                            )
                            IconButton(onClick = { viewModel.removeSong(song.id) }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Remove from playlist",
                                    tint = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
