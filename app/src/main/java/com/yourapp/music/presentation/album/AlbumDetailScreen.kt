package com.yourapp.music.presentation.album

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.yourapp.music.R
import com.yourapp.music.presentation.components.AppMessageText
import com.yourapp.music.domain.model.Song
import com.yourapp.music.presentation.components.ShimmerDetailScreen
import com.yourapp.music.presentation.components.SongListItem
import com.yourapp.music.presentation.player.MiniPlayer
import com.yourapp.music.presentation.player.PlayerViewModel

@OptIn(UnstableApi::class)
@Composable
fun AlbumDetailScreen(
    onBackClick: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onExpandPlayer: () -> Unit,
    viewModel: AlbumDetailViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val playerState by playerViewModel.uiState.collectAsState()
    val album = state.album

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(
                            R.string.back
                        )
                    )
                }
                Text(
                    stringResource(R.string.album),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (state.isLoading) {
                ShimmerDetailScreen(
                    modifier = Modifier.padding(top = 16.dp)
                )
            } else if (album == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AppMessageText(
                        state.errorMessage
                            ?: "album_not_found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = album.coverUrl,
                        contentDescription = album.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )

                    Spacer(Modifier.width(16.dp))

                    Column {
                        Text(
                            album.title,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(
                                R.string.album_artist_song_count,
                                album.artistName,
                                album.songCount
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = { onPlayAll(state.songs) },
                    enabled = state.songs.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.play_all))
                }

                Spacer(Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(
                        state.songs,
                        key = { it.id }) { song ->
                        SongListItem(
                            song = song,
                            onClick = {
                                onSongClick(
                                    song,
                                    state.songs
                                )
                            },
                            onPlayClick = {},
                            isCurrentSong = playerState.currentSong?.id == song.id,
                            isActivelyPlaying =
                                playerState.currentSong?.id == song.id &&
                                        playerState.isPlaying,
                            showPlayButton = false
                        )
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
